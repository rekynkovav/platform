#!/bin/bash
set -e

# Zero-downtime миграции для PostgreSQL
# Использование: ./zero-downtime-migration.sh <migration_name>

MIGRATION_NAME=$1
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-platform_db}
DB_USER=${DB_USER:-platform_user}
DB_PASSWORD=${DB_PASSWORD:-platform_pass_2026}

if [ -z "$MIGRATION_NAME" ]; then
    echo "Usage: $0 <migration_name>"
    exit 1
fi

echo "🚀 Starting zero-downtime migration: $MIGRATION_NAME"
echo "📊 Database: $DB_HOST:$DB_PORT/$DB_NAME"

# Функция для выполнения SQL
execute_sql() {
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "$1"
}

# Функция для выполнения SQL файла
execute_sql_file() {
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$1"
}

# 1. Создать новую миграцию (без блокировки)
echo "📝 Step 1: Creating migration file..."
MIGRATION_FILE="migrations/$(date +%Y%m%d_%H%M%S)_${MIGRATION_NAME}.sql"
mkdir -p migrations
cat > "$MIGRATION_FILE" << EOF
-- Zero-downtime migration: $MIGRATION_NAME
-- Created: $(date)

-- 1. Добавить новые колонки (IF NOT EXISTS)
-- 2. Заполнить данные (батчами по 1000)
-- 3. Добавить индексы (CONCURRENTLY)
-- 4. Создать триггеры/функции
-- 5. Обновить представления
-- 6. Удалить старые колонки (после успешного деплоя)

-- Пример:
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS new_column VARCHAR(255);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_new_column ON users(new_column);
EOF

echo "✅ Migration file created: $MIGRATION_FILE"

# 2. Проверить соединение с БД
echo "🔍 Step 2: Testing database connection..."
if ! execute_sql "SELECT 1;" > /dev/null 2>&1; then
    echo "❌ Database connection failed"
    exit 1
fi
echo "✅ Database connection successful"

# 3. Проверить текущие блокировки
echo "🔒 Step 3: Checking for existing locks..."
execute_sql "SELECT pid, usename, application_name, client_addr, query_start, state, query FROM pg_stat_activity WHERE state != 'idle' AND query NOT LIKE '%pg_stat_activity%' ORDER BY query_start;"

# 4. Создать резервную копию (опционально)
echo "💾 Step 4: Creating backup (optional)..."
BACKUP_FILE="backups/backup_$(date +%Y%m%d_%H%M%S).sql"
mkdir -p backups
if [ "${CREATE_BACKUP:-true}" = "true" ]; then
    echo "Creating backup to $BACKUP_FILE..."
    PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME --no-owner --no-privileges > "$BACKUP_FILE"
    echo "✅ Backup created: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"
fi

# 5. Выполнить миграцию в транзакции
echo "⚡ Step 5: Executing migration..."
echo "⚠️  WARNING: This will execute the migration. Make sure the file is correct!"
read -p "Continue? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [ -f "$MIGRATION_FILE" ]; then
        execute_sql_file "$MIGRATION_FILE"
        echo "✅ Migration executed successfully"
    else
        echo "❌ Migration file not found: $MIGRATION_FILE"
        exit 1
    fi
else
    echo "❌ Migration cancelled"
    exit 0
fi

# 6. Проверить состояние после миграции
echo "🔍 Step 6: Post-migration checks..."
execute_sql "SELECT schemaname, tablename, tableowner, tablespace FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;"

# 7. Анализировать таблицы для обновления статистики
echo "📈 Step 7: Analyzing tables..."
execute_sql "ANALYZE VERBOSE;"

# 8. Проверить производительность
echo "⚡ Step 8: Performance check..."
execute_sql "SELECT now() - query_start as duration, query FROM pg_stat_activity WHERE state = 'active' AND query NOT LIKE '%pg_stat_activity%' ORDER BY duration DESC LIMIT 5;"

echo "🎉 Zero-downtime migration completed successfully!"
echo "📋 Next steps:"
echo "   1. Deploy new application version"
echo "   2. Monitor application logs"
echo "   3. Run smoke tests"
echo "   4. If everything is OK, schedule cleanup of old columns"

# Шаблон для cleanup миграции
cat > "migrations/cleanup_${MIGRATION_NAME}.sql" << EOF
-- Cleanup migration for: $MIGRATION_NAME
-- Run this AFTER new version is deployed and stable

-- Пример удаления старых колонок:
-- ALTER TABLE users DROP COLUMN IF EXISTS old_column;

-- Удаление старых индексов:
-- DROP INDEX IF EXISTS idx_users_old_column;
EOF

echo "📝 Cleanup template created: migrations/cleanup_${MIGRATION_NAME}.sql"