package vn.rally.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter("/*")
public final class StoreFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var resp = (HttpServletResponse) response;
        String mapping = req.getServletPath();
        resp.setHeader("X-Content-Type-Options", "nosniff");
        if (mapping.startsWith("/assets/")) {
            chain.doFilter(req, resp);
            return;
        }

        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'");
        resp.setHeader("Referrer-Policy", "same-origin");
        req.setAttribute("ctx", req.getContextPath());
        var account = Web.user(req);
        req.setAttribute("account", account);

        boolean admin = mapping.equals("/admin") || mapping.startsWith("/admin/");
        boolean customer = mapping.equals("/cart") || mapping.equals("/checkout")
                || mapping.equals("/orders") || mapping.equals("/order");
        if ((admin || customer) && account == null) {
            Web.redirect(req, resp, "/login");
            return;
        }

        var session = req.getSession();
        String token;
        synchronized (session) {
            token = (String) session.getAttribute("csrf");
            if (token == null) {
                token = Web.token();
                session.setAttribute("csrf", token);
            }
        }
        req.setAttribute("csrf", token);
        if ((admin && !account.isAdmin()) || (customer && account.isAdmin())
                || ("POST".equals(req.getMethod()) && !validToken(token, req.getParameter("_csrf")))) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("title", "Truy cập bị từ chối");
            Web.view(req, resp, "forbidden");
            return;
        }
        chain.doFilter(req, resp);
    }

    private static boolean validToken(String expected, String submitted) {
        return submitted != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }
}
