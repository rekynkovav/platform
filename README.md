# Platform

A full-stack social/community platform combining a **React** frontend with a **Java Spring Boot** backend, deployed via **Docker Compose** with **Keycloak** authentication, live streaming (RTMP/HLS), real-time chat (WebSocket), and full observability stack (Prometheus, Grafana, ELK, Zipkin).

---

## 1. Project Overview

**Platform** is a Russian-language community portal originally built for the "Путь Сердца" (Path of the Heart) project. It serves as a centralized hub for:

- **News Feed** — text, audio, and video posts with admin CRUD, pagination, and inline audio playback
- **Live Broadcasting** — RTMP ingest → HLS delivery with real-time chat, device testing, and archive replay
- **Chat System** — multi-room WebSocket chats (general + nutrition) with question/answer moderation
- **Events Hub** — scheduled events with materials, feedback, and notifications
- **Dashboard** — navigation hub with menus, timers, and content sections
- **User Profiles** — personal profile management with role-based access control
- **Administration** — role switching, user management, statistics, and content moderation

The platform targets a Russian-speaking audience and the entire UI is in Russian.

---

## 2. Tech Stack

### Frontend (`/frontend`)
| Technology     | Version  | Purpose                        |
|----------------|----------|--------------------------------|
| React          | 18.2.0   | UI framework                   |
| React Router   | 6.20.0   | Client-side routing            |
| Axios          | 1.7.2    | HTTP client with interceptors  |
| Keycloak JS    | 24.0.3   | OIDC/OAuth2 authentication     |
| React Window   | 1.8.10   | Virtualized list rendering     |
| Webpack        | 5.88.2   | Module bundler (via CRA)       |
| Lodash         | 4.17.21  | Utility library                |
| ESLint         | 8.57.0   | Static analysis                |
| Prettier       | 3.2.5    | Code formatter                 |

**UI**: Custom CSS (no UI library). Theme support via CSS custom properties (`data-theme` attribute).

### Backend (`/backend`, Maven multi-module)
| Technology                  | Version | Purpose                          |
|-----------------------------|---------|----------------------------------|
| Java                        | 17      | Runtime                          |
| Spring Boot                 | 3.2.5   | Application framework            |
| Spring Security + OAuth2    | -       | JWT resource server              |
| Spring Data JPA + Hibernate | -       | Database ORM                     |
| Spring WebSocket            | -       | Real-time messaging              |
| PostgreSQL                  | 16      | Primary database                 |
| Redis                       | 7       | Cache + session store            |
| Liquibase                   | -       | Database migrations              |
| AWS SDK S3                  | 1.12.720| S3-compatible media storage      |
| Keycloak Admin Client       | 24.0.3  | Server-side user/role management |
| Bucket4j                    | 0.12.10 | Rate limiting                    |
| Caffeine                    | 3.1.8   | In-memory cache                  |
| Micrometer + Prometheus     | -       | Metrics export                   |
| Zipkin (Brave)              | -       | Distributed tracing              |
| Logback JSON                | -       | Structured logging               |
| SpringDoc OpenAPI           | 2.5.0   | API documentation (Swagger UI)   |
| FFmpeg                      | -       | Media transcoding (HLS)          |

### Infrastructure (Docker Compose)
| Service       | Image                          | Role                         |
|---------------|--------------------------------|------------------------------|
| frontend      | nginx:alpine (serves React build) | SPA hosting via nginx      |
| backend       | eclipse-temurin:17-jre-alpine  | Java application server      |
| postgres      | postgres:16-alpine             | Relational database          |
| redis         | redis:7-alpine                 | Cache & session store        |
| keycloak      | quay.io/keycloak/keycloak:24.0.5 | IDP / SSO                 |
| nginx         | nginx:1.26-alpine              | Reverse proxy, SSL, rate limiting |
| nginx-cdn     | nginx:1.26-alpine              | CDN for HLS segments         |
| rtmp          | alfg/nginx-rtmp                | RTMP ingest → HLS transcoding |
| coturn        | coturn/coturn:4.6.2            | TURN server for WebRTC       |
| prometheus    | prom/prometheus:v2.53.0        | Metrics collection           |
| grafana       | grafana/grafana:11.1.0         | Monitoring dashboards        |
| elasticsearch | elasticsearch:8.11.0           | Log storage                  |
| logstash      | logstash:8.11.0                | Log shipping / parsing       |
| kibana        | kibana/kibana:8.11.0           | Log visualization            |
| zipkin        | openzipkin/zipkin:3.4.0        | Distributed tracing          |
| alertmanager  | prom/alertmanager:v0.27.0      | Alert routing                |
| backup        | postgres:15                    | Scheduled DB backups         |

