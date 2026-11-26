package vn.rally.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vn.rally.model.bean.Cart;
import vn.rally.model.bo.CommerceBO.CartSummary;

final class SessionCart {
    private static final String CART_ATTRIBUTE = "shoppingCart";

    private SessionCart() {}

    static Cart get(HttpSession session) {
        if (session.getAttribute(CART_ATTRIBUTE) instanceof Cart cart) {
            return cart;
        }
        synchronized (SessionCart.class) {
            if (session.getAttribute(CART_ATTRIBUTE) instanceof Cart cart) {
                return cart;
            }
            Cart cart = new Cart();
            session.setAttribute(CART_ATTRIBUTE, cart);
            return cart;
        }
    }

    static void exposeSummary(HttpServletRequest request, CartSummary summary) {
        request.setAttribute("lines", summary.lines());
        request.setAttribute("cartTotal", summary.total());
        request.setAttribute("cartReady", summary.ready());
    }
}
