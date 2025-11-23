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
  # Clean up existing containers and data (IMPORTANT)
  docker compose -f docker-compose-hub.yml down -v
  rm -rf data
  mkdir data
  
  # Deploy using Docker Hub images (permissions set automatically)
  docker compose -f docker-compose-hub.yml up -d
  ```
- **Tear down**
  ```bash
  docker compose down
  # For complete cleanup (including data)
  docker compose down -v
  rm -rf data
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

## Cloud Deployment

### Oracle Cloud Infrastructure (OCI) Deployment

#### Prerequisites
- OCI account with appropriate permissions
- OCI CLI installed and configured
- Docker installed locally
- Docker Hub account with pushed images

#### Step 1: Create OCI Compute Instance
```bash
# Create a VM instance with Docker
oci compute instance launch \
  --compartment-id YOUR_COMPARTMENT_ID \
  --availability-domain YOUR_AD \
  --subnet-id YOUR_SUBNET_ID \
  --display-name "imdb-app" \
  --shape "VM.Standard2.1" \
  --image-id "ocid1.image.oc1.phx.aaaaaaaaoqj42sakaow2csvgur6jctz3u7d26l43czb3qb24mgre3w6a4a" \
  --ssh-authorized-keys-file ~/.ssh/id_rsa.pub \
  --assign-public-ip true
```

#### Step 2: Connect to Instance and Setup Docker
```bash
# SSH into the instance
ssh -i ~/.ssh/id_rsa opc@<PUBLIC_IP>

# Install Docker
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker opc

# Logout and SSH back in for group changes to take effect
exit
ssh -i ~/.ssh/id_rsa opc@<PUBLIC_IP>
```

#### Step 3: Deploy the Application
```bash
# Clone the repository
git clone https://github.com/1rptr1/IMDB.git
cd IMDB

# Clean up any existing containers and data (IMPORTANT)
docker compose -f docker-compose-hub.yml down -v
rm -rf data
mkdir data

# Deploy using Docker Hub images (permissions set automatically)
docker compose -f docker-compose-hub.yml up -d

# Check status
docker ps
```

#### Step 4: Configure Security Lists
```bash
# Open necessary ports in OCI Console
# Port 80 (HTTP) - for frontend
# Port 443 (HTTPS) - for SSL (optional)
# Port 22 (SSH) - for management
```

#### Step 5: Setup Nginx Reverse Proxy (Optional)
```bash
# Create nginx.conf for production
cat > nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream frontend {
        server frontend:80;
    }
    
    upstream backend {
        server backend:8080;
    }
    
    upstream suggestor {
        server suggestor:9000;
    }
    
    server {
        listen 80;
        server_name your-domain.com;
        
        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        location /suggestor/api/ {
            proxy_pass http://suggestor/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        location / {
            proxy_pass http://frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
EOF

# Add nginx service to docker-compose-hub.yml
```

### Alternative Cloud Platforms

#### AWS ECS Deployment
```bash
# Create ECS Cluster
aws ecs create-cluster --cluster-name imdb-cluster

# Push images to ECR
aws ecr create-repository --repository-name imdb-frontend
aws ecr create-repository --repository-name imdb-backend
aws ecr create-repository --repository-name imdb-suggestor

# Tag and push images
docker tag 1rptr1/imdb-frontend:latest <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/imdb-frontend:latest
docker push <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/imdb-frontend:latest
```

#### Google Cloud Run Deployment
```bash
# Enable Cloud Run API
gcloud services enable run.googleapis.com

# Deploy services
gcloud run deploy imdb-frontend --image 1rptr1/imdb-frontend:latest --platform managed
gcloud run deploy imdb-backend --image 1rptr1/imdb-backend:latest --platform managed
gcloud run deploy imdb-suggestor --image 1rptr1/imdb-suggestor:latest --platform managed
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

## Next Steps and Future Enhancements

### Performance & Scalability
- **Database Optimization**: Add proper indexing for frequently queried columns
- **Caching Layer**: Implement Redis caching for movie and actor data
- **Load Balancing**: Add multiple instances behind a load balancer
- **CDN Integration**: Serve static assets via Cloud CDN

### Features
- **User Authentication**: Add login/logout with JWT tokens
- **User Profiles**: Save favorite movies and watchlists
- **Movie Ratings**: Allow users to rate and review movies
- **Advanced Search**: Filter by director, runtime, language
- **Recommendation Engine**: ML-based movie recommendations
- **Real-time Updates**: WebSocket for live data updates

### Security & Monitoring
- **SSL/TLS**: Implement HTTPS with Let's Encrypt certificates
- **API Rate Limiting**: Prevent abuse with rate limiting
- **Logging**: Centralized logging with ELK stack
- **Monitoring**: Add Prometheus metrics and Grafana dashboards
- **Health Checks**: Implement comprehensive health check endpoints

### Deployment & DevOps
- **CI/CD Pipeline**: GitHub Actions for automated testing and deployment
- **Infrastructure as Code**: Terraform templates for OCI resources
- **Blue-Green Deployment**: Zero-downtime deployments
- **Auto-scaling**: Configure auto-scaling based on traffic
- **Backup Strategy**: Automated database backups and recovery

### Data Management
- **Data Updates**: Scheduled updates for fresh IMDb data
- **Data Validation**: Ensure data integrity and quality
- **Analytics Dashboard**: Usage statistics and insights
- **Export Features**: Allow users to export data

## Notes
- Git ignores common build artifacts, node_modules, logs, and large data files (`*.tsv`, `*.tsv.gz`, `*.csv`).
- IMDb datasets are automatically downloaded by the init-downloader service.
- **Automated Permissions**: The `fix-permissions` service automatically sets proper permissions on the data directory.
- The suggestor supports both actor names (e.g., "Tom Hanks") and actor IDs (e.g., "nm0000158").
- Pagination shows 7 movies per page for optimal performance.
- For production deployment, consider using environment-specific configurations and secrets management.
