# Gateway integration

For teams that run an API gateway in front of **auth-service** (Spring Cloud Gateway, NGINX, etc.).

## HTTP contract

| Item | Value |
|------|--------|
| Base path | `/auth` |
| Login | `POST /auth/login` — `{ "tenantId", "email", "password" }` |
| Refresh | `POST /auth/refresh` |
| Logout | `POST /auth/logout` |
| JWT | HS256 |
| Claims for downstream | `sub`, `tenantId`, `email`, `roles` |

## Spring Cloud Gateway

### Route

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: ${AUTH_SERVICE_URL}
          predicates:
            - Path=/auth/**
```

`AUTH_SERVICE_URL` must reach a running auth-service instance (see [deploy/README.md](../deploy/README.md)).

### Shared secret

| Component | Variable |
|-----------|----------|
| auth-service | `AUTH_JWT_SECRET` |
| gateway | `AUTH_JWT_SECRET` |

Values must match. Gateway validates tokens; auth-service signs them.

### Permit auth routes

```java
.pathMatchers("/auth/**").permitAll()
```

Use `oauth2ResourceServer().jwt()` with the same secret (HS256).

### Forward headers (recommended)

| Header | JWT claim |
|--------|-----------|
| `X-User-Id` | `sub` |
| `X-Tenant-Id` | `tenantId` |

Downstream services should trust these only from the gateway.

## Docker networks

auth-service creates network **`auth-platform`** when you start [deploy/infrastructure](../deploy/infrastructure/docker-compose.yml).

To call auth by container name from another compose stack, attach the gateway to that network:

```yaml
networks:
  auth-platform:
    external: true
    name: auth-platform

services:
  gateway:
    networks:
      - auth-platform
      - your-app-network
    environment:
      AUTH_SERVICE_URL: http://auth-service:8081
```

Alternatively use the server’s host/IP and port `8081` if the gateway cannot join `auth-platform`.

## Tenant id

Login and JWT include `tenantId`. Consumer applications must use the same tenant id in login requests and when validating the JWT claim.

## Smoke test

```bash
curl -s -X POST "$GATEWAY_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"my-app","email":"user@example.com","password":"secret"}'
```

Protected routes: `Authorization: Bearer <accessToken>`.
