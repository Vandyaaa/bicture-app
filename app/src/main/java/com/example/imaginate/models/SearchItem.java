package com.example.imaginate.models;

public class SearchItem {
    private String title;
    private String imageUrl;
    private String type;
    private String userId;
    private String username;
    private String photoProfile;
    private String description;
    private Album album;
    private String albumId; // Added album ID field
    public String tagname;
    private long timestamp; // Waktu upload dalam epoch
    private String uploadDate;

    public SearchItem(){

    }

    // Constructor for album items
    public SearchItem(String title, String imageUrl, String type, String userId,
                      String username, String photoProfile, String description) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.photoProfile = photoProfile;
        this.description = description;
    }

    // Constructor for user and history items
    public SearchItem(String title, String imageUrl, String type, String userId) {
        this(title, imageUrl, type, userId, null, null, null);
    }

    public SearchItem(String title, String imageUrl, String type) {
        this(title, imageUrl, type, null);
    }

    // Add getter and setter for album ID
    public String getAlbumId() {
        return albumId != null ? albumId : (album != null ? album.getId() : null);
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    // Existing getters
    public String getUsername() { return username; }
    public String getPhotoProfile() { return photoProfile; }
    public String getDescription() { return description; }
    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
    public String getType() { return type; }
    public String getUserId() { return userId; }
    public Album getAlbum() { return album; }
    public String getTagName() {

        return tagname;
    }

    public void setTagName(String tagName) {
        this.tagname = tagName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }


}