# Donation Matching Portal — Agent Instructions

## Project

Donation Matching Portal for Seva Sahayog Foundation.

The platform connects donors with receivers and helps administrators review and approve suitable donation matches.

The primary goal is to reduce manual coordination while maintaining transparency and administrative control.

---

## Current Phase

**All phases COMPLETE (Phase 0 through Phase 14).**

The MVP is feature-complete and deployment-ready. The full donation →
requirement → matching → approval → transaction workflow is implemented and
tested, along with search/filtering, global exception handling, OpenAPI/Swagger
documentation, and demo/production preparation (admin seeding, CORS, health
endpoint, Render deployment config).

Remaining work is future/maintenance phases (see the roadmap and revisit
points throughout this document). Do not start a new implementation phase
without an explicit instruction.

---

## Technology Direction

Initial technology choice:

- Backend: Spring Boot
- Database: PostgreSQL
- API style: REST/JSON
- Authentication: JWT
- Password hashing: BCrypt
- Persistence: Spring Data JPA / Hibernate
- API documentation: OpenAPI / Swagger

The architecture should keep the service/business layer independent enough that MongoDB could be considered later if the team changes the database choice.

---

# Roles

The system has exactly three roles:

## DONOR

Can:

- Register/login
- Create donations
- View own donations
- Manage eligible own donations
- Cancel eligible own donations
- View donation status

## RECEIVER

Can:

- Register/login
- Create requirements
- View own requirements
- Manage eligible own requirements
- Search approved donations
- View suitable donation opportunities

## ADMIN

Can:

- Review donations
- Approve/reject donations
- Review requirements
- Approve/reject requirements
- Review suggested matches
- Approve/reject matches
- Monitor transactions
- Access administrative records

ADMIN accounts are seeded/created administratively and are not self-registered through the normal registration flow.

A user account may participate as a donor and receiver; the system should not require separate accounts for both activities.

---

# MVP Requirements

The MVP prioritizes the following problem-statement requirements:

- B — Donor, Receiver and Admin roles
- D — Donor donation submission
- E — Persistent donation/requirement data
- H — Receiver requirement submission
- I — Search and filtering
- L — Suggested matching with Admin review/approval

The MVP also includes:

- Authentication
- JWT
- Role-based authorization
- Ownership checks
- Validation
- Admin approval/rejection
- Basic transaction completion
- Exception handling
- API documentation
- Automated testing

---

# Stretch Features

These are NOT MVP blockers:

- C — Multilingual support
- F — Automatic donation content/image moderation
- G — Donor notifications
- J — Automatic receiver-content moderation
- K — Receiver notifications
- M — Excel/PDF export or email reporting
- Geographic/distance-based matching
- Donation-age scoring
- Dual-party delivery confirmation
- Advanced analytics

Do not allow stretch features to delay the core donation → requirement → matching → approval workflow.

---

# Donation Model — Business Decision

A donation represents an item/resource offered by a donor.

Initial conceptual fields include:

- donor
- title
- description
- category
- quantity
- quantity unit
- condition
- city
- locality
- pincode
- status
- timestamps

Quantity is represented as a value + unit.

Supported units:

- PIECES
- KG
- LITRES
- BAGS
- BOXES
- PACKETS
- SETS

Quantity comparisons are only valid when the units are identical.

Photos are optional but recommended.

MVP photo limit:

- 0–5 photos per donation

---

# Requirement Model — Business Decision

A requirement represents a resource needed by a receiver.

Initial conceptual fields include:

- receiver
- title
- description
- category
- quantity required
- quantity unit
- city
- locality
- pincode
- urgency
- status
- timestamps

Urgency values:

- LOW
- MEDIUM
- HIGH

---

# Location

Location matching uses:

- City — used for matching
- Locality — display information
- Pincode — display information

The MVP does NOT perform geographic distance calculations.

---

# Donation Categories

Use a fixed category enum.

Current categories:

1. FOOD
2. CLOTHING
3. EDUCATION
4. MEDICAL
5. FURNITURE
6. ELECTRONICS
7. HOUSEHOLD
8. HYGIENE
9. OTHER

Do not introduce arbitrary category strings without a deliberate business decision.

---

# Donation Status

Initial lifecycle:

SUBMITTED
→ APPROVED
→ MATCHED
→ IN_FULFILMENT
→ COMPLETED

Rejected/cancelled states are terminal.

A completed donation cannot be reused.

Only APPROVED donations can participate in matching.

---

# Requirement Status

Initial lifecycle:

SUBMITTED
→ APPROVED
→ FULFILLED

Rejected/cancelled states are terminal.

Only APPROVED requirements can participate in matching.

A fulfilled requirement should not be re-matched unless explicitly supported by future business rules.

---

# Match Status

Initial conceptual lifecycle:

SUGGESTED
→ APPROVED
→ FULFILMENT / COMPLETED

Rejected matches are terminal.

The matching engine only SUGGESTS matches.

The Admin makes the final approval decision.

The matching algorithm must never automatically complete a donation.

---

# Transaction Status

Initial lifecycle:

PENDING
→ IN_PROGRESS
→ COMPLETED

A transaction is created only after an Admin approves a match.

The exact transaction/completion workflow will be finalized during the matching and transaction implementation phase.

---

# Matching Strategy

The MVP uses a simple explainable rule-based algorithm.

No machine learning or AI is required.

## Hard gates

A donation and requirement can only be considered compatible when:

1. Both are APPROVED.
2. Categories match.
3. Quantity units match.
4. Donation quantity is sufficient for the requirement.
5. Cities match.

If any hard gate fails, the pair is not suggested.

## Score

Potential score:

- Category: 30
- Quantity: 30
- Location: 20
- Urgency/time: 20

Maximum:

100 points.

Suggested match threshold:

**70/100**

The system may return the top 5 suggestions per requirement.

The algorithm must be explainable so that an Admin can understand why a match was suggested.

---

# Matching Allocation Decision

The initial design supports:

- Multiple requirements competing for donations.
- Multiple donations contributing toward one requirement.

However, exact partial-allocation and locking behavior must be finalized during the matching/transaction implementation phase.

Do not implement assumptions about partial allocation before that phase.

---

# Core Business Rules

1. Only authenticated users can create donations or requirements.
2. Donors can manage only their own donations.
3. Receivers can manage only their own requirements.
4. Only approved donations are visible for matching.
5. Only approved requirements participate in matching.
6. Admin controls approval/rejection.
7. Users cannot approve their own submissions.
8. Completed donations cannot be reused.
9. Fulfilled requirements cannot be re-matched by default.
10. Invalid quantities must be rejected.
11. Quantity comparison requires compatible units.
12. Matching does not automatically complete transactions.
13. Admin approval is required before a suggested match becomes an approved match.

---

# Architecture

Initial architecture:

Frontend
↓
Spring Boot REST API
↓
Controller
↓
Service
↓
Repository
↓
PostgreSQL

Supporting components:

- Security layer → authentication/authorization
- Storage service → donation photos
- Matching service → rule-based matching
- Notification service → future notifications
- Moderation service → future content moderation
- Reporting service → future exports

Business rules belong in the service layer, not controllers or repositories.

Entities must not be exposed directly through API responses.

DTOs should be used for API requests/responses.

---

# Domain Model

Main objects:

- User
- Donation
- DonationPhoto
- Requirement
- Match
- Transaction
- Notification
- AuditRecord

Conceptual relationships:

User 1 → many Donations

User 1 → many Requirements

Donation 1 → many DonationPhotos

Donation many ↔ many Requirement through Match

Match 1 → Transaction

Administrative actions may generate AuditRecord entries.

---

# Security Baseline

The eventual implementation should include:

