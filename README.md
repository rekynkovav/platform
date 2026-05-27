# Platform Project

## Технологический стек
- Backend: Spring Boot 3.2.5, Java 17, PostgreSQL, Redis
- Frontend: React 18, Keycloak
- DevOps: Docker, Prometheus, Grafana, ELK

## Быстрый старт

### Требования
- Docker Desktop
- Java 17
- Node.js 18+

### Запуск
```bash
# Клонирование
git clone <repo-url>
cd platform

# Запуск инфраструктуры
docker-compose up -d

# Запуск backend
cd backend
./mvnw spring-boot:run

# Запуск frontend
cd frontend
npm start
```

## Переменные окружения
Скопируйте `.env.example` в `.env` и заполните значения.

## API Документация
Swagger UI: http://localhost:8081/swagger-ui.html 
