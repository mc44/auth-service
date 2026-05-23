# auth-service

Multi-tenant JWT auth (login, refresh, logout). MongoDB logical database: **`auth`**.

Designed to run **standalone** (this repo) or as part of the [blog-cms deploy stack](../deploy/README.md).

## Prerequisites

| Mode | Mongo |
|------|--------|
| **Solo dev** | Included: `docker compose -f docker-compose.dev.yml up --build` |
| **CMS / VPS** | Start [deploy/prereqs](../deploy/prereqs/README.md) first — shared `mongo` on network `cms-shared` |

Optional **Redis**: `docker compose --profile redis up -d` in `deploy/prereqs`, then set `AUTH_REDIS_ENABLED=true`.

## Local run (JAR)

```bash
cp config/localhost.properties.example config/localhost.properties
# Start Mongo on localhost:27017 (prereqs or dev compose)
mvn spring-boot:run
```

## Login (tenant-scoped)

```bash
curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"blog-cms","email":"user@example.com","password":"change-me"}'
```

JWT includes claims: `sub`, `tenantId`, `email`, `roles`.

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `AUTH_MONGODB_URI` | `mongodb://mongo:27017` | Mongo connection |
| `AUTH_MONGODB_DATABASE` | `auth` | Logical database |
| `AUTH_JWT_SECRET` | (required prod) | HS256 signing key (shared with gateway) |
| `AUTH_SEED_TENANT_ID` | `blog-cms` | Bootstrap tenant |
| `AUTH_SEED_USER_EMAIL` / `PASSWORD` | see `.env.example` | Dev seed user |
| `AUTH_REDIS_ENABLED` | `false` | Enable when Redis wired |

## Plug into blog-cms

1. Run `deploy/prereqs` (mongo on `cms-shared`).
2. Set the same `AUTH_JWT_SECRET` on gateway and auth.
3. Use `deploy/docker-compose.yml` or pull `ghcr.io/.../auth-service:<tag>`.

Consumer apps set `BLOG_TENANT_ID=blog-cms` and validate the `tenantId` JWT claim.

## Data durability

Mongo data lives in Docker volume `mongo-data` (prereqs) or `auth-dev-mongo` (solo dev). Avoid `docker compose down -v` unless resetting dev data.
