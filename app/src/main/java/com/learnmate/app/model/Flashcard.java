package com.learnmate.app.model;

public class Flashcard {
    private long id;
    private long materialId;
    private String topic;
    private String frontText;
    private String backText;
    private boolean mastered;
    private long createdAt;

    public Flashcard() {}

    public Flashcard(long materialId, String topic, String frontText, String backText) {
        this.materialId = materialId;
        this.topic = topic;
        this.frontText = frontText;
        this.backText = backText;
        this.mastered = false;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMaterialId() { return materialId; }
    public void setMaterialId(long materialId) { this.materialId = materialId; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getFrontText() { return frontText; }
    public void setFrontText(String frontText) { this.frontText = frontText; }

    public String getBackText() { return backText; }
    public void setBackText(String backText) { this.backText = backText; }

    public boolean isMastered() { return mastered; }
    public void setMastered(boolean mastered) { this.mastered = mastered; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
