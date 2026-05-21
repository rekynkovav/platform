# PowerShell скрипт для запуска Docker Compose

Write-Host "=== ЗАПУСК DOCKER COMPOSE ===" -ForegroundColor Green
Write-Host "Дата: $(Get-Date)"
Write-Host ""

# Проверяем Docker
Write-Host "Проверка Docker..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker найден: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker не найден. Убедитесь, что Docker Desktop запущен." -ForegroundColor Red
    exit 1
}

# Проверяем Docker Compose
Write-Host "Проверка Docker Compose..." -ForegroundColor Yellow
try {
    $composeVersion = docker compose version
    Write-Host "✅ Docker Compose найден" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Docker Compose не найден, пробуем старый синтаксис..." -ForegroundColor Yellow
    try {
        $composeVersion = docker-compose --version
        Write-Host "✅ Docker Compose (старый синтаксис) найден" -ForegroundColor Green
    } catch {
        Write-Host "❌ Docker Compose не найден" -ForegroundColor Red
        exit 1
    }
}

# Останавливаем предыдущие контейнеры
Write-Host "Останавливаем предыдущие контейнеры..." -ForegroundColor Yellow
try {
    docker compose down 2>$null
    Write-Host "✅ Предыдущие контейнеры остановлены" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Не удалось остановить контейнеры (возможно, их не было)" -ForegroundColor Yellow
}

# Запускаем все сервисы
Write-Host "Запускаем все сервисы..." -ForegroundColor Green
try {
    docker compose up -d
    Write-Host "✅ Сервисы запущены в фоновом режиме" -ForegroundColor Green
} catch {
    Write-Host "❌ Ошибка при запуске сервисов" -ForegroundColor Red
    Write-Host "Пробуем старый синтаксис..." -ForegroundColor Yellow
    try {
        docker-compose up -d
        Write-Host "✅ Сервисы запущены (старый синтаксис)" -ForegroundColor Green
    } catch {
        Write-Host "❌ Критическая ошибка при запуске" -ForegroundColor Red
        exit 1
    }
}

# Ждём инициализации
Write-Host "Ждём 30 секунд для инициализации сервисов..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Проверяем статус
Write-Host "Проверка статуса сервисов:" -ForegroundColor Green
try {
    docker compose ps
} catch {
    docker-compose ps
}

Write-Host ""
Write-Host "=== СИСТЕМА ЗАПУЩЕНА ===" -ForegroundColor Green
Write-Host ""
Write-Host "Доступные сервисы:" -ForegroundColor Cyan
Write-Host "1. Backend API: http://localhost:8081" -ForegroundColor White
Write-Host "2. Keycloak: http://localhost:8080" -ForegroundColor White
Write-Host "3. Frontend: http://localhost:3000" -ForegroundColor White
Write-Host "4. Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host "5. Grafana: http://localhost:3001" -ForegroundColor White
Write-Host "6. Kibana: http://localhost:5601" -ForegroundColor White
Write-Host "7. Zipkin: http://localhost:9411" -ForegroundColor White
Write-Host "8. Elasticsearch: http://localhost:9200" -ForegroundColor White
Write-Host "9. Nginx: http://localhost:80" -ForegroundColor White
Write-Host "10. RTMP: rtmp://localhost:1935" -ForegroundColor White
Write-Host ""
Write-Host "Для просмотра логов выполните: docker compose logs -f" -ForegroundColor Yellow
Write-Host "Для остановки: docker compose down" -ForegroundColor Yellow