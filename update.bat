@echo off
chcp 65001 >nul
title Platform Project - Update
color 0E

echo ========================================
echo    Platform Project - Update
echo ========================================
echo.

REM 1. Остановка сервисов
echo [1/7] Остановка сервисов...
docker-compose down
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1

REM 2. Pull последних изменений
echo [2/7] Pull из Git...
git pull

REM 3. Пересборка Docker образов
echo [3/7] Пересборка Docker образов...
docker-compose build --no-cache

REM 4. Обновление зависимостей backend
echo [4/7] Обновление backend зависимостей...
cd backend
call mvnw.cmd dependency:resolve

REM 5. Обновление зависимостей frontend
echo [5/7] Обновление frontend зависимостей...
cd ..\frontend
call npm install

REM 6. Запуск миграций
echo [6/7] Запуск миграций БД...
cd ..\backend
call mvnw.cmd liquibase:update
cd ..

REM 7. Запуск сервисов
echo [7/7] Запуск сервисов...
call start.bat

echo.
echo ========================================
echo    ОБНОВЛЕНИЕ ЗАВЕРШЕНО!
echo ========================================
echo.
pause