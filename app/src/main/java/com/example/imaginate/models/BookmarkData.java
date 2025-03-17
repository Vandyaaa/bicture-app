package com.example.imaginate.models;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.List;
@IgnoreExtraProperties
public class BookmarkData {
    private String id;
    private String userId;
    private String albumId;
    private String title;
    private String description;
    private String imageUrl;
    private String username;
    private String photoprofile;
    private long timestamp;
    private int likes; // Jumlah likes
    private boolean userHasLiked;
    private List<LikeData> userLikes;
    private String uploadDate;

    public BookmarkData() {
        // Required empty constructor for Firebase
    }

    public BookmarkData(String id ,String userId, String albumId, String title,
                        String description, String imageUrl, String username,
                        String photoprofile,int likes, boolean userHasLiked ,long timestamp) {
        this.id = id;
        this.userId = userId;
        this.albumId = albumId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.username = username;
        this.photoprofile = photoprofile;
        this.timestamp = timestamp;
        this.likes = likes;
        this.userHasLiked = userHasLiked;
        this.uploadDate = "";
    }


    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAlbumId() { return albumId; }
    public void setAlbumId(String albumId) { this.albumId = albumId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPhotoprofile() {
        return photoprofile;
    }

    public void setPhotoprofile(String photoprofile) {
        this.photoprofile = photoprofile;
    }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public boolean isUserHasLiked() {
        return userHasLiked;
    }

    public void setUserHasLiked(boolean userHasLiked) {
        this.userHasLiked = userHasLiked;
    }
    public List<LikeData> getUserLikes() {
        return userLikes;
    }

    public void setUserLikes(List<LikeData> userLikes) {
        this.userLikes = userLikes;
    }
    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

}