---

## 3. Setup Instructions

### Prerequisites
- **Docker** 24+ and **Docker Compose** v2
- **Node.js** 20+ (for frontend development)
- **Java** 17+ and **Maven** 3.9+ (for backend development)
- **Make** (optional, for convenience targets)
- SSL certificates for `platform.local`, `api.platform.local`, `auth.platform.local` (self-signed for dev)

### Quick Start (Full Stack via Docker)

```bash
# 1. Clone the repository
git clone <repo-url> platform
cd platform

# 2. (Optional) Create a .env file from template
cp .env.example .env
# Edit .env to set required secrets: MEDIA_ENCRYPTION_KEY, REDIS_PASSWORD, etc.

# 3. Generate self-signed SSL certificates (for development)
mkdir -p docker/nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout docker/nginx/ssl/platform.key \
  -out docker/nginx/ssl/platform.crt \
  -subj "/CN=platform.local"
# Repeat for api.platform.local and auth.platform.local

# 4. Build and start all services
docker compose build --no-cache
docker compose up -d

# 5. Verify the stack
docker compose ps
# All services should show "Up" or "healthy"
```

### Frontend Development (Standalone)

```bash
cd frontend
npm install
# Start dev server with hot reload (port 3000)
npm start
# Or build for production
npm run build
```

The development server expects Keycloak at `https://auth.platform.local/auth` and the API at `https://api.platform.local/api`. These can be overridden via environment variables (see `.env`).

### Backend Development (Standalone)

```bash
# Build the module
cd backend
mvn clean install -DskipTests

# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Required infrastructure (PostgreSQL, Redis, Keycloak) can be started separately:

```bash
docker compose up -d postgres redis keycloak
```

---

## 4. Architecture Overview

### System Layout

```
                         ┌──────────────┐
                         │   Internet   │
                         └──────┬───────┘
                                │ :443
                         ┌──────▼───────┐
                         │   nginx       │  Reverse proxy, SSL termination, rate limiting
                         │ (platform)   │
                         └───┬───┬───┬──┘
                        ┌────┘   │   └────┐
                   ┌────▼──┐ ┌──▼───┐ ┌──▼────┐
                   │nginx  │ │ nginx │ │ nginx │
                   │cdn    │ │front  │ │auth   │
                   │:8001  │ │  :3000│ │  :8080│
                   └───┬───┘ └──┬───┘ └──┬───┘
                       │        │        │
              ┌────────▼──┐ ┌──▼──┐ ┌───▼──────┐
              │ RTMP      │ │React│ │ Keycloak │
              │(rtmp)     │ │SPA  │ │ 24.0.5   │
              │ :1935/8000│ └─────┘ └───┬──────┘
              └─────┬─────┘            │
                    │ HLS              │ JDBC
              ┌─────▼─────┐      ┌─────▼──────┐
              │  Backend  │◄─────┤ PostgreSQL │
              │  :8081    │      │  16-alpine │
              │  Spring   │      └────────────┘
              │  Boot 3.2 │         ┌────────┐
              └─────┬─────┘         │ Redis  │
                    │               │   7    │
                    │               └────────┘
              ┌─────▼─────┐
              │  S3 / EBS │  Media storage
              └───────────┘
