# 📋 Platform Project Context — Живой контекст проекта

**Последнее обновление:** 2026-04-25T00:40:00Z  
**Версия проекта:** 1.0.9  
**Статус:** ✅ Активная разработка

---

## 🎯 ЧТО ЭТО ЗА ПРОЕКТ

**Platform** — fullstack платформа для защищённого медиа-контента с социальными функциями.

**Основная фишка:** AES-256 шифрование + водяные знаки + HLS стриминг для защиты видео/аудио от пиратства.

---

## 🏗️ АРХИТЕКТУРА

### Backend
- **Framework:** Spring Boot 4.0.5 (Java 17)
- **Database:** PostgreSQL 16 (с Liquibase миграциями)
- **Auth:** Keycloak 24.0.5 (OAuth2 + JWT)
- **Storage:** Яндекс Cloud S3 (S3-совместимый)
- **Security:** Spring Security 6.x + Method-level security
- **WebSocket:** Для реального времени (чат, уведомления)
- **Media Processing:** FFmpeg (водяные знаки, HLS конвертация)

### Frontend
- **Framework:** React 18.3.1
- **Routing:** React Router 6.30.3
- **HTTP Client:** Axios 1.15.2
- **Auth:** Keycloak.js 24.0.5
- **Build Tool:** React Scripts 5.0.1
- **Themes:** Light/Dark с автопереключением

### Infrastructure
- **Orchestration:** Docker Compose
- **Reverse Proxy:** Nginx (HTTPS)
- **Ports:**
  - Frontend: 3005 (было 3000, изменено 2026-04-24)
  - Backend: 8081
  - Keycloak: 8080
  - PostgreSQL: 5432

---

## 📦 МОДУЛИ ПРИЛОЖЕНИЯ

### Frontend Components (15 модулей)
1. **Admin** — админ-панель управления
2. **Feed** — лента контента (посты, медиа)
3. **Chat** — мессенджер (WebSocket)
4. **Events** — события и мероприятия
5. **Dashboard** — дашборд пользователя
6. **Profile** — профиль текущего пользователя
7. **UserProfile** — просмотр профилей других
8. **Notification** — уведомления
9. **Archive** — архив контента
10. **Scheduled** — запланированные публикации
11. **Help** — справка
12. **Contacts** — контакты
13. **Common** — общие компоненты
14. **Layout** — лейауты
15. **Navigation** — навигация

### Backend Packages
- `controller/` — REST API endpoints
- `service/` — бизнес-логика (SecureMediaService, WatermarkService, HLSService, EncryptionService)
- `model/` — JPA сущности
- `repository/` — Spring Data репозитории
- `config/` — конфигурация Spring
- `websocket/` — WebSocket handlers
- `exception/` — обработка ошибок

---

## 🔐 СИСТЕМА БЕЗОПАСНОСТИ

### Защита медиа-контента (РЕАЛИЗОВАНО)
1. **AES-256 шифрование** всех файлов при загрузке
2. **Уникальные ключи** для каждого файла (masterKey + userId + fileId)
3. **Видимые водяные знаки** (userId + timestamp + fileId) на видео/изображениях
4. **HLS стриминг** с шифрованием сегментов (AES-128)
5. **Presigned URLs** с TTL 5 минут
6. **Аудит логи** всех обращений к файлам (таблица `media_access_logs`)

### Аутентификация
- **Keycloak** как Identity Provider
- **Роли (9 типов):**
  - `user0` — Пользователь 0 (просмотр сайта)
  - `user1` — Пользователь 1 (чат 1)
  - `user2` — Пользователь 2 (чат 2)
  - `admin-moderator` — Админ-модератор (всё кроме скачивания и питания)
  - `consultant-nutrition` — Консультант по питанию
  - `consultant-sport` — Консультант по спорту
  - `developer` — Разработчик (полный доступ)
  - `vyacheslav` — Владелец (без прав на события)
  - `super-admin` — Главный админ (всё кроме питания + скачивание)
- **JWT токены** с коротким TTL
- **Method-level security** на всех endpoints

### Матрица прав доступа
| Роль | Dashboard | Events | Scheduled | Скачивание | Питание |
|------|-----------|--------|-----------|------------|---------|
| `admin-moderator` | ✅ | ✅ | ✅ | ❌ | ❌ |
| `developer` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `super-admin` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `vyacheslav` | ❌ | ❌ | ❌ | ❌ | ❌ |
| `consultant-*` | ❌ | ❌ | ❌ | ❌ | ✅ (nutrition) |

