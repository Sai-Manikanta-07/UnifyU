package com.example.unifyu2;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.unifyu2.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewEventActivity extends AppCompatActivity {
    private String eventId;
    private DatabaseReference eventRef;
    private DatabaseReference clubRef;
    private Event currentEvent;
    
    private ImageView eventImage;
    private TextView eventTitle;
    private TextView eventDateTime;
    private TextView eventVenue;
    private TextView eventDescription;
    private TextView participantCount;
    private TextView clubName;
    private MaterialButton registerButton;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        // Get eventId from intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase references
        eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
        clubRef = FirebaseDatabase.getInstance().getReference("clubs");

        // Initialize views
        initializeViews();
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Details");
        }

        // Load event details
        loadEventDetails();
    }

    private void initializeViews() {
        eventImage = findViewById(R.id.eventImage);
        eventTitle = findViewById(R.id.eventTitle);
        eventDateTime = findViewById(R.id.eventDateTime);
        eventVenue = findViewById(R.id.eventVenue);
        eventDescription = findViewById(R.id.eventDescription);
        participantCount = findViewById(R.id.participantCount);
        clubName = findViewById(R.id.clubName);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        registerButton.setOnClickListener(v -> handleRegistration());
    }

    private void loadEventDetails() {
        progressBar.setVisibility(View.VISIBLE);
        
        eventRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                
                if (!snapshot.exists()) {
                    Toast.makeText(ViewEventActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                currentEvent = snapshot.getValue(Event.class);
                if (currentEvent != null) {
                    displayEventDetails();
                    loadClubDetails();
                    updateRegistrationButton();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ViewEventActivity.this, 
                    "Error loading event: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayEventDetails() {
        eventTitle.setText(currentEvent.getTitle());
        eventVenue.setText(currentEvent.getVenue());
        eventDescription.setText(currentEvent.getDescription());

        // Format and display date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        eventDateTime.setText(sdf.format(new Date(currentEvent.getDate())));

        // Load event image if available
        if (currentEvent.getImageUrl() != null && !currentEvent.getImageUrl().isEmpty()) {
            eventImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(currentEvent.getImageUrl())
                .centerCrop()
                .into(eventImage);
        } else {
            eventImage.setVisibility(View.GONE);
        }

        // Update participant count
        int registered = currentEvent.getRegisteredCount();
        String countText = registered + " registered";
        if (currentEvent.getMaxParticipants() > 0) {
            countText += " / " + currentEvent.getMaxParticipants() + " max";
        }
        participantCount.setText(countText);
    }

    private void loadClubDetails() {
        clubRef.child(currentEvent.getClubId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) {
                    clubName.setText(name);
                    clubName.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error silently
            }
        });
    }

    private void updateRegistrationButton() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isRegistered = currentEvent.isRegistered(currentUserId);
        
        if (isRegistered) {
            registerButton.setText("Unregister");
            registerButton.setStrokeColor(getColorStateList(R.color.error));
            registerButton.setTextColor(getColor(R.color.error));
        } else {
            registerButton.setText("Register");
            registerButton.setStrokeColor(getColorStateList(R.color.primary));
            registerButton.setTextColor(getColor(R.color.primary));
        }
        
        registerButton.setEnabled(currentEvent.isRegistrationOpen());
    }

    private void handleRegistration() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isRegistered = currentEvent.isRegistered(currentUserId);
        
        if (isRegistered) {
            // Unregister
            eventRef.child("registeredUsers").child(currentUserId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully unregistered", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to unregister: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
        } else {
            // Check if registration is still possible
            if (!currentEvent.canRegister()) {
                Toast.makeText(this, "Registration is closed or event is full", 
                    Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Register with phone number
            eventRef.child("registeredUsers").child(currentUserId)
                .setValue(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully registered", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to register: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 