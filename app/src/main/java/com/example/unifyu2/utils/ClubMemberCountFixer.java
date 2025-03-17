package com.example.unifyu2.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to fix member count discrepancies in clubs
 */
public class ClubMemberCountFixer {
    private static final String TAG = "ClubMemberCountFixer";
    
    /**
     * Synchronizes the member count for all clubs based on actual memberships
     * @param onComplete Callback to be called when the synchronization is complete
     */
    public static void synchronizeAllClubMemberCounts(Runnable onComplete) {
        DatabaseReference clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        
        // First, get all clubs
        clubsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot clubsSnapshot) {
                if (!clubsSnapshot.exists()) {
                    Log.d(TAG, "No clubs found");
                    if (onComplete != null) onComplete.run();
                    return;
                }
                
                // Get all memberships
                membershipsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot membershipsSnapshot) {
                        // Count memberships for each club
                        Map<String, Integer> clubMemberCounts = new HashMap<>();
                        
                        for (DataSnapshot membershipSnapshot : membershipsSnapshot.getChildren()) {
                            String clubId = membershipSnapshot.child("clubId").getValue(String.class);
                            if (clubId != null) {
                                int currentCount = clubMemberCounts.getOrDefault(clubId, 0);
                                clubMemberCounts.put(clubId, currentCount + 1);
                            }
                        }
                        
                        // Update club member counts if different
                        int fixedCount = 0;
                        for (DataSnapshot clubSnapshot : clubsSnapshot.getChildren()) {
                            String clubId = clubSnapshot.getKey();
                            Integer actualCount = clubMemberCounts.getOrDefault(clubId, 0);
                            Integer currentCount = clubSnapshot.child("memberCount").getValue(Integer.class);
                            
                            if (currentCount == null || !currentCount.equals(actualCount)) {
                                clubsRef.child(clubId).child("memberCount").setValue(actualCount);
                                fixedCount++;
                                Log.d(TAG, "Fixed member count for club " + clubId + 
                                      ": " + currentCount + " -> " + actualCount);
                            }
                        }
                        
                        Log.d(TAG, "Fixed " + fixedCount + " club member counts");
                        if (onComplete != null) onComplete.run();
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting memberships", error.toException());
                        if (onComplete != null) onComplete.run();
                    }
                });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error getting clubs", error.toException());
                if (onComplete != null) onComplete.run();
            }
        });
    }
} 