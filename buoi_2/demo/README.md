# HƯỚNG DẪN THỰC HIỆN VÀ CHỤP ẢNH MINH CHỨNG - BÀI THỰC HÀNH 2

> Thư mục lưu ảnh: `buoi_2/demo/screenshot/`  
> Công cụ chụp ảnh nhanh trên Windows: Nhấn tổ hợp phím `Windows + Shift + S`

---

## 1. Ảnh Minh chứng 1: Đăng nhập sinh JWT Token
- **Tên file lưu:** `minhChung1.png`
- **Mục tiêu:** Chứng minh API `/login` hoạt động và sinh ra chuỗi Token.
- **Các bước thực hiện trên Swagger UI:**
  1. Mở trình duyệt vào: `http://localhost:8080/swagger-ui/index.html`
  2. Tìm đến mục **auth-controller** $\rightarrow$ chọn thanh màu xanh lá **`POST /login`**.
  3. Bấm nút **Try it out**.
  4. Tại khung Request body, nhập:
     ```json
     {
       "userName": "admin",
       "password": "123456"
     }
     ```
  5. Bấm nút **Execute** màu xanh dương.
- **Thao tác chụp:** 
  - Kéo màn hình để thấy được: URL `/login`, nút Execute, mã phản hồi `200` và ô Response body có dòng `"message": "Dang nhap thanh cong!"` kèm chuỗi `"token": "eyJhbGci..."`.
  - Copy chuỗi Token này để dùng cho các bước tiếp theo.

---

## 2. Ảnh Minh chứng 2: Phân tích cấu trúc Token trên jwt.io
- **Tên file lưu:** `minhChung2.png`
- **Mục tiêu:** Chứng minh hiểu rõ cấu trúc 3 phần của JWT: Header, Payload, Signature.
- **Các bước thực hiện:**
  1. Mở trình duyệt vào trang web: [https://jwt.io](https://jwt.io)
  2. Tại cột bên trái (**Encoded**): Dán toàn bộ chuỗi token vừa lấy ở bước 1 vào.
  3. Cột bên phải (**Decoded**) sẽ tự động hiển thị:
     - Header: `"alg": "HS256"`
     - Payload: `"sub": "admin"`, ngày tạo và ngày hết hạn.
- **Thao tác chụp:** 
  - Chụp màn hình thấy rõ cả 2 cột: Cột Token đã mã hóa bên trái và cột giải mã Header + Payload bên phải.

---

## 3. Ảnh Minh chứng 3: Xác thực Token qua Header Authorization
Minh chứng này gồm 2 ảnh mô tả quá trình tra chìa khóa và kiểm tra Token:

### Ảnh 3.1: Nhập Token vào nút Authorize của Swagger
- **Tên file lưu:** `minhChung3_1.png`
- **Các bước thực hiện:**
  1. Quay lại trang Swagger UI (`http://localhost:8080/swagger-ui/index.html`).
  2. Bấm vào nút màu xanh lá **Authorize 🔓** (ở góc trên bên phải trang).
  3. Một popup hiện ra, tại ô **Value**, dán chuỗi token vào.
- **Thao tác chụp:** 
  - Chụp lại ô popup này khi đã dán token vào ô Value (trước hoặc ngay sau khi bấm nút Authorize bên trong popup).

### Ảnh 3.2: Thực thi API xác thực GET /auth
- **Tên file lưu:** `minhChung3_2.png`
- **Các bước thực hiện:**
  1. Đóng popup Authorize lại (icon chiếc khóa trên đầu Swagger chuyển sang khóa đóng 🔒).
  2. Kéo xuống mở route **`GET /auth`**.
  3. Bấm **Try it out** $\rightarrow$ bấm **Execute**.
- **Thao tác chụp:** 
  - Chụp khung kết quả Server response trả về mã **`200`** và nội dung: `Token hop le! Xin chao: admin`.

---

## 4. Ảnh Minh chứng 4: Kiểm tra Middleware bảo vệ API Hello World (GET /)
Minh chứng này chứng minh Middleware (`JwtFilter`) hoạt động đúng: chặn người không có token và cho phép người có token.

### Ảnh 4.1: Khi không có Token (Bị Middleware chặn)
- **Tên file lưu:** `minhChung4_1.png`
- **Cách làm nhanh:**
  1. Mở một tab trình duyệt mới hoàn toàn (tab ẩn danh hoặc tab thường).
  2. Nhập thẳng địa chỉ: `http://localhost:8080/` rồi nhấn Enter.
- **Thao tác chụp:** 
  - Chụp màn hình trình duyệt hiển thị rõ thanh địa chỉ `localhost:8080/` và dòng thông báo lỗi:  
    `Loi: Ban can truyen Token hop le qua Authorization Header de xem Hello World!`

### Ảnh 4.2: Khi có Token hợp lệ (Được Middleware cho phép)
- **Tên file lưu:** `minhChung4_2.png`
- **Cách làm qua Terminal:**
  1. Mở một tab Terminal mới trên VS Code (bấm dấu `+` ở khung Terminal).
  2. Gõ lệnh:
     ```powershell
     curl.exe -H "Authorization: Bearer <dán_token_ở_bước_1>" http://localhost:8080/
     ```
  3. Nhấn Enter.
- **Thao tác chụp:** 
  - Chụp cửa sổ Terminal thấy rõ dòng lệnh `curl.exe` kèm Token và kết quả in ra:  
    `Hello World! Spring Boot da chay thanh cong tren VS Code!`

---

## Danh sách file cần có trong thư mục `screenshot/`:
1. `minhChung1.png` (POST /login thành công)
2. `minhChung2.png` (Giải mã trên jwt.io)
3. `minhChung3_1.png` (Popup nhập Token trên Swagger)
4. `minhChung3_2.png` (GET /auth trả về 200 OK)
5. `minhChung4_1.png` (Truy cập / bị chặn thông báo lỗi)
6. `minhChung4_2.png` (Truy cập / có token in ra Hello World)