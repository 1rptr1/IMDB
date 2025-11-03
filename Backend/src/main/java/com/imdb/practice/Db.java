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
        ds = new HikariDataSource(cfg);
    }

    public static Connection get() throws SQLException { return ds.getConnection(); }

    private static String envOr(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
