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

@WebServlet("/checkout")
public final class CheckoutServlet extends HttpServlet {
    private final CommerceBO commerce = new CommerceBO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        show(request, response, SessionCart.get(request.getSession()), true);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Cart cart = SessionCart.get(session);
        try {
            long orderId;
            synchronized (cart) {
                orderId = commerce.checkout(Web.user(request).getId(), cart, request.getParameter("checkoutKey"),
                        request.getParameter("recipientName"), request.getParameter("phone"), request.getParameter("address"));
                session.setAttribute("cartCount", cart.getCount());
            }
            Web.redirect(request, response, "/order?id=" + orderId + "&placed=1");
        } catch (CommerceException invalid) {
            response.setStatus(invalid.isConflict() ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", invalid.getMessage());
            show(request, response, cart, false);
        } catch (SQLException e) {
            throw new ServletException("Could not place the order", e);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Cart cart, boolean redirectEmpty)
            throws ServletException, IOException {
        try {
            synchronized (cart) {
                CommerceBO.CartSummary summary = commerce.cartSummary(cart);
                if (redirectEmpty && !summary.ready()) {
                    Web.redirect(request, response, "/cart");
                    return;
                }
                SessionCart.exposeSummary(request, summary);
                request.setAttribute("checkoutKey", cart.getCheckoutKey());
            }
            request.setAttribute("title", "Đặt hàng");
            Web.view(request, response, "commerce/checkout");
        } catch (SQLException e) {
            throw new ServletException("Could not show checkout", e);
        }
    }
}