- BCrypt password hashing
- JWT authentication
- Role-based authorization
- Service-layer ownership checks
- Bean Validation
- Secure file upload validation
- File size/type restrictions
- Authentication rate limiting considerations
- Restricted CORS
- Environment variables for secrets
- No secrets committed to Git
- Appropriate security logging

Security implementation is NOT part of Phase 0.

---

# API Direction

The eventual API will be grouped into:

## Authentication

- registration
- login
- current-user information

## Donations

- create donation
- view own donations
- view/search approved donations
- view donation details
- update/cancel eligible donations

## Requirements

- create requirement
- view own requirements
- search/view requirements

## Matching

- generate/view suggestions
- Admin approve/reject matches

## Administration

- review/approve/reject donations
- review/approve/reject requirements
- manage matches
- view transactions

## Transactions

- view transaction
- update eligible transaction state
- complete transaction

Exact endpoint paths will be finalized during the DTO/controller phases.

---

# MVP User Flow

## Donor

Register/login
→ Create donation
→ Donation SUBMITTED
→ Admin reviews
→ Donation APPROVED

## Receiver

Register/login
→ Create requirement
→ Requirement SUBMITTED
→ Admin reviews
→ Requirement APPROVED

## Matching

Approved donation
+
Approved requirement
→ Matching engine
→ Suggested match
→ Admin reviews
→ Admin approves
→ Transaction created
→ Fulfilment
→ COMPLETED

---

# Implementation Roadmap

## Phase 0 — Requirements & Architecture
COMPLETE.

## Phase 1 — Project & Database Setup
COMPLETE. See "Phase 1 Build Record" below.

## Phase 2 — Domain Models
COMPLETE. See "Phase 2 Build Record" below.

## Phase 3 — Repository Layer
COMPLETE. See "Phase 3 Build Record" below.

## Phase 4 — DTOs & Validation
COMPLETE. See "Phase 4 Build Record" below.

## Phase 5 — Authentication
COMPLETE. Registration, login, BCrypt and JWT. See "Phase 5 Build Record" below.

## Phase 6 — Authorization
COMPLETE. Role-based access and ownership rules. See "Phase 6 Build Record" below.

## Phase 7 — Donation & Requirement Business Logic
COMPLETE. Core donor/receiver workflows and Admin approval. See "Phase 7" in
the Current Phase section; no separate build record was written for it.

## Phase 8 — Search & Filtering
COMPLETE. Approved donation/requirement discovery. See "Phase 8 Build Record" below.

## Phase 9 — Matching Engine
Rule-based scoring and match suggestions. COMPLETE.

## Phase 10 — Admin Match Approval & Transactions
Match approval and fulfilment/completion workflow. COMPLETE.

## Phase 11 — Global Exception Handling
Standard API error responses. COMPLETE.

## Phase 12 — Testing & Security Review
Integration tests, authorization tests, edge cases and production review. COMPLETE.

## Phase 13 — OpenAPI/Swagger
Complete API documentation. COMPLETE.

## Phase 14 — Demo & Production Preparation
Deployment/readiness checks and hackathon demo flow. COMPLETE.

---

# Phase 1 Build Record — Project & Database Foundation

Completed 2026-08-09. Verified working: Spring Boot starts and connects to
PostgreSQL successfully. No domain functionality exists yet.

## Versions

- Spring Boot 4.1.0 (spring-boot-starter-parent)
- Java 21 (compile target; built/run with a Java 21 JDK)
- Maven 3.9.16 via Maven wrapper 3.3.4 (`./mvnw`)
- PostgreSQL 18.4 (Homebrew, local service)
- Hibernate ORM 7.4.1 (managed by Spring Boot)
- Spring Data JPA, HikariCP connection pool

## Dependencies added (pom.xml)

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- org.postgresql:postgresql (runtime)
- org.projectlombok:lombok (optional; project convention)
- spring-boot-starter-test (test)

Not yet added: Spring Security, JWT, OpenAPI/Swagger, Flyway/Liquibase, file
storage, notification, moderation, reporting. They belong to later phases.

## Package structure

Root: `com.sevasahayog.donationmatching`

- `DonationMatchingApplication` — Spring Boot entry point (only class).
- Reserved (empty) packages: config, controller, dto, entity, repository,
  service, security, exception, matching.
- Test: `src/test/java/com/sevasahayog/donationmatching/`.

## Database configuration

- Environment-variable driven via `application.properties`:
  - `DB_URL` (default `jdbc:postgresql://localhost:5432/donation_matching_portal`)
  - `DB_USERNAME` (default `dipanshubhat` — local Homebrew superuser)
  - `DB_PASSWORD` (default empty — local trust auth)
  - `SERVER_PORT` (default 8080)
- Local defaults exist for convenience only; every value can be overridden by
  environment variables. Never commit real credentials.
- `spring.jpa.hibernate.ddl-auto=update` for local development only. Production
  MUST use migrations (Flyway/Liquibase) and `ddl-auto=validate`/`none`.
- `spring.jpa.open-in-view=false`.
- `application-dev.properties` enables SQL logging under the `dev` profile only.
- No domain tables exist yet; `update` currently creates nothing.

## Environment variables (required)

- `DB_URL` — JDBC URL (database `donation_matching_portal`)
- `DB_USERNAME` — database user
- `DB_PASSWORD` — database password (empty for local trust auth)
- `SERVER_PORT` — optional, HTTP port (default 8080)
- `JAVA_HOME` — must point to a Java 21 JDK when building/running

A `.env.example` (variable names only) is committed. `.env`, `.env.*` and other
secrets are gitignored. Spring Boot does not auto-load `.env`; export variables
in the terminal/IDE.

## How to run locally

1. Start PostgreSQL (Homebrew): `brew services start postgresql@18`
2. Ensure database exists: `psql -d postgres -c "CREATE DATABASE donation_matching_portal"`
3. Export environment variables (see above), e.g.:
   `export DB_URL=jdbc:postgresql://localhost:5432/donation_matching_portal
   export DB_USERNAME=dipanshubhat
   export DB_PASSWORD=`
4. Build/test: `JAVA_HOME=<java-21-home> ./mvnw clean test`
5. Run: `JAVA_HOME=<java-21-home> ./mvnw spring-boot:run`
   or `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` for SQL logging.

## Test result (2026-08-09)

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 2 tests, 0 failures, 0 errors.

- `contextLoads` — Spring context + DataSource bean load.
- `databaseConnectionIsAvailable` — real JDBC connection to
  `donation_matching_portal` (PostgreSQL 18.4) succeeds.

Startup verified separately with `./mvnw spring-boot:run`: HikariPool connected,
Tomcat started on port 8080, `Started DonationMatchingApplication in ~1.4s`.

## Decisions made in Phase 1

- Mirrored the sibling Spring Boot project conventions (Java 21, Spring Boot
  4.1.0, Maven wrapper, Lombok, `.properties` + `dev` profile, env-var names
  DB_URL/DB_USERNAME/DB_PASSWORD/SERVER_PORT).
- Local defaults in `application.properties` are dev-only conveniences; all are
  env-var overridable. Documented, not committed secrets.
- `ddl-auto=update` is a Phase-1/Phase-2 development convenience. Migrations are
  required before production; revisit in a later phase.

---

# Phase 2 Build Record — Domain Models / JPA Entities

Completed 2026-08-09. Entities are mapped; Hibernate generated the PostgreSQL
schema and it was verified. No domain business logic exists yet.

## Entities created (package com.sevasahayog.donationmatching.entity)

- `User` — name, email (unique, non-null), password (non-null; stores a hash in
  later phases — never plaintext), role, active (default true), timestamps.
