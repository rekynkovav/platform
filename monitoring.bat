@echo off
chcp 65001 >nul
title Platform Project - Monitoring
color 0A

echo ========================================
echo    Platform Project - Monitoring
echo ========================================
echo.

REM Проверка Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker не найден.
    pause
    exit /b 1
)

REM Запуск только мониторинга
echo Запуск Prometheus и Grafana...
docker-compose up -d prometheus grafana

echo.
echo ========================================
echo    МОНИТОРИНГ ЗАПУЩЕН!
echo ========================================
echo.
echo Prometheus: http://localhost:9090
echo Grafana:    http://localhost:3001 (admin/admin)
echo.
pause