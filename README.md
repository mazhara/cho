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
If ports `4041` or `1234` are occupied, the script stops their listening
processes before starting the servers (including forced termination if they
do not exit within five seconds). It reports the application as running only
after the backend health endpoint and frontend respond. If a development
process exits, the launcher stops the remaining processes.
The launcher disables sbt's boot socket and automatic server for its own sbt
processes, and waits for backend startup before launching the Scala.js watcher
to avoid concurrent sbt boot-server collisions.

Open <http://localhost:1234>. The backend listens on
<http://localhost:4041>; PostgreSQL is available on port `5432`.

The Compose database is initialized from `sql/0_init.sql` and the numbered SQL
scripts when its container is first created; these scripts create the events
table and the users table with `ADMIN`, `LIBRARIAN`, and `READER` roles. On
subsequent starts, `run-dev.sh` reapplies the idempotent catalog timestamp
migration and enables PostgreSQL's `pg_trgm` extension for typo-tolerant search.
It does not apply the events or users migrations to an existing database; use
the commands in “Run services manually” when needed. Visit
<http://localhost:1234/admin> to preview the role-oriented administration
dashboard. Its user and copy totals are loaded from the database, but the
chart, account lists, and management controls are still hard-coded and not
connected to backend operations. The header displays the signed-in user's name
and role from the login response.
The admin header also shows local time, refreshed every minute. Its settings
button opens a password-change dialog with current-password, new-password, and
confirmation fields. A successful change clears the local session and returns
the user to the login page, since the backend invalidates existing sessions.
Each field has its own password-visibility toggle. The new password must meet
the registration password policy, differ from the current password, and match
the confirmation. Password reuse is also rejected by the backend.
Authenticated administrators and librarians also see an administration link in
the main navigation.
The page renders only when the browser has a session token, login email, and
`ADMIN` or `LIBRARIAN` role; incomplete older sessions must sign in again.
Otherwise it shows the login page. This frontend role check is only a UX gate
and must not replace server-side authorization for protected data. Sign-in,
registration, and password recovery are available at `/login`, `/signup`,
`/forgotpassword`, and `/recoverpassword`. New registrations require consent to
personal-data processing and a valid international telephone number with a country
code (for example `+380501234567` or `+33612345678`). Spaces, parentheses, and
hyphens are accepted; numbers are stored in normalized international format.
New passwords must contain 8–64 characters, including uppercase and lowercase
Latin letters, a digit, and a symbol. This policy also applies to password changes
and recovery, but existing passwords remain valid for login. Login and sign-up
passwords are hidden by default. Login has an eye icon to toggle visibility;
sign-up has an eye icon in each password field, and either icon toggles both
password fields together.
Auth inputs bind their live DOM values to page state, so navigating between
registration and login does not carry over text from the previous form.
Library staff can open `/admin/books` to search and
page through the catalog, inspect copies, add books/authors/publishers, manage
copies, and delete books. These catalog mutations are also protected by
server-side staff-role checks. The Compose setup does not
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
docker compose exec -T db psql -U docker -d library -f /docker-entrypoint-initdb.d/7_publishers_country.sql
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

Email delivery uses the SMTP settings in `email-service-config`; set
`CHO_SMTP_FROM` to override the default From address (`corem@corem.corp`).
Password-recovery token lifetime is configured with an explicit duration in
`token-config.token-duration` (default: `"10 minutes"`).
SMTP delivery requires STARTTLS with a trusted certificate matching the SMTP
hostname; plaintext SMTP and self-signed certificates are rejected. Recovery
emails include the code to enter on `/recoverpassword`. Successful password
changes and recovery invalidate all previously issued sessions for the account;
users must sign in again.

Production must serve the frontend and API over HTTPS, typically through a
TLS-terminating reverse proxy. Restrict direct access to the backend HTTP port
to that proxy; the local HTTP development setup is not suitable for public
deployment. Browser bearer tokens are stored in `sessionStorage`, not passwords,
and must be protected against XSS.

The `ApiError` JSON codec includes `resourceType` and `identifier` for `not_found`
errors so decoding preserves the original resource and identifier. The separate
`ErrorResponse` HTTP envelope remains unchanged.

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