- `Donation` — donor, title, description, category, quantity (BigDecimal 12,3,
  positive), quantityUnit, condition, city, locality, pincode, status (default
  SUBMITTED), version, timestamps.
- `DonationPhoto` — donation, storageKey (required), originalFilename,
  contentType, fileSize, createdAt. Stores metadata/reference only, never image
  binary in PostgreSQL.
- `Requirement` — receiver, title, description, category, quantityRequired
  (BigDecimal 12,3, positive), quantityUnit, city, locality, pincode, urgency,
  status (default SUBMITTED), version, timestamps.
- `Match` — donation, requirement, score (BigDecimal 5,2), status (default
  SUGGESTED), reviewedAt, reviewedBy, version, timestamps.
- `Transaction` — match (unique OneToOne), donor, receiver, status (default
  PENDING), completedAt, version, timestamps.
- `AuditRecord` — actor (nullable), action, entityType, entityId (String),
  details, createdAt. Immutable/append-only.

## Enums created

- `Role` — DONOR, RECEIVER, ADMIN
- `DonationStatus` — SUBMITTED, APPROVED, MATCHED, IN_FULFILMENT, COMPLETED,
  REJECTED, CANCELLED
- `RequirementStatus` — SUBMITTED, APPROVED, FULFILLED, REJECTED, CANCELLED
- `MatchStatus` — SUGGESTED, APPROVED, IN_FULFILMENT, COMPLETED, REJECTED,
  CANCELLED
- `TransactionStatus` — PENDING, IN_PROGRESS, COMPLETED, CANCELLED
- `Category` — FOOD, CLOTHING, EDUCATION, MEDICAL, FURNITURE, ELECTRONICS,
  HOUSEHOLD, HYGIENE, OTHER
- `QuantityUnit` — PIECES, KG, LITRES, BAGS, BOXES, PACKETS, SETS
- `Urgency` — LOW, MEDIUM, HIGH
- `Condition` — NEW, GOOD, FAIR, USED

All enums persisted with `@Enumerated(EnumType.STRING)` (varchar columns;
Hibernate also emits DB-level CHECK constraints of allowed values).

## Relationships

- Donation.donor -> User (ManyToOne LAZY, non-null)
- Requirement.receiver -> User (ManyToOne LAZY, non-null)
- DonationPhoto.donation -> Donation (ManyToOne LAZY, non-null)
- Match.donation -> Donation, Match.requirement -> Requirement (ManyToOne LAZY)
- Match.reviewedBy -> User (ManyToOne LAZY, nullable)
- Transaction.match -> Match (OneToOne LAZY, non-null, unique)
- Transaction.donor/receiver -> User (ManyToOne LAZY, non-null)
- AuditRecord.actor -> User (ManyToOne LAZY, nullable)

No cascading REMOVE from User to any history. Historical donation/match/
transaction/audit records are never deleted via user removal. Collections are
minimized (all relationships are owning-side ManyToOne/OneToOne); no
bidirectional collections exist yet.

## Important constraints

- `users.email` UNIQUE (`uk_users_email`), all required fields NOT NULL.
- Quantity/quantityRequired: NOT NULL, numeric(12,3), CHECK > 0.
- Enum columns: varchar + DB CHECK of allowed string values (auto-generated).
- `matches (donation_id, requirement_id)` UNIQUE (`uk_matches_donation_requirement`)
  — prevents duplicate suggestions for a pair; its btree also serves
  donation_id lookups. NOTE: this also blocks re-creating a match for the same
  pair after a rejection/cancellation. Revisit if re-matching is ever required.
- `transactions.match_id` UNIQUE — one transaction per approved match.

## Indexes

- Donation: donor_id, status, category, city
- Requirement: receiver_id, status, category, city
- DonationPhoto: donation_id
- Match: requirement_id, status (+ the unique pair constraint covers donation_id)
- Transaction: status (match_id covered by its unique index)
- AuditRecord: (entity_type, entity_id), actor_id

Rationale: ownership lookups (donor_id/receiver_id), admin review queues
(status), and search/matching filters (category, city). Indexes deliberately
avoided for rarely-filtered columns (locality, pincode, timestamps).

## Timestamp strategy

- `Instant` everywhere (timestamptz columns), `@CreationTimestamp` for createdAt
  (updatable = false) and `@UpdateTimestamp` for updatedAt — matching the sibling
  project convention. No custom auditing framework.

## Optimistic locking decision

`@Version long version` added to Donation, Requirement, Match, Transaction —
mutable business entities that can be changed concurrently (e.g., Admin
approving while a donor edits, or matching allocation races). Retry/conflict
handling is deferred to a later phase. User, DonationPhoto and AuditRecord are
not versioned (append-light / immutable).

## Deferred by decision

- `Notification` NOT created. AGENTS.md lists it in the domain model, but the
  notification service is a stretch feature (G/K) with no MVP consumer; it will
  be introduced in the notification phase.

## Database verification (2026-08-09)

Started the app with ddl-auto=update; Hibernate created all 7 tables in
`donation_matching_portal`:

- Tables: users, donations, donation_photos, requirements, matches,
  transactions, audit_records — exactly 7, no unexpected tables.
- Foreign keys confirmed for every relationship above.
- Unique constraints confirmed (email, match pair, transaction match_id).
- CHECK constraints confirmed (positive quantity, enum value whitelists).
- Indexes confirmed for all declared indexes.
- Enums stored as varchar strings (verified by insert + `pg_indexes` output).
- All tables empty after testing — the persistence test is @Transactional and
  rolls back; no business/test data committed.

## Tests executed and result

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 3 tests, 0 failures, 0 errors.

- `DonationMatchingApplicationTests.contextLoads` / `databaseConnectionIsAvailable`
  (from Phase 1).
- `EntityPersistenceTest.persistsRepresentativeEntitiesAndRelationships` —
  persists User, Donation, DonationPhoto, Requirement, Match, Transaction,
  AuditRecord; verifies IDs, default statuses, timestamps, and relationship
  links via the EntityManager.

## Assumptions / deviations

- Donation.condition is required (NOT NULL): item condition is a core quality
  attribute for admin review/matching.
- Match.score is required (NOT NULL, numeric(5,2)): every suggestion carries a
  score.
- Transaction stores donor/receiver denormalized (in addition to the Match) to
  make fulfilment bookkeeping direct, per Phase 2 spec.
- The match pair UNIQUE constraint blocks historical re-matching of the same
  pair; documented as a revisit point.

---

# Phase 3 Build Record — Repository Layer

Completed 2026-08-09. Spring Data JPA repositories added and tested against
PostgreSQL. No business logic in this layer.

## Repositories created (package com.sevasahayog.donationmatching.repository)

All extend `JpaRepository<Entity, Long>`.

- `UserRepository`
- `DonationRepository`
- `DonationPhotoRepository`
- `RequirementRepository`
- `MatchRepository`
- `TransactionRepository`
- `AuditRecordRepository`

## Important query methods

- **UserRepository** — `findByEmail`, `existsByEmail`. Email lookup is exact
  (case-sensitive); email normalization is a Phase 5 (auth) concern, not a
  repository concern.
- **DonationRepository** — `findByIdAndDonorId` (ownership), `findAllByDonorId`
  (Page), `findAllByStatus` (Page, admin queue), `findAllByStatusAndCategory`
  (Page), `findAllByStatusAndCategoryAndCity` (Page, search preview).
- **DonationPhotoRepository** — `findAllByDonationId`.
- **RequirementRepository** — `findByIdAndReceiverId` (ownership),
  `findAllByReceiverId` (Page), `findAllByStatus` (Page, admin queue),
  `findAllByStatusAndCategory` (Page), `findAllByStatusAndCategoryAndCity` (Page).
