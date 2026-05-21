@echo off
chcp 65001 >nul
echo ==========================================
echo    ТЕСТИРОВАНИЕ СИСТЕМЫ
echo ==========================================
echo Дата: %date% %time%
echo.

echo ШАГ 1: Проверка Docker...
docker --version
if errorlevel 1 (
    echo ❌ Docker не найден
    pause
    exit /b 1
)

echo.
echo ШАГ 2: Проверка запущенных контейнеров...
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo ШАГ 3: Проверка доступности сервисов...
echo.

echo 1. Проверка Keycloak (порт 8080)...
curl -s -o nul -w "%%{http_code}" http://localhost:8080
if errorlevel 0 (
    echo ✅ Keycloak доступен
) else (
    echo ❌ Keycloak не доступен
)

echo.
echo 2. Проверка Backend API (порт 8081)...
curl -s -o nul -w "%%{http_code}" http://localhost:8081/api/health
if errorlevel 0 (
    echo ✅ Backend API доступен
) else (
    echo ❌ Backend API не доступен
)

echo.
echo 3. Проверка Prometheus (порт 9090)...
curl -s -o nul -w "%%{http_code}" http://localhost:9090
if errorlevel 0 (
    echo ✅ Prometheus доступен
) else (
    echo ❌ Prometheus не доступен
)

echo.
echo 4. Проверка Grafana (порт 3001)...
curl -s -o nul -w "%%{http_code}" http://localhost:3001
if errorlevel 0 (
    echo ✅ Grafana доступен
) else (
    echo ❌ Grafana не доступен
)

echo.
echo ==========================================
echo          РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ
echo ==========================================
echo.
echo Если все сервисы показывают "не доступен", значит:
echo 1. Docker Compose не запущен
echo 2. Нужно выполнить: docker compose up -d
echo.
echo Если Docker Compose выдает ошибку с аутентификацией:
echo 1. Перезапустите Docker Desktop
echo 2. Проверьте настройки Docker Engine
echo.
echo ==========================================
echo КОМАНДЫ ДЛЯ ЗАПУСКА:
echo.
echo 1. Остановить всё: docker compose down
echo 2. Запустить всё: docker compose up -d
echo 3. Просмотр логов: docker compose logs -f
echo 4. Проверка статуса: docker compose ps
echo.
pause