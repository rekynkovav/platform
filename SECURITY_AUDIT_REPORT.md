# Security Audit Report — Platform Project
**Date:** 2026-06-12  
**Scope:** /home/suveren/platform (backend, frontend, docker, keycloak, nginx, infrastructure)

---

## CRITICAL (CVSS 9.0-10.0)

### C-01: Hardcoded production secrets in docker-compose.yml
**File:** `docker-compose.yml`
**What:**
- PostgreSQL password `platform_pass_2026` hardcoded (lines 10, 107, 281)
- Keycloak admin password `admin` (line 41)
- Grafana password `admin` (line 194)
- Coturn user:password `platform:platform123` (line 313)
- Keycloak client secret default `backend-secret-2026` (line 113)
**Why:** All these are committed to git, visible to anyone with repo access. 
**Fix:** Use `${POSTGRES_PASSWORD}` env vars from `.env` file everywhere; never hardcode production passwords.

### C-02: Hardcoded user passwords in Keycloak realm export
**File:** `docker/keycloak/realm-export.json`
**What:** Every user has a plaintext hardcoded password:
- `superadmin`:`admin123` (line 54)
- `moderator_admin`:`moderator123` (line 64)
- `user0`,`user1`,`user2`:`password` (lines 74, 84, 94)
- `vyacheslav`:`vyacheslav123` (line 134)
**Why:** The realm export is committed to git. Anyone with repo access knows all passwords.
**Fix:** Remove user credentials from realm export; use Keycloak Admin API to set passwords at deployment.

### C-03: Keycloak confidential client secret exposed in realm export
**File:** `docker/keycloak/realm-export.json`, line 37
**What:** `"secret": "backend-secret-2026"` — the `platform-backend` client's secret is hardcoded.
**Why:** This secret is used for client_credentials grant and service account authentication. Exposed in clear text.
**Fix:** Remove the secret from realm export; generate it at deployment time.

### C-04: XssSanitizer never used in production code
**File:** `backend/src/main/java/com/platform/util/XssSanitizer.java`
**What:** The `XssSanitizer` class exists with HTML escaping methods (`sanitize`, `sanitizeForHtml`) but is **never imported or called** by any controller, service, or handler. Only referenced in unit tests.
**Why:** User-generated content (chat messages, posts, comments, user bios) is stored and rendered without ANY sanitization. Stored XSS is trivially exploitable.
**Fix:** Apply `XssSanitizer.sanitize()` to all user input fields before storing, and/or use output encoding in the frontend. Never render raw user HTML.

---

## HIGH (CVSS 7.0-8.9)

### H-01: No Content-Security-Policy in nginx headers
**File:** `docker/nginx/conf.d/platform.conf`
**What:** Neither `platform.local` nor `api.platform.local` server blocks include a `Content-Security-Policy` HTTP header. The only CSP is via `<meta http-equiv>` in `frontend/public/index.html` (line 6-13), which is less restrictive than an HTTP header and can be overridden.
**Why:** Meta-tag CSP is not enforced for resources loaded before the meta tag. Also `script-src 'unsafe-inline'` allows inline scripts, reducing XSS protection.
**Fix:** Add CSP as an nginx `add_header` in all server blocks; restrict `script-src` to nonces/hashes instead of `'unsafe-inline'`.

### H-02: directAccessGrantsEnabled on both Keycloak clients
**File:** `docker/keycloak/realm-export.json`
**What:** Both `platform-frontend` (line 31) and `platform-backend` (line 43) have `directAccessGrantsEnabled: true`.
**Why:** ROPC (Resource Owner Password Credentials) grant allows direct password-based token acquisition via REST API, bypassing the PKCE/OAuth2 browser flow. Any backend service with client credentials can authenticate as any user.
**Fix:** Set `directAccessGrantsEnabled: false` on both clients. Use `standardFlowEnabled` with PKCE for frontend and `serviceAccountsEnabled` for backend.

