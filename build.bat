@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   PLATFORM - СБОРКА ПРОЕКТА
echo ========================================
echo.

echo Выберите действие:
echo.
echo   1. Сборка backend
echo   2. Сборка frontend
echo   3. Сборка всех образов
echo   4. Очистка кэша сборки
echo   5. Выход
echo.

set /p choice="Введите номер (1-5): "

if "%choice%"=="1" goto :build_backend
if "%choice%"=="2" goto :build_frontend
if "%choice%"=="3" goto :build_all
if "%choice%"=="4" goto :clean
if "%choice%"=="5" goto :exit
echo Неверный выбор
pause
goto :eof

:build_backend
echo.
echo ===== СБОРКА BACKEND =====
echo [1/3] Очистка предыдущей сборки...
cd backend
call mvn clean -q
echo [2/3] Сборка проекта...
call mvn package -DskipTests -Pprod -q
echo [3/3] Создание Docker образа...
docker build -t platform-backend:latest .
cd ..
echo ✓ Backend собран успешно
pause
goto :eof

:build_frontend
echo.
echo ===== СБОРКА FRONTEND =====
echo [1/3] Установка зависимостей...
cd frontend
call npm ci --silent
echo [2/3] Сборка проекта...
call npm run build --silent
echo [3/3] Создание Docker образа...
docker build -t platform-frontend:latest .
cd ..
echo ✓ Frontend собран успешно
pause
goto :eof

:build_all
echo.
echo ===== СБОРКА ВСЕХ ОБРАЗОВ =====
call :build_backend
call :build_frontend
echo ✓ Все образы собраны успешно
pause
goto :eof

:clean
echo.
echo ===== ОЧИСТКА КЭША =====
echo [1/3] Очистка Docker...
docker system prune -f --volumes
echo [2/3] Очистка Maven...
cd backend
call mvn clean -q
cd ..
echo [3/3] Очистка npm...
cd frontend
if exist node_modules rmdir /s /q node_modules
if exist package-lock.json del package-lock.json
cd ..
echo ✓ Кэш очищен
pause
goto :eof

:exit
echo Выход...
timeout /t 1 /nobreak >nul