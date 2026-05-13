# Platform Project

Платформа для спортивного сообщества с функциями стриминга, социальных сетей и архива событий.

## Стек технологий

### Backend
- Java 17
- Spring Boot 3.x
- Spring Security (Keycloak)
- Spring Data JPA
- PostgreSQL
- Liquibase
- Redis
- AWS S3 (Media Storage)
- WebSocket (Chat)

### Frontend
- React 18
- TypeScript
- Tailwind CSS
- React Query
- Axios
- Keycloak React Adapter

## Новые возможности (v2)

### Обновление системы ролей
- Заменены роли: `admin` → `MODERATOR_ADMIN`, `super_admin` → `SUPER_ADMIN`, `vyacheslav` → `VYACHESLAV`
- Обновлены все контроллеры и сервисы
- Обновлена база данных (Liquibase changelog)

### Безопасность медиа
- Шифрование файлов (AES-256)
- Водяные знаки для видео и изображений
- HLS стриминг
- Контроль доступа по уровням (public, premium, private)

### Улучшенная модерация
- Автоматическое флаггирование контента
- Быстрая модерация постов и комментариев
- Логирование действий модераторов

## Структура проекта

```
platform/
├── backend/          # Spring Boot приложение
├── frontend/         # React приложение
├── docker/           # Docker конфигурация
└── scripts/          # Скрипты инициализации
```

## Запуск

### Локально
1. Запустить Keycloak и PostgreSQL
2. `cd backend && ./gradlew bootRun`
3. `cd frontend && npm start`

### Docker
```bash
cd docker
./start.sh
```

## Разработка

### Создание новой ветки
```bash
./pushNewBranch.bat
```

### Сборка
```bash
./gradlew build
```

## Лицензия

MIT
# New Features (10-05-2026 23-37-07) 
# New Features (10-05-2026 23-41-15) 
# New Features (13-05-2026 08-58-27) 
