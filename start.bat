@echo off
chcp 65001 >nul
title Platform Project - Full Start
color 0A

echo ========================================
echo    Platform Project - Full Start
echo ========================================
echo.

REM 1. Проверка Docker
echo [1/7] Проверка Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker не найден.
    pause
    exit /b 1
)
echo [OK] Docker доступен.

REM 2. Создание .env
echo [2/7] Создание .env файла...
if not exist ".env" (
    if exist ".env.example" (
        copy .env.example .env >nul
        echo [OK] .env создан из .env.example
    ) else (
        echo [INFO] .env.example не найден, создаю пустой .env
        echo # Platform Environment Variables > .env
        echo REDIS_PASSWORD=secure_password >> .env
        echo DB_PASSWORD=secure_password >> .env
        echo KEYCLOAK_ADMIN_PASSWORD=secure_password >> .env
    )
) else (
    echo [OK] .env уже существует
)

REM 3. Запуск Docker Compose (PostgreSQL, Redis, Keycloak, Nginx, Prometheus, Grafana, ELK, Zipkin)
echo [3/7] Запуск Docker контейнеров...
docker-compose up -d
if errorlevel 1 (
    echo [ERROR] Ошибка запуска Docker
    pause
    exit /b 1
)
echo [OK] Контейнеры запущены

REM 4. Ожидание готовности БД
echo [4/7] Ожидание готовности PostgreSQL...
timeout /t 10 /nobreak >nul
echo [OK] PostgreSQL готов

REM 5. Запуск backend
echo [5/7] Запуск backend...
start "Platform Backend" cmd /c "cd backend && mvnw.cmd spring-boot:run"
echo [OK] Backend запущен

REM 6. Запуск frontend
echo [6/7] Запуск frontend...
start "Platform Frontend" cmd /c "cd frontend && npm start"
echo [OK] Frontend запущен

REM 7. Запуск мониторинга
echo [7/7] Запуск мониторинга...
start "Prometheus" cmd /c "docker-compose logs -f prometheus"
start "Grafana" cmd /c "docker-compose logs -f grafana"

echo.
echo ========================================
echo    ВСЕ СЕРВИСЫ ЗАПУЩЕНЫ!
echo ========================================
echo.
echo Доступ:
echo   Frontend:    http://localhost:3000
echo   Backend:     http://localhost:8081
echo   Keycloak:    http://localhost:8080/auth (admin/admin)
echo   Nginx:       http://localhost:80
echo   Prometheus:  http://localhost:9090
echo   Grafana:     http://localhost:3001 (admin/admin)
echo   Kibana:      http://localhost:5601
echo   Zipkin:      http://localhost:9411
echo.
echo Для остановки: stop.bat
echo Для статуса:   status.bat
echo.
pause