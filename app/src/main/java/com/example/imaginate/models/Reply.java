package com.example.imaginate.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Reply {
    private String replyId;
    private String userId;
    private String username;
    private String photoprofile;
    private String text;
    private long timestamp;

    public Reply() {
        // Required empty constructor for Firebase
    }

    public Reply(String replyId, String userId, String username, String photoprofile,
                 String text, long timestamp) {
        this.replyId = replyId;
        this.userId = userId;
        this.username = username;
        this.photoprofile = photoprofile;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getReplyId() { return replyId; }
    public void setReplyId(String replyId) { this.replyId = replyId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhotoprofile() { return photoprofile; }
    public void setPhotoprofile(String photoprofile) { this.photoprofile = photoprofile; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}