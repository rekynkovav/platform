@echo off
chcp 65001 >nul
title Platform Project - Initialization
color 0E

echo ========================================
echo    Platform Project - Initialization
echo ========================================
echo.

REM 1. Проверка Docker
echo [1/6] Проверка Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker не найден.
    pause
    exit /b 1
)

REM 2. Запуск Docker Compose
echo [2/6] Запуск Docker контейнеров...
docker-compose up -d

REM 3. Проверка существования таблиц
echo [3/6] Проверка наличия таблиц...
docker exec platform-postgres-1 psql -U platform_user -d platform_db -c "\d users" >nul 2>&1
set users_exists=%errorlevel%
docker exec platform-postgres-1 psql -U platform_user -d platform_db -c "\d messages" >nul 2>&1
set messages_exists=%errorlevel%

if %users_exists% neq 0 (
    echo Таблицы не найдены, запуск миграций...
    cd backend
    call mvnw.cmd liquibase:update
    cd ..
) else if %messages_exists% neq 0 (
    echo Таблицы не найдены, запуск миграций...
    cd backend
    call mvnw.cmd liquibase:update
    cd ..
) else (
    echo Таблицы уже существуют
)

REM 4. Ожидание Keycloak
echo [4/6] Ожидание Keycloak (60 секунд)...
timeout /t 60 /nobreak >nul

REM 5. Импорт realm в Keycloak
echo [5/6] Импорт Keycloak realm...
docker exec platform-keycloak-1 /opt/keycloak/bin/kc.sh import --file /opt/keycloak/data/import/realm-export.json
echo [OK] Realm импортирован

REM 6. Создание admin пользователя через Keycloak
echo [6/6] Создание admin пользователя через Keycloak...
echo [INFO] Администратор создаётся через Keycloak UI

echo.
echo ========================================
echo    ИНИЦИАЛИЗАЦИЯ ЗАВЕРШЕНА!
echo ========================================
echo.
echo Теперь запустите start.bat
echo.
pause