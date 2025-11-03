FROM postgres:16-alpine

# Copy a pre-made dump and init script into the image
# Place imdb.dump (custom format from pg_dump -Fc) next to this Dockerfile before building.
COPY imdb.dump /docker-entrypoint-initdb.d/imdb.dump
COPY 00_restore.sh /docker-entrypoint-initdb.d/00_restore.sh
RUN chmod +x /docker-entrypoint-initdb.d/00_restore.sh

ENV POSTGRES_DB=imdb \
    POSTGRES_USER=postgres \
    POSTGRES_PASSWORD=postgres

EXPOSE 5432
