-- docker/postgres/seed-data.sql
-- Выполнять ВРУЧНУЮ после старта backend (когда Liquibase создаст таблицы):
--   docker exec -i platform-postgres psql -U platform_user -d platform_db < docker/postgres/seed-data.sql

-- Чат-комнаты
INSERT INTO chat_rooms (id, slug, name, type, is_active, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'general', 'Общий чат', 'general', true, NOW()),
    ('22222222-2222-2222-2222-222222222222', 'random', 'Случайный чат', 'general', true, NOW())
ON CONFLICT (id) DO NOTHING;

-- Пример события
INSERT INTO events (id, title, description, start_time, created_at) VALUES
    ('11111111-1111-1111-1111-111111111112', 'Открытие платформы', 'Добро пожаловать на платформу!', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
