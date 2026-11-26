<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section">
    <div class="page-heading">
        <h1>Sản phẩm cầu lông</h1>
    </div>

    <form class="panel catalog-filters" action="${ctx}/products" method="get" role="search">
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
                <h2>Chưa tìm thấy sản phẩm</h2>
                <c:if test="${not empty query or not empty selectedCategory}">
                    <a class="button button--secondary" href="${ctx}/products">Xóa bộ lọc</a>
                </c:if>
            </div>
        </c:when>
        <c:otherwise>
            <div class="product-grid">
                <c:forEach var="product" items="${products}">
                    <a class="product-card" href="${ctx}/product?id=${product.id}">
                        <span class="product-card__image">
                            <img src="${ctx}/assets/images/${product.imagePath}" alt="" loading="lazy">
                        </span>
                        <div class="product-card__body">
                            <p class="product-card__meta"><c:out value="${product.categoryName}"/> · <c:out value="${product.brand}"/></p>
                            <h2><c:out value="${product.name}"/></h2>
                            <p class="price"><fmt:formatNumber value="${product.price}" pattern="#,##0"/> ₫</p>
                            <c:if test="${not product.inStock}">
                                <p class="product-card__meta">Hết hàng</p>
                            </c:if>
                        </div>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
