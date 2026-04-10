# 🎯 Роли и права доступа

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

Все действия SUPER_ADMIN логируются с предупреждением:
```java
log.warn("SUPER_ADMIN download: user={}, role={}, file={}", userId, userRole, fileId);
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
