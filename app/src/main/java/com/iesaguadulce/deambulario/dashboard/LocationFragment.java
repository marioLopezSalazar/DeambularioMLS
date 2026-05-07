package com.iesaguadulce.deambulario.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.databinding.FragmentMilestoneLocationBinding;
import com.iesaguadulce.deambulario.map_and_location.MapUtils;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;

import java.util.Collections;

/**
 * Fragment to manage the Location tab corresponding to the ViewPager in Milestone view.
 *
 * @author Mario López Salazar.
 */
public class LocationFragment extends Fragment {

    /*
     * ViewBinding to perform the fragment view.
     */
    private FragmentMilestoneLocationBinding binding;

    /*
     * Current milestone.
     */
    Milestone currentMilestone;

    /*
     * Google map.
     */
    private GoogleMap googleMap;


    /**
     * Called to inflate the fragment's interface view.
     *
     * @param inflater           The LayoutInflater object used to inflate any views in the fragment.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMilestoneLocationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Prepares the map, sets up the fragment as an observer of the ViewModel and performs the UI.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-construct from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Preparing the map:
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_milestone_showing);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {

                // Setting map appearance
                this.googleMap = googleMap;
                this.googleMap.getUiSettings().setMapToolbarEnabled(false);
                this.googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                if (currentMilestone != null) {
                    updateMap();
                }
            });
        }

        // Getting the ViewModel:
        MilestoneViewModel milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        GlobalUIViewModel globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Observing the ViewModel:
        milestoneViewModel.getSelectedMilestone().observe(getViewLifecycleOwner(), milestone -> {
            currentMilestone = milestone;
            if (googleMap != null) {
                updateMap();
            }
        });

        // Performing UI appearance depending on the mode:
        Boolean isReadOnly = globalUIViewModel.isReadOnlyMode().getValue();
        if (isReadOnly != null && isReadOnly) {
            binding.buttonSettingMilestone.setVisibility(View.GONE);
        } else {
            binding.buttonSettingMilestone.setOnClickListener(v -> {

                // When click on Set Position button, request parent fragment to navigate to Coordinates selection fragment:
                Fragment parent = getParentFragment();
                if (parent instanceof MilestoneFragment) {
                    ((MilestoneFragment) parent).requestNavigationToSelectPosition();
                }
            });
        }
    }


    /**
     * Performs the map on the coordinates of the milestone.
     */
    private void updateMap() {

        if (currentMilestone != null && currentMilestone.getCoordinates() != null) {
            googleMap.clear();
            MapUtils.drawMilestonesOnMap(requireContext(), googleMap, Collections.singletonList(currentMilestone), true);
        }
    }


    /**
     * Performs step 3 of the Teacher guide.
     * Highlights the 'Set location' button.
     */
    void performGuide() {
        TeacherTourManager.checkMilestoneDetailsTour(
                (TeacherActivity) requireActivity(),
                binding.buttonSettingMilestone,
                () -> {
                    Fragment parent = getParentFragment();
                    if (parent instanceof MilestoneFragment) {
                        ((MilestoneFragment) parent).performGuide();
                    }
                }
        );
    }


    /**
     * Called when the Fragment is being destroyed. Avoids view binding memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}