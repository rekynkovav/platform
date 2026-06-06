# Исправление ошибки миграции Liquibase

## Проблема
Миграция v13 падала с ошибкой foreign key constraint при попытке пересоздать таблицу `notifications` через `DROP TABLE ... CASCADE`.

**Причина:**
- V1 создал таблицу `notifications` с UUID id
- JPA модель `Notification.java` ожидает Long id (BIGSERIAL)
- V13 пытался исправить через DROP/CREATE, но другие таблицы могли иметь foreign keys

## Что сделано

### 1. Удален проблемный файл
- `backend/src/main/resources/db/changelog/v13/01-fix-schema-mismatches.sql` (удалён)

### 2. Создана корректная миграция v13
- `backend/src/main/resources/db/changelog/v13/01-fix-notifications-schema.sql`
- Безопасно пересоздаёт таблицу notifications:
  - Переименовывает старую таблицу
  - Создаёт новую с правильной схемой (BIGSERIAL id, relatedId, relatedRoomSlug)
  - Переносит данные
  - Удаляет старую таблицу

### 3. Обновлён мастер-файл миграций
- `db.changelog-master.xml` теперь указывает на новый файл

### 4. Созданы вспомогательные скрипты
- `scripts/reset-database.sh` - полный сброс БД
- `scripts/run-migrations.sh` - ручной запуск миграций

## Как исправить (для чистой установки)

### Вариант А: Полный сброс (рекомендуется для dev)
 

### Вариант Б: Только выполнить миграцию v13
 

## Проверка
После успешного запуска таблица `notifications` должна иметь структуру:
 

## Примечания
- В production используйте вариант Б (только миграция)
- Для dev-окружения можно использовать полный сброс
- Все данные из старой таблицы notifications сохранятся (user_id, type, title, message, read, created_at)
- Поля `related_id` и `related_room_slug` останутся NULL (нужно заполнить программно)

## Статус
✅ Миграция исправлена
✅ Скрипты созданы
✅ Документация добавлена

Теперь backend должен запускаться без ошибок.
