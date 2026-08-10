# Donation Matching Portal

A REST API that connects donors with receivers for the **Seva Sahayog Foundation**. Donors list donations, receivers post requirements, and the system suggests compatible matches for administrators to review and approve — reducing manual coordination while keeping transparency and administrative control.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Database Setup](#database-setup)
  - [Running Locally](#running-locally)
  - [Environment Variables](#environment-variables)
- [API Overview](#api-overview)
- [Project Phases](#project-phases)
- [Testing](#testing)
- [Deployment](#deployment)
- [License](#license)

## Features

- **Role-based access** — DONOR, RECEIVER, ADMIN with server-enforced authorization (JWT + method security)
- **Donation lifecycle** — submit, approve/reject, match, fulfil, complete
- **Requirement lifecycle** — submit, approve/reject, fulfil
- **Search & filtering** — category, city, free-text, pagination, sorting
- **Matching engine** — explainable rule-based scoring with hard gates and top-5 suggestions
- **Admin workflows** — approve/reject donations, requirements and matches; transaction fulfilment/completion
- **API documentation** — live OpenAPI spec + interactive Swagger UI
- **Demo & production ready** — env-gated admin seeding, restricted CORS, DB health endpoint
- **Security baseline** — BCrypt hashing, JWT auth, ownership checks, bean validation
- **Comprehensive tests** — 245 tests across authentication, authorization, matching, search and persistence

## Tech Stack

| Layer       | Technology                                |
| ----------- | ----------------------------------------- |
| Backend     | Spring Boot 4.1.0, Java 21                |
| Persistence | Spring Data JPA / Hibernate 7.4, PostgreSQL |
| Auth        | Spring Security, JWT (jjwt 0.12.6), BCrypt |
| Build       | Maven (wrapper 3.9.x)                     |
| Tests       | JUnit, MockMvc, Spring Security Test      |
| Docs        | springdoc-openapi (Swagger UI, OpenAPI 3) |

## Architecture

```
Frontend
   ↓
Spring Boot REST API
   ↓
Controller → Service → Repository
   ↓
PostgreSQL
```

Supporting components: security layer (authN/authZ), matching service (rule-based), and future notification/moderation/reporting services. Business rules live in the service layer; entities are never exposed directly through API responses (DTOs at every boundary).

## Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Maven** (or use the included `./mvnw` wrapper)
- **PostgreSQL** (tested against 18.4; any modern version works)

### Database Setup

```bash
brew services start postgresql@18
psql -d postgres -c "CREATE DATABASE donation_matching_portal"
```

### Running Locally

```bash
# 1. Export environment variables (see table below)
export DB_URL=jdbc:postgresql://localhost:5432/donation_matching_portal
export DB_USERNAME=dipanshubhat
export DB_PASSWORD=
export JWT_SECRET=$(openssl rand -base64 48)

# 2. Run tests
./mvnw clean test

# 3. Start the application
./mvnw spring-boot:run
```

The API is then available at `http://localhost:8080`.

### Environment Variables

| Variable           | Required | Default                                                        | Description                        |
| ------------------ | -------- | -------------------------------------------------------------- | ---------------------------------- |
| `DB_URL`           | Yes*     | `jdbc:postgresql://localhost:5432/donation_matching_portal`    | PostgreSQL JDBC URL                |
| `DB_USERNAME`      | Yes*     | `dipanshubhat`                                                 | Database user                      |
| `DB_PASSWORD`      | Yes*     | *(empty — local trust auth)*                                   | Database password                  |
| `JWT_SECRET`       | Yes      | *(none — startup fails if blank)*              | JWT signing key (≥32 bytes)        |
| `JWT_EXPIRATION_MS`| No       | `3600000` (1 hour)                             | Access-token lifetime in ms        |
| `ADMIN_EMAIL`      | No       | *(empty — no admin seeded)*                    | Admin account email (seeded on boot) |
| `ADMIN_PASSWORD`   | No       | *(empty — no admin seeded)*                    | Admin account password (≥8 chars)  |
| `CORS_ALLOWED_ORIGINS` | No   | *(empty — CORS disabled)*                      | Comma-separated allowed origins    |
| `SERVER_PORT`      | No       | `8080`                                         | HTTP port                          |

\* Local defaults are dev conveniences only. Never commit real credentials.

## API Overview

| Group          | Example Endpoints                                              |
| -------------- | -------------------------------------------------------------- |
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/me` |
| Donations      | `POST /api/donations`, `GET /api/donations`, `PATCH /api/donations/{id}` |
| Requirements   | `POST /api/requirements`, `GET /api/requirements`, `PATCH /api/requirements/{id}` |
| Matching       | `GET /api/matches/...`, `POST /api/admin/matches/...`          |
| Administration | `GET /api/admin/queue`, approve/reject donations, requirements & matches, transactions |
| Operations     | `GET /api/health` (public), Swagger UI, `GET /v3/api-docs`     |

Interactive docs: `http://localhost:8080/swagger-ui/index.html` (use the **Authorize** button with a JWT to call protected endpoints). Every endpoint except `/api/auth/*`, `/api/health` and the docs paths requires an `Authorization: Bearer <token>` header.

> Note: the OpenAPI spec was written as a Phase 0 contract and evolves with each phase. `openapi.yaml` and the `openapi.html` rendered view are kept in sync at each phase boundary; springdoc additionally serves a live spec at `/v3/api-docs`.

## Project Phases

The project is built in incremental, test-verified phases (documented in `AGENTS.md`):

| Phase | Scope                                   | Status    |
| ----- | --------------------------------------- | --------- |
| 0     | Requirements & architecture             | Complete  |
| 1     | Project & database setup                | Complete  |
| 2     | Domain models / JPA entities            | Complete  |
| 3     | Repository layer                        | Complete  |
| 4     | DTOs & validation                       | Complete  |
| 5     | Authentication (JWT, BCrypt)            | Complete  |
| 6     | Authorization (RBAC, ownership)         | Complete  |
| 7     | Donation & requirement business logic   | Complete  |
| 8     | Search & filtering                      | Complete  |
| 9     | Matching engine                         | Complete  |
| 10    | Admin match approval & transactions   | Complete  |
| 11    | Global exception handling             | Complete  |
| 12    | Testing & security review             | Complete  |
| 13    | OpenAPI / Swagger                     | Complete  |
| 14    | Demo & production preparation         | Complete  |

## Testing

```bash
JAVA_HOME=/path/to/java-21 ./mvnw clean test
```

The suite covers authentication, role authorization, ownership, donation lifecycle, search/filtering, matching, transaction completion, admin approval, API documentation, health/CORS/seeders, DTO validation/serialization, repository constraints and entity persistence. Tests run against your local PostgreSQL instance (`@Transactional` tests roll back, leaving the database clean).

## Deployment

Deployment is configured via `render.yaml` (Render Blueprint) and `Dockerfile`:
1. Push the repository to GitHub
2. In Render: **New → Blueprint** → connect your GitHub repo
3. Render provisions a web service + free PostgreSQL, injecting `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` and a generated `JWT_SECRET`
4. Set `ADMIN_EMAIL` / `ADMIN_PASSWORD` (seeds an admin account on first boot) and `CORS_ALLOWED_ORIGINS` (frontend origin) in the Render dashboard
5. First deploy runs the Docker multi-stage build (Maven → JRE image); Render polls `GET /api/health` for readiness

The JDBC URL is composed in `application.properties` from `DB_HOST`/`DB_PORT`/`DB_NAME` (or an explicit `DB_URL`, which wins). The app binds to Render's injected `PORT`.

## Demo

A step-by-step hackathon runbook (seed data, exact `curl` calls, expected
outputs) lives in [`docs/demo-walkthrough.md`](docs/demo-walkthrough.md).

## License

MIT © [dipanshubhat810](LICENSE)
