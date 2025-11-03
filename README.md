# IMDB Project

A split Frontend/Backend project with Docker support.

- **Backend**: Java 17, Maven, PostgreSQL client (JDBC + HikariCP)
- **Frontend**: React + Vite + TypeScript + TailwindCSS
- **Orchestration**: Docker Compose (`docker-compose.yml`, `.local`, `.prod` variants)

## Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+ and npm 9+
- Docker Desktop (optional, for containers)

## Project Structure
- **Frontend/**: Vite React app
- **Backend/**: Java application (Maven)
- **db/**, **db-init/**: database mounts/init (if used by Compose)
- **docker-compose.yml** and variants: local/prod setups
- Large IMDb datasets (e.g., `*.tsv`, `*.tsv.gz`) are kept in the repo folder but are ignored by Git

## Quick Start (Docker Compose)
From the project root `IMDB`:

- **Default compose**
  ```bash
  docker compose up -d
  ```
- **Local overrides**
  ```bash
  docker compose -f docker-compose.yml -f docker-compose.local.yml up -d
  ```
- **Production**
  ```bash
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
  ```
- **Tear down**
  ```bash
  docker compose down
  ```

## Backend (Java, Maven)
From `d:\IMDB\Backend`:

- **Build**
  ```bash
  mvn clean package
  ```
- **Run (exec plugin)**
  ```bash
  mvn exec:java
  ```
  The `pom.xml` sets `com.imdb.practice.App` as the main class.

## Frontend (Vite React)
From `d:\IMDB\Frontend`:

- **Install**
  ```bash
  npm ci
  ```
- **Dev server** (default http://localhost:5173)
  ```bash
  npm run dev
  ```
- **Build**
  ```bash
  npm run build
  ```
- **Preview** (serves dist on port 5173)
  ```bash
  npm run preview
  ```

## Environment Variables
- Frontend dev variables can go in `Frontend/.env.development`.
- Backend env (DB URL, user, password, etc.) should be supplied via environment variables or Docker Compose.
- `.env` files are git-ignored. Commit example files as `*.env.example` if needed.

## Notes
- Git ignores common build artifacts, node_modules, logs, and large data files (`*.tsv`, `*.tsv.gz`, `*.csv`).
- Place IMDb datasets (e.g., `title.basics.tsv.gz`) in the project root or a data folder referenced by your app/compose.
