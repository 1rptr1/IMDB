package com.example.imdbsync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

public class Db {
    public static Connection connectImdb() throws SQLException {
        return connectTo(envOr("DB_NAME", "imdb"));
    }

    public static Connection connectTo(String db) throws SQLException {
        String host = envOr("DB_HOST", "localhost");
        int port = Integer.parseInt(envOr("DB_PORT", "5432"));
        String user = envOr("DB_USER", "postgres");
        String pass = envOr("DB_PASSWORD", "postgres");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        return DriverManager.getConnection(url, user, pass);
    }

    public static void waitForDb() {
        long start = System.currentTimeMillis();
        long timeoutMs = Duration.ofMinutes(5).toMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try (Connection c = connectTo("postgres")) {
                return;
            } catch (SQLException e) {
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new RuntimeException("Database not reachable within timeout");
    }

    private static String envOr(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
