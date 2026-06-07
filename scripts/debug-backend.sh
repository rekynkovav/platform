#!/bin/bash
# scripts/debug-backend.sh - Диагностика причин рестарта бэкенда (СУХОЙ ОТЧЁТ)
# Использование: chmod +x scripts/debug-backend.sh && ./scripts/debug-backend.sh

set -e

CONTAINER_ID=$(docker compose ps -q backend 2>/dev/null || true)

if [ -z "$CONTAINER_ID" ]; then
    echo "ERROR: Контейнер backend не найден"
    exit 1
fi

echo "=== СУХОЙ ОТЧЁТ ОШИБОК БЭКЕНДА ==="
echo ""

# Количество рестартов
RESTARTS=$(docker inspect --format='{{.RestartCount}}' "$CONTAINER_ID" 2>/dev/null || echo "N/A")
echo "Рестартов: $RESTARTS"
echo ""

# Только реальные ошибки (без стеков, без INFO, без WARN)
echo "--- ОШИБКИ ЛИКВИБЕЙЗА И ЗАПУСКА ---"
docker compose logs --tail=300 backend 2>&1 \
    | grep -oP '"message":"[^"]*"' \
    | grep -iE '(ERROR|Exception|Failed|cannot run|does not exist|no unique|constraint)' \
    | sort -u \
    | head -20
echo ""

# Healthcheck статус
echo "--- HEALTHCHECK ---"
HC_STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$CONTAINER_ID" 2>/dev/null || echo "N/A")
echo "Статус: $HC_STATUS"
echo ""

echo "=== КОНЕЦ ОТЧЁТА ==="
