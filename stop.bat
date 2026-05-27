@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   PLATFORM - ОСТАНОВКА СИСТЕМЫ
echo ========================================
echo.

echo [1/3] Остановка всех сервисов...
docker-compose -f docker-compose.yml down

echo [2/3] Очистка временных контейнеров...
docker container prune -f >nul 2>&1

echo [3/3] Очистка временных образов...
docker image prune -f >nul 2>&1

echo.
echo ========================================
echo   СИСТЕМА ОСТАНОВЛЕНА
echo ========================================
echo.
echo Все сервисы остановлены и очищены
echo.
echo Для запуска: start.bat
echo.
pause