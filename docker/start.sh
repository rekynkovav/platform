#!/bin/bash
# docker/start.sh - Запуск всего проекта

set -e

echo "🚀 Starting Platform..."

# Проверка наличия Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed"
    exit 1
fi

# Поддержка docker compose v2 и docker-compose v1
if command -v docker &> /dev/null && docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
else
    echo "❌ Docker Compose is not installed (neither v1 nor v2)"
    exit 1
fi

echo "📦 Using: $COMPOSE_CMD"

# Создание директорий для SSL
mkdir -p docker/nginx/ssl

# Генерация SSL сертификатов (если нет)
if [ ! -f docker/nginx/ssl/platform.crt ]; then
    echo "🔐 Generating SSL certificates..."
    cd docker/nginx/ssl
    chmod +x generate-certs.sh
    ./generate-certs.sh
    cd ../../..
fi

# Добавление hosts (для разработки)
if ! grep -q "platform.local" /etc/hosts; then
    echo "📝 Adding hosts entries..."
    echo "127.0.0.1 platform.local" | sudo tee -a /etc/hosts
    echo "127.0.0.1 api.platform.local" | sudo tee -a /etc/hosts
    echo "127.0.0.1 auth.platform.local" | sudo tee -a /etc/hosts
fi

# Остановка старых контейнеров
echo "🛑 Stopping old containers..."
$COMPOSE_CMD down

# Запуск
echo "🐳 Starting containers..."
$COMPOSE_CMD up -d

echo ""
echo "✅ Platform started successfully!"
echo ""
echo "📍 Access URLs:"
echo "   Main site:     https://platform.local"
echo "   API:           https://api.platform.local"
echo "   Keycloak:      https://auth.platform.local"
echo "   CDN:           http://localhost:8001"
echo ""
echo "👤 Test credentials:"
echo "   Admin:      admin / admin123"
echo "   Moderator:  moderator / moderator123"
echo "   User:       user / user123"
echo ""
echo "📊 Check status: $COMPOSE_CMD ps"
echo "📝 View logs:    $COMPOSE_CMD logs -f"
