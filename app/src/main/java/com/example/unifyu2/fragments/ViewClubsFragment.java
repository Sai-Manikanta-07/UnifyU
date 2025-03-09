package com.example.unifyu2.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unifyu2.R;
import com.example.unifyu2.CreateClubActivity;
import com.example.unifyu2.ManageClubActivity;
import com.example.unifyu2.adapters.ClubAdapter;
import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.ClubMembership;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewClubsFragment extends Fragment implements ClubAdapter.OnClubClickListener {
    private RecyclerView recyclerView;
    private ClubAdapter adapter;
    private View progressBar;
    private TextView emptyView;
    private ExtendedFloatingActionButton createClubFab;
    private DatabaseReference clubsRef;
    private DatabaseReference membershipsRef;
    private FirebaseAuth firebaseAuth;
    private ValueEventListener clubsListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_clubs, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.clubsRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        createClubFab = view.findViewById(R.id.createClubFab);

        // Initialize Firebase first
        firebaseAuth = FirebaseAuth.getInstance();
        clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");

        // Setup RecyclerView after Firebase initialization
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        adapter = new ClubAdapter(new ArrayList<>(), this, currentUserId);
        recyclerView.setAdapter(adapter);
        
        // Setup FAB
        createClubFab.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreateClubActivity.class);
            startActivity(intent);
        });
        
        loadClubs();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (clubsListener != null) {
            clubsRef.removeEventListener(clubsListener);
        }
    }

    private void loadClubs() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        clubsListener = clubsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Club> clubs = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Club club = snapshot.getValue(Club.class);
                    if (club != null) {
                        club.setId(snapshot.getKey());
                        clubs.add(club);
                    }
                }
                
                progressBar.setVisibility(View.GONE);
                if (clubs.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.updateClubs(clubs);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), 
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
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.join_club_title)
            .setMessage(getString(R.string.join_club_message, club.getName()))
            .setPositiveButton(R.string.join_action, (dialog, which) -> joinClub(club))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void joinClub(Club club) {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String membershipId = userId + "_" + club.getId();

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Check if already a member
        membershipsRef.child(membershipId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                        getString(R.string.already_member),
                        Toast.LENGTH_SHORT).show();
                } else {
                    // Create new membership
                    ClubMembership membership = new ClubMembership(userId, club.getId());
                    membershipsRef.child(membershipId).setValue(membership)
                            .addOnSuccessListener(aVoid -> {
                                // Increment member count
                                incrementMemberCount(club.getId());
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(),
                                    getString(R.string.join_failed, e.getMessage()),
                                    Toast.LENGTH_LONG).show();
                            });
                }
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(),
                    getString(R.string.error_checking_membership),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void incrementMemberCount(String clubId) {
        DatabaseReference clubRef = clubsRef.child(clubId).child("memberCount");
        clubRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer currentCount = task.getResult().getValue(Integer.class);
                int newCount = (currentCount != null ? currentCount : 0) + 1;
                clubRef.setValue(newCount).addOnCompleteListener(updateTask -> {
                    progressBar.setVisibility(View.GONE);
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(getContext(),
                            getString(R.string.join_success),
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onManageClubClick(Club club) {
        Intent intent = new Intent(getContext(), ManageClubActivity.class);
        intent.putExtra("club", club);
        startActivity(intent);
    }
} 