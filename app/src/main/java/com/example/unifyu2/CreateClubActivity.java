package com.example.unifyu2;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.ClubMembership;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateClubActivity extends AppCompatActivity {
    private TextInputEditText nameEdit, descriptionEdit;
    private DatabaseReference clubsRef;
    private DatabaseReference membershipsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_club);

        // Initialize Firebase references
        clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");

        // Initialize views
        nameEdit = findViewById(R.id.nameEditText);
        descriptionEdit = findViewById(R.id.descriptionEditText);

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Create Club");

        // Setup create button
        findViewById(R.id.createButton).setOnClickListener(v -> {
            String name = nameEdit.getText().toString().trim();
            String description = descriptionEdit.getText().toString().trim();

            if (name.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            createClub(name, description, "");
        });
    }

    private void createClub(String name, String description, String imageUrl) {
        String clubId = clubsRef.push().getKey();
        if (clubId == null) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        Club newClub = new Club(
            clubId,
            name,
            description,
            imageUrl,
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
                        Toast.makeText(this, "Club created successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Failed to create membership", Toast.LENGTH_SHORT).show()
                    );
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to create club", Toast.LENGTH_SHORT).show()
            );
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