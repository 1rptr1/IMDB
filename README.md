# IMDB Project

A full-stack IMDb application with movie suggestor, actor search, and SQL practice features.

## Architecture
- **Backend**: Java 17, Maven, PostgreSQL client (JDBC + HikariCP)
- **BackendSuggestor**: Java 17 service for movie recommendations and actor search
- **Frontend**: React + Vite + TypeScript + TailwindCSS
- **Database**: PostgreSQL 15
- **Orchestration**: Docker Compose with multiple deployment options

## Features
- **IMDb SQL Practice**: Interactive SQL queries against IMDb dataset
- **Movie Suggestor**: 
  - Search movies by genre, year, and actor
  - Actor name autocomplete with suggestions
  - Pagination (7 movies per page)
  - Movie details with cast information
  - Actor profiles with top films
- **Docker Hub Deployment**: Pre-built images for easy deployment

## Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+ and npm 9+
- Docker Desktop (optional, for containers)

## Project Structure
- **Frontend/**: Vite React app with TypeScript
- **Backend/**: Java application for SQL practice (Maven)
- **BackendSuggestor/**: Java service for movie recommendations
- **db/**, **db-init/**: Database initialization scripts
- **docker-compose.yml**: Local development setup
- **docker-compose-hub.yml**: Docker Hub deployment setup
- **build-and-push.sh**: Script to build and push Docker images
- Large IMDb datasets (e.g., `*.tsv`, `*.tsv.gz`) are kept in the data folder but are ignored by Git

## Quick Start (Docker Compose)
From the project root `IMDB`:

### Local Development
- **Default compose (builds images locally)**
  ```bash
  docker compose up -d
  ```
- **Docker Hub deployment (pulls pre-built images)**
  ```bash
  docker compose -f docker-compose-hub.yml up -d
  ```
- **Tear down**
  ```bash
  docker compose down
  ```

### Access Points
- **Frontend**: http://localhost:3000
  - IMDb SQL Practice: http://localhost:3000/
  - Movie Suggestor: http://localhost:3000/suggestor
- **Backend API**: http://localhost:3000/api/
- **Suggestor API**: http://localhost:3000/suggestor/api/
- **Database**: localhost:5432 (user: imdb, password: imdb)

## Backend Services

### SQL Practice Backend (Java, Maven)
From `Backend/`:

- **Build**
  ```bash
  mvn clean package
  ```
- **Run**
  ```bash
  mvn exec:java
  ```
  The `pom.xml` sets `com.imdb.practice.App` as the main class.

### Suggestor Backend (Java, Maven)
From `BackendSuggestor/`:

- **Build**
  ```bash
  mvn clean package
  ```
- **Run**
  ```bash
  java -jar target/imdb-backend-suggestor-0.1.0-jar-with-dependencies.jar
  ```

## Frontend (Vite React)
From `Frontend/`:

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

## Docker Hub Deployment

### Build and Push Images
```bash
# Update DOCKER_USERNAME in build-and-push.sh if needed
./build-and-push.sh
```

This will build and push:
- `1rptr1/imdb-frontend:latest`
- `1rptr1/imdb-backend:latest`
- `1rptr1/imdb-suggestor:latest`

### Deploy from Docker Hub
```bash
docker compose -f docker-compose-hub.yml up -d
```

## API Endpoints

### Suggestor API
- **GET** `/suggestor/api/genres` - List all genres
- **GET** `/suggestor/api/movies` - Search movies with filters
  - Query params: `genre`, `actorId` (supports names or IDs), `year`, `limit`, `offset`
- **GET** `/suggestor/api/movies/{id}` - Get movie details with cast
- **GET** `/suggestor/api/actors/search` - Search actors by name
  - Query params: `q` (search query), `limit`
- **GET** `/suggestor/api/actors/{id}` - Get actor details with top films

### Backend API
- **GET** `/api/problems` - List SQL practice problems
- **GET** `/api/problems/{id}` - Get specific problem
- **POST** `/api/execute` - Execute SQL query

## Environment Variables
- Frontend dev variables can go in `Frontend/.env.development`.
- Backend env (DB URL, user, password, etc.) should be supplied via environment variables or Docker Compose.
- `.env` files are git-ignored. Commit example files as `*.env.example` if needed.

## Notes
- Git ignores common build artifacts, node_modules, logs, and large data files (`*.tsv`, `*.tsv.gz`, `*.csv`).
- IMDb datasets are automatically downloaded by the init-downloader service.
- The suggestor supports both actor names (e.g., "Tom Hanks") and actor IDs (e.g., "nm0000158").
- Pagination shows 7 movies per page for optimal performance.
