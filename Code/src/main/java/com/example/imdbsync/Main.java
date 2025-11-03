package com.example.imdbsync;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws Exception {
        String dataDir = getenvOr("IMDB_DATA_DIR", "d:/IMDB");
        System.out.println("IMDb data dir: " + dataDir);

        System.out.println("Waiting for DB...");
        Db.waitForDb();
        System.out.println("DB is reachable. Connecting to imdb...");
        try (Connection conn = Db.connectImdb()) {
            System.out.println("Connected. Running loader...");
            new ImdbLoader(Paths.get(dataDir)).runOnce(conn);
            System.out.println("Done.");
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String getenvOr(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
