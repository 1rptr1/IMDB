package com.example.imdbsync;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

public class ImdbLoader {
    private final Path dataDir;

    public ImdbLoader(Path dataDir) { this.dataDir = dataDir; }

    public void runOnce(Connection conn) throws Exception {
        ensureStateTable(conn);
        ensureSchemas(conn);

        Map<String, String> files = new HashMap<>();
        files.put("title.basics.tsv.gz", "basics");
        files.put("title.ratings.tsv.gz", "ratings");
        files.put("name.basics.tsv.gz", "name_basics");
        files.put("title.akas.tsv.gz", "akas");
        files.put("title.crew.tsv.gz", "crew");
        files.put("title.principals.tsv.gz", "principals");
        files.put("title.episode.tsv.gz", "episode");

        for (Map.Entry<String, String> e : files.entrySet()) {
            String file = e.getKey();
            String table = e.getValue();
            Path p = dataDir.resolve(file);
            if (!Files.exists(p)) {
                System.out.println("Missing file: " + p);
                continue;
            }
            long size = Files.size(p);
            Long prev = getPrevSize(conn, file);
            if (prev == null || prev != size) {
                System.out.printf("Updating %s (size %d, prev %s)\n", file, size, prev);
                loadInto(conn, table, p);
                upsertSize(conn, file, size);
            } else {
                System.out.printf("No change for %s (size %d)\n", file, size);
            }
        }
    }

    private void ensureStateTable(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS imdb_ingest_state (\n" +
                    "  filename text PRIMARY KEY,\n" +
                    "  size bigint NOT NULL,\n" +
                    "  updated_at timestamptz NOT NULL DEFAULT now()\n" +
                    ")");
        }
    }

    private void ensureSchemas(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS basics (\n" +
                    "  tconst text PRIMARY KEY,\n" +
                    "  titleType text,\n" +
                    "  primaryTitle text,\n" +
                    "  originalTitle text,\n" +
                    "  isAdult boolean,\n" +
                    "  startYear integer,\n" +
                    "  endYear integer,\n" +
                    "  runtimeMinutes integer,\n" +
                    "  genres text\n" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS ratings (\n" +
                    "  tconst text PRIMARY KEY,\n" +
                    "  averageRating double precision,\n" +
                    "  numVotes integer\n" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS name_basics (\n" +
                    "  nconst text PRIMARY KEY,\n" +
                    "  primaryName text,\n" +
                    "  birthYear integer,\n" +
                    "  deathYear integer,\n" +
                    "  primaryProfession text,\n" +
                    "  knownForTitles text\n" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS akas (\n" +
                    "  titleId text,\n" +
                    "  ordering integer,\n" +
                    "  title text,\n" +
                    "  region text,\n" +
                    "  language text,\n" +
                    "  types text,\n" +
                    "  attributes text,\n" +
                    "  isOriginalTitle boolean\n" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS crew (\n" +
                    "  tconst text PRIMARY KEY,\n" +
                    "  directors text,\n" +
                    "  writers text\n" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS principals (\n" +
                    "  tconst text,\n" +
                    "  ordering integer,\n" +
                    "  nconst text,\n" +
                    "  category text,\n" +
                    "  job text,\n" +
                    "  characters text\n" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS episode (\n" +
                    "  tconst text PRIMARY KEY,\n" +
                    "  parentTconst text,\n" +
                    "  seasonNumber integer,\n" +
                    "  episodeNumber integer\n" +
                    ")");
        }
    }

    private void loadInto(Connection c, String table, Path gzPath) throws Exception {
        String copySql;
        if ("basics".equals(table)) {
            copySql = "COPY basics (tconst, titleType, primaryTitle, originalTitle, isAdult, startYear, endYear, runtimeMinutes, genres) " +
                    "FROM STDIN WITH (FORMAT csv, DELIMITER E'\t', HEADER true, NULL '\\N', QUOTE E'\b')";
        } else if ("ratings".equals(table)) {
            copySql = "COPY ratings (tconst, averageRating, numVotes) " +
                    "FROM STDIN WITH (FORMAT csv, DELIMITER E'\t', HEADER true, NULL '\\N', QUOTE E'\b')";
        } else if ("name_basics".equals(table)) {
            copySql = "COPY name_basics (nconst, primaryName, birthYear, deathYear, primaryProfession, knownForTitles) " +
                    "FROM STDIN WITH (FORMAT csv, DELIMITER E'\t', HEADER true, NULL '\\N', QUOTE E'\b')";
        } else if ("akas".equals(table)) {
            copySql = "COPY akas (titleId, ordering, title, region, language, types, attributes, isOriginalTitle) " +
                    "FROM STDIN WITH (FORMAT csv, DELIMITER E'\t', HEADER true, NULL '\\N', QUOTE E'\b')";
        } else if ("crew".equals(table)) {
            copySql = "COPY crew (tconst, directors, writers) " +
                    "FROM STDIN WITH (FORMAT csv, DELIMITER E'\t', HEADER true, NULL '\\N', QUOTE E'\b')";
        } else if ("principals".equals(table)) {
            copySql = "COPY principals (tconst, ordering, nconst, category, job, characters) " +
                    "FROM STDIN WITH (FORMAT csv, DELIMITER E'\t', HEADER true, NULL '\\N', QUOTE E'\b')";
        } else if ("episode".equals(table)) {
            copySql = "COPY episode (tconst, parentTconst, seasonNumber, episodeNumber) " +
                    "FROM STDIN WITH (FORMAT csv, DELIMITER E'\t', HEADER true, NULL '\\N', QUOTE E'\b')";
        } else {
            throw new IllegalArgumentException("Unknown table: " + table);
        }

        c.setAutoCommit(false);
        try (Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE " + table);
        }

        PGConnection pg = c.unwrap(PGConnection.class);
        CopyManager cm = pg.getCopyAPI();
        try (InputStream in = new GZIPInputStream(Files.newInputStream(gzPath));
             Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 20)) {
            cm.copyIn(copySql, reader);
        }
        c.commit();
        c.setAutoCommit(true);
    }

    private Long getPrevSize(Connection c, String filename) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT size FROM imdb_ingest_state WHERE filename = ?")) {
            ps.setString(1, filename);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                return null;
            }
        }
    }

    private void upsertSize(Connection c, String filename, long size) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO imdb_ingest_state(filename, size) VALUES(?, ?) " +
                        "ON CONFLICT (filename) DO UPDATE SET size = EXCLUDED.size, updated_at = now()")) {
            ps.setString(1, filename);
            ps.setLong(2, size);
            ps.executeUpdate();
        }
    }
}
