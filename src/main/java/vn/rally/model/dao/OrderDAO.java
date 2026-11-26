package vn.rally.model.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.TreeSet;
import vn.rally.model.bean.CartLine;
import vn.rally.model.bean.Order;
import vn.rally.model.bean.OrderItem;
import vn.rally.model.bean.OrderStatus;

public final class OrderDAO {
    private static final BigDecimal MAX_ORDER_TOTAL = new BigDecimal("9999999999999999");
    private static final String ORDER_QUERY = """
            SELECT o.id, o.recipient_name, o.phone, o.address, o.status, o.total, o.created_at,
                   u.name AS customer_name, u.email AS customer_email
            FROM orders o JOIN users u ON u.id = o.user_id
            """;

    public enum StatusChange { UPDATED, NOT_FOUND, INVALID, RESTOCK_LIMIT }

    private record PurchasedLine(long variantId, String productName, String sku, String sizeLabel,
                                 BigDecimal price, int quantity) {}

    private record ProductInfo(String name, BigDecimal price, boolean active) {}

    public Map<Long, CartLine> cartLines(Map<Long, Integer> quantities) throws SQLException {
        if (quantities.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", Collections.nCopies(quantities.size(), "?"));
        String sql = """
                SELECT v.id, v.product_id, v.sku, v.size_label, v.stock,
                       p.name, p.price, p.image_path, p.active
                FROM product_variants v JOIN products p ON p.id = v.product_id
                WHERE v.id IN (%s)
                """.formatted(placeholders);
        try (Connection db = Database.open(); PreparedStatement query = db.prepareStatement(sql)) {
            int index = 1;
            for (long variantId : quantities.keySet()) {
                query.setLong(index++, variantId);
            }
            try (ResultSet rows = query.executeQuery()) {
                Map<Long, CartLine> lines = new HashMap<>();
                while (rows.next()) {
                    long variantId = rows.getLong("id");
                    lines.put(variantId, new CartLine(variantId, rows.getLong("product_id"), rows.getString("name"),
                            rows.getString("sku"), rows.getString("size_label"), validImage(rows.getString("image_path")),
                            rows.getBigDecimal("price"), rows.getInt("stock"), rows.getBoolean("active"),
                            quantities.get(variantId)));
                }
                return lines;
            }
        }
    }

    private static String validImage(String filename) {
        if (filename != null && filename.matches("(?i)[a-z0-9][a-z0-9._-]*\\.(?:jpe?g|png|webp|gif|avif)")) {
            return filename;
        }
        return "";
    }

    public OptionalLong findCheckout(long userId, String checkoutKey) throws SQLException {
        try (Connection db = Database.open()) {
            return findCheckout(db, userId, checkoutKey);
        }
    }

    private static OptionalLong findCheckout(Connection db, long userId, String checkoutKey) throws SQLException {
        try (PreparedStatement query = db.prepareStatement("SELECT id FROM orders WHERE user_id = ? AND checkout_key = ?")) {
            query.setLong(1, userId);
            query.setString(2, checkoutKey);
            try (ResultSet rows = query.executeQuery()) {
                return rows.next() ? OptionalLong.of(rows.getLong("id")) : OptionalLong.empty();
            }
        }
    }

    public OptionalLong placeOrder(long userId, String checkoutKey, String recipientName, String phone, String address,
                                  Map<Long, Integer> quantities) throws SQLException {
        if (quantities.isEmpty()) {
            return OptionalLong.empty();
        }
        try (Connection db = Database.open()) {
            db.setAutoCommit(false);
            try {
                lockAccount(db, userId);
                OptionalLong placed = findCheckout(db, userId, checkoutKey);
                if (placed.isPresent()) {
                    db.commit();
                    return placed;
                }

                List<PurchasedLine> lines = lockInventory(db, quantities);
                BigDecimal total = BigDecimal.ZERO;
                for (PurchasedLine line : lines) {
                    total = total.add(line.price().multiply(BigDecimal.valueOf(line.quantity())));
                }
                if (lines.size() != quantities.size() || total.compareTo(MAX_ORDER_TOTAL) > 0) {
                    db.rollback();
                    return OptionalLong.empty();
                }

                long id = insertOrder(db, userId, checkoutKey, recipientName, phone, address, total);
                writeOrderItemsAndStock(db, id, lines);
                db.commit();
                return OptionalLong.of(id);
            } catch (SQLException | RuntimeException failure) {
                rollback(db, failure);
                throw failure;
            }
        }
    }

    private static void lockAccount(Connection db, long userId) throws SQLException {
        // Serialize checkouts for one account so a retry can see the first transaction's order.
        try (PreparedStatement query = db.prepareStatement("SELECT id FROM users WHERE id = ? FOR UPDATE")) {
            query.setLong(1, userId);
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("Checkout account is missing");
                }
            }
        }
    }

    private static List<PurchasedLine> lockInventory(Connection db, Map<Long, Integer> quantities) throws SQLException {
        Map<Long, Long> productIds = new TreeMap<>();
        try (PreparedStatement variant = db.prepareStatement("SELECT product_id FROM product_variants WHERE id = ?")) {
            for (long variantId : quantities.keySet()) {
                variant.setLong(1, variantId);
                try (ResultSet rows = variant.executeQuery()) {
                    if (!rows.next()) {
                        return List.of();
                    }
                    productIds.put(variantId, rows.getLong("product_id"));
                }
            }
        }

        Map<Long, ProductInfo> products = new HashMap<>();
        // Lock products before variants, in ascending order within each group.
        try (PreparedStatement product = db.prepareStatement("SELECT name, price, active FROM products WHERE id = ? FOR UPDATE")) {
            for (long productId : new TreeSet<>(productIds.values())) {
                product.setLong(1, productId);
                try (ResultSet rows = product.executeQuery()) {
                    if (!rows.next()) {
                        return List.of();
                    }
                    products.put(productId, new ProductInfo(rows.getString("name"), rows.getBigDecimal("price"),
                            rows.getBoolean("active")));
                }
            }
        }

        List<PurchasedLine> lines = new ArrayList<>();
        try (PreparedStatement variant = db.prepareStatement("""
                SELECT product_id, sku, size_label, stock FROM product_variants WHERE id = ? FOR UPDATE
                """)) {
            for (Map.Entry<Long, Long> entry : productIds.entrySet()) {
                long variantId = entry.getKey();
                long productId = entry.getValue();
                int quantity = quantities.get(variantId);
                ProductInfo product = products.get(productId);
                variant.setLong(1, variantId);
                try (ResultSet rows = variant.executeQuery()) {
                    if (!rows.next() || rows.getLong("product_id") != productId || !product.active()
                            || quantity <= 0 || rows.getInt("stock") < quantity) {
                        return List.of();
                    }
                    lines.add(new PurchasedLine(variantId, product.name(), rows.getString("sku"),
                            rows.getString("size_label"), product.price(), quantity));
                }
            }
        }
        return lines;
    }

    private static long insertOrder(Connection db, long userId, String checkoutKey, String name, String phone,
                                    String address, BigDecimal total) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement("""
                INSERT INTO orders (user_id, recipient_name, phone, address, status, total, checkout_key)
                VALUES (?, ?, ?, ?, 'PENDING', ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            insert.setLong(1, userId);
            insert.setString(2, name);
            insert.setString(3, phone);
            insert.setString(4, address);
            insert.setBigDecimal(5, total);
            insert.setString(6, checkoutKey);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Order insert did not return its generated ID");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void writeOrderItemsAndStock(Connection db, long orderId, List<PurchasedLine> lines) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement("""
                    INSERT INTO order_items (order_id, variant_id, product_name, sku, size_label, unit_price, quantity)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """);
             PreparedStatement stock = db.prepareStatement("UPDATE product_variants SET stock = stock - ? WHERE id = ?")) {
            for (PurchasedLine line : lines) {
                insert.setLong(1, orderId);
                insert.setLong(2, line.variantId());
                insert.setString(3, line.productName());
                insert.setString(4, line.sku());
                insert.setString(5, line.sizeLabel());
                insert.setBigDecimal(6, line.price());
                insert.setInt(7, line.quantity());
                insert.executeUpdate();

                stock.setInt(1, line.quantity());
                stock.setLong(2, line.variantId());
                stock.executeUpdate();
            }
        }
    }

    public List<Order> listOrders(Long userId) throws SQLException {
        String where = userId == null ? "" : "WHERE o.user_id = ? ";
        try (Connection db = Database.open(); PreparedStatement query = db.prepareStatement(
                ORDER_QUERY + where + "ORDER BY o.created_at DESC, o.id DESC")) {
            if (userId != null) {
                query.setLong(1, userId);
            }
            try (ResultSet rows = query.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (rows.next()) {
                    orders.add(readOrder(rows, List.of()));
                }
                return orders;
            }
        }
    }

    public Order findOrder(long id, Long userId) throws SQLException {
        String where = userId == null ? "WHERE o.id = ?" : "WHERE o.id = ? AND o.user_id = ?";
        try (Connection db = Database.open(); PreparedStatement query = db.prepareStatement(ORDER_QUERY + where)) {
            query.setLong(1, id);
            if (userId != null) {
                query.setLong(2, userId);
            }
            try (ResultSet rows = query.executeQuery()) {
                return rows.next() ? readOrder(rows, orderItems(db, id)) : null;
            }
        }
    }

    private static Order readOrder(ResultSet rows, List<OrderItem> items) throws SQLException {
        return new Order(rows.getLong("id"), rows.getString("recipient_name"), rows.getString("phone"),
                rows.getString("address"), OrderStatus.valueOf(rows.getString("status")), rows.getBigDecimal("total"),
                rows.getTimestamp("created_at"), rows.getString("customer_name"), rows.getString("customer_email"), items);
    }

    private static List<OrderItem> orderItems(Connection db, long orderId) throws SQLException {
        try (PreparedStatement query = db.prepareStatement("""
                SELECT product_name, sku, size_label, unit_price, quantity FROM order_items WHERE order_id = ? ORDER BY id
                """)) {
            query.setLong(1, orderId);
            try (ResultSet rows = query.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (rows.next()) {
                    items.add(new OrderItem(rows.getString("product_name"), rows.getString("sku"),
                            rows.getString("size_label"), rows.getBigDecimal("unit_price"), rows.getInt("quantity")));
                }
                return items;
            }
        }
    }

    public StatusChange changeStatus(long orderId, OrderStatus next) throws SQLException {
        return transitionStatus(orderId, null, next);
    }

    public StatusChange cancelCustomerOrder(long orderId, long userId) throws SQLException {
        return transitionStatus(orderId, userId, OrderStatus.CANCELLED);
    }

    private StatusChange transitionStatus(long orderId, Long customerId, OrderStatus next) throws SQLException {
        try (Connection db = Database.open()) {
            db.setAutoCommit(false);
            try {
                OrderStatus previous = lockStatus(db, orderId, customerId);
                boolean allowed = previous != null && (customerId == null
                        ? previous.getNextStatuses().contains(next)
                        : previous == OrderStatus.PENDING && next == OrderStatus.CANCELLED);
                StatusChange result;
                if (previous == null) {
                    result = StatusChange.NOT_FOUND;
                } else if (!allowed) {
                    result = StatusChange.INVALID;
                } else if (next == OrderStatus.CANCELLED && !restock(db, orderId)) {
                    db.rollback();
                    return StatusChange.RESTOCK_LIMIT;
                } else {
                    try (PreparedStatement update = db.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
                        update.setString(1, next.getCode());
                        update.setLong(2, orderId);
                        update.executeUpdate();
                    }
                    result = StatusChange.UPDATED;
                }
                db.commit();
                return result;
            } catch (SQLException | RuntimeException failure) {
                rollback(db, failure);
                throw failure;
            }
        }
    }

    private static OrderStatus lockStatus(Connection db, long orderId, Long customerId) throws SQLException {
        String sql = customerId == null
                ? "SELECT status FROM orders WHERE id = ? FOR UPDATE"
                : "SELECT status FROM orders WHERE id = ? AND user_id = ? FOR UPDATE";
        try (PreparedStatement query = db.prepareStatement(sql)) {
            query.setLong(1, orderId);
            if (customerId != null) {
                query.setLong(2, customerId);
            }
            try (ResultSet rows = query.executeQuery()) {
                return rows.next() ? OrderStatus.valueOf(rows.getString("status")) : null;
            }
        }
    }

    private static boolean restock(Connection db, long orderId) throws SQLException {
        try (PreparedStatement query = db.prepareStatement("""
                    SELECT variant_id, quantity FROM order_items WHERE order_id = ? ORDER BY variant_id
                    """);
             PreparedStatement update = db.prepareStatement("""
                    UPDATE product_variants SET stock = stock + ? WHERE id = ? AND stock <= ?
                    """)) {
            query.setLong(1, orderId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    int quantity = rows.getInt("quantity");
                    update.setInt(1, quantity);
                    update.setLong(2, rows.getLong("variant_id"));
                    update.setInt(3, Integer.MAX_VALUE - quantity);
                    if (update.executeUpdate() != 1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void rollback(Connection db, Throwable failure) {
        try {
            db.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
