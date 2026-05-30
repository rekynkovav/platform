-- docker/postgres/seed.sql
-- Тестовые данные для платформы (выполняется после миграций Liquibase)

-- Чат-комнаты
INSERT INTO chat_rooms (id, slug, name, type, is_active, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'general', 'Общий чат', 'general', true, NOW()),
    ('22222222-2222-2222-2222-222222222222', 'random', 'Случайный чат', 'general', true, NOW())
ON CONFLICT (id) DO NOTHING;

-- Пример события
INSERT INTO events (id, title, description, start_time, created_at) VALUES
    ('11111111-1111-1111-1111-111111111112', 'Открытие платформы', 'Добро пожаловать на платформу!', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Примечание: Администратор создаётся через Keycloak, не через прямую вставку в БД