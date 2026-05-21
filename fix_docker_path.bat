@echo off
echo ===== НАСТРОЙКА PATH ДЛЯ DOCKER =====
echo.

echo Текущий PATH для Docker:
where docker
echo.

echo Добавляем Docker в PATH...
set PATH=C:\Program Files\Docker\Docker\resources\bin;%PATH%

echo.
echo Проверка Docker...
docker --version
if errorlevel 1 (
    echo ❌ Docker не найден после добавления в PATH
    pause
    exit /b 1
)

echo.
echo Проверка docker-credential-desktop...
where docker-credential-desktop
if errorlevel 1 (
    echo ❌ docker-credential-desktop не найден
) else (
    echo ✅ docker-credential-desktop найден
)

echo.
echo Тест скачивания образа...
docker pull hello-world
if errorlevel 0 (
    echo ✅ Образ успешно скачан
    echo.
    echo Запуск тестового контейнера...
    docker run --rm hello-world
) else (
    echo ❌ Ошибка при скачивании образа
)

echo.
echo ===== ИНСТРУКЦИИ =====
echo.
echo 1. Для постоянного добавления Docker в PATH:
echo    - ПКМ на "Этот компьютер" → Свойства
echo    - Дополнительные параметры системы → Переменные среды
echo    - В "Системные переменные" найдите PATH
echo    - Добавьте: C:\Program Files\Docker\Docker\resources\bin
echo.
echo 2. Перезапустите терминал после изменения PATH
echo.
echo 3. После настройки PATH запустите:
echo    docker compose up -d
echo.
pause