### GODMODE правила (из .qwen/AGENTS.md)
- Prompt Injection Protection
- WebRTC шифрование (DTLS/SRTP)
- E2E шифрование пользовательских данных
- Yandex KMS для мастер-ключей
- Строгий CORS для Object Storage

---

## 🔑 КЛЮЧЕВЫЕ API ENDPOINTS

### Media API
```
POST   /api/media/upload                    # Загрузка с шифрованием + водяные знаки
GET    /api/media/stream/{fileId}           # Presigned URL (5 мин TTL)
GET    /api/media/key/{fileId}              # Ключ шифрования для HLS
GET    /api/media/download/{fileId}         # Скачать расшифрованный файл
GET    /api/media/{fileId}                  # Метаданные файла
GET    /api/media/my-files                  # Файлы текущего пользователя
GET    /api/media/hls/{fileId}/playlist.m3u8 # HLS плейлист
DELETE /api/media/{fileId}                  # Удалить файл
```

---

## 📁 СТРУКТУРА ПРОЕКТА

```
D:\Trinity\temp_workspace\platform\
├── .claude\
│   └── info.md                    # ← ЭТОТ ФАЙЛ (живой контекст)
├── .qwen\
│   └── AGENTS.md                  # GODMODE правила безопасности
├── backend\
│   └── src\main\java\com\platform\
│       ├── PlatformApplication.java
│       ├── config\
│       ├── controller\
│       ├── service\
│       ├── model\
│       ├── repository\
│       ├── websocket\
│       └── exception\
├── frontend\
│   ├── src\
│   │   ├── components\          # 15 модулей
│   │   ├── context\             # AudioPlayerContext
│   │   ├── hooks\               # useTheme
│   │   ├── App.js
│   │   └── index.js
│   ├── package.json
│   └── .env
├── docker\                       # Docker конфигурации
├── nginx\                        # Nginx конфиги
├── ssl\                          # HTTPS сертификаты
├── scripts\                      # Утилиты
├── docker-compose.yml
├── pom.xml
├── IMPLEMENTATION_COMPLETE.md    # Документация по медиа-защите
├── MEDIA_SETUP.md               # Инструкция по настройке
├── FFMPEG_SETUP.md              # Установка FFmpeg
├── ROLES_MAPPING.md             # Маппинг ролей Keycloak
└── PAYMENT_SYSTEM.md            # Платёжная система (если есть)
```

---

## 🚀 ТЕКУЩИЙ СТАТУС (2026-04-24)

### Что работает
- ✅ Backend Spring Boot запущен (порт 8081)
- ✅ Frontend React запущен (порт 3005)
- ✅ PostgreSQL 16 настроен
- ✅ Keycloak 24 интегрирован
- ✅ Медиа-защита полностью реализована
- ✅ Docker Compose конфигурация готова
- ✅ Все зависимости установлены

### Последние изменения
- **2026-04-24 18:35** — Фронтенд перезапущен на порту 3005 (был конфликт с портом 3001)
- **2026-04-24 18:30** — Проект изучен, создан файл контекста info.md

### Известные проблемы
- ⚠️ Порт 3001 был занят другим процессом (PID 8096) — решено переносом на 3005
- ⚠️ FFmpeg должен быть установлен в системе для работы водяных знаков

---

## 🔧 ПЕРЕМЕННЫЕ ОКРУЖЕНИЯ

### Backend (.env)
```bash
# PostgreSQL
DB_URL=jdbc:postgresql://localhost:5432/platform_db
DB_USER=platform_user
DB_PASSWORD=platform_pass_2026

# Keycloak
KEYCLOAK_URL=http://localhost:8080/auth
KEYCLOAK_REALM=platform
KEYCLOAK_CLIENT_ID=platform-backend
KEYCLOAK_CLIENT_SECRET=<secret>

# Яндекс S3
YC_ACCESS_KEY=<key>
YC_SECRET_KEY=<secret>
YC_BUCKET=platform-media

# Шифрование (КРИТИЧНО: сменить в продакшене!)
MEDIA_MASTER_KEY=change-this-to-strong-random-key-32-chars-minimum

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
```

### Frontend (.env)
```bash
REACT_APP_API_URL=https://platform.local/api
REACT_APP_KEYCLOAK_URL=https://auth.platform.local/auth
REACT_APP_KEYCLOAK_REALM=platform
REACT_APP_KEYCLOAK_CLIENT_ID=platform-frontend
```

---

## 📚 ДОКУМЕНТАЦИЯ

