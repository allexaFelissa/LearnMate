package com.learnmate.app.model;

public class Material {
    private long id;
    private long subjectId;
    private String subjectName; // Populated via JOIN for display convenience
    private String title;
    private String filePath;
    private int pageCount;
    private String extractedText;
    private long createdAt;
    private long lastOpenedAt;

    public Material() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSubjectId() { return subjectId; }
    public void setSubjectId(long subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastOpenedAt() { return lastOpenedAt; }
    public void setLastOpenedAt(long lastOpenedAt) { this.lastOpenedAt = lastOpenedAt; }
}
