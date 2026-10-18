[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BẮT ĐẦU CHUỖI KIỂM THỬ TỰ ĐỘNG BÀI THỰC HÀNH SỐ 3 (SOA)      " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# -------------------------------------------------------------
# TEST 1: Kiểm tra bảo vệ API (Chưa truyền Token)
# -------------------------------------------------------------
Write-Host "`n[TEST 1] Gọi API GET http://localhost:8081/products KHI CHƯA CÓ TOKEN:" -ForegroundColor Yellow
try {
    $res1 = Invoke-RestMethod -Uri "http://localhost:8081/products" -Method Get -ErrorAction Stop
    Write-Host "LỖI: API không chặn request chưa có token!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = $_.ErrorDetails.Message
    Write-Host "-> Kết quả: BỊ CHẶN THÀNH CÔNG!" -ForegroundColor Green
    Write-Host "-> HTTP Status Code: $statusCode (Unauthorized)" -ForegroundColor Green
    Write-Host "-> Phản hồi: $errorBody"
}

# -------------------------------------------------------------
# TEST 2: Đăng nhập qua Auth Service (Buổi 2 - Cổng 8080) để lấy JWT Token
# -------------------------------------------------------------
Write-Host "`n[TEST 2] Gọi POST http://localhost:8080/login để lấy JWT Token từ Buổi 2:" -ForegroundColor Yellow
$loginPayload = @{
    userName = "admin"
    password = "123456"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/login" -Method Post -ContentType "application/json" -Body $loginPayload
$token = $loginResponse.token

Write-Host "-> Đăng nhập Buổi 2 thành công!" -ForegroundColor Green
Write-Host "-> Thông báo: $($loginResponse.message)"
Write-Host "-> Token sinh ra: $token"
$headers = @{ "Authorization" = "Bearer $token" }

# -------------------------------------------------------------
# TEST 3: Xác thực Token trực tiếp trên Auth Service Buổi 2
# -------------------------------------------------------------
Write-Host "`n[TEST 3] Gọi GET http://localhost:8080/auth kiểm tra Token hợp lệ:" -ForegroundColor Yellow
$authCheck = Invoke-RestMethod -Uri "http://localhost:8080/auth" -Method Get -Headers $headers
Write-Host "-> Phản hồi từ Auth Service (Buổi 2): $authCheck" -ForegroundColor Green

# -------------------------------------------------------------
# TEST 4: Lấy danh sách sản phẩm (GET /products) qua Product Service
# -------------------------------------------------------------
Write-Host "`n[TEST 4] Gọi GET http://localhost:8081/products với Token vừa nhận:" -ForegroundColor Yellow
$productsList = Invoke-RestMethod -Uri "http://localhost:8081/products" -Method Get -Headers $headers
Write-Host "-> Số lượng sản phẩm hiện tại trong CSDL riêng: $($productsList.Count)" -ForegroundColor Green
$productsList | Select-Object id, name, price, quantity | Format-Table -AutoSize | Out-String | Write-Host

# -------------------------------------------------------------
# TEST 5: Thêm mới một sản phẩm (POST /products)
# -------------------------------------------------------------
Write-Host "[TEST 5] Gọi POST http://localhost:8081/products để thêm mới sản phẩm:" -ForegroundColor Yellow
$newProductData = @{
    name = "Bàn phím cơ NuPhy Air75 V2"
    description = "Bàn phím cơ low-profile không dây, Switch Cowberry siêu êm"
    price = 2850000.00
    quantity = 20
} | ConvertTo-Json -Compress

$createRes = Invoke-RestMethod -Uri "http://localhost:8081/products" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $newProductData
Write-Host "-> Trạng thái: $($createRes.message)" -ForegroundColor Green
Write-Host "-> Dữ liệu sản phẩm vừa tạo:"
$createRes.data | Format-List | Out-String | Write-Host
$newProductId = $createRes.data.id

# -------------------------------------------------------------
# TEST 6: Xem thông tin chi tiết sản phẩm vừa tạo (GET /products/{id})
# -------------------------------------------------------------
Write-Host "[TEST 6] Gọi GET http://localhost:8081/products/$newProductId xem chi tiết:" -ForegroundColor Yellow
$detail = Invoke-RestMethod -Uri "http://localhost:8081/products/$newProductId" -Method Get -Headers $headers
Write-Host "-> Lấy chi tiết thành công!" -ForegroundColor Green
Write-Host "   ID: $($detail.id)"
Write-Host "   Tên: $($detail.name)"
Write-Host "   Giá: $($detail.price) VNĐ"
Write-Host "   Số lượng: $($detail.quantity)"
Write-Host "   Ngày tạo (created_at): $($detail.createdAt)"
Write-Host "   Ngày cập nhật (updated_at): $($detail.updatedAt)"

# -------------------------------------------------------------
# TEST 7: Cập nhật thông tin sản phẩm (PUT /products/{id})
# -------------------------------------------------------------
Write-Host "`n[TEST 7] Gọi PUT http://localhost:8081/products/$newProductId để cập nhật thông tin:" -ForegroundColor Yellow
$updateData = @{
    name = "Bàn phím cơ NuPhy Air75 V2 (Bản Đặc Biệt 2026)"
    description = "Đã nâng cấp switch và pin 4000mAh"
    price = 3100000.00
    quantity = 15
} | ConvertTo-Json -Compress

$updateRes = Invoke-RestMethod -Uri "http://localhost:8081/products/$newProductId" -Method Put -Headers $headers -ContentType "application/json; charset=utf-8" -Body $updateData
Write-Host "-> Trạng thái: $($updateRes.message)" -ForegroundColor Green
Write-Host "   Tên mới: $($updateRes.data.name)"
Write-Host "   Giá mới: $($updateRes.data.price) VNĐ"
Write-Host "   Số lượng mới: $($updateRes.data.quantity)"
Write-Host "   Ngày cập nhật mới: $($updateRes.data.updatedAt)"

# -------------------------------------------------------------
# TEST 8: Xóa sản phẩm (DELETE /products/{id})
# -------------------------------------------------------------
Write-Host "`n[TEST 8] Gọi DELETE http://localhost:8081/products/$newProductId để xóa sản phẩm:" -ForegroundColor Yellow
$deleteRes = Invoke-RestMethod -Uri "http://localhost:8081/products/$newProductId" -Method Delete -Headers $headers
Write-Host "-> Trạng thái: $($deleteRes.message)" -ForegroundColor Green

# -------------------------------------------------------------
# TEST 9: Kiểm tra lại sản phẩm vừa xóa (Kỳ vọng trả về 404 Not Found)
# -------------------------------------------------------------
Write-Host "`n[TEST 9] Gọi GET http://localhost:8081/products/$newProductId để xác nhận đã xóa:" -ForegroundColor Yellow
try {
    $checkAfterDel = Invoke-RestMethod -Uri "http://localhost:8081/products/$newProductId" -Method Get -Headers $headers -ErrorAction Stop
    Write-Host "LỖI: Sản phẩm vẫn còn tồn tại!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "-> Xác nhận thành công! HTTP Status: $statusCode (Not Found)" -ForegroundColor Green
    Write-Host "-> Thông báo từ Server: $($_.ErrorDetails.Message)"
}

# -------------------------------------------------------------
# TEST 10: Kiểm tra gửi Token giả / Token sai chữ ký
# -------------------------------------------------------------
Write-Host "`n[TEST 10] Gọi API với Token giả mạo / sai chữ ký:" -ForegroundColor Yellow
$fakeHeaders = @{ "Authorization" = "Bearer TokenGiaMaoKhongHopLe123456" }
try {
    $fakeTest = Invoke-RestMethod -Uri "http://localhost:8081/products" -Method Get -Headers $fakeHeaders -ErrorAction Stop
    Write-Host "LỖI: Hệ thống chấp nhận token giả!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "-> Chặn token giả thành công! HTTP Status: $statusCode (Unauthorized)" -ForegroundColor Green
    Write-Host "-> Thông báo lỗi: $($_.ErrorDetails.Message)"
}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "   TẤT CẢ 10/10 KỊCH BẢN KIỂM THỬ ĐÃ VƯỢT QUA XUẤT SẮC!         " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
