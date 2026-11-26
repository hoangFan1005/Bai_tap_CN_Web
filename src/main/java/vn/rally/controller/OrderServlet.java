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
            showDetail(request, response, userId, CommerceBO.parseId(request.getParameter("id")));
        } catch (CommerceException invalid) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException e) {
            throw new ServletException("Could not show the customer's orders", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!"/order".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (!"cancel".equals(request.getParameter("action"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        long userId = Web.user(request).getId();
        long orderId;
        try {
            orderId = CommerceBO.parseId(request.getParameter("id"));
        } catch (CommerceException invalid) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            try {
                if (commerce.cancelCustomerOrder(userId, orderId)) {
                    Web.redirect(request, response, "/order?id=" + orderId + "&cancelled=1");
                    return;
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (CommerceException invalid) {
                response.setStatus(invalid.isConflict() ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_BAD_REQUEST);
                request.setAttribute("error", invalid.getMessage());
                showDetail(request, response, userId, orderId);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not cancel the customer's order", e);
        }
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long userId, long orderId)
            throws IOException, SQLException, ServletException {
        Order order = commerce.customerOrder(userId, orderId);
        if (order == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("title", "Chi tiết đơn hàng #" + order.getId());
        request.setAttribute("order", order);
        request.setAttribute("placed", "1".equals(request.getParameter("placed")));
        request.setAttribute("cancelled", "1".equals(request.getParameter("cancelled")));
        Web.view(request, response, "commerce/order");
    }
}
