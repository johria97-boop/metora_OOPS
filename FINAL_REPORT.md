# Mentora Final Engineering Report

## 1. Final verdict

**COMPLETE for the defined academic scope.** Phase 9 is **PASS**. The clean backend suite reports 98 tests, 98 passed, 0 failed, 0 errors, and 0 skipped. The executable backend package and frontend production bundle both build successfully. Live MySQL and Gemini checks are **BLOCKED**, not failed, because their environment credentials are absent.

## 2. Architecture

Mentora is a compact feature-based modular monolith. React communicates only with the Spring Boot REST API. Spring services enforce use cases and student ownership; Spring Data JPA/Hibernate persists the domain in MySQL. Recommendation generation is isolated behind a provider interface.

```text
Student
  |
  v
React + Vite
  |
  v
Spring Boot REST API
  |
  +--> Auth
  +--> Student
  +--> Academic
  +--> Productivity
  +--> Career
  +--> Recommendation
  +--> Dashboard
  |
  v
Spring Data JPA / Hibernate
  |
  v
MySQL

RecommendationService
  |
  v
RecommendationProvider
  |
  v
GeminiClient
  |
  v
Gemini API
```

## 3. Implemented modules

- Authentication: register, login, JWT validation, BCrypt, frontend logout, protected routes
- Student: authenticated profile read/update
- Academic: records, study plans, bounded progress
- Productivity: tasks, habits, calendar events
- Career: singleton profile, interests, skills
- Recommendation: explicit Gemini generation, validation, deduplication, lifecycle, history
- Dashboard: one-request read-only aggregation of existing student data
- Common: behavioral interfaces, security configuration, API exceptions and error handling

## 4. Database design summary

The 11 necessary tables are `users`, `students`, `academic_records`, `study_plans`, `tasks`, `habits`, `calendar_events`, `career_profiles`, `career_profile_interests`, `skills`, and `recommendations`. `students.user_id` and `career_profiles.student_id` are unique one-to-one foreign keys. Other feature records have mandatory student foreign keys. Skills have a unique `(student_id, normalized_name)` constraint. Interests are composed under their career profile. Recommendation terminal states remain stored as history. There is no dashboard table.

Production uses `DB_DDL_AUTO=validate` by default. A one-time local `update` run is documented for an empty development schema; no destructive schema mode is configured.

## 5. OOP concept summary

The exact evidence map is maintained in [OOP_ARCHITECTURE.md](OOP_ARCHITECTURE.md). Strong examples are:

- Encapsulation: private entity fields and invariant-protecting update/lifecycle methods
- Association/composition: student-owned entities and `CareerProfile`-owned interests
- Interfaces/abstraction: `Trackable`, `Schedulable`, `RecommendationProvider`, and Spring Data contracts
- Runtime polymorphism/overriding: `StudyPlan` and `Habit` implement `getProgress()`; `Task` and `CalendarEvent` implement scheduling operations; provider implementations are substitutable
- Constructors: protected JPA constructors plus public valid-creation constructors
- Collections/enums: meaningful `List`/`Set` use and typed closed domain states
- Exception handling: `ApiException` and `GlobalExceptionHandler`
- Dependency injection: constructor injection across production components
- Method overloading: **NOT REQUIRED**
- Custom domain inheritance: **INTENTIONALLY DEFERRED** because no truthful substitutable IS-A behavior exists

## 6. Security summary

- Exactly `POST /api/auth/register` and `POST /api/auth/login` are public.
- JWT signatures/expiry and BCrypt password verification are enforced.
- Protected operations derive the student from the validated JWT subject.
- Owned resource queries combine resource ID and student, preventing cross-student access.
- DTOs prevent JPA entities, password hashes, and internal normalized values from leaving controllers.
- Jakarta validation, domain invariants, unreadable-input handling, and one safe error shape are centralized.
- CORS uses explicit origins; wildcard origins are rejected. CSRF is disabled intentionally for stateless bearer headers, not cookie authentication.
- Axios attaches the token and centrally handles `401`; route guards and navigation are centralized.
- Gemini credentials remain backend-only and secrets are neither logged nor committed.
- The dashboard remains current-student-only.

