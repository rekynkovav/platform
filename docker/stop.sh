#!/bin/bash
# docker/stop.sh - Остановка проекта

echo "🛑 Stopping Platform..."
docker-compose down
echo "✅ Platform stopped"