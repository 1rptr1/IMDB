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
# Copy the fat JAR created by maven-assembly-plugin
COPY --from=builder /usr/src/app/target/imdb-backend-suggestor-0.1.0-jar-with-dependencies.jar /app/app.jar
ENV PORT=3010 \
    QUERY_TIMEOUT_SECONDS=10
EXPOSE 3010
# Run the fat JAR
CMD ["java","-jar","/app/app.jar"]
