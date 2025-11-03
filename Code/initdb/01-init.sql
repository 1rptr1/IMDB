-- Create two databases: imdb (for dataset) and userdb (for user-specific data)
CREATE DATABASE imdb;
CREATE DATABASE userdb;

\connect imdb
CREATE SCHEMA IF NOT EXISTS public;

-- State table to track ingested IMDb source sizes
CREATE TABLE IF NOT EXISTS imdb_ingest_state (
    filename text PRIMARY KEY,
    size bigint NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);
