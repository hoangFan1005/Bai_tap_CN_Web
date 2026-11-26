package vn.rally.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import vn.rally.model.bean.Cart;
import vn.rally.model.bo.CommerceBO;
import vn.rally.model.bo.CommerceException;

@WebServlet("/cart")
public final class CartServlet extends HttpServlet {
    private final CommerceBO commerce = new CommerceBO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        show(request, response, SessionCart.get(request.getSession()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Cart cart = SessionCart.get(session);
        try {
            synchronized (cart) {
                commerce.changeCart(cart, request.getParameter("action"), request.getParameter("variantId"),
                        request.getParameter("quantity"));
                session.setAttribute("cartCount", cart.getCount());
            }
            Web.redirect(request, response, "/cart");
        } catch (CommerceException invalid) {
            response.setStatus(invalid.isConflict() ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", invalid.getMessage());
            show(request, response, cart);
        } catch (SQLException e) {
            throw new ServletException("Could not update the cart", e);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Cart cart)
            throws ServletException, IOException {
        try {
            SessionCart.exposeSummary(request, commerce.cartSummary(cart));
            request.setAttribute("title", "Giỏ hàng");
            Web.view(request, response, "commerce/cart");
        } catch (SQLException e) {
            throw new ServletException("Could not show the cart", e);
        }
    }
}
