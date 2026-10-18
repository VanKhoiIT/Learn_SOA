# BÀI THỰC HÀNH SỐ 3: DỊCH VỤ QUẢN LÝ SẢN PHẨM (PRODUCT SERVICE)

## 1. Thông tin dịch vụ
- **Cổng hoạt động (Port):** `8081` (chạy độc lập với Auth Service buổi 2 ở cổng `8080`).
- **Cơ sở dữ liệu:** H2 Database dạng file lưu tại `./data/productdb`.
- **H2 Console:** `http://localhost:8081/h2-console` (JDBC URL: `jdbc:h2:file:./data/productdb`, User: `sa`, Pass để trống).
- **Swagger UI:** `http://localhost:8081/swagger-ui/index.html`.
- **Xác thực:** Dùng Token JWT sinh từ API `/login` của Buổi 2 (cổng 8080).

---

## 2. Bảng dữ liệu sản phẩm (products)
- `id`: INT (Primary Key, Auto Increment)
- `name`: VARCHAR(255)
- `description`: TEXT
- `price`: DECIMAL(10, 2)
- `quantity`: INT
- `created_at`: TIMESTAMP
- `updated_at`: TIMESTAMP

---

## 3. Cách khởi chạy hệ thống

### Bước 1: Chạy Auth Service (Buổi 2 - Cổng 8080)
Mở Terminal 1:
```powershell
cd "d:\MY STUDY\HK1_4\SOA\buoi_2\demo"
.\mvnw.cmd spring-boot:run
```

### Bước 2: Chạy Product Service (Buổi 3 - Cổng 8081)
Mở Terminal 2:
```powershell
cd "d:\MY STUDY\HK1_4\SOA\buoi_3\demo"
.\mvnw.cmd spring-boot:run
```

---

## 4. Danh sách các API trong bài làm
- `GET /products`: Lấy danh sách tất cả sản phẩm.
- `GET /products/{id}`: Xem chi tiết một sản phẩm theo ID.
- `POST /products`: Thêm mới một sản phẩm.
- `PUT /products/{id}`: Cập nhật thông tin sản phẩm theo ID.
- `DELETE /products/{id}`: Xóa một sản phẩm theo ID.

---

## 5. Hướng dẫn test nhanh trên Swagger UI
1. Mở Swagger Buổi 2: `http://localhost:8080/swagger-ui/index.html` $\rightarrow$ gọi `POST /login` với `{ "userName": "admin", "password": "123456" }` để lấy Token.
2. Mở Swagger Buổi 3: `http://localhost:8081/swagger-ui/index.html` $\rightarrow$ bấm nút **Authorize 🔓** ở góc trên bên phải $\rightarrow$ dán Token vào $\rightarrow$ bấm Authorize.
3. Thực hiện test các API `GET`, `POST`, `PUT`, `DELETE` trên giao diện Swagger.
