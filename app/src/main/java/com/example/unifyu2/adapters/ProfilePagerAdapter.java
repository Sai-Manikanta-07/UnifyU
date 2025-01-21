package com.example.unifyu2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.unifyu2.fragments.MyClubsFragment;
import com.example.unifyu2.fragments.MyPostsFragment;

public class ProfilePagerAdapter extends FragmentStateAdapter {
    public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new MyClubsFragment();
            case 1:
                return new MyPostsFragment();
            default:
                return new MyClubsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
} 