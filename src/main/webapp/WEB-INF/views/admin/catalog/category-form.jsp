<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section">
    <div class="page-heading">
        <h1><c:out value="${title}"/></h1>
        <a class="button button--quiet" href="${ctx}/admin/categories">Về danh sách danh mục</a>
    </div>
    <c:if test="${not empty error}">
        <p class="notice notice--error" role="alert"><c:out value="${error}"/></p>
    </c:if>
    <form class="panel form-grid" method="post" action="${ctx}/admin/categories/save">
        <input type="hidden" name="_csrf" value="${csrf}">
        <c:if test="${categoryId > 0}">
            <input type="hidden" name="id" value="${categoryId}">
        </c:if>
        <label class="field">
            <span>Tên danh mục</span>
            <input name="name" maxlength="80" required value="<c:out value='${categoryName}'/>">
        </label>
        <div class="inline-actions">
            <button class="button button--primary" type="submit">Lưu danh mục</button>
            <a class="button button--secondary" href="${ctx}/admin/categories">Hủy</a>
        </div>
    </form>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
