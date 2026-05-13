# Platform Project - CLAUDE.md

## Project Overview
Full-stack platform with React frontend and Spring Boot backend. Real-time chat, content management, user roles, and WebSocket integration.

## Current Architecture Status (May 2026)

### ✅ COMPLETED - Frontend ↔ Backend Synchronization (Stage H)
**Все localStorage заменены на бэкенд API, кроме необходимых для демо-режима**

#### 🔧 Обновленные файлы:
1. **`frontend/src/keycloak.js`** - Полная переработка с бэкенд-синхронизацией
2. **`frontend/src/services/api.js`** - Единый API клиент с WebSocket
3. **`frontend/src/services/mockApi.js`** - Mock API для демо-режима
4. **`frontend/src/components/Header/Header.js`** - WebSocket интеграция
5. **`frontend/src/components/Dashboard/Dashboard.js`** - Упрощенный дашборд
6. **`frontend/src/components/Chat/useChat.js`** - Чистый хук без localStorage

#### 🎯 Оставшиеся localStorage (только необходимые):
- `demoRole` - для демо-режима переключения ролей
- `token` - для авторизации (JWT)
- `welcomeShown` - для приветственного уведомления
- `userProfilePhoto` - временное фото профиля

#### 🗑️ Удаленные localStorage (будут заменены на API):
- Все посты в фидах (`mainFeedPosts`, `nutritionFeedPosts`)
- Архивные материалы (`archiveMaterials`)
- Фидбэки и вопросы (`userFeedbacks`, `userQuestions`)
- События (`scheduledEvents`)
- Данные дашборда (`dashboardMenuData`)

### 🏗️ Технический стек

#### Frontend (React)
- **React 18.2.0** с функциональными компонентами
- **React Router 6.20.0** для навигации
- **Axios 1.6.0** для HTTP-запросов
- **Keycloak 24.0.0** для аутентификации
- **WebSocket** для реального времени
- **Context API** для глобального состояния

#### Backend (Spring Boot)
- **Spring Boot 3.x** с Java 17
- **Spring Security** с JWT
- **Spring Data JPA** для работы с БД
- **WebSocket (STOMP)** для чата
- **PostgreSQL** как основная БД
- **Redis** для кэширования и сессий

### 📁 Структура проекта

```
platform/
├── frontend/                 # React приложение
│   ├── src/
│   │   ├── components/      # React компоненты
│   │   │   ├── Chat/        # Чат система
│   │   │   ├── Dashboard/   # Дашборд
│   │   │   ├── Feed/        # Ленты контента
│   │   │   ├── Header/      # Хедер с WebSocket
│   │   │   └── ...
│   │   ├── services/        # API сервисы
│   │   ├── hooks/           # Кастомные хуки
│   │   └── ...
│   └── package.json
├── backend/                  # Spring Boot приложение
│   ├── src/main/java/
│   │   ├── controller/      # REST контроллеры
│   │   ├── service/         # Бизнес-логика
│   │   ├── repository/      # JPA репозитории
│   │   ├── model/          # Сущности БД
│   │   ├── config/         # Конфигурации
│   │   └── security/       # Безопасность
│   └── pom.xml
└── CLAUDE.md               # Этот файл
```

### 🚀 Команды запуска

#### Frontend
```bash
cd frontend
npm start                    # Запуск на порту 3000
# или
PORT=3001 npm start         # Запуск на другом порту
```

#### Backend
```bash
cd backend
./mvnw spring-boot:run      # Запуск Spring Boot
```

### 🔌 API Endpoints (основные)

#### Аутентификация
- `POST /api/auth/login` - Вход
- `POST /api/auth/register` - Регистрация
- `GET /api/auth/profile` - Профиль пользователя

#### Чат
- `GET /api/chat/rooms/{slug}/messages` - Получить сообщения
- `POST /api/chat/rooms/{slug}/messages` - Отправить сообщение
- `WebSocket /ws/chat` - Реальный чат

#### Вопросы
- `GET /api/questions/{slug}/messages` - Вопросы
- `POST /api/questions/{slug}/messages` - Задать вопрос

#### Контент
- `GET /api/feed/main` - Основная лента
- `GET /api/feed/nutrition` - Питание
- `GET /api/events` - События
- `GET /api/scheduled` - Запланированные

### 🎭 Демо-режим

#### Роли пользователей:
- `USER_0` - Обычный пользователь
- `USER_1` - Премиум пользователь  
- `USER_2` - Администратор
- `USER_3` - Супер-администратор

#### Переключение ролей:
```javascript
// В компоненте RoleSwitcher
localStorage.setItem('demoRole', 'USER_2');
window.dispatchEvent(new CustomEvent('roleChanged', { detail: 'USER_2' }));
```

### 🔧 Конфигурация

#### Frontend (.env)
```
REACT_APP_API_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080/ws
REACT_APP_KEYCLOAK_URL=http://localhost:8081
```

#### Backend (application.properties)
```
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/platform
spring.redis.host=localhost
spring.redis.port=6379
```

### 📊 Состояние базы данных

#### Основные таблицы:
- `users` - Пользователи
- `chat_messages` - Сообщения чата
- `questions` - Вопросы
- `feed_posts` - Посты ленты
- `events` - События
- `scheduled_events` - Запланированные

### 🚨 Важные заметки

1. **WebSocket работает через Header компонент** - глобальная инициализация
2. **Все данные теперь с бэкенда** - localStorage только для демо
3. **JWT токен хранится в localStorage** для аутентификации
4. **Демо-режим можно отключить** удалив RoleSwitcher
5. **Реальный Keycloak можно подключить** заменив mockApi

### 🔄 Следующие шаги

1. **Завершить Stage H** - удалить оставшиеся localStorage из:
   - `EventsHub.js`, `MainFeed.js`, `NutritionFeed.js`
   - `ScheduledEvents.js`

2. **Настроить реальный бэкенд** - убедиться что все endpoints работают

3. **Добавить тесты** - для критической функциональности

4. **Оптимизировать производительность** - кэширование, lazy loading

### 📞 Контакты и поддержка

- **Проект**: Platform
- **Статус**: Активная разработка
- **Версия**: 0.1.0
- **Дата обновления**: Май 2026