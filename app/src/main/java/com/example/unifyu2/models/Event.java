package com.example.unifyu2.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.PropertyName;
import java.util.HashMap;
import java.util.Map;

public class Event implements Parcelable {
    private String eventId;
    private String clubId;
    private String title;
    private String description;
    private String venue;
    private long date;
    private String imageUrl;
    private Map<String, Object> registeredUsers;
    private int maxParticipants;
    private boolean registrationOpen;

    // Required empty constructor for Firebase
    public Event() {
        this.registeredUsers = new HashMap<>();
    }

    public Event(String eventId, String clubId, String title, String description, 
                String venue, long date, int maxParticipants) {
        this.eventId = eventId;
        this.clubId = clubId;
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.date = date;
        this.maxParticipants = maxParticipants;
        this.registrationOpen = true;
        this.registeredUsers = new HashMap<>();
    }

    protected Event(Parcel in) {
        eventId = in.readString();
        clubId = in.readString();
        title = in.readString();
        description = in.readString();
        venue = in.readString();
        date = in.readLong();
        imageUrl = in.readString();
        maxParticipants = in.readInt();
        registrationOpen = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(eventId);
        dest.writeString(clubId);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(venue);
        dest.writeLong(date);
        dest.writeString(imageUrl);
        dest.writeInt(maxParticipants);
        dest.writeByte((byte) (registrationOpen ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    // Getters and Setters
    @PropertyName("eventId")
    public String getEventId() { return eventId; }
    
    @PropertyName("eventId")
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    @PropertyName("clubId")
    public String getClubId() { return clubId; }
    
    @PropertyName("clubId")
    public void setClubId(String clubId) { this.clubId = clubId; }
    
    @PropertyName("title")
    public String getTitle() { return title; }
    
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }
    
    @PropertyName("description")
    public String getDescription() { return description; }
    
    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }
    
    @PropertyName("venue")
    public String getVenue() { return venue; }
    
    @PropertyName("venue")
    public void setVenue(String venue) { this.venue = venue; }
    
    @PropertyName("date")
    public long getDate() { return date; }
    
    @PropertyName("date")
    public void setDate(long date) { this.date = date; }
    
    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl; }
    
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    @PropertyName("registeredUsers")
    public Map<String, Object> getRegisteredUsers() { 
        return registeredUsers; 
    }
    
    @PropertyName("registeredUsers")
    public void setRegisteredUsers(Map<String, Object> registeredUsers) { 
        this.registeredUsers = registeredUsers; 
    }
    
    @PropertyName("maxParticipants")
    public int getMaxParticipants() { return maxParticipants; }
    
    @PropertyName("maxParticipants")
    public void setMaxParticipants(int maxParticipants) { 
        this.maxParticipants = maxParticipants; 
    }
    
    @PropertyName("registrationOpen")
    public boolean isRegistrationOpen() { return registrationOpen; }
    
    @PropertyName("registrationOpen")
    public void setRegistrationOpen(boolean registrationOpen) { 
        this.registrationOpen = registrationOpen; 
    }

    public int getRegisteredCount() {
        return registeredUsers != null ? registeredUsers.size() : 0;
    }

    public boolean isRegistered(String userId) {
        return registeredUsers != null && registeredUsers.containsKey(userId);
    }

    public String getRegisteredUserPhone(String userId) {
        Object value = registeredUsers != null ? registeredUsers.get(userId) : null;
        return value != null ? value.toString() : null;
    }

    public boolean canRegister() {
        return registrationOpen && (maxParticipants == 0 || getRegisteredCount() < maxParticipants);
    }
} 