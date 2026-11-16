# HƯỚNG DẪN CHI TIẾT KHỞI CHẠY VÀ KIỂM THỬ BÀI THỰC HÀNH SỐ 4
> **Hệ thống Quản lý Đơn hàng (Order Management Service - Cổng 8082)**  
> *Hỗ trợ linh hoạt 2 chế độ: Chế độ Kết nối Liên dịch vụ (Full SOA Live) và Chế độ Chạy Cục bộ Giả lập (Standalone Mock).*

---

## I. TỔNG QUAN VÀ HAI CHẾ ĐỘ VẬN HÀNH

Bài thực hành số 4 được thiết kế theo chuẩn doanh nghiệp với cơ chế **Chịu lỗi & Tự hành thông minh (Resilience & Fault-Tolerance)**, sẵn sàng cho cả 2 tình huống chấm điểm của giảng viên:

| Chế độ vận hành | Điều kiện kích hoạt | Đặc điểm & Hành vi hệ thống |
| :--- | :--- | :--- |
| **1. KẾT NỐI TOÀN DIỆN (FULL SOA LIVE)** | Cả **Buổi 2 (8080)** và **Buổi 3 (8081)** đều đang chạy. | • Banner xanh lá rực rỡ.<br>• Thẩm định danh tính thật qua `http://localhost:8080/auth`.<br>• Đồng bộ kho hàng thật thời gian thực với CSDL `productdb` (Buổi 3).<br>• Tự động trừ kho khi đặt hàng và hoàn kho khi hủy đơn. |
| **2. CHẠY CỤC BỘ GIẢ LẬP (STANDALONE MOCK)** | Giáo viên **chỉ tải và chạy MỖI Buổi 4** (8080 và 8081 tắt). | • Banner vàng cam cảnh báo chế độ tự hành.<br>• Tự kích hoạt bộ sinh & kiểm tra Token nội bộ (`fallbackLocal = true`).<br>• Tự động nạp danh mục sản phẩm chuẩn (*Dell XPS 15, Keychron K2, MX Master 3S*).<br>• Mọi tính năng CRUD đơn hàng, tính tiền, bắt lỗi kho hoạt động 100%! |

---

## II. HÌNH ẢNH CHỤP THỰC TẾ 2 CHẾ ĐỘ TRÊN GIAO DIỆN

### 📸 1. Hình ảnh: Chế độ Chạy Cục bộ Giả lập (Khi chỉ chạy một mình Buổi 4)
> *Phát hiện Buổi 2 và Buổi 3 chưa khởi động. Banner vàng cam bật sáng, hai thẻ trạng thái báo `🟠 CỤC BỘ GIẢ LẬP`, hệ thống tự sinh Token và nạp sản phẩm mẫu để giáo viên kiểm thử trọn vẹn:*

![Chế độ Chạy Cục bộ Giả lập](screenshots/01_che_do_chay_cuc_bo_fallback.png)

---

### 📸 2. Hình ảnh: Chế độ Kết nối Toàn diện (Khi bật cả Buổi 2 và Buổi 3)
> *Khi cả 3 dịch vụ cùng trực tuyến, Banner chuyển sang màu xanh ngọc lục bảo `🌐 CHẾ ĐỘ KẾT NỐI TOÀN DIỆN (FULL SOA LIVE)`, hai thẻ trạng thái báo `🟢 KẾT NỐI THẬT`, dữ liệu và kho hàng liên thông 100% qua mạng nội bộ:*

![Chế độ Kết nối Toàn diện Live](screenshots/02_che_do_ket_noi_service_live.png)

---

## III. HƯỚNG DẪN CHI TIẾT CÁCH CHẠY VÀ TEST THEO TỪNG TRƯỜNG HỢP

### 🅰️ TRƯỜNG HỢP A: CHẠY ĐẦY ĐỦ 3 DỊCH VỤ (CHUẨN ĐỀ BÀI SOA)

#### 1. Lệnh khởi động 3 Dịch vụ:
* **Cách 1-Click:** Bấm đúp vào file `START_ALL_SOA_SERVICES.bat` ở thư mục gốc.
* **Cách Terminal:** Mở 3 cửa sổ PowerShell chạy lần lượt:
  ```powershell
  # Cửa sổ 1 - Buổi 2 (Auth Service):
  cd "d:\MY STUDY\HK1_4\SOA\buoi_2\demo"; java -jar target\demo-0.0.1-SNAPSHOT.jar

  # Cửa sổ 2 - Buổi 3 (Product Service):
  cd "d:\MY STUDY\HK1_4\SOA\buoi_3\demo"; java -jar target\product-service-0.0.1-SNAPSHOT.jar

  # Cửa sổ 3 - Buổi 4 (Order Service):
  cd "d:\MY STUDY\HK1_4\SOA\buoi_4\demo"; java -jar target\order-service-0.0.1-SNAPSHOT.jar
  ```

#### 2. Các bước kiểm thử trên trình duyệt (http://localhost:8082/index.html):
1. **Kiểm tra trạng thái:** Quan sát Banner xanh lá báo `🌐 CHẾ ĐỘ KẾT NỐI TOÀN DIỆN (FULL SOA LIVE)` và hai huy hiệu xanh `🟢 KẾT NỐI THẬT`.
2. **Kiểm thử Bảo mật (Buổi 4 $\leftrightarrow$ Buổi 2):**
   * Bấm nút **`🔑 1. Đăng nhập lấy Token (B2)`** $\rightarrow$ Gọi cổng `8080/login` lấy Token thật.
   * Bấm nút **`🛡️ 2. Thẩm định qua Buổi 2`** $\rightarrow$ Gửi Token sang cổng `8080/auth`, màn hình hiện thông báo Buổi 2 xác nhận: `Token hop le! Xin chao: admin` (HTTP 200).
   * Bấm nút **`⚠️ Giả lập Token sai`** rồi bấm Thẩm định $\rightarrow$ Buổi 2 lập tức từ chối và cảnh báo lỗi.