### Основные файлы
- `IMPLEMENTATION_COMPLETE.md` — полная документация по медиа-защите
- `MEDIA_SETUP.md` — инструкция по настройке медиа-системы
- `FFMPEG_SETUP.md` — установка и использование FFmpeg
- `ROLES_MAPPING.md` — маппинг ролей Keycloak
- `.qwen/AGENTS.md` — правила безопасности GODMODE

### Внешние ссылки
- Spring Boot 4.0.5 Docs
- React 18 Docs
- Keycloak 24 Docs
- PostgreSQL 16 Docs

---

## 🎯 ROADMAP (TODO)

### MVP (готово)
- ✅ Базовая защита медиа
- ✅ Keycloak интеграция
- ✅ Основные модули UI

### Production (планируется)
- ⏳ Асинхронная обработка видео (RabbitMQ/Kafka)
- ⏳ CDN для быстрой доставки
- ⏳ Мониторинг и алерты
- ⏳ Load testing
- ⏳ Backup стратегия

### Enterprise (опционально)
- ⏳ DRM интеграция (Widevine, FairPlay)
- ⏳ Forensic watermarking (невидимые метки)
- ⏳ AI-based content moderation
- ⏳ Multi-region replication

---

## 🔄 CHANGELOG (История изменений)

### 2026-04-25
- **01:47** — Dashboard.js: обновлены все функции позиционирования подменю (handleSubmenuEnter и handleSubmenuItemEnter) - submenuWidth теперь учитывает min-width: 400px через Math.max(), увеличены отступы от границ экрана до 20px для всех подменю
- **01:45** — Dashboard.css: изменен width с max-content на auto для .menu-dropdown-positioned и .menu-submenu-positioned - теперь меню корректно соблюдают max-width и не выходят за границы экрана
- **01:45** — Dashboard.js: обновлена логика позиционирования в handleMenuEnter - menuWidth теперь учитывает min-width: 400px через Math.max(), увеличены отступы от границ экрана до 20px
- **01:43** — Dashboard.js: исправлена проблема мгновенного закрытия подменю "Эфиры с гостями" - добавлена проверка состояния мыши перед открытием подменю после задержки (если мышь ушла до истечения 1 секунды, подменю не открывается)
- **01:43** — Dashboard.js: увеличена задержка закрытия подменю с 300мс до 500мс для более комфортного перехода мыши
- **01:42** — Dashboard.js: исправлена ошибка "can't access property getBoundingClientRect" - rect и координаты скролла теперь сохраняются до setTimeout
- **01:41** — Dashboard.js: удалена устаревшая функция handleSubmenuClick которая использовала несуществующее состояние clickedSubmenu
- **01:35** — Dashboard.js: возвращено открытие подменю "Эфиры с гостями" по наведению мыши с задержкой 1 секунда (было по клику)
- **01:35** — Dashboard.js: изменено условие отображения подменю с `clickedSubmenu` на `hoveredSubmenu`
- **01:35** — Dashboard.js: элемент с подменю теперь использует onMouseEnter/onMouseLeave вместо onClick
- **01:35** — Dashboard.js: умное позиционирование подменю с проверкой всех границ viewport (право, лево, верх, низ) - подменю автоматически смещается чтобы не выходить за экран
- **01:24** — Dashboard.css: увеличена ширина выпадающих меню - min-width: 400px, max-width: min(800px, 90vw) (было 300px и 600px)
- **01:24** — Dashboard.css: увеличен размер шрифта в меню - заголовок 16px (было 14px), описание 14px (было 12px), padding элементов 14px 18px (было 12px 16px)
- **01:19** — Dashboard.js: добавлено умное позиционирование выпадающих меню - они всегда остаются внутри экрана (проверка границ viewport, автоматическое смещение влево/вправо/вверх при выходе за границы)
- **01:19** — Dashboard.css: обновлены стили выпадающих меню - max-width: min(600px, 90vw), max-height: min(500px, 70vh), добавлен overflow-x: hidden, word-wrap: break-word для правильного переноса текста
- **01:19** — Dashboard.css: добавлены стили переноса текста для .menu-item-title и .menu-item-description (word-wrap: break-word, overflow-wrap: break-word)
- **01:08** — Dashboard: создан отдельный файл initialData.js с данными для всех 18 категорий навигации (по 50 записей в каждой)
- **01:08** — Dashboard.js: добавлен импорт всех категорий данных из initialData.js
- **01:08** — Созданы данные для категорий: broadcasts-guests (эфиры с гостями), exercises (упражнения), recommendations (рекомендации), hardening (закаливание), about (о нас), newbies (для новичков), heart-voice (голос сердца), reflection (для размышления), interviews (интервью), participants (от участников), motivation (мотивация), reviews (отзывы), music (музыка), movies (фильмы), literature (литература), about-timoshenko (о Тимошенко), challenge (годовой челлендж)
- **01:08** — Каждая категория содержит 50 уникальных записей с описаниями от 10 до 30 слов
- **01:04** — Dashboard.js: исправлена проблема с отображением эфиров - массив `initialBroadcasts` перемещён перед useEffect, добавлена проверка на количество эфиров (если меньше 50, то обновляются)
- **01:00** — Dashboard: создано 50 эфиров вручную (каждый от 10 до 30 слов) по темам: здоровое питание, физические упражнения, медитация, йога, закаливание, сон, дыхательные практики, растяжка, кардио, силовые, психология, мотивация, энергия, баланс, осознанность, питьевой режим, детокс, витамины, суперфуды, спортивное питание, бег, плавание, велоспорт, танцы, пилатес, функциональный тренинг, восстановление, массаж, сауна, контрастный душ, интервальное голодание, вегетарианство, сыроедение, кето диета, палео диета, стресс-менеджмент, работа с эмоциями, позитивное мышление, целеполагание, тайм-менеджмент, осанка, здоровье глаз, зубов, кожи, волос, гормональный баланс, иммунитет, пищеварение, детоксикация печени, долголетие
- **00:57** — Dashboard.css: выпадающие меню теперь автоматически растягиваются под самую длинную запись (max-width: 90vw, width: max-content)
- **00:57** — Dashboard.css: добавлен вертикальный скролл для выпадающих меню (max-height: 70vh, overflow-y: auto) с кастомными стилями скроллбара
- **00:53** — Dashboard: подменю "С Гостями" теперь прилегает вплотную к основному меню эфиров (убран отступ 8px)
- **00:53** — Dashboard.css: кнопка "Добавить эфир с гостем" перекрашена в зелёный цвет (rgba(25, 135, 84, 0.15)) как у всех кнопок "Добавить"
- **00:50** — Добавлена автоматическая система переноса прошедших трансляций в Dashboard: прошедшие трансляции (status: 'completed') автоматически добавляются в раздел "Эфиры" навигации
- **00:50** — ArchiveList.js: добавлены функции `isBroadcastPast()` и `moveCompletedBroadcastsToDashboard()` для проверки и переноса прошедших трансляций
- **00:50** — Dashboard.js: добавлена кнопка скачивания (⬇️) для эфиров, доступная только для `super-admin`, функция `handleDownloadBroadcast()`
- **00:50** — Dashboard.js: добавлена переменная `isSuperAdmin` для проверки роли главного админа
- **00:50** — Dashboard.css: добавлены стили для `.download-item-btn` (кнопка скачивания эфиров)
- **00:50** — Исправлена иконка удаления в подменю "С Гостями" Dashboard: изменена с ✕ на 🗑️
- **00:45** — MainFeed.js: изменена иконка скачивания с 📥 на ⬇️
- **00:45** — MainFeed.css: увеличен размер иконок действий с 18px до 22px, увеличен отступ между кнопками до 14px
- **00:40** — Убран красный фон с кнопки удаления в Dashboard: теперь прозрачная с opacity (как у кнопки редактирования)
- **00:38** — Унифицирована иконка удаления в Dashboard (Навигация): изменена с ✕ на 🗑️ (как во всех остальных компонентах)
- **00:36** — ИСПРАВЛЕНА ГЛАВНАЯ ПРОБЛЕМА: Вкладка "Трансляции" — это ArchiveList, а не Dashboard! Добавлено редактирование трансляций в ArchiveList: кнопка ✏️, модальное окно, бейдж "(изменено)", права для `admin-moderator`, `developer`, `super-admin`
- **00:36** — Обновлены стили ArchiveList.css: добавлены `.edit-broadcast-btn`, `.broadcast-actions`, `.edited-badge-broadcast`
- **00:36** — Исправлена проверка прав в ArchiveList: добавлен `super-admin` (было только admin-moderator, developer, owner)
- **00:33** — Исправлены права в PostCard: редактирование и удаление постов доступно для `admin-moderator`, `developer`, `super-admin` (было только admin-moderator и super-admin)
- **00:27** — Добавлено редактирование постов с эфирами в PostCard: кнопка ✏️, модальное окно редактирования, бейдж "(изменено)", загрузка медиа, права только для `admin-moderator` и `super-admin`
- **00:27** — Обновлены стили PostCard.css: добавлены `.edited-badge-post`, `.post-edit`, `.post-actions-header`, модальное окно `.edit-post-modal` с полным набором стилей
- **00:23** — Обновлены права доступа в Dashboard (Трансляции): добавлена переменная `canEdit`, кнопки редактирования/удаления доступны только `admin-moderator` и `super-admin`, добавлен бейдж "(изменено)"
- **00:23** — Обновлены стили Dashboard.css: добавлен `.edited-badge`
- **00:20** — Добавлено редактирование событий в ScheduledEvents: кнопка ✏️, модальное окно, бейдж "(изменено)", права только для `admin-moderator` и `super-admin`
- **00:20** — Обновлены стили ScheduledEvents.css: добавлены `.edit-event-btn` и `.edited-badge`
- **00:14** — Исправлены права доступа: убрана несуществующая роль `admin`, `vyacheslav` лишён прав на события, обновлена матрица прав для `admin-moderator` и `super-admin`
- **00:14** — Dashboard.js: обновлена проверка isAdmin (убран `admin`, оставлены `admin-moderator`, `developer`, `super-admin`)
- **00:14** — EventsHub.js: обновлена проверка isAdmin (убран `admin`)
- **00:14** — ScheduledEvents.js: обновлена проверка isAdmin (убраны `admin` и `vyacheslav`)

