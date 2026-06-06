-- Миграция v9: Начальные данные для первого релиза
-- ИСПРАВЛЕНО:
-- 1. Убраны несуществующие колонки first_name, last_name, is_active в users
-- 2. Исправлены имена колонок в chat_room_permissions: room_id (не chat_room_id), id BIGSERIAL (не UUID)
-- 3. Добавлена колонка updated_at в INSERT для messages (согласовано с v7 и Message.java)
-- 4. Фиксированные UUID для комнат чата вместо gen_random_uuid() для идемпотентности
-- 5. Удалён INSERT INTO access_logs (таблица не существует, есть только media_access_logs из v3)
-- 6. Заменён ON CONFLICT (room_id, role) на DELETE+INSERT, т.к. idx_room_permissions_room_role — обычный индекс, не UNIQUE

-- 1. Создаем тестового пользователя (для Keycloak интеграции)
INSERT INTO users (id, username, email, role, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'testuser',
    'test@example.com',
    'USER_0',
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 2. Создаем базовые комнаты чата с ФИКСИРОВАННЫМИ UUID для согласованности
INSERT INTO chat_rooms (id, slug, type, name, description, is_active, version, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000009', 'user1', 'general', 'Общий чат 1', 'Основной общий чат для всех пользователей', true, 0, NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000010', 'user2', 'general', 'Общий чат 2', 'Дополнительный общий чат', true, 0, NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000011', 'nutrition', 'nutrition', 'Питание', 'Чат по вопросам питания', true, 0, NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000012', 'admins', 'admin', 'Админский чат', 'Чат для администраторов', true, 0, NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000006', 'questions-general', 'question', 'Общие вопросы', 'Общие вопросы пользователей', true, 0, NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000007', 'questions-nutrition', 'question', 'Вопросы по питанию', 'Вопросы по питанию и диетам', true, 0, NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000008', 'questions-vyacheslav', 'question', 'Вячеславу', 'Личные вопросы Вячеславу', true, 0, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    type = EXCLUDED.type,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active,
    updated_at = NOW();

-- 3. Настраиваем права доступа для комнат
-- ВНИМАНИЕ: idx_room_permissions_room_role — обычный индекс (не UNIQUE), поэтому ON CONFLICT невозможен.
-- Используем DELETE + INSERT для идемпотентности.
DO $$
DECLARE
    room_user1 UUID := 'a0000000-0000-0000-0000-000000000009';
    room_user2 UUID := 'a0000000-0000-0000-0000-000000000010';
    room_nutrition UUID := 'a0000000-0000-0000-0000-000000000011';
    room_admins UUID := 'a0000000-0000-0000-0000-000000000012';
    room_q_general UUID := 'a0000000-0000-0000-0000-000000000006';
    room_q_nutrition UUID := 'a0000000-0000-0000-0000-000000000007';
    room_q_vyacheslav UUID := 'a0000000-0000-0000-0000-000000000008';
BEGIN
    -- Удаляем старые права для этих комнат (идемпотентность)
    DELETE FROM chat_room_permissions WHERE room_id IN (
        room_user1, room_user2, room_nutrition, room_admins,
        room_q_general, room_q_nutrition, room_q_vyacheslav
    );

    -- Вставляем новые права
    INSERT INTO chat_room_permissions (room_id, role, permission_level, created_at)
    VALUES
        (room_user1, 'ROLE_USER_0', 'read_write', NOW()),
        (room_user1, 'ROLE_USER_1', 'read_write', NOW()),
        (room_user1, 'ROLE_USER_2', 'read_write', NOW()),
        (room_user1, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW()),
        (room_user1, 'ROLE_MODERATOR_ADMIN', 'admin', NOW()),
        (room_user1, 'ROLE_VYACHESLAV', 'admin', NOW()),
        (room_user1, 'ROLE_SUPER_ADMIN', 'admin', NOW()),

        (room_user2, 'ROLE_USER_0', 'read_write', NOW()),
        (room_user2, 'ROLE_USER_1', 'read_write', NOW()),
        (room_user2, 'ROLE_USER_2', 'read_write', NOW()),
        (room_user2, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW()),
        (room_user2, 'ROLE_MODERATOR_ADMIN', 'admin', NOW()),
        (room_user2, 'ROLE_VYACHESLAV', 'admin', NOW()),
        (room_user2, 'ROLE_SUPER_ADMIN', 'admin', NOW()),

        (room_nutrition, 'ROLE_USER_0', 'read_write', NOW()),
        (room_nutrition, 'ROLE_USER_1', 'read_write', NOW()),
        (room_nutrition, 'ROLE_USER_2', 'read_write', NOW()),
        (room_nutrition, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW()),
        (room_nutrition, 'ROLE_MODERATOR_ADMIN', 'admin', NOW()),
        (room_nutrition, 'ROLE_VYACHESLAV', 'admin', NOW()),
        (room_nutrition, 'ROLE_SUPER_ADMIN', 'admin', NOW()),

        (room_admins, 'ROLE_MODERATOR_ADMIN', 'read_write', NOW()),
        (room_admins, 'ROLE_VYACHESLAV', 'read_write', NOW()),
        (room_admins, 'ROLE_SUPER_ADMIN', 'read_write', NOW()),

        (room_q_general, 'ROLE_USER_0', 'read_write', NOW()),
        (room_q_general, 'ROLE_USER_1', 'read_write', NOW()),
        (room_q_general, 'ROLE_USER_2', 'read_write', NOW()),
        (room_q_general, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW()),
        (room_q_general, 'ROLE_MODERATOR_ADMIN', 'admin', NOW()),
        (room_q_general, 'ROLE_VYACHESLAV', 'admin', NOW()),
        (room_q_general, 'ROLE_SUPER_ADMIN', 'admin', NOW()),

        (room_q_nutrition, 'ROLE_USER_0', 'read_write', NOW()),
        (room_q_nutrition, 'ROLE_USER_1', 'read_write', NOW()),
        (room_q_nutrition, 'ROLE_USER_2', 'read_write', NOW()),
        (room_q_nutrition, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW()),
        (room_q_nutrition, 'ROLE_MODERATOR_ADMIN', 'admin', NOW()),
        (room_q_nutrition, 'ROLE_VYACHESLAV', 'admin', NOW()),
        (room_q_nutrition, 'ROLE_SUPER_ADMIN', 'admin', NOW()),

        (room_q_vyacheslav, 'ROLE_USER_0', 'read_write', NOW()),
        (room_q_vyacheslav, 'ROLE_USER_1', 'read_write', NOW()),
        (room_q_vyacheslav, 'ROLE_USER_2', 'read_write', NOW()),
        (room_q_vyacheslav, 'ROLE_MODERATOR_ADMIN', 'admin', NOW()),
        (room_q_vyacheslav, 'ROLE_VYACHESLAV', 'admin', NOW()),
        (room_q_vyacheslav, 'ROLE_SUPER_ADMIN', 'admin', NOW());
END $$;

-- 4. Создаем тестовые сообщения
-- ВНИМАНИЕ: таблица messages партиционирована по created_at (v7), PK = (id, created_at)
INSERT INTO messages (id, user_id, user_name, content, chat_type, room_id, room_slug, is_read, created_at, updated_at)
SELECT
    gen_random_uuid(),
    '550e8400-e29b-41d4-a716-446655440000',
    'Test User',
    CASE
        WHEN i = 1 THEN 'Добро пожаловать в платформу!'
        WHEN i = 2 THEN 'Это тестовое сообщение в общем чате.'
        WHEN i = 3 THEN 'Здесь можно обсуждать различные темы.'
        WHEN i = 4 THEN 'Система ролей позволяет гибко управлять доступом.'
        WHEN i = 5 THEN 'Попробуйте отправить свое первое сообщение!'
    END,
    'general',
    'a0000000-0000-0000-0000-000000000009',
    'user1',
    false,
    NOW() - (i * interval '1 minute'),
    NOW() - (i * interval '1 minute')
FROM generate_series(1, 5) i
ON CONFLICT (id, created_at) DO NOTHING;

COMMENT ON TABLE chat_rooms IS 'Комнаты чата платформы - инициализированы для первого релиза';
COMMENT ON TABLE chat_room_permissions IS 'Права доступа к комнатам - настроены для всех ролей';
