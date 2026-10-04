# todo

A backend REST API for a todo/task manager. It manages **todos with nested, time-blocked events** (calendar-style items) and **aggregated dashboard statistics**, secured with **JWT authentication delivered as httpOnly cookies**.

This repository is a **pure API server** — there is no frontend here. CORS is configured for a separate frontend running at `http://localhost:3000` by default.

## Tech stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Security) |
| Database | PostgreSQL |
| Auth | JJWT 0.13 (HMAC-signed JWTs), BCrypt password hashing |
| Build | Gradle 9.7.1 (wrapper included) |
| Tests | JUnit 5, MockMvc, Testcontainers (Postgres 16) |

## Architecture

The codebase follows a layered structure with a deliberate **two-layer model split**: JPA entities (`repository/entity/J*.java`) are separate from domain POJOs (`domain/entity/`), bridged by hand-written mappers.

```
src/main/java/mg/improve/todo/
├── TodoApplication.java       # @SpringBootApplication entry point
├── config/                    # SecurityConfig, JwtService, JwtAuthenticationFilter,
│                              # AuthCookieFactory, RestAuthenticationEntryPoint
├── endpoint/rest/             # REST controllers: Auth, Todo, Event, Stats
├── service/                   # AuthService, TodoService, EventService, StatsService
├── repository/                # Spring Data JPA repositories + JPA entities (entity/)
├── domain/
│   ├── entity/                # plain domain models (User, Todo, Event, RefreshToken)
│   ├── dto/request/           # request bodies (with partial-update field-presence flags)
│   ├── dto/response/          # response bodies + unified ErrorResponse
│   └── mappers/               # manual JPA ↔ domain mappers
├── validators/                # request validators (throw ValidationException)
└── exception/                 # GlobalExceptionHandler (@RestControllerAdvice)
```

## API

All IDs are UUIDs and all resources are **user-scoped** (foreign resources return 404, not 403).

Authentication model:

- The JWT **access token** is accepted from the `access_token` httpOnly cookie first, falling back to the `Authorization: Bearer` header.
- The **refresh token** lives in an httpOnly `refresh_token` cookie and is rotated on refresh when close to expiry.
- Sessions are stateless (Spring Security filter chain, CSRF disabled, BCrypt encoder).

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`, `DELETE /auth/me` |
| Todos | `GET /todos` (paginated; filters `isDone`, `title`, `page`, `perPage`), `POST /todos`, `GET/PATCH/DELETE /todos/{todoId}` |
| Events | `GET /todos/{todoId}/events`, `POST /todos/{todoId}/events` (409 on time overlap), `GET /events` (paginated; date-range filters), `GET/PATCH/DELETE /events/{eventId}` |
| Stats | `GET /stats` (undone count, upcoming events, closest deadline, calendar payload with optional `from`/`to`) |

Errors use a unified JSON body: `{code, message, timestamp, path, details[], event?}` emitted by `GlobalExceptionHandler` (400 validation, 401 auth, 404 not found, 409 email-in-use / event overlap with the conflicting event attached).

📦 **The full, hand-written OpenAPI 3.0.3 specification lives in [`src/main/resources/api.yaml`](src/main/resources/api.yaml)** — it is the authoritative reference for every endpoint, request/response body, and status code.

## Prerequisites

- **JDK 21**
- **PostgreSQL** — a running instance with a database (e.g. `todo_list`)
- **Docker** — required to run the test suite (Testcontainers spins up a `postgres:16` container)
- A frontend listening at your `CORS_ALLOWED_ORIGIN` (default `http://localhost:3000`) if you want to exercise CORS

## Database setup

Hibernate runs with `ddl-auto: validate`, so the schema is **not** generated automatically. Apply the SQL migration scripts in `src/main/resources/db/` manually, in order:

```
src/main/resources/db/V1__table_init.sql
src/main/resources/db/V2__refresh_table.sql
src/main/resources/db/V3__refresh_token_value_unique.sql
```

## Configuration

Copy `.env.example` to `.env` and fill in the values:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/todo_list` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `CORS_ALLOWED_ORIGIN` | Allowed frontend origin (defaults to `http://localhost:3000`; credentials allowed, all methods) |
| `JWT_SECRET` | HMAC signing secret (use a 256-bit base64-encoded secret) |
| `JWT_EXPIRATION_ACCESS` | Access token TTL in ms (e.g. `3600000` = 1 hour) |
| `JWT_EXPIRATION_REFRESH` | Refresh token TTL in ms (e.g. `604800000` = 7 days) |

> ⚠️ **Note:** Spring Boot does **not** auto-load `.env` files. Running the `TodoApplication` configuration from VS Code loads it automatically (see `.vscode/launch.json`); from a terminal, export the variables first (see below).

## Running

```bash
# terminal run (loads .env manually)
set -a && source .env && set +a && ./gradlew bootRun

# VS Code: just run the "TodoApplication" launch configuration
```

The server starts on **http://localhost:8080**.

Other useful tasks:

```bash
./gradlew build      # compile + tests
./gradlew assemble   # compile only
./gradlew bootJar    # build a runnable JAR at build/libs/todo-0.0.1-SNAPSHOT.jar
```

## Testing

```bash
./gradlew test
```

The test suite combines:

- **Unit tests** for services (`AuthServiceTest`, `TodoServiceTest`, `EventServiceTest`, `StatsServiceTest`) and validators.
- **Integration tests** (`AuthControllerIT`, `TodoControllerIT`, `EventControllerIT`, `StatsControllerIT`): MockMvc suites running against a real **Postgres 16 Testcontainer** with `ddl-auto=create-drop`, using short-lived access tokens (15 min) and a dedicated test JWT secret.

Docker must be running for the integration tests.