The known browser-side tradeoff is JWT storage in `localStorage`, which is exposed if an XSS flaw succeeds.

## 7. Gemini integration summary

React never calls Gemini. `RecommendationService` builds minimal student context and depends on `RecommendationProvider`; `GeminiClient` alone knows Gemini HTTP details. Output is treated as untrusted: structured content is parsed, limited to five entries, enum/length validated, normalized, deduplicated against active records, then persisted. Controlled errors cover missing keys, rate limits, timeouts, provider failures, transport failures, and malformed responses. Offline tests use a stub/local server and consume no quota.

## 8. Dashboard summary

`GET /api/dashboard` is authenticated and read-only. `Dashboard.jsx` makes one request to it. `DashboardService` resolves the current student, reads existing repositories, reuses domain progress calculations, and returns DTO summaries and a three-item active recommendation preview. It creates no entity/table and never invokes Gemini.

## 9. Test results

| Metric | Result |
|---|---:|
| Previous baseline | 98 |
| New Phase 9 tests | 0 |
| Total | 98 |
| Passed | 98 |
| Failed | 0 |
| Errors | 0 |
| Skipped | 0 |

Command: `backend\mvnw.cmd clean test`. Nine Surefire XML reports independently total the same 98 tests.

## 10. Build results

- Backend: **PASS** via `backend\mvnw.cmd package`; executable JAR: `backend\target\mentora-backend-0.0.1-SNAPSHOT.jar`
- Frontend install: **PASS**; 97 packages audited, 0 vulnerabilities reported
- Frontend: **PASS** via `npm run build`; Vite transformed 107 modules and emitted `frontend\dist`
- Development server: **PASS**; Vite became ready on `127.0.0.1:5173`
- Routes: `/auth`, `/dashboard`, `/profile`, `/academics`, `/productivity`, `/career`, and `/recommendations` each returned the SPA shell with HTTP 200

Generated `target`, `dist`, and `node_modules` directories remain ignored and are not part of the source deliverable.

## 11. Live MySQL status

**BLOCKED.** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` were not present. No credentials were invented and no existing data was touched. H2 integration/security tests provide source validation, but do not replace a deployment-environment MySQL smoke test.

## 12. Live Gemini status

**BLOCKED.** `GEMINI_API_KEY` was not present. No quota was consumed. Provider service tests and local HTTP-client tests remain authoritative for offline behavior, but do not prove the external service/account state.

## 13. File and duplication audit

The repository contains 51 production Java files, 9 Java test files, 13 frontend source files, and these 3 Markdown documents. Excluding `node_modules`, `target`, `dist`, `.git`, and ignored editor files, the measured project is 527,296 bytes (about 514.9 KiB): backend source/config/wrapper 339,224 bytes, frontend source/config/lockfile 148,598 bytes, and root documentation/config 39,474 bytes. Exact-content hashing found no duplicate source files. No abandoned package, implementation layer, DAO, mapper explosion, generic utility dump, second Axios instance, second route guard, unused custom repository method, TODO/FIXME/HACK placeholder, debug print, or proven dead class was found. Generated directories are present for validation but correctly ignored.

The largest code files are cohesive integration suites: `PhaseTwoIntegrationTests.java` (438 lines), `PhaseSixIntegrationTests.java` (424), and `PhaseThreeIntegrationTests.java` (423). The largest production UI file is `Productivity.jsx` (385), and the largest production Java file is `RecommendationService.java` (226). None warranted a final-phase split.

Direct dependencies correspond to used features. No version was deliberately upgraded and no dependency was removed without evidence. The source remains compact; cohesive large files were not split solely to reduce line count.

## 14. Known limitations

- MySQL and Gemini need real environment configuration for live validation.
- There are no versioned database migrations; initial development schema creation relies on one controlled Hibernate `update` run.
- JWTs have no refresh/revocation architecture, and frontend token storage has an XSS tradeoff.
- Production deployment automation, HTTPS termination, managed secrets, observability, rate limiting, email verification, password reset, and a formal accessibility audit are outside the current scope.

## 15. Future scope

**FUTURE SCOPE — not current functionality:** AI mock interview, voice AI mentor, placement prediction, parent dashboard, online mentor network, ERP integration, and richer AI recommendations. Chatbots, RAG, embeddings, vector databases, schedulers, notifications, admin features, OAuth/MFA, Redis/Kafka, microservices, and new analytics were also not added.

## 16. PBL readiness and demonstration checklist

**READY**, subject to supplying local MySQL credentials and optionally a Gemini key for live external-service steps.

1. Configure environment variables and start MySQL.
2. Start the backend with `backend\mvnw.cmd spring-boot:run`.
3. Start the frontend with `npm run dev`.
4. Register a student and demonstrate login/logout and protected routing.
5. Complete the profile.
6. Add an academic score and a study plan; update progress.
7. Add a task, habit, and calendar event; demonstrate domain actions.
8. Save a career goal, interests, and skills.
9. Open the dashboard and show the unified stored-data overview.
10. Generate recommendations once, show history and complete/dismiss lifecycle, then return to the dashboard.
11. Explain the architecture, student ownership, DTO/error boundary, and OOP examples.

If Gemini is unavailable, show existing stored recommendations if available, explain the provider boundary, and present the passing stub/local-server tests. Do not claim that a live call occurred.

Final student workflow:

```text
Register -> Login -> Dashboard
                    |-> Profile
                    |-> Academics -> Study Plans
                    |-> Tasks -> Habits -> Calendar
                    |-> Career -> Skills
                    `-> Generate Recommendations -> Lifecycle -> Updated Dashboard
```