### 2026-04-24
- **19:43** — Добавлена отладка в Events: console.log для проверки роли и статуса isAdmin
- **19:36** — Перекрашены кнопки редактирования сообщений в чате: "Сохранить" - бледно-зелёный (#86efac), "Отмена" - бледно-красный (#fca5a5) в Chat.css
- **19:26** — Переименовано оповещение о событии: "Новое событие!" → "Ближайшее Событие" в ScheduledEvents
- **19:25** — Перекрашены кнопки в модальных окнах: "Сохранить" - бледно-зелёный (#86efac), "Отмена" - бледно-красный (#fca5a5) в Events и Dashboard
- **19:24** — Исправлена проверка прав доступа в Events: добавлена роль 'admin' в условие isAdmin
- **19:11** — Добавлена проверка прав на удаление сообщений в GeneralChat и NutritionChat: пользователь может удалять только свои сообщения (проверка по author)
- **19:09** — Убраны смайлики из вкладок "Новички" и "Продвинутые" в GeneralChat
- **19:08** — Добавлено редактирование и удаление в Dashboard (навигация): кнопки редактирования для основного меню и подменю, обновлён заголовок модального окна, стили
- **19:04** — Добавлено редактирование и удаление материалов в Events (трансляции): кнопки редактирования/удаления, модальное окно подтверждения, стили
- **18:49** — Исправлен баг: дублирование функции cancelDeletePost в NutritionFeed.js:200 и :265 (удалён второй дубликат)
- **18:48** — Исправлен баг: дублирование функции handleSavePost в NutritionFeed.js:166 и :244 (удалён первый дубликат)
- **18:36** — Создан CLAUDE.md с инструкциями для автоматического обновления контекста
- **18:35** — Создан файл info.md с полным контекстом проекта
- **18:35** — Фронтенд перезапущен на порту 3005 (конфликт с 3001)
- **18:30** — Проект изучен Trinity Rabotnik

### 2026-04-23
- Проект скопирован в `D:\Trinity\temp_workspace\platform\`
- Документация обновлена (IMPLEMENTATION_COMPLETE.md)

---

## 📝 ИНСТРУКЦИЯ ДЛЯ CLAUDE

**ВАЖНО:** Этот файл — твоя память о проекте. Читай его при каждом старте сессии.

### Когда обновлять info.md
1. ✅ После любого изменения кода (новые файлы, рефакторинг)
2. ✅ После изменения конфигурации (порты, переменные окружения)
3. ✅ После обнаружения проблем или багов
4. ✅ После добавления новых модулей/функций
5. ✅ После изменения архитектуры
6. ✅ После обновления зависимостей

### Формат обновления
```markdown
### YYYY-MM-DD
- **HH:MM** — Краткое описание изменения
- **HH:MM** — Ещё одно изменение
```

### Что НЕ писать
- ❌ Временные эксперименты
- ❌ Откаты изменений (если откатили — удали запись)
- ❌ Дублирование информации из других файлов

---

**Версия контекста:** 1.0.0  
**Создан:** 2026-04-24T18:35:34Z  
**Ответственный:** Trinity Rabotnik
