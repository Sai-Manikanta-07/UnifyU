package com.example.unifyu2.models;

public class ClubMembership {
    private String userId;
    private String clubId;
    private long joinedAt;

    // Required empty constructor for Firebase
    public ClubMembership() {}

    public ClubMembership(String userId, String clubId) {
        this.userId = userId;
        this.clubId = clubId;
        this.joinedAt = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getClubId() { return clubId; }
    public void setClubId(String clubId) { this.clubId = clubId; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
} 