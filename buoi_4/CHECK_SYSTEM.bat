@echo off
chcp 65001 > nul
title SOA Buổi 4 - Tự động Chạy & Kiểm tra Lỗi Ngầm
cd /d "%~dp0demo"
powershell -NoProfile -ExecutionPolicy Bypass -File "run_buoi4_and_diagnose.ps1"
echo.
pause

