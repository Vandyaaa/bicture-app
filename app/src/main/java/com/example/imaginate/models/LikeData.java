package com.example.imaginate.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class LikeData {
    private String userId;
    private String username;
    private String photoProfile;
    private String tagName;

    // Constructor kosong (dibutuhkan oleh Firebase)
    public LikeData() {}

    // Constructor dengan parameter
    public LikeData(String userId, String username, String photoProfile, String tagName) {
        this.userId = userId;
        this.username = username;
        this.photoProfile = photoProfile;
        this.tagName = tagName;
    }



    // Getter dan Setter
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

    public String getPhotoProfile() {
        return photoProfile;
    }

    public void setPhotoProfile(String photoProfile) {
        this.photoProfile = photoProfile;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }
}
