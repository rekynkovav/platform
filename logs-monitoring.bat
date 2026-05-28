@echo off
chcp 65001 >nul
title Platform Project - Monitoring Logs
color 0E

echo ========================================
echo    Platform Project - Monitoring Logs
echo ========================================
echo.

echo Просмотр логов Prometheus и Grafana...
echo Для выхода нажмите Ctrl+C
echo.

docker-compose logs -f prometheus grafana

echo.
echo ========================================
echo    ЛОГИ ЗАВЕРШЕНЫ
echo ========================================
echo.
pause