```

### Frontend Architecture

The frontend is a single-page application (SPA) with a modular component structure:

```
frontend/src/
├── App.js                    # Root component — auth init, tab routing
├── index.js                  # Entry point — ThemeProvider + ErrorBoundary
├── keycloak.js               # Keycloak singleton (auth lifecycle)
├── components/
│   ├── Navigation/           # NavTabs (tab bar with split support)
│   ├── Feed/                 # MainFeed, NutritionFeed
│   ├── Chat/                 # GeneralChat, NutritionChat
│   ├── Dashboard/            # Dashboard, Timer
│   ├── Events/               # EventsHub
│   ├── Archive/              # ArchiveList
│   ├── live/                 # LiveBroadcastViewSimple, EnhancedLiveBroadcastView, LivePlayer, etc.
│   ├── Notification/         # NotificationBell, GlobalEventNotification
│   ├── Admin/                # RoleSwitcher
│   ├── UserProfile/          # UserProfile
│   ├── Help/                 # Questions (FAQ)
│   ├── Scheduled/            # ScheduledEvents
│   └── common/               # ErrorBoundary, ConfirmModal, ProtectedRoute, etc.
├── services/
│   ├── api.js                # Axios client + all endpoint modules
│   └── mockApi.js            # Mock API for offline dev
├── hooks/
│   ├── useWebSocket.js       # WebSocket connection management
│   ├── usePermissions.js     # Role / level-based access control
│   ├── useAudioRecorder.js   # Browser microphone recording
│   ├── usePostAudioControls.js # Audio playback controls per post
│   ├── useTheme.js           # Dark/light theme toggling
│   ├── useRoles.js           # Role state management
│   └── live/                 # useLiveBroadcast, useLiveChat
├── context/
│   └── AudioPlayerContext.js # Global audio player state
├── utils/                    # safeStorage, chatLogger, chatLogViewer
└── constants/                # fallbackPosts, roles
```

**Key patterns:**
- **Tab-based routing**: The active tab is tracked in App state, not URL route. Each tab renders a corresponding component.
- **Global audio state**: AudioPlayerContext provides a unified audio player across all components.
- **Keycloak singleton**: Authentication state is initialized once and shared via imports/exports (no Redux).
- **JWT interceptors**: Axios request interceptor automatically attaches and refreshes the Bearer token.

### Backend Architecture (package layout)

```
backend/src/main/java/com/platform/
├── config/              # Security, Jackson, Resilience, ProductionGuard
├── controller/          # REST controllers
├── service/             # ChatService, MediaStorageService, EncryptionService, etc.
├── repository/          # JPA repositories
├── model/               # JPA entities (Post, Event, ChatMessage, etc.)
├── exception/           # GlobalExceptionHandler
└── util/                # FFmpegExecutor, etc.
```

### Key Design Patterns
- **Singleton Keycloak wrapper** — the Keycloak JS client is created once and reused across the app
- **Axios interceptors** — centralized JWT management and error handling
- **Custom hooks** — all shared stateful logic (WebSocket, permissions, audio) is extracted into hooks
- **CSS custom properties** — theme switching via the `data-theme` attribute on `<html>`
- **Docker multi-stage builds** — frontend: Node build → nginx serve; backend: Maven build → JRE runtime

---

## 5. Development Guide

### Environment Variables

| Variable                     | Default                                    | Description                       |
|------------------------------|--------------------------------------------|-----------------------------------|
| `REACT_APP_KEYCLOAK_URL`     | `https://auth.platform.local/auth`         | Keycloak server URL               |
| `REACT_APP_API_URL`          | `https://api.platform.local/api`           | Backend API base URL              |
| `REACT_APP_USE_MOCK_AUTH`    | `false`                                    | Use mock auth instead of Keycloak |
| `REACT_APP_ENVIRONMENT`      | `development`                              | Build target environment          |

### Key NPM Scripts (from `/frontend/package.json`)

```bash
npm start          # Development server (hot reload)
npm run build      # Production build
npm run build:prod # Production build with REACT_APP_ENVIRONMENT=production
npm test           # Run test suite
npm run lint       # ESLint check
npm run format     # Prettier formatting
npm run analyze    # Bundle size analysis via source-map-explorer
```

### Testing

