# РЕАЛЬНЫЕ ОШИБКИ ПРИ ЗАПУСКЕ BACKEND

## Ошибка 1: Проблема с зависимостью bucket4j

**Файл:** `backend/pom.xml`  
**Строка:** 108-111  
**Ошибка:** 
```
[ERROR] Failed to execute goal on project platform: Could not resolve dependencies for project com.platform:platform:jar:1.0.0
[ERROR] dependency: com.github.vladimir-bukhtoyarov.bucket4j:bucket4j-spring-boot-starter:jar:0.8.0 (compile)
[ERROR] Could not find artifact com.github.vladimir-bukhtoyarov.bucket4j:bucket4j-spring-boot-starter:jar:0.8.0 in central (https://repo.maven.apache.org/maven2)
```

**Решение применено:** Обновлена версия с 0.8.0 на 7.6.0

## Ошибка 2: Проблема с портами в Windows

**Файл:** `docker-compose-public.yml`  
**Ошибка:**
```
Error response from daemon: ports are not available: exposing port TCP 0.0.0.0:6381 -> 127.0.0.1:0: listen tcp 0.0.0.0:6381: bind: An attempt was made to access a socket in a way forbidden by its access permissions.
```

**Проблема:** Windows имеет ограничения на использование портов, особенно в диапазоне 6000-7000.

**Решение:** Запуск сервисов без публичных портов (внутри Docker сети).

## Ошибка 3: Проблема с образами OpenJDK

**Файл:** `backend/Dockerfile` (исходная версия)  
**Ошибка:** 
```
 => ERROR [1/7] FROM docker.io/library/openjdk:17-jdk-slim
```

**Решение применено:** Заменён образ на `eclipse-temurin:17-jdk`

## Ошибка 4: Проблема с путями копирования

**Файл:** `backend/Dockerfile` (исходная версия)  
**Ошибка:** 
```
COPY failed: file not found in build context or excluded by .dockerignore: stat backend/src/main/resources: file does not exist
```

**Решение применено:** Исправлены пути копирования

## ТЕКУЩИЕ ПРОБЛЕМЫ

### 1. Зависимости Maven
После обновления bucket4j до версии 7.6.0 могут возникнуть проблемы совместимости с Spring Boot 3.2.5.

**Проверка:** Нужно проверить совместимость версий:
- Spring Boot 3.2.5
- bucket4j-spring-boot-starter 7.6.0

### 2. Проблема с портами в Windows
Сервисы запускаются только внутри Docker сети, без доступа с хоста.

**Альтернативные решения:**
1. Использовать WSL2 для Docker
2. Настроить порты в другом диапазоне (например, 8000-9000)
3. Использовать Docker Desktop с WSL2 backend

### 3. Отсутствие Maven на хосте
Нельзя собрать проект локально для тестирования.

## ПРОВЕРКА ПОДКЛЮЧЕНИЯ К СЕРВИСАМ

### PostgreSQL (внутри Docker сети)
**Статус:** ✅ Работает  
**Проверка:** `docker exec platform-postgres-1 pg_isready -U platform_user -d platform_db`  
**Логи:** Нет ошибок, база готова к подключениям

### Redis (внутри Docker сети)  
**Статус:** ✅ Работает  
**Проверка:** `docker exec platform-redis-1 redis-cli --raw incr ping`  
**Логи:** Нет ошибок, Redis принимает соединения

### Prometheus (внутри Docker сети)
**Статус:** ✅ Работает  
**Проверка:** `docker exec platform-prometheus-1 wget -q --spider http://localhost:9090`  
**Логи:** Сервер запущен на порту 9090

### Grafana (внутри Docker сети)
**Статус:** ✅ Работает  
**Проверка:** `docker exec platform-grafana-1 wget -q --spider http://localhost:3000`  
**Логи:** Сервер запущен на порту 3000

## РЕКОМЕНДАЦИИ

### 1. Для решения проблемы с bucket4j:
```xml
<!-- Попробовать другую версию -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov.bucket4j</groupId>
    <artifactId>bucket4j-spring-boot-starter</artifactId>
    <version>0.7.0</version> <!-- Или 0.6.0 -->
</dependency>
```

### 2. Для решения проблемы с портами:
- Установить Docker Desktop с WSL2 backend
- Или использовать порты в диапазоне 8000-9000

### 3. Для локальной сборки:
- Установить Maven на Windows
- Или использовать WSL2 с установленным Maven

## СЛЕДУЮЩИЕ ШАГИ

1. **Проверить совместимость bucket4j с Spring Boot 3.2.5**
2. **Настроить порты для доступа с хоста** (через WSL2 или другой диапазон портов)
3. **Попробовать собрать backend локально** с исправленными зависимостями
4. **Протестировать подключение backend к сервисам** через Docker сеть