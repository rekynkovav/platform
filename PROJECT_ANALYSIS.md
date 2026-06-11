# Project Analysis - Platform

## Overview
React frontend + Java Spring Boot backend + Docker deployment

## Frontend Structure
- **Framework**: React 18 with React Router
- **UI**: Custom CSS (no UI library)
- **Auth**: Keycloak integration
- **State**: React hooks (useState, useEffect)

## Navigation Tabs
1. **Лента Новостей** (Feed) - split: Питание + Новости
2. **Трансляции** (Archive) - Live broadcasts
3. **События** (Events) - Scheduled events
4. **Навигация** (Dashboard) - Dashboard
5. **Чат** (Chat) - split: Основной + Питание
6. **Помощь** (Help) - FAQ/Questions

## Key Components
- MainFeed / NutritionFeed - News feeds
- ArchiveList - Live broadcast archive
- ScheduledEvents - Event management
- Dashboard - Navigation dashboard
- GeneralChat / NutritionChat - Chat system
- Questions - Help/FAQ
- LiveBroadcastViewSimple - Live streaming
- RoleSwitcher - Admin role management
- NotificationBell / GlobalEventNotification - Notifications
- UserProfile - User profile management

## Backend Structure
- **Framework**: Java Spring Boot
- **Database**: PostgreSQL
- **Cache**: Redis
- **Auth**: Keycloak
- **Monitoring**: Prometheus + Grafana

## Docker Services
- frontend (React)
- backend (Spring Boot)
- postgres (PostgreSQL)
- redis (Redis)
- keycloak (Auth)
- nginx (Reverse proxy)
- prometheus (Metrics)
- grafana (Dashboards)
- alertmanager (Alerts)

## Issues Found
1. Duplicate live components (LiveBroadcastView.js, .tsx, Simple)
2. Old/backup files (.old, .bak)
3. Empty files
4. Temp files
5. Log files
6. Missing .gitignore entries

## Recommendations
1. Consolidate live components
2. Add proper TypeScript types
3. Implement error boundaries
4. Add unit tests
5. Set up CI/CD pipeline
6. Add environment validation
