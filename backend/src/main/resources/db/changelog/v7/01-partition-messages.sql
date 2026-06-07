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
    i INTEGER;
BEGIN
    FOR i IN 0..23 LOOP
        start_date := date_trunc('month', CURRENT_DATE) + (i || ' months')::INTERVAL;
        end_date := start_date + INTERVAL '1 month';
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS messages_%s PARTITION OF messages_new FOR VALUES FROM (%L) TO (%L)',
            to_char(start_date, 'YYYY_MM'),
            start_date,
            end_date
        );
    END LOOP;
END $$;
