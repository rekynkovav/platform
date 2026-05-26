@echo off
echo === ЗАПУСК DOCKER COMPOSE ===
echo Дата: %date% %time%
echo.

echo Проверка Docker...
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Docker найден
) else (
    echo ❌ Docker не найден. Убедитесь, что Docker Desktop запущен.
    pause
    exit /b 1
)

echo Останавливаем предыдущие контейнеры...
docker compose down >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Предыдущие контейнеры остановлены
) else (
    docker-compose down >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ Предыдущие контейнеры остановлены (старый синтаксис)
    ) else (
        echo ⚠️  Не удалось остановить контейнеры (возможно, их не было)
    )
)

echo Запускаем все сервисы...
docker compose up -d
if %errorlevel% equ 0 (
    echo ✅ Сервисы запущены в фоновом режиме
) else (
    echo Пробуем старый синтаксис...
    docker-compose up -d
    if %errorlevel% equ 0 (
        echo ✅ Сервисы запущены (старый синтаксис)
    ) else (
        echo ❌ Критическая ошибка при запуске
        pause
        exit /b 1
    )
)

echo Ждём 30 секунд для инициализации сервисов...
timeout /t 30 /nobreak >nul

echo Проверка статуса сервисов:
docker compose ps
if %errorlevel% neq 0 (
    docker-compose ps
)

echo.
echo === СИСТЕМА ЗАПУЩЕНА ===
echo.
echo Доступные сервисы:
echo 1. Backend API: http://localhost:8081
echo 2. Keycloak: http://localhost:8080
echo 3. Frontend: http://localhost:3000
echo 4. Prometheus: http://localhost:9090
echo 5. Grafana: http://localhost:3001
echo 6. Kibana: http://localhost:5601
echo 7. Zipkin: http://localhost:9411
echo 8. Elasticsearch: http://localhost:9200
echo 9. Nginx: http://localhost:80
echo 10. RTMP: rtmp://localhost:1935
echo.
echo Для просмотра логов выполните: docker compose logs -f
echo Для остановки: docker compose down
echo.
pause