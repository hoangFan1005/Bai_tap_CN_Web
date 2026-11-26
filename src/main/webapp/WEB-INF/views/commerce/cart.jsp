<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section" aria-labelledby="cart-title">
    <div class="page-heading">
        <h1 id="cart-title">Giỏ hàng</h1>
        <a class="text-link" href="${ctx}/products">Tiếp tục mua sắm <span aria-hidden="true">↗</span></a>
    </div>
    <c:if test="${not empty error}">
        <p class="notice notice--error" role="alert"><c:out value="${error}" /></p>
    </c:if>
    <c:choose>
        <c:when test="${empty lines}">
            <div class="empty-state panel">
                <h2>Giỏ hàng của bạn đang trống</h2>
                <a class="button button--primary" href="${ctx}/products">Khám phá sản phẩm</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="cart-layout">
                <div class="panel table-scroll" role="region" aria-label="Sản phẩm trong giỏ" tabindex="0">
                    <table class="data-table">
                        <thead>
                            <tr><th scope="col">Sản phẩm</th><th scope="col">Đơn giá</th><th scope="col">Số lượng</th><th scope="col">Tạm tính</th><th scope="col"><span class="sr-only">Thao tác</span></th></tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${lines}" var="line">
                                <tr>
                                    <th scope="row">
                                        <div class="table-product">
                                            <c:if test="${not empty line.imagePath}">
                                                <img src="${ctx}/assets/images/<c:out value='${line.imagePath}' />" alt="" loading="lazy" data-fallback-image>
                                            </c:if>
                                            <div>
                                                <c:choose>
                                                    <c:when test="${line.editable}"><a href="${ctx}/product?id=${line.productId}"><c:out value="${line.productName}" /></a></c:when>
                                                    <c:otherwise><c:out value="${line.productName}" /></c:otherwise>
                                                </c:choose>
                                                <c:if test="${not empty line.sku}">
                                                    <small><c:if test="${not empty line.sizeLabel}"><c:out value="${line.sizeLabel}" /> · </c:if><c:out value="${line.sku}" /></small>
                                                </c:if>
                                                <c:if test="${not line.available}">
                                                    <small class="field-error">
                                                        <c:choose>
                                                            <c:when test="${line.editable}">Còn ${line.stock} sản phẩm</c:when>
                                                            <c:otherwise>Hiện không thể đặt</c:otherwise>
                                                        </c:choose>
                                                    </small>
                                                </c:if>
                                            </div>
                                        </div>
                                    </th>
                                    <td><c:if test="${line.productId gt 0}"><fmt:formatNumber value="${line.price}" maxFractionDigits="0" />&nbsp;₫</c:if></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${line.editable}">
                                                <form class="inline-actions" method="post" action="${ctx}/cart">
                                                    <input type="hidden" name="_csrf" value="<c:out value='${csrf}' />">
                                                    <input type="hidden" name="action" value="update">
                                                    <input type="hidden" name="variantId" value="${line.variantId}">
                                                    <label class="sr-only" for="qty-${line.variantId}">Số lượng <c:out value="${line.productName}" /></label>
                                                    <input class="cart-quantity" id="qty-${line.variantId}" type="number" name="quantity" min="1" max="${line.stock}" value="${line.quantity}" required>
                                                    <button class="button button--secondary" type="submit">Cập nhật</button>
                                                </form>
                                            </c:when>
                                            <c:otherwise>${line.quantity}</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><c:if test="${line.productId gt 0}"><fmt:formatNumber value="${line.subtotal}" maxFractionDigits="0" />&nbsp;₫</c:if></td>
                                    <td>
                                        <form method="post" action="${ctx}/cart">
                                            <input type="hidden" name="_csrf" value="<c:out value='${csrf}' />">
                                            <input type="hidden" name="action" value="remove">
                                            <input type="hidden" name="variantId" value="${line.variantId}">
                                            <button class="button button--quiet" type="submit">Xóa<span class="sr-only"> <c:out value="${line.productName}" /></span></button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <aside class="panel order-summary" aria-labelledby="cart-summary-title">
                    <h2 id="cart-summary-title">Tóm tắt giỏ hàng</h2>
                    <dl class="summary-totals">
                        <div class="summary-total"><dt>Tạm tính</dt><dd class="price"><fmt:formatNumber value="${cartTotal}" maxFractionDigits="0" />&nbsp;₫</dd></div>
                    </dl>
                    <c:choose>
                        <c:when test="${cartReady}"><a class="button button--primary" href="${ctx}/checkout">Tiến hành đặt hàng</a></c:when>
                        <c:otherwise><p class="notice notice--error">Hãy cập nhật số lượng hoặc xóa các mặt hàng không còn bán để tiếp tục.</p></c:otherwise>
                    </c:choose>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
