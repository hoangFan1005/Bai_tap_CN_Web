<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="shell section">
    <div class="page-heading">
        <h1><c:out value="${title}"/></h1>
        <a class="button button--quiet" href="${ctx}/admin/products">Về danh sách sản phẩm</a>
    </div>
    <c:if test="${not empty error}">
        <p class="notice notice--error" role="alert"><c:out value="${error}"/></p>
    </c:if>
    <c:choose>
        <c:when test="${empty categories}">
            <div class="empty-state">
                <p>Hãy tạo danh mục cho sản phẩm trước.</p>
                <a class="button button--primary" href="${ctx}/admin/categories/new">Thêm danh mục</a>
            </div>
        </c:when>
        <c:otherwise>
            <form class="panel form-grid" method="post" action="${ctx}/admin/products/save">
                <input type="hidden" name="_csrf" value="${csrf}">
                <c:if test="${productId > 0}">
                    <input type="hidden" name="id" value="${productId}">
                </c:if>
                <label class="field">
                    <span>Tên sản phẩm</span>
                    <input name="name" maxlength="140" autocomplete="off" required value="<c:out value='${fields.name}'/>">
                </label>
                <label class="field">
                    <span>Danh mục</span>
                    <select name="categoryId" required>
                        <option value="">Chọn danh mục</option>
                        <c:forEach var="category" items="${categories}">
                            <option value="${category.id}" ${category.id == selectedCategory ? 'selected' : ''}><c:out value="${category.name}"/></option>
                        </c:forEach>
                    </select>
                </label>
                <label class="field">
                    <span>Thương hiệu</span>
                    <input name="brand" maxlength="100" required value="<c:out value='${fields.brand}'/>">
                </label>
                <label class="field">
                    <span>Giá bán (₫)</span>
                    <input name="price" inputmode="numeric" pattern="[0-9]{1,12}" maxlength="12" required
                           value="<c:out value='${fields.price}'/>">
                </label>
                <label class="field">
                    <span>Tên tệp ảnh</span>
                    <input name="imagePath" list="catalog-images" maxlength="128" placeholder="racket.webp"
                           value="<c:out value='${fields.imagePath}'/>">
                    <datalist id="catalog-images">
                        <option value="racket.webp"></option>
                        <option value="shuttlecock.webp"></option>
                        <option value="apparel.webp"></option>
                        <option value="net.webp"></option>
                        <option value="grip.webp"></option>
                    </datalist>
                </label>
                <label class="field field--wide">
                    <span>Mô tả</span>
                    <textarea name="description" rows="4" maxlength="16000"><c:out value="${fields.description}"/></textarea>
                </label>
                <label class="check-field field--wide">
                    <input type="checkbox" name="active" value="true" ${fields.active ? 'checked' : ''}>
                    <span>Hiển thị sản phẩm trong cửa hàng</span>
                </label>
                <div class="variant-editor field--wide">
                    <h2>Phiên bản sản phẩm</h2>
                    <p>Nhập mã SKU và tồn kho cho mỗi phiên bản mới. Có thể để trống quy cách.</p>
                    <c:forEach var="variant" items="${variants}" varStatus="row">
                        <fieldset class="panel variant-editor__row">
                            <legend>
                                <c:choose>
                                    <c:when test="${empty variant.id}">Thêm phiên bản</c:when>
                                    <c:otherwise>Phiên bản ${row.count}</c:otherwise>
                                </c:choose>
                            </legend>
                            <input type="hidden" name="variantId" value="<c:out value='${variant.id}'/>">
                            <input type="hidden" name="originalStock" value="<c:out value='${variant.originalStock}'/>">
                            <label class="field">
                                <span>Mã SKU</span>
                                <input name="sku" maxlength="48" value="<c:out value='${variant.sku}'/>">
                            </label>
                            <label class="field">
                                <span>Quy cách</span>
                                <input name="sizeLabel" maxlength="32" value="<c:out value='${variant.sizeLabel}'/>">
                            </label>
                            <label class="field">
                                <span>Tồn kho</span>
                                <input name="stock" type="number" min="0" max="2147483647" step="1"
                                       value="<c:out value='${variant.stock}'/>">
                            </label>
                            <c:if test="${not empty variant.id}">
                                <label class="check-field">
                                    <input type="checkbox" name="removeVariant" value="<c:out value='${variant.id}'/>"
                                           ${variant.remove ? 'checked' : ''}>
                                    <span>Xóa phiên bản</span>
                                </label>
                            </c:if>
                        </fieldset>
                    </c:forEach>
                </div>
                <div class="inline-actions">
                    <button class="button button--primary" type="submit">Lưu sản phẩm</button>
                    <a class="button button--secondary" href="${ctx}/admin/products">Hủy</a>
                </div>
            </form>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
