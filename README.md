# Cho Library

Cho is a library application with a Scala 3/http4s backend, a Scala.js/Tyrian
frontend, and a PostgreSQL database.

## Development setup

### Requirements

- Java (JDK) compatible with Scala 3
- sbt 1.9.9
- Node.js and npm
- Docker with the Compose plugin (`docker compose`)

### Start the application

From the repository root, run:

```sh
./run-dev.sh
```

On the first run, the script installs the frontend npm dependencies if needed,
starts PostgreSQL, waits for the `library` database to be ready, compiles the
frontend, and starts the backend and frontend development servers. It then
keeps the frontend Scala.js compiler watching for changes. Press `Ctrl+C` to
stop the application processes.

Open <http://localhost:1234>. The backend listens on
<http://localhost:4041>; PostgreSQL is available on port `5432`.

The Compose database is initialized from `sql/0_init.sql` and the numbered SQL
scripts when its container is first created. `run-dev.sh` also applies the
idempotent catalog timestamp migration and enables PostgreSQL's `pg_trgm`
extension for typo-tolerant search. It also creates the events table 
then creates/upgrades the users table with `ADMIN`,
`LIBRARIAN`, and `READER` roles. Visit <http://localhost:1234/admin> to preview
the role-oriented administration dashboard; its metrics and account lists are
currently stub data. The dashboard's frontend role check is only a UX gate and
must not replace server-side authorization for protected data. Sign-in,
registration, and password recovery are available at `/login`, `/signup`,
`/forgotpassword`, and `/recoverpassword`. New registrations require consent to
personal-data processing. The Compose setup does not
use a persistent volume, so removing the database container also removes its
data. The local development configuration connects as user `docker` with
password `docker` to database `library`.

### Run services manually

If you prefer separate terminals, use the following commands from the
repository root:

```sh
docker compose up -d db
docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/2_catalog_added_at.sql
docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/3_enable_pg_trgm.sql
docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/4_events.sql
docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/5_users_and_roles.sql
docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/6_auth_profile_fields.sql
docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/7_schema_integrity.sql

sbt "server/run"
```

In another terminal, compile the frontend and start Parcel:

```sh
sbt "app/fullOptJS"
cd app
npm install
npm run start
```

For frontend development, keep the Scala.js compiler watching in an additional
terminal:

```sh
sbt "~app/fullOptJS"
```

The frontend imports the optimized Scala.js output from
`app/target/scala-3.3.3/app-opt`, so use `fullOptJS` for local serving as well
as deployment.

## Backend catalog behavior

Book updates replace the author associations together with the book details in
one transaction; omitted authors or an empty author map clears the associations.
Author name searches are case-insensitive. Updating books or authors requires an
`ADMIN` or `LIBRARIAN` role; these permissions are not based on ownership.
`GET /events` returns a JSON list (including an empty list) on success and a
structured `ErrorResponse` with HTTP 500 if fetching events fails.

## Production build

Build the backend distribution and frontend assets with:

```sh
sbt "server/packageZipTarball" "app/fullOptJS"
cd app && npm run build
```

The backend archive is written to `server/target/universal/`; Parcel writes
production frontend assets to `app/dist/`.
