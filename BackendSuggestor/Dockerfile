FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /usr/src/app
COPY pom.xml .
RUN set -e; for i in 1 2 3; do \
  mvn -B -q -e -DskipTests dependency:go-offline && break || { \
    echo "Maven go-offline failed (attempt $i). Retrying in 5s..."; sleep 5; \
  }; \
done
COPY src ./src
RUN set -e; for i in 1 2 3; do \
  mvn -B -q -DskipTests package && break || { \
    echo "Maven package failed (attempt $i). Retrying in 5s..."; sleep 5; \
  }; \
done

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /usr/src/app/target/imdb-backend-suggestor-0.1.0.jar /app/app.jar
COPY --from=builder /usr/src/app/target/dependency /app/dependency
ENV PORT=3010 \
    QUERY_TIMEOUT_SECONDS=10
EXPOSE 3010
CMD ["java","-cp","/app/app.jar:/app/dependency/*","com.imdb.suggestor.App"]
