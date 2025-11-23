# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /usr/src/app
COPY pom.xml .
# Retry downloading dependencies to handle transient network issues
RUN set -e; for i in 1 2 3; do \
  mvn -B -q -e -DskipTests dependency:go-offline && break || { \
    echo "Maven go-offline failed (attempt $i). Retrying in 5s..."; sleep 5; \
  }; \
done
COPY src ./src
# Retry packaging as well
RUN set -e; for i in 1 2 3; do \
  mvn -B -q -DskipTests package && break || { \
    echo "Maven package failed (attempt $i). Retrying in 5s..."; sleep 5; \
  }; \
done

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the fat JAR created by maven-assembly-plugin
COPY --from=builder /usr/src/app/target/imdb-backend-1.0-SNAPSHOT-jar-with-dependencies.jar /app/app.jar
# Config
ENV PORT=3001 \
    DB_HOST=db \
    DB_PORT=5432 \
    DB_NAME=imdb \
    DB_USER=imdb \
    DB_PASSWORD=imdb \
    QUERY_TIMEOUT_SECONDS=10
EXPOSE 3001
# Run the fat JAR
CMD ["java","-jar","/app/app.jar"]
