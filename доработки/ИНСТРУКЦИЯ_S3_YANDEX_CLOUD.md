# 🔧 ИНСТРУКЦИЯ: Исправление S3/Yandex Cloud интеграции
## Для локальной нейросети / разработчика

**Дата:** 2026-05-28  
**Приоритет:** P0 (без этого prod не заработает, dev/staging не стартуют)  
**Стек:** AWS SDK 1.12.650, Yandex Cloud S3, Spring Boot 3.2.5

---

## 📋 СОДЕРЖАНИЕ
1. [Текущая архитектура](#1-текущая-архитектура)
2. [Проблемы и их приоритеты](#2-проблемы-и-их-приоритеты)
3. [Исправление P0: Конфигурация Yandex S3](#3-исправление-p0-конфигурация-yandex-s3)
4. [Исправление P1: Зависимости от S3StorageService](#4-исправление-p1-зависимости-от-s3storageservice)
5. [Исправление P2: MediaStorageService](#5-исправление-p2-mediastorageservice)
6. [Проверка после исправлений](#6-проверка-после-исправлений)
7. [Переменные окружения для продакшена](#7-переменные-окружения-для-продакшена)

---

## 1. Текущая архитектура

### Интерфейс StorageService
 

### Реализации
| Класс | Профиль | Хранилище |
|-------|---------|-----------|
| `LocalStorageService` | `!prod` | Локальная ФС (`./uploads`) |
| `S3StorageService` | `prod` | Yandex Cloud S3 |

### Кто использует StorageService
| Сервис | Как инжектирует | Проблема? |
|--------|----------------|-----------|
| `SecureMediaService` | `StorageService` (интерфейс) | ✅ OK |
| `HLSStreamingService` | `StorageService` (интерфейс) | ✅ OK |
| `PostService` | Через `SecureMediaService` | ✅ OK |
| `BroadcastArchiveService` | `S3StorageService` (конкретный класс!) | ❌ БАГ |
| `MediaCleanupService` | `S3StorageService` (конкретный класс!) | ❌ БАГ |
| `MediaStorageService` | Не использует StorageService вообще | ❌ БАГ |

---

## 2. Проблемы и их приоритеты

### P0 — Prod не заработает без S3 ключей
- Код читает `yandex.s3.*` переменные
- `application.yml` содержит `app.storage.s3.*` (другие имена!)
- `application-prod.yml` НЕ содержит никаких S3 настроек
- Результат: пустые ключи → все операции с S3 падают

### P1 — Dev/Staging/Test не стартуют
- `BroadcastArchiveService` и `MediaCleanupService` требуют `S3StorageService`
- В не-prod профилях `S3StorageService` бин не существует (`@Profile("prod")`)
- Приложение падает при старте: `NoSuchBeanDefinitionException`

### P2 — Несогласованность хранения
- `MediaStorageService` пишет файлы напрямую на диск через `Files.copy()`
- Игнорирует `StorageService` → в prod посты на диске, а архивы в S3

---

## 3. Исправление P0: Конфигурация Yandex S3

### Шаг 3.1: Добавить yandex.s3.* в application-prod.yml

Открыть `backend/src/main/resources/application-prod.yml` и добавить в конец:

 

### Шаг 3.2: Заменить мёртвую секцию app.storage.s3 в application.yml

Открыть `backend/src/main/resources/application.yml`.

Найти блок:
 

Заменить на:
 

> **Почему:** Секция `app.storage.s3.*` нигде не читается кодом. Код использует `yandex.s3.*`. Старая секция вводила в заблуждение неправильными дефолтами (us-east-1, s3.amazonaws.com).

### Шаг 3.3: Добавить yandex.s3.* в application-staging.yml (опционально)

Если staging тоже должен использовать S3:

 

> ⚠️ Если staging использует LocalStorageService, этот шаг не нужен.

---

## 4. Исправление P1: Зависимости от S3StorageService

### Шаг 4.1: Расширить интерфейс StorageService

Открыть `backend/src/main/java/com/platform/service/StorageService.java`.

Добавить два default-метода:

 

### Шаг 4.2: Обновить BroadcastArchiveService

Открыть `backend/src/main/java/com/platform/service/BroadcastArchiveService.java`.

Заменить:
 

На:
 

Заменить все вызовы `s3StorageService.xxx()` на `storageService.xxx()`:
- `s3StorageService.upload(...)` → `storageService.upload(...)`
- `s3StorageService.verifyUpload(...)` → `storageService.verifyUpload(...)`
- `s3StorageService.isValidMediaFile(...)` → `storageService.isValidMediaFile(...)`
- `s3StorageService.generatePresignedUrl(...)` → `storageService.generatePresignedUrl(...)`

Удалить импорт `S3StorageService` и `LocalStorageService`, добавить `StorageService` если нет.

### Шаг 4.3: Обновить MediaCleanupService

Открыть `backend/src/main/java/com/platform/service/MediaCleanupService.java`.

Заменить:
 

На:
 

Заменить `s3StorageService.delete(...)` на `storageService.delete(...)`.

Обновить импорты.

---

## 5. Исправление P2: MediaStorageService

### Шаг 5.1: Переписать MediaStorageService на StorageService

Открыть `backend/src/main/java/com/platform/service/MediaStorageService.java`.

Текущий код использует `Files.copy()` напрямую. Нужно заменить на `StorageService`.

Заменить поле:
 

На:
 

(Добавить `@RequiredArgsConstructor` или конструкторную инъекцию.)

В методе `saveMedia()` заменить файловую запись на:
 

> ⚠️ Это изменение влияет на все профили. В dev/test файлы будут в `./uploads`, в prod — в S3.

---

## 6. Проверка после исправлений

### Компиляция
 
Ожидаемый результат: `BUILD SUCCESS`

### Unit-тесты
 

### Проверка контекста (самый важный тест)
 
Если контекст загружается — зависимости корректны.

### Ручная проверка в dev
 
Приложение должно стартовать без ошибок `NoSuchBeanDefinitionException`.

---

## 7. Переменные окружения для продакшена

Для работы S3StorageService в prod необходимо установить:

| Переменная | Описание | Пример |
|------------|----------|--------|
| `YANDEX_S3_ACCESS_KEY` | Access Key ID из Yandex Cloud Console | `YCAJE...` |
| `YANDEX_S3_SECRET_KEY` | Secret Access Key из Yandex Cloud Console | `YCMxG...` |
| `YANDEX_S3_BUCKET` | Имя бакета (опционально, дефолт: platform-media) | `platform-media` |

### Где получить ключи
1. Зайти в [Yandex Cloud Console](https://console.cloud.yandex.ru/)
2. Перейти в «Сервисные аккаунты»
3. Создать сервисный аккаунт с ролью `storage.editor`
4. Создать статический ключ доступа
5. Сохранить Access Key ID и Secret Access Key

### Docker Compose пример
 

---

## 🆘 Если что-то пошло не так

### Ошибка NoSuchBeanDefinitionException: S3StorageService
Значит `BroadcastArchiveService` или `MediaCleanupService` всё ещё инжектируют `S3StorageService` напрямую. Выполните шаги 4.2 и 4.3.

### Ошибка "Access Denied" при загрузке в S3
Проверьте:
1. Переменные `YANDEX_S3_ACCESS_KEY` и `YANDEX_S3_SECRET_KEY` установлены
2. Сервисный аккаунт имеет роль `storage.editor` на бакете
3. Бакет существует в регионе `ru-central1`

### Ошибка "Cannot find symbol: AmazonS3Exception"
Класс находится в пакете `com.amazonaws.services.s3.AmazonS3Exception`. Убедитесь что зависимость `aws-java-sdk-s3` версии 1.12.650 присутствует в pom.xml.

### Файлы сохраняются локально вместо S3
Проверьте активный профиль: `SPRING_PROFILES_ACTIVE=prod`. В любом другом профиле используется `LocalStorageService`.

---

**Последнее обновление:** 2026-05-28
