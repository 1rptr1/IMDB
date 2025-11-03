# IMDb Docker + Java Sync

This project runs a Postgres database in Docker with two databases: `imdb` and `userdb`. A Java CLI loads IMDb TSV.GZ files into the `imdb` database when their file sizes change.

## Prerequisites
- Docker Desktop
- Java 17+
- Maven 3.8+
- IMDb `.tsv.gz` files present in `d:/IMDB` (default) or set `IMDB_DATA_DIR` env var.

## Setup
1. Start Postgres:
   ```bash
   docker compose up -d
   ```
2. Build the app:
   ```bash
   mvn -q -DskipTests package
   ```
3. Run the sync:
   ```bash
   set DB_HOST=localhost
   set DB_PORT=5432
   set DB_NAME=imdb
   set DB_USER=postgres
   set DB_PASSWORD=postgres
   set IMDB_DATA_DIR=d:/IMDB
   java -jar target/imdb-sync-0.1.0-shaded.jar
   ```

The app will load `title.basics.tsv.gz` and `title.ratings.tsv.gz` when sizes change, updating the `imdb_ingest_state` tracker.

## Notes
- Extend `ImdbLoader` to handle more files (akas, crew, principals, episodes) similarly.
- `userdb` is provisioned and ready for your application data; it's independent of `imdb`.
