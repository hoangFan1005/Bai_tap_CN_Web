<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section" aria-labelledby="admin-orders-title">
    <div class="page-heading"><h1 id="admin-orders-title">Quản lý đơn hàng</h1></div>
    <nav class="admin-nav" aria-label="Quản trị cửa hàng">
        <a href="${ctx}/admin/products">Sản phẩm</a>
        <a href="${ctx}/admin/categories">Danh mục</a>
        <a href="${ctx}/admin/orders" aria-current="page">Đơn hàng</a>
    </nav>
    <c:choose>
        <c:when test="${empty orders}"><div class="empty-state panel"><h2>Chưa có đơn hàng</h2></div></c:when>
        <c:otherwise>
            <div class="panel table-scroll" role="region" aria-label="Danh sách đơn hàng" tabindex="0">
                <table class="data-table">
                    <thead><tr><th scope="col">Mã đơn</th><th scope="col">Khách hàng</th><th scope="col">Ngày đặt</th><th scope="col">Trạng thái</th><th scope="col">Tổng tiền</th></tr></thead>
                    <tbody>
                        <c:forEach items="${orders}" var="order">
                            <tr>
                                <th scope="row"><a class="text-link" href="${ctx}/admin/orders?id=${order.id}">#${order.id}</a></th>
                                <td><c:out value="${order.customerName}" /></td>
                                <td><fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm" timeZone="Asia/Ho_Chi_Minh" /></td>
                                <td><span class="status-pill status-pill--${order.status.cssName}">${order.status.label}</span></td>
                                <td class="price"><fmt:formatNumber value="${order.total}" maxFractionDigits="0" />&nbsp;₫</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
