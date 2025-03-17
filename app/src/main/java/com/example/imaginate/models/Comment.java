package com.example.imaginate.models;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;
@IgnoreExtraProperties
public class Comment {
    private String id;
    private String userId;
    private String username;
    private String text;
    private String photoprofile;
    private long timestamp;
    private String parentUserId;
    private List<Comment> replies;
    private String parentCommentId; // Untuk reply
    private boolean isReplyToReply;
    private String repliedToUsername;

    public Comment() {
        // Required empty constructor for Firebase
        this.replies = new ArrayList<>();
    }

    public Comment(String userId, String username, String text, String photoprofile, long timestamp) {
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.photoprofile = photoprofile;
        this.timestamp = timestamp;
        this.replies = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPhotoprofile() {
        return photoprofile;
    }

    public void setPhotoprofile(String photoprofile) {
        this.photoprofile = photoprofile;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    public void addReply(Comment reply) {
        if (replies == null) {
            replies = new ArrayList<>();
        }
        replies.add(reply);
    }

    public boolean isReplyToReply() {
        return isReplyToReply;
    }


    public void setIsReplyToReply(boolean isReplyToReply) {
        this.isReplyToReply = isReplyToReply;
    }

    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }


    public String getParentUserId() {
        return parentUserId;
    }


    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public String getRepliedToUsername() {
        return repliedToUsername;
    }

    public void setRepliedToUsername(String repliedToUsername) {
        this.repliedToUsername = repliedToUsername;
    }
}