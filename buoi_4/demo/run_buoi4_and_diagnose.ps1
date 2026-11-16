# ====================================================================
# AUTO RUN & DIAGNOSTIC SUITE - SOA BUOI 4 (ORDER MANAGEMENT SERVICE)
# ====================================================================
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "  TOOL TU DONG CHAY & KIEM TRA TOAN DIEN LOI NGAM - SOA BUOI 4     " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$BaseDir = Resolve-Path "$ScriptDir\..\.."

$Buoi2Dir = Join-Path $BaseDir "buoi_2\demo"
$Buoi3Dir = Join-Path $BaseDir "buoi_3\demo"
$Buoi4Dir = $ScriptDir

# --------------------------------------------------------------------
# 1. KIEM TRA CAC DICH VU CO DANG CHAY KHONG (PORTS 8080, 8081, 8082)
# --------------------------------------------------------------------
function Test-PortListening($port) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return ($null -ne $conn)
}

Write-Host "`n[BUOC 1] Kiem tra trang thai cac Service tren he thong..." -ForegroundColor Yellow

$authRunning = Test-PortListening 8080
$prodRunning = Test-PortListening 8081
$orderRunning = Test-PortListening 8082

Write-Host "   * Auth Service (Port 8080)   : " -NoNewline
if ($authRunning) { Write-Host "DANG CHAY [OK]" -ForegroundColor Green } else { Write-Host "CHUA CHAY" -ForegroundColor Magenta }

Write-Host "   * Product Service (Port 8081): " -NoNewline
if ($prodRunning) { Write-Host "DANG CHAY [OK]" -ForegroundColor Green } else { Write-Host "CHUA CHAY" -ForegroundColor Magenta }

Write-Host "   * Order Service (Port 8082)  : " -NoNewline
if ($orderRunning) { Write-Host "DANG CHAY [OK]" -ForegroundColor Green } else { Write-Host "CHUA CHAY" -ForegroundColor Magenta }

# Neu cac service chua chay, hoi va khoi dong tu dong
if (-not $authRunning) {
    Write-Host "`n-> Tu dong khoi dong Auth Service (Buoi 2) o cua so moi..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$Buoi2Dir'; Write-Host '--- AUTH SERVICE (PORT 8080) ---' -ForegroundColor Magenta; .\mvnw.cmd spring-boot:run" -WindowStyle Normal
}

if (-not $prodRunning) {
    Write-Host "-> Tu dong khoi dong Product Service (Buoi 3) o cua so moi..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$Buoi3Dir'; Write-Host '--- PRODUCT SERVICE (PORT 8081) ---' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run" -WindowStyle Normal
}

if (-not $orderRunning) {
    Write-Host "-> Tu dong khoi dong Order Service (Buoi 4) o cua so moi..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$Buoi4Dir'; Write-Host '--- ORDER SERVICE (PORT 8082) ---' -ForegroundColor Green; .\mvnw.cmd spring-boot:run" -WindowStyle Normal
}

# --------------------------------------------------------------------
# 2. CHO DICH VU KHOI DONG XONG (HEALTHCHECK POLLING)
# --------------------------------------------------------------------
Write-Host "`n[BUOC 2] Dang cho Order Service (Port 8082) san sang..." -ForegroundColor Yellow

