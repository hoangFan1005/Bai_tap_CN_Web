package vn.rally.model.bo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import vn.rally.model.bean.Cart;
import vn.rally.model.bean.CartLine;
import vn.rally.model.bean.Order;
import vn.rally.model.bean.OrderStatus;
import vn.rally.model.dao.OrderDAO;
import vn.rally.model.dao.OrderDAO.StatusChange;

public final class CommerceBO {
    private static final Pattern CHECKOUT_KEY = Pattern.compile("[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}");
    private final OrderDAO orders = new OrderDAO();

    public record CartSummary(List<CartLine> lines, BigDecimal total, boolean ready) {}

    public CartSummary cartSummary(Cart cart) throws SQLException {
        Map<Long, Integer> quantities = cart.contents();
        Map<Long, CartLine> available = orders.cartLines(quantities);
        List<CartLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean ready = !quantities.isEmpty();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            long id = entry.getKey();
            CartLine line = available.get(id);
            if (line == null) {
                line = new CartLine(id, 0, "Sản phẩm không còn bán", "", "", "", BigDecimal.ZERO, 0, false,
                        entry.getValue());
            }
            lines.add(line);
            total = total.add(line.getSubtotal());
            ready &= line.isAvailable();
        }
        return new CartSummary(lines, total, ready);
    }

    public void changeCart(Cart cart, String action, String rawId, String rawQuantity)
            throws CommerceException, SQLException {
        long variantId = parseId(rawId);
        if ("remove".equals(action)) {
            cart.setQuantity(variantId, 0);
            return;
        }
        if (!"add".equals(action) && !"update".equals(action)) {
            throw new CommerceException("Thao tác giỏ hàng không hợp lệ.");
        }
        int quantity = parseQuantity(rawQuantity);
        int existing = cart.quantity(variantId);
        if ("update".equals(action) && existing == 0) {
            throw new CommerceException("Sản phẩm này không còn trong giỏ hàng.", true);
        }
        long desired = "add".equals(action) ? (long) existing + quantity : quantity;
        CartLine line = orders.cartLines(Map.of(variantId, 1)).get(variantId);
        if (line == null || !line.isEditable()) {
            throw new CommerceException("Phiên bản sản phẩm này đã hết hàng hoặc ngừng bán.", true);
        }
        if (desired > line.getStock()) {
            throw new CommerceException("Kho chỉ còn " + line.getStock() + " sản phẩm cho phiên bản này.", true);
        }
        cart.setQuantity(variantId, (int) desired);
    }

    public long checkout(long userId, Cart cart, String key, String rawName, String rawPhone, String rawAddress)
            throws CommerceException, SQLException {
        if (key == null || !CHECKOUT_KEY.matcher(key).matches()) {
            throw new CommerceException("Phiên đặt hàng không hợp lệ. Vui lòng tải lại trang.");
        }
        if (!key.equals(cart.getCheckoutKey())) {
            OptionalLong existing = orders.findCheckout(userId, key);
            if (existing.isPresent()) {
                return existing.getAsLong();
            }
            throw new CommerceException("Giỏ hàng đã thay đổi. Vui lòng xem lại giỏ hàng và đặt lại.", true);
        }
        Map<Long, Integer> quantities = cart.contents();
        if (quantities.isEmpty()) {
            throw new CommerceException("Giỏ hàng đang trống.", true);
        }
        ShippingAddress shipping = ShippingAddress.validate(rawName, rawPhone, rawAddress);
        OptionalLong order = orders.placeOrder(userId, key, shipping.recipientName(), shipping.phone(),
                shipping.address(), quantities);
        if (order.isEmpty()) {
            throw new CommerceException("Không thể đặt hàng với số lượng hiện tại. Vui lòng kiểm tra lại giỏ hàng.", true);
        }
        cart.clearIfKey(key);
        return order.getAsLong();
    }

    public List<Order> customerOrders(long userId) throws SQLException {
        return orders.listOrders(userId);
    }

    public Order customerOrder(long userId, long orderId) throws SQLException {
        return orders.findOrder(orderId, userId);
    }

    public boolean cancelCustomerOrder(long userId, long orderId) throws CommerceException, SQLException {
        StatusChange result = orders.cancelCustomerOrder(orderId, userId);
        return switch (result) {
            case UPDATED -> true;
            case NOT_FOUND -> false;
            case INVALID -> throw new CommerceException("Chỉ có thể hủy đơn hàng khi đang chờ xác nhận.", true);
            case RESTOCK_LIMIT -> throw new CommerceException("Không thể hoàn số lượng vào kho cho đơn hàng này.", true);
        };
    }

    public List<Order> adminOrders() throws SQLException {
        return orders.listOrders(null);
    }

    public Order adminOrder(long orderId) throws SQLException {
        return orders.findOrder(orderId, null);
    }

    public boolean setOrderStatus(long orderId, String rawStatus) throws CommerceException, SQLException {
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(rawStatus == null ? "" : rawStatus);
        } catch (IllegalArgumentException e) {
            throw new CommerceException("Trạng thái đơn hàng không hợp lệ.");
        }
        StatusChange result = orders.changeStatus(orderId, status);
        return switch (result) {
            case UPDATED -> true;
            case NOT_FOUND -> false;
            case INVALID -> throw new CommerceException("Đơn hàng không thể chuyển sang trạng thái đã chọn.", true);
            case RESTOCK_LIMIT -> throw new CommerceException("Không thể hoàn số lượng vào kho cho đơn hàng này.", true);
        };
    }

    public static long parseId(String raw) throws CommerceException {
        try {
            long id = Long.parseLong(raw);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Report malformed and out-of-range numbers as form validation errors.
        }
        throw new CommerceException("Mã không hợp lệ.");
    }

    private static int parseQuantity(String raw) throws CommerceException {
        try {
            int quantity = Integer.parseInt(raw);
            if (quantity > 0) {
                return quantity;
            }
        } catch (NumberFormatException ignored) {
            // Report malformed and out-of-range numbers as form validation errors.
        }
        throw new CommerceException("Vui lòng nhập số lượng nguyên lớn hơn 0.");
    }
}