## 17. Viva readiness

- **Why React?** Component state and routing suit a compact interactive SPA.
- **Why Spring Boot?** It integrates REST, validation, security, dependency injection, persistence, and testing with little setup.
- **Why MySQL?** The domain is relational and needs constraints, foreign keys, and durable history.
- **Why Spring Data JPA?** Repository contracts remove boilerplate while Hibernate maps the domain relationships.
- **Why JWT?** It supports a simple stateless bearer-token API between the SPA and backend.
- **Why Gemini?** It turns existing student context into mentoring suggestions while business validation stays local.
- **Why `RecommendationProvider`?** It prevents Gemini HTTP details from coupling the service and enables deterministic offline substitution.
- **Where is encapsulation?** Private entity fields change through methods such as `updateProgress`, `recordCompletion`, `reschedule`, `markCompleted`, and `dismiss`.
- **Where is abstraction?** Behavioral interfaces, repositories, services, DTO records, and the provider boundary hide implementations.
- **Where is polymorphism?** `Trackable`, `Schedulable`, and `RecommendationProvider` references dispatch to runtime implementations.
- **Where are interfaces?** `Trackable`, `Schedulable`, `RecommendationProvider`, and the Spring Data repositories.
- **Where is overriding?** Study plans/habits override progress; tasks/events override scheduling; GeminiClient overrides generation.
- **Where is overloading?** No business method is overloaded; it is not required. Constructor variants are not method overloading.
- **Where are constructors?** Entities use protected JPA no-arg and public invariant-validating constructors; components use constructor injection.
- **Where are collections?** Ordered resource `List`s, unique interest `Set`, AI context groups, previews, and polymorphism tests.
- **Where are enums?** User role, task/recommendation priority, skill level, recommendation category/status.
- **Where is exception handling?** `ApiException`, `GlobalExceptionHandler`, and Gemini error translation.
- **Why is inheritance deferred?** No current domain types share a valid substitutable IS-A relationship requiring a base class.
- **Why composition over inheritance?** Student resources and profile interests are owned HAS-A relationships, not specialized parent types.
- **How is ownership secured?** JWT subject lookup plus repository methods scoped by both resource ID and current student.
- **Why does Dashboard not call Gemini?** A read endpoint must remain predictable, fast, quota-free, and side-effect-free; it shows stored results only.
- **How are duplicate AI recommendations prevented?** Titles are normalized, a generated batch uses a set, and active persisted category/title matches are checked before saving.

The project is ready for its report, presentation, PBL demonstration, and OOTS viva, with live external checks explicitly dependent on local credentials.
