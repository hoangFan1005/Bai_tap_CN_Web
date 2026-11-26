package vn.rally.model.bean;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public record Order(long id, String recipientName, String phone, String address, OrderStatus status,
                    BigDecimal total, Timestamp createdAt, String customerName, String customerEmail,
                    List<OrderItem> items) {
    public long getId() { return id; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public List<OrderItem> getItems() { return items; }
}
