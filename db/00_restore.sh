#!/bin/sh
set -e

DB_NAME=${POSTGRES_DB:-imdb}
DB_USER=${POSTGRES_USER:-postgres}

if [ -f /docker-entrypoint-initdb.d/imdb.dump ]; then
  echo "[init] Found imdb.dump, restoring into database '$DB_NAME'..."
  psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d postgres -c "CREATE DATABASE \"$DB_NAME\" WITH OWNER \"$DB_USER\" TEMPLATE template0 ENCODING 'UTF8';" || true
  pg_restore -v -U "$DB_USER" -d "$DB_NAME" /docker-entrypoint-initdb.d/imdb.dump
  echo "[init] Restore complete."
else
  echo "[init] No imdb.dump present in image."
fi
