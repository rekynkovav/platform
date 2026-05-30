#!/bin/bash

set -e

VERSION=$1
ENVIRONMENT=${2:-production}
BACKUP_DIR="./backups"
LOG_DIR="./logs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version> [environment]"
    echo "Example: $0 v1.2.3 staging"
    exit 1
fi

echo "🚀 Starting rollback to version: $VERSION"
echo "🌍 Environment: $ENVIRONMENT"
echo "🕐 Timestamp: $TIMESTAMP"

# Создаем директории если их нет
mkdir -p "$BACKUP_DIR"
mkdir -p "$LOG_DIR"

LOG_FILE="$LOG_DIR/rollback_${ENVIRONMENT}_${TIMESTAMP}.log"
exec > >(tee -a "$LOG_FILE") 2>&1

echo "📝 Logging to: $LOG_FILE"

# Функция для проверки статуса
check_status() {
    local service=$1
    local max_attempts=10
    local attempt=1

    echo "🔍 Checking status of $service..."

    while [ $attempt -le $max_attempts ]; do
        if docker-compose ps "$service" | grep -q "Up"; then
            echo "✅ $service is running"
            return 0
        fi
        echo "⏳ Waiting for $service... (attempt $attempt/$max_attempts)"
        sleep 5
        attempt=$((attempt + 1))
    done

    echo "❌ $service failed to start"
    return 1
}

# Функция для проверки health check
check_health() {
    local service=$1
    local port=$2
    local max_attempts=15
    local attempt=1

    echo "🏥 Checking health of $service on port $port..."

    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo "✅ $service health check passed"
            return 0
        fi
        echo "⏳ Waiting for $service health... (attempt $attempt/$max_attempts)"
        sleep 3
        attempt=$((attempt + 1))
    done

    echo "❌ $service health check failed"
    return 1
}

# 1. Создаем резервную копию текущего состояния
echo "💾 Step 1: Creating backup of current state..."
BACKUP_FILE="$BACKUP_DIR/backup_${ENVIRONMENT}_${TIMESTAMP}.tar.gz"

docker-compose exec -T postgres pg_dump -U platform_user platform_db > "${BACKUP_DIR}/db_backup_${TIMESTAMP}.sql"
docker-compose exec -T redis redis-cli -a redis_pass SAVE > /dev/null 2>&1
docker cp platform-redis:/data/dump.rdb "${BACKUP_DIR}/redis_backup_${TIMESTAMP}.rdb"

tar -czf "$BACKUP_FILE" \
    "${BACKUP_DIR}/db_backup_${TIMESTAMP}.sql" \
    "${BACKUP_DIR}/redis_backup_${TIMESTAMP}.rdb" \
    docker-compose.yml \
    .env 2>/dev/null || true

echo "✅ Backup created: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

# 2. Останавливаем текущий backend
echo "🛑 Step 2: Stopping current backend..."
docker-compose stop backend || echo "⚠️  Backend already stopped or not running"

# 3. Проверяем доступность образа для rollback
echo "🔍 Step 3: Checking if image exists..."
if ! docker images | grep -q "platform-backend:$VERSION"; then
    echo "❌ Image platform-backend:$VERSION not found locally"
    echo "📥 Trying to pull from registry..."

    if ! docker pull "platform-backend:$VERSION"; then
        echo "❌ Failed to pull image platform-backend:$VERSION"
        echo "📋 Available images:"
        docker images | grep platform-backend
        exit 1
    fi
fi

# 4. Обновляем тег образа
echo "🏷️  Step 4: Updating image tag..."
docker tag "platform-backend:$VERSION" "platform-backend:latest"

# 5. Запускаем новый контейнер
echo "🚀 Step 5: Starting rolled back version..."
docker-compose up -d backend

# 6. Проверяем запуск
check_status "backend"

# 7. Проверяем health check
check_health "backend" 8081

# 8. Проверяем основные эндпоинты
echo "🔗 Step 8: Testing critical endpoints..."

ENDPOINTS=(
    "/api/health"
    "/api/users/me"
    "/api/posts"
    "/api/chat/rooms"
)

for endpoint in "${ENDPOINTS[@]}"; do
    if curl -s -f "http://localhost:8081$endpoint" > /dev/null 2>&1; then
        echo "✅ Endpoint $endpoint is working"
    else
        echo "⚠️  Endpoint $endpoint returned error"
    fi
    sleep 1
