package vn.rally.model.bo;

import vn.rally.model.bean.Category;
import vn.rally.model.bean.Product;
import vn.rally.model.bean.ProductDeletion;
import vn.rally.model.bean.Variant;
import vn.rally.model.dao.CatalogDAO;
import vn.rally.model.dao.CatalogDAO.VariantChange;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class CatalogBO {
    private static final Pattern SKU = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,47}");
    private static final Pattern VND = Pattern.compile("[0-9]{1,12}");
    private final CatalogDAO catalog = new CatalogDAO();

    public List<Category> categories() throws SQLException {
        return catalog.categories();
    }

    public List<Product> search(String query, Long categoryId) throws SQLException {
        return catalog.listProducts(clean(query), categoryId, false);
    }

    public List<Product> adminProducts(String query, Long categoryId) throws SQLException {
        return catalog.listProducts(clean(query), categoryId, true);
    }

    public Product product(long id, boolean includeInactive) throws SQLException {
        return catalog.product(id, includeInactive);
    }

    public Category category(long id) throws SQLException {
        return catalog.category(id);
    }

    public boolean saveProduct(long id, ProductFields fields, List<VariantFields> variants)
            throws SQLException, ValidationException {
        ProductInput product = validateProduct(id, fields, variants);
        try {
            return catalog.saveProduct(product.product(), product.variants());
        } catch (SQLException e) {
            switch (e.getErrorCode()) {
                case 1062 -> throw new ValidationException("Mã SKU đã được sử dụng.");
                case 1451 -> throw new ValidationException(
                        "Phiên bản đã có đơn hàng. Bạn có thể đặt tồn kho về 0.");
                case 1452 -> throw new ValidationException("Danh mục đã chọn không còn tồn tại.");
                default -> {
                    if ("40001".equals(e.getSQLState()) || e.getErrorCode() == 1205) {
                        throw new ValidationException(
                                "Sản phẩm hoặc tồn kho vừa thay đổi. Hãy mở lại trang sửa rồi thử lại.");
                    }
                    throw e;
                }
            }
        }
    }

    public ProductDeletion deleteProduct(long id) throws SQLException {
        return catalog.deleteProduct(id);
    }

    public boolean saveCategory(long id, String name) throws SQLException, ValidationException {
        Category category = new Category(id, requiredText(name, 80, "Tên danh mục"));
        try {
            return catalog.saveCategory(category);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new ValidationException("Tên danh mục đã được sử dụng.");
            }
            throw e;
        }
    }

    public boolean deleteCategory(long id) throws SQLException, ValidationException {
        try {
            return catalog.deleteCategory(id);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                throw new ValidationException("Danh mục đang có sản phẩm. Hãy chuyển các sản phẩm sang danh mục khác.");
            }
            throw e;
        }
    }

    public record ProductFields(String categoryId, String name, String brand, String description,
                                String price, String imagePath, boolean active) {
        public String getCategoryId() { return categoryId; }
        public String getName() { return name; }
        public String getBrand() { return brand; }
        public String getDescription() { return description; }
        public String getPrice() { return price; }
        public String getImagePath() { return imagePath; }
        public boolean isActive() { return active; }
    }

    public record VariantFields(String id, String sku, String sizeLabel, String stock,
                                String originalStock, boolean remove) {
        public String getId() { return id; }
        public String getSku() { return sku; }
        public String getSizeLabel() { return sizeLabel; }
        public String getStock() { return stock; }
        public String getOriginalStock() { return originalStock; }
        public boolean isRemove() { return remove; }

        private boolean isBlank() {
            return clean(id).isEmpty() && clean(sku).isEmpty() && clean(sizeLabel).isEmpty() && clean(stock).isEmpty();
        }
    }

    static ProductInput validateProduct(long id, ProductFields fields, List<VariantFields> variantFields)
            throws ValidationException {
        long categoryId = positiveId(fields.categoryId(), "Hãy chọn một danh mục.");
        String name = requiredText(fields.name(), 140, "Tên sản phẩm");
        String brand = requiredText(fields.brand(), 100, "Thương hiệu");
        String description = clean(fields.description());
        if (description.length() > 16000) {
            throw new ValidationException("Mô tả được nhập tối đa 16.000 ký tự.");
        }

        String rawPrice = clean(fields.price());
        if (!VND.matcher(rawPrice).matches() || Long.parseLong(rawPrice) == 0) {
            throw new ValidationException("Giá phải là số nguyên dương, tối đa 12 chữ số.");
        }
        String imagePath = clean(fields.imagePath());
        if (!imagePath.isEmpty() && !Product.isValidImagePath(imagePath)) {
            throw new ValidationException("Hãy nhập tên tệp ảnh có đuôi webp, png, jpg, jpeg hoặc avif.");
        }

        List<VariantChange> variants = validateVariants(id, variantFields);
        Product product = new Product(id, categoryId, null, name, brand, description,
                new BigDecimal(rawPrice), imagePath, fields.active(), false, List.of());
        return new ProductInput(product, variants);
    }

    private static List<VariantChange> validateVariants(long id, List<VariantFields> fields)
            throws ValidationException {
        Set<Long> existingIds = new HashSet<>();
        Set<String> skus = new HashSet<>();
        List<VariantChange> variants = new ArrayList<>();
        for (VariantFields field : fields) {
            if (field.isBlank()) {
                continue;
            }
            long variantId = clean(field.id()).isEmpty() ? 0 : positiveId(field.id(), "Phiên bản không hợp lệ.");
            if (variantId != 0 && (id == 0 || !existingIds.add(variantId))) {
                throw new ValidationException("Phiên bản không hợp lệ.");
            }
            if (field.remove()) {
                if (variantId == 0) {
                    throw new ValidationException("Phiên bản không hợp lệ.");
                }
                variants.add(new VariantChange(new Variant(variantId, null, null, 0), null, true));
                continue;
            }
            String sku = clean(field.sku());
            if (!SKU.matcher(sku).matches()) {
                throw new ValidationException("Mã SKU phải có 1–48 ký tự chữ cái, chữ số hoặc dấu . _ -");
            }
            if (!skus.add(sku.toUpperCase(Locale.ROOT))) {
                throw new ValidationException("Mỗi phiên bản cần một mã SKU riêng.");
            }
            String size = clean(field.sizeLabel());
            if (size.codePointCount(0, size.length()) > 32) {
                throw new ValidationException("Quy cách được nhập tối đa 32 ký tự.");
            }
            int stock = quantity(field.stock());
            Integer originalStock = variantId == 0 ? null : quantity(field.originalStock());
            variants.add(new VariantChange(new Variant(variantId, sku, size, stock), originalStock, false));
        }
        if (skus.isEmpty()) {
            throw new ValidationException("Sản phẩm cần ít nhất một phiên bản.");
        }
        return variants;
    }

    private static String requiredText(String value, int limit, String label) throws ValidationException {
        String cleaned = clean(value);
        if (cleaned.isEmpty() || cleaned.codePointCount(0, cleaned.length()) > limit) {
            throw new ValidationException(label + " phải có từ 1 đến " + limit + " ký tự.");
        }
        return cleaned;
    }

    private static int quantity(String value) throws ValidationException {
        try {
            int quantity = Integer.parseInt(clean(value));
            if (quantity >= 0) {
                return quantity;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new ValidationException("Tồn kho phải là số nguyên không âm.");
    }

    private static long positiveId(String value, String message) throws ValidationException {
        try {
            long id = Long.parseLong(clean(value));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new ValidationException(message);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    record ProductInput(Product product, List<VariantChange> variants) {}

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
