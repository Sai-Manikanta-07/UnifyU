package com.example.unifyu2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unifyu2.R;
import com.example.unifyu2.models.Club;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ClubViewHolder> {
    private List<Club> clubs;
    private OnClubClickListener listener;
    private String currentUserId;

    public interface OnClubClickListener {
        void onClubClick(Club club);
        void onManageClubClick(Club club);
    }

    public ClubAdapter(List<Club> clubs, OnClubClickListener listener, String currentUserId) {
        this.clubs = clubs;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_club, parent, false);
        return new ClubViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
        Club club = clubs.get(position);
        holder.bind(club, listener, currentUserId);
    }

    @Override
    public int getItemCount() {
        return clubs.size();
    }

    public void updateClubs(List<Club> newClubs) {
        this.clubs = newClubs;
        notifyDataSetChanged();
    }

    static class ClubViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView descriptionText;
        private TextView memberCountText;
        private MaterialButton joinButton;
        private MaterialButton exitButton;
        private Chip membershipStatus;
        private DatabaseReference membershipsRef;
        private DatabaseReference clubsRef;
        private String userId;
        private View adminPanel;
        private MaterialButton manageButton;

        ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.clubName);
            descriptionText = itemView.findViewById(R.id.clubDescription);
            memberCountText = itemView.findViewById(R.id.memberCount);
            joinButton = itemView.findViewById(R.id.joinButton);
            exitButton = itemView.findViewById(R.id.exitButton);
            membershipStatus = itemView.findViewById(R.id.membershipStatus);
            adminPanel = itemView.findViewById(R.id.adminPanel);
            manageButton = itemView.findViewById(R.id.manageButton);

            // Initialize Firebase references
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
            clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        }

        void bind(Club club, OnClubClickListener listener, String currentUserId) {
            nameText.setText(club.getName());
            descriptionText.setText(club.getDescription());
            memberCountText.setText(club.getMemberCount() + " members");
            
            // Check membership status
            checkMembershipStatus(club);

            // Show/hide admin options
            boolean isAdmin = currentUserId.equals(club.getAdminId());
            adminPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            
            if (isAdmin) {
                joinButton.setVisibility(View.GONE);
                exitButton.setVisibility(View.GONE);
                manageButton.setOnClickListener(v -> listener.onManageClubClick(club));
            } else {
                joinButton.setVisibility(View.VISIBLE);
                joinButton.setOnClickListener(v -> listener.onClubClick(club));
                
                // Setup exit button click
                exitButton.setOnClickListener(v -> showExitConfirmation(club));
            }
        }

        private void checkMembershipStatus(Club club) {
            String membershipId = userId + "_" + club.getId();
            membershipsRef.child(membershipId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && !userId.equals(club.getAdminId())) {
                        joinButton.setVisibility(View.GONE);
                        membershipStatus.setVisibility(View.VISIBLE);
                        exitButton.setVisibility(View.VISIBLE);
                    } else {
                        joinButton.setVisibility(View.VISIBLE);
                        membershipStatus.setVisibility(View.GONE);
                        exitButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
        }

        private void showExitConfirmation(Club club) {
            new MaterialAlertDialogBuilder(itemView.getContext())
                .setTitle("Exit Club")
                .setMessage("Are you sure you want to exit " + club.getName() + "?")
                .setPositiveButton("Exit", (dialog, which) -> exitClub(club))
                .setNegativeButton("Cancel", null)
                .show();
        }

        private void exitClub(Club club) {
            String membershipId = userId + "_" + club.getId();
            
            // First remove the membership
            membershipsRef.child(membershipId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Then decrement the member count
                    DatabaseReference memberCountRef = clubsRef.child(club.getId()).child("memberCount");
                    memberCountRef.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Integer currentCount = task.getResult().getValue(Integer.class);
                            if (currentCount != null && currentCount > 0) {
                                memberCountRef.setValue(currentCount - 1)
                                    .addOnSuccessListener(aVoid2 -> 
                                        Toast.makeText(itemView.getContext(),
                                            "Successfully left " + club.getName(),
                                            Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> 
                                        Toast.makeText(itemView.getContext(),
                                            "Error updating member count",
                                            Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(itemView.getContext(),
                        "Error leaving club",
                        Toast.LENGTH_SHORT).show());
        }
    }
} 