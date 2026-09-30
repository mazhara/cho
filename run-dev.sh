#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Required command not found: %s\n' "$1" >&2
    exit 1
  fi
}

require_command docker
require_command sbt
require_command npm

if ! docker compose version >/dev/null 2>&1; then
  printf 'Docker Compose is not available. Install the Docker Compose plugin and try again.\n' >&2
  exit 1
fi

if [ ! -d app/node_modules ]; then
  printf 'Installing frontend dependencies...\n'
  (cd app && npm install)
fi

printf 'Starting PostgreSQL...\n'
docker compose up -d db --wait

printf 'Waiting for the library database to become ready...\n'
attempt=0
until docker compose exec -T db pg_isready -U docker -d library >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    printf 'PostgreSQL did not become ready within 60 seconds.\n' >&2
    exit 1
  fi
  sleep 1
done

docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/2_catalog_added_at.sql
docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/3_enable_pg_trgm.sql

printf 'Compiling frontend...\n'
sbt "app/fullOptJS"

cleanup() {
  trap - EXIT INT TERM
  for pid in "$BACKEND_PID" "$FRONTEND_WATCH_PID" "$PARCEL_PID"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || :
    fi
  done
  wait "$BACKEND_PID" "$FRONTEND_WATCH_PID" "$PARCEL_PID" 2>/dev/null || :
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

printf 'Starting backend on http://localhost:4041...\n'
sbt "server/run" &
BACKEND_PID=$!

printf 'Watching Scala.js sources for changes...\n'
sbt "~app/fullOptJS" &
FRONTEND_WATCH_PID=$!

printf 'Starting frontend on http://localhost:1234...\n'
(cd app && npm run start) &
PARCEL_PID=$!

printf 'Application is running. Press Ctrl+C to stop the backend and frontend.\n'
wait "$BACKEND_PID" "$FRONTEND_WATCH_PID" "$PARCEL_PID"
