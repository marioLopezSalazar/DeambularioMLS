package com.iesaguadulce.deambulario.dashboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.iesaguadulce.deambulario.map_and_location.MapUtils;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.adapters.MilestoneListAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentRouteViewBinding;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.model.repository.callback.ReposSingleCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment that displays the detail of a route.
 *
 * @author Mario López Salazar
 */
public class RouteViewFragment extends Fragment implements MilestoneListAdapter.OnMilestoneDetailClickListener, MilestoneListAdapter.OnMilestoneClickListener, OnMapReadyCallback {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentRouteViewBinding binding;

    /*
     * Adapter to manage the milestones list.
     */
    MilestoneListAdapter milestoneAdapter;

    /*
     * ViewModel to manage global UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;
    private boolean isRouteLocked = true;


    /*
     * ViewModel to manage the data access.
     */
    private RouteViewModel routeViewModel;
    private MilestoneViewModel milestoneViewModel;

    /*
     * Google Map and variables to display the milestones.
     */
    private GoogleMap map;
    private List<Marker> markers = new ArrayList<>();
    private float seeMarkerZoom = -1;
    private boolean isWaitingForMapToSetZoom = false;


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
        binding = FragmentRouteViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Main configurator of the fragment.
     * Prepares the UI and sets upt the fragment with the LiveData.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Preparing the ToolBar menu:
        setUpToolbarMenu();

        // Setting up the layout's RecyclerView with the MilestoneListAdapter:
        milestoneAdapter = new MilestoneListAdapter(UIUtils.AdapterMode.READ_ONLY, this, this, null);
        binding.recyclerviewMilestones.setAdapter(milestoneAdapter);

