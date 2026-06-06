-- Миграция v7: Партиционирование таблицы messages
-- Исправлено: безопасная версия без переключения таблицы,
-- только создание партиционированной таблицы для новых данных.
-- Существующая таблица messages остаётся нетронутой.

-- 1. Создаём новую партиционированную таблицу (только если не существует)
CREATE TABLE IF NOT EXISTS messages_new (
    LIKE messages INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES
) PARTITION BY RANGE (created_at);

-- 2. Создаём партиции на 24 месяца вперёд
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
    i INTEGER;
BEGIN
    FOR i IN 0..24 LOOP
        start_date := DATE_TRUNC('month', CURRENT_DATE + (i || ' months')::INTERVAL)::DATE;
        end_date := start_date + INTERVAL '1 month';
        partition_name := 'messages_' || TO_CHAR(start_date, 'YYYY_MM');

        IF NOT EXISTS (
            SELECT 1 FROM pg_class WHERE relname = partition_name
        ) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF messages_new FOR VALUES FROM (%L) TO (%L)',
                partition_name, start_date, end_date
            );
        END IF;
    END LOOP;

    -- 3. Прикрепляем существующие данные (если есть)
    IF NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'messages_legacy'
    ) THEN
        CREATE TABLE messages_legacy (LIKE messages INCLUDING DEFAULTS);
        ALTER TABLE messages_legacy ADD PRIMARY KEY (id, created_at);
        CREATE INDEX IF NOT EXISTS idx_messages_legacy_created ON messages_legacy(created_at DESC);

        INSERT INTO messages_legacy
        SELECT * FROM messages
        WHERE created_at >= '2024-01-01' AND created_at < DATE_TRUNC('month', CURRENT_DATE)::DATE
        ON CONFLICT (id, created_at) DO NOTHING;

        ALTER TABLE messages_new ATTACH PARTITION messages_legacy
        FOR VALUES FROM ('2024-01-01') TO (DATE_TRUNC('month', CURRENT_DATE)::DATE);
    END IF;

    -- 4. Создаём или прикрепляем партицию для текущего месяца
    start_date := DATE_TRUNC('month', CURRENT_DATE)::DATE;
    end_date := start_date + INTERVAL '1 month';
    partition_name := 'messages_' || TO_CHAR(start_date, 'YYYY_MM');

    IF NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = partition_name
    ) THEN
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF messages_new FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date
        );
    END IF;
END $$;
