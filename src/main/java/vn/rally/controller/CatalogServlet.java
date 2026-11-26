package vn.rally.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.rally.model.bean.Product;
import vn.rally.model.bo.CatalogBO;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/products", "/product"})
public class CatalogServlet extends HttpServlet {
    private final CatalogBO catalog = new CatalogBO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("section", "products");
        try {
            if ("/product".equals(request.getServletPath())) {
                Long id = positiveId(request.getParameter("id"));
                Product product = id == null ? null : catalog.product(id, false);
                if (product == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("title", product.getName());
                request.setAttribute("product", product);
                Web.view(request, response, "catalog/detail");
            } else {
                String query = request.getParameter("q");
                Long category = positiveId(request.getParameter("category"));
                request.setAttribute("title", "Sản phẩm cầu lông");
                request.setAttribute("query", query == null ? "" : query.strip());
                request.setAttribute("selectedCategory", category);
                request.setAttribute("categories", catalog.categories());
                request.setAttribute("products", catalog.search(query, category));
                Web.view(request, response, "catalog/list");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    static Long positiveId(String value) {
        if (value != null) {
            try {
                long id = Long.parseLong(value.strip());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
