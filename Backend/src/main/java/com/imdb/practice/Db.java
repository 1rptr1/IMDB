package com.imdb.practice;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Db {
    private static HikariDataSource ds;

    public static void initPool() {
        HikariConfig cfg = new HikariConfig();
        String host = envOr("DB_HOST", "localhost");
        String port = envOr("DB_PORT", "5432");
        String db = envOr("DB_NAME", "imdb");
        String user = envOr("DB_USER", "postgres");
        String pass = envOr("DB_PASSWORD", "postgres");
        String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        cfg.setJdbcUrl(jdbc);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(10_000);
        cfg.setIdleTimeout(60_000);
        cfg.setMaxLifetime(30 * 60_000);
        cfg.setConnectionInitSql("SET TIME ZONE 'UTC'");
        cfg.addDataSourceProperty("options", "-c TimeZone=UTC");
        // Add PostgreSQL-specific properties
        cfg.addDataSourceProperty("ssl", "false");
        cfg.addDataSourceProperty("sslmode", "disable");
        cfg.addDataSourceProperty("prepareThreshold", "0"); // Disable server-side prepared statements
        ds = new HikariDataSource(cfg);
    }

    private static String envOr(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}