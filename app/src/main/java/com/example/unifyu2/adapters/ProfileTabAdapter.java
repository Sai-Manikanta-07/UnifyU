package com.example.unifyu2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.unifyu2.fragments.MyClubsFragment;
import com.example.unifyu2.fragments.MyEventsFragment;
import com.example.unifyu2.fragments.MyPostsFragment;
import com.example.unifyu2.fragments.ProfileFragment;

public class ProfileTabAdapter extends FragmentStateAdapter {
    private final int tabCount;
    private final boolean showPostsTab;

    public ProfileTabAdapter(@NonNull ProfileFragment fragment, int tabCount, boolean showPostsTab) {
        super(fragment);
        this.tabCount = tabCount;
        this.showPostsTab = showPostsTab;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (!showPostsTab) {
            // Without Posts tab (just Clubs and Events)
            switch (position) {
                case 0:
                    return new MyClubsFragment();
                case 1:
                    return new MyEventsFragment();
                default:
                    return new MyClubsFragment();
            }
        } else {
            // With Posts tab
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
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
} 