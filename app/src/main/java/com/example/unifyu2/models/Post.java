package com.example.unifyu2.models;

import com.google.firebase.database.PropertyName;

public class Post {
    private String id;
    private String clubId;
    private String clubName;
    private String content;
    private String imageUrl;
    private long timestamp;
    private String authorId;
    private String authorName;

    // Required empty constructor for Firebase
    public Post() {}

    public Post(String id, String clubId, String content, String authorId) {
        this.id = id;
        this.clubId = clubId;
        this.content = content;
        this.authorId = authorId;
        this.timestamp = System.currentTimeMillis();
    }

    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("clubId")
    public String getClubId() { return clubId; }
    @PropertyName("clubId")
    public void setClubId(String clubId) { this.clubId = clubId; }

    @PropertyName("clubName")
    public String getClubName() { return clubName; }
    @PropertyName("clubName")
    public void setClubName(String clubName) { this.clubName = clubName; }

    @PropertyName("content")
    public String getContent() { return content; }
    @PropertyName("content")
    public void setContent(String content) { this.content = content; }

    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl; }
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @PropertyName("authorId")
    public String getAuthorId() { return authorId; }
    @PropertyName("authorId")
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    @PropertyName("authorName")
    public String getAuthorName() { return authorName; }
    @PropertyName("authorName")
    public void setAuthorName(String authorName) { this.authorName = authorName; }
} 