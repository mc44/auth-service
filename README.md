# auth-service

Multi-tenant authentication API: login, refresh, logout, JWT issuance.

## Choose a guide

| | IDE | Docker |
|---|-----|--------|
| **Use when** | Local dev with IDE or `mvn spring-boot:run` | Laptop or VPS — same flow |
| **Config file** | `config/localhost.properties` | `deploy/.env` |
| **Prerequisites** | JDK, Maven, Mongo on `localhost:27017` | Docker Compose v2 |
| **Template** | [config/localhost.properties.example](config/localhost.properties.example) | [deploy/.env.example](deploy/.env.example) |

## IDE guide

1. **Clone**

```bash
# cwd: ~
git clone https://github.com/<you>/auth-service.git
cd auth-service
```

2. **Start Mongo** *(skip if Mongo already on `localhost:27017`)*

```bash
# cwd: auth-service/deploy/infrastructure
docker compose -f docker-compose.yml up -d mongo
```

3. **Create config**

```bash
# cwd: auth-service
cp config/localhost.properties.example config/localhost.properties
```

4. **Edit must-change fields** in `config/localhost.properties`:

| Property | How |
|----------|-----|
| `auth.jwt.secret` | `openssl rand -base64 48` → paste output |
| `seed.tenant.id` | Tenant id for login, e.g. `my-app` |
| `seed.user.email` | Bootstrap user email |
| `seed.user.password` | Bootstrap user password |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017` if using step 2 |

5. **Run**

```bash
# cwd: auth-service
mvn spring-boot:run
```

6. **Verify** — [Try the API](#try-the-api) (use IDE seed row).

## Docker guide

1. **Clone**

```bash
# cwd: ~
git clone https://github.com/<you>/auth-service.git
cd auth-service
```

2. **Check ports** (27017, 8081; 6379 if Redis)

```bash
# cwd: auth-service
chmod +x deploy/scripts/check-ports.sh
./deploy/scripts/check-ports.sh
```

3. **Create and edit `deploy/.env`**

```bash
# cwd: auth-service/deploy
cp .env.example .env
chmod 600 .env
```

Generate secrets and paste into `.env`:

```bash
echo "AUTH_JWT_SECRET=$(openssl rand -base64 48)"
echo "AUTH_REFRESH_TOKEN_HMAC_KEY=$(openssl rand -base64 32)"
```

| Variable | Required | How |
|----------|----------|-----|
| `AUTH_JWT_SECRET` | Yes | Output above — share with API gateway |
| `AUTH_REFRESH_TOKEN_HMAC_KEY` | Yes | Output above |
| `AUTH_SEED_TENANT_ID` | Yes | Tenant id, e.g. `my-app` |
| `AUTH_SEED_USER_EMAIL` | Yes | Bootstrap login email |
| `AUTH_SEED_USER_PASSWORD` | Yes | Bootstrap login password |
| `AUTH_SEED_USER_ROLE` | No | Default `ROLE_OPERATOR` |

Seed vars create tenant/user on first start only if absent.

4. **Start Mongo**

```bash
# cwd: auth-service/deploy/infrastructure
docker compose -f docker-compose.yml up -d mongo
```

5. **Start Redis** *(optional)*

Set `AUTH_REDIS_ENABLED=true` in `deploy/.env`, then:

```bash
# cwd: auth-service/deploy/infrastructure
docker compose -f docker-compose.yml --profile redis up -d redis
```

```bash
# cwd: auth-service
./deploy/scripts/check-ports.sh --with-redis
```

6. **Build and start auth-service**

```bash
# cwd: auth-service/deploy
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

