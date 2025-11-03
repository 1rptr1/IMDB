package com.imdb.practice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ProblemStore {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, Problem> BY_ID = new LinkedHashMap<>();

    public static void init() throws Exception {
        try (InputStream in = ProblemStore.class.getResourceAsStream("/problems.json")) {
            if (in == null) throw new IllegalStateException("problems.json not found on classpath");
            List<Problem> problems = MAPPER.readValue(in, new TypeReference<List<Problem>>(){});
            for (Problem p : problems) {
                BY_ID.put(p.id, p);
            }
        }
    }

    public static List<Map<String, Object>> listPublic() {
        return BY_ID.values().stream().map(Problem::publicView).collect(Collectors.toList());
    }

    public static Problem get(String id) { return BY_ID.get(id); }

    public static List<String> orderedIds() { return new ArrayList<>(BY_ID.keySet()); }

    public static String nextId(String id) {
        List<String> ids = orderedIds();
        int idx = ids.indexOf(id);
        if (idx == -1 || idx+1 >= ids.size()) return null;
        return ids.get(idx+1);
    }
}
