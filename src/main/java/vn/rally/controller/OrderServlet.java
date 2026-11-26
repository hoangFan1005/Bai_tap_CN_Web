package vn.rally.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import vn.rally.model.bean.Order;
import vn.rally.model.bo.CommerceBO;
import vn.rally.model.bo.CommerceException;

@WebServlet({"/orders", "/order"})
public final class OrderServlet extends HttpServlet {
    private final CommerceBO commerce = new CommerceBO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long userId = Web.user(request).getId();
        try {
            if ("/orders".equals(request.getServletPath())) {
                request.setAttribute("title", "Đơn hàng của tôi");
                request.setAttribute("orders", commerce.customerOrders(userId));
                Web.view(request, response, "commerce/orders");
                return;
            }
            Order order = commerce.customerOrder(userId, CommerceBO.parseId(request.getParameter("id")));
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("title", "Chi tiết đơn hàng #" + order.getId());
            request.setAttribute("order", order);
            request.setAttribute("placed", "1".equals(request.getParameter("placed")));
            Web.view(request, response, "commerce/order");
        } catch (CommerceException invalid) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException e) {
            throw new ServletException("Could not show the customer's orders", e);
        }
    }
}
