package vn.rally.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import vn.rally.model.bo.AuthBO;

@WebServlet({"/login", "/register", "/logout"})
public final class AuthServlet extends HttpServlet {
    private final AuthBO auth = new AuthBO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getServletPath();
        if ("/logout".equals(path)) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        var account = Web.user(req);
        if (account != null) {
            Web.redirect(req, resp, account.isAdmin() ? "/admin/products" : "/home");
            return;
        }
        if ("/login".equals(path) && req.getSession().getAttribute("newAccount") != null) {
            req.getSession().removeAttribute("newAccount");
            req.setAttribute("notice", "Tạo tài khoản thành công. Bạn có thể đăng nhập.");
        }
        render(req, resp, "/register".equals(path));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getServletPath();
        if ("/logout".equals(path)) {
            req.getSession().invalidate();
            Web.redirect(req, resp, "/home");
            return;
        }
        var current = Web.user(req);
        if (current != null) {
            Web.redirect(req, resp, current.isAdmin() ? "/admin/products" : "/home");
            return;
        }
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        req.setAttribute("emailValue", bounded(email, 254));
        boolean registration = "/register".equals(path);
        try {
            if (registration) {
                String name = req.getParameter("name");
                req.setAttribute("nameValue", bounded(name, 120));
                String error = auth.register(name, email, password);
                if (error == null) {
                    req.getSession().setAttribute("newAccount", true);
                    Web.redirect(req, resp, "/login");
                    return;
                }
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                req.setAttribute("error", error);
            } else {
                var account = auth.authenticate(email, password);
                if (account != null) {
                    req.changeSessionId();
                    var session = req.getSession();
                    session.setAttribute("csrf", Web.token());
                    session.setAttribute("account", account);
                    Web.redirect(req, resp, account.isAdmin() ? "/admin/products" : "/products");
                    return;
                }
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                req.setAttribute("error", "Email hoặc mật khẩu chưa đúng.");
            }
            render(req, resp, registration);
        } catch (SQLException exception) {
            throw new ServletException("Cannot access accounts", exception);
        }
    }

    private static void render(HttpServletRequest req, HttpServletResponse resp, boolean registration)
            throws IOException, ServletException {
        req.setAttribute("title", registration ? "Tạo tài khoản" : "Đăng nhập");
        req.setAttribute("section", "account");
        req.setAttribute("registration", registration);
        Web.view(req, resp, "auth");
    }

    private static String bounded(String text, int length) {
        return text == null || text.length() <= length ? text : text.substring(0, length);
    }
}
