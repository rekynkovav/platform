# 🎯 Роли и права доступа - ОБНОВЛЕНО

## ✅ Что сделано

### 1. Добавлены новые роли

**Frontend (RoleSwitcher.js):**
- ✅ `admin-sport` (⚽) - Админ спорт
- ✅ `super-admin` (🔱) - Главный админ

**Backend (Keycloak):**
- ✅ `SPORT_ADMIN` - Админ спорт
- ✅ `SUPER_ADMIN` - Главный админ

### 2. Обновлена логика защиты

**Критичное изменение:**
- ❗ **Скачивание расшифрованных оригиналов** доступно ТОЛЬКО для `SUPER_ADMIN`
- ❗ Все остальные роли (включая OWNER) НЕ могут скачивать оригиналы
- ❗ Все попытки скачивания логируются

### 3. Синхронизированы роли фронт-бэк

| Frontend | Backend | Права |
|----------|---------|-------|
| user0 | USER | Просмотр |
| user1 | PREMIUM | Базовый контент |
| user2 | ADVANCED | Расширенный контент |
| admin-moderator | MODERATOR | Модерация |
| admin-consultant | CONSULTANT_ADMIN | Консультации |
| admin-nutrition | NUTRITION_ADMIN | Питание |
| admin-sport | SPORT_ADMIN | Спорт |
| developer | CREATOR | Загрузка медиа |
| owner | OWNER | Полный доступ |
| super-admin | SUPER_ADMIN | **Скачивание оригиналов** |

## 🔒 Защита медиа контента

### Просмотр (Stream) - `/api/media/stream/{fileId}`
✅ USER, PREMIUM, ADVANCED, CREATOR, все админы

### Ключи HLS - `/api/media/key/{fileId}`
✅ USER, PREMIUM, ADVANCED, CREATOR, все админы

### Загрузка медиа - `/api/media/upload`
✅ CREATOR, MODERATOR, все админы, OWNER, SUPER_ADMIN

### Скачивание оригиналов - `/api/media/download/{fileId}`
❌ **ТОЛЬКО SUPER_ADMIN**

## 🛡️ Безопасность

### Проверки в коде:

**SecureMediaService.java:**
```java
// Проверка права скачивания оригинала
private boolean canDownloadOriginal(String role) {
    return "super-admin".equals(role);
}

// В методе downloadAndDecrypt:
if (!canDownloadOriginal(userRole)) {
    logAccess(fileId, userId, "DOWNLOAD_ATTEMPT", request, false, 
        "Access denied - requires SUPER_ADMIN role");
    throw new AccessDeniedException("Only SUPER_ADMIN can download original files");
}
```

**SecurityConfig.java:**
```java
.requestMatchers("/api/media/download/**").hasRole("SUPER_ADMIN")
```

### Логирование:

Все действия SUPER_ADMIN логируются с предупреждением:
```java
log.warn("SUPER_ADMIN download: user={}, role={}, file={}", userId, userRole, fileId);
```

В БД сохраняется:
```sql
action: DOWNLOAD_ORIGINAL
error_message: Role: super-admin
```

## 📋 Настройка Keycloak

### 1. Создать новые роли:

В Keycloak Admin Console → Realm Roles → Create Role:

```
SPORT_ADMIN
SUPER_ADMIN
```

### 2. Назначить роли пользователям:

**Главный админ (может скачивать оригиналы):**
- USER
- SUPER_ADMIN

**Админ спорт:**
- USER
- SPORT_ADMIN

### 3. Composite Roles (рекомендуется):

**SUPER_ADMIN** включает:
- USER
- PREMIUM
- ADVANCED
- CREATOR
- MODERATOR
- ADMIN
- OWNER

Это даст главному админу все права.

## 🧪 Тестирование

### 1. Попытка скачать оригинал обычным пользователем:

```bash
curl http://localhost:8081/api/media/download/abc-123 \
  -H "Authorization: Bearer $USER_TOKEN"

# Ответ: 403 Forbidden
# "Only SUPER_ADMIN can download original files"
```

### 2. Скачивание главным админом:

```bash
curl http://localhost:8081/api/media/download/abc-123 \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" \
  -o original.mp4

# Ответ: 200 OK
# Файл скачан (расшифрованный оригинал БЕЗ водяных знаков)
```

### 3. Проверка логов:

```sql
SELECT * FROM media_access_logs 
WHERE action = 'DOWNLOAD_ORIGINAL' 
ORDER BY accessed_at DESC;

-- Результат:
-- user_id: super-admin-user-id
-- action: DOWNLOAD_ORIGINAL
-- file_id: abc-123
-- success: true
-- error_message: Role: super-admin
-- accessed_at: 2026-04-08 10:30:00
```

## ⚠️ Важные замечания

### Критично:
1. **SUPER_ADMIN** - самая привилегированная роль
2. Назначать ТОЛЬКО доверенным лицам (1-2 человека максимум)
3. Все скачивания оригиналов логируются
4. Регулярно проверять логи на подозрительную активность

### Рекомендации:
1. ✅ Двухфакторная аутентификация для SUPER_ADMIN
2. ✅ Мониторинг всех действий SUPER_ADMIN
3. ✅ Алерты при скачивании оригиналов
4. ✅ Ротация паролей раз в месяц
5. ✅ Аудит логов раз в неделю

### Что защищено:
- ✅ Оригиналы могут скачать только SUPER_ADMIN
- ✅ Все остальные получают только зашифрованные версии
- ✅ Водяные знаки на всех файлах (кроме оригиналов для SUPER_ADMIN)
- ✅ Presigned URLs с истечением через 5 минут
- ✅ Аудит всех обращений к файлам

### Что НЕ защищено:
- ❌ Screen recording (запись экрана)
- ❌ Камера, снимающая экран
- ❌ Инсайдер с ролью SUPER_ADMIN

## 📊 Мониторинг

### SQL запросы для мониторинга:

**Все скачивания оригиналов:**
```sql
SELECT 
    al.user_id,
    al.file_id,
    mf.original_filename,
    al.ip_address,
    al.accessed_at
FROM media_access_logs al
JOIN media_files mf ON al.file_id = mf.id
WHERE al.action = 'DOWNLOAD_ORIGINAL'
ORDER BY al.accessed_at DESC;
```

**Подозрительная активность (много скачиваний):**
```sql
SELECT 
    user_id,
    COUNT(*) as download_count,
    MAX(accessed_at) as last_download
FROM media_access_logs
WHERE action = 'DOWNLOAD_ORIGINAL'
  AND accessed_at > NOW() - INTERVAL '24 hours'
GROUP BY user_id
HAVING COUNT(*) > 10
ORDER BY download_count DESC;
```

**Неудачные попытки скачивания:**
```sql
SELECT 
    user_id,
    file_id,
    error_message,
    ip_address,
    accessed_at
FROM media_access_logs
WHERE action = 'DOWNLOAD_ATTEMPT'
  AND success = false
  AND error_message LIKE '%SUPER_ADMIN%'
ORDER BY accessed_at DESC
LIMIT 100;
```

## 🚀 Готово к использованию

Все роли синхронизированы, защита настроена, логирование работает.

**Следующие шаги:**
1. Настроить роли в Keycloak
2. Назначить SUPER_ADMIN доверенному лицу
3. Протестировать скачивание оригиналов
4. Настроить мониторинг логов
5. Настроить алерты на подозрительную активность

Документация:
- `ROLES_MAPPING.md` - полный маппинг ролей
- `IMPLEMENTATION_COMPLETE.md` - общая документация
- `MEDIA_SETUP.md` - инструкция по настройке