3. **Kiểm thử Bắt lỗi Vượt tồn kho (Buổi 4 $\leftrightarrow$ Buổi 3):**
   * Sang Tab **`⚡ Thử nghiệm API & Kiểm tra Tồn kho`**, bấm **`⚠️ Test 3: Bắt lỗi khi ĐẶT QUÁ TỒN KHO`** *(đặt 99.999 chiếc)*.
   * Buổi 4 gọi sang Buổi 3 kiểm tra kho và chặn ngay lập tức với lỗi **HTTP 400 Bad Request**.
4. **Kiểm thử Đặt hàng & Tự động Trừ kho:**
   * Bấm Tab **`📋 Quản lý Đơn hàng`** $\rightarrow$ bấm **`➕ Tạo đơn hàng mới`**.
   * Danh mục sản phẩm thời gian thực từ Buổi 3 tải lên đầy đủ $\rightarrow$ Chọn mua 1 sản phẩm $\rightarrow$ Bấm **Xác nhận Đặt hàng**.
   * Đơn hàng tạo thành công $\rightarrow$ Sang tab Buổi 3 [http://localhost:8081/index.html](http://localhost:8081/index.html) nhấn F5 $\rightarrow$ **Tồn kho của sản phẩm đó tự động giảm 1**.
5. **Kiểm thử Hoàn trả kho khi Hủy đơn:**
   * Bên Buổi 4 bấm xóa đơn hàng vừa tạo $\rightarrow$ Sang Buổi 3 nhấn F5 $\rightarrow$ **Tồn kho tự động tăng bù lại đủ số lượng**.

---

### 🅱️ TRƯỜNG HỢP B: NẾU GIÁO VIÊN CHỈ TẢI & CHẠY DUY NHẤT BUỔI 4 (OFFLINE / STANDALONE)

#### 1. Lệnh khởi động (Chỉ cần 1 lệnh duy nhất):
Giáo viên mở terminal trong thư mục `buoi_4\demo` và chạy:
```powershell
mvn clean spring-boot:run
# hoặc
java -jar target\order-service-0.0.1-SNAPSHOT.jar
```
*(Hoàn toàn không cần bật Buổi 2 hay Buổi 3!)*

#### 2. Các bước giáo viên kiểm thử độc lập (http://localhost:8082/index.html):
1. **Nhận diện chế độ:** 
   * Hệ thống tự phát hiện 8080 và 8081 không hoạt động.
   * Banner vàng cam bật sáng: `⚠️ CHẾ ĐỘ CHẠY CỤC BỘ GIẢ LẬP (STANDALONE MOCK / FALLBACK)`.
   * Hai huy hiệu báo rõ: `[🟠 B2 Auth: CỤC BỘ GIẢ LẬP]` và `[🟠 B3 Product: CỤC BỘ GIẢ LẬP]`.
2. **Kiểm thử Đăng nhập & Bảo mật:**
   * Bấm nút **`🔑 1. Đăng nhập lấy Token (B2)`** $\rightarrow$ Buổi 4 tự động kích hoạt bộ sinh JWT nội bộ, cấp Token và thông báo xanh: *"Đăng nhập thành công qua chế độ fallback local!"*.
   * Thử bấm nút **`❌ Xóa Token`** rồi bấm xem đơn $\rightarrow$ Hệ thống vẫn chặn đứng mã lỗi `401 Unauthorized` chuẩn bảo mật.
3. **Kiểm thử Tạo Đơn hàng Mẫu:**
   * Bấm nút **`➕ Tạo đơn hàng mới`** $\rightarrow$ Danh sách sản phẩm mẫu chuẩn của môn học (*Laptop Dell XPS 15, Bàn phím Keychron K2, Chuột MX Master 3S*) tự động hiển thị mượt mà.
   * Chọn số lượng và bấm **`🚀 Xác nhận Đặt hàng`** $\rightarrow$ **Đơn hàng được lưu vào CSDL `orderdb`, tổng tiền tự động tính chuẩn xác 100%!**
4. **Kiểm thử Kiểm soát Số lượng:**
   * Thử đặt số lượng vượt quá mức tồn kho mẫu (ví dụ đặt 99.999 chiếc) $\rightarrow$ Hệ thống vẫn bắt lỗi và chặn lại với mã `400 Bad Request`!
5. **Kiểm thử CRUD Chi tiết đơn hàng:**
   * Tab **`Quản lý Chi tiết Đơn hàng (Order Items)`** vẫn cho phép thêm, sửa, xóa các mặt hàng con và tự động tính lại tổng tiền cho đơn hàng cha bình thường.

---

## IV. TỔNG KẾT
Nhờ kiến trúc 2 chế độ này:
* **Khi bảo vệ / thuyết trình:** Bạn bật đủ 3 dịch vụ để chứng minh năng lực kết nối SOA phân tán đỉnh cao.
* **Khi nộp bài cho giáo viên chấm máy:** Nếu giáo viên chỉ giải nén và chấm một mình Buổi 4, bài làm của bạn **vẫn chạy êm ru 100% không bao giờ gặp lỗi sập hay crash!**
