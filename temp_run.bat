@echo off
echo ===== ЗАПУСК DOCKER COMPOSE =====
echo.

cd /d "C:\Users\Suveren\deepseek-cli\platform"

echo Проверка Docker...
"C:\Program Files\Docker\Docker\resources\bin\docker.exe" --version
if %errorlevel% neq 0 (
    echo ❌ Docker не найден
    pause
    exit /b 1
)

echo.
echo Останавливаем предыдущие контейнеры...
"C:\Program Files\Docker\Docker\resources\bin\docker.exe" compose down

echo.
echo Запускаем все сервисы...
echo Это займет несколько минут...
"C:\Program Files\Docker\Docker\resources\bin\docker.exe" compose up -d

echo.
echo Ждем 30 секунд для инициализации...
timeout /t 30 /nobreak >nul

echo.
echo Проверка статуса...
"C:\Program Files\Docker\Docker\resources\bin\docker.exe" compose ps

echo.
echo ===== СИСТЕМА ЗАПУЩЕНА =====
echo.
pause