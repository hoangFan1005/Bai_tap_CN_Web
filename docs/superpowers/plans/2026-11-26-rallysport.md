# Kế hoạch triển khai Rally Sport

> Thực hiện song song các phần độc lập, kiểm thử qua HTTP trên Tomcat và rà soát bản tích hợp trước khi bàn giao.

Mục tiêu là triển khai đề tài 16 thành website bán dụng cụ cầu lông có ba vai trò, quản trị sản phẩm và đặt hàng COD mô phỏng. Thiết kế đã được duyệt trong cuộc trò chuyện ngày 26/11/2026.

Servlet điều phối yêu cầu, BO xử lý nghiệp vụ, DAO làm việc với JDBC, Bean biểu diễn dữ liệu và JSP/JSTL dựng giao diện. Dự án Java 17 đóng gói Maven WAR, chạy trên Tomcat 10.1 và MySQL/MariaDB của XAMPP.

## Giao diện giữa các phần

- `vn.rally.model.dao.Database.open()` trả về kết nối JDBC. Lược đồ nằm trong `database/setup.sql`.
- `vn.rally.controller.Web` cung cấp `view(req, resp, name)` tới `/WEB-INF/views/<name>.jsp`, `redirect(req,resp,path)` và `user(req)`. `User` có `getId()`, `getName()`, `getEmail()`, `isAdmin()`.
- JSP dùng JSTL `jakarta.tags.core`. Giao diện chung là `/WEB-INF/views/common/header.jspf` và `footer.jspf`. Biến `${ctx}` là context path. Mọi biểu mẫu POST gồm trường `_csrf` lấy từ `${csrf}`.
- Các tuyến chung gồm `/home`, `/login`, `/register`, `/logout`. Danh mục tại `/products`, chi tiết tại `/product?id=…`. Giỏ và đặt hàng tại `/cart`, `/checkout`, lịch sử tại `/orders`.

## Công việc

- [x] Nền dự án, tài khoản và bộ lọc. Kiểm tra guest vào trang quản trị bị chuyển đến đăng nhập, user nhận 403, token POST sai bị từ chối, đăng ký chỉ tạo vai trò user.
- [x] Danh mục và quản trị. Tìm theo tên tiếng Việt, lọc danh mục, quản lý sản phẩm và SKU, xóa sản phẩm chưa có đơn, ẩn sản phẩm đã có đơn.
- [x] Giỏ, COD và vận hành đơn. Ghi đơn, chi tiết và cập nhật tồn kho trong cùng transaction. Xác minh thao tác đặt lặp, cạnh tranh tồn kho, quyền sở hữu đơn và hoàn tồn đúng một lần.
- [x] Giao diện tiếng Việt. Dựng trang chủ, hệ thống bố cục và màn hình responsive, rà tại kích thước desktop và điện thoại.
- [x] Tích hợp. Nạp dữ liệu mẫu vào MariaDB, đóng WAR và chạy kiểm thử hồi quy trên Tomcat cục bộ. Hướng dẫn chạy và demo nằm trong README.
