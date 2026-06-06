#!/bin/bash
# Скрипт для полного сброса базы данных
# ВНИМАНИЕ: Удаляет ВСЕ данные!

set -e

echo "=== СБРОС БАЗЫ ДАННЫХ ==="
echo "ВНИМАНИЕ: Это удалит все данные!"
read -p "Продолжить? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Отменено."
    exit 1
fi

# Параметры подключения (можно переопределить переменными окружения)
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-platform}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-postgres}

echo "Подключение к БД $DB_NAME на $DB_HOST:$DB_PORT..."

# Сброс схемы public
export PGPASSWORD=$DB_PASSWORD
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME << EOF
-- Удаляем таблицу liquibase
DROP TABLE IF EXISTS databasechangelog CASCADE;
DROP TABLE IF EXISTS databasechangeloglock CASCADE;

-- Очищаем схему public
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO $DB_USER;
GRANT ALL ON SCHEMA public TO public;

-- Подтверждение
SELECT 'Database reset completed!' as status;
EOF

echo "=== БД ОЧИЩЕНА ==="
echo "Теперь можно запустить приложение - Liquibase создаст все таблицы заново"
