# MENTORA

MENTORA is a compact modular-monolith learning management and personalized mentoring application. It combines student profile, academic planning, productivity tracking, career preparation, secure authentication, a read-only dashboard, backend-only Gemini recommendations, and persistent AI learning roadmaps in one maintainable academic project.

See [OOP_ARCHITECTURE.md](OOP_ARCHITECTURE.md) for the OOP concept map and [FINAL_REPORT.md](FINAL_REPORT.md) for the final audit and demonstration guide.

## Features

- JWT registration and login with BCrypt password hashing
- Authenticated student profile management
- Academic records and study-plan progress
- Tasks, habits, and calendar events
- Career profile, interests, and skills
- Explicit Gemini-powered recommendation generation and lifecycle management
- Explicit Gemini-powered learning-roadmap generation with persisted step progress
- One-request, read-only student dashboard
- Consistent validation, ownership checks, and API error responses

## Technology stack

- Frontend: React 19, Vite, React Router, Axios, Bootstrap 5, custom CSS
- Backend: Java 21, Spring Boot 3.5, Spring Web, Spring Security, Jakarta Validation, Spring Data JPA/Hibernate
- Build: Maven Wrapper and npm
- Database: MySQL; H2 only for automated tests
- AI: Gemini REST API, called only from the backend
- Tests: JUnit 5, Mockito, Spring Boot Test, MockMvc

## Architecture

The repository uses a compact modular monolith. Backend code is grouped by feature rather than technical layer, and each feature contains only the controller, service, entity, repository, and compact DTO container it needs. Spring Data repositories are the persistence abstraction; there are no DAO/DAOImpl, ServiceImpl, RepositoryImpl, mapper-per-entity, or generic utility layers.

```text
Mentora/
|-- backend/
|   `-- src/main/java/com/mentora/
|       |-- auth/
|       |-- student/
|       |-- academic/
|       |-- productivity/
|       |-- career/
|       |-- recommendation/
|       |-- roadmap/
|       |-- dashboard/
|       `-- common/
|-- frontend/
|   `-- src/
|       |-- components/
|       |-- pages/
|       |-- api.js
|       |-- auth.jsx
|       |-- App.jsx
|       |-- main.jsx
|       `-- styles.css
|-- OOP_ARCHITECTURE.md
`-- FINAL_REPORT.md
```

## Prerequisites

- JDK 21 or newer, with `JAVA_HOME` configured
- Node.js 20.19+ or 22.12+ and npm
- MySQL 8+
- Internet access on the first Maven/npm install and for live Gemini generation

Maven does not need to be installed globally; the repository includes Maven Wrapper 3.3.4 pinned to Maven 3.9.16.

## Environment setup

Both Spring Boot and Vite read the single repository-root `.env` file. The real file is ignored by Git; [`.env.example`](.env.example) is the safe trackable template.

Create the local file once if it is absent:

```powershell
cd D:\clg_pbl\oots
Copy-Item .env.example .env
```

Edit `.env` and replace the placeholders for `DB_PASSWORD`, `JWT_SECRET`, and `GEMINI_API_KEY`, together with any other machine-specific values. The JWT secret must be a Base64-encoded key containing at least 32 random bytes. Do not commit or share the completed `.env` file.

Operating-system environment variables retain higher precedence, so deployment environments can override local `.env` values without changing files.

Required backend variables:

| Variable | Secret | Purpose |
|---|---:|---|
| `DB_URL` | No | MySQL JDBC URL |
| `DB_USERNAME` | No | MySQL account |
| `DB_PASSWORD` | Yes | MySQL password |
| `JWT_SECRET` | Yes | Base64 key containing at least 32 random bytes |

Optional backend variables:

| Variable | Default | Secret | Purpose |
|---|---|---:|---|
| `DB_DDL_AUTO` | `validate` | No | Hibernate schema policy |
| `JWT_EXPIRATION_MS` | `3600000` | No | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | No | Comma-separated trusted origins |
| `SERVER_PORT` | `8080` | No | Backend port |
| `GEMINI_API_KEY` | empty | Yes | Required only for live generation |
| `GEMINI_MODEL` | `gemini-2.5-flash` | No | Centralized model selection |
| `GEMINI_TIMEOUT_SECONDS` | `20` | No | Provider connection/request timeout |

Optional frontend variable:

| Variable | Default | Secret | Purpose |
|---|---|---:|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | No | Spring API base URL |

Only `VITE_API_BASE_URL` is exposed to browser code. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `GEMINI_API_KEY` remain backend-only even though both tools read the same root file.

## MySQL setup

Create an empty database and a least-privilege local account using your own credentials:

```sql
CREATE DATABASE mentora CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

The safe default is `DB_DDL_AUTO=validate`. Because this academic repository does not include Flyway/Liquibase migrations, a new local database needs one controlled initial run with `DB_DDL_AUTO=update` to create the schema. Stop the backend afterward, change `.env` back to `DB_DDL_AUTO=validate`, and restart. Do not use `create` or `create-drop` against retained data.

## Run the backend

From the project root:

```powershell
cd D:\clg_pbl\oots\backend
.\mvnw.cmd spring-boot:run
```

The backend starts on `http://localhost:8080` unless `SERVER_PORT` is changed. Startup requires the four required backend variables and a reachable, schema-compatible MySQL database.

## Run the frontend

In a second terminal:

```powershell
cd D:\clg_pbl\oots\frontend
npm install
npm run dev
```