- **MatchRepository** — `findByDonationId`, `findByRequirementId`,
  `findByDonationIdAndStatus`, `findByRequirementIdAndStatus`,
  `existsByDonationIdAndRequirementId`,
  `findTop5ByRequirementIdAndStatusOrderByScoreDesc` (suggestion ranking),
  `findAllByStatus` (Page, admin review queue).
- **TransactionRepository** — `findByMatchId` (Optional; match_id is unique),
  `findAllByStatus` (Page), `findByDonorId`, `findByReceiverId`.
- **AuditRecordRepository** — `findByEntityTypeAndEntityId`,
  `findByActorId`, `findAllByOrderByCreatedAtDesc` (DB-level ordering via method
  name, not in-memory sort).

## Pagination decisions

`Page<Entity> findAllBy*(…, Pageable)` is used wherever a list can realistically
grow: ownership lists (donations/requirements by user), admin status queues
(donations/requirements/matches/transactions), and search-filter previews.
`findByMatchId` returns Optional (unique). Small/lookup-oriented queries
(donation photos, audit lookups, match lookups by donation/requirement) return
`List`. The matching top-5 suggestion query returns a bounded List (Top5).

## @Query / custom queries

None. All queries are Spring Data derived queries; no native SQL or JPQL needed.

## EntityGraph / fetch decisions

None added. All Phase 2 relationships remain LAZY. The top-5 suggestion query
accesses donation/requirement fields lazily within a service transaction in the
future matching phase; no EAGER or @EntityGraph was introduced. This will be
revisited in Phase 9 only if a concrete query shows N+1 pain.

## Ownership-query decisions

`findByIdAndDonorId` / `findByIdAndReceiverId` exist so services can enforce
ownership in a single indexed query. Repositories contain NO authorization and
never touch Spring Security; enforcement remains a service-layer responsibility.

## Constraints verified

Phase 2 database constraints were re-verified through the repository layer:
duplicate user email, duplicate match pair, and non-positive quantity all throw
`DataIntegrityViolationException`.

## Tests executed and result

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 14 tests, 0 failures, 0 errors.

- `RepositoryLayerTest` — 11 tests covering every repository's representative
  queries plus the three constraint violations above. @SpringBootTest +
  @Transactional (rolls back; development DB left empty).
- `EntityPersistenceTest` (Phase 2) and `DonationMatchingApplicationTests`
  (Phase 1) still pass.

## Queries intentionally deferred to Phase 8

The complete search/filter API (optional filter combinations, sorting, paging
params) is Phase 8. Phase 3 only added the core status/category/city query shapes
needed to prove the pattern; Phase 8 will add any additional derived queries or
Specifications only if derived queries become unreadable.

## Issues / revisit points

- Email uniqueness is case-sensitive at the DB level (`uk_users_email`). If
  case-insensitive login is desired, the auth phase must normalize emails
  (lowercase) at registration/login rather than adding DB functions.
- No N+1 or fetch concern identified yet; revisit in Phase 9 if the matching
  query pattern proves slow.

---

# Phase 4 Build Record — DTOs & Validation

Completed 2026-08-09. DTO layer created as Java records (matching the sibling
project convention). No controllers/services/security yet.

## DTOs created (package com.sevasahayog.donationmatching.dto)

Request DTOs:
- `UserRegisterRequest` — name, email, password.
- `DonationRequest` — title, description, category, quantity, quantityUnit,
  condition, city, locality, pincode.
- `RequirementRequest` — title, description, category, quantity, quantityUnit,
  city, locality, pincode, urgency.

Response DTOs:
- `UserResponse` — id, name, email, role.
- `UserSummaryResponse` — id, name (nested donor/receiver/actor summary).
- `DonationResponse` — id, title, description, category, quantity, quantityUnit,
  condition, city, locality, pincode, status, donor summary, photos, createdAt,
  updatedAt.
- `DonationSummaryResponse` — id, title, city, status (used in MatchResponse).
- `DonationPhotoResponse` — id, storageKey, originalFilename, contentType,
  fileSize, createdAt.
- `RequirementResponse` — id, title, description, category, quantity,
  quantityUnit, city, locality, pincode, urgency, status, receiver summary,
  createdAt, updatedAt.
- `RequirementSummaryResponse` — id, title, city, status.
- `MatchResponse` — id, donation summary, requirement summary, score, status,
  reviewedAt, reviewedBy summary, createdAt, updatedAt.
- `TransactionResponse` — id, matchId, donor summary, receiver summary, status,
  createdAt, updatedAt, completedAt.
- `AuditRecordResponse` — id, entityType, entityId, action, actor summary,
  details, createdAt.

## Validation rules

- **UserRegisterRequest** — name: @NotBlank + @Size(max=100); email: @NotBlank +
  @Email + @Size(max=255); password: @NotBlank + @Size(min=8, max=72).
- **DonationRequest** — title @NotBlank @Size(max=200); description @NotBlank
  @Size(max=2000, API cap — entity column is TEXT); category @NotNull; quantity
  @NotNull @Positive @Digits(integer=9, fraction=3) matching numeric(12,3);
  quantityUnit @NotNull; condition @NotNull; city @NotBlank @Size(max=100);
  locality @Size(max=100); pincode @Size(max=20).
- **RequirementRequest** — same text/quantity/category/unit/city rules plus
  urgency @NotNull.

`@Digits(integer=9, fraction=3)` mirrors the Phase 2 numeric(12,3) columns so
database and API precision agree.

## Server-controlled fields (never accepted in requests)

- IDs, user/owner IDs, role, statuses, timestamps, approval/review fields, match
  scores, transaction status. None of these exist on request DTOs; unknown JSON
  properties are ignored by Jackson, so a client sending
  `{"status":"APPROVED","donorId":123}` has those values silently dropped (not
  bound). A serialization test locks this in.

## Sensitive fields excluded from responses

- Passwords/password hashes (UserResponse has no password; UserSummaryResponse
  has id+name only). No internal persistence details (version, actor ids as raw
  refs) are exposed. Serialization tests assert the JSON never contains
  "password".

## Mapping strategy

Static `from(entity)` factory methods on each response DTO (e.g.,
`DonationResponse.from(donation, photos)`). No MapStruct or mapping library.
Request→entity conversion is deferred to the service layer (Phase 7).
`DonationResponse.from(Donation)` (single-arg) maps with an empty photo list;
services pass photos explicitly. LAZY relationships are only touched inside
service transactions; DTO mapping never triggers uncontrolled lazy loading.

## Enum handling

DTOs use the Phase 2 entity enums (Role, DonationStatus, RequirementStatus,
MatchStatus, TransactionStatus, Category, QuantityUnit, Urgency, Condition)
directly. Jackson maps strings to enums; an invalid value throws during
deserialization and will surface as a 400 via the future exception handler.
Enum deserialization behavior is covered by tests.

## Tests performed and result

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 56 tests, 0 failures, 0 errors.

- `DtoValidationTest` (32 tests) — every listed case for registration, donation,
  and requirement requests (valid + blank/oversized/malformed inputs).
- `DtoSerializationTest` (10 tests) — response DTOs never serialize "password";
  summaries expose only id/name; enum string deserialization; invalid enum
  throws; server-controlled JSON fields are ignored.
- Phase 1–3 tests (RepositoryLayerTest, EntityPersistenceTest, smoke tests)
  still pass.

## Decisions / revisit points

- `DonationRequest` deliberately has NO urgency field — the Donation entity has
  no urgency (Requirement only). Condition is included and required.