Tests use **React Testing Library** and **Jest** (via CRA's built-in test runner):

```bash
cd frontend
npm test                    # Run all tests
npm test -- --coverage      # Run with coverage report
```

Test files live alongside their components in `__tests__/` directories:
- `frontend/src/__tests__/App.test.js`
- `frontend/src/__tests__/api.test.js`
- `frontend/src/__tests__/MainFeed.test.js`
- `frontend/src/__tests__/NavTabs.test.js`
- `frontend/src/__tests__/GeneralChat.test.js`
- `frontend/src/__tests__/Dashboard.test.js`
- `frontend/src/__tests__/LiveBroadcastView.test.js`
- `frontend/src/__tests__/usePermissions.test.js`
- `frontend/src/__tests__/useAudioRecorder.test.js`
- `frontend/src/__tests__/useWebSocket.test.js`
- `frontend/src/__tests__/AudioPlayerContext.test.js`

### Code Style

- Frontend: ESLint (default CRA config) + Prettier
- Backend: Standard Spring Boot conventions, Lombok for POJOs
- Commit messages: descriptive, in Russian or English

### Adding a New Tab

1. Create the component in `frontend/src/components/<Category>/`
2. Import it in `App.js` and add a case to `renderContent()` switch
3. Add the tab entry in `NavTabs.js` tabs array
4. Add the tab button/menu item if needed

---

## 6. Deployment Guide

### Production Build

```bash
# Full stack with Docker Compose
docker compose -f docker-compose.yml build
docker compose -f docker-compose.yml up -d

# Or individual builds
cd frontend && npm run build:prod
cd ../backend && mvn clean package -DskipTests
```

### SSL Certificates

Production requires valid SSL certificates for three domains:
- `platform.local` — main site
- `api.platform.local` — API endpoint
- `auth.platform.local` — Keycloak auth

Place them in `docker/nginx/ssl/` as:
- `platform.crt` / `platform.key`
- `api.crt` / `api.key`
- `auth.crt` / `auth.key`

### Environment-Specific Config

| File                          | Purpose                        |
|-------------------------------|--------------------------------|
| `backend/src/main/resources/application.yml` | Default Spring config         |
| `backend/src/main/resources/application-prod.yml` | Production overrides          |
| `.env`                        | Docker Compose secrets         |
| `frontend/.env`               | Frontend build-time variables  |

### Deployment Checklist

- [ ] SSL certificates generated and mounted
- [ ] `.env` populated with strong secrets (`MEDIA_ENCRYPTION_KEY`, `REDIS_PASSWORD`, `KEYCLOAK_SECRET`)
- [ ] Keycloak realm exported and placed at `docker/keycloak/realm-export.json`
- [ ] Database backup cron configured (Docker Compose `backup` service)
- [ ] Monitoring stack accessible (Grafana on port 3001, Kibana on 5601)
- [ ] Rate limiting tuned in `docker/nginx/conf.d/platform.conf`
- [ ] Resource limits adjusted in `docker-compose.yml` (`mem_limit`, `cpus`)
- [ ] `REACT_APP_ENVIRONMENT=production` set for frontend build

### Monitoring & Observability

| Tool       | Port | Access                              |
|------------|------|-------------------------------------|
| Grafana    | 3001 | `http://localhost:3001` (admin/admin) |
| Prometheus | 9090 | `http://localhost:9090`             |
| Kibana     | 5601 | `http://localhost:5601`             |
| Zipkin     | 9411 | `http://localhost:9411`             |
| Alertmanager | 9093 | `http://localhost:9093`            |

### Backup & Recovery

The `backup` service runs a daily PostgreSQL dump at 02:00. Backups are stored in `./backups/`.

```bash
# Manual backup
docker compose exec postgres pg_dump -U platform_user platform_db > backup_$(date +%Y%m%d).sql

# Restore
cat backup.sql | docker compose exec -T postgres psql -U platform_user platform_db
```

### Stopping & Cleanup

```bash
# Stop all services
docker compose down

# Stop and remove volumes (destroys data)
docker compose down -v

# Remove dangling images
docker image prune
```

---

## Project Structure (Top-Level)

```
platform/
├── frontend/              # React SPA
│   ├── public/            # Static assets + index.html
│   ├── src/               # Source code
│   ├── build/             # Production build output
│   ├── Dockerfile         # Multi-stage build (Node → nginx)
│   ├── nginx.conf         # Frontend nginx config
│   └── package.json
├── backend/               # Spring Boot application
│   ├── src/               # Java sources + tests
│   ├── Dockerfile         # Multi-stage build (Maven → JRE)
│   └── pom.xml
├── docker/                # Infrastructure configs
│   ├── nginx/             # Reverse proxy config
│   ├── postgres/          # init.sql
│   └── keycloak/          # realm-export.json
├── docker-compose.yml     # Multi-service orchestration
├── pom.xml                # Root Maven POM (parent)
├── prometheus.yml         # Prometheus scraping config
├── alertmanager.yml       # Alert routing rules
├── alerts.yml             # Alert definitions
├── logstash.conf          # Logstash pipeline
├── rtmp.conf              # RTMP → HLS transcoding config
├── nginx-cdn.conf         # CDN nginx config
├── scripts/               # Utility scripts (backup, etc.)
└── backups/               # Database dump destination
```
