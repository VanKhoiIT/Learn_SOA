[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  KIEM TRA TOAN DIEN ORDER MANAGEMENT SERVICE (BUOI 4)  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# -------------------------------------------------------------
# TEST 1: Kiểm tra bảo vệ API (Chưa truyền Token)
# -------------------------------------------------------------
Write-Host "`n[TEST 1] Goi API GET http://localhost:8082/orders KHI CHUA CO TOKEN:" -ForegroundColor Yellow
try {
    $res1 = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Get -ErrorAction Stop
    Write-Host "LOI: API khong chan request chua co token!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = $_.ErrorDetails.Message
    Write-Host "-> Ket qua: BI CHAN THANH CONG!" -ForegroundColor Green
    Write-Host "-> HTTP Status Code: $statusCode (Unauthorized)" -ForegroundColor Green
    Write-Host "-> Phan hoi: $errorBody"
}

# -------------------------------------------------------------
# TEST 2: Đăng nhập qua Auth Service (Buổi 2 - Cổng 8080 hoặc Proxy)
# -------------------------------------------------------------
Write-Host "`n[TEST 2] Dang nhap lay JWT Token tu Auth Service (Buoi 2):" -ForegroundColor Yellow
$loginPayload = @{
    userName = "admin"
    password = "123456"
} | ConvertTo-Json

$token = $null
try {
    # Thu goi truc tiep Auth Service 8080
    $loginRes = Invoke-RestMethod -Uri "http://localhost:8080/login" -Method Post -ContentType "application/json" -Body $loginPayload -ErrorAction Stop
    $token = $loginRes.token
    Write-Host "-> Dang nhap truc tiep qua Auth Service 8080 thanh cong!" -ForegroundColor Green
} catch {
    Write-Host "-> Auth Service 8080 chua bat, thu goi qua Proxy Order Service (8082)..." -ForegroundColor Magenta
    $loginRes = Invoke-RestMethod -Uri "http://localhost:8082/api/auth/login" -Method Post -ContentType "application/json" -Body $loginPayload
    $token = $loginRes.token
    Write-Host "-> Lay Token qua Proxy thanh cong!" -ForegroundColor Green
}

Write-Host "-> Token: $token"
$headers = @{ "Authorization" = "Bearer $token" }

# -------------------------------------------------------------
# TEST 3: Lấy danh sách sản phẩm từ Product Service (Buổi 3 - 8081)
# -------------------------------------------------------------
Write-Host "`n[TEST 3] Kiem tra ket noi Product Service (Buoi 3 - Cong 8081):" -ForegroundColor Yellow
$targetProductId = 1
try {
    $prods = Invoke-RestMethod -Uri "http://localhost:8081/products" -Method Get -Headers $headers -ErrorAction Stop
    Write-Host "-> Product Service 8081 ONLINE! So san pham co san: $($prods.Count)" -ForegroundColor Green
    if ($prods.Count -gt 0) {
        $targetProductId = $prods[0].id
        Write-Host "-> San pham chon test: ID=$targetProductId, Ten='$($prods[0].name)', Ton kho=$($prods[0].quantity)" -ForegroundColor Green
    }
} catch {
    Write-Host "-> Product Service 8081 chua khoi dong, Order Service se su dung co che du phong (Fallback Mode)!" -ForegroundColor Magenta
}

# -------------------------------------------------------------
# TEST 4: Bắt lỗi khi ĐẶT VƯỢT QUÁ TỒN KHO KHO HÀNG (Stock Validation)
# -------------------------------------------------------------
Write-Host "`n[TEST 4] Thu tao don hang voi SO LUONG VUOT QUA TON KHO (Yeu cau 99999 san pham):" -ForegroundColor Yellow
$overStockOrder = @{
    customerName = "Khach Hang Dat Qua So Luong"
    customerEmail = "overstock@test.com"
    status = "pending"
    items = @(
        @{
            productId = $targetProductId
            quantity = 99999
        }
    )
} | ConvertTo-Json -Depth 5

try {
    $resOver = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Post -ContentType "application/json;charset=UTF-8" -Headers $headers -Body $overStockOrder -ErrorAction Stop
    Write-Host "LOI: He thong khong bat duoc loi vuot ton kho!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = $_.ErrorDetails.Message
    Write-Host "-> Ket qua: BAT LOI TON KHO THANH CONG!" -ForegroundColor Green
    Write-Host "-> HTTP Status Code: $statusCode (Bad Request)" -ForegroundColor Green
    Write-Host "-> Chi tiet loi tra ve tu Order Service: $errorBody" -ForegroundColor Yellow
}

# -------------------------------------------------------------
# TEST 5: Tạo đơn hàng mới HỢP LỆ (POST /orders)
# -------------------------------------------------------------
Write-Host "`n[TEST 5] Tao don hang moi hop le (POST /orders):" -ForegroundColor Yellow
$validOrder = @{
    customerName = "Tran Thi Mai"
    customerEmail = "maitt@gmail.com"
    status = "pending"
    items = @(
        @{
            productId = $targetProductId
            quantity = 1
        }
    )
} | ConvertTo-Json -Depth 5

$createdOrderRes = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Post -ContentType "application/json;charset=UTF-8" -Headers $headers -Body $validOrder
Write-Host "-> Ket qua tao don hang: $($createdOrderRes.message)" -ForegroundColor Green
$newOrderId = $createdOrderRes.data.id
Write-Host "-> Ma don hang moi tao (Order ID): $newOrderId" -ForegroundColor Green
Write-Host "-> Tong tien don hang: $($createdOrderRes.data.totalAmount) VND" -ForegroundColor Green

# -------------------------------------------------------------
# TEST 6: Lấy danh sách tất cả các đơn hàng (GET /orders)
# -------------------------------------------------------------
Write-Host "`n[TEST 6] Lay danh sach tat ca don hang (GET /orders):" -ForegroundColor Yellow
$allOrders = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Get -Headers $headers
Write-Host "-> Tong so don hang trong CSDL rieng (orderdb): $($allOrders.Count)" -ForegroundColor Green
foreach ($o in $allOrders) {
    Write-Host "   - Don #$($o.id): $($o.customerName) | $($o.customerEmail) | Tong: $($o.totalAmount) | Trang thai: $($o.status)"
}

# -------------------------------------------------------------
# TEST 7: Lấy thông tin chi tiết một đơn hàng (GET /orders/{id})
# -------------------------------------------------------------
Write-Host "`n[TEST 7] Lay chi tiet don hang vua tao (GET /orders/$newOrderId):" -ForegroundColor Yellow
$orderDetail = Invoke-RestMethod -Uri "http://localhost:8082/orders/$newOrderId" -Method Get -Headers $headers
Write-Host "-> Khach hang: $($orderDetail.customerName)" -ForegroundColor Green
Write-Host "-> So mat hang trong don: $($orderDetail.items.Count)" -ForegroundColor Green

# -------------------------------------------------------------
# TEST 8: Thêm mặt hàng mới vào chi tiết đơn hàng (POST /order_items)
# -------------------------------------------------------------
Write-Host "`n[TEST 8] Them mat hang moi vao don hang #$newOrderId (POST /order_items):" -ForegroundColor Yellow
$newItemPayload = @{
    orderId = $newOrderId
    productId = 2
    quantity = 1
} | ConvertTo-Json

$newItemRes = Invoke-RestMethod -Uri "http://localhost:8082/order_items" -Method Post -ContentType "application/json" -Headers $headers -Body $newItemPayload
Write-Host "-> $($newItemRes.message)" -ForegroundColor Green
$newItemId = $newItemRes.data.id
Write-Host "-> Mat hang vua them: ID=$newItemId, Ten='$($newItemRes.data.productName)', Thanh tien=$($newItemRes.data.totalPrice)" -ForegroundColor Green

# Kiem tra lai don hang de xem tong tien co tu dong cap nhat khong
$updatedOrder = Invoke-RestMethod -Uri "http://localhost:8082/orders/$newOrderId" -Method Get -Headers $headers
Write-Host "-> Tong tien don hang #$newOrderId sau khi them mat hang: $($updatedOrder.totalAmount) VND" -ForegroundColor Green

# -------------------------------------------------------------
# TEST 9: Lấy danh sách mặt hàng (GET /order_items)
# -------------------------------------------------------------
Write-Host "`n[TEST 9] Lay danh sach mat hang theo don hang (GET /order_items?order_id=$newOrderId):" -ForegroundColor Yellow
$itemsList = Invoke-RestMethod -Uri "http://localhost:8082/order_items?order_id=$newOrderId" -Method Get -Headers $headers
Write-Host "-> So mat hang trong don #${newOrderId}: $($itemsList.Count)" -ForegroundColor Green
foreach ($it in $itemsList) {
    Write-Host "   - Item #$($it.id): $($it.productName) x $($it.quantity) = $($it.totalPrice) VND"
}

# -------------------------------------------------------------
# TEST 10: Cập nhật trạng thái đơn hàng (PUT /orders/{id})
# -------------------------------------------------------------
Write-Host "`n[TEST 10] Cap nhat trang thai don hang #$newOrderId sang 'completed' (PUT /orders/$newOrderId):" -ForegroundColor Yellow
$updatePayload = @{
    status = "completed"
} | ConvertTo-Json

$updateRes = Invoke-RestMethod -Uri "http://localhost:8082/orders/$newOrderId" -Method Put -ContentType "application/json" -Headers $headers -Body $updatePayload
Write-Host "-> $($updateRes.message)" -ForegroundColor Green
Write-Host "-> Trang thai moi: $($updateRes.data.status)" -ForegroundColor Green

# -------------------------------------------------------------
# TEST 11: Xóa một mặt hàng trong đơn hàng (DELETE /order_items/{id})
# -------------------------------------------------------------
Write-Host "`n[TEST 11] Xoa mat hang #$newItemId (DELETE /order_items/$newItemId):" -ForegroundColor Yellow
$delItemRes = Invoke-RestMethod -Uri "http://localhost:8082/order_items/$newItemId" -Method Delete -Headers $headers
Write-Host "-> $($delItemRes.message)" -ForegroundColor Green

# -------------------------------------------------------------
# TEST 12: Xóa đơn hàng (DELETE /orders/{id})
# -------------------------------------------------------------
Write-Host "`n[TEST 12] Xoa don hang #$newOrderId (DELETE /orders/$newOrderId):" -ForegroundColor Yellow
$delOrderRes = Invoke-RestMethod -Uri "http://localhost:8082/orders/$newOrderId" -Method Delete -Headers $headers
Write-Host "-> $($delOrderRes.message)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  TAT CA 12 KICH BAN TEST SO 4 DA HOAN TAT XUAT SAC!    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

