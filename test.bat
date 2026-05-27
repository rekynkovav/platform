@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   PLATFORM - ТЕСТИРОВАНИЕ СИСТЕМЫ
echo ========================================
echo.

echo [1/6] Проверка окружения...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ✗ Docker не доступен
    pause
    exit /b 1
)
echo ✓ Docker доступен

echo [2/6] Проверка сервисов...
docker-compose -f docker-compose.yml ps | findstr "Up" >nul
if errorlevel 1 (
    echo ✗ Сервисы не запущены
    echo Запустите систему: start.bat
    pause
    exit /b 1
)
echo ✓ Сервисы запущены

echo [3/6] Тест подключения к PostgreSQL...
docker exec -it platform-postgres-1 pg_isready -U platform_user -h localhost -p 5432 >nul 2>&1
if errorlevel 1 (
    echo ✗ PostgreSQL недоступен
    set error=1
) else (
    echo ✓ PostgreSQL доступен
)

echo [4/6] Тест подключения к Redis...
docker exec -it platform-redis-1 redis-cli ping | findstr "PONG" >nul 2>&1
if errorlevel 1 (
    echo ✗ Redis недоступен
    set error=1
) else (
    echo ✓ Redis доступен
)

echo [5/6] Тест health check backend...
curl -s -f http://localhost:8081/actuator/health > health.json 2>nul
if errorlevel 1 (
    echo ✗ Backend недоступен
    set error=1
) else (
    echo ✓ Backend доступен
    type health.json | findstr "UP" >nul
    if errorlevel 1 (
        echo ✗ Backend в состоянии DEGRADED
        set error=1
    ) else (
        echo ✓ Backend в состоянии UP
    )
    del health.json
)

echo [6/6] Тест WebSocket health check...
curl -s -f http://localhost:8081/api/health/websocket > websocket_health.json 2>nul
if errorlevel 1 (
    echo ✗ WebSocket health check недоступен
    set error=1
) else (
    echo ✓ WebSocket health check доступен
    type websocket_health.json | findstr "UP" >nul
    if errorlevel 1 (
        echo ✗ WebSocket в состоянии DEGRADED
        set error=1
    ) else (
        echo ✓ WebSocket в состоянии UP
    )
    del websocket_health.json
)

echo.
echo ========================================
if defined error (
    echo   ТЕСТИРОВАНИЕ ЗАВЕРШЕНО С ОШИБКАМИ
    echo   Проверьте логи: logs.bat
) else (
    echo   ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО
)
echo ========================================
echo.
echo Сводка:
echo   - Docker:          ✓
echo   - Сервисы:         ✓
echo   - PostgreSQL:      %postgres_status%
echo   - Redis:           %redis_status%
echo   - Backend:         %backend_status%
echo   - WebSocket:       %websocket_status%
echo.
pause