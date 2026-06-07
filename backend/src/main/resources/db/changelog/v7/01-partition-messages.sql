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
    partit
