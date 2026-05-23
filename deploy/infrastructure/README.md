# Infrastructure for auth-service

MongoDB and optional Redis for the auth microservice. Start these **before** [../README.md](../README.md) step 4 (auth-service container).

## Containers

| Service | Image | Container (typical) | Port on host | Hostname on `auth-platform` |
|---------|-------|---------------------|--------------|----------------------------|
| `mongo` | `mongo:7` | `auth-platform-mongo-1` | 27017 | `mongo` |
| `redis` | `redis:7-alpine` | `auth-platform-redis-1` | 6379 | `redis` |

Volume for Mongo: **`auth-platform_auth-mongo-data`**

Network: **`auth-platform`** (auth-service joins this network when deployed).

## Port check

From repository root, ensure **27017** (and **6379** if using Redis) are free or already used by this stack:

```bash
./deploy/scripts/check-ports.sh
./deploy/scripts/check-ports.sh --with-redis   # when starting Redis
```

See [../README.md](../README.md#check-host-ports-avoid-conflicts).

## Commands

From repository root:

```bash
# 1. MongoDB (required)
cd deploy/infrastructure
docker compose -f docker-compose.yml up -d mongo

# 2. Redis (optional)
docker compose -f docker-compose.yml --profile redis up -d redis
```

## Verify MongoDB

```bash
docker compose -f docker-compose.yml ps
docker exec auth-platform-mongo-1 mongosh --quiet --eval 'db.adminCommand({ ping: 1 })'
```

## Verify Redis (if started)

```bash
redis-cli -h 127.0.0.1 -p 6379 ping
```

## Next step

[../README.md](../README.md) — configure `deploy/.env` and start **auth-service**.
