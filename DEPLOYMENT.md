# Deployment Guide

## Требования
- Docker 20.10+
- Docker Compose 2.0+
- 4GB RAM минимум

## Переменные окружения
Создайте `.env` файл:
```
REDIS_PASSWORD=secure_password
DB_PASSWORD=secure_password
KEYCLOAK_ADMIN_PASSWORD=secure_password
```

## Запуск production

```bash
# Сборка образов
docker build -t platform-backend:latest -f backend/Dockerfile ./backend
docker build -t platform-frontend:latest -f frontend/Dockerfile ./frontend

# Запуск всех сервисов
docker-compose -f docker-compose.yml up -d

# Проверка здоровья
curl http://localhost:8081/actuator/health
```

## Rollback процедура

```bash
# Откат на предыдущую версию
docker-compose down
docker tag platform-backend:previous platform-backend:latest
docker-compose up -d
```

## Мониторинг
- Grafana: http://localhost:3001 (admin/admin)
- Prometheus: http://localhost:9090
- Kibana: http://localhost:5601