### H-03: Conflicting/inconsistent role hierarchy
**Files:** `SecurityConfig.java`, `RoleUtils.java`, `realm-export.json`
**What:**
- In `realm-export.json` roles are lowercase: `super_admin`, `vyacheslav`, `moderator_admin`
- In `SecurityConfig.java` line 76: `ROLE_SUPER_ADMIN > ROLE_VYACHESLAV` (SUPER_ADMIN outranks VYACHESLAV)
- In `SecurityConfig.java` lines 85-87: `ROLE_VYACHESLAV > ROLE_USER_0/1/2` (VYACHESLAV can do what USER can)
- But in `RoleUtils.java` line 11: `ROLE_PRIORITY` lists SUPER_ADMIN first then VYACHESLAV
- In `SecureMediaService.java` lines 349-353: `isAdmin()` lists SUPER_ADMIN, VYACHESLAV, MODERATOR_ADMIN, DEVELOPER — no hierarchy
**Why:** Inconsistent authorization enforcement. Code uses `hasRole()` (Spring Security) which uses the `RoleHierarchy`, but `RoleUtils` and `SecureMediaService` use string equality checks that ignore the hierarchy.
**Fix:** Align role hierarchy: VYACHESLAV should be top-level owner, SUPER_ADMIN below. Use only Spring Security's role hierarchy checks, not string comparisons.

### H-04: Stored XSS in chat messages
**Files:** `ChatWebSocketHandler.java`, `ChatController.java`, `PostController.java`, `CommentController.java`
**What:** Chat message content (`handleTextMessage` line 114: `payloadObj.getContent()`), POST content, and comments are stored and retrieved without any XSS sanitization.
**Why:** If user sends `<script>alert(1)</script>` as a message, it's stored in DB and broadcast to all room participants. If frontend uses `dangerouslySetInnerHTML` or React's default escaping is bypassed, XSS occurs.
**Fix:** Sanitize all user text input via `XssSanitizer.sanitize()` before storage AND encode on output in the frontend. Never trust user input.

### H-05: JWT audience (aud) validation missing
**File:** `backend/src/main/resources/application.yml`, lines 37-41
**What:** Spring Security OAuth2 resource server is configured only with `issuer-uri` and `jwk-set-uri`. There is NO `audience` validation.
**Why:** A JWT issued for any client in the same Keycloak realm could be accepted by the backend resource server. If a different client's token is compromised, it can access this platform's APIs.
**Fix:** Add `spring.security.oauth2.resourceserver.jwt.audiences: platform-backend` or implement a custom `JwtAuthenticationConverter` that validates the `aud` claim.

---

## MEDIUM (CVSS 4.0-6.9)

### M-01: WebSocket - no JWT in query parameter fallback for SockJS
**File:** `WebSocketHandshakeInterceptor.java`, `WebSocketConfig.java`
**What:** `WebSocketConfig.java` registers chat with `.withSockJS()` (line 31), which enables SockJS fallback transports (XHR, JSONP polling). SockJS transports CANNOT set custom HTTP headers. The handshake interceptor only reads JWT from `Authorization` header (line 36-42).
**Why:** SockJS fallback connections will always fail authentication because they can't send the Authorization header. Also, the `/ws/` path is open per SecurityConfig line 43.
**Fix:** Support `access_token` query parameter in WebSocket URL for SockJS transports. Validate JWT from `Authorization` header OR `token`/`access_token` parameter.

### M-02: BroadcastStats WebSocket lacks per-broadcast authorization
**File:** `BroadcastStatsWebSocketHandler.java`
**What:** After WebSocket handshake authentication, the handler (line 35-40) adds any session to the broadcast's session map without checking if the user has permission to view that specific broadcast's stats.
**Why:** Any authenticated user can connect to ANY broadcast's stats WebSocket by guessing/brute-forcing the broadcast ID.
**Fix:** Add permission check in `afterConnectionEstablished()` — verify the user has access to the specific broadcast before adding them to the session map.

