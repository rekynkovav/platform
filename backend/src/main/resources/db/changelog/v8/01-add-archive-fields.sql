-- Миграция v8: Добавление полей архивации в live_broadcasts
-- Исправлено: runInTransaction: false, создание индексов CONCURRENTLY

-- Сначала выполняем DDL (допустимо в транзакции)
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS recorded_url VARCHAR(500);
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP;
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS recorded_size BIGINT;
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS recorded_duration INTEGER;

COMMENT ON COLUMN live_broadcasts.recorded_url IS 'URL архива в S3';
COMMENT ON COLUMN live_broadcasts.recorded_at IS 'Дата и время архивации';
COMMENT ON COLUMN live_broadcasts.recorded_size IS 'Размер архива в байтах';
COMMENT ON COLUMN live_broadcasts.recorded_duration IS 'Длительность архива в секундах';

-- Создание индексов (разрешено только вне транзакции)
-- Этот блок должен выполняться с runInTransaction: false в Liquibase
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_live_broadcasts_recorded_at
ON live_broadcasts(recorded_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_live_broadcasts_recorded_size
ON live_broadcasts(recorded_size DESC);
