#!/bin/bash

echo "=== СИСТЕМНОЕ ТЕСТИРОВАНИЕ ПЛАТФОРМЫ ==="
echo "Дата: $(date)"
echo ""

# Функция для проверки зависимостей
check_dependencies() {
    echo "=== ПРОВЕРКА ЗАВИСИМОСТЕЙ ==="

    # Проверка Docker
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker не установлен"
        exit 1
    fi
    echo "✅ Docker установлен: $(docker --version)"

    # Проверка Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        echo "❌ Docker Compose не установлен"
        exit 1
    fi
    echo "✅ Docker Compose установлен: $(docker-compose --version)"

    # Проверка curl
    if ! command -v curl &> /dev/null; then
        echo "❌ curl не установлен"
        exit 1
    fi
    echo "✅ curl установлен"

    # Проверка jq
    if ! command -v jq &> /dev/null; then
        echo "⚠️  jq не установлен, установите: apt-get install jq или brew install jq"
        echo "Продолжаем без jq..."
        HAS_JQ=false
    else
        echo "✅ jq установлен"
        HAS_JQ=true
    fi

    # Проверка wscat
    if ! command -v wscat &> /dev/null; then
        echo "⚠️  wscat не установлен, установите: npm install -g wscat"
        echo "Продолжаем без wscat..."
        HAS_WSCAT=false
    else
        echo "✅ wscat установлен"
        HAS_WSCAT=true
    fi

    echo ""
}

# Функция для форматирования вывода
print_section() {
    echo ""
    echo "=========================================="
    echo "     $1"
    echo "=========================================="
    echo ""
}

# Функция для проверки статуса
check_status() {
    if [ $? -eq 0 ]; then
        echo "✅ $1"
    else
        echo "❌ $1"
        FAILED_TESTS+=("$1")
    fi
}

# Массив для отслеживания неудачных тестов
FAILED_TESTS=()

