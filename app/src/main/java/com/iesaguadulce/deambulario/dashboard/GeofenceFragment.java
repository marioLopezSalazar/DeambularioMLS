package com.iesaguadulce.deambulario.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;
import com.google.firebase.firestore.GeoPoint;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.adapters.GeofenceAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentRouteGeofenceBinding;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.map_and_location.MapUtils;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that allows user to set up the route geofence, by drawing bubbles on the map.
 *
 * @author Mario López Salazar.
 */
public class GeofenceFragment extends Fragment implements OnMapReadyCallback {

    /*
     * ViewBinding to manage the views.
     */
    private FragmentRouteGeofenceBinding binding;

    /*
     * ViewModel to manage the UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;

    /*
     * ViewModel to manage the data access.
     */
    private RouteViewModel routeViewModel;
    private Route route;
    private MilestoneViewModel milestoneViewModel;
    private List<Milestone> milestones;
    private boolean wasGeofenceEnabled;
    private boolean wasGeofenceEnabledCaptured = false;

    /*
     * Variables to manage the map appearance and geofence building.
     */
    private GoogleMap map;
    boolean isMapReady = false;
    private List<Marker> markers;
    private List<Route.Geofence> geofenceList = new ArrayList<>();
    private final List<Circle> bubbles = new ArrayList<>();
    private Marker dragMarker;
    private long lastValidationTime = 0;
    private static final long VALIDATION_INTERVAL_MS = 100;

    /*
     * List adapter for bubbles recyclerview.
     */
    private GeofenceAdapter adapter;
    private int currentBubble = -1;

    /*
     * Default bubbles radius, to be performed when initiates drawing a new one.
     */
    private static final int DEFAULT_RADIUS = 30;


