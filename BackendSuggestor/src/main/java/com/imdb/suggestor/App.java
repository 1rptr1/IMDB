package com.imdb.suggestor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.net.URI;
import java.sql.*;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final int DEFAULT_PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "3010"));
    private static final int TIMEOUT_MS = (int) Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("QUERY_TIMEOUT_SECONDS", "10"))).toMillis();

    public static void main(String[] args) throws Exception {
        Db.initPool();
        HttpServer server = HttpServer.create(new InetSocketAddress(DEFAULT_PORT), 0);
        server.createContext("/api", new ApiHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("BackendSuggestor running on port " + DEFAULT_PORT);
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCors(exchange);
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();

                if ("OPTIONS".equalsIgnoreCase(method)) {
                    writeNoContent(exchange);
                    return;
                }

                if ("/api/health".equals(path) && "GET".equalsIgnoreCase(method)) {
                    writeJson(exchange, 200, Map.of("status", "ok"));
                    return;
                }

                // List available genres
                if ("/api/genres".equals(path) && "GET".equalsIgnoreCase(method)) {
                    try (Connection c = Db.get();
                         PreparedStatement ps = c.prepareStatement(
                                 "select distinct trim(g) as genre from (" +
                                         " select unnest(string_to_array(genres, ',')) g from title_basics where genres is not null" +
                                         ") x where g <> '' order by genre asc")) {
                        List<Map<String, Object>> items = new ArrayList<>();
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                items.add(Map.of("name", rs.getString("genre")));
                            }
                        }
                        writeJson(exchange, 200, Map.of("genres", items.stream().map(m -> m.get("name")).toArray()));
                    } catch (Exception e) {
                        writeJson(exchange, 400, Map.of("error", e.getMessage()));
                    }
                    return;
                }

                // Movies listing with filters: genre, actorId (nconst), year, limit, offset
                if ("/api/movies".equals(path) && "GET".equalsIgnoreCase(method)) {
                    Map<String, String> q = parseQuery(exchange.getRequestURI());
                    String genre = Optional.ofNullable(q.get("genre")).orElse("");
                    String actorId = Optional.ofNullable(q.get("actorId")).orElse("");
                    String year = Optional.ofNullable(q.get("year")).orElse("");
                    int limit = parseIntOr(q.get("limit"), 50);
                    int offset = parseIntOr(q.get("offset"), 0);

                    StringBuilder sql = new StringBuilder();
                    sql.append("select tb.tconst as id, tb.\"primaryTitle\" as title, tb.\"startYear\" as year, tb.genres as genres, tr.\"averageRating\" as rating, tr.\"numVotes\" as votes\n");
                    sql.append("from title_basics tb\n");
                    sql.append("left join title_ratings tr on tr.tconst = tb.tconst\n");
                    if (!actorId.isBlank()) {
                        sql.append("join title_principals tp on tp.tconst = tb.tconst and tp.nconst = ?\n");
                    }
                    sql.append("where tb.titleType = 'movie'\n");
                    List<Object> params = new ArrayList<>();
                    if (!genre.isBlank()) {
                        sql.append("  and tb.genres ILIKE ?\n");
                        params.add("%" + genre + "%");
                    }
                    if (!actorId.isBlank()) {
                        params.add(actorId);
                    }
                    if (!year.isBlank()) {
                        sql.append("  and tb.\"startYear\" = ?\n");
                        params.add(year);
                    }
                    sql.append("order by tr.\"averageRating\" desc nulls last, tr.\"numVotes\" desc nulls last\n");
                    sql.append("limit ? offset ?");
                    params.add(limit);
                    params.add(offset);

                    try (Connection c = Db.get(); PreparedStatement ps = prepare(c, sql.toString(), params)) {
                        List<Map<String, Object>> items = new ArrayList<>();
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("id", rs.getString("id"));
                                m.put("title", rs.getString("title"));
                                m.put("year", rs.getObject("year"));
                                m.put("genres", rs.getString("genres"));
                                m.put("rating", rs.getObject("rating"));
                                m.put("votes", rs.getObject("votes"));
                                items.add(m);
                            }
                        }
                        writeJson(exchange, 200, Map.of("items", items));
                    } catch (Exception e) {
                        writeJson(exchange, 400, Map.of("error", e.getMessage()));
                    }
                    return;
                }

                // Movie details with cast
                if (path.startsWith("/api/movies/") && "GET".equalsIgnoreCase(method)) {
                    String id = path.substring("/api/movies/".length());
                    try (Connection c = Db.get()) {
                        Map<String, Object> movie;
                        try (PreparedStatement ps = c.prepareStatement(
                                "select tb.tconst as id, tb.\"primaryTitle\" as title, tb.\"startYear\" as year, tb.genres as genres, tr.\"averageRating\" as rating, tr.\"numVotes\" as votes " +
                                        "from title_basics tb left join title_ratings tr on tr.tconst = tb.tconst where tb.tconst = ?")) {
                            ps.setString(1, id);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (!rs.next()) { writeJson(exchange, 404, Map.of("error", "Movie not found")); return; }
                                movie = new LinkedHashMap<>();
                                movie.put("id", rs.getString("id"));
                                movie.put("title", rs.getString("title"));
                                movie.put("year", rs.getObject("year"));
                                movie.put("genres", rs.getString("genres"));
                                movie.put("rating", rs.getObject("rating"));
                                movie.put("votes", rs.getObject("votes"));
                            }
                        }
                        List<Map<String, Object>> actors = new ArrayList<>();
                        try (PreparedStatement ps = c.prepareStatement(
                                "select nb.nconst as id, nb.\"primaryName\" as name, tp.category as category " +
                                        "from title_principals tp join name_basics nb on nb.nconst = tp.nconst " +
                                        "where tp.tconst = ? and tp.category in ('actor','actress') order by nb.\"primaryName\" asc")) {
                            ps.setString(1, id);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    Map<String, Object> a = new LinkedHashMap<>();
                                    a.put("id", rs.getString("id"));
                                    a.put("name", rs.getString("name"));
                                    a.put("category", rs.getString("category"));
                                    actors.add(a);
                                }
                            }
                        }
                        movie.put("actors", actors);
                        writeJson(exchange, 200, movie);
                    } catch (Exception e) {
                        writeJson(exchange, 400, Map.of("error", e.getMessage()));
                    }
                    return;
                }

                // Actor details with top 10 films by combined score (rating * ln(1+votes))
                if (path.startsWith("/api/actors/") && "GET".equalsIgnoreCase(method)) {
                    String id = path.substring("/api/actors/".length());
                    try (Connection c = Db.get()) {
                        Map<String, Object> actor;
                        try (PreparedStatement ps = c.prepareStatement(
                                "select nb.nconst as id, nb.\"primaryName\" as name, nb.\"birthYear\" as birthYear " +
                                        "from name_basics nb where nb.nconst = ?")) {
                            ps.setString(1, id);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (!rs.next()) { writeJson(exchange, 404, Map.of("error", "Actor not found")); return; }
                                actor = new LinkedHashMap<>();
                                actor.put("id", rs.getString("id"));
                                actor.put("name", rs.getString("name"));
                                actor.put("birthYear", rs.getObject("birthYear"));
                            }
                        }
                        List<Map<String, Object>> films = new ArrayList<>();
                        try (PreparedStatement ps = c.prepareStatement(
                                "select tb.tconst as id, tb.\"primaryTitle\" as title, tb.\"startYear\" as year, tr.\"averageRating\" as rating, tr.\"numVotes\" as votes, " +
                                        " (tr.\"averageRating\" * ln(1 + tr.\"numVotes\")) as score " +
                                        "from title_principals tp " +
                                        " join title_basics tb on tb.tconst = tp.tconst and tb.titleType = 'movie' " +
                                        " left join title_ratings tr on tr.tconst = tb.tconst " +
                                        "where tp.nconst = ? and tp.category in ('actor','actress') " +
                                        "order by score desc nulls last, tr.\"averageRating\" desc nulls last, tr.\"numVotes\" desc nulls last " +
                                        "limit 10")) {
                            ps.setString(1, id);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    Map<String, Object> f = new LinkedHashMap<>();
                                    f.put("id", rs.getString("id"));
                                    f.put("title", rs.getString("title"));
                                    f.put("year", rs.getObject("year"));
                                    f.put("rating", rs.getObject("rating"));
                                    f.put("votes", rs.getObject("votes"));
                                    f.put("score", rs.getObject("score"));
                                    films.add(f);
                                }
                            }
                        }
                        actor.put("topFilms", films);
                        writeJson(exchange, 200, actor);
                    } catch (Exception e) {
                        writeJson(exchange, 400, Map.of("error", e.getMessage()));
                    }
                    return;
                }

                writeJson(exchange, 404, Map.of("error", "Not found"));
            } catch (Exception e) {
                try { writeJson(exchange, 500, Map.of("error", e.getMessage())); } catch (IOException ignored) {}
            }
        }
    }

    private static Map<String, Object> readJsonBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (body == null || body.isBlank()) return new HashMap<>();
            try {
                return MAPPER.readValue(body, Map.class);
            } catch (Exception e) {
                return new HashMap<>();
            }
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        String q = uri.getQuery();
        Map<String, String> out = new HashMap<>();
        if (q == null || q.isBlank()) return out;
        for (String part : q.split("&")) {
            int i = part.indexOf('=');
            if (i <= 0) continue;
            String k = decode(part.substring(0, i));
            String v = decode(part.substring(i + 1));
            out.put(k, v);
        }
        return out;
    }

    private static String decode(String s) {
        try { return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private static int parseIntOr(String s, int def) {
        try { return s == null ? def : Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static PreparedStatement prepare(Connection c, String sql, List<Object> params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        int idx = 1;
        for (Object p : params) {
            ps.setObject(idx++, p);
        }
        return ps;
    }

    private static void addCors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    }

    private static void writeJson(HttpExchange ex, int status, Object obj) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(obj);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void writeNoContent(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }
}
