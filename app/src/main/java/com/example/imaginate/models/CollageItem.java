package com.example.imaginate.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class CollageItem {
    private String id;
    private String name;
    private long timestamp;

    // Constructor kosong dengan nilai default
    public CollageItem() {
        this.id = "";
        this.name = "";
        this.timestamp = 0L;
    }

    // Constructor utama
    public CollageItem(String id, String name, long timestamp) {
        this.id = id;
        this.name = name;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
