<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section">
    <div class="page-heading">
        <h1>Quản lý danh mục</h1>
        <div class="inline-actions">
            <a class="button button--secondary" href="${ctx}/admin/products">Quản lý sản phẩm</a>
            <a class="button button--primary" href="${ctx}/admin/categories/new">Thêm danh mục</a>
        </div>
    </div>
    <c:if test="${not empty notice}">
        <p class="notice notice--success" role="status"><c:out value="${notice}"/></p>
    </c:if>
    <c:if test="${not empty error}">
        <p class="notice notice--error" role="alert"><c:out value="${error}"/></p>
    </c:if>
    <c:choose>
        <c:when test="${empty categories}">
            <p class="empty-state">Chưa có danh mục.</p>
        </c:when>
        <c:otherwise>
            <div class="panel table-scroll">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th scope="col">Tên danh mục</th>
                            <th scope="col">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="category" items="${categories}">
                            <tr>
                                <th scope="row"><c:out value="${category.name}"/></th>
                                <td>
                                    <div class="inline-actions">
                                        <a class="button button--quiet" href="${ctx}/admin/categories/edit?id=${category.id}">Sửa</a>
                                        <form method="post" action="${ctx}/admin/categories/delete"
                                              data-confirm="Xóa danh mục này?">
                                            <input type="hidden" name="_csrf" value="${csrf}">
                                            <input type="hidden" name="id" value="${category.id}">
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
