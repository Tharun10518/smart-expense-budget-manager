# Smart Expense & Budget Manager

A production-oriented full-stack application for personal expense tracking, budgeting, financial reporting, and smart spending insights.

## Technology Stack

- **Frontend:** React, Vite, JavaScript, Tailwind CSS, Recharts
- **Backend:** Java, Spring Boot, Spring Web, Spring Data JPA, Spring Security, JWT, Maven
- **Database:** PostgreSQL
- **Delivery direction:** GitHub, Maven, and Docker are currently supported. CI/CD, infrastructure, orchestration, and observability integrations are reserved for later steps.

## Repository Layout

```text
.
├── frontend/    # React and Vite client application
├── backend/     # Spring Boot REST API
├── docker-compose.yml
└── README.md
```

## Current Status

Steps 1 through 17 are implemented. The application includes PostgreSQL-backed JPA entities, REST APIs, BCrypt password hashing, stateless JWT authentication, protected routes, user data isolation, expense/income records, budgets, dashboards, analytics, reports, CSV export, and Docker Compose containerization.

## Prerequisites

- Node.js 20+
- npm 10+
- Java 17+
- Maven 3.9+
- Docker Desktop (optional, for PostgreSQL)

## Getting Started

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite development server runs at `http://localhost:5173` by default.

### Backend

```bash
cd backend
mvn spring-boot:run
```

The Spring Boot API runs at `http://localhost:8080` by default.

Before starting the backend, provide `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET` (at least 32 characters), and optionally `JWT_EXPIRATION` in the environment. See `backend/.env.example` for the variable names.

### PostgreSQL

Set `DATABASE_USERNAME` and `DATABASE_PASSWORD` in your shell or a local root `.env` file before starting the database:

```bash
docker compose up -d postgres
```

The local database is available on port `5432`. The Compose file reads the database name, username, and password from environment variables; it does not contain a database password.

## Docker Compose

Docker Compose runs the frontend, Spring Boot backend, and PostgreSQL database together. Docker Desktop is required.

Create a root `.env` file with local values (never commit it):

```dotenv
POSTGRES_DB=smart_expense
DATABASE_USERNAME=smart_expense_app
DATABASE_PASSWORD=replace-with-a-local-password
JWT_SECRET=replace-with-a-random-secret-at-least-32-characters
JWT_EXPIRATION=86400000
FRONTEND_URL=http://localhost:5173
```

Build and start the stack:

```bash
docker compose up --build -d
```

The application is available at:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:5432` (database service name inside Compose: `postgres`)

Useful commands:

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose down
```

`docker compose down` removes containers and networks but keeps the named `postgres_data` volume. To intentionally reset the database too:

```bash
docker compose down -v
```

The frontend Nginx container serves the SPA with history fallback and proxies `/api` requests to the `backend` Compose service. PostgreSQL data persists in the `postgres_data` named volume.

## Configuration

- Frontend environment template: `frontend/.env.example`
- Backend environment template: `backend/.env.example`
- Backend connection and JWT variables are defined in `backend/.env.example` and consumed by `backend/src/main/resources/application.yml`.

Do not commit real credentials or environment-specific secrets. Use environment variables or a secret manager in deployed environments.

## Quality and Delivery Direction

The frontend and backend are independently buildable so CI can run focused checks in parallel. The backend follows feature-oriented layers (`controller`, `service`, `repository`, `entity`, `dto`) with cross-cutting configuration and security packages reserved for later steps. Container, infrastructure, observability, and deployment automation can be added without coupling the application modules together.

## Planned Development Steps

1. Project structure and baseline configuration
2. Authentication and user management (current)
3. Expense and income management
4. Budgets and usage tracking
5. Financial dashboard, charts, and reports
6. Spending insights, profile management, testing, observability, and delivery automation

