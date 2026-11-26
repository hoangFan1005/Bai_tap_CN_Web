package vn.rally.model.bean;

public class Variant {
    private final long id;
    private final String sku;
    private final String sizeLabel;
    private final int stock;

    public Variant(long id, String sku, String sizeLabel, int stock) {
        this.id = id;
        this.sku = sku;
        this.sizeLabel = sizeLabel;
        this.stock = stock;
    }

    public long getId() { return id; }
    public String getSku() { return sku; }
    public String getSizeLabel() { return sizeLabel; }
    public int getStock() { return stock; }
}
