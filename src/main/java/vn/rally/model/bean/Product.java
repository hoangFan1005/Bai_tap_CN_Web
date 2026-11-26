package vn.rally.model.bean;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

public class Product {
    private static final Pattern IMAGE_PATH = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9_-]{0,119}\\.(webp|png|jpe?g|avif)", Pattern.CASE_INSENSITIVE);
    private static final String DEFAULT_IMAGE = "racket.webp";

    private final long id;
    private final long categoryId;
    private final String categoryName;
    private final String name;
    private final String brand;
    private final String description;
    private final BigDecimal price;
    private final String imagePath;
    private final boolean active;
    private final boolean inStock;
    private final List<Variant> variants;

    public Product(long id, long categoryId, String categoryName, String name, String brand,
                   String description, BigDecimal price, String imagePath, boolean active,
                   boolean inStock, List<Variant> variants) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.price = price;
        this.imagePath = isValidImagePath(imagePath) ? imagePath : DEFAULT_IMAGE;
        this.active = active;
        this.inStock = inStock;
        this.variants = List.copyOf(variants);
    }

    public long getId() { return id; }
    public long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getImagePath() { return imagePath; }
    public boolean isActive() { return active; }
    public boolean isInStock() { return inStock; }
    public List<Variant> getVariants() { return variants; }

    public static boolean isValidImagePath(String path) {
        return path != null && IMAGE_PATH.matcher(path).matches();
    }
}
