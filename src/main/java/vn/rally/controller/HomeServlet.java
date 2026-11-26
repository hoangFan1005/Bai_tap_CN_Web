package vn.rally.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import vn.rally.model.bo.CatalogBO;

@WebServlet({"", "/home"})
public final class HomeServlet extends HttpServlet {
    private final CatalogBO catalog = new CatalogBO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            var products = catalog.search("", null);
            var categoryImages = new LinkedHashMap<Long, String>();
            products.forEach(product -> categoryImages.putIfAbsent(product.getCategoryId(), product.getImagePath()));
            req.setAttribute("title", "Dụng cụ cầu lông");
            req.setAttribute("section", "home");
            req.setAttribute("homeCategories", catalog.categories().stream().limit(5).toList());
            req.setAttribute("homeCategoryImages", categoryImages);
            req.setAttribute("homeProducts", products.stream().limit(4).toList());
            Web.view(req, resp, "home");
        } catch (SQLException exception) {
            throw new ServletException("Cannot load the storefront", exception);
        }
    }
}