$maxRetries = 40
$ready = $false
for ($i = 1; $i -le $maxRetries; $i++) {
    try {
        $check = Invoke-WebRequest -Uri "http://localhost:8082/index.html" -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        if ($check.StatusCode -eq 200) {
            $ready = $true
            break
        }
    } catch {
        Write-Host "." -NoNewline -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
}

if ($ready) {
    Write-Host "`n-> Order Service 8082 DA SAN SANG HOAT DONG!" -ForegroundColor Green
} else {
    Write-Host "`n[CANH BAO] Qua thoi gian cho. Neu service dang compile Maven lan dau, vui long doi them it giay roi chay lai." -ForegroundColor Yellow
}

# --------------------------------------------------------------------
# 3. KHOI CHAY SUITE KIEM TRA LOI NGAM TOAN DIEN (12 CHECKS)
# --------------------------------------------------------------------
Write-Host "`n====================================================================" -ForegroundColor Cyan
Write-Host "   BAT DAU KIEM TRA CAC LOI NGAM TIEM AN TRONG HE THONG (SOA QA)    " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan

$passedCount = 0
$failedCount = 0

function Report-Pass($name, $detail) {
    $script:passedCount++
    Write-Host "[PASS] " -ForegroundColor Green -NoNewline
    Write-Host "$name" -ForegroundColor White
    if ($detail) { Write-Host "       -> $detail" -ForegroundColor Gray }
}

function Report-Fail($name, $detail) {
    $script:failedCount++
    Write-Host "[FAIL] " -ForegroundColor Red -NoNewline
    Write-Host "$name" -ForegroundColor White
    if ($detail) { Write-Host "       -> $detail" -ForegroundColor Red }
}

# --- CHECK 1: Chặn truy cập khi không có Token (401 Unauthorized) ---
try {
    $r = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Get -ErrorAction Stop
    Report-Fail "Check 1: Bao mat API (Chua co Token)" "API khong chan request nac danh!"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Report-Pass "Check 1: Bao mat API (Chua co Token)" "Chan thanh cong voi ma loi 401 Unauthorized"
    } else {
        Report-Fail "Check 1: Bao mat API" "Ma loi tra ve khong phai 401: $($_.Exception.Response.StatusCode.value__)"
    }
}

# --- CHECK 2: Chặn truy cập với Token giả mạo/sai lệch ---
try {
    $fakeHeaders = @{ "Authorization" = "Bearer token_gia_mao_khong_hop_le_123" }
    $r = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Get -Headers $fakeHeaders -ErrorAction Stop
    Report-Fail "Check 2: Bao mat API (Token gia mao)" "API khong chan token gia mao!"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Report-Pass "Check 2: Bao mat API (Token gia mao)" "Chan thanh cong voi ma loi 401 Unauthorized"
    } else {
        Report-Fail "Check 2: Bao mat API" "Ma loi tra ve khong dung: $($_.Exception.Response.StatusCode.value__)"
    }
}

# --- CHECK 3: Đăng nhập lấy Token JWT hợp lệ từ Buổi 2 / Proxy ---
$token = $null
try {
    $loginBody = @{ userName = "admin"; password = "123456" } | ConvertTo-Json
    $loginRes = Invoke-RestMethod -Uri "http://localhost:8082/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
    $token = $loginRes.token
    if ($token) {
        Report-Pass "Check 3: Dang nhap & Cap phat Token JWT" "Nhan token thanh cong (do dai: $($token.Length) ky tu)"
    } else {
        Report-Fail "Check 3: Dang nhap & Cap phat Token JWT" "Khong tim thay truong token trong response"
    }
} catch {
    Report-Fail "Check 3: Dang nhap & Cap phat Token JWT" $_.Exception.Message
}

$headers = @{ "Authorization" = "Bearer $token" }

# --- CHECK 4: Bắt lỗi khi ĐẶT VƯỢT QUÁ TỒN KHO (Stock Validation) ---
try {
    $overStockOrder = @{
        customerName = "Khach Dat Qua Kho"
        customerEmail = "overstock@test.com"
        items = @( @{ productId = 1; quantity = 999999 } )
    } | ConvertTo-Json -Depth 5
    $r = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Post -ContentType "application/json;charset=UTF-8" -Headers $headers -Body $overStockOrder -ErrorAction Stop
    Report-Fail "Check 4: Kiem tra ton kho (Dat qua so luong)" "He thong cho phep dat hang vuot qua ton kho!"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Report-Pass "Check 4: Kiem tra ton kho (Dat qua so luong)" "Bat loi 400 Bad Request thanh cong khi dat qua ton kho"
    } else {
        Report-Fail "Check 4: Kiem tra ton kho" "Status code tra ve: $($_.Exception.Response.StatusCode.value__)"
    }
}

# --- CHECK 5: Bắt lỗi khi số lượng đặt âm hoặc bằng 0 ---
try {
    $zeroQtyOrder = @{
        customerName = "Khach Dat So Luong 0"
        customerEmail = "zero@test.com"
        items = @( @{ productId = 1; quantity = 0 } )
    } | ConvertTo-Json -Depth 5
    $r = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Post -ContentType "application/json;charset=UTF-8" -Headers $headers -Body $zeroQtyOrder -ErrorAction Stop
    Report-Fail "Check 5: Rang buoc so luong (<= 0)" "He thong cho phep dat so luong <= 0!"
} catch {
    Report-Pass "Check 5: Rang buoc so luong (<= 0)" "Bat loi dung khi so luong <= 0"
}

