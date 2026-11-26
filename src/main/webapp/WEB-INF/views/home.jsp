<%@ include file="common/header.jspf" %>
<section class="hero" aria-labelledby="home-title">
    <img class="hero__image" src="${ctx}/assets/images/hero.webp" alt="" fetchpriority="high" data-fallback-image>
    <div class="shell hero__inner">
        <div class="hero__content">
            <p class="eyebrow hero__eyebrow">Dụng cụ cầu lông</p>
            <h1 id="home-title">Sẵn sàng<br><em>vào sân.</em></h1>
            <p class="hero__description">Vợt, cầu và phụ kiện cho từng buổi tập và mỗi trận đấu.</p>
            <div class="hero__actions">
                <a class="button button--accent" href="${ctx}/products">
                    Khám phá sản phẩm <span class="arrow" aria-hidden="true">↗</span>
                </a>
                <c:if test="${not empty homeCategories}">
                    <a class="hero__secondary" href="#danh-muc">Mua theo danh mục <span aria-hidden="true">↓</span></a>
                </c:if>
            </div>
        </div>
    </div>
</section>

<c:if test="${not empty homeCategories}">
    <section class="shell section home-section" id="danh-muc" aria-labelledby="category-title">
        <div class="section-heading">
            <h2 id="category-title">Mua theo danh mục</h2>
            <a class="text-link" href="${ctx}/products">Tất cả sản phẩm <span class="arrow" aria-hidden="true">↗</span></a>
        </div>
        <div class="category-grid">
            <c:forEach items="${homeCategories}" var="category">
                <a class="category-card" href="${ctx}/products?category=${category.id}">
                    <span class="category-card__image">
                        <c:if test="${not empty homeCategoryImages[category.id]}">
                            <img src="${ctx}/assets/images/<c:out value='${homeCategoryImages[category.id]}' />" alt="" loading="lazy" data-fallback-image>
                        </c:if>
                    </span>
                    <span class="category-card__label">
                        <strong><c:out value="${category.name}" /></strong>
                        <span class="arrow" aria-hidden="true">↗</span>
                    </span>
                </a>
            </c:forEach>
        </div>
    </section>
</c:if>

<c:if test="${not empty homeProducts}">
    <section class="featured" aria-labelledby="featured-title">
        <div class="shell section home-section">
            <div class="section-heading">
                <h2 id="featured-title">Sản phẩm nổi bật</h2>
                <a class="text-link" href="${ctx}/products">Xem cửa hàng <span class="arrow" aria-hidden="true">↗</span></a>
            </div>
            <div class="product-grid">
                <c:forEach items="${homeProducts}" var="product">
                    <a class="product-card" href="${ctx}/product?id=${product.id}">
                        <span class="product-card__image">
                            <c:if test="${not empty product.imagePath}">
                                <img src="${ctx}/assets/images/<c:out value='${product.imagePath}' />" alt="" loading="lazy" data-fallback-image>
                            </c:if>
                        </span>
                        <div class="product-card__body">
                            <p class="product-card__meta"><c:out value="${product.brand}" /></p>
                            <h3><c:out value="${product.name}" /></h3>
                            <p class="price"><fmt:formatNumber value="${product.price}" maxFractionDigits="0" />&nbsp;₫</p>
                        </div>
                    </a>
                </c:forEach>
            </div>
        </div>
    </section>
</c:if>
<%@ include file="common/footer.jspf" %>
