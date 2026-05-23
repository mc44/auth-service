#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Missing deploy/.env — copy from deploy/.env.example"
  exit 1
fi

# shellcheck disable=SC1091
set -a && source .env && set +a

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ "${AUTH_REDIS_ENABLED:-false}" == "true" ]]; then
  "$SCRIPT_DIR/check-ports.sh" --with-redis
else
  "$SCRIPT_DIR/check-ports.sh"
fi

echo "Building and starting auth-service..."
docker compose -f docker-compose.yml up -d --build auth-service

echo "Auth API: http://localhost:8081/actuator/health"
