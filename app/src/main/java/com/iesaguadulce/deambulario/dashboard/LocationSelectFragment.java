package com.iesaguadulce.deambulario.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.FragmentMilestoneLocationSelectBinding;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;


/**
 * Fragment that allows teacher to locate a milestone.
 *
 * @author Mario López Salazar.
 */
public class LocationSelectFragment extends Fragment implements OnMapReadyCallback {

    /*
     * ViewBinding to perform the fragment view.
     */
    private FragmentMilestoneLocationSelectBinding binding;

    /*
     * Google map.
     */
    private GoogleMap googleMap;

    /*
     * Current coordinates.
     */
    private LatLng selectedLatLng;

    /*
     * Milestone ViewModel.
     */
    private MilestoneViewModel milestoneViewModel;




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
        binding = FragmentMilestoneLocationSelectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Prepares the map, requests the ViewModel and performs the UI.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-construct from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Getting the ViewModel:
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);

        // Preparing the map:
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.selection_map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setting confirm button listener:
        binding.buttonConfirmLocation.setOnClickListener(v -> saveLocationAndReturn());
    }


    /**
     * Launched when the Google Map is ready. Used to perform the map appearance and show the current coordinates.
     *
     * @param map The ready Google Map.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Performing the map appearance:
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        checkMyLocationPermission();

        // Updating the coordinates when user moves the camera:
        googleMap.setOnCameraIdleListener(() ->
                selectedLatLng = googleMap.getCameraPosition().target);

        // Getting coordinates and centering the camera:
        Milestone milestone = milestoneViewModel.getSelectedMilestone().getValue();
        if (milestone != null && milestone.getCoordinates() != null) {
            LatLng initialPosition = new LatLng(milestone.getCoordinates().getLatitude(), milestone.getCoordinates().getLongitude());
            selectedLatLng = initialPosition;
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 22f));
        }
    }


    /**
     * Checks if the app has permission to get the user location, and shows the 'my location' button on map.
     */
    private void checkMyLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
        }
    }


    /**
     * Helps user to tune up the location and saves the milestone with the updates coordinates.
     */
    private void saveLocationAndReturn() {
        if (googleMap == null || selectedLatLng == null) {
            Toast.makeText(requireContext(), R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        // Requiring a high zoom before to save position:
        float currentZoom = googleMap.getCameraPosition().zoom;
        float requiredZoom = googleMap.getMaxZoomLevel() - 1.5f;
        if (currentZoom < requiredZoom) {
            Toast.makeText(requireContext(), getString(R.string.zoom_in), Toast.LENGTH_LONG).show();
            googleMap.animateCamera(CameraUpdateFactory.zoomIn());
            return;
        }

        // Saving milestone on the ViewModel:
        Milestone milestone = milestoneViewModel.getSelectedMilestone().getValue();
        if (milestone != null) {
            milestone.setCoordinates(new GeoPoint(selectedLatLng.latitude, selectedLatLng.longitude));
            milestoneViewModel.setSelectedMilestone(milestone);
        }

        // Navigating back:
        Navigation.findNavController(requireView()).popBackStack();
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