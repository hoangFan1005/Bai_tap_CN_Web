package vn.rally.model.bean;

import java.math.BigDecimal;

public record OrderItem(String productName, String sku, String sizeLabel, BigDecimal unitPrice, int quantity) {
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public String getSizeLabel() { return sizeLabel; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
