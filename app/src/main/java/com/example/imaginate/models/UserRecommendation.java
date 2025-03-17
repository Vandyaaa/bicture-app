package com.example.imaginate.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UserRecommendation {
    private String userId;
    private String username;
    private String photoProfile;
    private long followers;

    public UserRecommendation () {}

    public UserRecommendation(String userId, String username, String photoProfile, long followers) {
        this.userId = userId;
        this.username = username;
        this.photoProfile = photoProfile;
        this.followers = followers;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPhotoProfile() { return photoProfile; }
    public long getFollowers() { return followers; }
}