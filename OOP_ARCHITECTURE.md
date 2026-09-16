# Mentora OOP Architecture

## 1. Overview

Mentora is a compact modular monolith whose object-oriented design comes from real learning-management behavior. Feature packages contain domain entities, Spring Data repositories, one cohesive service, compact DTOs, and a controller. Shared contracts exist only where multiple domain types genuinely perform the same operation.

The design favors:

- encapsulated domain state;
- student-owned associations;
- composition for owned value collections;
- small behavioral interfaces for actual polymorphism;
- constructor injection;
- centralized API exceptions; and
- DTO boundaries instead of exposing persistence entities.

No inheritance hierarchy is present merely for demonstration.

### Final concept status

| OOP concept | Status | Evidence or decision |
|---|---|---|
| Classes and objects | IMPLEMENTED | Entities, services, controllers, provider, security, and error classes model real responsibilities; their objects are created, validated, persisted, mapped, and tested. |
| Encapsulation | IMPLEMENTED | Private entity state changes through invariant-protecting domain methods. |
| Association | IMPLEMENTED | `Student` is associated with user-owned academic, productivity, career, and recommendation data. |
| Composition | IMPLEMENTED | `CareerProfile` owns its `Set<String>` interests through `@ElementCollection`. |
| Interfaces | IMPLEMENTED | `Trackable`, `Schedulable`, and `RecommendationProvider` express real contracts. |
| Abstraction | IMPLEMENTED | Behavioral, repository, DTO, service, and external-provider boundaries hide implementation detail. |
| Runtime polymorphism | IMPLEMENTED | Multiple `Trackable`/`Schedulable` implementations and provider substitution dispatch through interfaces. |
| Method overriding | IMPLEMENTED | Domain classes implement interface operations with type-specific behavior. |
| Method overloading | NOT REQUIRED | No business use case needs one method name with multiple parameter signatures. |
| Constructors | IMPLEMENTED | Protected no-argument JPA constructors coexist with public invariant-validating creation constructors. |
| Inheritance | DEFERRED | No honest custom domain IS-A hierarchy currently has substitutable runtime behavior. |
| Collections | IMPLEMENTED | `List` and `Set` model ordered resources, unique interests, context, and response groups. |
| Enums | IMPLEMENTED | Roles, priorities, levels, categories, and statuses are closed typed value sets. |
| Exception handling | IMPLEMENTED | `ApiException` and `GlobalExceptionHandler` provide controlled, consistent failures. |
| Dependency injection | IMPLEMENTED | Spring collaborators use constructor injection and services depend on contracts where justified. |

## 2. Classes and objects

The principal domain classes are:

- `User`: authentication identity, password hash, and role;
- `Student`: the authenticated learner's profile;
- `AcademicRecord`: subject score for a semester;
- `StudyPlan`: target and completed study hours;
- `Task`: scheduled work with priority and completion state;
- `Habit`: streak state and progress;
- `CalendarEvent`: a dated student event;
- `CareerProfile`: career goal and owned interests;
- `Skill`: display name, normalized identity, and proficiency level; and
- `Recommendation`: validated mentoring guidance with category, priority, lifecycle status, and normalized duplicate identity.

`AuthService`, `StudentService`, `AcademicService`, `ProductivityService`, `CareerService`, `RecommendationService`, and `DashboardService` coordinate application use cases. `DashboardService` is an application-level aggregator rather than a persistent domain entity. Controllers translate authenticated HTTP requests into those service operations.

## 3. Encapsulation

Entity fields are private and there are no unrestricted public setters. State with rules changes through domain methods:

| Class | Method | Protected invariant |
|---|---|---|
| `Student` | `updateProfile(...)` | Required, trimmed name/branch and semester 1–12 |
| `AcademicRecord` | `update(...)` | Required subject, score 0–100, semester 1–12 |
| `StudyPlan` | `update(...)`, `updateProgress(...)` | Positive target and completed hours within target |
| `Task` | `update(...)`, `markCompleted()`, `reschedule(...)` | Valid title/priority, controlled completion, non-past date |
| `Habit` | `update(...)`, `recordCompletion(...)` | Positive target and deterministic non-negative streak |
| `CalendarEvent` | `updateTitle(...)`, `reschedule(...)` | Required title and non-past date |
| `CareerProfile` | `updateCareerGoal(...)`, `replaceInterests(...)` | Bounded goal and normalized interest collection |
| `Skill` | `rename(...)`, `updateLevel(...)` | Normalized name and required enum level |
| `Recommendation` | constructor, `markCompleted()`, `dismiss()` | Valid bounded content, initially active state, and only active-to-terminal transitions |

`CareerProfile.getInterests()` returns an unmodifiable copy, so callers cannot bypass `replaceInterests(...)`.

## 4. Association and composition

The persistence model uses HAS-A relationships rather than artificial IS-A relationships:

