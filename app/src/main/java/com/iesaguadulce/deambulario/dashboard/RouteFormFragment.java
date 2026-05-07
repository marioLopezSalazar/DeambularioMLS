package com.iesaguadulce.deambulario.dashboard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.GeoPoint;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.adapters.MilestoneListAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentRouteFormBinding;
import com.iesaguadulce.deambulario.map_and_location.MapUtils;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Fragment that displays and manages a form to create a new route or edit an existing one.
 *
 * @author Mario López Salazar
 */
public class RouteFormFragment extends Fragment implements MilestoneListAdapter.OnMilestoneDetailClickListener, MilestoneListAdapter.OnMilestoneClickListener, MilestoneListAdapter.OnStartDragListener, OnMapReadyCallback {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentRouteFormBinding binding;

    /*
     * ViewModel to manage global UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;

    /*
     * ViewModel to manage the data access.
     */
    private RouteViewModel routeViewModel;
    private MilestoneViewModel milestoneViewModel;
    private Route currentRoute;

    /*
     * To track if we are editing an existing route or creating a new one.
     */
    private boolean isEditMode = false;

    /*
     * To track where to navigate after the saving operation.
     */
    private enum NavigateTo {
        NONE, BACK, NEW_MILESTONE, DETAIL_MILESTONE, GEOFENCE
    }

    private NavigateTo navigateTo;

    /*
     * Adapter for the milestones list.
     */
    private MilestoneListAdapter milestoneAdapter;
    private ItemTouchHelper itemTouchHelper;

    /*
     * Google Map and variables to display the milestones.
     */
    private GoogleMap map;
    private List<Marker> markers = new ArrayList<>();
    private Polyline routePolyline;
    private Marker startLabelMarker;
    private Marker endLabelMarker;


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
        binding = FragmentRouteFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up the UI and establishes this fragment as an observer of the ViewModel.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-construct from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.switchGeofence.setSaveEnabled(false);

        // Preparing the ToolBar menu:
        setUpToolbar();

        // Setting up the layout's RecyclerView with the MilestoneListAdapter:
        milestoneAdapter = new MilestoneListAdapter(UIUtils.AdapterMode.REORDERABLE, this, this, this);
        binding.recyclerviewMilestones.setAdapter(milestoneAdapter);
        setupDragAndDrop();

