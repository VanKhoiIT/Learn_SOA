@echo off
chcp 65001 > nul
title SOA - Khởi chạy 3 Dịch vụ (Auth 8080, Product 8081, Order 8082)
echo ====================================================================
echo   KHỞI ĐỘNG CẢ 3 DỊCH VỤ TRONG KIẾN TRÚC SOA
echo ====================================================================

set ROOT_DIR=%~dp0..\..

echo [1/3] Khởi động Auth Service (Buổi 2 - Cổng 8080)...
start "Auth Service (Port 8080)" cmd /k "cd /d "%ROOT_DIR%\buoi_2\demo" && mvnw.cmd spring-boot:run"

echo [2/3] Khởi động Product Service (Buổi 3 - Cổng 8081)...
start "Product Service (Port 8081)" cmd /k "cd /d "%ROOT_DIR%\buoi_3\demo" && mvnw.cmd spring-boot:run"

echo [3/3] Khởi động Order Service (Buổi 4 - Cổng 8082)...
start "Order Service (Port 8082)" cmd /k "cd /d "%~dp0" && mvnw.cmd spring-boot:run"

echo.
echo Cả 3 dịch vụ đang được khởi động ở 3 cửa sổ riêng biệt.
echo Sau khi các dịch vụ khởi động xong, hãy chạy file start_and_check.bat để kiểm tra lỗi ngầm!
echo.
pause

