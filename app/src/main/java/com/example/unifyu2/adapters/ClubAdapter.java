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
    private DatabaseReference membershipsRef;

    public interface OnClubClickListener {
        void onClubClick(Club club);
        void onManageClubClick(Club club);
    }

    public ClubAdapter(List<Club> clubs, OnClubClickListener listener, String currentUserId) {
        this.clubs = clubs;
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
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
        holder.bind(club, listener, currentUserId, membershipsRef);
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
        private final TextView nameText;
        private final TextView descriptionText;
        private final TextView memberCountText;
        private final MaterialButton joinButton;
        private final MaterialButton manageButton;
        private final View adminPanel;
        private final Chip membershipStatus;

        public ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.clubName);
            descriptionText = itemView.findViewById(R.id.clubDescription);
            memberCountText = itemView.findViewById(R.id.memberCount);
            joinButton = itemView.findViewById(R.id.joinButton);
            manageButton = itemView.findViewById(R.id.manageButton);
            adminPanel = itemView.findViewById(R.id.adminPanel);
            membershipStatus = itemView.findViewById(R.id.membershipStatus);
        }

        void bind(Club club, OnClubClickListener listener, String currentUserId, DatabaseReference membershipsRef) {
            nameText.setText(club.getName());
            descriptionText.setText(club.getDescription());
            memberCountText.setText(club.getMemberCount() + " members");

            // Check if current user is admin
            boolean isAdmin = currentUserId.equals(club.getAdminId());
            
            // Check membership status
            String membershipId = currentUserId + "_" + club.getId();
            membershipsRef.child(membershipId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean isMember = snapshot.exists();
                    
                    // Update UI based on user's role
                    if (isAdmin) {
                        // Admin view
                        joinButton.setVisibility(View.GONE);
                        adminPanel.setVisibility(View.VISIBLE);
                        membershipStatus.setVisibility(View.VISIBLE);
                        membershipStatus.setText("Admin");
                        membershipStatus.setChipBackgroundColorResource(R.color.admin_chip_color);
                        manageButton.setOnClickListener(v -> listener.onManageClubClick(club));
                    } else if (isMember) {
                        // Member view
                        joinButton.setVisibility(View.GONE);
                        adminPanel.setVisibility(View.GONE);
                        membershipStatus.setVisibility(View.VISIBLE);
                        membershipStatus.setText("Member");
                        membershipStatus.setChipBackgroundColorResource(R.color.member_chip_color);
                    } else {
                        // Non-member view
                        joinButton.setVisibility(View.VISIBLE);
                        adminPanel.setVisibility(View.GONE);
                        membershipStatus.setVisibility(View.GONE);
                        joinButton.setOnClickListener(v -> listener.onClubClick(club));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(itemView.getContext(), 
                        "Error checking membership status", 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
} 