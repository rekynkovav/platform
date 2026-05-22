@echo off
chcp 65001 >nul
echo ==========================================
echo    ПРОВЕРКА ПОДКЛЮЧЕНИЯ К СЕРВИСАМ
echo ==========================================
echo Дата: %date% %time%
echo.

echo ШАГ 1: Проверка запущенных контейнеров...
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo ШАГ 2: Проверка подключения к PostgreSQL...
docker exec platform-postgres-1 pg_isready -U platform_user -d platform_db
if errorlevel 1 (
    echo ❌ Ошибка подключения к PostgreSQL
    echo Логи PostgreSQL:
    docker logs platform-postgres-1 --tail=10
) else (
    echo ✅ PostgreSQL доступен
    echo Тестируем создание таблицы...
    docker exec platform-postgres-1 psql -U platform_user -d platform_db -c "CREATE TABLE IF NOT EXISTS test_table (id SERIAL PRIMARY KEY, name VARCHAR(50)); INSERT INTO test_table (name) VALUES ('test'); SELECT * FROM test_table; DROP TABLE test_table;"
)

echo.
echo ШАГ 3: Проверка подключения к Redis...
docker exec platform-redis-1 redis-cli --raw incr ping
if errorlevel 1 (
    echo ❌ Ошибка подключения к Redis
    echo Логи Redis:
    docker logs platform-redis-1 --tail=10
) else (
    echo ✅ Redis доступен
    echo Тестируем операции с Redis...
    docker exec platform-redis-1 redis-cli --raw SET test_key "test_value"
    docker exec platform-redis-1 redis-cli --raw GET test_key
    docker exec platform-redis-1 redis-cli --raw DEL test_key
)

echo.
echo ШАГ 4: Проверка Prometheus...
docker exec platform-prometheus-1 wget -q --spider http://localhost:9090
if errorlevel 1 (
    echo ❌ Prometheus не отвечает
    echo Логи Prometheus:
    docker logs platform-prometheus-1 --tail=10
) else (
    echo ✅ Prometheus доступен
    echo Проверяем статус Prometheus...
    docker exec platform-prometheus-1 wget -q -O- http://localhost:9090/api/v1/status/runtimeinfo | head -20
)

echo.
echo ШАГ 5: Проверка Grafana...
docker exec platform-grafana-1 wget -q --spider http://localhost:3000
if errorlevel 1 (
    echo ❌ Grafana не отвечает
    echo Логи Grafana:
    docker logs platform-grafana-1 --tail=10
) else (
    echo ✅ Grafana доступен
    echo Проверяем статус Grafana...
    docker exec platform-grafana-1 wget -q -O- http://localhost:3000/api/health | head -5
)

echo.
echo ==========================================
echo          РЕЗУЛЬТАТЫ ПРОВЕРКИ
echo ==========================================
echo.
echo Для просмотра полных логов:
echo docker logs platform-postgres-1
echo docker logs platform-redis-1
echo docker logs platform-prometheus-1
echo docker logs platform-grafana-1
echo.
pause