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

## Использование

### 1. Водяные знаки на видео

```java
// Автоматически при загрузке видео
POST /api/media/upload
- file: video.mp4
- type: video

// Результат: видео с водяным знаком
```

### 2. HLS стриминг

```java
// Автоматически создается при загрузке видео
GET /api/media/hls/{fileId}/playlist.m3u8
```

## Настройка параметров

В `application.yaml`:

```yaml
media:
  watermark:
    enabled: true
    font-size: 24
    opacity: 0.5
  hls:
    enabled: true
    segment-duration: 6
```

## Docker

Если используете Docker, добавьте FFmpeg в Dockerfile:

```dockerfile
FROM openjdk:21-slim

RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

COPY target/platform-1.0.1.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```
