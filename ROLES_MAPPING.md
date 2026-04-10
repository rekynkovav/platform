# Маппинг ролей Frontend ↔ Backend

## Роли во фронтенде → Роли в Keycloak/Backend

| Frontend Role | Keycloak Role | Backend Role | Описание | Права |
|--------------|---------------|--------------|----------|-------|
| `user0` | `USER` | `USER` | Пользователь 0 - Просмотр сайта | Только просмотр help |
| `user1` | `PREMIUM` | `PREMIUM` | Пользователь 1 - Новички + Чат 1 | Базовый контент + Чат 1 |
| `user2` | `ADVANCED` | `ADVANCED` | Пользователь 2 - Продвинутые + Чат 2 | Расширенный контент + Чат 2 |
| `admin-moderator` | `MODERATOR` | `MODERATOR` | Админ (модератор) | Модерация + создание контента |
| `admin-consultant` | `CONSULTANT_ADMIN` | `CONSULTANT_ADMIN` | Админ (консультации) | Консультации |
| `admin-nutrition` | `NUTRITION_ADMIN` | `NUTRITION_ADMIN` | Админ (питание) | Лента питания |
| `admin-sport` | `SPORT_ADMIN` | `SPORT_ADMIN` | Админ (спорт) | Спортивный контент |
| `developer` | `CREATOR` | `CREATOR` | Разработчик | Загрузка медиа + разработка |
| `owner` | `OWNER` | `OWNER` | Владелец | Полный доступ |
| `super-admin` | `SUPER_ADMIN` | `SUPER_ADMIN` | Главный Админ | **Скачивание оригиналов** |

## Иерархия ролей (от высшего к низшему)

1. **SUPER_ADMIN** 🔱 - Главный админ
   - Все права OWNER
   - **Единственный, кто может скачивать расшифрованные оригиналы**
   - Критичные операции логируются

2. **OWNER** 👑 - Владелец
   - Полный доступ ко всему
   - Управление всеми пользователями
   - Все админские права

3. **ADMIN** 🛡️ - Администратор
   - Модерация контента
   - Управление пользователями
   - Создание контента

4. **MODERATOR** 🛡️ - Модератор
   - Модерация контента
   - Ответы на вопросы
   - Создание постов

5. **Специализированные админы:**
   - **SPORT_ADMIN** ⚽ - Админ спорт
   - **NUTRITION_ADMIN** 🥗 - Админ питание
   - **CONSULTANT_ADMIN** 💼 - Админ консультации

6. **CREATOR** 💻 - Создатель контента
   - Загрузка медиа файлов
   - Создание постов

7. **ADVANCED** ⭐ - Продвинутый пользователь
   - Доступ к продвинутому контенту
   - Чат 2

8. **PREMIUM** 🌱 - Премиум пользователь
   - Доступ к премиум контенту
   - Чат 1

9. **USER** 👁️ - Базовый пользователь
   - Только просмотр

## Права доступа к медиа контенту

### Просмотр (Stream)
✅ Все роли от USER и выше

### Получение ключей HLS
✅ Все роли от USER и выше

### Загрузка медиа
✅ CREATOR, все админы, OWNER, SUPER_ADMIN

### Скачивание оригиналов (расшифрованных)
❌ **ТОЛЬКО SUPER_ADMIN**

## Настройка в Keycloak

### 1. Создать роли в Realm Roles:

```
USER
PREMIUM
ADVANCED
CREATOR
MODERATOR
ADMIN
SPORT_ADMIN
NUTRITION_ADMIN
CONSULTANT_ADMIN
OWNER
SUPER_ADMIN
```

### 2. Назначить роли пользователям:

**Обычный пользователь:**
- USER

**Премиум пользователь:**
- USER
- PREMIUM

**Продвинутый пользователь:**
- USER
- PREMIUM
- ADVANCED

**Модератор:**
- USER
- MODERATOR

**Админ спорт:**
- USER
- SPORT_ADMIN

**Создатель контента:**
- USER
- CREATOR

**Владелец:**
- USER
- OWNER

**Главный админ:**
- USER
- SUPER_ADMIN

### 3. Composite Roles (опционально):

Можно создать составные роли для удобства:

**PREMIUM** включает:
- USER

**ADVANCED** включает:
- USER
- PREMIUM

**MODERATOR** включает:
- USER
- CREATOR

**OWNER** включает:
- USER
- PREMIUM
- ADVANCED
- CREATOR
- MODERATOR
- ADMIN

**SUPER_ADMIN** включает:
- Все роли

## Проверка ролей в коде

### Backend (Java):

```java
// Получение роли пользователя
String userRole = getUserRole(jwt);

// Проверка админа
boolean isAdmin = isAdmin(userRole);

// Проверка права скачивания оригиналов
boolean canDownload = canDownloadOriginal(userRole); // только SUPER_ADMIN
```

### Frontend (JavaScript):

```javascript
// Получение роли из localStorage
const demoRole = localStorage.getItem('demoRole');

// Проверка админа
const isAdmin = demoRole.includes('admin') || 
                demoRole === 'developer' || 
                demoRole === 'owner' || 
                demoRole === 'super-admin';

// Проверка главного админа
const isSuperAdmin = demoRole === 'super-admin';
```

## Логика защиты

### Frontend:
- **user0**: доступ ТОЛЬКО к вкладке "help"
- **user1**: базовый контент + чат 1
- **user2**: расширенный контент + чат 2
- **admin-***: создание контента, модерация
- **super-admin**: кнопка "Скачать оригинал" (если реализована)

### Backend:
- **USER**: просмотр публичного контента
- **PREMIUM**: просмотр премиум контента
- **ADVANCED**: просмотр продвинутого контента
- **CREATOR**: загрузка медиа
- **Админы**: модерация, управление
- **SUPER_ADMIN**: скачивание расшифрованных оригиналов

## Аудит

Все действия SUPER_ADMIN логируются:

```sql
SELECT * FROM media_access_logs 
WHERE action = 'DOWNLOAD_ORIGINAL' 
ORDER BY accessed_at DESC;
```

Результат:
```
user_id: super-admin-123
action: DOWNLOAD_ORIGINAL
file_id: abc-456
error_message: Role: super-admin
accessed_at: 2026-04-08 10:30:00
```

## Безопасность

### Критично:
- ❗ **SUPER_ADMIN** - самая привилегированная роль
- ❗ Назначать только доверенным лицам
- ❗ Все скачивания оригиналов логируются
- ❗ Регулярно проверять логи на подозрительную активность

### Рекомендации:
1. Минимум пользователей с ролью SUPER_ADMIN (1-2 человека)
2. Двухфакторная аутентификация для SUPER_ADMIN
3. Мониторинг всех действий SUPER_ADMIN
4. Ротация паролей раз в месяц
5. Алерты при скачивании оригиналов

## Миграция существующих пользователей

Если у вас уже есть пользователи:

```sql
-- Обновить роли в Keycloak через Admin API
-- Или вручную через Keycloak Admin Console

-- Пример маппинга:
-- Старая роль "admin" → MODERATOR
-- Старая роль "user" → PREMIUM
-- Владелец → OWNER
-- Главный админ → SUPER_ADMIN
```
