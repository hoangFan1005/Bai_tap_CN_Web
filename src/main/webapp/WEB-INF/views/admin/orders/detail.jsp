<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section" aria-labelledby="admin-order-title">
    <div class="page-heading">
        <h1 id="admin-order-title">Đơn hàng #${order.id}</h1>
        <a class="text-link" href="${ctx}/admin/orders">Quản lý đơn hàng</a>
    </div>
    <c:if test="${not empty error}">
        <p class="notice notice--error" role="alert"><c:out value="${error}" /></p>
    </c:if>
    <div class="checkout-layout">
        <%@ include file="/WEB-INF/views/commerce/order-items.jspf" %>
        <aside class="panel order-summary" aria-labelledby="shipping-title">
            <%@ include file="/WEB-INF/views/commerce/order-shipping.jspf" %>
            <dl class="order-meta">
                <div><dt>Tài khoản khách hàng</dt><dd><c:out value="${order.customerName}" /> · <c:out value="${order.customerEmail}" /></dd></div>
            </dl>
            <c:if test="${not empty order.status.nextStatuses}">
                <form class="stack" method="post" action="${ctx}/admin/orders">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrf}' />">
                    <input type="hidden" name="id" value="${order.id}">
                    <label class="field" for="order-status">
                        <span>Cập nhật trạng thái</span>
                        <select id="order-status" name="status" required>
                            <option value="">Chọn trạng thái</option>
                            <c:forEach items="${order.status.nextStatuses}" var="next">
                                <option value="${next.code}">${next.label}</option>
                            </c:forEach>
                        </select>
                    </label>
                    <button type="submit" class="button button--primary">Lưu trạng thái</button>
                </form>
            </c:if>
        </aside>
    </div>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
