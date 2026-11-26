<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="container section auth-layout">
    <div class="auth-side">
        <img src="${ctx}/assets/images/hero.webp" alt="" width="1080" height="900">
    </div>
    <div class="auth-card panel">
        <h1 class="page-title"><c:out value="${title}"/></h1>
        <c:if test="${not empty notice}">
            <p class="alert alert-success" role="status"><c:out value="${notice}"/></p>
        </c:if>
        <c:if test="${not empty error}">
            <p class="alert alert-error" role="alert"><c:out value="${error}"/></p>
        </c:if>
        <form method="post" action="${ctx}${registration ? '/register' : '/login'}" class="stack">
            <input type="hidden" name="_csrf" value="${csrf}">
            <c:if test="${registration}">
                <label class="field">Họ và tên
                    <input name="name" autocomplete="name" maxlength="120" value="<c:out value='${nameValue}'/>" required>
                </label>
            </c:if>
            <label class="field">Email
                <input name="email" type="email" autocomplete="email" maxlength="254" value="<c:out value='${emailValue}'/>" required>
            </label>
            <label class="field">Mật khẩu
                <input name="password" type="password" autocomplete="${registration ? 'new' : 'current'}-password" minlength="8" maxlength="128" required>
            </label>
            <button class="btn btn-primary" type="submit"><c:out value="${title}"/></button>
        </form>
        <p>
            <c:choose>
                <c:when test="${registration}">Đã có tài khoản? <a href="${ctx}/login">Đăng nhập</a></c:when>
                <c:otherwise>Chưa có tài khoản? <a href="${ctx}/register">Tạo tài khoản</a></c:otherwise>
            </c:choose>
        </p>
    </div>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