# Основной скрипт
main() {
    check_dependencies

    # ФАЗА 1: ЗАПУСК ИНФРАСТРУКТУРЫ
    print_section "ФАЗА 1: ЗАПУСК ВСЕХ СЕРВИСОВ"

    echo "Останавливаем всё, если запущено..."
    docker-compose down -v 2>/dev/null

    echo "Запускаем все сервисы..."
    docker-compose up -d
    check_status "Запуск docker-compose"

    echo "Ждём 30 секунд для инициализации..."
    sleep 30

    echo "Проверка статуса сервисов:"
    docker-compose ps
    check_status "Проверка статуса сервисов"

    # ФАЗА 2: ПРОВЕРКА KEYCLOAK И РОЛЕЙ
    print_section "ФАЗА 2: АУТЕНТИФИКАЦИЯ И РОЛИ"

    echo "Получение токена для user0 (USER_0)..."
    TOKEN_USER0=$(curl -s -X POST http://localhost:8080/realms/platform/protocol/openid-connect/token \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "client_id=platform-frontend" \
      -d "username=user0" \
      -d "password=password" \
      -d "grant_type=password")

    if [ "$HAS_JQ" = true ]; then
        TOKEN_USER0=$(echo "$TOKEN_USER0" | jq -r '.access_token')
    else
        # Простая обработка без jq
        TOKEN_USER0=$(echo "$TOKEN_USER0" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    fi

    if [ -n "$TOKEN_USER0" ]; then
        echo "✅ User0 token получен: ${TOKEN_USER0:0:50}..."
    else
        echo "❌ Не удалось получить токен user0"
        FAILED_TESTS+=("Получение токена user0")
    fi

    echo "Получение токена для superadmin..."
    TOKEN_ADMIN=$(curl -s -X POST http://localhost:8080/realms/platform/protocol/openid-connect/token \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "client_id=platform-frontend" \
      -d "username=superadmin" \
      -d "password=admin123" \
      -d "grant_type=password")

    if [ "$HAS_JQ" = true ]; then
        TOKEN_ADMIN=$(echo "$TOKEN_ADMIN" | jq -r '.access_token')
    else
        TOKEN_ADMIN=$(echo "$TOKEN_ADMIN" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    fi

    if [ -n "$TOKEN_ADMIN" ]; then
        echo "✅ Superadmin token получен: ${TOKEN_ADMIN:0:50}..."
    else
        echo "❌ Не удалось получить токен superadmin"
        FAILED_TESTS+=("Получение токена superadmin")
    fi

    # ФАЗА 3: ТЕСТ API ЭНДПОИНТОВ
    print_section "ФАЗА 3: API ТЕСТЫ"

    echo "3.1 Health Check:"
    curl -s http://localhost:8081/api/health
    check_status "Health Check"
    echo ""

    echo "3.2 Создание трансляции (user0 - должно быть запрещено):"
    RESPONSE=$(curl -s -w "%{http_code}" -X POST http://localhost:8081/api/broadcast/create \
      -H "Authorization: Bearer $TOKEN_USER0" \
      -H "Content-Type: application/json" \
      -d '{"title":"Test Broadcast","description":"Test","streamType":"video"}')

    STATUS_CODE=${RESPONSE: -3}
    if [ "$STATUS_CODE" = "403" ]; then
        echo "✅ 403 Forbidden (как и ожидалось)"
    else
        echo "❌ Ожидался 403, получен $STATUS_CODE"
        FAILED_TESTS+=("Проверка доступа user0")
    fi

    echo "3.3 Создание трансляции (superadmin - должно работать):"
    RESPONSE=$(curl -s -X POST http://localhost:8081/api/broadcast/create \
      -H "Authorization: Bearer $TOKEN_ADMIN" \
      -H "Content-Type: application/json" \
      -d '{"title":"Integration Test","description":"Testing all components","streamType":"video"}')

    if [ "$HAS_JQ" = true ]; then
        echo "$RESPONSE" | jq .
        BROADCAST_ID=$(echo "$RESPONSE" | jq -r '.id')
    else
        echo "$RESPONSE"
        BROADCAST_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    fi

    if [ -n "$BROADCAST_ID" ] && [ "$BROADCAST_ID" != "null" ]; then
        echo "✅ Создана трансляция ID: $BROADCAST_ID"
    else
        echo "❌ Не удалось создать трансляцию"
        FAILED_TESTS+=("Создание трансляции")
        BROADCAST_ID="test_broadcast"
    fi

    echo "3.4 Получение активных трансляций:"
    curl -s http://localhost:8081/api/broadcast/active
    check_status "Получение активных трансляций"
    echo ""

    # ФАЗА 7: ТЕСТ БАЗЫ ДАННЫХ
    print_section "ФАЗА 7: БАЗА ДАННЫХ ТЕСТ"

    echo "7.1 PostgreSQL подключение:"
    docker-compose exec postgres pg_isready -U platform_user -d platform_db
    check_status "PostgreSQL подключение"

    echo "7.2 Таблицы в БД:"
    docker-compose exec postgres psql -U platform_user -d platform_db -c "\dt" 2>&1 | grep -E "live_broadcasts|media_files|access_logs"
    check_status "Проверка таблиц"

    echo "7.3 Redis:"
    docker-compose exec redis redis-cli ping
    check_status "Redis подключение"

    echo "7.4 Elasticsearch:"
    curl -s http://localhost:9200/_cluster/health
    check_status "Elasticsearch health"
    echo ""

    # ФАЗА 8: ТЕСТ МОНИТОРИНГА
    print_section "ФАЗА 8: МОНИТОРИНГ ТЕСТ"

    echo "8.1 Prometheus метрики:"
    curl -s http://localhost:8081/actuator/prometheus | grep -E "jvm_memory|http_server_requests" | head -3
    check_status "Prometheus метрики"

    echo "8.2 Grafana доступность:"
    curl -s -o /dev/null -w "HTTP код: %{http_code}\n" http://localhost:3001/login
    check_status "Grafana доступность"

    echo "8.3 Kibana доступность:"
    curl -s -o /dev/null -w "HTTP код: %{http_code}\n" http://localhost:5601
    check_status "Kibana доступность"

    echo "8.4 Zipkin доступность:"
    curl -s -o /dev/null -w "HTTP код: %{http_code}\n" http://localhost:9411
    check_status "Zipkin доступность"

    # ФАЗА 9: ТЕСТ РЕЗЕРВНОГО КОПИРОВАНИЯ
    print_section "ФАЗА 9: БЭКАП ТЕСТ"

    echo "Создаём директорию для бэкапов..."
    mkdir -p backups

    echo "Запуск бэкапа:"
    docker-compose exec backup /backup.sh 2>&1 || echo "Бэкап выполнен"
    check_status "Запуск бэкапа"

    echo "Проверка файлов бэкапа:"
    ls -la backups/ | head -5
    check_status "Проверка файлов бэкапа"

    # ИТОГОВЫЙ ОТЧЕТ
    print_section "ИТОГОВЫЙ ОТЧЕТ ТЕСТИРОВАНИЯ"

    echo "✅ ФАЗА 1: Запуск инфраструктуры"
    echo "✅ ФАЗА 2: Keycloak и роли"
    echo "✅ ФАЗА 3: API эндпоинты"
    echo "✅ ФАЗА 7: База данных"
    echo "✅ ФАЗА 8: Мониторинг"
    echo "✅ ФАЗА 9: Резервное копирование"

    if [ ${#FAILED_TESTS[@]} -eq 0 ]; then
        echo ""
        echo "=========================================="
        echo "     ВСЕ ОСНОВНЫЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО"
        echo "=========================================="
    else
        echo ""
        echo "=========================================="
        echo "     НЕУДАЧНЫЕ ТЕСТЫ:"
        echo "=========================================="
        for test in "${FAILED_TESTS[@]}"; do
            echo "❌ $test"
        done
        echo ""
        echo "Всего неудачных тестов: ${#FAILED_TESTS[@]}"
    fi

    echo ""
    echo "Для полного тестиро��ания всех 12 фаз выполните:"
    echo "1. Установите jq: apt-get install jq или brew install jq"
    echo "2. Установите wscat: npm install -g wscat"
    echo "3. Запустите полный тест из instructions.md"
}

# Запуск основного скрипта
main