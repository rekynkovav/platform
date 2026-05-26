-- Создание партиционированной таблицы сообщений
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
BEGIN
    -- Создаём новую партиционированную таблицу
    CREATE TABLE IF NOT EXISTS messages_new (
        id UUID NOT NULL,
        content TEXT,
        user_id VARCHAR(255),
        room_id UUID,
        created_at TIMESTAMP NOT NULL,
        media_id VARCHAR(255),
        media_type VARCHAR(50),
        file_name VARCHAR(500),
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

-- Перенос данных (постепенно, небольшими батчами)
INSERT INTO messages_new
SELECT * FROM messages
WHERE created_at >= '2024-01-01'
ON CONFLICT (id, created_at) DO NOTHING;

-- Переключение таблиц
ALTER TABLE messages RENAME TO messages_old;
ALTER TABLE messages_new RENAME TO messages;

-- Создание индексов
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_room_created
ON messages(room_id, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_user_created
ON messages(user_id, created_at DESC);

-- Анализ для планировщика
ANALYZE messages;