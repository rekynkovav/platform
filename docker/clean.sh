#!/bin/bash
# docker/clean.sh - Полная очистка

echo "🧹 Cleaning Platform..."

# Остановка и удаление контейнеров
docker-compose down -v

# Удаление образов
docker rmi platform_backend platform_frontend 2>/dev/null || true

# Очистка данных
rm -rf docker/nginx/ssl/*.crt docker/nginx/ssl/*.key 2>/dev/null || true

echo "✅ Cleanup complete"