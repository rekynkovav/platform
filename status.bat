@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   PLATFORM - СТАТУС СИСТЕМЫ
echo ========================================
echo.

echo [1/4] Проверка Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ✗ Docker не доступен
    goto :end
)
echo ✓ Docker доступен

echo [2/4] Статус контейнеров...
echo.
docker-compose -f docker-compose.yml ps

echo.
echo [3/4] Проверка подключений...
echo.

REM Проверка PostgreSQL
echo - PostgreSQL:
timeout /t 1 /nobreak >nul
docker exec -it platform-postgres-1 pg_isready -U platform_user >nul 2>&1
if errorlevel 1 (
    echo   ✗ Недоступен
) else (
    echo   ✓ Доступен
)

REM Проверка Redis
echo - Redis:
timeout /t 1 /nobreak >nul
docker exec -it platform-redis-1 redis-cli ping >nul 2>&1
if errorlevel 1 (
    echo   ✗ Недоступен
) else (
    echo   ✓ Доступен
)

REM Проверка Backend
echo - Backend:
timeout /t 1 /nobreak >nul
curl -s -f http://localhost:8081/actuator/health >nul 2>&1
if errorlevel 1 (
    echo   ✗ Недоступен
) else (
    echo   ✓ Доступен
)

echo.
echo [4/4] Использование ресурсов...
echo.
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" platform-backend-1 platform-postgres-1 platform-redis-1 2>nul || echo "   Контейнеры не запущены"

:end
echo.
echo ========================================
echo   КОНЕЦ ПРОВЕРКИ
echo ========================================
echo.
echo Для запуска: start.bat
echo Для логов:   logs.bat
echo Для остановки: stop.bat
echo.
pause