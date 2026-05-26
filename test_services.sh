#!/bin/bash

echo "=========================================="
echo "   ТЕСТИРОВАНИЕ СЕРВИСОВ ПЛАТФОРМЫ"
echo "=========================================="
echo "Дата: $(date)"
echo

echo "ШАГ 1: Проверка Docker..."
PATH="/c/Program Files/Docker/Docker/resources/bin:$PATH" /c/Program\ Files/Docker/Docker/resources/bin/docker.exe --version
if [ $? -ne 0 ]; then
    echo "❌ Docker не найден"
    exit 1
fi

echo
echo "ШАГ 2: Проверка запущенных контейнеров..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo
echo "ШАГ 3: Проверка состояния сервисов..."

# Проверка PostgreSQL
echo "1. PostgreSQL..."
docker exec platform-postgres-1 pg_isready -U platform_user -d platform_db
if [ $? -eq 0 ]; then
    echo "   ✅ Работает"
else
    echo "   ❌ Не работает"
fi

# Проверка Redis
echo "2. Redis..."
docker exec platform-redis-1 redis-cli --raw incr ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Работает"
else
    echo "   ❌ Не работает"
fi

# Проверка Prometheus
echo "3. Prometheus..."
docker exec platform-prometheus-1 wget -q --spider http://localhost:9090
if [ $? -eq 0 ]; then
    echo "   ✅ Работает"
else
    echo "   ❌ Не работает"
fi

# Проверка Grafana
echo "4. Grafana..."
docker exec platform-grafana-1 wget -q --spider http://localhost:3000
if [ $? -eq 0 ]; then
    echo "   ✅ Работает"
else
    echo "   ❌ Не работает"
fi

echo
echo "=========================================="
echo "          РЕЗУЛЬТАТЫ"
echo "=========================================="
echo
echo "Все базовые сервисы запущены и работают внутри Docker сети."
echo
echo "Для доступа к сервисам можно использовать:"
echo "1. docker exec -it platform-postgres-1 psql -U platform_user -d platform_db"
echo "2. docker exec -it platform-redis-1 redis-cli"
echo "3. docker exec -it platform-prometheus-1 sh"
echo "4. docker exec -it platform-grafana-1 sh"
echo
echo "Для остановки: docker compose -f docker-compose-internal.yml down"
echo "Для просмотра логов: docker compose -f docker-compose-internal.yml logs -f"
echo
echo "=========================================="