### M-03: Rate limiting largely disabled
**Files:** `RateLimitingConfig.java`, `MediaController.java`
**What:**
- Bucket4j is disabled: `bucket4j.enabled: false` in both `application.yml` and `application-prod.yml`
- Media controller has ALL `@RateLimiting` annotations commented out (lines 30, 42, 58, 74, 92, 101, 111, 133)
- `IpRateLimitFilter` is in-memory Caffeine — resets on restart, not distributed, max 10000 IPs
**Why:** No effective rate limiting on media upload, stream key generation, or file download. An attacker can upload gigabytes, make unlimited stream requests, or brute-force file IDs.
**Fix:** Re-enable Bucket4j with Redis backend for distributed rate limiting; uncomment `@RateLimiting` annotations; set sensible limits per endpoint.

### M-04: `/actuator/` exposure through nginx
**File:** `docker/nginx/conf.d/platform.conf` lines 91-96, `SecurityConfig.java` line 43
**What:** `SecurityConfig.java` permits all to `/actuator/**`. Nginx's `api.platform.local` has `allow 127.0.0.1; deny all;` for `/actuator/`, but:
- The main `platform.local` server block proxies everything to frontend, which may expose actuator info
- If internal network access is compromised, actuators are wide open
**Why:** Spring Boot Actuator endpoints can leak sensitive information (env, metrics, health details, routes).
**Fix:** Remove `/actuator/**` from `permitAll`; require authentication; keep nginx `deny all` for non-internal requests.

### M-05: SockJS + /ws/ path open without authentication
**File:** `SecurityConfig.java` line 43, `WebSocketConfig.java` line 31
**What:** `/ws/**` is `permitAll` in SecurityConfig. The chat WebSocket uses `.withSockJS()` which enables XHR polling fallback. SockJS XHR endpoints would be accessible without authentication.
**Why:** While the handshake interceptor does authenticate, the `/ws/**` endpoints are open to unauthenticated requests until the interceptor runs. This could enable some SockJS attacks.
**Fix:** Remove `/ws/**` from `permitAll`; apply authentication at Spring Security level (but allow the pre-websocket HTTP requests for SockJS info endpoint).

### M-06: In-memory rate limit filter uses X-Forwarded-For without validation
**File:** `IpRateLimitFilter.java` lines 76-88
**What:** The IP rate limiter uses `X-Forwarded-For` header's first IP for rate limiting. This header can be spoofed by the client.
**Why:** An attacker can inject arbitrary `X-Forwarded-For` values to bypass IP-based rate limiting or cause denial of service to other users by spoofing their IP.
**Fix:** Only trust `X-Forwarded-For` from the proxy (nginx). Use `X-Real-IP` set by nginx, or validate against the proxy's known IP.

### M-07: No audit trail for admin operations
**Files:** `AdminController.java`, `KeycloakService.java`
**What:** Admin operations (role changes, user bans, payment confirmations) are only logged via `log.info()` at DEBUG or INFO level. No structured audit log is maintained for security-critical operations.
**Why:** In case of a security incident, there's no reliable audit trail to determine who changed what and when.
**Fix:** Implement centralized audit logging for all admin operations: before/after state, actor ID, timestamp, IP address.

