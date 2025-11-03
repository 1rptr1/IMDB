# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /usr/src/app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy app JAR and dependency jars copied by maven-dependency-plugin
COPY --from=builder /usr/src/app/target/imdb-practice-backend-0.1.0.jar /app/app.jar
COPY --from=builder /usr/src/app/target/dependency /app/dependency
# Config
ENV PORT=3001 \
    DB_HOST=db \
    DB_PORT=5432 \
    DB_NAME=imdb \
    DB_USER=postgres \
    DB_PASSWORD=postgres \
    QUERY_TIMEOUT_SECONDS=10
EXPOSE 3001
# Use classpath with dependency/* since jar is not shaded
CMD ["java","-cp","/app/app.jar:/app/dependency/*","com.imdb.practice.App"]
