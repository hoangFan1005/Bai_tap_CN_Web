package vn.rally.model.bean;

import java.util.List;
import java.util.Locale;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getCssName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getLabel() {
        return label;
    }

    public List<OrderStatus> getNextStatuses() {
        return switch (this) {
            case PENDING -> List.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> List.of(SHIPPING, CANCELLED);
            case SHIPPING -> List.of(COMPLETED);
            case COMPLETED, CANCELLED -> List.of();
        };
    }
}
