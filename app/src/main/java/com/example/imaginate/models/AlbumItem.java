package com.example.imaginate.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
// Album Item Model
public class AlbumItem {
    private String title;
    private String imageUrl;
    private String description;
    private String uploadDate;
    private String username;
    private String userProfilePic;
    private Long likesCount;

    public AlbumItem() {

    }

    public AlbumItem(String title, String imageUrl, String description,
                     String uploadDate, String username, String userProfilePic,
                     Long likesCount) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.description = description;
        this.uploadDate = uploadDate;
        this.username = username;
        this.userProfilePic = userProfilePic;
        this.likesCount = likesCount;
    }



    // Getters
    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public String getUploadDate() { return uploadDate; }
    public String getUsername() { return username; }
    public String getUserProfilePic() { return userProfilePic; }
    public Long getLikesCount() { return likesCount; }
}

