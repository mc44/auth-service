#!/usr/bin/env bash
# Fail if host ports are taken by a non-Docker process, or by an unexpected container.
set -euo pipefail

die() { echo "check-ports: $*" >&2; exit 1; }

port_in_use() {
  local port=$1
  if command -v ss >/dev/null 2>&1; then
    ss -H -ltn "sport = :$port" 2>/dev/null | grep -q .
    return $?
  fi
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  die "Need ss (Linux) or lsof to check ports"
}

docker_on_port() {
  local port=$1
  docker ps --format '{{.Names}}\t{{.Ports}}' 2>/dev/null \
    | grep -E "(:|\\])${port}->" \
    | head -1 \
    || true
}

# Container name prefixes we expect from this repo's compose files.
expected_container() {
  local name=$1
  [[ "$name" == auth-service ]] \
    || [[ "$name" == auth-platform-mongo-* ]] \
    || [[ "$name" == auth-platform-redis-* ]] \
    || [[ "$name" == auth-app-auth-service-* ]]
}

check_one() {
  local port=$1
  if ! port_in_use "$port"; then
    echo "  OK   $port — free"
    return 0
  fi

  local line name
  line=$(docker_on_port "$port")
  if [[ -z "$line" ]]; then
    echo "  FAIL $port — in use on the host (not published by a Docker container we recognize)"
    echo "       Inspect: ss -tlnp | grep :$port   OR   sudo lsof -nP -iTCP:$port -sTCP:LISTEN"
    return 1
  fi

  name=${line%%$'\t'*}
  if expected_container "$name"; then
    echo "  OK   $port — Docker ($name)"
    return 0
  fi

  echo "  FAIL $port — Docker ($name) is using it; stop that container or change the host port in compose"
  return 1
}

PORTS=(27017 8081)
if [[ "${1:-}" == "--with-redis" ]]; then
  PORTS=(27017 6379 8081)
  shift
fi
if [[ $# -gt 0 ]]; then
  PORTS=("$@")
fi

echo "Checking host ports: ${PORTS[*]}"
failed=0
for p in "${PORTS[@]}"; do
  check_one "$p" || failed=1
done

if [[ "$failed" -ne 0 ]]; then
  echo
  die "Free or reassign conflicting ports before continuing. To change auth API port, edit host mapping in deploy/docker-compose.yml (e.g. \"9081:8081\") and use that port in curl/health checks."
fi
