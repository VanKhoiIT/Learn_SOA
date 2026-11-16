# BÀI THỰC HÀNH SỐ 4: DỊCH VỤ QUẢN LÝ ĐƠN HÀNG (ORDER MANAGEMENT SERVICE)

## 1. Mục tiêu và Kiến trúc SOA
- **Kiến trúc hướng dịch vụ (SOA):** Dịch vụ Quản lý Đơn hàng (Order Management Service) hoạt động hoàn toàn độc lập trên cổng riêng (`8082`) và sở hữu cơ sở dữ liệu riêng biệt (`orderdb`), tuân thủ triệt để nguyên tắc *Database-per-Service*.
- **Tích hợp liên dịch vụ (Inter-Service Communication):**
  1. **Xác thực:** Xác thực token JWT của mọi request thông qua Auth Service đã xây dựng ở Bài thực hành số 2 (`http://localhost:8080/auth`).
  2. **Kiểm tra tồn kho:** Khi tạo đơn hàng (`POST /orders`) hoặc thêm chi tiết đơn hàng (`POST /order_items`), Order Service tự động gọi sang Product Service ở Bài thực hành số 3 (`http://localhost:8081/products/{id}`) để lấy đơn giá, tên sản phẩm và kiểm tra số lượng tồn kho theo thời gian thực (chặn đơn hàng nếu số lượng yêu cầu vượt quá tồn kho).

---

## 2. Thông tin Dịch vụ & Cổng kết nối
| Dịch vụ | Cổng (Port) | Cơ sở dữ liệu | Vai trò |
| :--- | :--- | :--- | :--- |
| **Auth Service (Buổi 2)** | `8080` | `auth_db` / In-Memory | Xác thực đăng nhập & cấp phát/kiểm tra Token JWT |
| **Product Service (Buổi 3)** | `8081` | H2 `productdb` | Quản lý danh mục và số lượng tồn kho sản phẩm |
| **Order Service (Buổi 4)** | `8082` | H2 `orderdb` | Quản lý đơn hàng, chi tiết đơn hàng & kiểm tra tồn kho |

- **Swagger UI:** `http://localhost:8082/swagger-ui/index.html`
- **Giao diện Dashboard Live:** `http://localhost:8082/index.html`
- **H2 Console:** `http://localhost:8082/h2-console` (JDBC URL: `jdbc:h2:file:./data/orderdb`, User: `sa`, Pass để trống)

---

## 3. Thiết kế Cơ sở Dữ liệu (Theo đúng Phụ lục Đề bài)

### Bảng Đơn hàng (`orders`)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | ID duy nhất của đơn hàng |
| `customer_name` | VARCHAR(255) | NOT NULL | Tên khách hàng |
| `customer_email` | VARCHAR(255) | NOT NULL | Email khách hàng |
| `total_amount` | DECIMAL(10, 2) | | Tổng số tiền của đơn hàng |
| `status` | VARCHAR(50) | | Trạng thái đơn hàng (`pending`, `completed`, `cancelled`) |
| `created_at` | TIMESTAMP | | Ngày đơn hàng được tạo |
| `updated_at` | TIMESTAMP | | Ngày đơn hàng được cập nhật lần cuối |

### Bảng Chi tiết Đơn hàng (`order_items`)
| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | ID duy nhất của chi tiết đơn hàng |
| `order_id` | INT | FOREIGN KEY (`orders.id`) | ID đơn hàng liên kết |
| `product_id` | INT | NOT NULL | ID sản phẩm (lưu vết để liên kết với Product Service) |
| `product_name` | VARCHAR(255) | NOT NULL | Tên sản phẩm |
| `quantity` | INT | NOT NULL | Số lượng sản phẩm trong đơn hàng |
| `unit_price` | DECIMAL(10, 2) | NOT NULL | Giá sản phẩm tại thời điểm đặt hàng |
| `total_price` | DECIMAL(10, 2) | NOT NULL | Tổng giá trị cho từng sản phẩm (`quantity * unit_price`) |

---

## 4. Danh sách RESTful API Đầy đủ

### A. Quản lý Đơn hàng (Orders API)
- `GET /orders`: Lấy danh sách tất cả các đơn hàng.
- `GET /orders/{id}`: Lấy thông tin chi tiết một đơn hàng kèm danh sách chi tiết mặt hàng.
- `POST /orders`: Tạo đơn hàng mới (xác thực token qua Buổi 2, gọi Buổi 3 kiểm tra tồn kho).
- `PUT /orders/{id}`: Cập nhật thông tin hoặc trạng thái đơn hàng (`pending`, `completed`, `cancelled`).
- `DELETE /orders/{id}`: Xóa một đơn hàng (và các mặt hàng liên quan, tự động hoàn trả kho nếu hủy).

### B. Quản lý Chi tiết Đơn hàng (Order Items API)
- `GET /order_items`: Lấy danh sách tất cả mặt hàng (hỗ trợ lọc theo `?order_id=...`).
- `GET /order_items/{id}`: Lấy thông tin chi tiết một mặt hàng.
- `POST /order_items`: Thêm một mặt hàng mới vào đơn hàng (kiểm tra tồn kho qua Product Service, tự động cập nhật lại tổng tiền đơn hàng cha).
- `PUT /order_items/{id}`: Cập nhật thông tin/số lượng mặt hàng (kiểm tra chênh lệch tồn kho).
- `DELETE /order_items/{id}`: Xóa một mặt hàng khỏi đơn hàng (tự động tính lại tổng tiền đơn hàng).

---

## 5. Hướng dẫn Khởi chạy Hệ thống

### Bước 1: Khởi động Auth Service (Buổi 2 - Cổng 8080)
Mở Terminal 1:
```powershell
cd "d:\MY STUDY\HK1_4\SOA\buoi_2\demo"
.\mvnw.cmd spring-boot:run
```

### Bước 2: Khởi động Product Service (Buổi 3 - Cổng 8081)
Mở Terminal 2:
```powershell
cd "d:\MY STUDY\HK1_4\SOA\buoi_3\demo"
.\mvnw.cmd spring-boot:run
```

### Bước 3: Khởi động Order Service (Buổi 4 - Cổng 8082)
Mở Terminal 3:
```powershell
cd "d:\MY STUDY\HK1_4\SOA\buoi_4\demo"
.\mvnw.cmd spring-boot:run
```

---

## 6. Kiểm thử Tự động (Automated Test Script)
Mở PowerShell tại thư mục `buoi_4\demo` và chạy:
```powershell
.\test_service.ps1
```
Kịch bản test sẽ tự động thực hiện 12 bài kiểm tra từ bảo mật token, kiểm tra tồn kho, tạo đơn hàng, sửa trạng thái đến xóa đơn hàng.

