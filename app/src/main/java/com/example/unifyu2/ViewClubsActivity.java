package com.example.unifyu2;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unifyu2.adapters.ClubAdapter;
import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.ClubMembership;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ViewClubsActivity extends AppCompatActivity implements ClubAdapter.OnClubClickListener {
    private RecyclerView recyclerView;
    private ClubAdapter adapter;
    private ProgressBar progressBar;
    private DatabaseReference clubsRef;
    private DatabaseReference membershipsRef;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_clubs);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        recyclerView = findViewById(R.id.clubsRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClubAdapter(new ArrayList<>(), this, 
            FirebaseAuth.getInstance().getCurrentUser().getUid());
        recyclerView.setAdapter(adapter);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        
        loadClubs();
    }

    private void loadClubs() {
        progressBar.setVisibility(View.VISIBLE);
        
        clubsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Club> clubs = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Club club = snapshot.getValue(Club.class);
                    if (club != null) {
                        club.setId(snapshot.getKey());
                        clubs.add(club);
                    }
                }
                adapter.updateClubs(clubs);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ViewClubsActivity.this, 
                    "Error loading clubs: " + databaseError.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onClubClick(Club club) {
        showJoinConfirmationDialog(club);
    }

    private void showJoinConfirmationDialog(Club club) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.join_club_title))
                .setMessage(getString(R.string.join_club_message, club.getName()))
                .setPositiveButton(getString(R.string.join_button), (dialog, which) -> joinClub(club))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void joinClub(Club club) {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String membershipId = userId + "_" + club.getId();

        // Check if already a member
        membershipsRef.child(membershipId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    // Already a member
                    Toast.makeText(this, 
                        getString(R.string.already_member), 
                        Toast.LENGTH_SHORT).show();
                } else {
                    // Not a member, proceed with joining
                    ClubMembership membership = new ClubMembership(userId, club.getId());
                    membershipsRef.child(membershipId).setValue(membership)
                            .addOnSuccessListener(aVoid -> {
                                // Increment member count
                                incrementMemberCount(club.getId());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this,
                                    getString(R.string.join_failed, e.getMessage()),
                                    Toast.LENGTH_LONG).show();
                            });
                }
            } else {
                Toast.makeText(this,
                    getString(R.string.error_checking_membership),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void incrementMemberCount(String clubId) {
        DatabaseReference clubRef = FirebaseDatabase.getInstance().getReference("clubs")
            .child(clubId).child("memberCount");
        clubRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer currentCount = task.getResult().getValue(Integer.class);
                int newCount = (currentCount != null ? currentCount : 0) + 1;
                clubRef.setValue(newCount).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this,
                            getString(R.string.join_success),
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
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

    @Override
    public void onManageClubClick(Club club) {
        Intent intent = new Intent(this, ManageClubActivity.class);
        intent.putExtra("club", club);
        startActivity(intent);
    }

    @Override
    public void onExitClubClick(Club club) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Exit Club")
            .setMessage("Are you sure you want to exit " + club.getName() + "?")
            .setPositiveButton("Exit", (dialog, which) -> exitClub(club))
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void exitClub(Club club) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String membershipId = userId + "_" + club.getId();
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        
        // Remove membership
        DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        membershipsRef.child(membershipId).removeValue()
            .addOnSuccessListener(aVoid -> {
                // Decrement member count
                decrementMemberCount(club.getId());
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this,
                    "Failed to exit club: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            });
    }
    
    private void decrementMemberCount(String clubId) {
        DatabaseReference clubRef = FirebaseDatabase.getInstance().getReference("clubs").child(clubId).child("memberCount");
        clubRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer currentCount = task.getResult().getValue(Integer.class);
                if (currentCount != null && currentCount > 0) {
                    int newCount = currentCount - 1;
                    clubRef.setValue(newCount).addOnCompleteListener(updateTask -> {
                        progressBar.setVisibility(View.GONE);
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(this,
                                "Successfully exited club",
                                Toast.LENGTH_SHORT).show();
                            // Refresh the clubs list
                            loadClubs();
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this,
                    "Error updating member count",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
} 