    /**
     * Used to create de view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate  any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRouteGeofenceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up the UI and establishes this fragment as an observer of the ViewModel.
     *
     * @param view               The View returned by the onCreateView() method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Getting the ViewModels:
        routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Setting the recyclerview adapter:
        adapter = new GeofenceAdapter(this::onGeofenceClick);
        binding.recyclerGeofences.setAdapter(adapter);

        // Preparing the map:
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_geofence);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setting up this fragment as an observer of the ViewModels:
        routeViewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            this.route = route;

            // Getting route stored geofences:
            geofenceList = route.getGeofences();
            if (geofenceList == null) {
                geofenceList = new ArrayList<>();
            }

            // Filling in geofences recyclerview:
            adapter.submitList(new ArrayList<>(geofenceList));

            // Managing previous stored 'geofence enabled' status:
            if (!wasGeofenceEnabledCaptured) {
                wasGeofenceEnabled = geofenceList.isEmpty() || route.isGeofenceEnabled();
                wasGeofenceEnabledCaptured = true;
            }

            // Drawing map:
            drawMap();
        });

        milestoneViewModel.getMilestones().observe(getViewLifecycleOwner(), milestoneList -> {
            this.milestones = milestoneList;
            drawMap();
        });

        routeViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                routeViewModel.clearError();
            }
        });

        // Setting UI listeners:
        binding.sliderRadius.addOnChangeListener(this::onRadiusChange);
        binding.buttonDeleteGeofence.setOnClickListener(this::onDelete);

        // Managing back button:
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            /**
             * Actions to do when the user presses the back button.
             * Saves changes and navigates back.
             */
            @Override
            public void handleOnBackPressed() {
                saveAndNavigateBack();
            }
        });

        // Toolbar setup:
        setUpToolbar();

        // Managing UI lock on loading operations:
        observeLoadingStates();
    }


    /**
     * Locks or unlocks the UI depending on the loading data operations status.
     * Performs this fragment as an observer of the 'isLoading' flags of all the DataViewModels.
     */
    private void observeLoadingStates() {

        // Creating a common observer which performs UI lock-unlock:
        Observer<Boolean> loadingObserver = isLoading ->
                UIUtils.updateLoadingState(
                        Arrays.asList(routeViewModel, milestoneViewModel),
                        globalUIViewModel);

        // Launching that common observer when some DataViewModel loading flag changes:
        routeViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        milestoneViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
    }


    /**
     * Performs the action to do when the user taps on a CardView of the bubbles recyclerview.
     * Shows bubble editing controls and focuses the map camera on the bubble.
     *
     * @param geofence The tapped geofence bubble.
     * @param position The position of the bubble on the geofence list.
     */
    private void onGeofenceClick(@NotNull Route.Geofence geofence, int position) {
        currentBubble = position;
        LatLng center = new LatLng(geofence.getCenter().getLatitude(), geofence.getCenter().getLongitude());

        // Showing bubble edit controls:
        binding.layoutEditControls.setVisibility(View.VISIBLE);
        binding.sliderRadius.setValue(geofence.getRadius());
        binding.textRadiusLabel.setText(String.format(Locale.getDefault(), "%dm", geofence.getRadius()));
        updateDragMarker(center);

        // Focusing camera on the bubble:
        map.animateCamera(CameraUpdateFactory.newLatLng(center));
    }


    /**
     * Manages the draggable marker at the center of the currently selected bubble.
     *
     * @param position The LatLng where the drag handle should be placed.
     */
    private void updateDragMarker(LatLng position) {
        if (dragMarker != null) {
            dragMarker.remove();
        }
        BitmapDescriptor dragIcon = MapUtils.getGeofenceCenterBitmapDescriptor(requireContext());
        dragMarker = map.addMarker(new MarkerOptions()
                .position(position)
                .draggable(true)
                .icon(dragIcon)
                .anchor(0.5f, 0.5f)
                .zIndex(100f));
    }


    /**
     * Performs the action to do when de user modifies the bubble radius using the UI radius slider. Includes re-validating milestones coverage.
     *
     * @param slider   The UI radius slider.
     * @param value    The radius value established by user
     * @param fromUser Indicates that the value has been changed by the user (typically true). When false, this method does nothing.
     */
    private void onRadiusChange(Slider slider, float value, boolean fromUser) {
        if (fromUser && currentBubble != -1) {
            int newRadius = (int) value;

            // Updating radius label:
            binding.textRadiusLabel.setText(String.format(Locale.getDefault(), "%dm", newRadius));

            // Updating radius on bubble:
            geofenceList.get(currentBubble).setRadius(newRadius);
            bubbles.get(currentBubble).setRadius(newRadius);

            // Re-validating milestones coverage:
            MapUtils.validateMilestonesCoverage(requireContext(), map, markers, geofenceList);
        }
    }


    /**
     * Performs the actions to do when de user clicks on the 'Delete bubble' button. Includes re-validating milestones coverage.
     *
     * @param view The clicked view (i.e. the 'Delete bubble' button).
     */
    private void onDelete(View view) {
        if (currentBubble != -1) {

            // Removing the bubble:
            bubbles.get(currentBubble).remove();
            bubbles.remove(currentBubble);
            geofenceList.remove(currentBubble);

            // Updating bubble recyclerview:
            adapter.submitList(new ArrayList<>(geofenceList), () -> adapter.notifyItemRangeChanged(0, geofenceList.size()));
            currentBubble = -1;
            adapter.clearSelection();
            binding.layoutEditControls.setVisibility(View.INVISIBLE);


            // Hiding bubble center:
            if (dragMarker != null) {
                dragMarker.remove();
                dragMarker = null;
            }

            // Re-validating milestones coverage:
            MapUtils.validateMilestonesCoverage(requireContext(), map, markers, geofenceList);
        }
    }


    /**
     * Launched when the Google Map is ready. Performs the map appearance and establishes map click listeners.
     *
     * @param googleMap The ready Google Map.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        isMapReady = true;
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.getUiSettings().setMapToolbarEnabled(false);

        // Ignoring clicks on markers:
        map.setOnMarkerClickListener(marker -> true);

        // Drawing a new bubble when click on map:
        map.setOnMapClickListener(this::newBubble);

        // Moving bubbles when center dragging:
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            /**
             * Actions to do when user starts dragging the bubble-center marker. No actions performed.
             * @param marker Bubble-center marker.
             */
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {
            }

            /**
             * Actions to do when the user is dragging the bubble-center marker.
             * Moves the bubble on the map, updates bubble coordinates,
             * and re-validates milestones coverage (delayed on certain period of time to avoid overcharge).
             * @param marker Bubble-center marker.
             */
            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
                // Moving bubble on map and updating coordinates:
                LatLng newPos = marker.getPosition();
                bubbles.get(currentBubble).setCenter(newPos);
                geofenceList.get(currentBubble).setCenter(new GeoPoint(newPos.latitude, newPos.longitude));

                // Re-validating milestones coverage, only on time intervals:
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastValidationTime > VALIDATION_INTERVAL_MS) {
                    MapUtils.validateMilestonesCoverage(requireContext(), map, markers, geofenceList);
                    lastValidationTime = currentTime;
                }
            }

            /**
             * Actions to do when the user ends dragging the bubble-center marker.
             * Enforces a final coverage validation, and visually returns the marker to the center of the bubble.
             * @param marker Bubble-center marker.
             */
            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                GeoPoint geoPoint = geofenceList.get(currentBubble).getCenter();
                LatLng center = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                updateDragMarker(center);
                map.animateCamera(CameraUpdateFactory.newLatLng(center));
                MapUtils.validateMilestonesCoverage(requireContext(), map, markers, geofenceList);
            }
        });

        // Launching map elements drawing:
        drawMap();
    }


    /**
     * Draws elements on the map (milestones, polyline and geofence bubbles). Used when fragment initializes and when user performs some data change.
     */
    private void drawMap() {

        // Checking resources are available:
        if (!isMapReady || milestones == null) {
            return;
        }

        // Clearing previous map data:
        map.clear();

        if (milestones != null && !milestones.isEmpty()) {
            // Drawing milestones:
            markers = MapUtils.drawMilestonesOnMap(requireContext(), map, milestones, true);
            // Drawing polyline:
            if (route.isInOrder()) {
                MapUtils.drawRouteLine(requireContext(), map, milestones);
                MapUtils.addStartAndEnd(requireContext(), map, milestones, null, null);
            }
        }

        // Drawing geofence bubbles:
        bubbles.clear();
        bubbles.addAll(MapUtils.drawGeofence(requireContext(), map, geofenceList, true));

        // Drawing the center of the bubble on edition (when fragment recreating):
        if (currentBubble >= 0) {
            Route.Geofence editingBubble = geofenceList.get(currentBubble);
            updateDragMarker(new LatLng(editingBubble.getCenter().getLatitude(), editingBubble.getCenter().getLongitude()));
        } else {
            dragMarker = null;
        }

        // Checking milestones coverage:
        MapUtils.validateMilestonesCoverage(requireContext(), map, markers, geofenceList);
    }


    /**
     * Adds a new geofence bubble on the map. This method is attached to the OnMapClick event.
     *
     * @param latLng Coordinates in which the user has clicked on map.
     */
    private void newBubble(@NonNull LatLng latLng) {

        // Adding new geofence bubble to the route:
        GeoPoint geoPoint = new GeoPoint(latLng.latitude, latLng.longitude);
        Route.Geofence newGeofence = new Route.Geofence(geoPoint, DEFAULT_RADIUS);
        geofenceList.add(newGeofence);

        // Drawing the new circle on the map:
        Circle circle = map.addCircle(new CircleOptions()
                .center(latLng)
                .radius(DEFAULT_RADIUS)
                .strokeWidth(5f)
                .strokeColor(requireContext().getColor(R.color.deambulario_orange_ultra_dark))
                .fillColor(requireContext().getColor(R.color.deambulario_orange_light) & 0x40FFFFFF));
        bubbles.add(circle);
        updateDragMarker(latLng);
        map.animateCamera(CameraUpdateFactory.newLatLng(latLng));

        // Re-validating the milestones' coverage:
        MapUtils.validateMilestonesCoverage(requireContext(), map, markers, geofenceList);

        // Adding a new CardView to the bubbles recyclerview:
        currentBubble = geofenceList.size() - 1;
        adapter.submitList(new ArrayList<>(geofenceList), () -> {
            binding.recyclerGeofences.smoothScrollToPosition(currentBubble);
            adapter.setSelectedPosition(currentBubble);

            // Showing bubble edition controls:
            binding.layoutEditControls.setVisibility(View.VISIBLE);
            binding.sliderRadius.setValue(DEFAULT_RADIUS);
            binding.textRadiusLabel.setText(String.format(Locale.getDefault(), "%dm", DEFAULT_RADIUS));
        });
    }


    /**
     * Sets up the ToolBar containing only the Back-Save button.
     */
    private void setUpToolbar() {
        requireActivity().addMenuProvider(
                new MenuProvider() {
                    /**
                     * Creates a customized menu on the Toolbar. Non used on this implementation.
                     * @param menu         The Save menu.
                     * @param menuInflater The inflater to be used to inflate the menu.
                     */
                    @Override
                    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                    }

                    /**
                     * Manages the OnClick event on the ToolBar menu button. Tries to save the route.
                     * @param menuItem The ToolBar Save menu.
                     * @return True if the event has been consumed.
                     */
                    @Override
                    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                        if (menuItem.getItemId() == android.R.id.home) {
                            saveAndNavigateBack();
                            return true;
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    /**
     * Save changes on route geofence and navigates back.
     */
    private void saveAndNavigateBack() {
        route.setGeofences(geofenceList);
        if (geofenceList.isEmpty()) {
            route.setGeofenceEnabled(false);
        } else {
            route.setGeofenceEnabled(wasGeofenceEnabled);
        }
        routeViewModel.setSelectedRoute(route);
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