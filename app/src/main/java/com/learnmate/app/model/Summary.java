package com.learnmate.app.model;

import java.util.ArrayList;
import java.util.List;

public class Summary {
    public static final String STYLE_CONCISE = "CONCISE";
    public static final String STYLE_DETAILED = "DETAILED";
    public static final String STYLE_EXAM_CHEAT_SHEET = "EXAM_CHEAT_SHEET";

    private long id;
    private long materialId;
    private String style;
    private String overview;
    private List<String> keyPoints = new ArrayList<>();
    private List<KeyTerm> keyTerms = new ArrayList<>();
    private long generatedAt;

    public Summary() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMaterialId() { return materialId; }
    public void setMaterialId(long materialId) { this.materialId = materialId; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }

    public List<String> getKeyPoints() { return keyPoints; }
    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints != null ? keyPoints : new ArrayList<>();
    }

    public List<KeyTerm> getKeyTerms() { return keyTerms; }
    public void setKeyTerms(List<KeyTerm> keyTerms) {
        this.keyTerms = keyTerms != null ? keyTerms : new ArrayList<>();
    }

    public long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }

    public static class KeyTerm {
        private String term;
        private String definition;

        public KeyTerm() {}

        public KeyTerm(String term, String definition) {
            this.term = term;
            this.definition = definition;
        }

        public String getTerm() { return term; }
        public void setTerm(String term) { this.term = term; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
    }
}