        // Preparing the map:
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.route_map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Getting the ViewModels:
        routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);


        // Setting up this fragment as an observer of the RouteViewModel:
        routeViewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            if (route != null) {
                // Showing route data on components:
                showRouteData(route);
                // Requesting milestone loading:
                milestoneViewModel.loadMilestones(route.getId());
                // Refreshing map, when returning from route edition:
                drawMilestonesAndGeofence(milestoneViewModel.getMilestones().getValue());
                // Checking if route edition must be locked:
                new ViewModelProvider(requireActivity()).get(SessionViewModel.class)
                        .checkRouteLock(route.getId(), new ReposSingleCallback<>() {
                            @Override
                            public void onSuccess(Boolean locked) {
                                isRouteLocked = locked;
                                requireActivity().invalidateOptionsMenu();
                            }
                            @Override
                            public void onError(Exception e) {
                                isRouteLocked = true;
                                requireActivity().invalidateOptionsMenu();
                            }
                        });

            }
        });
        routeViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                routeViewModel.clearError();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        // Setting up this fragment as an observer of the MilestoneViewModel:
        milestoneViewModel.getMilestones().observe(getViewLifecycleOwner(), milestones -> {

            // Filling the milestone recyclerview:
            if (milestones != null && !milestones.isEmpty()) {
                milestoneAdapter.submitList(new ArrayList<>(milestones));
                binding.recyclerviewMilestones.setVisibility(android.view.View.VISIBLE);
                binding.layoutEmptyStateMilestones.setVisibility(android.view.View.GONE);
            } else {
                binding.layoutEmptyStateMilestones.setVisibility(android.view.View.VISIBLE);
                binding.recyclerviewMilestones.setVisibility(android.view.View.GONE);

            } // Drawing milestones and geofence:
            drawMilestonesAndGeofence(milestones);
        });
        milestoneViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                milestoneViewModel.clearError();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        // Managing UI lock on loading operations:
        observeLoadingStates();

        // Setting 'curricula' button:
        binding.buttonShowCurricula.setOnClickListener(this::seeCurricula);
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
     * Sets up the ToolBar menu containing the Edit and Delete buttons.
     */
    private void setUpToolbarMenu() {
        requireActivity().addMenuProvider(
                new MenuProvider() {

                    /**
                     * Creates the Edit-Delete menu on the Toolbar, and performs the Edit appearance depending on the lock-status of the route.
                     * @param menu         The Edit-Delete menu.
                     * @param menuInflater The inflater to be used to inflate the menu.
                     */
                    @Override
                    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                        menuInflater.inflate(R.menu.menu_editdelete, menu);
                        MenuItem editItem = menu.findItem(R.id.action_edit);
                        if (isRouteLocked) {
                            editItem.setIcon(R.drawable.icon_lock);
                            if (editItem.getIcon() != null) {
                                editItem.getIcon().setTint(ContextCompat.getColor(requireContext(), R.color.gray));
                            }
                            menu.removeItem(R.id.action_delete);
                        }
                    }

                    /**
                     * Manages the OnClick event on the ToolBar menu buttons.
                     * @param menuItem The ToolBar Edit-Delete menu.
                     * @return True if the event has been consumed.
                     */
                    @Override
                    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                        int id = menuItem.getItemId();
                        if (id == R.id.action_edit) {
                            if (isRouteLocked) {
                                Snackbar.make(binding.getRoot(), R.string.route_edition_locked, Snackbar.LENGTH_LONG).show();
                            } else {
                                Navigation.findNavController(requireView()).navigate(R.id.action_route_view_to_route_form);
                            }
                            return true;
                        } else if (id == R.id.action_delete) {
                            confirmDelete();
                            return true;
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }


    /**
     * Performs the navigation to a milestone detail fragment, when user clicks on a recyclerview detail button.
     *
     * @param milestone Selected milestone.
     */
    @Override
    public void onMilestoneDetailClick(Milestone milestone) {
        if (milestone == null) {
            return;
        }
        milestoneViewModel.setSelectedMilestone(milestone);
        globalUIViewModel.setReadOnlyMode(true);
        Navigation.findNavController(requireView()).navigate(R.id.action_route_view_fragment_to_milestone_fragment);
    }


    /**
     * Launched when the Google Map is ready. Performs the map, launches the drawing of markers and
     * sets up click listener on them.
     *
     * @param googleMap The ready Google Map.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Performing map appearance:
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Establishing zoom to show markers when milestone selected on recyclerview:
        map.setOnCameraIdleListener(() -> {
            // Only on the first map performance:
            if (isWaitingForMapToSetZoom && map.getCameraPosition().zoom > 5f) {
                // Requesting full route zoom established when drawing all markers, and zooming-in a little bit:
                seeMarkerZoom = map.getCameraPosition().zoom + 1.5f;
                isWaitingForMapToSetZoom = false;
            }
        });

        // Setting markers click listener:
        map.setOnMarkerClickListener(this::onMarkerClick);

        // Drawing markers and geofence on the map, if loaded:
        map.setOnMapLoadedCallback(() ->
                drawMilestonesAndGeofence(milestoneViewModel.getMilestones().getValue()));
    }


    /**
     * Used to draw the milestones on the map, if exist and are loaded. Additionally, draws the geofence, if established.
     * @param milestones The list of milestones of the route.
     */
    private void drawMilestonesAndGeofence(List<Milestone> milestones) {
        // Checking if resources are available:
        Route currentRoute = routeViewModel.getSelectedRoute().getValue();
        if (map == null || currentRoute == null) {
            return;
        }
        map.clear();

        // When there are milestones:
        if (milestones != null && !milestones.isEmpty()) {
            isWaitingForMapToSetZoom = true;
            markers = MapUtils.drawMilestonesOnMap(requireContext(), map, milestones, true);
            if (currentRoute.isInOrder()) {
                MapUtils.drawRouteLine(requireContext(), map, milestones);
                MapUtils.addStartAndEnd(requireContext(), map, milestones, null, null);
            }
            binding.routeMapView.setVisibility(View.VISIBLE);
        }
        // When there are no milestones:
        else {
            binding.routeMapView.setVisibility(View.GONE);
        }

        // Drawing geofence, if established:
        if(currentRoute.getGeofences() != null){
            MapUtils.drawGeofence(requireContext(), map, currentRoute.getGeofences(), currentRoute.isGeofenceEnabled());
            MapUtils.validateMilestonesCoverage(requireContext(), map, markers, currentRoute.getGeofences());
        }

        // Updating UI data when markers are drawn:
        showRouteData(currentRoute);
    }


    /**
     * Launched when user clicks on a marker of the map. Highlights the milestone on the recyclerview.
     *
     * @param marker Clicked marker.
     * @return False, indicating that the camera moves to the marker.
     */
    private boolean onMarkerClick(Marker marker) {
        // Masking clicks on START and END labels markers:
        if ("START".equals(marker.getTag())) {
            return onMarkerClick(markers.get(0));
        } else if ("END".equals(marker.getTag())) {
            return onMarkerClick(markers.get(markers.size() - 1));
        }

        // Getting the milestone:
        Milestone milestone = (Milestone) marker.getTag();
        if (milestone != null) {
            // Highlighting the marker on the recyclerview:
            binding.recyclerviewMilestones.smoothScrollToPosition(milestone.getOrder() - 1);
            milestoneAdapter.setSelectedPosition(milestone.getOrder() - 1);
        }
        // Moving camera to marker:
        return false;
    }


    /**
     * Launched when user taps a milestone on recyclerview. Highlights the marker on the map.
     *
     * @param milestone The milestone corresponding to the tapped Card View.
     * @param position The position of the milestone in the list.
     */
    @Override
    public void onMilestoneClick(Milestone milestone, int position) {
        if (map == null || markers == null) {
            return;
        }

        // Performing appearance changes on the cards:
        milestoneAdapter.setSelectedPosition(position);

        // Getting the marker whose milestone has been selected on recyclerview:
        Marker marker = null;
        for (int i = 0; i < markers.size() && marker == null; i++) {
            Milestone m = (Milestone) (markers.get(i).getTag());
            if (m != null && m.getId().equals(milestone.getId())) {
                marker = markers.get(i);
            }
        }

        // Performing camera position+zoom:
        if (marker != null) {
            float targetZoom = (seeMarkerZoom > 0) ? seeMarkerZoom : 18f;
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), targetZoom), 1000, null);
            marker.showInfoWindow();
        }
    }


    /**
     * Fills UI TextFields with the information of the current Route.
     *
     * @param route Current route.
     */
    private void showRouteData(Route route) {
        binding.textviewTitle.setText(route.getTitle());
        binding.textviewLevel.setText(route.getLevel());
        String geofenceText;
        boolean hasGeofences = (route.getGeofences() != null) && (!route.getGeofences().isEmpty());

        boolean geofencesCoverAllMilestones = true;
        if (map != null && markers != null && !markers.isEmpty()) {
            geofencesCoverAllMilestones = MapUtils.validateMilestonesCoverage(requireContext(), map, markers, route.getGeofences());
        }

        if (!hasGeofences) {
            geofenceText = getString(R.string.geofence_none);
        } else if (!route.isGeofenceEnabled()) {
            geofenceText = getString(R.string.geofence_inactive);
        } else if (!geofencesCoverAllMilestones) {
            geofenceText = getString(R.string.non_covered_milestones);
        } else {
            geofenceText = getString(R.string.geofence_active);
        }
        binding.textviewGeofence.setText(geofenceText);

        String orderText = route.isInOrder() ? getString(R.string.in_order) : getString(R.string.no_order);
        binding.textviewInOrder.setText(orderText);
    }


    /**
     * Shows a bottom sheet including the curricula information of the route.
     *
     * @param view Button to launch the bottom sheet opening.
     */
    @SuppressLint("InflateParams")
    private void seeCurricula(View view) {
        if (routeViewModel.getSelectedRoute().getValue() == null) {
            return;
        }

        // Creating the bottom sheet content:
        View sheetView = getLayoutInflater().inflate(R.layout.sheet_curricula, null);
        TextView textCurricula = sheetView.findViewById(R.id.bottom_sheet_text_curricula);
        String curriculaText = routeViewModel.getSelectedRoute().getValue().getCurriculum().trim();
        if (curriculaText.isEmpty()) {
            curriculaText = getString(R.string.no_curricula_established);
        }
        textCurricula.setText(curriculaText);

        // Attaching to a bottom sheet:
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }


    /**
     * Shows a confirmation dialog before deleting the route.
     * If confirmation is given, the route is deleted and the UI navigates back to the Route list.
     */
    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_route_title)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(R.string.delete_route_confirmation_message)
                .setNeutralButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {

                    // Deleting the Route and its Milestones:
                    Route route = routeViewModel.getSelectedRoute().getValue();
                    if (route != null) {
                        // KEY POINT: Deleting the milestones before deleting the route (ViewModels are independent):
                        milestoneViewModel.deleteAllMilestonesFromRoute(route.getId(), new ReposVoidCallback() {
                            @Override
                            public void onSuccess() {
                                // Deleting the route:
                                routeViewModel.deleteRoute(route);
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // Navigating back:
                    Navigation.findNavController(requireView()).popBackStack();
                })
                .show();
    }


    /**
     * Called when the Fragment is being destroyed.
     * Avoids view binding memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}