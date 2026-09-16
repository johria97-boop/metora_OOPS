# Mentora Final Engineering Report

## 1. Final verdict

**COMPLETE for the defined academic scope.** Phase 10 is **PASS**. The clean backend suite reports 137 tests, 137 passed, 0 failed, 0 errors, and 0 skipped. The executable backend package and frontend production bundle both build successfully. Real MySQL 8.0.46 schema validation, one controlled Gemini roadmap generation, stored progress reload, and dashboard integration all passed.

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
  +--> Roadmap
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

RoadmapService --> RoadmapProvider --> GeminiClient --> Gemini API
```

## 3. Implemented modules

- Authentication: register, login, JWT validation, BCrypt, frontend logout, protected routes
- Student: authenticated profile read/update
- Academic: records, study plans, bounded progress
- Productivity: tasks, habits, calendar events
- Career: singleton profile, interests, skills
- Recommendation: explicit Gemini generation, validation, deduplication, lifecycle, history
- Roadmap: explicit Gemini generation, aggregate persistence, step status, and calculated progress
- Dashboard: one-request read-only aggregation of existing student data
- Common: behavioral interfaces, security configuration, API exceptions and error handling

## 4. Database design summary

The 13 necessary tables are `users`, `students`, `academic_records`, `study_plans`, `tasks`, `habits`, `calendar_events`, `career_profiles`, `career_profile_interests`, `skills`, `recommendations`, `roadmaps`, and `roadmap_steps`. Roadmaps belong to students; ordered steps belong to a roadmap and are cascade/orphan-managed. `(roadmap_id, position)` is unique. Step status is persisted while progress is derived. There is no dashboard table.

Production uses `DB_DDL_AUTO=validate` by default. A one-time local `update` run is documented for an empty development schema; no destructive schema mode is configured.

## 5. OOP concept summary

The exact evidence map is maintained in [OOP_ARCHITECTURE.md](OOP_ARCHITECTURE.md). Strong examples are:

- Encapsulation: private entity fields and invariant-protecting update/lifecycle methods
- Association/composition: student-owned entities and `CareerProfile`-owned interests
- Interfaces/abstraction: `Trackable`, `Schedulable`, `RecommendationProvider`, `RoadmapProvider`, and Spring Data contracts
- Runtime polymorphism/overriding: `StudyPlan`, `Habit`, and `Roadmap` implement `getProgress()`; provider implementations are substitutable
- Composition: `Roadmap` owns its ordered `RoadmapStep` collection
- Constructors: protected JPA constructors plus public valid-creation constructors
- Collections/enums: meaningful `List`/`Set` use and typed closed domain states
- Exception handling: `ApiException` and `GlobalExceptionHandler`
- Dependency injection: constructor injection across production components
- Method overloading: used naturally by `GeminiClient` for its two provider request types
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

React never calls Gemini. Recommendation and roadmap services depend on separate provider contracts; one `GeminiClient` implements both and owns the shared HTTP/key/model/timeout/error boundary. Roadmap output is schema-constrained, parsed as untrusted input, fully validated, deterministically ordered, then persisted. Only `POST /api/roadmaps/generate` calls the provider; roadmap reads, progress updates, and dashboard loads are MySQL-only. Offline tests use stubs/local HTTP and consume no quota.

## 8. Dashboard summary

`GET /api/dashboard` is authenticated and read-only. `DashboardService` resolves the current student, reads existing repositories, and returns stored roadmap totals plus the current roadmap's calculated progress alongside existing summaries. It creates no entity/table and never invokes Gemini.

## 9. Test results

| Metric | Result |
|---|---:|
| Previous baseline | 98 |
| New Phase 10 tests | 39 |
| Total | 137 |
| Passed | 137 |
| Failed | 0 |
| Errors | 0 |
| Skipped | 0 |

Command: `backend\mvnw.cmd clean test`. Eleven Surefire XML reports independently total the same 137 tests.

## 10. Build results

- Backend: **PASS** via `backend\mvnw.cmd package`; executable JAR: `backend\target\mentora-backend-0.0.1-SNAPSHOT.jar`
- Frontend install: **PASS**; 97 packages audited, 0 vulnerabilities reported
- Frontend: **PASS** via `npm run build`; Vite transformed 108 modules and emitted `frontend\dist`
- Development server: **PASS**; Vite became ready on `127.0.0.1:5173`
- Routes: `/auth`, `/dashboard`, `/profile`, `/academics`, `/productivity`, `/career`, `/recommendations`, and `/roadmaps` each returned the SPA shell with HTTP 200

Generated `target`, `dist`, and `node_modules` directories remain ignored and are not part of the source deliverable.

## 11. Live MySQL status

**PASS.** A controlled `DB_DDL_AUTO=update` startup against MySQL 8.0.46 added exactly `roadmaps` and `roadmap_steps`, their two foreign keys, and unique `(roadmap_id, position)`. No existing data was deleted. Configuration was restored to `validate`, and a clean validate-mode restart passed. Direct SQL confirmed the live validation roadmap and its 12 persisted steps.

## 12. Live Gemini status

**PASS.** Exactly one controlled live generation used configured model `gemini-3.5-flash-lite` and returned HTTP 201 with 12 validated steps. After one step update, API reload, direct MySQL inspection, and dashboard all showed one completed step and 8.3333% progress. Backend logs contained one roadmap-generation request; subsequent reads and dashboard access triggered none. The key was never printed.

## 13. File and duplication audit

The repository contains 58 production Java files, 11 Java test files, 14 frontend source files, and these 3 Markdown documents. The roadmap module uses exactly seven production files and no step repository, implementation class, mapper, helper, utility, or split progress/generation service. The audit found one `Trackable`, one Axios client, one `AppNav`, one Gemini HTTP boundary, and no frontend Gemini/key reference. Generated directories are present for validation but correctly ignored.

Phase 10's largest production files are `Roadmaps.jsx` (225 lines), `RoadmapService.java` (210), `Roadmap.java` (186), and the extended `GeminiClient.java` (346). They remain cohesive; splitting would add the exact layers the low-file-count requirement rejects.

Direct dependencies correspond to used features. No version was deliberately upgraded and no dependency was removed without evidence. The source remains compact; cohesive large files were not split solely to reduce line count.

## 14. Known limitations

- MySQL and Gemini remain externally configured services and can become unavailable or rate-limited.
- There are no versioned database migrations; initial development schema creation relies on one controlled Hibernate `update` run.
- JWTs have no refresh/revocation architecture, and frontend token storage has an XSS tradeoff.
- Production deployment automation, HTTPS termination, managed secrets, observability, rate limiting, email verification, password reset, and a formal accessibility audit are outside the current scope.

## 15. Future scope

**FUTURE SCOPE — not current functionality:** AI mock interview, voice AI mentor, placement prediction, parent dashboard, online mentor network, ERP integration, and richer AI recommendations. Chatbots, RAG, embeddings, vector databases, schedulers, notifications, admin features, OAuth/MFA, Redis/Kafka, microservices, and new analytics were also not added.

## 16. PBL readiness and demonstration checklist

**READY**. The configured local MySQL and Gemini services were live-validated for Phase 10.

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
11. Generate a roadmap explicitly, update one step, reload it, and show stored progress on the dashboard.
12. Explain the architecture, student ownership, DTO/error boundary, and OOP examples.

If Gemini is unavailable, show existing stored recommendations if available, explain the provider boundary, and present the passing stub/local-server tests. Do not claim that a live call occurred.

Final student workflow:

```text
Register -> Login -> Dashboard
                    |-> Profile
                    |-> Academics -> Study Plans
                    |-> Tasks -> Habits -> Calendar
                    |-> Career -> Skills
                    |-> Generate Recommendations -> Lifecycle
                    `-> Generate Roadmap -> Step Progress -> Updated Dashboard
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
- **Where is polymorphism?** `Trackable`, `Schedulable`, `RecommendationProvider`, and `RoadmapProvider` references dispatch to runtime implementations.
- **Where are interfaces?** `Trackable`, `Schedulable`, both provider contracts, and the Spring Data repositories.
- **Where is overriding?** Study plans, habits, and roadmaps override progress; tasks/events override scheduling; GeminiClient implements both generation contracts.
- **Where is overloading?** `GeminiClient.generate(StudentContext)` and `generate(RoadmapRequest)` are genuine provider-driven overloads.
- **Where are constructors?** Entities use protected JPA no-arg and public invariant-validating constructors; components use constructor injection.
- **Where are collections?** Ordered resource `List`s, unique interest `Set`, AI context groups, previews, and polymorphism tests.
- **Where are enums?** User role, task/recommendation priority, skill level, recommendation category/status.
- **Where is exception handling?** `ApiException`, `GlobalExceptionHandler`, and Gemini error translation.
- **Why is inheritance deferred?** No current domain types share a valid substitutable IS-A relationship requiring a base class.
- **Why composition over inheritance?** Student resources and profile interests are owned HAS-A relationships, not specialized parent types.
- **How is ownership secured?** JWT subject lookup plus repository methods scoped by both resource ID and current student.
- **Why does Dashboard not call Gemini?** A read endpoint must remain predictable, fast, quota-free, and side-effect-free; it shows stored results only.
- **How is roadmap progress persisted?** Step status is stored in MySQL; `Roadmap.getProgress()` derives a bounded percentage from completed steps.
- **How are duplicate AI recommendations prevented?** Titles are normalized, a generated batch uses a set, and active persisted category/title matches are checked before saving.

The project is ready for its report, presentation, PBL demonstration, and OOTS viva, with live external checks explicitly dependent on local credentials.
