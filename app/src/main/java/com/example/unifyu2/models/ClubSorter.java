package com.example.unifyu2.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClubSorter {
    public static List<Club> sortClubs(List<Club> clubs, String currentUserId) {
        List<Club> adminClubs = new ArrayList<>();
        List<Club> memberClubs = new ArrayList<>();
        List<Club> otherClubs = new ArrayList<>();
        
        for (Club club : clubs) {
            if (club.getAdminId().equals(currentUserId)) {
                adminClubs.add(club);
            } else {
                otherClubs.add(club);
            }
        }
        
        // Sort each list by name
        Collections.sort(adminClubs, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
        Collections.sort(memberClubs, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
        Collections.sort(otherClubs, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
        
        // Combine lists in order: admin clubs first, then member clubs, then others
        List<Club> sortedClubs = new ArrayList<>();
        sortedClubs.addAll(adminClubs);
        sortedClubs.addAll(memberClubs);
        sortedClubs.addAll(otherClubs);
        
        return sortedClubs;
    }
} 