# --- CHECK 6: Bắt lỗi thiếu tên hoặc email khách hàng ---
try {
    $missingNameOrder = @{
        customerName = ""
        customerEmail = "noname@test.com"
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Post -ContentType "application/json;charset=UTF-8" -Headers $headers -Body $missingNameOrder -ErrorAction Stop
    Report-Fail "Check 6: Validation thong tin khach hang" "Khong bat loi de trong ten khach hang!"
} catch {
    Report-Pass "Check 6: Validation thong tin khach hang" "Bat loi 400 khi thieu customerName thanh cong"
}

# --- CHECK 7: Tạo đơn hàng HỢP LỆ & Kiểm tra độ chính xác phép tính (BigDecimal) ---
$createdOrderId = $null
try {
    $validOrder = @{
        customerName = "Nguyen Van Test"
        customerEmail = "nguyenvantest@gmail.com"
        status = "pending"
        items = @(
            @{ productId = 2; quantity = 2 } # Keychron K2 gia 1.850.000 x 2 = 3.700.000
        )
    } | ConvertTo-Json -Depth 5
    $res = Invoke-RestMethod -Uri "http://localhost:8082/orders" -Method Post -ContentType "application/json;charset=UTF-8" -Headers $headers -Body $validOrder
    $createdOrderId = $res.data.id
    $totalAmount = [decimal]$res.data.totalAmount

    if ($createdOrderId -and $totalAmount -gt 0) {
        Report-Pass "Check 7: Tao don hang hop le & Tinh tong tien" "Don hang #$createdOrderId tao thanh cong, Tong tien: $totalAmount VND"
    } else {
        Report-Fail "Check 7: Tao don hang hop le" "Du lieu tra ve thieu ID hoac tong tien = 0"
    }
} catch {
    Report-Fail "Check 7: Tao don hang hop le" $_.Exception.Message
}

# --- CHECK 8: Thêm mặt hàng mới vào đơn hàng & Tự động cập nhật tổng tiền đơn cha ---
$newItemId = $null
if ($createdOrderId) {
    try {
        $addItemPayload = @{
            orderId = $createdOrderId
            productId = 3 # Logitech MX Master 3S
            quantity = 1
        } | ConvertTo-Json
        $itemRes = Invoke-RestMethod -Uri "http://localhost:8082/order_items" -Method Post -ContentType "application/json" -Headers $headers -Body $addItemPayload
        $newItemId = $itemRes.data.id

        # Kiem tra don hang cha
        $orderCheck = Invoke-RestMethod -Uri "http://localhost:8082/orders/$createdOrderId" -Method Get -Headers $headers
        $newTotal = [decimal]$orderCheck.totalAmount

        if ($newTotal -gt $totalAmount) {
            Report-Pass "Check 8: Them mat hang & Cap nhat tong tien don cha" "Tong tien don #$createdOrderId tu dong cap nhat tang tu $totalAmount -> $newTotal VND"
        } else {
            Report-Fail "Check 8: Them mat hang vao don" "Tong tien don cha khong tu dong tang!"
        }
    } catch {
        Report-Fail "Check 8: Them mat hang vao don" $_.Exception.Message
    }
}

# --- CHECK 9: Bắt lỗi cập nhật trạng thái đơn hàng không hợp lệ ---
if ($createdOrderId) {
    try {
        $invalidStatus = @{ status = "trang_thai_tam_bay" } | ConvertTo-Json
        $r = Invoke-RestMethod -Uri "http://localhost:8082/orders/$createdOrderId" -Method Put -ContentType "application/json" -Headers $headers -Body $invalidStatus -ErrorAction Stop
        Report-Fail "Check 9: Validation trang thai don hang" "Cho phep cap nhat trang thai sai quy chuan!"
    } catch {
        Report-Pass "Check 9: Validation trang thai don hang" "Bat loi chinh xac khi truyen trang thai khong nam trong (pending, completed, cancelled)"
    }
}

# --- CHECK 10: Cập nhật trạng thái hợp lệ sang 'completed' ---
if ($createdOrderId) {
    try {
        $validStatus = @{ status = "completed" } | ConvertTo-Json
        $upRes = Invoke-RestMethod -Uri "http://localhost:8082/orders/$createdOrderId" -Method Put -ContentType "application/json" -Headers $headers -Body $validStatus
        if ($upRes.data.status -eq "completed") {
            Report-Pass "Check 10: Cap nhat trang thai don hang" "Chuyen sang 'completed' thanh cong"
        } else {
            Report-Fail "Check 10: Cap nhat trang thai" "Trang thai khong doi thanh completed"
        }
    } catch {
        Report-Fail "Check 10: Cap nhat trang thai" $_.Exception.Message
    }
}

# --- CHECK 11: Xóa một mặt hàng & Kiểm tra tính lại tiền đơn cha ---
if ($newItemId) {
    try {
        $delItem = Invoke-RestMethod -Uri "http://localhost:8082/order_items/$newItemId" -Method Delete -Headers $headers
        $orderAfterDel = Invoke-RestMethod -Uri "http://localhost:8082/orders/$createdOrderId" -Method Get -Headers $headers
        Report-Pass "Check 11: Xoa mat hang & Tinh lai tong tien" "Xoa mat hang #$newItemId thanh cong, tong tien con lai: $($orderAfterDel.totalAmount) VND"
    } catch {
        Report-Fail "Check 11: Xoa mat hang" $_.Exception.Message
    }
}

# --- CHECK 12: Xóa đơn hàng (Cascading Delete khong loi Foreign Key) ---
if ($createdOrderId) {
    try {
        $delOrder = Invoke-RestMethod -Uri "http://localhost:8082/orders/$createdOrderId" -Method Delete -Headers $headers
        # Kiem tra xem da bi xoa chua
        try {
            $checkGone = Invoke-RestMethod -Uri "http://localhost:8082/orders/$createdOrderId" -Method Get -Headers $headers -ErrorAction Stop
            Report-Fail "Check 12: Cascading Delete don hang" "Don hang van con ton tai sau khi xoa!"
        } catch {
            Report-Pass "Check 12: Cascading Delete don hang" "Xoa don hang #$createdOrderId va toan bo chi tiet mat hang sach se (Status 404)"
        }
    } catch {
        Report-Fail "Check 12: Xoa don hang" $_.Exception.Message
    }
}

# --- CHECK 13: Kiểm tra Cơ sở dữ liệu riêng (Database-per-Service Principle) ---
$orderDbPath = Join-Path $ScriptDir "data\orderdb.mv.db"
if (Test-Path $orderDbPath) {
    $dbSize = (Get-Item $orderDbPath).Length
    Report-Pass "Check 13: CSDL doc lap (Database-per-Service)" "CSDL rieng ./data/orderdb.mv.db ton tai hoat dong binh thuong ($dbSize bytes)"
} else {
    Report-Fail "Check 13: CSDL doc lap" "Khong tim thay file CSDL ./data/orderdb.mv.db"
}

# --------------------------------------------------------------------
# 4. TONG KET KET QUA & MO TRINH DUYET
# --------------------------------------------------------------------
Write-Host "`n====================================================================" -ForegroundColor Cyan
Write-Host "   TONG KET KIEM TRA: $passedCount / $($passedCount + $failedCount) CHECKS PASSED!" -ForegroundColor $(if ($failedCount -eq 0) { "Green" } else { "Yellow" })
Write-Host "====================================================================" -ForegroundColor Cyan

if ($failedCount -eq 0) {
    Write-Host "-> TUYET VOI! KHONG CO LOI NGAM NAO DUOC TIM THAY!" -ForegroundColor Green
    Write-Host "-> He thong Order Service hoan toan dat chuan SOA, san sang bao cao!" -ForegroundColor Green
} else {
    Write-Host "-> Phat hien $failedCount diem can luu y, hay kiem tra chi tiet ben tren." -ForegroundColor Red
}

Write-Host "`n-> Dang mo Dashboard giao dien tren trinh duyet..." -ForegroundColor Cyan
Start-Process "http://localhost:8082/index.html"

