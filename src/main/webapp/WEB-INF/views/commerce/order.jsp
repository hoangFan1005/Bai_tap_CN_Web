<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section" aria-labelledby="order-title">
    <div class="page-heading">
        <h1 id="order-title">Đơn hàng #${order.id}</h1>
        <a class="text-link" href="${ctx}/orders">Đơn hàng của tôi</a>
    </div>
    <c:if test="${not empty error}">
        <p class="notice notice--error" role="alert"><c:out value="${error}" /></p>
    </c:if>
    <c:if test="${placed and order.status.code eq 'PENDING'}">
        <p class="notice notice--success" role="status">Bạn đã đặt hàng thành công.</p>
    </c:if>
    <c:if test="${cancelled and order.status.code eq 'CANCELLED'}">
        <p class="notice notice--success" role="status">Bạn đã hủy đơn hàng thành công.</p>
    </c:if>
    <div class="checkout-layout">
        <%@ include file="/WEB-INF/views/commerce/order-items.jspf" %>
        <aside class="panel order-summary" aria-labelledby="shipping-title">
            <%@ include file="/WEB-INF/views/commerce/order-shipping.jspf" %>
            <c:if test="${order.status.code eq 'PENDING'}">
                <form class="stack" method="post" action="${ctx}/order"
                      data-confirm="Bạn có chắc muốn hủy đơn hàng này?">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrf}' />">
                    <input type="hidden" name="action" value="cancel">
                    <input type="hidden" name="id" value="${order.id}">
                    <button type="submit" class="button button--danger">Hủy đơn hàng</button>
                </form>
            </c:if>
        </aside>
    </div>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
