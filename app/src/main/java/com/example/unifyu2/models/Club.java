package com.example.unifyu2.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.PropertyName;

public class Club implements Parcelable {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private int memberCount;
    private String adminId;

    // Required empty constructor for Firebase
    public Club() {}

    protected Club(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        imageUrl = in.readString();
        memberCount = in.readInt();
        adminId = in.readString();
    }

    public static final Creator<Club> CREATOR = new Creator<Club>() {
        @Override
        public Club createFromParcel(Parcel in) {
            return new Club(in);
        }

        @Override
        public Club[] newArray(int size) {
            return new Club[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(imageUrl);
        dest.writeInt(memberCount);
        dest.writeString(adminId);
    }

    public Club(String id, String name, String description, String imageUrl, String adminId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.memberCount = 0;
        this.adminId = adminId;
    }

    @PropertyName("id")
    public String getId() { return id; }
    
    @PropertyName("id")
    public void setId(String id) { this.id = id; }
    
    @PropertyName("name")
    public String getName() { return name; }
    
    @PropertyName("name")
    public void setName(String name) { this.name = name; }
    
    @PropertyName("description")
    public String getDescription() { return description; }
    
    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }
    
    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl; }
    
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("memberCount")
    public int getMemberCount() { return memberCount; }
    
    @PropertyName("memberCount")
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    @PropertyName("adminId")
    public String getAdminId() { return adminId; }
    
    @PropertyName("adminId")
    public void setAdminId(String adminId) { this.adminId = adminId; }
} 