- `User` and `Student` have a one-to-one association through `students.user_id`.
- `AcademicRecord`, `StudyPlan`, `Task`, `Habit`, `CalendarEvent`, `Skill`, and `Recommendation` each have a mandatory many-to-one association to `Student`.
- `CareerProfile` has a mandatory, unique one-to-one association to `Student`.
- `CareerProfile` composes its interest strings with `@ElementCollection`. Interests have no independent identity or lifecycle outside that profile.

The student-resource relationships are associations rather than Java inheritance. Their foreign keys express ownership while services enforce authenticated access.

## 5. Interfaces

Mentora defines two shared domain behavior interfaces:

```java
public interface Trackable {
    double getProgress();
}
```

Implemented by `StudyPlan` and `Habit`.

```java
public interface Schedulable {
    LocalDate getScheduledDate();
    void reschedule(LocalDate newDate);
}
```

Implemented by `Task` and `CalendarEvent`.

Both interfaces have multiple runtime implementations and describe behavior needed by the current domain.

Phase 6 also adds one external-provider boundary:

```java
public interface RecommendationProvider {
    List<GeneratedRecommendation> generate(StudentContext context);
}
```

`GeminiClient` is the production implementation. `RecommendationService` depends on this interface, while tests substitute a deterministic provider without changing service code. Its single production implementation is justified because it isolates an external system and enables offline tests, rather than anticipating hypothetical domain types.

## 6. Abstraction

`Trackable` abstracts how progress is obtained without exposing whether progress comes from study hours or a habit streak. `Schedulable` abstracts dated rescheduling without requiring callers to know whether the runtime object is a task or calendar event.

Spring Data's `JpaRepository` interfaces abstract persistence. Services use repository contracts rather than DAOs or repository implementations. DTO records abstract the HTTP representation from JPA entities. `RecommendationProvider` abstracts recommendation generation so the application layer has no Gemini HTTP, authentication-header, model, or JSON-response dependency.

`DashboardService` provides a read-only abstraction over several student-owned data sources. It reuses domain calculations and produces a composed `DashboardResponse` without exposing persistence entities or creating a second source of truth.

## 7. Runtime polymorphism

`PhaseThreeDomainTests` proves runtime polymorphism with heterogeneous collections:

- a `List<Trackable>` contains a `StudyPlan` and a `Habit`; invoking `Trackable.getProgress()` dispatches to each runtime implementation;
- a `List<Schedulable>` contains a `Task` and a `CalendarEvent`; invoking `reschedule(...)` and `getScheduledDate()` dispatches to each runtime implementation.

Phase 6 uses interface polymorphism at the application boundary: Spring injects `GeminiClient` as a `RecommendationProvider` in production, while `PhaseSixIntegrationTests` injects a stub implementation. The same `RecommendationService` invokes `generate(...)` against either runtime object.

This is behavioral polymorphism used through shared contracts, not merely matching method names.

## 8. Method overriding

The following interface operations are overridden:

- `StudyPlan.getProgress()` calculates completed study hours as a percentage of target hours.
- `Habit.getProgress()` calculates current streak as a percentage of target streak and caps it at 100%.
- `Task.getScheduledDate()` and `Task.reschedule(...)` operate on `dueDate`.
- `CalendarEvent.getScheduledDate()` and `CalendarEvent.reschedule(...)` operate on `eventDate`.
- `GeminiClient.generate(...)` implements the `RecommendationProvider` contract by performing a structured provider request and response conversion.

The same interface operation therefore has domain-specific runtime behavior.

## 9. Method overloading

Method overloading is **NOT REQUIRED**. No business use case needs one method name with multiple parameter signatures, so artificial overloads were intentionally avoided.

JPA entities do have a protected no-argument constructor for persistence and a public parameterized constructor for valid domain creation. These are constructor variants, not method overloading, and are documented separately to keep the OOP explanation technically precise.

## 10. Inheritance decision

Custom domain inheritance is intentionally deferred.

`Task`, `Habit`, and `CalendarEvent` do not share an accurate IS-A parent. Their common behavior is already expressed more precisely by `Trackable` and `Schedulable`. Creating `Activity` would add shared structure without shared domain semantics.

The proposed `Mentor -> AcademicMentor / CareerMentor` hierarchy still has no runtime role. One Gemini provider produces all three recommendation categories, while category-specific validation uses enums rather than substitutable mentor objects. Adding the hierarchy in Phase 6 would therefore be dead demonstration code; inheritance remains deferred until real runtime behavior requires it.

Framework repository interfaces extend `JpaRepository`, but that framework mechanism is not presented as a custom Mentora domain hierarchy.

## 11. Collections

Meaningful collection use includes:

