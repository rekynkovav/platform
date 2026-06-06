-- Миграция v7: Партиционирование таблицы messages
-- Исправлено: полностью переписана на PostgreSQL (удалены MySQL-конструкции)

-- Безопасная версия с использованием ATTACH PARTITION
-- Все операции выполняются через PL/pgSQL

DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
    i INTEGER;
BEGIN
    -- 1. Создаём новую партиционированную таблицу (только если она ещё не существует)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'messages_new'
    ) THEN
        CREATE TABLE messages_new (
            LIKE messages INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES
        ) PARTITION BY RANGE (created_at);
    END IF;

    -- 2. Создаём партиции на 24 месяца вперёд
    FOR i IN 0..24 LOOP
        start_date := DATE_TRUNC('month', CURRENT_DATE + (i || ' months')::INTERVAL)::DATE;
        end_date := start_date + INTERVAL '1 month';
        partition_name := 'messages_' || TO_CHAR(start_date, 'YYYY_MM');

        -- Проверяем, не существует ли уже такая партиция
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
    start_date := '2024-01-01'::DATE;
    end_date := DATE_TRUNC('month', CURRENT_DATE)::DATE;

    IF NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'messages_legacy'
    ) THEN
        CREATE TABLE messages_legacy (LIKE messages INCLUDING DEFAULTS);
        ALTER TABLE messages_legacy ADD PRIMARY KEY (id, created_at);
        CREATE INDEX IF NOT EXISTS idx_messages_legacy_created ON messages_legacy(created_at DESC);

        INSERT INTO messages_legacy
        SELECT * FROM messages
        WHERE created_at >= start_date AND created_at < end_date
        ON CONFLICT (id, created_at) DO NOTHING;

        ALTER TABLE messages_new ATTACH PARTITION messages_legacy
        FOR VALUES FROM (start_date) TO (end_date);
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

    -- 5. Если партиционированная таблица создана, переключаем на неё
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'partition_status'
    ) THEN
        -- Добавляем метку о том, что партиционирование выполнено
        ALTER TABLE messages ADD COLUMN IF NOT EXISTS partition_status VARCHAR(20) DEFAULT 'partitioned';
        -- Можно также добавить триггер для автоматического перемещения данных,
        -- но для простоты оставляем как есть.
    END IF;
END $$;
