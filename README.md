# Rally Sport

Đồ án Công nghệ Web, đề tài 16. Website bán vợt, cầu lông, trang phục, lưới và quấn cán, với đặt hàng COD mô phỏng.

## Demo bằng IntelliJ trên máy hiện tại

Mở thư mục RallySport, chọn **Rally Sport - Demo** trên thanh chạy rồi bấm **Run**. IntelliJ chạy Maven `package`, triển khai WAR lên Tomcat 10.1.61 và phục vụ tại [địa chỉ demo cục bộ](http://127.0.0.1:8784/RallySport/). Cấu hình này dùng MariaDB demo ở cổng `33316` để giữ nguyên dữ liệu và các tài khoản mẫu trong phiên hiện tại.

## Chạy dự án

1. Mở XAMPP và khởi động MySQL. Trong phpMyAdmin, chọn **Import** và nạp [database/setup.sql](database/setup.sql). Tệp tạo CSDL riêng `rally_sport`, các bảng và dữ liệu sản phẩm mẫu.
2. Mở thư mục này trong IntelliJ dưới dạng dự án Maven. Chọn JDK 17 trở lên và Tomcat 10.1.x. Chạy `mvn clean package` trong terminal có Maven hoặc bằng cửa sổ công cụ Maven của IntelliJ.
3. Chép `target/RallySport.war` vào thư mục `webapps` của Tomcat, rồi chạy Tomcat và mở `http://localhost:8080/RallySport/`. Nếu dùng IntelliJ với cấu hình Tomcat Local, thêm artifact WAR và đặt Application context là `/RallySport`.

Ứng dụng mặc định kết nối MySQL/MariaDB cục bộ tại `127.0.0.1:3306`, CSDL `rally_sport`, tài khoản XAMPP `root` với mật khẩu trống. Nếu máy dùng cấu hình khác, đặt biến môi trường `RALLY_DB_URL`, `RALLY_DB_USER`, `RALLY_DB_PASSWORD` cho tiến trình Tomcat. Ví dụ giá trị URL cho cổng cục bộ 3307 là `jdbc:mysql://127.0.0.1:3307/rally_sport?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8`.

### Tạo tài khoản quản trị để demo

Đăng ký một tài khoản quản trị riêng bằng trang Đăng ký. Trong thẻ SQL của CSDL `rally_sport`, chạy câu lệnh sau với email vừa đăng ký.

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'quantri@example.test';
```

Đăng xuất và đăng nhập lại để nạp quyền mới. Sử dụng một tài khoản đăng ký khác để demo mua hàng với quyền `USER`. Trình duyệt riêng tư giúp giữ hai phiên đồng thời.

## Kịch bản trình bày

1. **Guest** mở trang chủ, tìm “quấn cán”, lọc danh mục và xem chi tiết.
2. **User** đăng nhập, chọn một size áo, thêm vào giỏ và chỉnh số lượng. Điền thông tin mẫu rồi đặt COD. Xem đơn mới ở trạng thái **Chờ xác nhận**.
3. **Admin** vào trang quản trị, tạo một sản phẩm, tìm sản phẩm đó, sửa giá rồi xóa. Chọn đơn vừa đặt và cập nhật **Đã xác nhận → Đang giao → Hoàn tất**.
4. **User** mở lại lịch sử đơn để quan sát trạng thái mới. Để minh họa hoàn tồn kho, có thể tạo đơn khác và hủy khi còn chờ xác nhận.

## Mã nguồn và kiểm thử

- `controller` chứa Servlet và bộ lọc phân quyền, `model/bo` xử lý nghiệp vụ và `model/dao` chứa JDBC. `model/bean` chứa dữ liệu chuyển giữa các lớp. Các JSP nằm trong `src/main/webapp/WEB-INF/views`.
- `database/setup.sql` chứa bảng tài khoản, danh mục, sản phẩm, biến thể, đơn và chi tiết đơn. Tổng tiền lưu theo VND. Đơn COD trừ tồn kho khi được tạo và giữ nguyên đơn giá, tên hàng, SKU, size đã mua.
- `mvn test` chạy các bài kiểm thử nghiệp vụ. `mvn -Pintegration clean verify` đóng gói WAR và chạy HTTP trên Tomcat nhúng với một CSDL thử nghiệm riêng. Có thể đặt `RALLY_TEST_DB_SERVER`, `RALLY_TEST_DB_USER`, `RALLY_TEST_DB_PASSWORD` cho kết nối thử nghiệm. `RALLY_TEST_DB_SERVER` là URL JDBC tới máy chủ, kết thúc bằng `/`, ví dụ `jdbc:mysql://127.0.0.1:3306/`. Tài khoản thử nghiệm cần quyền tạo và xóa CSDL tạm.

Các ảnh WebP trong `src/main/webapp/assets/images` là ảnh minh họa do ImageGen tích hợp tạo. Ảnh bìa mô tả vận động viên cầu lông Việt Nam trên sân trong nhà tông xanh đậm. Ảnh sản phẩm là ảnh studio của vợt, cầu, áo, lưới và cuộn quấn cán.