- Donation photos are NOT part of DonationRequest; photo upload is a later
  dedicated endpoint. `DonationPhotoResponse.storageKey` is exposed so the
  client can reference the stored file (no serving layer yet); revisit when a
  file-serving/URL layer is added.
- Description capped at 2000 chars at the API (DB is TEXT); revisit if needed.
- Pincode kept as a length-constrained optional string (no regex); MVP treats it
  as display info.

---

# Phase 5 Build Record — Authentication & JWT Security

Completed 2026-08-09. Registration, login, BCrypt password hashing and JWT
authentication are implemented and tested. No other business logic was added.

## Dependencies added (pom.xml)

- `spring-boot-starter-security`
- `io.jsonwebtoken:jjwt-api` (0.12.6), `jjwt-impl` (runtime), `jjwt-jackson`
  (runtime) — managed via `jjwt.version` property.
- `spring-security-test` (test) and `spring-boot-webmvc-test` (test; provides
  `@AutoConfigureMockMvc` in Spring Boot 4).

## Files created

Security (`com.sevasahayog.donationmatching.security`):
- `JwtProperties` — `@ConfigurationProperties(prefix = "jwt")` record
  (`secret`, `expirationMs`). Fails fast at startup when `secret` is blank or
  shorter than 32 bytes (HS256/384 requires a 256-bit key) or when expiration
  is not positive.
- `JwtService` — generates and verifies tokens. Claims are minimal: `sub`
  (email), `iat`, `exp`. Role is NOT embedded in the token; the authoritative
  role is always loaded from the database on every request, so role changes
  take effect immediately.
- `UserPrincipal` — `UserDetails` wrapper over `User` (id, email, password
  hash, role, active). Authority is `ROLE_<ROLE>`. Password hash is required by
  `UserDetails` and is never serialized to API responses.
- `AppUserDetailsService` — `UserDetailsService` that normalizes email
  (trim + lowercase) and loads the user by email.
- `JwtAuthenticationFilter` — `OncePerRequestFilter` before
  `UsernamePasswordAuthenticationFilter`. Reads `Authorization: Bearer <token>`,
  validates signature/expiration, loads the user, and populates the
  SecurityContext only when the account is enabled. Skips `/api/auth/**`.
- `SecurityConfig` — stateless, CSRF off, form login/basic auth off, public
  `/api/auth/register` and `/api/auth/login`, everything else authenticated,
  401 authentication entry point / 403 access-denied handler returning JSON,
  BCrypt `PasswordEncoder`, and an `AuthenticationManager` bean.

Auth (`controller`, `service`, `dto`):
- `AuthController` — `POST /api/auth/register` (201) and
  `POST /api/auth/login` (200).
- `AuthService` — register + login. Login delegates to
  `AuthenticationManager` (DaoAuthenticationProvider), which rejects unknown
  emails, wrong passwords, and disabled accounts with `AuthenticationException`
  → 401.
- `LoginRequest`, `AuthResponse` — new DTOs. `AuthResponse` carries
  `accessToken`, `tokenType`, `expiresIn`, `userId`, `email`, `role`.

Exceptions (`exception`):
- `DuplicateEmailException` → 409.
- `ErrorResponse` (timestamp, status, error, message, path) + a MINIMAL
  `GlobalExceptionHandler` covering only what authentication needs: validation
  400, malformed body 400, duplicate email 409, authentication failure 401,
  unknown path 404, fallback 500. Phase 11 will expand this into the full
  standard error system.

## Files modified

- `pom.xml` — security/JWT/test dependencies.
- `src/main/resources/application.properties` — `jwt.secret` and
  `jwt.expiration-ms` (env-driven).
- `.env.example` — added `JWT_SECRET` and `JWT_EXPIRATION_MS`.
- `src/test/resources/application.properties` (new) — test-only JWT secret so
  the security layer initializes during the build without a real secret.

## Endpoints

- `POST /api/auth/register` — public. Validates `UserRegisterRequest`, stores
  email lowercased/trimmed and password as a BCrypt hash, returns 201 with
  `AuthResponse`. Role is always `DONOR` (server-side); any client-supplied
  `role` is ignored.
- `POST /api/auth/login` — public. Normalized email + password via
  `AuthenticationManager`; returns 200 with `AuthResponse` or 401.
- Everything else — requires a valid JWT (401 without one).

## Registration role decision

New registrations are created with role `DONOR` ("normal application user").
The registration API does NOT accept a role and there is no admin-registration
endpoint. AGENTS.md states a single account may act as both donor and receiver;
Phase 6 must decide how the `RECEIVER` role is obtained (e.g. a separate
role-granting flow) and how authorization treats a user who should act as both.
ADMIN is not obtainable through registration.

## Admin seeding approach (documented only)

ADMIN accounts are created administratively, not through the API. Recommended
approach (not yet scripted): insert the ADMIN row directly into PostgreSQL with
a BCrypt hash produced by the application's encoder, e.g. generate the hash
with a one-off `PasswordEncoder` invocation and
`INSERT INTO users (name, email, password, role, active) VALUES (...);`.
A seeding script can be added in a later phase (e.g. Phase 10/14). No secrets
are committed.

## Email normalization

- Registration: email is trimmed + lowercased in `AuthService` before the
  uniqueness check and persistence. The DB `uk_users_email` uniqueness is
  therefore effectively case-insensitive for API-created users (the constraint
  itself is still case-sensitive; revisit if direct DB writes could collide).
- Login: the presented email is normalized before authentication, and
  `AppUserDetailsService` normalizes again, so `Alice@Example.com` logs into
  `alice@example.com`.
- Boundary behavior: `@Email` validation runs on the RAW request value, so an
  email padded with surrounding whitespace is rejected with 400 before
  normalization. Only lowercase/uppercase variance is normalized at the API.

## Environment variables

- `JWT_SECRET` — REQUIRED to start the application (blank is rejected at
  startup). Must be at least 32 bytes; generate with `openssl rand -base64 48`.
  Never commit a real secret.
- `JWT_EXPIRATION_MS` — access-token lifetime in milliseconds (default
  `3600000` = 1 hour).
- Tests override both via `src/test/resources/application.properties` with a
  test-only secret; the main application has no baked-in secret default.

## Tests executed and result

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 76 tests, 0 failures, 0 errors.

- `AuthenticationIntegrationTest` (20 tests) — successful registration returns
  token + DONOR role; email lowercasing; duplicate email (case-insensitive) →
  409; password stored as a BCrypt hash; client-supplied ADMIN/RECEIVER roles
  ignored; password never appears in responses; validation 400s; successful
  login; wrong password → 401; unknown email → 401; inactive user → 401;
  protected endpoint without token → 401; malformed/expired/invalid-signature
  JWT → 401; valid JWT authenticates (reaches an unauthenticated-safe request,
  currently a 404 for the not-yet-implemented `/api/donations`); token for a
  deleted/unknown user → 401.
- Phase 1–4 tests (RepositoryLayerTest, EntityPersistenceTest, DtoValidationTest,
  DtoSerializationTest, smoke tests) still pass.

A live smoke run (app started with `JWT_SECRET` exported) verified register 201,
login 200 with uppercase email, 401 without token, 401 with garbage token, and
404 (authenticated) on `/api/donations`.

## Decisions / assumptions Phase 6 must know

1. Default registration role is `DONOR`. Decide how users become `RECEIVER`
   (AGENTS.md allows one account to be both donor and receiver) before adding
   role-based endpoint rules.
2. Method-level role authorization was intentionally NOT added
   (`@EnableMethodSecurity` / `@PreAuthorize` are absent). Phase 6 adds it.
3. JWT carries no role claim; the role is read from the database on every
   request, so it can be changed (e.g. promote to RECEIVER) without token
   reissuance. If a role claim is ever added, plan for re-issuance on role
   change.
