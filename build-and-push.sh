#!/bin/bash

# Set your Docker Hub username
DOCKER_USERNAME="1rptr1"  # Docker Hub username

echo "Building and pushing Docker images to Docker Hub..."

# Build Frontend image
echo "Building Frontend image..."
docker build -t ${DOCKER_USERNAME}/imdb-frontend:latest ./Frontend
if [ $? -eq 0 ]; then
    echo "Frontend build successful, pushing to Docker Hub..."
    docker push ${DOCKER_USERNAME}/imdb-frontend:latest
else
    echo "Frontend build failed!"
    exit 1
fi

# Build Backend image
echo "Building Backend image..."
docker build -t ${DOCKER_USERNAME}/imdb-backend:latest ./Backend
if [ $? -eq 0 ]; then
    echo "Backend build successful, pushing to Docker Hub..."
    docker push ${DOCKER_USERNAME}/imdb-backend:latest
else
    echo "Backend build failed!"
    exit 1
fi

# Build Suggestor image
echo "Building Suggestor image..."
docker build -t ${DOCKER_USERNAME}/imdb-suggestor:latest ./BackendSuggestor
if [ $? -eq 0 ]; then
    echo "Suggestor build successful, pushing to Docker Hub..."
    docker push ${DOCKER_USERNAME}/imdb-suggestor:latest
else
    echo "Suggestor build failed!"
    exit 1
fi

echo "All images built and pushed successfully!"
echo "Don't forget to update docker-compose.yml to use your Docker Hub username: ${DOCKER_USERNAME}"
