# auth-service

Multi-tenant authentication API: login, refresh, logout, JWT issuance.

## Deploy on a server

Use [deploy/README.md](deploy/README.md):

1. Start **MongoDB** (required).
2. Start **Redis** (optional).
3. Configure `deploy/.env` and start the **auth-service** container.

Infrastructure details: [deploy/infrastructure/README.md](deploy/infrastructure/README.md).

## Local development

```bash
docker compose -f docker-compose.dev.yml up --build
```

Or:

```bash
cp config/localhost.properties.example config/localhost.properties
mvn spring-boot:run
```

## API

| Method | Path | Body |
|--------|------|------|
| POST | `/auth/login` | `{ "tenantId", "email", "password" }` |
| POST | `/auth/refresh` | `{ "refreshToken" }` |
| POST | `/auth/logout` | `{ "refreshToken" }` |

JWT claims: `sub`, `tenantId`, `email`, `roles`.

## Gateway integration

Consumers (API gateway) route `/auth/**` to this service and use the same `AUTH_JWT_SECRET`. See [docs/GATEWAY_INTEGRATION.md](docs/GATEWAY_INTEGRATION.md).

## Configuration

| Variable | Description |
|----------|-------------|
| `AUTH_MONGODB_URI` | Default `mongodb://mongo:27017` |
| `AUTH_MONGODB_DATABASE` | `auth` |
| `AUTH_JWT_SECRET` | Required in production |
| `AUTH_REFRESH_TOKEN_HMAC_KEY` | Refresh token hashing |
| `AUTH_SEED_TENANT_ID` | On first startup, creates this tenant + seed user if missing; login `tenantId` must match |
| `AUTH_REDIS_ENABLED` | `true` when Redis container is running |

Full list: [deploy/.env.example](deploy/.env.example).