4. `@Email` rejects whitespace-padded emails (400) — normalization handles case
   only at the API boundary. Do not rely on trimming in authorization logic.
5. The minimal `GlobalExceptionHandler`/`ErrorResponse` is auth-scoped; Phase 11
   expands it (including field-level `fields` on validation errors).
6. No other controllers exist yet; the only authenticated-safe behavior for a
   valid token is reaching the not-found handler (404). Phase 7+ endpoints will
   give real protected behavior.
7. Inactive (`active=false`) users cannot log in (401) and are not
   authenticated by the JWT filter.

---

# Phase 6 Build Record — Authorization, RBAC & Ownership

Completed 2026-08-09. Role-based authorization via Spring Security method-level
security, service-layer ownership checks, and minimal protected endpoints that
prove the security model. No Phase 7+ business logic was added.

## Authorization design

- `@EnableMethodSecurity` added to `SecurityConfig`.
- Controllers carry `@PreAuthorize("hasRole('...')")`; the filter chain still
  requires authentication for every non-auth endpoint (401 for anonymous).
- `AccessDeniedException` (from `@PreAuthorize`) and `ForbiddenException`
  (from ownership checks) both produce a JSON 403 via `GlobalExceptionHandler`.
- Authorization is enforced entirely on the backend. Frontend role checks, when
  added, are UI convenience only and are never authoritative.

## Final DONOR / RECEIVER decision

**Preserved the existing single-role model.** The `User` entity has one `role`
column (DONOR/RECEIVER/ADMIN). Supporting "one account acts as both donor and
receiver" would require a schema redesign (role collection or a SET column),
which is out of scope for this MVP phase. Consequence (documented limitation):

- Registration always creates `DONOR` (unchanged from Phase 5).
- `RECEIVER` and `ADMIN` are provisioned administratively, exactly like ADMIN
  (DB seeding; no API path). A user who must act as both donor and receiver
  currently requires an administrative role change, or a future-phase schema
  change (e.g. a `user_roles` join table). Documented revisit point.

## Role matrix (enforced server-side)

| Operation                         | DONOR | RECEIVER | ADMIN |
| --------------------------------- | ----: | -------: | ----: |
| POST /api/auth/register           | Public|   Public |   No  |
| POST /api/auth/login              | Public|   Public |   No  |
| GET /api/me (view own profile)    |  Yes  |    Yes   |  Yes  |
| GET /api/donations/{id}           |  Yes  |    No    |   No* |
| PATCH /api/donations/{id}         |  Yes  |    No    |   No* |
| GET /api/requirements/{id}        |   No  |    Yes   |   No* |
| PATCH /api/requirements/{id}      |   No  |    Yes   |   No* |
| GET /api/admin/queue              |   No  |    No    |  Yes  |

`*` ADMIN does NOT implicitly receive donor/receiver CRUD (least privilege).
The Phase 6 stub endpoints are role-gated to their actor role; if a later phase
needs ADMIN access to a donor/receiver resource it must be granted explicitly
and deliberately.

## ADMIN provisioning decision

Unchanged from Phase 5: ADMIN is created only outside the public registration
flow (direct DB insert with a BCrypt hash produced by the application encoder).
Registration still ignores any client-supplied `role`. There is no endpoint to
promote a user; a normal authenticated user cannot change their own role.

## Ownership strategy

- The authenticated user's ID comes only from the Spring Security principal
  (`UserPrincipal`), which is populated by the JWT filter from the database.
  Client-supplied `userId`/`donorId`/`receiverId`/`id` in request bodies are
  never trusted (stub PATCH endpoints ignore the body entirely).
- Service-layer checks use the single indexed ownership queries from Phase 3:
  `findByIdAndDonorId(id, userId)` for donations and
  `findByIdAndReceiverId(id, userId)` for requirements.
- A resource that does NOT exist OR is not owned by the caller returns **403**
  (not 404). This deliberately hides whether another user's private resource
  exists. If existence-hiding is ever relaxed (e.g. when donations become
  publicly searchable in Phase 8), switch these to 404 for truly missing rows.

## JWT role decision

Unchanged from Phase 5: the JWT contains no role claim. It identifies the user
by `sub` (email); the backend loads the user from the database on every request
and uses the current DB role. Consequences, now verified by tests:

- Changing a role in the database takes effect without reissuing a token.
- A forged role claim cannot elevate privileges (there is no role claim to
  forge).

## Endpoints added (minimal proofs; Phase 7 will expand/replace)

- `GET /api/me` — any authenticated role; returns `CurrentUserResponse`
  (id, email, role). Real endpoint.
- `GET /api/donations/{id}` — DONOR only + ownership; returns `DonationResponse`.
- `PATCH /api/donations/{id}` — DONOR only + ownership; **stub**: verifies
  role+ownership, ignores the body, returns 204. Actual update logic is Phase 7.
- `GET /api/requirements/{id}` — RECEIVER only + ownership; returns
  `RequirementResponse`.
- `PATCH /api/requirements/{id}` — RECEIVER only + ownership; **stub** (same as
  donation stub).
- `GET /api/admin/queue` — ADMIN only; placeholder that proves the admin gate.

These stubs exist only to prove the security model now; Phase 7 replaces them
with the real donation/requirement workflows.

## Files created

- `dto/CurrentUserResponse`
- `controller/{MeController, DonationController, RequirementController, AdminController}`
- `service/{DonationService, RequirementService}` (ownership checks)
- `exception/ForbiddenException`
- tests: `AuthorizationIntegrationTest`, `OwnershipIntegrationTest`

## Files modified

- `security/SecurityConfig` — added `@EnableMethodSecurity`.
- `exception/GlobalExceptionHandler` — added `AccessDeniedException` (403) and
  `ForbiddenException` (403) handlers.
- `AGENTS.md` — this record.

## Security-test coverage

- `AuthorizationIntegrationTest` (15 tests): unauthenticated → 401; DONOR /
  RECEIVER / ADMIN tokens each authenticate on `/api/me`; `/api/me` never
  exposes "password"; full role matrix (DONOR↔donor endpoint allowed,
  DONOR→receiver/admin 403, RECEIVER↔receiver allowed, RECEIVER→donor/admin
  403, ADMIN→admin allowed, ADMIN→donor/receiver 403); registration cannot
  create ADMIN; an authenticated user cannot escalate their own role through
  any endpoint.
- `OwnershipIntegrationTest` (10 tests): DONOR A views/modifies own donation
  (200/204); DONOR A cannot view/modify DONOR B's donation (403); RECEIVER A
  same for requirements; request-body `id`/`donorId` cannot bypass ownership
  (path + principal are authoritative); nonexistent resource returns 403
  (no existence leak).
- Authentication coverage (401, expired/forged/malformed JWT) remains in
  `AuthenticationIntegrationTest` from Phase 5.

## Tests executed and result

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 101 tests, 0 failures, 0 errors.

Breakdown: AuthenticationIntegrationTest 20, AuthorizationIntegrationTest 15,
OwnershipIntegrationTest 10, RepositoryLayerTest 11, DtoValidationTest 32,
DtoSerializationTest 10, EntityPersistenceTest 1, smoke tests 2. All Phase 1–5
tests still pass.

## Limitations / deferred to later phases

1. One account = one role today. Acting as both donor and receiver requires an
   administrative role change or a future role-collection schema change.
2. `PATCH /api/donations|requirements/{id}` are authorization stubs (204,
   body ignored); real mutation lands in Phase 7.
3. `GET /api/admin/queue` is a placeholder; the real admin review/approval
   workflows land in Phase 7/9/10.
