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

@WebServlet("/admin/orders")
public final class AdminOrderServlet extends HttpServlet {
    private final CommerceBO commerce = new CommerceBO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String id = request.getParameter("id");
            if (id != null) {
                showDetail(request, response, CommerceBO.parseId(id));
                return;
            }
            request.setAttribute("title", "Quản lý đơn hàng");
            request.setAttribute("orders", commerce.adminOrders());
            Web.view(request, response, "admin/orders/index");
        } catch (CommerceException invalid) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException e) {
            throw new ServletException("Could not show orders for administration", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long id;
        try {
            id = CommerceBO.parseId(request.getParameter("id"));
        } catch (CommerceException invalid) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            try {
                if (commerce.setOrderStatus(id, request.getParameter("status"))) {
                    Web.redirect(request, response, "/admin/orders?id=" + id);
                    return;
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (CommerceException invalid) {
                response.setStatus(invalid.isConflict() ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_BAD_REQUEST);
                request.setAttribute("error", invalid.getMessage());
                showDetail(request, response, id);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not update the order status", e);
        }
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long id)
            throws IOException, SQLException, ServletException {
        Order order = commerce.adminOrder(id);
        if (order == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("title", "Đơn hàng #" + order.getId());
        request.setAttribute("order", order);
        Web.view(request, response, "admin/orders/detail");
    }
}
