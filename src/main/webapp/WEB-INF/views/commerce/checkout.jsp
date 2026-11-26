<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section" aria-labelledby="checkout-title">
    <div class="page-heading">
        <h1 id="checkout-title">Đặt hàng</h1>
        <a class="text-link" href="${ctx}/cart">Quay lại giỏ hàng</a>
    </div>
    <c:if test="${not empty error}">
        <p class="notice notice--error" role="alert"><c:out value="${error}" /></p>
    </c:if>
    <c:choose>
        <c:when test="${cartReady}">
            <div class="checkout-layout">
                <form class="panel form-grid shipping-form" method="post" action="${ctx}/checkout">
                    <h2 class="field--wide">Thông tin giao hàng</h2>
                    <input type="hidden" name="_csrf" value="<c:out value='${csrf}' />">
                    <input type="hidden" name="checkoutKey" value="<c:out value='${checkoutKey}' />">
                    <label class="field" for="recipient-name">
                        <span>Họ và tên người nhận</span>
                        <input id="recipient-name" name="recipientName" value="<c:out value='${pageContext.request.method eq "POST" ? param.recipientName : account.name}' />" maxlength="120" autocomplete="name" required>
                    </label>
                    <label class="field" for="recipient-phone">
                        <span>Số điện thoại</span>
                        <input id="recipient-phone" type="tel" name="phone" value="<c:out value='${param.phone}' />" maxlength="24" autocomplete="tel" required>
                    </label>
                    <label class="field field--wide" for="recipient-address">
                        <span>Địa chỉ giao hàng</span>
                        <textarea id="recipient-address" name="address" rows="3" maxlength="350" autocomplete="street-address" required><c:out value="${param.address}" /></textarea>
                    </label>
                    <div class="field--wide">
                        <button class="button button--primary" type="submit">Xác nhận đặt hàng</button>
                    </div>
                </form>
                <aside class="panel order-summary" aria-labelledby="checkout-summary-title">
                    <h2 id="checkout-summary-title">Đơn hàng của bạn</h2>
                    <dl class="summary-totals">
                        <c:forEach items="${lines}" var="line">
                            <div>
                                <dt><c:out value="${line.productName}" /> × ${line.quantity}<c:if test="${not empty line.sizeLabel}"><br><small><c:out value="${line.sizeLabel}" /></small></c:if></dt>
                                <dd><fmt:formatNumber value="${line.subtotal}" maxFractionDigits="0" />&nbsp;₫</dd>
                            </div>
                        </c:forEach>
                        <div class="summary-total"><dt>Tổng cộng</dt><dd class="price"><fmt:formatNumber value="${cartTotal}" maxFractionDigits="0" />&nbsp;₫</dd></div>
                    </dl>
                    <p>Thanh toán khi nhận hàng (COD)</p>
                </aside>
            </div>
        </c:when>
        <c:otherwise>
            <div class="empty-state panel">
                <h2>Giỏ hàng chưa sẵn sàng để đặt</h2>
                <a class="button button--secondary" href="${ctx}/cart">Xem giỏ hàng</a>
            </div>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
