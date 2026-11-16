@echo off
chcp 65001 > nul
title SOA Buổi 4 - Tự động Chạy & Kiểm tra Lỗi Ngầm
echo ====================================================================
echo   KHỞI ĐỘNG VÀ KIỂM TRA TOÀN DIỆN HỆ THỐNG SOA BUỔI 4
echo ====================================================================
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "run_buoi4_and_diagnose.ps1"
echo.
pause

