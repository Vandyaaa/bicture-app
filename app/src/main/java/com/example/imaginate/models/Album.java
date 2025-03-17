package com.example.imaginate.models;

import java.util.List;

public class Album {
    private String id; // ID album
    private String title; // Judul gambar
    private String userId; // ID pengguna yang mengunggah
    private String username; // Username pengguna
    private String description; // Deskripsi gambar
    private String url; // URL gambar
    private String photoprofile; // URL foto profil pengguna
    private int likes; // Jumlah likes
    private boolean userHasLiked; // Status apakah user saat ini telah menyukai album
    private long timestamp; // Waktu upload dalam epoch
    private String uploadDate;
    private List<LikeData> userLikes;

    // Constructor default (penting untuk Firebase)
    public Album() {
    }

    // Constructor lengkap
    public Album( String userId, String id, String title, String username, String description, String url, String photoprofile, int likes, boolean userHasLiked, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.username = username;
        this.description = description;
        this.url = url;
        this.photoprofile = photoprofile;
        this.likes = likes;
        this.userHasLiked = userHasLiked;
        this.timestamp = timestamp;
    }

    // Getters dan Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPhotoprofile() {
        return photoprofile;
    }

    public void setPhotoprofile(String photoprofile) {
        this.photoprofile = photoprofile;
    }

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

    public List<LikeData> getUserLikes() {
        return userLikes;
    }

    public void setUserLikes(List<LikeData> userLikes) {
        this.userLikes = userLikes;
    }
}
