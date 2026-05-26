#!/bin/bash

set -e

BATCH_SIZE=10000
TABLE=$1

if [ -z "$TABLE" ]; then
    echo "Usage: ./batch-migration.sh <table_name>"
    echo "Example: ./batch-migration.sh users"
    exit 1
fi

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-platform_db}
DB_USER=${DB_USER:-platform_user}
DB_PASSWORD=${DB_PASSWORD:-platform_pass_2026}

echo "Starting zero-downtime batch migration for table: $TABLE"
echo "Database: $DB_HOST:$DB_PORT/$DB_NAME"
echo "Batch size: $BATCH_SIZE"

# Проверяем существование таблицы
if ! PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "\dt $TABLE" | grep -q "$TABLE"; then
    echo "Error: Table $TABLE does not exist"
    exit 1
fi

# Проверяем существование колонок
echo "Checking columns..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "\d $TABLE"

read -p "Continue with migration? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Migration cancelled"
    exit 0
fi

TOTAL_ROWS=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM $TABLE;" | tr -d ' ')
echo "Total rows to process: $TOTAL_ROWS"

PROCESSED=0
START_TIME=$(date +%s)

while true; do
    # Обновляем батч строк
    UPDATED=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
        WITH batch AS (
            SELECT id
            FROM $TABLE
            WHERE new_column IS NULL
            ORDER BY id
            LIMIT $BATCH_SIZE
            FOR UPDATE SKIP LOCKED
        )
        UPDATE $TABLE t
        SET new_column = old_column,
            updated_at = NOW()
        FROM batch b
        WHERE t.id = b.id
        RETURNING t.id;
    " | wc -l)

    PROCESSED=$((PROCESSED + UPDATED))

    # Рассчитываем прогресс
    PERCENTAGE=0
    if [ $TOTAL_ROWS -gt 0 ]; then
        PERCENTAGE=$((PROCESSED * 100 / TOTAL_ROWS))
    fi

    ELAPSED=$(( $(date +%s) - START_TIME ))
    REMAINING=0
    if [ $PROCESSED -gt 0 ]; then
        REMAINING=$(( (ELAPSED * TOTAL_ROWS / PROCESSED) - ELAPSED ))
    fi

    echo "Progress: $PROCESSED/$TOTAL_ROWS ($PERCENTAGE%) - Updated $UPDATED rows - Elapsed: ${ELAPSED}s - Remaining: ${REMAINING}s"

    if [ "$UPDATED" -eq 0 ]; then
        break
    fi

    # Даем БД отдохнуть между батчами
    sleep 0.5
done

END_TIME=$(date +%s)
TOTAL_TIME=$((END_TIME - START_TIME))

echo "Migration complete!"
echo "Total time: ${TOTAL_TIME}s"
echo "Rows processed: $PROCESSED"
echo "Average speed: $((PROCESSED / (TOTAL_TIME > 0 ? TOTAL_TIME : 1))) rows/second"

# Создаем индекс CONCURRENTLY после миграции
echo "Creating index concurrently..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_${TABLE}_new_column ON $TABLE(new_column);"

# Анализируем таблицу для обновления статистики
echo "Analyzing table..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "ANALYZE VERBOSE $TABLE;"

# Проверяем результаты
echo "Verification..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
    SELECT
        COUNT(*) as total_rows,
        COUNT(new_column) as migrated_rows,
        COUNT(*) - COUNT(new_column) as remaining_null
    FROM $TABLE;
"

echo "✅ Batch migration completed successfully!"