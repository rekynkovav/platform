@echo off
chcp 65001 >nul
title Platform Project - Clean
color 0C

echo ========================================
echo    Platform Project - Clean
echo ========================================
echo.
echo ВНИМАНИЕ! Это удалит:
echo   - Все Docker контейнеры
echo   - Все Docker образы
echo   - Локальные сборки
echo.

set /p confirm="Уверены? (y/N): "
if /i not "%confirm%"=="y" (
    echo Отменено.
    pause
    exit /b 0
)

echo.
echo [1/4] Остановка и удаление контейнеров...
docker-compose down -v

echo [2/4] Удаление образов...
docker rmi platform-backend platform-frontend 2>nul

echo [3/4] Очистка backend...
cd backend
call mvnw.cmd clean
del /q target\* 2>nul
cd ..

echo [4/4] Очистка frontend...
cd frontend
rmdir /s /q node_modules 2>nul
del /q package-lock.json 2>nul
cd ..

echo.
echo ========================================
echo    ОЧИСТКА ЗАВЕРШЕНА!
echo ========================================
echo.
pause