        // Preparing the map:
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.route_map_form);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Getting the ViewModels:
        routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Determine mode (Creation vs Edition) and populate
        determineModeAndShowRouteData();

        // Setting up this fragment as an observer of the RouteViewModel:
        routeViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                navigateTo = NavigateTo.NONE;
                Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                routeViewModel.clearError();
            }
        });

        // Setting up this fragment as an observer of the MilestoneViewModel:
        milestoneViewModel.getMilestones().observe(getViewLifecycleOwner(), milestones -> {
            if (milestones != null) {

                // Filling the milestone recyclerview:
                milestoneAdapter.submitList(new ArrayList<>(milestones));
                drawMilestonesAndGeofence(milestones);
            }
        });
        milestoneViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                navigateTo = NavigateTo.NONE;
                Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                milestoneViewModel.clearError();
            }
        });

        // Setting up buttons:
        binding.buttonAddMilestone.setOnClickListener(v -> {
            /* Trying to save route before navigating to milestone fragment.
             * Navigation performed by routeViewModel.isLoading.observe(), launched after saving. */
            if (saveRoute(true)) {
                navigateTo = NavigateTo.NEW_MILESTONE;
            } else {
                navigateTo = NavigateTo.NONE;
            }
        });
        binding.buttonGeofence.setOnClickListener(v -> {
            /* Trying to save route before navigating to geofence fragment.
             * Navigation performed by routeViewModel.isLoading.observe(), launched after saving. */
            if (saveRoute(true)) {
                navigateTo = NavigateTo.GEOFENCE;
            } else {
                navigateTo = NavigateTo.NONE;
            }
        });

        // Performing teacher guide:
        performGuide();

        // Setup 'enter' on level textview:
        UIUtils.setEndFormOnClickListener(requireActivity(), binding.textInputCourse, null, null);

        // Setting a listener on the switches:
        binding.switchInorder.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateRoutePolyline());
        binding.switchGeofence.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateRouteGeofence());


        // Managing back button:
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            /**
             * Actions to do when the user presses the back button.
             */
            @Override
            public void handleOnBackPressed() {
                saveAndBack();
            }
        });

        // Managing UI lock on loading operations:
        observeLoadingStates();
    }


    /**
     * Locks or unlocks the UI depending on the loading data operations status, and manage pending navigations if any.
     */
    private void observeLoadingStates() {

        // Creating a common observer:
        Observer<Boolean> loadingObserver = isLoading -> {

            // Performing the UI lock-unlock:
            UIUtils.updateLoadingState(
                    Arrays.asList(routeViewModel, milestoneViewModel),
                    globalUIViewModel);

            // Managing pending navigation:
            boolean isRouteLoading = Boolean.TRUE.equals(routeViewModel.isLoading().getValue());
            boolean isMilestoneLoading = Boolean.TRUE.equals(milestoneViewModel.isLoading().getValue());
            if (!isRouteLoading && !isMilestoneLoading && navigateTo != NavigateTo.NONE) {
                // Setting this route as the current route:
                routeViewModel.setSelectedRoute(currentRoute);
                // Navigating:
                navigate();
            }
        };

        // Launching that common observer when some DataViewModel loading flag changes:
        routeViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        milestoneViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
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
                            saveAndBack();
                            return true;
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }


    /**
     * Sets up the reorder operation on the milestones recyclerview.
     */
    private void setupDragAndDrop() {

        // Configuring an ItemTouchHelper to manage the user gestures (drag-up and drag-down, but not lateral swipes):
        ItemTouchHelper.SimpleCallback dragCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0
        ) {
            /**
             * Manages the long-press. In this implementation, it does nothing.
             * @return If long-press was managed.
             */
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            /**
             * Performs the actions to do when the user drags-and-drops an element on the recyclerview.
             * @param recyclerView The RecyclerView to which ItemTouchHelper is attached to.
             * @param viewHolder   The ViewHolder which is being dragged by the user.
             * @param target       The ViewHolder over which the currently active item is being dragged.
             * @return True if the action is consumed.
             */
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false;
                }
                milestoneAdapter.swapItem(fromPosition, toPosition);
                return true;
            }

            /**
             * Performs actions when the user left-right swipes an element on the recyclerview. Non actions implemented.
             * @param viewHolder The ViewHolder which has been swiped by the user.
             * @param direction  The direction to which the ViewHolder is swiped.
             */
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            /**
             * Performs the actions to do when de interaction ends. Used to redraw the polyline on the map.
             * @param recyclerView The RecyclerView which is controlled by the ItemTouchHelper.
             * @param viewHolder   The View that was interacted by the user.
             */
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                updateRoutePolyline();
            }
        };
        itemTouchHelper = new ItemTouchHelper(dragCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerviewMilestones);
    }


    /**
     * Used by the adapter when the drag handle is touched.
     *
     * @param viewHolder The ViewHolder that should start dragging.
     */
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }


    /**
     * Performs the navigation to a milestone detail fragment.
     *
     * @param milestone Selected milestone.
     */
    @Override
    public void onMilestoneDetailClick(Milestone milestone) {
        if (milestone == null) {
            return;
        }
        milestoneViewModel.setSelectedMilestone(milestone);
        if (saveRoute(false)) {
            navigateTo = NavigateTo.DETAIL_MILESTONE;
        } else {
            navigateTo = NavigateTo.NONE;
        }
    }


    /**
     * Launched when the Google Map is ready. Performs the map, draws the markers and sets up click listener on them.
     *
     * @param googleMap The ready Google Map.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Performing map appearance:
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.getUiSettings().setAllGesturesEnabled(false);

        // Setting markers click listener:
        map.setOnMarkerClickListener(this::onMarkerClick);

        // Drawing markers and geofence on the map, if loaded:
        map.setOnMapLoadedCallback(() ->
                drawMilestonesAndGeofence(milestoneViewModel.getMilestones().getValue()));
    }


    /**
     * Used to draw the milestones on the map, if exist and are loaded. Additionally, draws the geofence, if established.
     *
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
        if (milestones != null) {
            markers = MapUtils.drawMilestonesOnMap(requireContext(), map, milestones, true);
            if (currentRoute.isInOrder() && markers.size() >= 2) {
                routePolyline = MapUtils.drawRouteLine(requireContext(), map, milestones);
                Marker[] newLabels = MapUtils.addStartAndEnd(requireContext(), map, milestones, startLabelMarker, endLabelMarker);
                startLabelMarker = newLabels[0];
                endLabelMarker = newLabels[1];
            }
        }

        // Drawing geofence, if established:
        if (currentRoute.getGeofences() != null) {
            MapUtils.drawGeofence(requireContext(), map, currentRoute.getGeofences(), currentRoute.isGeofenceEnabled());
            MapUtils.validateMilestonesCoverage(requireContext(), map, markers, currentRoute.getGeofences());
        }
    }


    /**
     * Used to decide if draw a polyline on the map, depending on if the route must be walked on order.
     */
    private void updateRoutePolyline() {
        if (map == null) {
            return;
        }

        // Deleting previous polyline, if exists.
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
            startLabelMarker.remove();
            endLabelMarker.remove();
        }

        // Drawing the polyline, if the route is configured to be walked on order:
        List<Milestone> currentList = milestoneAdapter.getCurrentList();
        if (binding.switchInorder.isChecked() && currentList.size() >= 2) {
            routePolyline = MapUtils.drawRouteLine(requireContext(), map, currentList);
            Marker[] newLabels = MapUtils.addStartAndEnd(requireContext(), map, currentList, startLabelMarker, endLabelMarker);
            startLabelMarker = newLabels[0];
            endLabelMarker = newLabels[1];
        }
    }


    /**
     * Used to decide if draw geofence bubbles on the map, depending on if the geofence is established and/or enabled.
     */
    private void updateRouteGeofence() {
        if (map == null) {
            return;
        }
        // Updating route:
        currentRoute.setGeofenceEnabled(binding.switchGeofence.isChecked());

        // Redrawing the map:
        drawMilestonesAndGeofence(milestoneViewModel.getMilestones().getValue());
    }


    /**
     * Launched when user clicks on a marker of the map. Highlights the milestone on the recyclerview.
     *
     * @param marker Clicked marker.
     * @return True, indicating that the camera doesn't move to the marker.
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

            // Finding milestone position on the adapter:
            int adapterPosition = -1;
            List<Milestone> milestones = milestoneAdapter.getCurrentList();
            for (int i = 0; i < milestones.size() && adapterPosition < 0; i++) {
                if (milestones.get(i).getId().equals(milestone.getId())) {
                    adapterPosition = i;
                }
            }
            // Highlighting CardView:
            if (adapterPosition != -1) {
                binding.recyclerviewMilestones.smoothScrollToPosition(adapterPosition);
                milestoneAdapter.setSelectedPosition(adapterPosition);
            }
        }
        marker.showInfoWindow();

        // NOT moving camera to marker:
        return true;
    }


    /**
     * Launched when user taps a milestone on recyclerview. Highlights the marker on the map.
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

        // Showing infoWindow:
        if (marker != null) {
            marker.showInfoWindow();
        }
    }


    /**
     * Determines if the fragment is in edit mode or creation mode based on the selected route.
     * Fills UI with the information of the current Route, if in edit mode.
     */
    private void determineModeAndShowRouteData() {
        currentRoute = routeViewModel.getSelectedRoute().getValue();

        // EDIT MODE (there is a current route):
        if (currentRoute != null) {
            isEditMode = true;

            // Filling in form fields:
            requireActivity().setTitle(R.string.edit);
            binding.textInputTitle.setText(currentRoute.getTitle());
            binding.textInputCourse.setText(currentRoute.getLevel());
            binding.textInputCurricular.setText(currentRoute.getCurriculum());
            binding.switchInorder.setChecked(currentRoute.isInOrder());
            if (currentRoute.getGeofences() == null || currentRoute.getGeofences().isEmpty()) {
                binding.textGeofenceActive.setEnabled(false);
                binding.switchGeofence.setEnabled(false);
                binding.switchGeofence.setChecked(false);
            } else {
                binding.switchGeofence.setChecked(currentRoute.isGeofenceEnabled());
            }

            // Getting milestones:
            milestoneViewModel.loadMilestones(currentRoute.getId());

        }
        // CREATION MODE (there is NOT a current route):
        else {
            isEditMode = false;
            binding.switchGeofence.setEnabled(false);
            binding.switchGeofence.setChecked(false);

            //New route:
            currentRoute = new Route();
            currentRoute.setGeofences(new ArrayList<>());

            // New milestones list:
            milestoneViewModel.clear();
        }

        // Setting the 'hiding keyboard' after filling in the educative level:
        UIUtils.setEndFormOnClickListener(requireActivity(), binding.textInputCourse, null, null);
    }


    /**
     * Collects data from the form, updates the current route object, and launches the saving operation.
     *
     * @param maskWhenEmpty True indicates that the saving operation depends on the non-empty state of the UI.
     * @return True if the saving operation was launched.
     */
    private boolean saveRoute(boolean maskWhenEmpty) {

        // Getting form values:
        String title = Objects.requireNonNull(binding.textInputTitle.getText()).toString().trim();
        String level = Objects.requireNonNull(binding.textInputCourse.getText()).toString().trim();
        String curriculum = Objects.requireNonNull(binding.textInputCurricular.getText()).toString().trim();

        // Checking if form fields are empty when creating, and avoiding geofence-milestones navigation in that case:
        if (checkEmptyRoute(title, level, curriculum) && !maskWhenEmpty) {
            navigateTo = NavigateTo.BACK;
            navigate();
            return false;
        }

        // Non-empty route title:
        title = UIUtils.validateNotEmpty(binding.inputTitle, getResources().getString(R.string.title_cannot_be_empty));
        if (title == null || title.isBlank()) {
            binding.inputTitle.requestFocus();
            return false;
        }

        // Updating the Route object with form data:
        currentRoute.setTitle(title);
        currentRoute.setLevel(level);
        currentRoute.setCurriculum(curriculum);
        currentRoute.setInOrder(binding.switchInorder.isChecked());
        currentRoute.setGeofenceEnabled(binding.switchGeofence.isChecked());

        // Persisting on EDIT mode:
        if (isEditMode) {
            // Updating route:
            routeViewModel.updateRoute(currentRoute);
            // Setting new milestones order:
            List<Milestone> finalReorderedList = milestoneAdapter.getCurrentList();
            for (int i = 0; i < finalReorderedList.size(); i++) {
                finalReorderedList.get(i).setOrder(i + 1);
            }
            // Updating milestones:
            milestoneViewModel.updateAllMilestonesFromRoute(currentRoute.getId(), finalReorderedList);

        }
        // Persisting on CREATION mode:
        else {
            routeViewModel.saveRoute(currentRoute);
        }

        return true;
    }

    /**
     * Manages the fragment navigation, depending on the user click and the saving result.
     */
    private void navigate() {

        // Managing the navigation:
        if (navigateTo == NavigateTo.BACK) {
            Navigation.findNavController(requireView()).popBackStack();
        } else if (navigateTo == NavigateTo.NEW_MILESTONE) {
            milestoneViewModel.setSelectedMilestone(null);
            globalUIViewModel.setReadOnlyMode(false);
            Navigation.findNavController(requireView()).navigate(R.id.action_route_form_fragment_to_milestone_fragment);
        } else if (navigateTo == NavigateTo.DETAIL_MILESTONE) {
            globalUIViewModel.setReadOnlyMode(false);
            Navigation.findNavController(requireView()).navigate(R.id.action_route_form_fragment_to_milestone_fragment);
        } else if (navigateTo == NavigateTo.GEOFENCE) {
            Navigation.findNavController(requireView()).navigate(R.id.action_route_form_fragment_to_route_geofence_fragment);
        }

        // Consuming the navigation flag:
        navigateTo = NavigateTo.NONE;
    }


    /**
     * Manages 'back' option. Tries to save the route.
     */
    private void saveAndBack() {
        if (saveRoute(false)) {
            navigateTo = NavigateTo.BACK;
        } else {
            navigateTo = NavigateTo.NONE;
        }
    }


    /**
     * Validates if the UI textFields are blank when on CREATION mode.
     *
     * @param title      Title of the route.
     * @param level      Level of the route.
     * @param curriculum Curriculum of the route.
     * @return True if all the three textFields are empty.
     */
    private boolean checkEmptyRoute(String title, String level, String curriculum) {
        List<Milestone> milestones = milestoneViewModel.getMilestones().getValue();
        boolean isMilestonesEmpty = (milestones == null || milestones.isEmpty());

        return (!isEditMode
                && title.isEmpty() && level.isEmpty() && curriculum.isEmpty()
                && (currentRoute.getGeofences() == null || currentRoute.getGeofences().isEmpty())
                && isMilestonesEmpty);
    }


    /**
     * Performs steps 2-7-8-9-10-11-12 of the Teacher guide.
     */
    private void performGuide() {
        Route route = routeViewModel.getSelectedRoute().getValue();

        // STEP 2 - Route basics:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_2_ROUTE_INFO_AND_ADD_MILESTONES_BUTTON) {

            // Filling in the title and educative level:
            UIUtils.animateTyping(
                    new TextInputEditText[]{binding.textInputTitle, binding.textInputCourse, binding.textInputCurricular},
                    new String[]{getString(R.string.guide_route_title), getString(R.string.guide_route_level), getString(R.string.guide_route_curricula)},
                    () -> {
                        // Assuring fragment is alive after filling text fields:
                        if (!isAdded() || isDetached() || getView() == null) return;
                        // Highlighting guide on the 'Add milestone' button:
                        TeacherTourManager.checkCreateRouteTour((TeacherActivity) requireActivity(), binding.buttonAddMilestone, null);
                    });
        }

        // STEP 7 - Milestones list:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_7_MILESTONE_LIST) {

            Milestone milestone = milestoneViewModel.getSelectedMilestone().getValue();
            if (route != null) {
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        // Assuring fragment is alive:
                        if (!isAdded() || isDetached() || getView() == null) return;

                        // Creating two more milestone examples:
                        GeoPoint[] inventedGeopoints = MapUtils.inventMilestones(milestone);
                        if (i == 0) {
                            handler.postDelayed(this, 1000);
                            i++;
                        } else if (i == 1) {
                            Milestone m1 = new Milestone(2, getString(R.string.guide_milestone2), inventedGeopoints[0], null, null);
                            milestoneViewModel.saveMilestone(currentRoute.getId(), m1);
                            i++;
                            handler.postDelayed(this, 500);
                        } else if (i == 2) {
                            Milestone m2 = new Milestone(3, getString(R.string.guide_milestone3), inventedGeopoints[1], null, null);
                            milestoneViewModel.saveMilestone(currentRoute.getId(), m2);
                            i++;
                            handler.postDelayed(this, 500);
                        }

                        // Highlighting some 'Milestone drag' button :
                        else {
                            binding.recyclerviewMilestones.post(() -> {
                                RecyclerView.ViewHolder holder = binding.recyclerviewMilestones.findViewHolderForAdapterPosition(0);
                                if (holder != null) {
                                    View button = holder.itemView.findViewById(R.id.button_drag);
                                    if (button != null) {
                                        TeacherTourManager.checkMilestonesListTour((TeacherActivity) requireActivity(), button, () -> performGuide());
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }

        // STEP 8 - Milestone edition:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_8_MILESTONES_EDITION) {

            // Highlighting some 'Milestone detail' button :
            binding.recyclerviewMilestones.post(() -> {
                RecyclerView.ViewHolder holder = binding.recyclerviewMilestones.findViewHolderForAdapterPosition(1);
                if (holder != null) {
                    View button = holder.itemView.findViewById(R.id.button_detail_arrow);
                    if (button != null) {
                        TeacherTourManager.checkMilestoneEditTour((TeacherActivity) requireActivity(), button, this::performGuide);
                    }
                }
            });
        }

        // STEP 9 - In order route:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_9_IN_ORDER_ROUTE) {

            // Highlighting the 'In order' switch:
            TeacherTourManager.checkRouteInOrderTour((TeacherActivity) requireActivity(), binding.switchInorder, this::performGuide);
        }

        // STEPS 10-11 - Geofence:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_10_GEOFENCE) {

            // Scrolling down to the 'Set geofence' button:
            binding.routeFormScroll.post(() -> {
                binding.routeFormScroll.smoothScrollTo(0, binding.buttonGeofence.getBottom());
                final Handler scrollHandler = new Handler(Looper.getMainLooper());
                scrollHandler.postDelayed(() -> {

                    // Assuring fragment is alive:
                    if (!isAdded() || isDetached() || getView() == null) return;

                    // Highlighting the 'Set geofence' button:
                    TeacherTourManager.checkGeofenceTour((TeacherActivity) requireActivity(), binding.buttonGeofence, () -> {
                        if (route != null) {

                            // Adding an invented geofence:
                            route.setGeofences(MapUtils.inventGeofenceFromMilestones(milestoneViewModel.getMilestones().getValue()));
                            MapUtils.drawGeofence(requireContext(), map, route.getGeofences(), false);
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(() -> {
                                // Assuring fragment is alive:
                                if (!isAdded() || isDetached() || getView() == null) return;

                                // Highlighting the 'Enable geofence' switch:
                                binding.textGeofenceActive.setEnabled(true);
                                binding.switchGeofence.setEnabled(true);
                                TeacherTourManager.checkEnableGeofenceTour((TeacherActivity) requireActivity(), binding.switchGeofence, this::performGuide);
                            }, 500);
                        }
                    });

                }, 500);
            });
        }

        // STEPS 12 - Save route:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_12_AUTOSAVE) {

            // Getting the Toolbar:
            Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                // Highlighting the 'Back' button:
                TeacherTourManager.checkFinishRouteTour((TeacherActivity) requireActivity(), toolbar);
            }
        }
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