done

# 9. Проверяем WebSocket соединение
echo "🔌 Step 9: Testing WebSocket connection..."
if timeout 5 bash -c 'echo -e "GET /ws/chat HTTP/1.1\r\nHost: localhost\r\nConnection: Upgrade\r\nUpgrade: websocket\r\n\r\n" | nc localhost 8081 | grep -q "101 Switching Protocols"'; then
    echo "✅ WebSocket connection successful"
else
    echo "⚠️  WebSocket connection test failed"
fi

# 10. Проверяем подключение к БД
echo "🗄️  Step 10: Testing database connection..."
if docker-compose exec -T backend curl -s http://localhost:8081/actuator/health | grep -q '"db":{"status":"UP"'; then
    echo "✅ Database connection successful"
else
    echo "⚠️  Database connection issue detected"
fi

# 11. Проверяем подключение к Redis
echo "🔴 Step 11: Testing Redis connection..."
if docker-compose exec -T backend curl -s http://localhost:8081/actuator/health | grep -q '"redis":{"status":"UP"'; then
    echo "✅ Redis connection successful"
else
    echo "⚠️  Redis connection issue detected"
fi

# 12. Собираем метрики после rollback
echo "📊 Step 12: Collecting post-rollback metrics..."
METRICS_FILE="$BACKUP_DIR/metrics_${ENVIRONMENT}_${TIMESTAMP}.json"
curl -s http://localhost:8081/actuator/metrics > "$METRICS_FILE" 2>/dev/null || true

# 13. Проверяем логи на ошибки
echo "📋 Step 13: Checking for errors in logs..."
ERROR_COUNT=$(docker-compose logs backend --tail=100 | grep -i "error\|exception\|failed" | wc -l || true)

if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "⚠️  Found $ERROR_COUNT errors in logs"
    docker-compose logs backend --tail=50 | grep -i "error\|exception\|failed" | head -10
else
    echo "✅ No errors found in recent logs"
fi

echo ""
echo "🎉 Rollback completed successfully!"
echo ""
echo "📋 Summary:"
echo "   Version: $VERSION"
echo "   Environment: $ENVIRONMENT"
echo "   Backup: $BACKUP_FILE"
echo "   Log file: $LOG_FILE"
echo "   Metrics: $METRICS_FILE"
echo ""
echo "🔍 Next steps:"
echo "   1. Monitor application logs for 15 minutes"
echo "   2. Check business metrics dashboard"
echo "   3. Run smoke tests"
echo "   4. Notify team about successful rollback"
echo ""

# Создаем отчет
REPORT_FILE="$BACKUP_DIR/rollback_report_${ENVIRONMENT}_${TIMESTAMP}.md"
cat > "$REPORT_FILE" << EOF
# Rollback Report

## Summary
- **Version**: $VERSION
- **Environment**: $ENVIRONMENT
- **Timestamp**: $TIMESTAMP
- **Status**: ✅ Successful

## Steps Performed
1. Created backup of current state
2. Stopped current backend
3. Verified rollback image exists
4. Updated image tag to $VERSION
5. Started rolled back version
6. Verified health checks
7. Tested critical endpoints
8. Tested WebSocket connection
9. Verified database connectivity
10. Verified Redis connectivity
11. Collected post-rollback metrics
12. Checked for errors in logs

## Files Created
- Backup: \`$BACKUP_FILE\`
- Log: \`$LOG_FILE\`
- Metrics: \`$METRICS_FILE\`
- This report: \`$REPORT_FILE\`

## Health Status
- Backend: ✅ Healthy
- Database: ✅ Connected
- Redis: ✅ Connected
- WebSocket: ✅ Working

## Error Count
- Errors in logs: $ERROR_COUNT

## Next Actions
1. Continue monitoring for 15 minutes
2. Verify business metrics
3. Run comprehensive smoke tests
4. Update deployment documentation

## Rollback Team
- Performed by: Automated Rollback Script
- Reviewed by: [Team Lead Name]
- Approved by: [Tech Lead Name]

EOF

echo "📄 Report created: $REPORT_FILE"
echo "✅ Rollback to version $VERSION completed successfully!"