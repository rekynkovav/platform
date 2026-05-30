-- Добавление полей для архивации стримов
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS recorded_url VARCHAR(500);
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP;
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS recorded_size BIGINT;
ALTER TABLE live_broadcasts ADD COLUMN IF NOT EXISTS recorded_duration INTEGER;

-- Комментарии к полям
COMMENT ON COLUMN live_broadcasts.recorded_url IS 'URL архива в S3';
COMMENT ON COLUMN live_broadcasts.recorded_at IS 'Дата и время архивации';
COMMENT ON COLUMN live_broadcasts.recorded_size IS 'Размер архива в байтах';
COMMENT ON COLUMN live_broadcasts.recorded_duration IS 'Длительность архива в секундах';

-- Индекс для поиска по дате архивации
CREATE INDEX IF NOT EXISTS idx_live_broadcasts_recorded_at ON live_broadcasts(recorded_at DESC);

-- Индекс для поиска по размеру архива
CREATE INDEX IF NOT EXISTS idx_live_broadcasts_recorded_size ON live_broadcasts(recorded_size DESC);