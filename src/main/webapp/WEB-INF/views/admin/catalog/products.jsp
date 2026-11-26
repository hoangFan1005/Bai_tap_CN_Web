<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section">
    <div class="page-heading">
        <h1>Quản lý sản phẩm</h1>
        <div class="inline-actions">
            <a class="button button--primary" href="${ctx}/admin/products/new">Thêm sản phẩm</a>
        </div>
    </div>
    <nav class="admin-nav" aria-label="Quản trị cửa hàng">
        <a href="${ctx}/admin/products" aria-current="page">Sản phẩm</a>
        <a href="${ctx}/admin/categories">Danh mục</a>
        <a href="${ctx}/admin/orders">Đơn hàng</a>
    </nav>
    <c:if test="${not empty notice}">
        <p class="notice notice--success" role="status"><c:out value="${notice}"/></p>
    </c:if>
    <form class="panel catalog-filters" action="${ctx}/admin/products" method="get" role="search">
        <label class="field">
            <span>Tìm theo tên</span>
            <input type="search" name="q" value="<c:out value='${query}'/>" placeholder="Vợt Rally Aero 88">
        </label>
        <label class="field">
            <span>Danh mục</span>
            <select name="category">
                <option value="">Tất cả danh mục</option>
                <c:forEach var="category" items="${categories}">
                    <option value="${category.id}" ${category.id == selectedCategory ? 'selected' : ''}><c:out value="${category.name}"/></option>
                </c:forEach>
            </select>
        </label>
        <button class="button button--primary" type="submit">Tìm sản phẩm</button>
    </form>
    <c:choose>
        <c:when test="${empty products}">
            <div class="empty-state">
                <c:choose>
                    <c:when test="${not empty query or not empty selectedCategory}">
                        <h2>Chưa tìm thấy sản phẩm</h2>
                        <a class="button button--secondary" href="${ctx}/admin/products">Xóa bộ lọc</a>
                    </c:when>
                    <c:otherwise>
                        <p>Chưa có sản phẩm.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:when>
        <c:otherwise>
            <div class="panel table-scroll">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th scope="col">Sản phẩm</th>
                            <th scope="col">Danh mục</th>
                            <th scope="col">Giá</th>
                            <th scope="col">Trạng thái</th>
                            <th scope="col">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="product" items="${products}">
                            <tr>
                                <th scope="row"><c:out value="${product.name}"/></th>
                                <td><c:out value="${product.categoryName}"/></td>
                                <td><fmt:formatNumber value="${product.price}" pattern="#,##0"/> ₫</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not product.active}">Đang ẩn</c:when>
                                        <c:when test="${product.inStock}">Đang bán</c:when>
                                        <c:otherwise>Hết hàng</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <div class="inline-actions">
                                        <a class="button button--quiet" href="${ctx}/admin/products/edit?id=${product.id}">Sửa</a>
                                        <form method="post" action="${ctx}/admin/products/delete"
                                              data-confirm="Xóa sản phẩm này? Sản phẩm đã có đơn hàng sẽ được ẩn.">
                                            <input type="hidden" name="_csrf" value="${csrf}">
                                            <input type="hidden" name="id" value="${product.id}">
                                            <button class="button button--quiet button--danger" type="submit">Xóa</button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
