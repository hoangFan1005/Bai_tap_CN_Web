package vn.rally.model.dao;

import vn.rally.model.bean.Category;
import vn.rally.model.bean.Product;
import vn.rally.model.bean.ProductDeletion;
import vn.rally.model.bean.Variant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CatalogDAO {
    private static final int REFERENCED_ROW = 1451;
    private static final String PRODUCTS = """
            SELECT p.id, p.category_id, c.name AS category_name, p.name, p.brand,
                   p.description, p.price, p.image_path, p.active,
                   EXISTS(SELECT 1 FROM product_variants v WHERE v.product_id = p.id AND v.stock > 0) AS in_stock
            FROM products p
            JOIN categories c ON c.id = p.category_id
            """;

    public record VariantChange(Variant variant, Integer originalStock, boolean remove) {}

    public List<Category> categories() throws SQLException {
        try (Connection connection = Database.open();
             PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM categories ORDER BY name");
             ResultSet result = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (result.next()) {
                categories.add(readCategory(result));
            }
            return categories;
        }
    }

    public Category category(long id) throws SQLException {
        try (Connection connection = Database.open()) {
            return category(connection, id);
        }
    }

    public boolean saveCategory(Category category) throws SQLException {
        boolean create = category.getId() == 0;
        String sql = create ? "INSERT INTO categories (name) VALUES (?)" : "UPDATE categories SET name = ? WHERE id = ?";
        try (Connection connection = Database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            if (!create) {
                statement.setLong(2, category.getId());
            }
            return statement.executeUpdate() > 0 || category(connection, category.getId()) != null;
        }
    }

    public boolean deleteCategory(long id) throws SQLException {
        try (Connection connection = Database.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public List<Product> listProducts(String query, Long categoryId, boolean includeInactive) throws SQLException {
        String sql = PRODUCTS + """
                WHERE (? OR p.active = TRUE)
                  AND p.category_id = COALESCE(?, p.category_id)
                  AND LOCATE(?, p.name) > 0
                ORDER BY p.id DESC
                """;
        try (Connection connection = Database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, includeInactive);
            if (categoryId == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(2, categoryId);
            }
            statement.setString(3, query);
            try (ResultSet result = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (result.next()) {
                    products.add(readProduct(result, List.of()));
                }
                return products;
            }
        }
    }

    public Product product(long id, boolean includeInactive) throws SQLException {
        String sql = PRODUCTS + "WHERE p.id = ? AND (? OR p.active = TRUE)";
        try (Connection connection = Database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setBoolean(2, includeInactive);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                List<Variant> variants = new ArrayList<>(variants(connection, id, false).values());
                return readProduct(result, variants);
            }
        }
    }

    public boolean saveProduct(Product product, List<VariantChange> changes) throws SQLException {
        try (Connection connection = Database.open()) {
            connection.setAutoCommit(false);
            try {
                long productId = product.getId();
                Map<Long, Variant> current = Map.of();
                if (productId == 0) {
                    productId = insertProduct(connection, product);
                } else {
                    if (!lockProduct(connection, productId)) {
                        connection.rollback();
                        return false;
                    }
                    current = variants(connection, productId, true);
                    checkVariantChanges(current, changes);
                    updateProduct(connection, product);
                }
                writeVariants(connection, productId, current, changes);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    public ProductDeletion deleteProduct(long id) throws SQLException {
        try (Connection connection = Database.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM products WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0 ? ProductDeletion.DELETED : ProductDeletion.NOT_FOUND;
        } catch (SQLException e) {
            if (e.getErrorCode() != REFERENCED_ROW) {
                throw e;
            }
        }
        try (Connection connection = Database.open();
             PreparedStatement statement = connection.prepareStatement("UPDATE products SET active = FALSE WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
            return ProductDeletion.ARCHIVED;
        }
    }

    private static Category category(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM categories WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readCategory(result) : null;
            }
        }
    }

    private static Category readCategory(ResultSet result) throws SQLException {
        return new Category(result.getLong("id"), result.getString("name"));
    }

    private static Product readProduct(ResultSet result, List<Variant> variants) throws SQLException {
        return new Product(result.getLong("id"), result.getLong("category_id"), result.getString("category_name"),
                result.getString("name"), result.getString("brand"), result.getString("description"),
                result.getBigDecimal("price"), result.getString("image_path"), result.getBoolean("active"),
                result.getBoolean("in_stock"), variants);
    }

    private static Map<Long, Variant> variants(Connection connection, long id, boolean lock) throws SQLException {
        String sql = "SELECT id, sku, size_label, stock FROM product_variants WHERE product_id = ? ORDER BY id"
                + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                Map<Long, Variant> variants = new LinkedHashMap<>();
                while (result.next()) {
                    long variantId = result.getLong("id");
                    variants.put(variantId, new Variant(variantId, result.getString("sku"),
                            result.getString("size_label"), result.getInt("stock")));
                }
                return variants;
            }
        }
    }

    private static boolean lockProduct(Connection connection, long id) throws SQLException {
        try (PreparedStatement lock = connection.prepareStatement("SELECT id FROM products WHERE id = ? FOR UPDATE")) {
            lock.setLong(1, id);
            try (ResultSet result = lock.executeQuery()) {
                return result.next();
            }
        }
    }

    private static long insertProduct(Connection connection, Product product) throws SQLException {
        String sql = """
                INSERT INTO products (category_id, name, brand, description, price, image_path, active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProduct(statement, product);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key for inserted product.");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void updateProduct(Connection connection, Product product) throws SQLException {
        String sql = """
                UPDATE products SET category_id = ?, name = ?, brand = ?, description = ?, price = ?,
                    image_path = ?, active = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindProduct(statement, product);
            statement.setLong(8, product.getId());
            statement.executeUpdate();
        }
    }

    private static void bindProduct(PreparedStatement statement, Product product) throws SQLException {
        statement.setLong(1, product.getCategoryId());
        statement.setString(2, product.getName());
        statement.setString(3, product.getBrand());
        statement.setString(4, product.getDescription());
        statement.setBigDecimal(5, product.getPrice());
        statement.setString(6, product.getImagePath());
        statement.setBoolean(7, product.isActive());
    }

    private static void checkVariantChanges(Map<Long, Variant> current, List<VariantChange> changes)
            throws SQLException {
        Set<Long> submittedIds = new HashSet<>();
        for (VariantChange change : changes) {
            Variant variant = change.variant();
            if (variant.getId() == 0) {
                continue;
            }
            submittedIds.add(variant.getId());
            Variant stored = current.get(variant.getId());
            if (stored == null || (!change.remove() && variant.getStock() != change.originalStock()
                    && stored.getStock() != change.originalStock())) {
                throw new SQLTransactionRollbackException("Product variants changed since the form loaded.", "40001");
            }
        }
        if (!current.keySet().equals(submittedIds)) {
            throw new SQLTransactionRollbackException("Product variants changed since the form loaded.", "40001");
        }
    }

    private static void writeVariants(Connection connection, long productId, Map<Long, Variant> current,
                                      List<VariantChange> changes) throws SQLException {
        deleteAndReleaseSkus(connection, current, changes);
        String insertSql = "INSERT INTO product_variants (sku, size_label, stock, product_id) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE product_variants SET sku = ?, size_label = ?, stock = ? WHERE id = ?";
        try (PreparedStatement insert = connection.prepareStatement(insertSql);
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            for (VariantChange change : changes) {
                if (change.remove()) {
                    continue;
                }
                Variant variant = change.variant();
                Variant stored = current.get(variant.getId());
                PreparedStatement statement = stored == null ? insert : update;
                int stock = variant.getStock();
                if (stored != null && stock == change.originalStock()) {
                    stock = stored.getStock();
                }
                statement.setString(1, variant.getSku());
                statement.setString(2, variant.getSizeLabel());
                statement.setInt(3, stock);
                statement.setLong(4, stored == null ? productId : variant.getId());
                statement.executeUpdate();
            }
        }
    }

    private static void deleteAndReleaseSkus(Connection connection, Map<Long, Variant> current,
                                              List<VariantChange> changes) throws SQLException {
        // Release changed SKU values before writing any new values so that swaps respect the unique index.
        String deleteSql = "DELETE FROM product_variants WHERE id = ?";
        String releaseSql = "UPDATE product_variants SET sku = CONCAT('~', UUID()) WHERE id = ?";
        try (PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement release = connection.prepareStatement(releaseSql)) {
            for (VariantChange change : changes) {
                Variant variant = change.variant();
                Variant stored = current.get(variant.getId());
                if (change.remove()) {
                    delete.setLong(1, variant.getId());
                    delete.executeUpdate();
                } else if (stored != null && !stored.getSku().equals(variant.getSku())) {
                    release.setLong(1, variant.getId());
                    release.executeUpdate();
                }
            }
        }
    }
}
