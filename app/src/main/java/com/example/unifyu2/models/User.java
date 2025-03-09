package com.example.unifyu2.models;

import com.google.firebase.database.PropertyName;

public class User {
    private String id;
    private String username;
    private String email;
    private String rollNumber;
    private String semester;

    // Required empty constructor for Firebase
    public User() {}

    public User(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    @PropertyName("id")
    public String getId() { return id; }
    
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("username")
    public String getUsername() { return username; }
    
    @PropertyName("username")
    public void setUsername(String username) { this.username = username; }

    @PropertyName("email")
    public String getEmail() { return email; }
    
    @PropertyName("email")
    public void setEmail(String email) { this.email = email; }

    @PropertyName("rollNumber")
    public String getRollNumber() { return rollNumber != null ? rollNumber : ""; }
    
    @PropertyName("rollNumber")
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    @PropertyName("semester")
    public String getSemester() { return semester != null ? semester : ""; }
    
    @PropertyName("semester")
    public void setSemester(String semester) { this.semester = semester; }
} 