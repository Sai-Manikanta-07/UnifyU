package com.example.unifyu2.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unifyu2.ManageClubActivity;
import com.example.unifyu2.R;
import com.example.unifyu2.adapters.ClubAdapter;
import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.ClubMembership;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyClubsFragment extends Fragment implements ClubAdapter.OnClubClickListener {
    private static final String TAG = "MyClubsFragment";
    
    private RecyclerView recyclerView;
    private TextView noClubsText;
    private CircularProgressIndicator progressBar;
    private ClubAdapter adapter;
    private List<Club> clubList;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_clubs, container, false);
        
        recyclerView = view.findViewById(R.id.clubsRecyclerView);
        noClubsText = view.findViewById(R.id.noClubsText);
        progressBar = view.findViewById(R.id.progressBar);
        
        clubList = new ArrayList<>();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        adapter = new ClubAdapter(clubList, this, currentUserId);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        loadMyClubs();
        
        return view;
    }
    
    private void loadMyClubs() {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        
        // First get all club memberships for this user
        DatabaseReference membershipRef = FirebaseDatabase.getInstance().getReference("club_memberships");
        membershipRef.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                
                List<String> clubIds = new ArrayList<>();
                for (DataSnapshot membershipSnapshot : snapshot.getChildren()) {
                    ClubMembership membership = membershipSnapshot.getValue(ClubMembership.class);
                    if (membership != null) {
                        clubIds.add(membership.getClubId());
                    }
                }
                
                if (clubIds.isEmpty()) {
                    noClubsText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                
                // Now fetch the club details for each club ID
                loadClubDetails(clubIds);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Database error: ", error.toException());
                Toast.makeText(getContext(), "Failed to load clubs", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    private void loadClubDetails(List<String> clubIds) {
        clubList.clear();
        DatabaseReference clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        
        for (String clubId : clubIds) {
            clubsRef.child(clubId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded() || getContext() == null) return;
                    
                    Club club = snapshot.getValue(Club.class);
                    if (club != null) {
                        clubList.add(club);
                        adapter.notifyDataSetChanged();
                        
                        noClubsText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    
                    if (clubList.size() == clubIds.size()) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Database error: ", error.toException());
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onClubClick(Club club) {
        // Handle club click if needed
    }

    @Override
    public void onManageClubClick(Club club) {
        Intent intent = new Intent(getContext(), ManageClubActivity.class);
        intent.putExtra("club", club);
        startActivity(intent);
    }
} 