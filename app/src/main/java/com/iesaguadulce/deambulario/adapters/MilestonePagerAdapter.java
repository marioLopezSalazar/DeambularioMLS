package com.iesaguadulce.deambulario.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.iesaguadulce.deambulario.dashboard.ActivitiesFragment;
import com.iesaguadulce.deambulario.dashboard.ContentsFragment;
import com.iesaguadulce.deambulario.dashboard.LocationFragment;

/**
 * Adapter class for displaying the Milestones customizing fragments.
 * @author Mario López Salazar
 */
public class MilestonePagerAdapter extends FragmentStateAdapter {

    /**
     * Builds a new MilestonePageAdapter.
     * @param fragment Parent fragment.
     */
    public MilestonePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    /**
     * Manages the page navigation.
     * @param position The page position no navigate.
     * @return The fragment on the page to navigate.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new LocationFragment();
            case 1:
                return new ContentsFragment();
            case 2:
                return new ActivitiesFragment();
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the number of available pages.
     * @return The number of available pages (3 pages).
     */
    @Override
    public int getItemCount() {
        return 3;
    }
}