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

REM 3. Ожидание Keycloak
echo [3/6] Ожидание Keycloak (60 секунд)...
timeout /t 60 /nobreak >nul

REM 4. Импорт realm в Keycloak
echo [4/6] Импорт Keycloak realm...
docker exec -it platform-keycloak-1 /opt/keycloak/bin/kc.sh import --file /opt/keycloak/data/import/realm-export.json
echo [OK] Realm импортирован

REM 5. Запуск миграций Liquibase
echo [5/6] Запуск миграций БД...
cd backend
call mvnw.cmd liquibase:update
cd ..
echo [OK] Миграции выполнены

REM 6. Создание admin пользователя
echo [6/6] Создание admin пользователя...
docker exec -it platform-postgres-1 psql -U platform_user -d platform_db -c "INSERT INTO users (id, username, email, role, created_at) VALUES ('00000000-0000-0000-0000-000000000001', 'admin', 'admin@platform.local', 'SUPER_ADMIN', NOW()) ON CONFLICT (id) DO NOTHING;"

echo.
echo ========================================
echo    ИНИЦИАЛИЗАЦИЯ ЗАВЕРШЕНА!
echo ========================================
echo.
echo Теперь запустите start.bat
echo.
pause