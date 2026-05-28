# Platform Project

Платформа для онлайн-общения, трансляций и управления подписками.

## Технологии

- **Backend**: Spring Boot 3.2.5, Java 17, PostgreSQL, Redis
- **Frontend**: React 18, Keycloak
- **DevOps**: Docker, Prometheus, Grafana, ELK, Zipkin

## Быстрый старт

### Требования
- Docker Desktop
- Java 17
- Node.js 18+

### Установка

```bash
git clone <repo-url>
cd platform
copy .env.example .env
# Заполните .env файл
```

### Запуск

```bash
# Инициализация (один раз)
init.bat

# Запуск всех сервисов
start.bat

# Остановка
stop.bat
```

### Доступ к сервисам

| Сервис | URL | Логин/Пароль |
|--------|-----|--------------|
| Frontend | http://localhost:3000 | - |
| Backend API | http://localhost:8081 | - |
| Keycloak | http://localhost:8080/auth | admin/admin |
| Grafana | http://localhost:3001 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Kibana | http://localhost:5601 | - |

## Переменные окружения

Создайте файл `.env` со следующими переменными:

- `REDIS_PASSWORD` — пароль Redis
- `DB_PASSWORD` — пароль PostgreSQL
- `KEYCLOAK_ADMIN_PASSWORD` — пароль админа Keycloak

## Документация

- [API Документация](API.md)
- [Деплой](DEPLOYMENT.md)

## Лицензия

MIT 
