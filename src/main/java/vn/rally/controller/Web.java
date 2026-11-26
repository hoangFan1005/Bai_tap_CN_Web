package vn.rally.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import vn.rally.model.bean.User;

public final class Web {
    private Web() {
    }

    public static void view(HttpServletRequest req, HttpServletResponse resp, String name)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(req, resp);
    }

    public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    public static User user(HttpServletRequest req) {
        var session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("account");
    }

    static String token() {
        return UUID.randomUUID().toString();
    }
}
