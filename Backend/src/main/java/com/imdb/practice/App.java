package com.imdb.practice;

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
import java.sql.*;
import java.time.Duration;
import java.util.*;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final int DEFAULT_PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "3001"));
    private static final int ROW_LIMIT = 10;
    private static final int TIMEOUT_MS = (int) Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("QUERY_TIMEOUT_SECONDS", "10"))).toMillis();

    public static void main(String[] args) throws Exception {
        // Ensure JDBC sends a server-accepted timezone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        ProblemStore.init();
        Db.initPool();
        HttpServer server = HttpServer.create(new InetSocketAddress(DEFAULT_PORT), 0);
        // Single context for API, route inside
        server.createContext("/api", new ApiHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Backend running on port " + DEFAULT_PORT);
    }

    private static List<Map<String, Object>> readTableSchema(String table) throws Exception {
        String sql = "SELECT column_name AS name, data_type AS type, is_nullable FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> out = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", rs.getString("name"));
                    row.put("type", rs.getString("type"));
                    row.put("nullable", "YES".equalsIgnoreCase(rs.getString("is_nullable")));
                    out.add(row);
                }
                return out;
            }
        }
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

                if (path.equals("/api/schema") && "GET".equalsIgnoreCase(method)) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query == null || !query.contains("tables=")) {
                        writeJson(exchange, 400, Map.of("error", "tables query param required"));
                        return;
                    }
                    String tablesParam = Arrays.stream(query.split("&"))
                            .filter(s -> s.startsWith("tables="))
                            .findFirst().orElse("tables=");
                    String[] tables = tablesParam.substring("tables=".length()).split(",");
                    Map<String, Object> schemas = new LinkedHashMap<>();
                    for (String t : tables) {
                        String tn = t.trim();
                        if (tn.isEmpty()) continue;
                        try {
                            schemas.put(tn, readTableSchema(tn));
                        } catch (Exception e) {
                            schemas.put(tn, List.of(Map.of("error", e.getMessage())));
                        }
                    }
                    writeJson(exchange, 200, Map.of("schemas", schemas));
                    return;
                }

                if ("/api/health".equals(path) && "GET".equalsIgnoreCase(method)) {
                    writeJson(exchange, 200, Map.of("status", "ok"));
                    return;
                }

                if (path.equals("/api/problems") && "GET".equalsIgnoreCase(method)) {
                    writeJson(exchange, 200, ProblemStore.listPublic());
                    return;
                }

                if (path.startsWith("/api/problems/") && path.endsWith("/hint") && "GET".equalsIgnoreCase(method)) {
                    String id = path.substring("/api/problems/".length(), path.length() - "/hint".length());
                    Problem p = ProblemStore.get(id);
                    if (p == null) {
                        writeJson(exchange, 404, Map.of("error", "Problem not found"));
                        return;
                    }
                    List<String> verbs = extractVerbs(p.solutionSql);
                    writeJson(exchange, 200, Map.of("verbs", verbs));
                    return;
                }

                if (path.startsWith("/api/problems/") && path.endsWith("/next") && "GET".equalsIgnoreCase(method)) {
                    String id = path.substring("/api/problems/".length(), path.length() - "/next".length());
                    String next = ProblemStore.nextId(id);
                    writeJson(exchange, 200, Map.of("nextId", next));
                    return;
                }

                if (path.startsWith("/api/problems/") && path.endsWith("/solution") && "GET".equalsIgnoreCase(method)) {
                    String id = path.substring("/api/problems/".length(), path.length() - "/solution".length());
                    Problem p = ProblemStore.get(id);
                    if (p == null) {
                        writeJson(exchange, 404, Map.of("error", "Problem not found"));
                        return;
                    }
                    writeJson(exchange, 200, Map.of("sql", p.solutionSql));
                    return;
                }

                if (path.startsWith("/api/problems/") && "GET".equalsIgnoreCase(method)) {
                    String id = path.substring("/api/problems/".length());
                    Problem p = ProblemStore.get(id);
                    if (p == null) {
                        writeJson(exchange, 404, Map.of("error", "Problem not found"));
                    } else {
                        writeJson(exchange, 200, p.publicView());
                    }
                    return;
                }

                if (path.equals("/api/run") && "POST".equalsIgnoreCase(method)) {
                    Map<String, Object> body = readJsonBody(exchange);
                    String sql = sanitizeSql(Optional.ofNullable((String) body.get("sql")).orElse(""));
                    if (!isSelectOnly(sql)) {
                        writeJson(exchange, 400, Map.of("error", "Only SELECT queries are allowed"));
                        return;
                    }
                    String wrapped = wrapLimit(sql, ROW_LIMIT);
                    try {
                        List<Map<String, Object>> rows = executeSelect(wrapped);
                        writeJson(exchange, 200, Map.of("rows", rows));
                    } catch (Exception e) {
                        writeJson(exchange, 400, Map.of("error", e.getMessage()));
                    }
                    return;
                }

                if (path.equals("/api/grade") && "POST".equalsIgnoreCase(method)) {
                    Map<String, Object> body = readJsonBody(exchange);
                    String problemId = Optional.ofNullable((String) body.get("problemId")).orElse("");
                    String userSql = sanitizeSql(Optional.ofNullable((String) body.get("sql")).orElse(""));
                    if (problemId.isBlank()) {
                        writeJson(exchange, 400, Map.of("error", "problemId is required"));
                        return;
                    }
                    if (!isSelectOnly(userSql)) {
                        writeJson(exchange, 400, Map.of("error", "Only SELECT queries are allowed"));
                        return;
                    }
                    Problem p = ProblemStore.get(problemId);
                    if (p == null) {
                        writeJson(exchange, 404, Map.of("error", "Problem not found"));
                        return;
                    }
                    try {
                        String userWrapped = wrapLimit(userSql, ROW_LIMIT);
                        String solWrapped = wrapLimit(sanitizeSql(p.solutionSql), ROW_LIMIT);
                        List<Map<String, Object>> userRows = executeSelect(userWrapped);
                        List<Map<String, Object>> solRows = executeSelect(solWrapped);
                        boolean correct;
                        try {
                            correct = normalizeRows(userRows).equals(normalizeRows(solRows));
                        } catch (Exception ex) {
                            correct = false;
                        }
                        writeJson(exchange, 200, Map.of(
                                "correct", correct,
                                "expectedCount", solRows.size(),
                                "actualCount", userRows.size(),
                                "sampleExpected", solRows.stream().limit(5).toArray(),
                                "sampleActual", userRows.stream().limit(5).toArray()
                        ));
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

    private static List<String> extractVerbs(String sql) {
        if (sql == null) return List.of();
        String s = sql.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        // Core clauses
        if (s.contains("select")) out.add("SELECT");
        if (s.contains("distinct")) out.add("DISTINCT");
        if (s.contains(" from ")) out.add("FROM");
        if (s.contains(" join ")) out.add("JOIN");
        if (s.contains(" where ")) out.add("WHERE");
        if (s.contains(" group by ")) out.add("GROUP BY");
        if (s.contains(" having ")) out.add("HAVING");
        if (s.contains(" order by ")) out.add("ORDER BY");
        if (s.contains(" limit ")) out.add("LIMIT");
        // Aggregates
        if (s.contains(" count(")) out.add("COUNT");
        if (s.contains(" sum(")) out.add("SUM");
        if (s.contains(" avg(")) out.add("AVG");
        if (s.contains(" min(")) out.add("MIN");
        if (s.contains(" max(")) out.add("MAX");
        // Subquery heuristic
        if (s.matches(".*\\(\\s*select\\s+.*")) out.add("SUBQUERY");
        // Window functions heuristic
        if (s.contains(" over (")) out.add("WINDOW");
        // Remove duplicates while preserving order
        LinkedHashSet<String> set = new LinkedHashSet<>(out);
        return new ArrayList<>(set);
    }

    private static Set<String> normalizeRows(List<Map<String, Object>> rows) throws Exception {
        List<String> asJson = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            asJson.add(MAPPER.writeValueAsString(r));
        }
        Collections.sort(asJson);
        return new LinkedHashSet<>(asJson);
    }

    private static boolean isSelectOnly(String sql) {
        String s = sql.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return false;
        if (!s.startsWith("select")) return false;
        // basic guards
        String[] blocked = new String[]{";", "--", "/*", "insert", "update", "delete", "create", "drop", "alter", "truncate", "copy", "grant", "revoke", "call"};
        for (String b : blocked) {
            if (s.contains(" " + b + " ") || s.contains(b + " ")) return false;
        }
        return true;
    }

    private static String wrapLimit(String sql, int limit) {
        String s = sql.trim();
        return "select * from (" + s + ") _q limit " + limit;
    }

    private static List<Map<String, Object>> executeSelect(String sql) throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            c.setAutoCommit(true);
            st.setQueryTimeout(Math.max(1, TIMEOUT_MS / 1000));
            // Ensure statement_timeout on server too
            try (Statement st2 = c.createStatement()) {
                st2.execute("SET LOCAL statement_timeout = '" + Math.max(1, TIMEOUT_MS / 1000) + "s'");
            } catch (SQLException ignored) {}

            try (ResultSet rs = st.executeQuery(sql)) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                List<Map<String, Object>> out = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        Object val = rs.getObject(i);
                        String col = md.getColumnLabel(i);
                        // Trim 'tt' prefix from movie/tconst-like identifiers before exposing to clients
                        if (val instanceof String) {
                            String s = (String) val;
                            if (startsWithTtId(col, s)) {
                                // remove leading 'tt' only
                                s = s.replaceFirst("^tt", "");
                                val = s;
                            }
                        }
                        row.put(col, val);
                    }
                    out.add(row);
                }
                return out;
            }
        }
    }

    private static boolean startsWithTtId(String col, String s) {
        // Only apply to known ID columns and strings beginning with 'tt' followed by digits
        String lc = col == null ? "" : col.toLowerCase(Locale.ROOT);
        if (!("tconst".equals(lc) || "parenttconst".equals(lc) || "titleid".equals(lc))) return false;
        return s != null && s.matches("^tt\\d+.*");
    }

    // --- HTTP helpers ---
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

    private static String sanitizeSql(String sql) {
        if (sql == null) return "";
        String s = sql.trim();
        // Remove any trailing semicolons and surrounding whitespace
        while (s.endsWith(";") || s.endsWith(" ") || s.endsWith("\n") || s.endsWith("\r") || s.endsWith("\t")) {
            s = s.replaceFirst("[;\n\r\t ]+$", "");
        }
        return s;
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
