package com.imdb.practice;

import java.util.List;
import java.util.Map;

public class Problem {
    public String id;
    public String title;
    public String description;
    public String difficulty; // easy, medium, hard
    public List<String> tables; // related tables
    public String starterSql; // optional starter
    public String solutionSql; // reference solution (not sent to client)

    public Problem() {}

    public Problem(String id, String title, String description, String difficulty, List<String> tables, String starterSql, String solutionSql) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.tables = tables;
        this.starterSql = starterSql;
        this.solutionSql = solutionSql;
    }

    public Map<String, Object> publicView() {
        return Map.of(
                "id", id,
                "title", title,
                "description", description,
                "difficulty", difficulty,
                "tables", tables
        );
    }
}
