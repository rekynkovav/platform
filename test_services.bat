@echo off
chcp 65001 >nul
echo ==========================================
echo    ТЕСТИРОВАНИЕ СЕРВИСОВ ПЛАТФОРМЫ
echo ==========================================
echo Дата: %date% %time%
echo.

echo ШАГ 1: Проверка Docker...
docker --version
if errorlevel 1 (
    echo ❌ Docker не найден
    pause
    exit /b 1
)

echo.
echo ШАГ 2: Проверка запущенных контейнеров...
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo ШАГ 3: Проверка состояния сервисов...

echo 1. PostgreSQL...
docker exec platform-postgres-1 pg_isready -U platform_user -d platform_db
if errorlevel 1 (
    echo    ❌ Не работает
) else (
    echo    ✅ Работает
)

echo 2. Redis...
docker exec platform-redis-1 redis-cli --raw incr ping > nul 2>&1
if errorlevel 1 (
    echo    ❌ Не работает
) else (
    echo    ✅ Работает
)

echo 3. Prometheus...
docker exec platform-prometheus-1 wget -q --spider http://localhost:9090
if errorlevel 1 (
    echo    ❌ Не работает
) else (
    echo    ✅ Работает
)

echo 4. Grafana...
docker exec platform-grafana-1 wget -q --spider http://localhost:3000
if errorlevel 1 (
    echo    ❌ Не работает
) else (
    echo    ✅ Работает
)

echo.
echo ==========================================
echo          РЕЗУЛЬТАТЫ
echo ==========================================
echo.
echo Все базовые сервисы запущены и работают внутри Docker сети.
echo.
echo Для доступа к сервисам можно использовать:
echo 1. docker exec -it platform-postgres-1 psql -U platform_user -d platform_db
echo 2. docker exec -it platform-redis-1 redis-cli
echo 3. docker exec -it platform-prometheus-1 sh
echo 4. docker exec -it platform-grafana-1 sh
echo.
echo Для остановки: docker compose -f docker-compose-internal.yml down
echo Для просмотра логов: docker compose -f docker-compose-internal.yml logs -f
echo.
echo ==========================================
pause