4. Ownership denial returns 403 for both "not found" and "not yours" to hide
   resource existence; revisit when public search (Phase 8) is designed.
5. `GlobalExceptionHandler` remains auth/authorization-scoped; Phase 11 builds
   the full standard error system.

---

# Phase 8 Build Record — Search, Filtering & Discovery

Completed 2026-08-10. Discovery endpoints for approved donations and
requirements with filters, pagination and role-based authorization. Matching
(Phase 9) was NOT implemented.

## Endpoints created

- `GET /api/donations` — discovery/search of donations.
  - RECEIVER → only APPROVED donations; the `status` parameter is NOT
    accepted (400 if supplied); availability is always APPROVED.
  - ADMIN → all statuses; optional `status` filter.
  - DONOR → 403. Unauthenticated → 401.
- `GET /api/requirements` — discovery/search of requirements.
  - DONOR → only APPROVED requirements; `status` param NOT accepted (400).
  - ADMIN → all statuses; optional `status` filter.
  - RECEIVER → 403. Unauthenticated → 401.

## Supported filters

- `category` — enum `Category`; invalid value → 400.
- `city` — string, trimmed, case-insensitive equality.
- `query` — optional free text over title OR description, case-insensitive
  substring; capped at 200 chars (400 beyond).
- `status` — ADMIN only (see above); invalid value → 400.
- Pagination `page` / `size` (Spring Data `Pageable`).
- `sort` — whitelisted to `createdAt`; anything else → 400.

Example: `GET /api/donations?category=FOOD&city=Pune&page=0&size=10`.

## Authorization

- Method-level `@PreAuthorize` on the endpoints (RECEIVER|ADMIN for
  donations; DONOR|ADMIN for requirements) plus service-layer role checks
  as defense-in-depth. Backend remains authoritative; the frontend never
  decides access.

## Pagination / sorting

- Spring Data `Page` + `Pageable`; the database executes the count + content
  queries (no in-memory paging).
- Default order: `createdAt` DESC (newest first) via `@PageableDefault`.
- `spring.data.web.pageable.max-page-size=100` clamps oversized `size`
  (e.g. size=500 → 100).
- Spring Data web safely normalizes `page=-1` → 0 and non-numeric `size` →
  the configured default (20); verified by tests. `sort` is whitelisted in
  the service to `createdAt`; an invalid/other property → 400.

## Repository / search implementation

- `DonationRepository` and `RequirementRepository` now also extend
  `JpaSpecificationExecutor` (existing derived queries untouched).
- Small `Specification` builders: `DonationSpecifications` /
  `RequirementSpecifications` (status, category, case-insensitive city,
  title/description text). Derived-query combinations were avoided because
  the optional-filter matrix made them excessive.
- Services build the `Specification` (availability rules live in the service
  layer), call `findAll(spec, pageable)` and map to the existing
  `DonationResponse` / `RequirementResponse` DTOs. No entities exposed.
- No native SQL, no query-string concatenation, no authorization in
  repositories.

## Indexes

- None added. Discovery predicates use the Phase 2 indexes on status,
  category and city. A composite index was not added because the filter
  combinations are open-ended and Phase 8 data volume does not justify it;
  revisit if search performance demands it.

## Error handling (GlobalExceptionHandler additions)

- `MethodArgumentTypeMismatchException` → 400 (invalid enum `category`/`status`).
- `MissingServletRequestParameterException` → 400.
- `IllegalArgumentException` → 400 (defensive; Spring Data pageable resolution
  normalizes rather than throwing in this stack).
- `HttpRequestMethodNotSupportedException` → 405 (previously fell into the
  catch-all 500, e.g. a GET on a POST-only path).
- Reuses the existing `ErrorResponse`; no stack traces leak.

## Design decisions

1. Discovery role mapping: RECEIVER searches approved donations; DONOR
   searches approved requirements; ADMIN searches both with an optional
   status filter. Matches the Phase 8 spec's intended discovery model.
2. `status` is a discovery filter for ADMIN only. Non-admin roles always get
   APPROVED results and any explicit `status` parameter is rejected with 400
   (rather than silently ignored) so the contract is explicit.
3. Full `DonationResponse`/`RequirementResponse` are returned (not the
   summary DTOs) so searchers get the detail they need to choose.
4. City matching is case-insensitive (trim + lower) because city names are
   human-typed; category/status remain exact enum equality.
5. Text search is simple `lower(x) LIKE %term%` over title/description; no
   full-text infrastructure (per spec: no search framework).
6. `GET /api/donations` makes the previously-unmatched GET path a real
   endpoint; `GET /api/donations/my` and `/api/donations/{id}` remain
   unchanged. `/api/donations/{id}` was NOT relaxed to RECEIVER (search
   returns full detail; single-donation detail stays donor/admin) — see
   revisit points.

## Pre-existing test fixes (found during regression)

Two Phase 7-era tests were failing before this phase; both were corrected:

- `AuthenticationIntegrationTest.validJwtAuthenticatesAndReachesProtectedRequest`
  expected 404 on `GET /api/donations`, which 500'd because the path matched
  a POST-only mapping (HttpRequestMethodNotSupported → catch-all 500). The
  test now hits a genuinely unmatched path (`/api/no-such-endpoint`) and
  still proves a valid token passes the security filter.
- `DonationLifecycleIntegrationTest.concurrentStaleUpdateThrowsOptimisticLockingFailure`
  used a REQUIRED (same-transaction) TransactionTemplate, so `findById`
  returned the SAME managed instance and no version conflict occurred. It now
  seeds/updates through PROPAGATION_REQUIRES_NEW transactions and cleans up
  afterwards, asserting either ObjectOptimisticLockingFailureException or
  OptimisticLockException.

## Tests executed and result

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 165 tests, 0 failures, 0 errors.

New `SearchIntegrationTest` (32 tests) covers:
- Donation discovery: receiver search, rejected/submitted exclusion, category
  filter, case-insensitive city filter, pagination, admin all-status search,
  admin status filter, donor 403, unauthenticated 401, receiver status-filter
  400, invalid category/status 400, text search (title+description), sort
  whitelist 400, page normalization, non-numeric size fallback, size clamp
  (max-page-size=100), combined category+city+pagination.
- Requirement discovery: donor search, rejected/submitted exclusion, category
  and city filters, pagination, admin all-status search, receiver 403,
  unauthenticated 401, donor status-filter 400, invalid category 400, text
  search, combined category+city+pagination.
- All Phase 1–7 tests still pass.

## Files created

- `repository/DonationSpecifications.java`
- `repository/RequirementSpecifications.java`
- `test/SearchIntegrationTest.java`

## Files modified

- `repository/DonationRepository.java` — added `JpaSpecificationExecutor`.
- `repository/RequirementRepository.java` — added `JpaSpecificationExecutor`.
- `service/DonationService.java` — `search(...)`, spec building, sort whitelist.
- `service/RequirementService.java` — `search(...)`, spec building, sort whitelist.
- `controller/DonationController.java` — `GET /api/donations`.
- `controller/RequirementController.java` — `GET /api/requirements`.
- `exception/GlobalExceptionHandler.java` — 400/405 query-param handlers.
- `src/main/resources/application.properties` — `spring.data.web.pageable.max-page-size=100`.
- `src/test/resources/application.properties` — same pagination cap.
- `test/AuthenticationIntegrationTest.java` — fixed 404 target test.
- `test/DonationLifecycleIntegrationTest.java` — fixed optimistic-lock test.
- `AGENTS.md` — this record.

## Known limitations / revisit points

1. RECEIVER cannot open `GET /api/donations/{id}` detail (still DONOR/ADMIN
   only per Phase 6 matrix); search results carry full details so discovery
   works, but single-donation navigation for receivers needs an explicit
   decision in a later phase.
