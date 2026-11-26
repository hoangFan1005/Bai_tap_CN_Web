package vn.rally.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.rally.model.bean.Category;
import vn.rally.model.bean.Product;
import vn.rally.model.bean.ProductDeletion;
import vn.rally.model.bean.Variant;
import vn.rally.model.bo.CatalogBO;
import vn.rally.model.bo.CatalogBO.ProductFields;
import vn.rally.model.bo.CatalogBO.ValidationException;
import vn.rally.model.bo.CatalogBO.VariantFields;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet({"/admin/products/*", "/admin/categories/*"})
public class AdminCatalogServlet extends HttpServlet {
    private static final String PRODUCTS = "/admin/products";
    private static final String CATEGORIES = "/admin/categories";
    private static final VariantFields EMPTY_VARIANT = new VariantFields("", "", "", "", "", false);
    private final CatalogBO catalog = new CatalogBO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean categories = CATEGORIES.equals(request.getServletPath());
        try {
            switch (path(request)) {
                case "/" -> {
                    if (categories) {
                        showCategories(request, response);
                    } else {
                        showProducts(request, response);
                    }
                }
                case "/new" -> {
                    if (categories) {
                        showCategoryForm(request, response, 0, "");
                    } else {
                        showProductForm(request, response, 0,
                                new ProductFields("", "", "", "", "", "", true),
                                List.of(EMPTY_VARIANT, EMPTY_VARIANT, EMPTY_VARIANT));
                    }
                }
                case "/edit" -> {
                    Long id = CatalogServlet.positiveId(request.getParameter("id"));
                    if (id == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    } else if (categories) {
                        editCategory(request, response, id);
                    } else {
                        editProduct(request, response, id);
                    }
                }
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = path(request);
        if (!"/save".equals(action) && !"/delete".equals(action)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String idParam = request.getParameter("id");
        Long id = CatalogServlet.positiveId(idParam);
        if (id == null && ("/delete".equals(action) || (idParam != null && !idParam.isBlank()))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean categories = CATEGORIES.equals(request.getServletPath());
        try {
            if ("/delete".equals(action)) {
                if (categories) {
                    deleteCategory(request, response, id);
                } else {
                    deleteProduct(request, response, id);
                }
            } else if (categories) {
                saveCategory(request, response, id == null ? 0 : id);
            } else {
                saveProduct(request, response, id == null ? 0 : id);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showProducts(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        request.setAttribute("title", "Quản lý sản phẩm");
        request.setAttribute("products", catalog.adminProducts());
        String status = request.getParameter("status");
        if (status != null) {
            request.setAttribute("notice", switch (status) {
                case "saved" -> "Đã lưu sản phẩm.";
                case "deleted" -> "Đã xóa sản phẩm.";
                case "archived" -> "Sản phẩm đã có đơn hàng và đã được ẩn khỏi cửa hàng.";
                default -> null;
            });
        }
        Web.view(request, response, "admin/catalog/products");
    }

    private void editProduct(HttpServletRequest request, HttpServletResponse response, long id)
            throws SQLException, ServletException, IOException {
        Product product = catalog.product(id, true);
        if (product == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ProductFields fields = new ProductFields(Long.toString(product.getCategoryId()), product.getName(),
                product.getBrand(), product.getDescription(), product.getPrice().toPlainString(),
                product.getImagePath(), product.isActive());
        List<VariantFields> variants = new ArrayList<>();
        for (Variant variant : product.getVariants()) {
            String stock = Integer.toString(variant.getStock());
            variants.add(new VariantFields(Long.toString(variant.getId()), variant.getSku(), variant.getSizeLabel(),
                    stock, stock, false));
        }
        variants.add(EMPTY_VARIANT);
        showProductForm(request, response, id, fields, variants);
    }

    private void showProductForm(HttpServletRequest request, HttpServletResponse response, long id,
                                 ProductFields fields, List<VariantFields> variants)
            throws SQLException, ServletException, IOException {
        request.setAttribute("title", id == 0 ? "Thêm sản phẩm" : "Sửa sản phẩm");
        request.setAttribute("productId", id);
        request.setAttribute("fields", fields);
        request.setAttribute("variants", variants);
        request.setAttribute("selectedCategory", CatalogServlet.positiveId(fields.categoryId()));
        request.setAttribute("categories", catalog.categories());
        Web.view(request, response, "admin/catalog/product-form");
    }

    private void saveProduct(HttpServletRequest request, HttpServletResponse response, long id)
            throws SQLException, ServletException, IOException {
        ProductFields fields = new ProductFields(request.getParameter("categoryId"), request.getParameter("name"),
                request.getParameter("brand"), request.getParameter("description"), request.getParameter("price"),
                request.getParameter("imagePath"), "true".equals(request.getParameter("active")));
        List<VariantFields> variants = readVariants(request);
        if (variants == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (!catalog.saveProduct(id, fields, variants)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Web.redirect(request, response, PRODUCTS + "?status=saved");
        } catch (ValidationException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", e.getMessage());
            showProductForm(request, response, id, fields, variants.isEmpty() ? List.of(EMPTY_VARIANT) : variants);
        }
    }

    private static List<VariantFields> readVariants(HttpServletRequest request) {
        String[] ids = parameters(request, "variantId");
        String[] skus = parameters(request, "sku");
        String[] sizes = parameters(request, "sizeLabel");
        String[] stocks = parameters(request, "stock");
        String[] original = parameters(request, "originalStock");
        if (ids.length != skus.length || ids.length != sizes.length || ids.length != stocks.length
                || ids.length != original.length) {
            return null;
        }
        Set<String> removals = new HashSet<>(Arrays.asList(parameters(request, "removeVariant")));
        List<VariantFields> variants = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            variants.add(new VariantFields(ids[i], skus[i], sizes[i], stocks[i], original[i], removals.contains(ids[i])));
        }
        return variants;
    }

    private static String[] parameters(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? new String[0] : values;
    }

    private void deleteProduct(HttpServletRequest request, HttpServletResponse response, long id)
            throws SQLException, IOException {
        ProductDeletion result = catalog.deleteProduct(id);
        if (result == ProductDeletion.NOT_FOUND) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String status = result == ProductDeletion.ARCHIVED ? "archived" : "deleted";
        Web.redirect(request, response, PRODUCTS + "?status=" + status);
    }

    private void showCategories(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        request.setAttribute("title", "Quản lý danh mục");
        request.setAttribute("categories", catalog.categories());
        String status = request.getParameter("status");
        if ("saved".equals(status)) {
            request.setAttribute("notice", "Đã lưu danh mục.");
        } else if ("deleted".equals(status)) {
            request.setAttribute("notice", "Đã xóa danh mục.");
        }
        Web.view(request, response, "admin/catalog/categories");
    }

    private void editCategory(HttpServletRequest request, HttpServletResponse response, long id)
            throws SQLException, ServletException, IOException {
        Category category = catalog.category(id);
        if (category == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            showCategoryForm(request, response, id, category.getName());
        }
    }

    private void showCategoryForm(HttpServletRequest request, HttpServletResponse response, long id, String name)
            throws ServletException, IOException {
        request.setAttribute("title", id == 0 ? "Thêm danh mục" : "Sửa danh mục");
        request.setAttribute("categoryId", id);
        request.setAttribute("categoryName", name);
        Web.view(request, response, "admin/catalog/category-form");
    }

    private void saveCategory(HttpServletRequest request, HttpServletResponse response, long id)
            throws SQLException, ServletException, IOException {
        String name = request.getParameter("name");
        try {
            if (!catalog.saveCategory(id, name)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Web.redirect(request, response, CATEGORIES + "?status=saved");
        } catch (ValidationException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", e.getMessage());
            showCategoryForm(request, response, id, name);
        }
    }

    private void deleteCategory(HttpServletRequest request, HttpServletResponse response, long id)
            throws SQLException, ServletException, IOException {
        try {
            if (!catalog.deleteCategory(id)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Web.redirect(request, response, CATEGORIES + "?status=deleted");
        } catch (ValidationException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            request.setAttribute("error", e.getMessage());
            showCategories(request, response);
        }
    }

    private static String path(HttpServletRequest request) {
        return request.getPathInfo() == null ? "/" : request.getPathInfo();
    }
}
