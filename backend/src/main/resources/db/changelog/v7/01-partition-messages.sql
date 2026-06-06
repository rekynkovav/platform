-- Создание партиционированной таблицы сообщений
-- ИСПРАВЛЕНО: убран CREATE INDEX CONCURRENTLY (не работает внутри транзакции Liquibase)
-- ИСПРАВЛЕНО: старая таблица messages НЕ имеет колонки updated_at

DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
BEGIN
    -- Создаём новую партиционированную таблицу с ПОЛНОЙ схемой
    CREATE TABLE IF NOT EXISTS messages_new (
        id UUID NOT NULL,
        content TEXT,
        user_id VARCHAR(255),
        user_name VARCHAR(128),
        chat_type VARCHAR(32) DEFAULT 'general',
        recipient_id VARCHAR(255),
        is_read BOOLEAN DEFAULT false,
        deleted_at TIMESTAMPTZ,
        room_id UUID,
        room_slug VARCHAR(100),
        media_id VARCHAR(255),
        media_type VARCHAR(50),
        file_name VARCHAR(500),
        version BIGINT DEFAULT 0,
        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id, created_at)
    ) PARTITION BY RANGE (created_at);

    -- Создаём партиции на 24 месяца вперёд
    FOR i IN 0..24 LOOP
        start_date := DATE_TRUNC('month', CURRENT_DATE + (i || ' months')::INTERVAL)::DATE;
        end_date := start_date + INTERVAL '1 month';
        partition_name := 'messages_' || TO_CHAR(start_date, 'YYYY_MM');

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I PARTITION OF messages_new
            FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date
        );
    END LOOP;
END $$;

-- Перенос данных с ЯВНЫМ списком колонок
-- ВНИМАНИЕ: старая таблица messages НЕ имеет updated_at, поэтому используем created_at как значение по умолчанию
INSERT INTO messages_new (
    id, content, user_id, user_name, chat_type, recipient_id, is_read, deleted_at,
    room_id, room_slug, media_id, media_type, file_name, version, created_at, updated_at
)
SELECT
    id::UUID,
    content,
    user_id,
    user_name,
    chat_type,
    recipient_id,
    is_read,
    deleted_at,
    room_id,
    room_slug,
    NULL as media_id,
    NULL as media_type,
    NULL as file_name,
    COALESCE(version, 0),
    created_at,
    created_at as updated_at
FROM messages
WHERE created_at >= '2024-01-01'
ON CONFLICT (id, created_at) DO NOTHING;

-- Переключение таблиц
ALTER TABLE messages RENAME TO messages_old;
ALTER TABLE messages_new RENAME TO messages;

-- Создание индексов (БЕЗ CONCURRENTLY — Liquibase выполняет всё в одной транзакции)
CREATE INDEX IF NOT EXISTS idx_messages_room_created
ON messages(room_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_messages_user_created
ON messages(user_id, created_at DESC);

-- Анализ для планировщика
ANALYZE messages;
