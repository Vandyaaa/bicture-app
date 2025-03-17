package com.example.imaginate.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ProfileData {
    public String username;
    public String photoprofile;
    public String location;
    public String bio;
    public String email;
    public long followers;
    public long following;
    public long post;
    public String tagname;

    public ProfileData () {}

    public ProfileData(String username, String photoprofile, String location, String bio, long followers, long following, long post, String tagname) {
        this.username = username;
        this.photoprofile = photoprofile;
        this.bio = bio;
        this.tagname = tagname;
        this.location = location;
        this.followers = followers;
        this.following = following;
        this.post = post;
    }

    public String getTagName() {

        return tagname;
    }

    public void setTagName(String tagName) {
        this.tagname = tagName;
    }
}