2. Text search uses unescaped `%`/`_` LIKE wildcards and `lower()` (case
   folding depends on the DB collation); fine for MVP ASCII, revisit for
   non-ASCII or escaping.
3. Search results map the LAZY donor/receiver (and a fixed empty photo list)
   per row; on large pages this is N+1. Phase 9's matching queries should
   revisit fetch strategy (join fetch / entity graph) — no EAGER added here.
4. `IllegalArgumentException → 400` is a pragmatic catch-all at the web layer;
   the Phase 11 error system should narrow this down.
5. Within-transaction timestamp ties mean search ordering is only
   deterministic when `createdAt` differs; tests assert membership/counts,
   not exact order.
6. The `lower(city)` / `lower(title/description)` predicates cannot use the
   Phase 2 plain btree indexes. Revisit with an expression index or
   full-text search if discovery volume grows.

---

# Phase 13 Build Record — OpenAPI / Swagger

Completed 2026-08-10. Live API documentation via springdoc-openapi. No
business logic was added.

## Dependencies added (pom.xml)

- `org.springdoc:springdoc-openapi-starter-webmvc-ui` version `3.1.0`
  (managed via the `springdoc.version` property). 3.1.0 is the release line
  that supports Spring Boot 4.1 + Jackson 3.

## What was added

- `config/OpenApiConfig` — `OpenAPI` bean with API title/description/version,
  contact and license, plus a global HTTP `bearerAuth` JWT security scheme so
  Swagger UI's **Authorize** button works. No `@Tag`/`@Operation` annotations
  were added; springdoc derives the schema from the controllers/DTOs.
- `security/SecurityConfig` — permits the documentation routes
  (`/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`,
  `/swagger-resources/**`) without authentication.

## Endpoints

- `GET /v3/api-docs` — live OpenAPI JSON (public).
- `GET /swagger-ui/index.html` (and `/swagger-ui.html`) — interactive UI
  (public).

## Jackson 2/3 verification

The project uses Jackson 3 (`tools.jackson`, managed by Spring Boot 4) while
jjwt-jackson pulls Jackson 2 (`com.fasterxml`) onto the classpath. Known
springdoc+Jackson 2/3 conflict reports (e.g. springdoc issue #3157/#3200) were
verified empirically: springdoc 3.1.0 starts cleanly, generates the full spec
including the Phase 10 endpoints, and serves Swagger UI without the
PolymorphicModelConverter exception. No `spring-boot-jackson2` shim was needed.
This was confirmed by the tests below.

## Tests executed and result

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 236 tests, 0 failures, 0 errors.

New `OpenApiIntegrationTest` (3 tests): `GET /v3/api-docs` returns the
expected title and contains the auth, admin match-approval and admin
transaction-completion paths plus the `bearerAuth` security scheme; Swagger UI
is served; the docs are public without a token. All Phase 1–12 tests still
pass.

## Files created

- `config/OpenApiConfig.java`
- `test/OpenApiIntegrationTest.java`

## Files modified

- `pom.xml` — springdoc dependency + `springdoc.version` property.
- `security/SecurityConfig.java` — permit the documentation routes.
- `AGENTS.md` — this record.

---

# Phase 14 Build Record — Demo & Production Preparation

Completed 2026-08-10. Deployment-readiness and hackathon demo support. No
business logic was added.

## What was added

### Admin seeding (`config/AdminSeeder`)

- `CommandLineRunner` that creates an ADMIN account on startup when BOTH
  `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set and no user with that email
  exists. Email is trimmed/lowercased; password is BCrypt-hashed with the
  application encoder; account name is "Administrator"; role ADMIN, active.
- Fail-fast guardrails: setting only one of the two variables, or a password
  shorter than 8 characters, throws `IllegalStateException` at startup so a
  misconfigured deployment never silently runs without an admin.
- Skipped entirely (no DB writes) when neither variable is set, which is the
  default.

### Restricted CORS (`config/CorsConfig`)

- `WebMvcConfigurer` driven by `CORS_ALLOWED_ORIGINS` (comma-separated list).
  When set, `/api/**` allows only those origins (GET/POST/PATCH/DELETE/OPTIONS,
  `Authorization`/`Content-Type` headers, credentials allowed). When unset,
  no CORS headers are emitted at all, so browser cross-origin callers are
  blocked by default (preflight → 403).
- `SecurityConfig` adds `.cors(Customizer.withDefaults())` so Spring Security
  honors the MVC CORS config for preflight requests before authorization.

### Health endpoint (`controller/HealthController`)

- `GET /api/health` — public; runs `SELECT 1` against the database. Returns
  200 `{"status":"UP","database":"UP"}` or 503 `DOWN` when the DB is
  unreachable. Used by Render's `healthCheckPath`.

### Deployment config

- `render.yaml` — added `healthCheckPath: /api/health` and (sync:false)
  `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `CORS_ALLOWED_ORIGINS` variables to be
  filled in from the Render dashboard.
- `.env.example` — documented the new variables.
- `README.md` — features, env-var table, API overview (health + docs), phase
  table (0–14 complete) and deployment steps updated.

## Environment variables (new)

- `ADMIN_EMAIL` / `ADMIN_PASSWORD` — both-or-neither; seeds an admin on boot.
- `CORS_ALLOWED_ORIGINS` — comma-separated allowed origins for `/api/**`.

## Tests executed and result

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test`

Result: BUILD SUCCESS — 245 tests, 0 failures, 0 errors.

New tests:
- `config/AdminSeederTest` (6, unit/Mockito) — seeds with correct email
  normalization/role/hash; skips when credentials absent; skips when the
  admin already exists; fails fast for email-without-password,
  password-without-email and password < 8 chars.
- `CorsIntegrationTest` (2) — preflight from an allowed origin returns 200
  with `Access-Control-Allow-Origin`; preflight from a disallowed origin is
  403.
- `HealthEndpointTest` (1) — `GET /api/health` is public and reports
  UP/UP.
- All Phase 1–13 tests still pass.

## Files created

- `config/AdminSeeder.java`
- `config/CorsConfig.java`
- `controller/HealthController.java`
- tests: `config/AdminSeederTest.java`, `CorsIntegrationTest.java`,
  `HealthEndpointTest.java`

## Files modified

- `security/SecurityConfig.java` — `.cors(withDefaults())`, public
  `/api/health`.
- `src/main/resources/application.properties` — `admin.email`,
  `admin.password`, `cors.allowed-origins`.
- `render.yaml`, `.env.example`, `README.md`, `AGENTS.md`.

## Documentation debt (from the interrupted Phase 9–12 session)

AGENTS.md has no build records for Phase 9 (Matching Engine), Phase 10 (Admin
Match Approval & Transactions), Phase 11 (Global Exception Handling) or Phase
12 (Testing & Security Review), even though the current-phase section now
declares them complete and Phase 13's test run confirms 236 tests were already
passing. Backfill these records from the actual code before the next phase.

---

# Agent Rules

1. Read AGENTS.md before starting any phase.
2. Follow the current phase only.
3. Do not silently implement future-phase functionality.
4. Do not expose entities directly through APIs.
5. Keep business logic in services.
6. Keep repositories focused on persistence.
7. Use DTOs at API boundaries.
8. Do not add unnecessary dependencies.
9. Run tests after implementation phases.
10. Update AGENTS.md after completing each phase.
11. Never commit secrets.
12. Do not implement stretch features before MVP requirements are functional.
13. If a requirement conflicts with this document, stop and identify the conflict rather than silently choosing a behavior.
14. Clearly report assumptions and business decisions.