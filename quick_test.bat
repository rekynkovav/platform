@echo off
chcp 65001 >nul
echo ==========================================
echo    БЫСТРЫЙ ТЕСТ СИСТЕМЫ
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
echo ШАГ 3: Проверка образов...
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | findstr /i "postgres redis prom grafana"

echo.
echo ШАГ 4: Проверка доступности портов...
echo.

set /a available=0
set /a total=0

echo 1. PostgreSQL (5432)...
netstat -an | findstr ":5432" >nul
if errorlevel 1 (
    echo   ❌ Не слушает
) else (
    echo   ✅ Слушает
    set /a available+=1
)
set /a total+=1

echo.
echo 2. Redis (6379)...
netstat -an | findstr ":6379" >nul
if errorlevel 1 (
    echo   ❌ Не слушает
) else (
    echo   ✅ Слушает
    set /a available+=1
)
set /a total+=1

echo.
echo 3. Prometheus (9090)...
netstat -an | findstr ":9090" >nul
if errorlevel 1 (
    echo   ❌ Не слушает
) else (
    echo   ✅ Слушает
    set /a available+=1
)
set /a total+=1

echo.
echo 4. Grafana (3001)...
netstat -an | findstr ":3001" >nul
if errorlevel 1 (
    echo   ❌ Не слушает
) else (
    echo   ✅ Слушает
    set /a available+=1
)
set /a total+=1

echo.
echo ==========================================
echo          РЕЗУЛЬТАТЫ
echo ==========================================
echo.
echo Доступно портов: %available% из %total%
echo.

if %available% EQU 0 (
    echo ⚠️  Система ещё не запущена
    echo.
    echo Запустите: docker compose -f docker-compose-core.yml up -d
    echo И подождите 2-3 минуты для полной инициализации
) else if %available% LSS %total% (
    echo ⚠️  Система частично запущена
    echo.
    echo Проверьте логи: docker compose -f docker-compose-core.yml logs
) else (
    echo ✅ Система полностью запущена!
    echo.
    echo Доступные сервисы:
    echo - PostgreSQL: localhost:5432
    echo - Redis: localhost:6379
    echo - Prometheus: http://localhost:9090
    echo - Grafana: http://localhost:3001 (admin/admin)
)

echo.
echo ==========================================
echo КОМАНДЫ ДЛЯ УПРАВЛЕНИЯ:
echo.
echo 1. Просмотр логов: docker compose -f docker-compose-core.yml logs -f
echo 2. Остановка: docker compose -f docker-compose-core.yml down
echo 3. Перезапуск: docker compose -f docker-compose-core.yml restart
echo.
pause