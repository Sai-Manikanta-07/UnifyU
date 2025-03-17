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

public class ClubAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CLUB = 1;
    
    private List<Object> items; // Can be either String (header) or Club
    private OnClubClickListener listener;
    private String currentUserId;
    private DatabaseReference membershipsRef;

    public interface OnClubClickListener {
        void onClubClick(Club club);
        void onManageClubClick(Club club);
        void onExitClubClick(Club club);
    }

    public ClubAdapter(List<Club> clubs, OnClubClickListener listener, String currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        this.items = new ArrayList<>();
        updateClubs(clubs);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_club, parent, false);
            return new ClubViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.headerTitle.setText((String) items.get(position));
        } else if (holder instanceof ClubViewHolder) {
            ClubViewHolder clubHolder = (ClubViewHolder) holder;
            Club club = (Club) items.get(position);
            clubHolder.bind(club, listener, currentUserId, membershipsRef);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? VIEW_TYPE_HEADER : VIEW_TYPE_CLUB;
    }

    public void updateClubs(List<Club> newClubs) {
        items.clear();
        
        List<Club> adminClubs = new ArrayList<>();
        List<Club> memberClubs = new ArrayList<>();
        List<Club> otherClubs = new ArrayList<>();
        
        // First pass: separate admin clubs
        for (Club club : newClubs) {
            if (club.getAdminId().equals(currentUserId)) {
                adminClubs.add(club);
            } else {
                // Temporarily add to otherClubs, will check membership later
                otherClubs.add(club);
            }
        }
        
        // Second pass: check memberships for remaining clubs
        for (Club club : new ArrayList<>(otherClubs)) {
            String membershipId = currentUserId + "_" + club.getId();
            membershipsRef.child(membershipId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        otherClubs.remove(club);
                        memberClubs.add(club);
                        refreshList(adminClubs, memberClubs, otherClubs);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Keep club in otherClubs if membership check fails
                }
            });
        }
        
        // Initial population of the list
        refreshList(adminClubs, memberClubs, otherClubs);
    }
    
    private void refreshList(List<Club> adminClubs, List<Club> memberClubs, List<Club> otherClubs) {
        items.clear();
        
        if (!adminClubs.isEmpty()) {
            items.add("Clubs You Manage");
            items.addAll(adminClubs);
        }
        
        if (!memberClubs.isEmpty()) {
            items.add("Clubs You've Joined");
            items.addAll(memberClubs);
        }
        
        if (!otherClubs.isEmpty()) {
            items.add("Other Clubs");
            items.addAll(otherClubs);
        }
        
        notifyDataSetChanged();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;
        
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.sectionTitle);
        }
    }

    static class ClubViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView descriptionText;
        private final TextView memberCountText;
        private final MaterialButton joinButton;
        private final MaterialButton exitButton;
        private final MaterialButton manageButton;
        private final View adminPanel;
        private final Chip membershipStatus;

        public ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.clubName);
            descriptionText = itemView.findViewById(R.id.clubDescription);
            memberCountText = itemView.findViewById(R.id.memberCount);
            joinButton = itemView.findViewById(R.id.joinButton);
            exitButton = itemView.findViewById(R.id.exitButton);
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
            
            // Create membership ID once for both membership check and join action
            final String membershipId = currentUserId + "_" + club.getId();
            
            // Check membership status
            membershipsRef.child(membershipId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean isMember = snapshot.exists();
                    
                    // Update UI based on user's role
                    if (isAdmin) {
                        joinButton.setVisibility(View.GONE);
                        exitButton.setVisibility(View.GONE);
                        membershipStatus.setVisibility(View.VISIBLE);
                        membershipStatus.setText("Admin");
                        adminPanel.setVisibility(View.VISIBLE);
                    } else if (isMember) {
                        joinButton.setVisibility(View.GONE);
                        exitButton.setVisibility(View.VISIBLE);
                        membershipStatus.setVisibility(View.VISIBLE);
                        membershipStatus.setText("Member");
                        adminPanel.setVisibility(View.GONE);
                    } else {
                        joinButton.setVisibility(View.VISIBLE);
                        exitButton.setVisibility(View.GONE);
                        membershipStatus.setVisibility(View.GONE);
                        adminPanel.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });

            // Set click listeners
            itemView.setOnClickListener(v -> listener.onClubClick(club));
            manageButton.setOnClickListener(v -> listener.onManageClubClick(club));
            exitButton.setOnClickListener(v -> listener.onExitClubClick(club));
            
            joinButton.setOnClickListener(v -> {
                joinButton.setEnabled(false);
                
                // Use the existing membershipId
                membershipsRef.child(membershipId).setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        // Increment member count
                        DatabaseReference clubRef = FirebaseDatabase.getInstance()
                            .getReference("clubs")
                            .child(club.getId())
                            .child("memberCount");
                            
                        clubRef.get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Integer currentCount = task.getResult().getValue(Integer.class);
                                if (currentCount != null) {
                                    clubRef.setValue(currentCount + 1);
                                }
                            }
                            joinButton.setEnabled(true);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(),
                            "Failed to join club: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                        joinButton.setEnabled(true);
                    });
            });
        }
    }
} 