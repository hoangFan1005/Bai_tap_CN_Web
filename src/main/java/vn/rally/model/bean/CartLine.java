package vn.rally.model.bean;

import java.math.BigDecimal;

public record CartLine(long variantId, long productId, String productName, String sku, String sizeLabel,
                       String imagePath, BigDecimal price, int stock, boolean active, int quantity) {
    public long getVariantId() { return variantId; }
    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public String getSizeLabel() { return sizeLabel; }
    public String getImagePath() { return imagePath; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public int getQuantity() { return quantity; }

    public boolean isAvailable() {
        return active && stock >= quantity;
    }

    public boolean isEditable() {
        return active && stock > 0;
    }

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
