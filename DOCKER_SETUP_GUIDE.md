# РУКОВОДСТВО ПО ЗАПУСКУ DOCKER DESKTOP

## ТЕКУЩАЯ СИТУАЦИЯ
✅ Docker установлен  
❌ Docker Desktop Service не запущен  
❌ Не удаётся скачать образы из-за проблем с аутентификацией

## РЕШЕНИЕ

### ШАГ 1: ЗАПУСТИТЕ DOCKER DESKTOP
1. Нажмите **Win + S** и введите "Docker Desktop"
2. Запустите приложение Docker Desktop
3. Дождитесь полной загрузки (иконка в трее станет белой/зелёной)

### ШАГ 2: НАСТРОЙКА АУТЕНТИФИКАЦИИ (если потребуется)
1. Откройте Docker Desktop
2. Перейдите в Settings → Docker Engine
3. Убедитесь, что конфигурация содержит:
```json
{
  "credsStore": "desktop",
  "experimental": false
}
```

### ШАГ 3: ПРОВЕРКА РАБОТОСПОСОБНОСТИ
После запуска Docker Desktop выполните в терминале:

```bash
# Проверка Docker
docker --version

# Проверка Docker Compose  
docker compose version

# Тестовый запуск образа
docker run hello-world
```

### ШАГ 4: ЗАПУСК НАШЕЙ СИСТЕМЫ
Когда Docker Desktop будет работать, выполните:

```bash
cd C:\Users\Suveren\deepseek-cli\platform

# Запуск всех сервисов
docker compose up -d

# Проверка статуса
docker compose ps

# Просмотр логов
docker compose logs -f
```

## АЛЬТЕРНАТИВНЫЙ ВАРИАНТ: WSL 2
Если Docker Desktop продолжает вызывать проблемы, рассмотрите установку WSL 2:

1. Установите WSL 2: `wsl --install`
2. Установите Docker в WSL: `sudo apt update && sudo apt install docker.io docker-compose`
3. Работайте в среде WSL

## КРАТКАЯ КОМАНДА ДЛЯ ЗАПУСКА
Создайте файл `run.bat` на рабочем столе:

```batch
@echo off
cd C:\Users\Suveren\deepseek-cli\platform
docker compose up -d
timeout /t 30
docker compose ps
pause
```

## КОНТАКТНЫЕ ДАННЫЕ СЕРВИСОВ
После успешного запуска будут доступны:

| Сервис | URL | Порт |
|--------|-----|------|
| Backend API | http://localhost:8081 | 8081 |
| Keycloak | http://localhost:8080 | 8080 |
| Frontend | http://localhost:3000 | 3000 |
| Prometheus | http://localhost:9090 | 9090 |
| Grafana | http://localhost:3001 | 3001 |
| Kibana | http://localhost:5601 | 5601 |
| Zipkin | http://localhost:9411 | 9411 |
| Elasticsearch | http://localhost:9200 | 9200 |
| Nginx | http://localhost:80 | 80 |
| RTMP | rtmp://localhost:1935 | 1935 |

## УСТРАНЕНИЕ НЕПОЛАДОК

### Проблема: "docker-credential-desktop not found"
**Решение:** Переустановите Docker Desktop или обновите PATH

### Проблема: Сервисы не запускаются
**Решение:** Проверьте логи: `docker compose logs [service_name]`

### Проблема: Порты заняты
**Решение:** Измените порты в `docker-compose.yml` или освободите порты

### Проблема: Не хватает памяти
**Решение:** В Docker Desktop увеличьте лимит RAM (Settings → Resources)

---
**Статус системы:** Конфигурация готова, требуется запуск Docker Desktop  
**Следующие шаги:** Запустите Docker Desktop → Выполните `docker compose up -d` → Проведите тестирование