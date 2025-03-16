package com.example.unifyu2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.unifyu2.fragments.MyClubsFragment;
import com.example.unifyu2.fragments.MyEventsFragment;
import com.example.unifyu2.fragments.MyPostsFragment;
import com.example.unifyu2.fragments.ProfileFragment;

public class ProfileTabAdapter extends FragmentStateAdapter {

    public ProfileTabAdapter(@NonNull ProfileFragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new MyClubsFragment();
            case 1:
                return new MyPostsFragment();
            case 2:
                return new MyEventsFragment();
            default:
                return new MyClubsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Three tabs: My Clubs, My Posts, My Events
    }
} 