### M-08: EditUsernameAllowed in Keycloak
**File:** `realm-export.json` line 12
**What:** `"editUsernameAllowed": true` allows users to change their username after creation.
**Why:** Users could impersonate others by changing to a taken username (if uniqueness isn't enforced), or cause confusion by frequently changing display identity.
**Fix:** Set `editUsernameAllowed: false`. Use a separate display name field for user-facing names.

---

## LOW (CVSS 1.0-3.9)

### L-01: Self-signed SSL certificates without SAN
**File:** `docker/nginx/ssl/generate-certs.sh`
**What:** Three separate self-signed certificates without Subject Alternative Names (SAN). Each domain gets its own cert.
**Why:** Browsers will show certificate warnings. No SAN means no wildcard support and each subdomain requires a separate cert/SNI configuration.
**Fix:** Generate a single wildcard cert `*.platform.local` with SAN; consider Let's Encrypt for production.

### L-02: Elasticsearch security disabled
**File:** `docker-compose.yml` line 205
**What:** `xpack.security.enabled=false` — Elasticsearch has no authentication.
**Why:** Anyone who can reach Elasticsearch port 9200 can read/modify all indexed data.
**Fix:** Enable xpack security and configure authentication.

### L-03: Redis exposed with default password
**File:** `docker-compose.yml` line 58-60
**What:** Redis runs with `--requirepass ${REDIS_PASSWORD:-redis_pass}` — default password `redis_pass`.
**Why:** Default passwords can be brute-forced. Redis is used for rate limiting data and WebSocket session registry.
**Fix:** Require `REDIS_PASSWORD` env var to be set (no default fallback).

### L-04: Grafana with default admin password
**File:** `docker-compose.yml` line 194
**What:** `GF_SECURITY_ADMIN_PASSWORD=admin`
**Why:** Default Grafana admin password is a well-known credential.
**Fix:** Set via `${GRAFANA_ADMIN_PASSWORD}` env var.

### L-05: Dev profile has 500MB file upload limit
**File:** `application.yml` lines 30-31
**What:** `max-file-size: 500MB, max-request-size: 500MB`
**Why:** Extremely large upload limit in dev could exhaust disk space or memory if exploited.
**Fix:** Reduce dev limit or match it with prod's 100MB.

### L-06: Detailed SQL logging in dev mode
**File:** `application.yml` lines 59-61
**What:** `org.hibernate.SQL: DEBUG`, `org.hibernate.type.descriptor.sql.BasicBinder: TRACE`
**Why:** SQL queries and bound parameter values are logged, potentially leaking sensitive data in logs.
**Fix:** Remove SQL debug logging in prod profile (already done); but dev should also limit it.

### L-07: No request body size limit on nginx
**File:** `docker/nginx/nginx.conf` line 32
**What:** `client_max_body_size 50M` — 50MB limit is still generous for most API calls. But backend allows 100MB (prod) so nginx would truncate.
**Why:** Moderate risk — large payloads could consume resources.
**Fix:** Align nginx client_max_body_size with backend limits.

---

## MISCELLANEOUS / OBSERVATIONS

### O-01: Keycloak `KC_HOSTNAME_STRICT: false`
Allows requests to Keycloak on any hostname, which can enable host header injection attacks in certain configurations.

### O-02: Backend Dockerfile generates pom.xml inline
The Dockerfile (`backend/Dockerfile` line 4) uses `echo` to create a parent POM file. This is fragile — any syntax error breaks the build.

### O-03: Payment details hardcoded in application.yml
Lines 82-88 contain hardcoded bank account details (BIC, corr account, INN, KPP) of what appears to be a real Russian bank account. These should be in environment variables, not in config files.

### O-04: WebSocket heartbeat approach
`ChatWebSocketHandler` sends `PingMessage` to all sessions every 30 seconds. If the session count is high, this creates significant overhead.

### O-05: `@PreAuthorize` on class level in AdminController
`AdminController` has `@PreAuthorize` at class level (MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV) and also on individual methods. The class-level annotation already restricts access.

---

## SUMMARY

| Severity | Count | Key Issues |
|----------|-------|------------|
| CRITICAL | 4 | Hardcoded passwords (docker-compose, realm-export), XssSanitizer unused, client secret exposed |
| HIGH     | 5 | No CSP header, directAccessGrants, role hierarchy inconsistency, stored XSS, missing JWT audience |
| MEDIUM   | 8 | WebSocket SockJS auth, broadcast perms, disabled rate limiting, actuator exposure, IP spoofing, audit trail, etc. |
| LOW      | 7 | Self-signed certs, Elasticsearch/Redis/Grafana configs, large upload limits, SQL logging |
| **Total** | **24** | |

### Most Critical Action Items
1. **Immediately:** Move ALL secrets to `.env` and gitignore; remove passwords from `realm-export.json` and `docker-compose.yml`
2. **Immediately:** Start using `XssSanitizer` on all user input before storing in database
3. **High priority:** Disable `directAccessGrantsEnabled`, add CSP headers at nginx level
4. **High priority:** Fix JWT audience validation, align role hierarchy
5. **Medium priority:** Re-enable distributed rate limiting, fix WebSocket auth fallback, add audit logging
