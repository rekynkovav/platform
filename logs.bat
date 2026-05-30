@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   PLATFORM - ПРОСМОТР ЛОГОВ
echo ========================================
echo.

echo Выберите сервис для просмотра логов:
echo.
echo   1. Backend (основное приложение)
echo   2. PostgreSQL (база данных)
echo   3. Redis (кэш)
echo   4. Keycloak (аутентификация)
echo   5. Все сервисы
echo   6. Выход
echo.

set /p choice="Введите номер (1-6): "

if "%choice%"=="1" goto :backend
if "%choice%"=="2" goto :postgres
if "%choice%"=="3" goto :redis
if "%choice%"=="4" goto :keycloak
if "%choice%"=="5" goto :all
if "%choice%"=="6" goto :exit
echo Неверный выбор
pause
goto :eof

:backend
echo.
echo ===== ЛОГИ BACKEND =====
docker-compose -f docker-compose.yml logs -f --tail=100 backend
goto :eof

:postgres
echo.
echo ===== ЛОГИ POSTGRESQL =====
docker-compose -f docker-compose.yml logs -f --tail=50 postgres
goto :eof

:redis
echo.
echo ===== ЛОГИ REDIS =====
docker-compose -f docker-compose.yml logs -f --tail=50 redis
goto :eof

:keycloak
echo.
echo ===== ЛОГИ KEYCLOAK =====
docker-compose -f docker-compose.yml logs -f --tail=50 keycloak
goto :eof

:all
echo.
echo ===== ЛОГИ ВСЕХ СЕРВИСОВ =====
docker-compose -f docker-compose.yml logs -f --tail=50
goto :eof

:exit
echo Выход...
timeout /t 1 /nobreak >nul