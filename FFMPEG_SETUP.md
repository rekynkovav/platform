# Установка и настройка FFmpeg

## Что такое FFmpeg

FFmpeg - это мощный инструмент для работы с видео и аудио. Мы используем его для:
- Добавления водяных знаков на видео/изображения
- Конвертации видео в HLS формат
- Шифрования HLS сегментов
- Получения метаданных (длительность, разрешение)

## Установка FFmpeg

### Windows

**Вариант 1: Через Chocolatey (рекомендуется)**
```bash
choco install ffmpeg
```

**Вариант 2: Вручную**
1. Скачать с https://www.gyan.dev/ffmpeg/builds/
2. Распаковать в `C:\ffmpeg`
3. Добавить в PATH: `C:\ffmpeg\bin`
4. Перезапустить терминал

**Проверка:**
```bash
ffmpeg -version
ffprobe -version
```

### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install ffmpeg
```

### macOS

```bash
brew install ffmpeg
```

## Проверка работы в приложении

После запуска Spring Boot приложения проверьте логи:

```
INFO  c.p.service.WatermarkService - FFmpeg available: true
```

Если FFmpeg не найден:
```
WARN  c.p.service.WatermarkService - FFmpeg not available: Cannot run program "ffmpeg"
```

## Использование

### 1. Водяные знаки на видео

```java
// Автоматически при загрузке видео
POST /api/media/upload
- file: video.mp4
- type: video

// Результат: видео с водяным знаком "User: 12345678 | 2026-04-08 10:30:00 | ID: abcd1234"
```

### 2. HLS стриминг

```java
// Автоматически создается при загрузке видео
// Видео конвертируется в:
// - playlist.m3u8 (плейлист)
// - segment0.ts, segment1.ts, ... (зашифрованные сегменты по 6 сек)

// Доступ через:
GET /api/media/hls/{fileId}/playlist.m3u8
```

### 3. Водяные знаки на изображения

```java
POST /api/media/upload
- file: image.jpg
- type: image

// Результат: изображение с водяным знаком в левом нижнем углу
```

## Настройка параметров

В `application.yaml`:

```yaml
media:
  watermark:
    enabled: true          # Включить/выключить водяные знаки
    font-size: 24         # Размер шрифта
    opacity: 0.5          # Прозрачность (0.0 - 1.0)
  
  hls:
    enabled: true         # Включить/выключить HLS конвертацию
    segment-duration: 6   # Длительность сегмента в секундах
```

## Производительность

### Время обработки (примерно)

**Водяной знак:**
- Видео 100MB (5 мин): ~30 сек
- Изображение 5MB: ~2 сек

**HLS конвертация:**
- Видео 100MB (5 мин): ~2-3 мин
- Зависит от разрешения и битрейта

### Оптимизация

1. **Асинхронная обработка** (рекомендуется для продакшена):
```java
@Async
public CompletableFuture<MediaFile> uploadSecureMedia(...) {
    // Обработка в фоне
}
```

2. **Очередь задач** (для высоких нагрузок):
- RabbitMQ / Kafka для очереди
- Отдельные воркеры для обработки видео

3. **Кеширование HLS**:
- Сохранять HLS версии в S3
- Не пересоздавать при каждом запросе

## Troubleshooting

### Ошибка: "Cannot run program ffmpeg"

**Решение:**
1. Проверить установку: `ffmpeg -version`
2. Проверить PATH
3. Перезапустить приложение

### Ошибка: "FFmpeg failed with exit code: 1"

**Решение:**
1. Проверить логи FFmpeg в консоли
2. Проверить формат входного файла
3. Проверить права доступа к временным файлам

### Видео не конвертируется в HLS

**Решение:**
1. Проверить `media.hls.enabled: true`
2. Проверить доступность FFmpeg
3. Проверить логи: `Failed to convert video to HLS`

### Водяной знак не появляется

**Решение:**
1. Проверить `media.watermark.enabled: true`
2. Проверить, что FFmpeg установлен
3. Проверить логи: `Failed to add video watermark`

## Альтернативы FFmpeg

Если FFmpeg недоступен:

1. **Без водяных знаков**: файлы загружаются без обработки
2. **Без HLS**: используется прямой стриминг через presigned URLs
3. **Облачные сервисы**:
   - AWS Elemental MediaConvert
   - Cloudflare Stream
   - Mux Video

## Примеры команд FFmpeg

### Добавить водяной знак
```bash
ffmpeg -i input.mp4 \
  -vf "drawtext=text='User: 12345678':fontsize=24:fontcolor=white@0.5:x=10:y=10" \
  output.mp4
```

### Конвертировать в HLS
```bash
ffmpeg -i input.mp4 \
  -c:v libx264 -c:a aac \
  -hls_time 6 \
  -hls_playlist_type vod \
  -hls_segment_filename "segment%d.ts" \
  playlist.m3u8
```

### HLS с шифрованием
```bash
# Создать ключ
openssl rand 16 > key.bin

# Создать keyinfo файл
echo "http://localhost:8081/api/media/key/FILE_ID" > keyinfo.txt
echo "key.bin" >> keyinfo.txt
echo $(xxd -p key.bin) >> keyinfo.txt

# Конвертировать
ffmpeg -i input.mp4 \
  -hls_key_info_file keyinfo.txt \
  -hls_time 6 \
  playlist.m3u8
```

## Docker

Если используете Docker, добавьте FFmpeg в Dockerfile:

```dockerfile
FROM openjdk:21-slim

# Установка FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# Копирование приложения
COPY target/platform-1.0.1.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```
