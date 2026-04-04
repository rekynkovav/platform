-- docker/postgres/init.sql
-- Создание расширений
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Настройка кодировки
SET client_encoding = 'UTF8';

-- Создание схемы для приложения
CREATE SCHEMA IF NOT EXISTS platform;

-- Настройка прав
GRANT ALL PRIVILEGES ON DATABASE platform_db TO platform_user;
GRANT ALL PRIVILEGES ON SCHEMA platform TO platform_user;