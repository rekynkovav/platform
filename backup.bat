@echo off
chcp 65001 >nul
title Platform Project - Backup
color 0E

set BACKUP_DIR=backups\platform
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Чтение пароля Redis из .env
set REDIS_PASSWORD=redis_pass
for /f "tokens=1,* delims==" %%a in ('type .env 2^>nul') do (
    if "%%a"=="REDIS_PASSWORD" set REDIS_PASSWORD=%%b
)

echo ========================================
echo    Platform Project - Backup
echo ========================================
echo.

REM Создание папки для бекапов
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

REM 1. Бекап PostgreSQL
echo [1/4] Бекап PostgreSQL...
docker exec platform-postgres-1 pg_dump -U platform_user platform_db > "%BACKUP_DIR%\db_%TIMESTAMP%.sql"
if errorlevel 1 (
    echo [ERROR] Ошибка бекапа PostgreSQL
    pause
    exit /b 1
)
echo [OK] Бекап БД: db_%TIMESTAMP%.sql

REM 2. Бекап Redis
echo [2/4] Бекап Redis...
docker exec platform-redis-1 redis-cli -a %REDIS_PASSWORD% SAVE
if errorlevel 1 (
    echo [ERROR] Ошибка сохранения Redis
    pause
    exit /b 1
)
docker cp platform-redis-1:/data/dump.rdb "%BACKUP_DIR%\redis_%TIMESTAMP%.rdb"
if errorlevel 1 (
    echo [ERROR] Ошибка копирования Redis дампа
    pause
    exit /b 1
)
echo [OK] Бекап Redis: redis_%TIMESTAMP%.rdb

REM 3. Бекап конфигов
echo [3/4] Бекап конфигураций...
xcopy /E /I *.yml "%BACKUP_DIR%\configs_%TIMESTAMP%\" >nul
xcopy /E /I *.properties "%BACKUP_DIR%\configs_%TIMESTAMP%\" >nul
echo [OK] Бекап конфигов

REM 4. Очистка старых бекапов (старше 30 дней)
echo [4/4] Очистка старых бекапов...
forfiles /p "%BACKUP_DIR%" /m "*.sql" /d -30 /c "cmd /c del @file" 2>nul
forfiles /p "%BACKUP_DIR%" /m "*.rdb" /d -30 /c "cmd /c del @file" 2>nul

echo.
echo ========================================
echo    БЕКАП ЗАВЕРШЕН!
echo ========================================
echo.
echo Папка бекапов: %BACKUP_DIR%
echo.
pause