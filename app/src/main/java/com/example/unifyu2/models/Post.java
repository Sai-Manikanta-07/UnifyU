package com.example.unifyu2.models;

import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post {
    private String postId;
    private String userId;
    private String authorId;  // Added for backward compatibility
    private String userName;
    private String content;
    private String imageUrl;
    private String linkUrl;
    private String linkTitle;
    private String linkDescription;
    private Map<String, Boolean> likes;
    private Map<String, Object> reactions;
    private Map<String, List<String>> reactedUsers;
    private Object timestamp;
    private String postType;
    private String clubId;
    private String clubName;
    private String authorName;
    private int totalReactions;

    public Post() {
        // Required empty constructor for Firebase
        this.likes = new HashMap<>();
        this.reactions = new HashMap<>();
        this.reactedUsers = new HashMap<>();
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public Post(String userId, String userName, String content, String postType) {
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.postType = postType;
        this.likes = new HashMap<>();
        this.reactions = new HashMap<>();
        this.reactedUsers = new HashMap<>();
        this.timestamp = ServerValue.TIMESTAMP;
    }

    // Getters and Setters
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    @PropertyName("id")  // For backward compatibility
    public String getId() { return postId; }
    @PropertyName("id")
    public void setId(String id) { this.postId = id; }

    public String getUserId() { return userId != null ? userId : authorId; }
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("authorId")  // For backward compatibility
    public String getAuthorId() { return authorId; }
    @PropertyName("authorId")
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getUserName() { return userName != null ? userName : authorName; }
    public void setUserName(String userName) { this.userName = userName; }

    @PropertyName("authorName")  // For backward compatibility
    public String getAuthorName() { return authorName; }
    @PropertyName("authorName")
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public String getLinkTitle() { return linkTitle; }
    public void setLinkTitle(String linkTitle) { this.linkTitle = linkTitle; }

    public String getLinkDescription() { return linkDescription; }
    public void setLinkDescription(String linkDescription) { this.linkDescription = linkDescription; }

    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }

    public Map<String, Object> getReactions() { return reactions; }
    public void setReactions(Map<String, Object> reactions) { this.reactions = reactions; }

    public Map<String, List<String>> getReactedUsers() { return reactedUsers; }
    public void setReactedUsers(Map<String, List<String>> reactedUsers) { this.reactedUsers = reactedUsers; }

    public Object getTimestamp() { return timestamp; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    public Long getTimestampLong() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Number) {
            return ((Number) timestamp).longValue();
        }
        return 0L; // Default value if timestamp is not a number
    }

    public String getPostType() { 
        if (postType == null) {
            // Infer post type if not set
            if (imageUrl != null && !imageUrl.isEmpty()) {
                return "IMAGE";
            } else if (linkUrl != null && !linkUrl.isEmpty()) {
                return "LINK";
            } else {
                return "TEXT";
            }
        }
        return postType; 
    }
    
    public void setPostType(String postType) { this.postType = postType; }

    public String getClubId() { return clubId; }
    public void setClubId(String clubId) { this.clubId = clubId; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public int getTotalReactions() { 
        if (reactions != null) {
            return reactions.size();
        }
        return 0;
    }
    
    public void setTotalReactions(int totalReactions) { this.totalReactions = totalReactions; }
    
    // Helper methods
    public boolean isLikedBy(String userId) {
        if (likes != null && likes.containsKey(userId)) {
            return likes.get(userId);
        }
        return false;
    }
    
    public String getReactionByUser(String userId) {
        if (reactions != null && reactions.containsKey(userId)) {
            return reactions.get(userId).toString();
        }
        return null;
    }
} 