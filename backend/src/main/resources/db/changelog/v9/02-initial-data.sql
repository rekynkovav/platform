-- Миграция v9: Начальные данные для первого релиза

-- 1. Создаем тестового пользователя (для Keycloak интеграции)
INSERT INTO users (id, username, email, first_name, last_name, role, is_active, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'testuser',
    'test@example.com',
    'Test',
    'User',
    'USER_0',
    true,
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 2. Создаем базовые комнаты чата
INSERT INTO chat_rooms (id, slug, type, name, description, is_active, created_at, updated_at)
VALUES
    -- Общие чаты
    (gen_random_uuid(), 'user1', 'general', 'Общий чат 1', 'Основной общий чат для всех пользователей', true, NOW(), NOW()),
    (gen_random_uuid(), 'user2', 'general', 'Общий чат 2', 'Дополнительный общий чат', true, NOW(), NOW()),
    (gen_random_uuid(), 'nutrition', 'nutrition', 'Питание', 'Чат по вопросам питания', true, NOW(), NOW()),

    -- Админские чаты
    (gen_random_uuid(), 'admins', 'admin', 'Админский чат', 'Чат для администраторов', true, NOW(), NOW()),

    -- Комнаты вопросов
    (gen_random_uuid(), 'questions-general', 'question', 'Общие вопросы', 'Общие вопросы пользователей', true, NOW(), NOW()),
    (gen_random_uuid(), 'questions-nutrition', 'question', 'Вопросы по питанию', 'Вопросы по питанию и диетам', true, NOW(), NOW()),
    (gen_random_uuid(), 'questions-vyacheslav', 'question', 'Вячеславу', 'Личные вопросы Вячеславу', true, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- 3. Настраиваем права доступа для комнат
DO $$
DECLARE
    room_user1 UUID;
    room_user2 UUID;
    room_nutrition UUID;
    room_admins UUID;
    room_q_general UUID;
    room_q_nutrition UUID;
    room_q_vyacheslav UUID;
BEGIN
    -- Получаем ID комнат
    SELECT id INTO room_user1 FROM chat_rooms WHERE slug = 'user1';
    SELECT id INTO room_user2 FROM chat_rooms WHERE slug = 'user2';
    SELECT id INTO room_nutrition FROM chat_rooms WHERE slug = 'nutrition';
    SELECT id INTO room_admins FROM chat_rooms WHERE slug = 'admins';
    SELECT id INTO room_q_general FROM chat_rooms WHERE slug = 'questions-general';
    SELECT id INTO room_q_nutrition FROM chat_rooms WHERE slug = 'questions-nutrition';
    SELECT id INTO room_q_vyacheslav FROM chat_rooms WHERE slug = 'questions-vyacheslav';

    -- Общие чаты - все пользователи могут читать и писать
    INSERT INTO chat_room_permissions (id, chat_room_id, role, permission_level, created_at, updated_at)
    VALUES
        (gen_random_uuid(), room_user1, 'ROLE_USER_0', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_user1, 'ROLE_USER_1', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_user1, 'ROLE_USER_2', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_user1, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_user1, 'ROLE_MODERATOR_ADMIN', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_user1, 'ROLE_VYACHESLAV', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_user1, 'ROLE_SUPER_ADMIN', 'admin', NOW(), NOW()),

        (gen_random_uuid(), room_user2, 'ROLE_USER_0', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_user2, 'ROLE_USER_1', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_user2, 'ROLE_USER_2', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_user2, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_user2, 'ROLE_MODERATOR_ADMIN', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_user2, 'ROLE_VYACHESLAV', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_user2, 'ROLE_SUPER_ADMIN', 'admin', NOW(), NOW()),

    -- Чат по питанию - все пользователи + консультант питания
        (gen_random_uuid(), room_nutrition, 'ROLE_USER_0', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_nutrition, 'ROLE_USER_1', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_nutrition, 'ROLE_USER_2', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_nutrition, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_nutrition, 'ROLE_MODERATOR_ADMIN', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_nutrition, 'ROLE_VYACHESLAV', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_nutrition, 'ROLE_SUPER_ADMIN', 'admin', NOW(), NOW()),

    -- Админский чат - только админы
        (gen_random_uuid(), room_admins, 'ROLE_MODERATOR_ADMIN', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_admins, 'ROLE_VYACHESLAV', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_admins, 'ROLE_SUPER_ADMIN', 'read_write', NOW(), NOW()),

    -- Общие вопросы - все пользователи могут читать и писать
        (gen_random_uuid(), room_q_general, 'ROLE_USER_0', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_general, 'ROLE_USER_1', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_general, 'ROLE_USER_2', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_general, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_q_general, 'ROLE_MODERATOR_ADMIN', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_q_general, 'ROLE_VYACHESLAV', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_q_general, 'ROLE_SUPER_ADMIN', 'admin', NOW(), NOW()),

    -- Вопросы по питанию - все пользователи + консультант питания
        (gen_random_uuid(), room_q_nutrition, 'ROLE_USER_0', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_nutrition, 'ROLE_USER_1', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_nutrition, 'ROLE_USER_2', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_nutrition, 'ROLE_CONSULTANT_PITANIE', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_q_nutrition, 'ROLE_MODERATOR_ADMIN', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_q_nutrition, 'ROLE_VYACHESLAV', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_q_nutrition, 'ROLE_SUPER_ADMIN', 'admin', NOW(), NOW()),

    -- Вопросы Вячеславу - все пользователи + админы
        (gen_random_uuid(), room_q_vyacheslav, 'ROLE_USER_0', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_vyacheslav, 'ROLE_USER_1', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_vyacheslav, 'ROLE_USER_2', 'read_write', NOW(), NOW()),
        (gen_random_uuid(), room_q_vyacheslav, 'ROLE_MODERATOR_ADMIN', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_q_vyacheslav, 'ROLE_VYACHESLAV', 'admin', NOW(), NOW()),
        (gen_random_uuid(), room_q_vyacheslav, 'ROLE_SUPER_ADMIN', 'admin', NOW(), NOW())
    ON CONFLICT (chat_room_id, role) DO UPDATE SET
        permission_level = EXCLUDED.permission_level,
        updated_at = NOW();
END $$;

-- 4. Создаем тестовые сообщения
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
    (SELECT id FROM chat_rooms WHERE slug = 'user1'),
    'user1',
    false,
    NOW() - (i * interval '1 minute'),
    NOW() - (i * interval '1 minute')
FROM generate_series(1, 5) i;

-- 5. Логируем создание начальных данных
INSERT INTO access_logs (id, user_id, action, resource_type, resource_id, ip_address, user_agent, created_at)
VALUES (
    gen_random_uuid(),
    '550e8400-e29b-41d4-a716-446655440000',
    'INITIAL_DATA_LOAD',
    'SYSTEM',
    'v9_migration',
    '127.0.0.1',
    'Liquibase Migration',
    NOW()
);

-- Комментарий для завершения миграции
COMMENT ON TABLE chat_rooms IS 'Комнаты чата платформы - инициализированы для первого релиза';
COMMENT ON TABLE chat_room_permissions IS 'Права доступа к комнатам - настроены для всех ролей';