7. **Verify** — [Try the API](#try-the-api) (use Docker seed row).

After updates: see [Pulling updates and redeploying](#pulling-updates-and-redeploying).

**Ports busy:** re-run `check-ports.sh`, stop conflicting containers (`docker ps`), or change the host port (left side) in `deploy/docker-compose.yml`. One auth stack per host.

> `docker-compose.dev.yml` at repo root bundles mongo + app in one command for a quick smoke test. Use this Docker guide for the supported flow.

## Try the API

Base URL: `http://127.0.0.1:8081`

| Guide | tenantId | email | password |
|-------|----------|-------|----------|
| IDE | `my-app` | `user@example.com` | `change-me` |
| Docker | `my-app` | value of `AUTH_SEED_USER_EMAIL` | value of `AUTH_SEED_USER_PASSWORD` |

**Health**

```bash
curl -s http://127.0.0.1:8081/actuator/health
```

**Login**

```bash
curl -s -X POST http://127.0.0.1:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"my-app","email":"user@example.com","password":"change-me"}'
```

Expected: JSON with `accessToken`, `refreshToken`, `roles`. Use your seed email/password for Docker.

**Refresh and logout**

```bash
REFRESH=$(curl -s -X POST http://127.0.0.1:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"my-app","email":"user@example.com","password":"change-me"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])")

curl -s -X POST http://127.0.0.1:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

curl -s -o /dev/null -w "%{http_code}\n" -X POST http://127.0.0.1:8081/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

Logout should print `204`.

## Reference

### API

| Method | Path | Body |
|--------|------|------|
| POST | `/auth/login` | `{ "tenantId", "email", "password" }` |
| POST | `/auth/refresh` | `{ "refreshToken" }` |
| POST | `/auth/logout` | `{ "refreshToken" }` |

JWT claims: `sub`, `tenantId`, `email`, `roles`.

### Gateway

Route `/auth/**` to this service. Gateway and auth-service must share `AUTH_JWT_SECRET`. See [docs/GATEWAY_INTEGRATION.md](docs/GATEWAY_INTEGRATION.md).

### Config mapping

| Concept | `deploy/.env` | `localhost.properties` |
|---------|---------------|------------------------|
| JWT secret | `AUTH_JWT_SECRET` | `auth.jwt.secret` |
| Refresh HMAC | `AUTH_REFRESH_TOKEN_HMAC_KEY` | *(Docker only)* |
| Bootstrap tenant | `AUTH_SEED_TENANT_ID` | `seed.tenant.id` |
| Bootstrap user | `AUTH_SEED_USER_EMAIL`, `AUTH_SEED_USER_PASSWORD` | `seed.user.email`, `seed.user.password` |
| Mongo URI | set in compose | `spring.data.mongodb.uri` |
| Redis | `AUTH_REDIS_ENABLED` | `auth.redis.enabled` |

Example files: [deploy/.env.example](deploy/.env.example), [config/localhost.properties.example](config/localhost.properties.example).

## FAQ

### Changing a user password

There is no `/auth/change-password` API. Update `passwordHash` in MongoDB (BCrypt). For bootstrap admin behavior see [Changing the default admin account](#changing-the-default-admin-account). For Mongo shell access see [Inspecting MongoDB and normal edits](#inspecting-mongodb-and-normal-edits).

1. Generate a BCrypt hash (Spring `BCryptPasswordEncoder`, strength 10):

```bash
python3 -c "import bcrypt; print(bcrypt.hashpw(b'NEW_PASSWORD', bcrypt.gensalt(rounds=10)).decode())"
```

(`pip install bcrypt` if needed.)

2. Update the user in Mongo (`auth` database, `users` collection):

```bash
docker exec auth-platform-mongo-1 mongosh auth --eval '
  db.users.updateOne(
    { tenantId: "my-app", email: "admin@example.com" },
    { $set: { passwordHash: "PASTE_BCRYPT_HASH", updatedAt: new Date() } }
  )
'
```

Adjust `tenantId`, `email`, and container name (`docker ps`). Optionally revoke active refresh tokens for that user (see [Inspecting MongoDB](#inspecting-mongodb-and-normal-edits)).

### Changing the default admin account

Editing seed creds and rerunning Docker **does not** change an existing admin. Seed logic is create-only — it skips when the tenant/user already exists.

| Situation | Result |
|-----------|--------|
| User **already exists** in Mongo | **No** — new `AUTH_SEED_USER_PASSWORD` / `seed.user.password` ignored |
| **First start**, config edited before app boot | **Yes** — bootstrap creates user with those creds |
| Changed `.env`, redeployed, login still fails | User was created on an earlier boot — use [password FAQ](#changing-a-user-password) |

| When | Action |
|------|--------|
| **Before first boot** | Set `AUTH_SEED_*` (Docker) or `seed.user.*` (IDE), then start app |
| **After first boot** | [Change password](#changing-a-user-password) in Mongo, or edit `email` in Mongo |
| **Dev reset only** | Wipe Mongo volume — see [Keeping auth data](#keeping-auth-data-across-docker-restarts) |

### Rotating keys

**`AUTH_JWT_SECRET` / `auth.jwt.secret`**

| Effect | Detail |
|--------|--------|
| Access tokens | Invalid immediately after redeploy |
| Gateway | Must use the **same** new secret — [docs/GATEWAY_INTEGRATION.md](docs/GATEWAY_INTEGRATION.md) |
| Refresh tokens | Still valid until expiry |

```bash
openssl rand -base64 48
# Update deploy/.env or localhost.properties, then redeploy (Docker guide step 6)
```

**`AUTH_REFRESH_TOKEN_HMAC_KEY`**

| Effect | Detail |
|--------|--------|
| Refresh tokens | All existing refresh tokens stop working |
| Users | Must log in again |

```bash
openssl rand -base64 32
# Update deploy/.env → ./scripts/deploy.sh
```

Suggested order: rotate JWT secret and update gateway first (access TTL default 15 min), then refresh HMAC key if you need all sessions cleared.

### Keeping auth data across Docker restarts

Normal restarts and redeploys keep data. Only `docker compose down -v` deletes it.

| Action | Data kept? |
|--------|------------|
| `docker compose up -d`, `restart`, `./scripts/deploy.sh` | Yes |
| `docker compose down` (no `-v`) | Yes — volume retained |
| `docker compose down -v` in `deploy/infrastructure/` | **No** — deletes `auth-platform_auth-mongo-data` |
| Rebuild auth-service container | Yes — app is stateless; data is in Mongo |

| Stack | Volume (typical) |
|-------|------------------|
| Docker guide (infra compose) | `auth-platform_auth-mongo-data` |
| `docker-compose.dev.yml` smoke test | `auth-dev_auth-dev-mongo` |

```bash
# cwd: auth-service/deploy/infrastructure  — NEVER in production:
docker compose -f docker-compose.yml down -v
```

Inspect volumes: `docker volume ls | grep auth`

### Pulling updates and redeploying

`git pull` then rebuild/restart the app only. Mongo, Redis, and `deploy/.env` stay as-is.

**Docker:**

```bash
# cwd: auth-service/deploy
git -C .. pull
./scripts/deploy.sh
```

| Component | On redeploy |
|-----------|-------------|
| auth-service container | Rebuilt and restarted |
| Mongo / Redis | Left running |
| `deploy/.env` | Untouched (gitignored) |
| Mongo data | Preserved — users, tenants, tokens remain |

```bash
curl -s http://127.0.0.1:8081/actuator/health
```

If `.env.example` gained new vars after pull, merge them manually into `deploy/.env` — do not overwrite `.env`.

**IDE:**

```bash
# cwd: auth-service
git pull
mvn spring-boot:run
```

Restart the JVM only. Update `config/localhost.properties` if the example file added new keys.

Restart infra (`deploy/infrastructure/`, `docker compose up -d`) only when compose files or Mongo/Redis images changed — never use `-v`.

### Inspecting MongoDB and normal edits

```bash
docker ps --format '{{.Names}}' | grep mongo
docker exec -it auth-platform-mongo-1 mongosh auth
```

One-shot:

```bash
docker exec auth-platform-mongo-1 mongosh auth --quiet --eval 'db.getCollectionNames()'
```

| Collection | Purpose |
|------------|---------|
| `tenants` | Tenant ids for login `tenantId` |
| `users` | Login accounts per tenant |
| `refresh_tokens` | Hashed refresh tokens and session state |

```javascript
db.tenants.find().pretty()
db.users.find({}, { tenantId: 1, email: 1, roles: 1, active: 1 }).pretty()
db.refresh_tokens.find({ status: "ACTIVE" }, { userId: 1, tenantId: 1, expiresAt: 1, status: 1 }).pretty()
```

| Collection | Field | When / how |
|------------|-------|------------|
| `users` | `passwordHash` | Change password — BCrypt only ([FAQ](#changing-a-user-password)) |
| `users` | `email` | Rename login email; unique per `tenantId` |
| `users` | `roles` | e.g. `["ROLE_OPERATOR"]` |
| `users` | `active` | `false` disables login |
| `tenants` | `active` | `false` blocks all logins for tenant |
| `tenants` | `name` | Display label; login uses `_id` |
| `refresh_tokens` | `status` | `REVOKED` invalidates session (`ACTIVE`, `ROTATED`, `REVOKED`, `EXPIRED`) |

Disable a user:

```javascript
db.users.updateOne(
  { tenantId: "my-app", email: "user@example.com" },
  { $set: { active: false, updatedAt: new Date() } }
)
```

Revoke all refresh tokens for a user (use `userId` from `users`):

```javascript
db.refresh_tokens.updateMany(
  { userId: "USER_ID_HERE", status: "ACTIVE" },
  { $set: { status: "REVOKED", updatedAt: new Date() } }
)
```

Do not edit casually: `refresh_tokens.tokenHash`, document `_id` fields, or plain-text passwords.