- `List<AcademicRecord>` and `List<StudyPlan>` returned for one student;
- `List<Task>`, `List<Habit>`, and `List<CalendarEvent>` returned for one student;
- `List<Skill>` returned for one student;
- `Set<String>` for a career profile's unique interests;
- `List` records inside `RecommendationProvider.StudentContext` for minimal academic, study-plan, task, habit, calendar, career, and skill data;
- generated and stored recommendation lists, with a `Set<String>` used to deduplicate one generated batch;
- dashboard summary and bounded preview lists composed from existing feature collections;
- `List<Trackable>` and `List<Schedulable>` in polymorphism tests; and
- stream mapping from entities to immutable DTO record lists.

Repositories order lists for predictable API presentation. The domain's mutable interest set is never returned directly.

## 12. Enums

Closed value sets use nested enums:

- `User.Role`: currently `STUDENT`;
- `Task.Priority`: `LOW`, `MEDIUM`, `HIGH`;
- `Skill.Level`: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`;
- `Recommendation.Category`: `ACADEMIC`, `PRODUCTIVITY`, `CAREER`;
- `Recommendation.Priority`: `URGENT`, `HIGH`, `MEDIUM`, `LOW`; and
- `Recommendation.Status`: `ACTIVE`, `COMPLETED`, `DISMISSED`.

Task priority, skill level, and recommendation enums remain nested because they are local to one entity. JPA persists them with `EnumType.STRING`, preserving readable and stable database values.

## 13. Exception handling

`ApiException` carries an HTTP status for expected domain/application failures. Domain entities and services use it for invalid invariants, missing resources, and duplicate skills.

`GlobalExceptionHandler` centrally converts:

- `ApiException`;
- Jakarta request validation failures;
- unreadable JSON or invalid enum values;
- database constraint conflicts; and
- unexpected exceptions

into consistent API error responses. Feature-specific exception classes and repeated controller `try/catch` blocks are unnecessary.

`GeminiClient` maps missing configuration, timeout, rate-limit, HTTP, transport, and malformed-response conditions to controlled `ApiException` responses. `RecommendationService` additionally rejects invalid generated entries before persistence and converts unexpected provider failures without exposing configuration or provider internals.

## 14. Dependency injection

Production Spring components consistently use constructor injection:

- controllers receive one cohesive feature service;
- services receive their feature repositories;
- authenticated feature services receive `StudentService`;
- `RecommendationService` receives `RecommendationProvider` plus only the student-scoped repositories needed to assemble context;
- `DashboardController` receives `DashboardService`, which receives `StudentService` and existing repositories through its constructor; and
- security configuration receives authentication collaborators through bean method parameters.

There is no production field injection and no manual construction of services inside application code.

## 15. Practical architecture examples

- `AcademicService.requireStudyPlan(...)` uses `findByIdAndStudent(...)`, preventing a student from updating another student's plan.
- `ProductivityService.requireTask(...)`, `requireHabit(...)`, and `requireCalendarEvent(...)` centralize owned-resource lookup inside one cohesive module.
- `CareerService.requireSkill(...)` applies the same secure scoped-query pattern.
- `StudentService.requireCurrentStudent(...)` is the single reusable conversion from authenticated email to domain student.
- `Skill.normalizeName(...)` preserves `Java` for display while comparing and constraining `java` for duplicate prevention.
- `CareerProfile.replaceInterests(...)` uses domain-specific normalization and retains only the first clean case-insensitive value.
- DTO container classes keep request/response records compact and prevent `Student`, `User`, password hashes, normalized skill names, and JWT state from leaking through APIs.
- `RecommendationService.buildContext(...)` maps entities into provider-owned records, excluding database identifiers, email, roles, secrets, and normalized internal names.
- `Recommendation.normalizeTitle(...)` gives service/database deduplication one stable case- and whitespace-insensitive identity while DTOs expose only the display title.
- `Recommendation.markCompleted()` and `dismiss()` prevent arbitrary or repeated lifecycle mutation.
- `DashboardService.getDashboard(...)` resolves the authenticated student once, composes factual summaries, and delegates progress calculations to `StudyPlan` and `Habit`.

## 16. Relationship diagram

```text
User
 |
 | one-to-one
 v
Student
 |
 +-- AcademicRecord
 +-- StudyPlan implements Trackable
 +-- Task implements Schedulable
 +-- Habit implements Trackable
 +-- CalendarEvent implements Schedulable
 +-- CareerProfile
 |    `-- composed Set<String> interests
 +-- Skill
 `-- Recommendation

Trackable
 |-- StudyPlan
 `-- Habit

Schedulable
 |-- Task
 `-- CalendarEvent

RecommendationProvider <|.. GeminiClient
RecommendationService --> RecommendationProvider

DashboardResponse
 |-- StudentSummary
 |-- AcademicSummary + StudyPlanSummary
 |-- TaskSummary + HabitSummary + CalendarSummary
 |-- CareerSummary
 `-- RecommendationSummary
```
