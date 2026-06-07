-- Миграция v9: Начальные данные для первого релиза
-- ИСПРАВЛЕНО: gen_random_uuid() заменё�� на фиксированные UUID для сообщений,
-- чтобы избежать дублирова��ия при повторном запуске.

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
-- Используем DELETE + INSERT для идемпотентности (индекс не UNIQUE)
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
    DELETE FROM chat_room_permissions WHERE room_id IN (
        room_user1, room_user2, room_nutrition, room_admins,
        room_q_general, room_q_nutrition, room_q_vyacheslav
    );

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

-- 4. Создаем тестовые сообщения с ФИКСИРОВАННЫМИ UUID
INSERT INTO messages (id, user_id, user_name, content, chat_type, room_id, room_slug, is_read, created_at, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'Test User', 'Добро пожаловать в платформу!', 'general', 'a0000000-0000-0000-0000-000000000009', 'user1', false, NOW() - interval '5 minutes', NOW() - interval '5 minutes'),
    ('b0000000-0000-0000-0000-000000000002', '550e8400-e29b-41d4-a716-446655440000', 'Test User', 'Это тестовое сообщение в общем чате.', 'general', 'a0000000-0000-0000-0000-000000000009', 'user1', false, NOW() - interval '4 minutes', NOW() - interval '4 minutes'),
    ('b0000000-0000-0000-0000-000000000003', '550e8400-e29b-41d4-a716-446655440000', 'Test User', 'Здесь можно обсуждать различные темы.', 'general', 'a0000000-0000-0000-0000-000000000009', 'user1', false, NOW() - interval '3 minutes', NOW() - interval '3 minutes'),
    ('b0000000-0000-0000-0000-000000000004', '550e8400-e29b-41d4-a716-446655440000', 'Test User', 'Система ролей позволяет гибко управлять доступом.', 'general', 'a0000000-0000-0000-0000-000000000009', 'user1', false, NOW() - interval '2 minutes', NOW() - interval '2 minutes'),
    ('b0000000-0000-0000-0000-000000000005', '550e8400-e29b-41d4-a716-446655440000', 'Test User', 'Попробуйте отправить свое первое сообщение!', 'general', 'a0000000-0000-0000-0000-000000000009', 'user1', false, NOW() - interval '1 minute', NOW() - interval '1 minute')
ON CONFLICT (id, created_at) DO NOTHING;

COMMENT ON TABLE chat_rooms IS 'Комнаты чата платформы - и��ициализированы для первого релиза';
COMMENT ON TABLE chat_room_permissions IS 'Права доступа к комнатам - настроены для всех ролей';
