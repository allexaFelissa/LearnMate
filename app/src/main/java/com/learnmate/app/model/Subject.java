package com.learnmate.app.model;

public class Subject {
    private long id;
    private String name;
    private String colorHex;
    private long createdAt;

    public Subject() {}

    public Subject(long id, String name, String colorHex, long createdAt) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name;
    }
}
