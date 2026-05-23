# Deploy auth-service on a server

Run everything from a **git clone** on the VPS. Images are **built on the server** with Docker Compose (`--build`). You do not need GHCR or GitHub Actions for this flow.

## Prerequisites

- Docker Engine and Docker Compose v2
- Git clone of this repository on the server, e.g. `~/auth-service`

```bash
git clone https://github.com/<you>/auth-service.git
cd auth-service
```

## What gets started


| Layer | Path                    | Containers                                                   |
| ----- | ----------------------- | ------------------------------------------------------------ |
| Data  | `deploy/infrastructure` | MongoDB (required), Redis (optional)                         |
| App   | `deploy`                | `auth-service` on port **8081**, network `**auth-platform`** |


Details for Mongo/Redis: [infrastructure/README.md](infrastructure/README.md).

## Check host ports (avoid conflicts)

This stack binds these **host** ports by default:

| Port | Service |
|------|---------|
| 27017 | Auth MongoDB |
| 6379 | Auth Redis (optional, `--profile redis`) |
| 8081 | auth-service API |

If something else already listens (another app, old Tomcat, a second auth install), Compose will fail or traffic will hit the wrong process.

**Before Step 1** (infrastructure) and **before Step 4** (app), run:

```bash
chmod +x deploy/scripts/check-ports.sh

# Mongo + API
./deploy/scripts/check-ports.sh

# If you use Redis
./deploy/scripts/check-ports.sh --with-redis
```

`deploy/scripts/deploy.sh` runs the 8081 check automatically (and 6379 when `AUTH_REDIS_ENABLED=true`).

**Manual inspection (Linux VPS):**

```bash
ss -tlnp | grep -E ':(27017|6379|8081)\b'
docker ps --format 'table {{.Names}}\t{{.Ports}}'
```

**If a port is taken:**

1. Stop the other service, or stop an old container: `docker ps` then `docker stop <name>`.
2. Or change the **left** side of the port mapping in compose (host port), e.g. in `deploy/docker-compose.yml` use `"9081:8081"` instead of `8081`, then use `http://127.0.0.1:9081` for health checks.
3. Do not run two auth stacks on the same host with the same host ports.

## Step 1 — Start MongoDB

```bash
./deploy/scripts/check-ports.sh   # 27017 must be free or auth-platform mongo

cd deploy/infrastructure
docker compose -f docker-compose.yml up -d mongo
```

Check:

```bash
docker compose -f docker-compose.yml ps
docker compose -f docker-compose.yml logs mongo --tail 20
```

## Step 2 — Start Redis (optional)

Skip unless you use Redis-backed features (`AUTH_REDIS_ENABLED=true`).

```bash
cd deploy/infrastructure
docker compose -f docker-compose.yml --profile redis up -d redis
```

Set in `deploy/.env` before starting the app:

```bash
AUTH_REDIS_ENABLED=true
```

## Step 3 — Configure secrets and bootstrap user

```bash
cd deploy
cp .env.example .env
chmod 600 .env
```

Edit `.env` — at minimum:


| Variable                                           | Purpose                                                       |
| -------------------------------------------------- | ------------------------------------------------------------- |
| `AUTH_JWT_SECRET`                                  | Signs access tokens — generate with `openssl rand -base64 48` |
| `AUTH_REFRESH_TOKEN_HMAC_KEY`                      | Hashes refresh tokens — `openssl rand -base64 32`             |
| `AUTH_SEED_TENANT_ID`                              | First tenant id created on startup (see below)                |
| `AUTH_SEED_USER_EMAIL` / `AUTH_SEED_USER_PASSWORD` | First login user for that tenant                              |
| `AUTH_SEED_USER_ROLE`                              | Role on the seeded user (default `ROLE_OPERATOR`)             |


Share `**AUTH_JWT_SECRET**` with any API gateway that validates JWTs from this service.

### What `AUTH_SEED_TENANT_ID` is for

On **first startup**, auth-service ensures MongoDB has:

1. A **tenant** with id `AUTH_SEED_TENANT_ID` (if missing).
2. A **user** under that tenant with `AUTH_SEED_USER_EMAIL` / password (if missing).

This is **bootstrap data only** — it does not run on every request. Existing tenants/users are left unchanged if they already exist.

**Login always requires `tenantId` in the body**, and it must match a tenant in the database:

```json
{ "tenantId": "my-app", "email": "admin@example.com", "password": "..." }
```

The JWT then includes a `tenantId` claim so downstream services can scope data per tenant.

For production you can change seed env vars after the first boot; they only affect creation when records are absent. Add more tenants/users via your own admin flow or API later.

## Step 4 — Build and start auth-service

```bash
cd deploy
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

`deploy.sh` runs `docker compose up -d --build auth-service` (builds from the repo `Dockerfile`, no registry required).

Or manually:

```bash
cd deploy
set -a && source .env && set +a
docker compose -f docker-compose.yml up -d --build auth-service
```

## Verify

```bash
curl -s http://127.0.0.1:8081/actuator/health

curl -s -X POST http://127.0.0.1:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"my-app","email":"admin@example.com","password":"YOUR_SEED_PASSWORD"}'
```

Use the same `tenantId` as `AUTH_SEED_TENANT_ID` and the same email/password as in `.env`.

## Redeploy after `git pull`

Mongo and Redis stay running. Rebuild only the app:

```bash
cd deploy
git pull
./scripts/deploy.sh
```

## Data safety

Do not run `docker compose down -v` under `deploy/infrastructure` in production — that removes Mongo data.

## API gateway

If a separate gateway routes traffic to this service, see [../docs/GATEWAY_INTEGRATION.md](../docs/GATEWAY_INTEGRATION.md).

---

## Pending: CI/CD (GitHub Actions + GHCR)

Not required for the clone-and-build flow above. Planned later:

1. **GitHub Actions** on push to `main`: `mvn verify`, `docker build`, push to **GHCR** (`ghcr.io/<owner>/auth-service:<git-sha>`).
2. **VPS deploy job** (optional): SSH to the server, set `AUTH_IMAGE_TAG=<sha>`, `docker compose pull` + `up -d` (no `--build`).
3. **Secrets**: `AUTH_JWT_`* stay in server `deploy/.env` only — never in the image or public repo.
4. **Pin tags** in production (use commit SHA, not floating `latest`).

Workflow stub: [.github/workflows/publish.yml](../.github/workflows/publish.yml) (build/push today; VPS deploy step to be added).