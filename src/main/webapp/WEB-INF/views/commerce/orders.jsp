<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section" aria-labelledby="orders-title">
    <div class="page-heading"><h1 id="orders-title">Đơn hàng của tôi</h1></div>
    <c:choose>
        <c:when test="${empty orders}">
            <div class="empty-state panel">
                <h2>Bạn chưa có đơn hàng</h2>
                <a class="button button--primary" href="${ctx}/products">Khám phá sản phẩm</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="panel table-scroll" role="region" aria-label="Lịch sử đơn hàng" tabindex="0">
                <table class="data-table">
                    <thead><tr><th scope="col">Mã đơn</th><th scope="col">Ngày đặt</th><th scope="col">Trạng thái</th><th scope="col">Tổng tiền</th></tr></thead>
                    <tbody>
                        <c:forEach items="${orders}" var="order">
                            <tr>
                                <th scope="row"><a class="text-link" href="${ctx}/order?id=${order.id}">#${order.id}</a></th>
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
