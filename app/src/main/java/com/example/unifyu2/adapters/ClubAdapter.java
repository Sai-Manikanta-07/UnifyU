package com.example.unifyu2.adapters;

import android.content.Context;
import android.util.Log;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        void onMembershipChanged(Club club);
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
        
        Log.d("ClubAdapter", "Updating clubs with " + newClubs.size() + " clubs: " + newClubs);
        
        List<Club> adminClubs = new ArrayList<>();
        List<Club> memberClubs = new ArrayList<>();
        List<Club> otherClubs = new ArrayList<>();
        
        // First pass: separate admin clubs
        for (Club club : newClubs) {
            if (club.getAdminId() != null && club.getAdminId().equals(currentUserId)) {
                Log.d("ClubAdapter", "Adding club to admin list: " + club);
                adminClubs.add(club);
            } else {
                // Temporarily add to otherClubs, will check membership later
                Log.d("ClubAdapter", "Club " + club.getName() + " not admin, checking membership");
                otherClubs.add(club);
            }
        }
        
        // If no clubs to process, show final list
        if (otherClubs.isEmpty()) {
            refreshList(adminClubs, memberClubs, otherClubs);
            return;
        }
        
        // Second pass: check memberships for remaining clubs
        List<Club> clubsToCheck = new ArrayList<>(otherClubs);
        final int[] processedCount = {0};
        final int totalToProcess = clubsToCheck.size();
        
        for (Club club : clubsToCheck) {
            String membershipId = currentUserId + "_" + club.getId();
            Log.d("ClubAdapter", "Checking membership: " + membershipId);
            
            membershipsRef.child(membershipId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    processedCount[0]++;
                    
                    if (snapshot.exists()) {
                        Log.d("ClubAdapter", "User is a member of club: " + club.getName());
                        otherClubs.remove(club);
                        memberClubs.add(club);
                    } else {
                        Log.d("ClubAdapter", "User is NOT a member of club: " + club.getName());
                    }
                    
                    if (processedCount[0] >= totalToProcess) {
                        Log.d("ClubAdapter", "Membership checks complete. Refreshing list.");
                        refreshList(adminClubs, memberClubs, otherClubs);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    processedCount[0]++;
                    Log.e("ClubAdapter", "Error checking membership: " + error.getMessage());
                    
                    if (processedCount[0] >= totalToProcess) {
                        Log.d("ClubAdapter", "Membership checks complete (with errors). Refreshing list.");
                        refreshList(adminClubs, memberClubs, otherClubs);
                    }
                }
            });
        }
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
            memberCountText.setText(itemView.getContext().getString(
                R.string.member_count, club.getMemberCount()));
            
            // Default visibility
            joinButton.setVisibility(View.GONE);
            exitButton.setVisibility(View.GONE);
            adminPanel.setVisibility(View.GONE);
            membershipStatus.setVisibility(View.GONE);

            // Check if current user is admin
            boolean isAdmin = club.getAdminId().equals(currentUserId);
            
            // Check if current user is a member
            final String membershipId = currentUserId + "_" + club.getId();
            
            Log.d("ClubAdapter", "Checking membership for club: " + club.getName() + " (ID: " + club.getId() + ")");
            Log.d("ClubAdapter", "Membership ID: " + membershipId);
            Log.d("ClubAdapter", "Membership reference path: " + membershipsRef.child(membershipId).toString());
            
            membershipsRef.child(membershipId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean isMember = snapshot.exists();
                    Log.d("ClubAdapter", "Is member: " + isMember + " for club: " + club.getName());
                    
                    Context context = itemView.getContext();
                    
                    if (isAdmin) {
                        // Admin UI
                        joinButton.setVisibility(View.GONE);
                        exitButton.setVisibility(View.GONE);
                        adminPanel.setVisibility(View.VISIBLE);
                        membershipStatus.setVisibility(View.VISIBLE);
                        membershipStatus.setText(R.string.admin_status);
                        membershipStatus.setChipBackgroundColorResource(R.color.admin_chip_background);
                    } else if (isMember) {
                        // Member UI
                        joinButton.setVisibility(View.GONE);
                        exitButton.setVisibility(View.VISIBLE);
                        adminPanel.setVisibility(View.GONE);
                        membershipStatus.setVisibility(View.VISIBLE);
                        membershipStatus.setText(R.string.member_status);
                        membershipStatus.setChipBackgroundColorResource(R.color.member_chip_background);
                    } else {
                        // Non-member UI
                        joinButton.setVisibility(View.VISIBLE);
                        exitButton.setVisibility(View.GONE);
                        adminPanel.setVisibility(View.GONE);
                        membershipStatus.setVisibility(View.GONE);
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
                
                // Create a proper membership object
                // Use the existing membershipsRef parameter instead of creating a new one
                // Use the existing membershipId parameter instead of creating a new one
                
                // Create membership with proper structure
                Map<String, Object> membershipData = new HashMap<>();
                membershipData.put("userId", currentUserId);
                membershipData.put("clubId", club.getId());
                membershipData.put("joinedAt", System.currentTimeMillis());
                
                // Save the membership data
                membershipsRef.child(membershipId).setValue(membershipData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ClubAdapter", "Successfully joined club: " + club.getName());
                        
                        // Increment member count
                        DatabaseReference clubRef = FirebaseDatabase.getInstance()
                            .getReference("clubs")
                            .child(club.getId());
                            
                        clubRef.child("memberCount").get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Integer currentCount = task.getResult().getValue(Integer.class);
                                if (currentCount != null) {
                                    clubRef.child("memberCount").setValue(currentCount + 1);
                                    
                                    // Update club in local list
                                    club.setMemberCount(currentCount + 1);
                                    
                                    // Update UI immediately
                                    joinButton.setVisibility(View.GONE);
                                    exitButton.setVisibility(View.VISIBLE);
                                    membershipStatus.setVisibility(View.VISIBLE);
                                    membershipStatus.setText(R.string.member_status);
                                    membershipStatus.setChipBackgroundColorResource(R.color.member_chip_background);
                                    
                                    // Update member count text
                                    memberCountText.setText(itemView.getContext().getString(
                                        R.string.member_count, currentCount + 1));
                                        
                                    // We can't use ClubAdapter.this.notifyDataSetChanged() from a static context
                                    // Instead, use the listener to trigger a UI refresh
                                    if (listener != null) {
                                        // Notify the UI that the data has changed by calling a method on the activity
                                        ((OnClubClickListener) listener).onMembershipChanged(club);
                                    }
                                        
                                    // Show toast
                                    Toast.makeText(itemView.getContext(),
                                        "Successfully joined " + club.getName(),
                                        Toast.LENGTH_SHORT).show();
                                }
                            }
                            joinButton.setEnabled(true);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ClubAdapter", "Failed to join club: " + e.getMessage());
                        Toast.makeText(itemView.getContext(),
                            "Failed to join club: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                        joinButton.setEnabled(true);
                    });
            });
        }
    }
} 