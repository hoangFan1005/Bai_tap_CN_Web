package vn.rally.model.bean;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Cart implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<Long, Integer> quantities = new LinkedHashMap<>();
    private String checkoutKey = UUID.randomUUID().toString();

    public synchronized Map<Long, Integer> contents() {
        return new LinkedHashMap<>(quantities);
    }

    public synchronized int quantity(long variantId) {
        return quantities.getOrDefault(variantId, 0);
    }

    public synchronized long getCount() {
        return quantities.values().stream().mapToLong(Integer::longValue).sum();
    }

    public synchronized String getCheckoutKey() {
        return checkoutKey;
    }

    public synchronized void setQuantity(long variantId, int quantity) {
        if (variantId <= 0 || quantity < 0) {
            throw new IllegalArgumentException("Invalid variant or quantity");
        }
        if (quantity == quantity(variantId)) {
            return;
        }
        if (quantity == 0) {
            quantities.remove(variantId);
        } else {
            quantities.put(variantId, quantity);
        }
        checkoutKey = UUID.randomUUID().toString();
    }

    public synchronized boolean clearIfKey(String key) {
        if (!checkoutKey.equals(key)) {
            return false;
        }
        quantities.clear();
        checkoutKey = UUID.randomUUID().toString();
        return true;
    }
}
