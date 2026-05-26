@echo off
echo ==========================================
echo    ЗАПУСК СИСТЕМЫ ПОСЛЕ АКТИВАЦИИ DOCKER
echo ==========================================
echo.

echo ШАГ 1: Проверка Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker не найден. Запустите Docker Desktop!
    pause
    exit /b 1
)
echo ✅ Docker доступен

echo.
echo ШАГ 2: Остановка предыдущих контейнеров...
docker compose down >nul 2>&1
if errorlevel 1 (
    echo ⚠️  Используем старый синтаксис...
    docker-compose down >nul 2>&1
)
echo ✅ Контейнеры остановлены

echo.
echo ШАГ 3: Запуск всех сервисов (это займет несколько минут)...
echo Скачивание образов...
docker compose up -d
if errorlevel 1 (
    echo ⚠️  Используем старый синтаксис...
    docker-compose up -d
)

echo.
echo ШАГ 4: Ожидание инициализации (30 секунд)...
timeout /t 30 /nobreak >nul

echo.
echo ШАГ 5: Проверка статуса...
docker compose ps
if errorlevel 1 (
    docker-compose ps
)

echo.
echo ==========================================
echo          СИСТЕМА ЗАПУЩЕНА!
echo ==========================================
echo.
echo 📊 ДОСТУПНЫЕ СЕРВИСЫ:
echo.
echo 1.  Backend API:    http://localhost:8081
echo 2.  Keycloak:       http://localhost:8080
echo 3.  Frontend:       http://localhost:3000
echo 4.  Prometheus:     http://localhost:9090
echo 5.  Grafana:        http://localhost:3001
echo 6.  Kibana:         http://localhost:5601
echo 7.  Zipkin:         http://localhost:9411
echo 8.  Elasticsearch:  http://localhost:9200
echo 9.  Nginx:          http://localhost:80
echo 10. RTMP:           rtmp://localhost:1935
echo.
echo 🔧 КОМАНДЫ ДЛЯ УПРАВЛЕНИЯ:
echo.
echo Просмотр логов:    docker compose logs -f
echo Остановка:         docker compose down
echo Перезапуск:        docker compose restart
echo.
echo 🧪 ДЛЯ ТЕСТИРОВАНИЯ:
echo Запустите: test_system.sh (в Git Bash) или test_system.bat
echo.
pause