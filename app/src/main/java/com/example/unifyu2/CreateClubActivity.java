package com.example.unifyu2;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.ClubMembership;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateClubActivity extends AppCompatActivity {
    private TextInputEditText nameEdit, descriptionEdit;
    private View progressBar;
    private DatabaseReference clubsRef;
    private DatabaseReference membershipsRef;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_club);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase references
        clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");

        // Initialize views
        nameEdit = findViewById(R.id.nameEditText);
        descriptionEdit = findViewById(R.id.descriptionEditText);
        progressBar = findViewById(R.id.progressBar);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Club");
        }

        // Setup create button
        findViewById(R.id.createButton).setOnClickListener(v -> {
            String name = nameEdit.getText().toString().trim();
            String description = descriptionEdit.getText().toString().trim();

            if (name.isEmpty()) {
                nameEdit.setError("Please enter a club name");
                return;
            }

            if (description.isEmpty()) {
                descriptionEdit.setError("Please enter a description");
                return;
            }

            createClub(name, description);
        });
    }

    private void createClub(String name, String description) {
        progressBar.setVisibility(View.VISIBLE);
        String clubId = clubsRef.push().getKey();
        if (clubId == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error generating club ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        Club newClub = new Club(
            clubId,
            name,
            description,
            "",  // No image URL initially
            currentUserId  // Set the current user as admin
        );

        clubsRef.child(clubId).setValue(newClub)
            .addOnSuccessListener(aVoid -> {
                // Create initial membership for admin
                String membershipId = currentUserId + "_" + clubId;
                ClubMembership membership = new ClubMembership(currentUserId, clubId);
                membership.setJoinedAt(System.currentTimeMillis());
                
                membershipsRef.child(membershipId).setValue(membership)
                    .addOnSuccessListener(aVoid2 -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Club created successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to create membership: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to create club: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
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