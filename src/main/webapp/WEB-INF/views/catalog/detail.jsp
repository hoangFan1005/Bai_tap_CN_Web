<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section">
    <nav class="breadcrumbs" aria-label="Đường dẫn">
        <a href="${ctx}/products">Sản phẩm</a>
        <span aria-hidden="true">/</span>
        <a href="${ctx}/products?category=${product.categoryId}"><c:out value="${product.categoryName}"/></a>
    </nav>
    <div class="product-detail">
        <div class="product-detail__image">
            <img src="${ctx}/assets/images/${product.imagePath}" alt="<c:out value='${product.name}'/>">
        </div>
        <div class="product-detail__body">
            <p class="eyebrow"><c:out value="${product.brand}"/></p>
            <h1><c:out value="${product.name}"/></h1>
            <p class="price"><fmt:formatNumber value="${product.price}" pattern="#,##0"/> ₫</p>
            <c:if test="${not empty product.description}">
                <p class="product-description"><c:out value="${product.description}"/></p>
            </c:if>
            <c:choose>
                <c:when test="${empty product.variants}">
                    <p class="notice">Sản phẩm hiện đã hết hàng.</p>
                </c:when>
                <c:otherwise>
                    <h2>Phiên bản</h2>
                    <ul class="variant-list">
                        <c:forEach var="variant" items="${product.variants}">
                            <li class="panel">
                                <div>
                                    <strong><c:out value="${empty variant.sizeLabel ? 'Tiêu chuẩn' : variant.sizeLabel}"/></strong>
                                    <small><c:out value="${variant.sku}"/></small>
                                    <c:choose>
                                        <c:when test="${variant.stock > 0}"><small>Còn ${variant.stock} sản phẩm</small></c:when>
                                        <c:otherwise><small>Hết hàng</small></c:otherwise>
                                    </c:choose>
                                </div>
                                <c:if test="${variant.stock > 0 and not empty account and not account.admin}">
                                    <form class="inline-actions" method="post" action="${ctx}/cart">
                                        <input type="hidden" name="_csrf" value="${csrf}">
                                        <input type="hidden" name="action" value="add">
                                        <input type="hidden" name="variantId" value="${variant.id}">
                                        <label class="field">
                                            <span>Số lượng</span>
                                            <input type="number" name="quantity" min="1" max="${variant.stock}" value="1" required>
                                        </label>
                                        <button class="button button--primary" type="submit">Thêm vào giỏ</button>
                                    </form>
                                </c:if>
                            </li>
                        </c:forEach>
                    </ul>
                    <c:if test="${product.inStock and empty account}">
                        <a class="button button--primary" href="${ctx}/login">Đăng nhập để mua</a>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
