package com.example.unifyu2.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.unifyu2.ManageClubActivity;
import com.example.unifyu2.R;
import com.example.unifyu2.adapters.ClubAdapter;
import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.ClubMembership;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class MyClubsFragment extends Fragment implements ClubAdapter.OnClubClickListener {
    private RecyclerView recyclerView;
    private TextView emptyView;
    private View progressBar;
    private ClubAdapter adapter;
    private DatabaseReference clubsRef;
    private DatabaseReference membershipsRef;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_clubs, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        
        clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClubAdapter(new ArrayList<>(), this, 
            FirebaseAuth.getInstance().getCurrentUser().getUid());
        recyclerView.setAdapter(adapter);
        
        loadClubs();
        
        return view;
    }

    private void showEmpty() {
        if (isAdded() && getView() != null) {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    private void loadClubs() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // First get all memberships for this user
        membershipsRef.orderByChild("userId").equalTo(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot membershipsSnapshot) {
                    if (!isAdded()) return;
                    
                    List<String> clubIds = new ArrayList<>();
                    
                    for (DataSnapshot membershipSnapshot : membershipsSnapshot.getChildren()) {
                        String clubId = membershipSnapshot.child("clubId").getValue(String.class);
                        if (clubId != null) {
                            clubIds.add(clubId);
                        }
                    }

                    if (clubIds.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    // Now get all clubs that user is member of
                    clubsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot clubsSnapshot) {
                            if (!isAdded()) return;
                            
                            List<Club> userClubs = new ArrayList<>();
                            
                            for (String clubId : clubIds) {
                                DataSnapshot clubSnapshot = clubsSnapshot.child(clubId);
                                if (clubSnapshot.exists()) {
                                    Club club = clubSnapshot.getValue(Club.class);
                                    if (club != null) {
                                        club.setId(clubId);
                                        userClubs.add(club);
                                    }
                                }
                            }

                            if (userClubs.isEmpty()) {
                                showEmpty();
                            } else {
                                progressBar.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                                emptyView.setVisibility(View.GONE);
                                adapter.updateClubs(userClubs);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            if (!isAdded()) return;
                            showEmpty();
                            Toast.makeText(getContext(), 
                                "Error loading clubs: " + error.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    if (!isAdded()) return;
                    showEmpty();
                    Toast.makeText(getContext(), 
                        "Error loading memberships: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
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