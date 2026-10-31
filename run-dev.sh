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
require_command lsof
require_command curl

run_sbt() {
  sbt -Dsbt.boot.server=false -Dsbt.server.autostart=false "$@"
}

free_port() {
  local port="$1" pid attempt
  local pids
  pids="$(lsof -nP -tiTCP:"$port" -sTCP:LISTEN || true)"
  [ -n "$pids" ] || return 0

  for pid in $pids; do
    printf 'Stopping process %s listening on port %s...\n' "$pid" "$port"
    if ! kill "$pid" 2>/dev/null && kill -0 "$pid" 2>/dev/null; then
      printf 'Cannot stop process %s on port %s. Check its ownership and permissions.\n' "$pid" "$port" >&2
      return 1
    fi
  done
  for attempt in 1 2 3 4 5; do
    if ! lsof -nP -tiTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  for pid in $pids; do
    if lsof -nP -a -p "$pid" -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      printf 'Process %s did not stop; forcing it to release port %s.\n' "$pid" "$port"
      kill -KILL "$pid"
    fi
  done
  sleep 1
  if lsof -nP -tiTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    printf 'Port %s is still occupied; aborting startup.\n' "$port" >&2
    return 1
  fi
}

PIDS=()

stop_process_tree() {
  local pid="$1" child
  for child in $(ps -axo pid=,ppid= | awk -v parent="$pid" '$2 == parent { print $1 }'); do
    stop_process_tree "$child"
  done
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
  fi
}

cleanup() {
  trap - EXIT INT TERM
  for pid in ${PIDS[@]+"${PIDS[@]}"}; do
    stop_process_tree "$pid"
  done
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

check_processes() {
  local pid
  for pid in ${PIDS[@]+"${PIDS[@]}"}; do
    if ! kill -0 "$pid" 2>/dev/null; then
      printf 'Development process %s exited unexpectedly. Check its output above.\n' "$pid" >&2
      return 1
    fi
  done
}

wait_for_url() {
  local url="$1" attempt
  for ((attempt = 0; attempt < 180; attempt++)); do
    check_processes || return 1
    if curl --fail --silent --output /dev/null --max-time 2 "$url"; then
      return 0
    fi
    sleep 1
  done
  printf 'Server did not become ready at %s. Check its output above.\n' "$url" >&2
  return 1
}

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
run_sbt "app/fullOptJS"

free_port 4041
free_port 1234

printf 'Starting backend on http://localhost:4041...\n'
run_sbt "server/run" &
BACKEND_PID=$!
PIDS+=("$BACKEND_PID")

printf 'Waiting for the backend before starting the Scala.js watcher...\n'
wait_for_url http://localhost:4041/api/health

printf 'Watching Scala.js sources for changes...\n'
run_sbt "~app/fullOptJS" &
FRONTEND_WATCH_PID=$!
PIDS+=("$FRONTEND_WATCH_PID")

printf 'Starting frontend on http://localhost:1234...\n'
(cd app && exec npm run start -- --port 1234) &
PARCEL_PID=$!
PIDS+=("$PARCEL_PID")

printf 'Waiting for backend and frontend servers...\n'
wait_for_url http://localhost:4041/api/health
wait_for_url http://localhost:1234
printf 'Application is running. Press Ctrl+C to stop the backend and frontend.\n'
while check_processes; do
  sleep 1
done
exit 1
