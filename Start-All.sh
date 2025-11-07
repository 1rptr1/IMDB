#!/usr/bin/env bash
set -euo pipefail

API_URL="http://localhost:3001"
REBUILD=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --api-url)
      API_URL="$2"; shift 2 ;;
    --rebuild)
      REBUILD=true; shift ;;
    *)
      echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed or not on PATH. Install Docker and try again." >&2
  exit 1
fi

echo "== IMDB: Start All =="
echo "API URL for frontend build: $API_URL"

args=(compose build --build-arg "VITE_API_URL=${API_URL}")
if [[ "$REBUILD" == "true" ]]; then
  args+=(--no-cache)
fi

echo "Building images..."
docker "${args[@]}"

echo "Starting services (db, backend, frontend)..."
docker compose up -d

# Determine host IP (best-effort)
HOST_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"

echo
echo "=== Services ==="
echo "- PostgreSQL:\thost=localhost port=5432"
echo "- Backend:\t  http://localhost:3001/"
echo "- Frontend:\t http://localhost:8080/"

if [[ -n "${HOST_IP}" ]]; then
  echo
  echo "=== External Access (same network) ==="
  echo "- Backend:\t  http://${HOST_IP}:3001/"
  echo "- Frontend:\t http://${HOST_IP}:8080/"
fi

echo
echo "Done. Use 'docker compose logs -f' to tail logs, 'docker compose down' to stop."
