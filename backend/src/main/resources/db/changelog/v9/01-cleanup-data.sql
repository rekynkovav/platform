-- Миграция v9: Очистка данных для первого релиза
-- ИСПРАВЛЕНО: удалены ссылки на несуществующие таблицы (access_logs, media_access_logs)
-- ИСПРАВЛЕНО: все DELETE обёрнуты в DO $$ ... EXCEPTION WHEN undefined_table THEN NULL

DO $$
BEGIN
    -- Удаляем данные в правильном порядке (с учетом foreign keys)
    BEGIN DELETE FROM notifications; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM messages; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM chat_room_permissions; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM chat_rooms; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM live_broadcasts; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM media_files; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM archives; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM reactions; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM comments; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM posts; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM events; EXCEPTION WHEN undefined_table THEN NULL; END;
    BEGIN DELETE FROM users; EXCEPTION WHEN undefined_table THEN NULL; END;
END $$;

-- Сбрасываем последовательности только для таблиц с BIGSERIAL/BIGINT PK
ALTER SEQUENCE IF EXISTS chat_room_permissions_id_seq RESTART WITH 1;

-- Комментарий для истории миграций
COMMENT ON DATABASE platform IS 'Платформа - Первый релиз (чистая БД)';
