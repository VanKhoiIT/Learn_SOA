# HƯỚNG DẪN THỰC HIỆN VÀ CHỤP ẢNH MINH CHỨNG - BÀI THỰC HÀNH SỐ 3
## DỊCH VỤ QUẢN LÝ SẢN PHẨM (PRODUCT MANAGEMENT SERVICE) - KIẾN TRÚC SOA

> **Thư mục dự án:** `buoi_3/demo/`  
> **Thư mục lưu ảnh minh chứng:** `buoi_3/demo/screenshot/`  
> **Phím tắt chụp ảnh trên Windows:** `Windows + Shift + S`

---

## I. TỔNG QUAN KIẾN TRÚC SOA (SERVICE-ORIENTED ARCHITECTURE)

Dịch vụ Quản lý Sản phẩm (Product Management Service) được thiết kế và triển khai tuân thủ đầy đủ các nguyên tắc cốt lõi của **Kiến trúc Hướng dịch vụ (SOA)**:
1. **Dịch vụ độc lập (Loose Coupling & Autonomy):** Chạy trên một tiến trình và cổng riêng biệt: `http://localhost:8081` (khác với Auth Service ở Buổi 2 chạy trên cổng `8080`).
2. **Cơ sở dữ liệu độc lập (Database-per-Service):** Dịch vụ sở hữu cơ sở dữ liệu riêng biệt (`productdb` thông qua H2 Database file-based hoặc MySQL), không dùng chung bảng hay truy vấn trực tiếp bảng của service khác.
3. **Giao tiếp liên dịch vụ (Inter-Service Communication):** Cơ chế xác thực sử dụng Bearer JWT Token được sinh bởi **Auth Service Buổi 2** (`POST http://localhost:8080/login`). Khi nhận request, Product Service sẽ xác thực token qua endpoint `GET http://localhost:8080/auth`.
4. **Chuẩn hóa giao diện RESTful API:** Cung cấp đầy đủ 5 phương thức CRUD chuẩn RESTful và tài liệu trực quan hóa bằng Swagger OpenAPI UI tại `http://localhost:8081/swagger-ui/index.html`.

---

## II. THÔNG TIN CẤU TRÚC DỮ LIỆU BẢNG `products`

| Tên cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `INT / BIGINT (PRIMARY KEY, AUTO_INCREMENT)` | ID duy nhất định danh sản phẩm |
| `name` | `VARCHAR(255) (NOT NULL)` | Tên sản phẩm |
| `description` | `TEXT` | Mô tả chi tiết sản phẩm |
| `price` | `DECIMAL(10, 2)` | Giá sản phẩm |
| `quantity` | `INT` | Số lượng sản phẩm còn trong kho |
| `created_at` | `TIMESTAMP` | Ngày giờ sản phẩm được tạo ban đầu |
| `updated_at` | `TIMESTAMP` | Ngày giờ sản phẩm được cập nhật gần nhất |

---

## III. HƯỚNG DẪN KHỞI ĐỘNG HỆ THỐNG

Để hệ thống hoạt động đầy đủ cả 2 dịch vụ giao tiếp với nhau:

### Bước 1: Khởi động Auth Service (Buổi 2 - Cổng 8080)
Mở Terminal thứ nhất trong VS Code:
```powershell
cd "d:\MY STUDY\HK1_4\SOA\buoi_2\demo"
.\mvnw.cmd spring-boot:run
```
*Dịch vụ Auth Service sẽ sẵn sàng tại `http://localhost:8080`.*

### Bước 2: Khởi động Product Service (Buổi 3 - Cổng 8081)
Mở Terminal thứ hai trong VS Code:
```powershell
cd "d:\MY STUDY\HK1_4\SOA\buoi_3\demo"
.\mvnw.cmd spring-boot:run
```
*Dịch vụ Product Service sẽ sẵn sàng tại `http://localhost:8081`.*

---

