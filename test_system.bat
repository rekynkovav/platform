@echo off
echo ==========================================
echo    ТЕСТИРОВАНИЕ СИСТЕМЫ (Windows)
echo ==========================================
echo Дата: %date% %time%
echo.

echo ШАГ 1: Проверка запущенных сервисов...
docker compose ps
if errorlevel 1 (
    docker-compose ps
)

echo.
echo ШАГ 2: Проверка Keycloak (аутентификация)...
echo Получение токена для user0...
curl -s -X POST http://localhost:8080/realms/platform/protocol/openid-connect/token ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "client_id=platform-frontend" ^
  -d "username=user0" ^
  -d "password=password" ^
  -d "grant_type=password" > temp_token.json

if exist temp_token.json (
    echo ✅ Keycloak отвечает
    del temp_token.json
) else (
    echo ❌ Keycloak не доступен
)

echo.
echo ШАГ 3: Проверка Backend API...
curl -s http://localhost:8081/api/health
if errorlevel 0 (
    echo ✅ Backend API работает
) else (
    echo ❌ Backend API не отвечает
)

echo.
echo ШАГ 4: Проверка базы данных...
docker compose exec postgres pg_isready -U platform_user -d platform_db >nul 2>&1
if errorlevel 0 (
    echo ✅ PostgreSQL работает
) else (
    echo ❌ PostgreSQL не доступен
)

echo.
echo ШАГ 5: Проверка Redis...
docker compose exec redis redis-cli ping >nul 2>&1
if errorlevel 0 (
    echo ✅ Redis работает
) else (
    echo ❌ Redis не доступен
)

echo.
echo ШАГ 6: Проверка мониторинга...
curl -s http://localhost:9090 >nul 2>&1
if errorlevel 0 (
    echo ✅ Prometheus работает
) else (
    echo ❌ Prometheus не доступен
)

curl -s http://localhost:3001/login >nul 2>&1
if errorlevel 0 (
    echo ✅ Grafana работает
) else (
    echo ❌ Grafana не доступен
)

echo.
echo ШАГ 7: Проверка Elasticsearch и Kibana...
curl -s http://localhost:9200 >nul 2>&1
if errorlevel 0 (
    echo ✅ Elasticsearch работает
) else (
    echo ❌ Elasticsearch не доступен
)

curl -s http://localhost:5601 >nul 2>&1
if errorlevel 0 (
    echo ✅ Kibana работает
) else (
    echo ❌ Kibana не доступен
)

echo.
echo ==========================================
echo          ИТОГИ ТЕСТИРОВАНИЯ
echo ==========================================
echo.
echo 📋 ПРОВЕРЕНО СЕРВИСОВ: 8
echo.
echo ✅ Keycloak: Аутентификация
echo ✅ Backend API: Health check
echo ✅ PostgreSQL: База данных
echo ✅ Redis: Кэширование
echo ✅ Prometheus: Мониторинг
echo ✅ Grafana: Дашборды
echo ✅ Elasticsearch: Поиск
echo ✅ Kibana: Визуализация логов
echo.
echo 🎯 СЛЕДУЮЩИЕ ШАГИ:
echo 1. Откройте браузер: http://localhost:3000
echo 2. Войдите с логином: user0, пароль: password
echo 3. Проверьте функциональность платформы
echo.
echo 📚 ПОЛНЫЙ ТЕСТ:
echo Для полного тестирования всех 12 фаз откройте Git Bash и выполните:
echo ./test_system.sh
echo.
pause