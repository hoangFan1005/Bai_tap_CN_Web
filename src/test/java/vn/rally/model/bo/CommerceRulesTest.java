package vn.rally.model.bo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vn.rally.model.bean.Cart;
import vn.rally.model.bean.OrderStatus;

class CommerceRulesTest {
    @Test
    void changingCartContentsInvalidatesTheOldCheckoutKey() {
        Cart cart = new Cart();
        String initialKey = cart.getCheckoutKey();
        cart.setQuantity(12L, 3);
        String populatedKey = cart.getCheckoutKey();

        cart.setQuantity(12L, 3);
        assertEquals(populatedKey, cart.getCheckoutKey());
        assertNotEquals(initialKey, populatedKey);
        assertEquals(3L, cart.getCount());

        cart.setQuantity(12L, 0);
        assertNotEquals(populatedKey, cart.getCheckoutKey());
        assertEquals(Map.of(), cart.contents());
    }

    @Test
    void staleCheckoutCannotClearNewCartContents() {
        Cart cart = new Cart();
        cart.setQuantity(12L, 3);
        String submittedKey = cart.getCheckoutKey();
        cart.setQuantity(13L, 2);

        assertFalse(cart.clearIfKey(submittedKey));
        assertEquals(Map.of(12L, 3, 13L, 2), cart.contents());

        assertTrue(cart.clearIfKey(cart.getCheckoutKey()));
        assertEquals(0L, cart.getCount());
    }

    @Test
    void cancelledAndCompletedOrdersHaveNoFurtherTransitions() {
        assertEquals(List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED), OrderStatus.PENDING.getNextStatuses());
        assertEquals(List.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED), OrderStatus.CONFIRMED.getNextStatuses());
        assertEquals(List.of(OrderStatus.COMPLETED), OrderStatus.SHIPPING.getNextStatuses());
        assertEquals(List.of(), OrderStatus.COMPLETED.getNextStatuses());
        assertEquals(List.of(), OrderStatus.CANCELLED.getNextStatuses());
    }

    @Test
    void shippingAddressRejectsInvalidPhoneAndOversizedFields() throws CommerceException {
        assertThrows(CommerceException.class, () -> ShippingAddress.validate("Nguyễn An", "123", "12 Lê Lợi"));
        assertThrows(CommerceException.class, () -> ShippingAddress.validate("Nguyễn An", "0912345678", "a".repeat(351)));
        ShippingAddress shipping = ShippingAddress.validate(" Nguyễn An ", " +84 912 345 678 ", " 12 Lê Lợi ");
        assertEquals("Nguyễn An", shipping.recipientName());
        assertEquals("+84 912 345 678", shipping.phone());
        assertEquals("12 Lê Lợi", shipping.address());
    }
}
