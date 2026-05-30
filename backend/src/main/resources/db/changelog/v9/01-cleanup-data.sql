-- Миграция v9: Очистка данных для первого релиза
-- Удаляем все данные, но сохраняем структуру таблиц

-- Отключаем триггеры для ускорения
ALTER TABLE messages DISABLE TRIGGER ALL;
ALTER TABLE chat_room_permissions DISABLE TRIGGER ALL;
ALTER TABLE media_files DISABLE TRIGGER ALL;
ALTER TABLE access_logs DISABLE TRIGGER ALL;

-- Удаляем данные в правильном порядке (с учетом foreign keys)
DELETE FROM access_logs;
DELETE FROM messages;
DELETE FROM chat_room_permissions;
DELETE FROM chat_rooms;
DELETE FROM media_files;
DELETE FROM users;

-- Включаем триггеры обратно
ALTER TABLE messages ENABLE TRIGGER ALL;
ALTER TABLE chat_room_permissions ENABLE TRIGGER ALL;
ALTER TABLE media_files ENABLE TRIGGER ALL;
ALTER TABLE access_logs ENABLE TRIGGER ALL;

-- Сбрасываем последовательности
ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS messages_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS media_files_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS access_logs_id_seq RESTART WITH 1;

-- Комментарий для истории миграций
COMMENT ON DATABASE platform IS 'Платформа - Первый релиз (чистая БД)';