-- docker/postgres/init.sql
-- Создание расширений
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Настройка кодировки
SET client_encoding = 'UTF8';

-- Настройка прав
GRANT ALL PRIVILEGES ON DATABASE platform_db TO platform_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO platform_user;