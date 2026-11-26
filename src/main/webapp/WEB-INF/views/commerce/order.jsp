<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section" aria-labelledby="order-title">
    <div class="page-heading">
        <h1 id="order-title">Đơn hàng #${order.id}</h1>
        <a class="text-link" href="${ctx}/orders">Đơn hàng của tôi</a>
    </div>
    <c:if test="${placed and order.status.code eq 'PENDING'}">
        <p class="notice notice--success" role="status">Bạn đã đặt hàng thành công.</p>
    </c:if>
    <div class="checkout-layout">
        <%@ include file="/WEB-INF/views/commerce/order-items.jspf" %>
        <aside class="panel order-summary" aria-labelledby="shipping-title">
            <%@ include file="/WEB-INF/views/commerce/order-shipping.jspf" %>
        </aside>
    </div>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
