# Frontend Architecture Pass — ЧЕКЛИСТ

**Дата:** 2026-06-26
**Контекст:** После unified chat refactor. Фронт ~60 файлов, 18.5K LOC JS + 13.6K LOC CSS.
**Цель:** Привести к единому стандарту. Убрать мусор. Сделать архитектуру предсказуемой.

---

## ФАЗА 1: АУДИТ (сначала прочитать, потом чинить)

### 1.1 Dead Code Sweep
- [ ] `grep -rn` по всем импортам — найти файлы которые нигде не импортируются
- [ ] Удалить мёртвые компоненты (UserStatus.test.js, ProtectedRoute.test.js — тесты удалённых компонентов)
- [ ] Проверить hooks/ — useTheme, useAudioRecorder — используются?
- [ ] Проверить Common/ — Events.js, Notifications.js, ContentModal.js — используются?
- [ ] Удалить orphaned CSS (Statistics.css, UsersList.css, ChatContainer.css, GeneralChat.css — уже удалены, но проверить)

### 1.2 CSS Audit
- [ ] Сколько CSS файлов? Какие дублируются?
- [ ] Какие компоненты НЕ импортируют свой CSS? (проблема #5 из deep analysis)
- [ ] index.css — глобальные ресеты (button { background: none }) — кто ломает?
- [ ] CSS variables — используется ли единая палитра или хардкод?
- [ ] Dark mode — есть ли поддержка через CSS vars?

### 1.3 Console.log Sweep
- [ ] `grep -rn "console\.\(log\|warn\|error\)" src/ --include="*.js" --include="*.ts" --include="*.tsx" | grep -v __tests__`
- [ ] Оставить ТОЛЬКО в error boundaries и keycloak.js debug helpers
- [ ] Удалить все остальные

### 1.4 Import Hygiene
- [ ] Проверить циклические зависимости
- [ ] Проверить что barrel exports (index.ts) не тянут мёртвый код
- [ ] Проверить path aliases — нет ли `../../` танцев когда можно `@/`

---

## ФАЗА 2: КОМПОНЕНТЫ (порядок по важности)

### 2.1 Navigation (NavTabs)
- [ ] Split tabs — работают? Не ломаются при resize?
- [ ] pathMap — актуален? (chat/nutrition, chat/user2 → удалён)
- [ ] Hover behavior — не уезжает за экран на мобильных?

### 2.2 Feed (MainFeed + NutritionFeed)
- [ ] Admin panel — sticky работает? (flex scroll antipattern фиксился)
- [ ] Add post form — modal или inline? (должен быть modal)
- [ ] Infinite scroll — pagination есть? Работает?
- [ ] Optimistic updates — при добавлении/удалении постов

### 2.3 Live Streaming
- [ ] LivePage — рендерится? Real components подключены?
- [ ] BroadcastControls — LiveSessionProvider обёрнут? Кнопка create работает?
- [ ] LiveChat — отдельный от regular chat? WebSocket подключается?
- [ ] HLS player — quality switching работает?

### 2.4 Dashboard
- [ ] Menu items — из API или статика?
- [ ] Timer — используется?

### 2.5 Events + Archive + Scheduled
- [ ] EventsHub — async materials фиксился. Работает?
- [ ] ArchiveList — CRUD работает?
- [ ] ScheduledEvents — отображает будущие события?

### 2.6 Notifications
- [ ] NotificationBell — polling 30с. Работает? Навигация через useNavigate?
- [ ] GlobalEventNotification — откуда берёт данные?

### 2.7 User Profile
- [ ] UserProfile — auto-provision работает? Error handling?
- [ ] Payment flow — SBP QR code отображается?

### 2.8 RoleSwitcher
- [ ] Dev mode — переключение ролей работает?
- [ ]影响 Permissions hook корректно?

---

## ФАЗА 3: ХУКИ

### 3.1 usePermissions
- [ ]ighestRole detection — по ROLE_LEVELS, не по appRoles[0]
- [ ] hasLevel / hasRole / hasAnyRole — покрывают все use cases?

### 3.2 useWebSocket
- [ ] Reconnection — exponential backoff работает?
- [ ] Token refresh — при истечении JWT?
- [ ] Cleanup — WS закрывается при unmount?

### 3.3 Live hooks
- [ ] useLiveBroadcast — API integration
- [ ] useLiveChat — WebSocket
- [ ] useHlsPlayer — quality switching, destroy cleanup

### 3.4 Мёртвые hooks
- [ ] useRoles — удалён? Или orphaned?
- [ ] useChat.ts — удалён? (заменили useChatRoom)

---

## ФАЗА 4: API LAYER

### 4.1 services/api.js
- [ ] 429 retry — max 3 retries. Работает?
- [ ] Multipart upload — Content-Type boundary не ломается?
- [ ] JWT interceptor — auto token refresh?
- [ ] Error interceptor —统一 обработка ошибок?

### 4.2 live/ services (TypeScript)
- [ ] realApi.ts — token из keycloak.js, НЕ localStorage
- [ ] realWebSocket.ts — token expiry check
- [ ] realMedia.ts — HLS cleanup
- [ ] Barrel export index.ts — type + value совпадают?

---

## ФАЗА 5: ТЕСТЫ

### 5.1 Сломанные тесты
- [ ] NotificationBell.test.js — useNavigate без Router context
- [ ] UserStatus.test.js — удалён компонент → удалить тест
- [ ] ProtectedRoute.test.js — удалён компонент → удалить тест

### 5.2 Coverage
- [ ] Какие компоненты БЕЗ тестов?
- [ ] Критические пути: auth flow, chat send, post CRUD, live broadcast

---

## ФАЗА 6: TypeScript Migration (по желанию)

- [ ] Определить priority файлы для .ts конвертации
- [ ] services/api.js → api.ts
- [ ] hooks/*.js → hooks/*.ts
- [ ] tsc --noEmit = 0 errors (source, не тесты)

---

## ПРИОРИТЕТЫ

1. **P0 — Dead code + orphaned tests** (30 мин)
   - Удалить UserStatus.test.js, ProtectedRoute.test.js
   - Найти и удалить мёртвые компоненты/hooks

2. **P1 — CSS Audit** (1 час)
   - Найти дубли, проверить импорты, unified variables

3. **P2 — Console.log + Import cleanup** (30 мин)

4. **P3 — Feed + Live verification** (1 час)
   - Убедиться что sticky, modal, infinite scroll работают

5. **P4 — Test fixes** (30 мин)
   - починить NotificationBell.test.js (добавить MemoryRouter)

6. **P5 — TypeScript migration** (по времени)

---

## ИНСТРУКЦИЯ ДЛЯ СЛЕДУЮЩЕЙ СЕССИИ

```
Продолжаем архитектурный проход фронта. Загрузи skill platform, прочитай этот чеклист.

Начни с ФАЗЫ 1 (аудит):
1. Dead code sweep — grep по импортам, найти мёртвые файлы
2. Console.log sweep — grep, оставить только в keycloak.js + error boundaries
3. CSS audit — посчитать файлы, найти дубли, проверить импорты

После аудита — покажи список найденного (max 20 строк), потом фикси по одному.
Не всё сразу. Пошагово.

Ключевые паттерны (из прошлых сессий):
- CSS class mismatch = невидимый элемент (сравнивать JSX className с CSS селектором)
- sessionStorage demoRole = баг в production (использовать usePermissions)
- async в useEffect = не присваивать результат напрямую
- git history first при "верни как было"
- Never delete routes/tabs без подтверждения
```