Vite prints the local browser URL, normally `http://localhost:5173`.

## Testing and builds

Backend tests use isolated H2 configuration and a test provider, so they need neither MySQL nor Gemini:

```powershell
cd D:\clg_pbl\oots\backend
.\mvnw.cmd clean test
.\mvnw.cmd package
```

Frontend production build:

```powershell
cd D:\clg_pbl\oots\frontend
npm install
npm run build
```

## API inventory

Only registration and login are public. All other routes require `Authorization: Bearer <token>`.

| Module | Method and route |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Student | `GET /api/student/me`, `PUT /api/student/me` |
| Academic records | `GET /api/academic/records`, `POST /api/academic/records`, `PUT /api/academic/records/{id}`, `DELETE /api/academic/records/{id}` |
| Study plans | `GET /api/academic/study-plans`, `POST /api/academic/study-plans`, `PUT /api/academic/study-plans/{id}`, `PATCH /api/academic/study-plans/{id}/progress`, `DELETE /api/academic/study-plans/{id}` |
| Tasks | `GET /api/productivity/tasks`, `POST /api/productivity/tasks`, `PUT /api/productivity/tasks/{id}`, `PATCH /api/productivity/tasks/{id}/complete`, `PATCH /api/productivity/tasks/{id}/reschedule`, `DELETE /api/productivity/tasks/{id}` |
| Habits | `GET /api/productivity/habits`, `POST /api/productivity/habits`, `PUT /api/productivity/habits/{id}`, `PATCH /api/productivity/habits/{id}/complete`, `DELETE /api/productivity/habits/{id}` |
| Calendar | `GET /api/productivity/calendar`, `POST /api/productivity/calendar`, `PUT /api/productivity/calendar/{id}`, `PATCH /api/productivity/calendar/{id}/reschedule`, `DELETE /api/productivity/calendar/{id}` |
| Career | `GET /api/career`, `PUT /api/career`, `GET /api/career/skills`, `POST /api/career/skills`, `PUT /api/career/skills/{id}`, `DELETE /api/career/skills/{id}` |
| Recommendations | `POST /api/recommendations/generate`, `GET /api/recommendations`, `PATCH /api/recommendations/{id}/complete`, `PATCH /api/recommendations/{id}/dismiss` |
| Roadmaps | `POST /api/roadmaps/generate`, `GET /api/roadmaps`, `GET /api/roadmaps/{id}`, `PATCH /api/roadmaps/{roadmapId}/steps/{stepId}/status`, `DELETE /api/roadmaps/{id}` |
| Dashboard | `GET /api/dashboard` |

Student-owned resource IDs are always resolved together with the authenticated student; no protected endpoint accepts a student ID from the browser.

## Gemini setup and architecture

Gemini is optional for ordinary startup and automated tests. For live generation, set `GEMINI_API_KEY` in the backend process, optionally set `GEMINI_MODEL`, start MySQL and the backend, authenticate, then explicitly call `POST /api/recommendations/generate` or `POST /api/roadmaps/generate`.

```text
React -> Spring controller -> RecommendationService
      -> RecommendationProvider -> GeminiClient -> Gemini HTTPS API

React -> Spring controller -> RoadmapService
      -> RoadmapProvider -> GeminiClient -> Gemini HTTPS API
```

The API key never reaches the browser. Provider output is treated as untrusted and is validated before persistence. Roadmap generation occurs only when the student presses **Generate Roadmap**; opening `/roadmaps` reads saved `roadmaps` and `roadmap_steps` data from MySQL. Step statuses are stored in MySQL and overall progress is calculated in Java from completed steps. The dashboard reads this stored progress and never invokes Gemini.

## Security model

- Signed, expiring JWT bearer tokens; BCrypt password hashes
- Stateless Spring Security with explicit public routes
- Authenticated subject converted to the current student on the server
- Student-scoped repository queries for reads and mutations
- DTO-only API responses; password hashes and internal normalized fields are not exposed
- Jakarta validation plus entity/service invariants and centralized error handling
- Explicit CORS allowlist; wildcard origins rejected
- Secrets supplied only through backend environment variables

The SPA currently stores the access token in `localStorage`. This is simple for an academic SPA but remains exposed to successful XSS. A production deployment should evaluate secure HttpOnly cookies, CSRF protection, token refresh/revocation, rate limiting, HTTPS, and managed secret storage.

## OOP design

The project demonstrates classes/objects, encapsulation, associations, composition, interfaces, abstraction, runtime polymorphism, overriding, collections, enums, exception handling, and constructor-based dependency injection. Custom domain inheritance and business-method overloading are intentionally not required because no honest runtime use case exists. Details and viva examples are in [OOP_ARCHITECTURE.md](OOP_ARCHITECTURE.md).

## Known limitations

- No versioned production database migrations; initial local schema creation is controlled through Hibernate.
- No JWT refresh/revocation or multi-device session administration.
- Browser token storage has the XSS tradeoff described above.
- No production observability, deployment automation, rate limiting, email verification, password reset, or accessibility audit.
- Live MySQL and Gemini behavior depends on user-supplied infrastructure and credentials.

## Final status

The implementation is feature-complete for its defined academic scope. Automated backend and frontend validation are documented in [FINAL_REPORT.md](FINAL_REPORT.md). Live MySQL and Gemini checks remain environment-dependent and must not be reported as passed without real credentials and services.
