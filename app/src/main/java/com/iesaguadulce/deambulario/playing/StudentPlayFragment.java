package com.iesaguadulce.deambulario.playing;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.StudentActivity;
import com.iesaguadulce.deambulario.databinding.FragmentStudentPlayBinding;
import com.iesaguadulce.deambulario.map_and_location.LocationTrackingService;
import com.iesaguadulce.deambulario.map_and_location.MapUtils;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.repository.SessionRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.notifications.NotifUtils;
import com.iesaguadulce.deambulario.settings.Constants;
import com.iesaguadulce.deambulario.settings.StudentTourManager;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that allows the student to play the session, tracking their location
 * and performing the route milestones.
 */
public class StudentPlayFragment extends Fragment implements OnMapReadyCallback {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentStudentPlayBinding binding;

    /*
     * Reference to the parent Activity.
     */
    private StudentActivity activity;

    /*
     * ViewModels to manage data access and UI states.
     */
    private SessionViewModel sessionViewModel;
    private RouteViewModel routeViewModel;
    private MilestoneViewModel milestoneViewModel;
    private GlobalUIViewModel globalUIViewModel;

    /*
     * Variables holding the current session, route, milestones, and student data.
     */
    private Session currentSession;
    private Route currentRoute;
    private List<Milestone> allMilestones;
    private Student currentStudent;

    /*
     * Variables to manage milestone selection and proximity logic.
     */
    private Milestone nearbyMilestone = null;
    private Milestone selectedMilestone = null;

    /*
     * Map to show the student's location and milestones.
     */
    private GoogleMap map;
    private String lastFocusedMilestoneId = null;


    /*
     * Client and callback to manage real-time location updates.
     */
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    /*
     * Handler to manage milestone automatic de-selection:
     */
    private final Handler deselectionHandler = new Handler(Looper.getMainLooper());
    private Runnable deselectionRunnable;

    /*
     * Geofence radius to consider a milestone as reachable.
     */
    private static final float REACHABLE_MILESTONE_RADIUS = 15.0f;

    /*
     * Time to automatic de-selection a milestone.
     */
    private static final int DESELECTION_TIME = 3000;

    /*
     * Receiver to listen for incoming FCM messages while the app is open.
     */
    private BroadcastReceiver messageReceiver;

    /*
     * Avoids Student Guide re-starting.
     */
    private boolean isGuideShowing = false;


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
        binding = FragmentStudentPlayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up this fragment as an observer of the ViewModels and performs UI initialization.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Performing map space:
        performMapSpace();

        // Getting the activity:
        activity = (StudentActivity) requireActivity();

        // Initializing the ViewModels:
        sessionViewModel = new ViewModelProvider(activity).get(SessionViewModel.class);
        routeViewModel = new ViewModelProvider(activity).get(RouteViewModel.class);
        milestoneViewModel = new ViewModelProvider(activity).get(MilestoneViewModel.class);
        globalUIViewModel = new ViewModelProvider(activity).get(GlobalUIViewModel.class);

        // Performing actions to do when the ViewModel changes:
        observeData();

        // Setting up the toolbar menu and appearance:
        setupToolbar();

        // Performing a listener for the 'Enter to milestone' button:
        binding.buttonViewMilestone.setOnClickListener(v -> {

            // Guessing which milestone is announced on the UI:
            Milestone targetMilestone = null;
            List<String> visitedIds = (currentStudent != null && currentStudent.getVisitedMilestones() != null)
                    ? currentStudent.getVisitedMilestones()
                    : new ArrayList<>();

            // Review previously visited milestone:
            if (selectedMilestone != null && visitedIds.contains(selectedMilestone.getId())) {
                targetMilestone = selectedMilestone;
            }
            // Enter to nearest/next milestone:
            else if (nearbyMilestone != null) {
                targetMilestone = nearbyMilestone;
                if (!visitedIds.contains(targetMilestone.getId())) {
                    visitedIds.add(targetMilestone.getId());
                    UIUtils.playShortSound(requireContext(), R.raw.sound_first_entering);
                    if (currentSession != null) {
                        sessionViewModel.updateStudentVisitedMilestones(currentSession.getId(), visitedIds);
                    }
                }
            }

            // Setting the preferred milestone as the current milestone on ViewModel, and navigating to its content/activities:
            if (targetMilestone != null) {
                stopDeselectionTimer();
                milestoneViewModel.setSelectedMilestone(targetMilestone);
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_student_play_to_student_milestone);
            }
        });


        // Setting up the receiver for incoming messages:
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && NotifUtils.ACTION_NEW_MESSAGE.equals(intent.getAction())) {
                    String message = intent.getStringExtra(NotifUtils.EXTRA_MESSAGE_BODY);
                    if (message != null) {
                        // Checking if it's the first message to clear the "Waiting..." placeholder
                        if (binding.textviewMessageLog.getText().toString().equals(getString(R.string.no_teacher_messages_from_now))) {
                            binding.textviewMessageLog.setText(message);
                        } else {
                            binding.textviewMessageLog.append("\n" + message);
                        }
                        UIUtils.scrollLogToBottom(binding.scrollviewLog);
                    }
                }
            }
        };

        // Requesting map initialization:
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.session_student_map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initializing location client:
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }


    /**
     * Called when the fragment is visible to the user and actively running.
     * Used to start the message receiver and deselect previous visited milestone.
     */
    @Override
    public void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(requireContext(), messageReceiver, new IntentFilter(NotifUtils.ACTION_NEW_MESSAGE), ContextCompat.RECEIVER_NOT_EXPORTED);
        startDeselectionTimer();
    }


    /**
     * Establishes the space occupied by the map on the UI, depending on the preferred font size.
     */
    private void performMapSpace() {
        View mapContainer = binding.sessionStudentMapView;
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mapContainer.getLayoutParams();
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        String fontSize = prefs.getString("pref_student_font_size", "medium");
        switch (fontSize) {
            case "small":
                params.matchConstraintPercentHeight = 0.60f;
                break;
            case "large":
                params.matchConstraintPercentHeight = 0.40f;
                break;
            case "medium":
            default:
                params.matchConstraintPercentHeight = 0.55f;
                break;
        }
        mapContainer.setLayoutParams(params);
    }


    /**
     * Sets up the ToolBar text and the menu containing the Leave Session option.
     */
    private void setupToolbar() {

        // Typing student nick on the toolbar:
        globalUIViewModel.setToolbarTitle(activity.getStudentNick());

        // Setting up the toolbar menu:
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_student, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_student_progress) {
                    TrackBottomFragment bottomSheet = new TrackBottomFragment();
                    Fragment prev = getChildFragmentManager().findFragmentByTag("ProgressBottomSheet");
                    if (prev == null) {
                        bottomSheet.show(getChildFragmentManager(), "ProgressBottomSheet");
                    }
                    return true;
                } else if (menuItem.getItemId() == R.id.action_leave_session) {
                    confirmLeaveSession();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_student_alert) {
                    SOSMessageButton();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_student_settings) {
                    isGuideShowing = false;
                    Navigation.findNavController(binding.getRoot()).navigate(R.id.action_student_play_to_student_settings);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }


    /**
     * Shows a dialog to request confirmation before leaving the session.
     */
    private void confirmLeaveSession() {

        // Checking if the student has finished all tasks:
        int message = isSessionFinished()
                ? R.string.leave_session_confirm
                : R.string.leave_session_confirm_pending;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.leave_session)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(message)
                .setPositiveButton(R.string.leave_session, (dialog, which) -> {

                    // Stopping location service:
                    Intent serviceIntent = new Intent(requireContext(), LocationTrackingService.class);
                    requireContext().stopService(serviceIntent);
                    if (currentSession != null) {
                        // Clearing the local messages log:
                        NotifUtils.clearMessageLog(requireContext(), currentSession.getId());
                        UIUtils.playShortSound(requireContext(), R.raw.sound_logout);
                        // Indicating disconnection on ViewModel:
                        SessionRepository.getInstance().studentAbandonedApp(currentSession.getId(), new ReposVoidCallback() {
                            @Override
                            public void onSuccess() {
                                // Kicking student back to Auth screen:
                                activity.kickStudentToAuth(R.string.kick_as_your_desire);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Anyway, kicking student back to Auth screen:
                                activity.kickStudentToAuth(R.string.kick_as_your_desire);
                            }
                        });
                    } else {
                        // Anyway, kicking student back to Auth screen:
                        activity.kickStudentToAuth(R.string.kick_as_your_desire);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    /**
     * Performs the actions to do when the ViewModels change.
     */
    private void observeData() {

        // When the current session changes:
        sessionViewModel.getCurrentSession().observe(getViewLifecycleOwner(), session -> {
            if (session == null) {
                return;
            }
            this.currentSession = session;

            // If the teacher closed the session:
            if (session.getState() == Session.SessionState.CLOSED) {
                activity.kickStudentToAuth(R.string.session_closed);
                return;
            }

            // Setting up messages log textview and loading previously saved messages:
            String savedLog = NotifUtils.getMessageLog(requireContext(), currentSession.getId());
            if (savedLog != null && !savedLog.isEmpty() && !binding.textviewMessageLog.getText().toString().equals(savedLog)) {
                binding.textviewMessageLog.setText(savedLog);
                UIUtils.scrollLogToBottom(binding.scrollviewLog);
            }

            // Loading full route information:
            if (currentRoute == null || !currentRoute.getId().equals(session.getRouteId())) {
                routeViewModel.loadRouteById(session.getRouteId());
            }
        });

        // When the route full-info is loaded:
        routeViewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            if (route != null) {
                this.currentRoute = route;
                // Requesting route milestones:
                milestoneViewModel.loadMilestones(route.getId());
            }
        });

        // When the route milestones are loaded:
        milestoneViewModel.getMilestones().observe(getViewLifecycleOwner(), milestones -> {
            if (milestones != null) {
                this.allMilestones = milestones;
                updateMapDrawing();
            }
        });

        // When the current student data changes:
        sessionViewModel.getCurrentStudent().observe(getViewLifecycleOwner(), student -> {
            if (student != null) {
                this.currentStudent = student;
                updateMapDrawing();
            }
        });

        // Performing UI lock when data loading:
        Observer<Boolean> loadingObserver = isLoading ->
                UIUtils.updateLoadingState(
                        Arrays.asList(routeViewModel, milestoneViewModel),
                        globalUIViewModel);
        routeViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        milestoneViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
    }


    /**
     * Performs the actions to do when the Google Map is loaded.
     *
     * @param googleMap The loaded Google Map.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;

        // Setting map appearance:
        map.getUiSettings().setMapToolbarEnabled(false);
        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_deambulario));
        } catch (Exception ignored) {
        }
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Showing student location:
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            startLocationTracking();
        } else {
            activity.kickStudentToAuth(R.string.permissions_required_to_play);
            return;
        }

        // Map milestone markers click listener:
        map.setOnMarkerClickListener(marker -> {
            if (marker.getTag() instanceof Milestone) {
                selectedMilestone = (Milestone) marker.getTag();
                milestoneViewModel.setSelectedMilestone(selectedMilestone);
                refreshActionButtonUI();
                marker.showInfoWindow();
                startDeselectionTimer();
            }
            return true;
        });

        // Map click listener to clear milestone selection:
        map.setOnMapClickListener(latLng -> {
            selectedMilestone = null;
            milestoneViewModel.setSelectedMilestone(null);
            refreshActionButtonUI();
            stopDeselectionTimer();
        });

        // Drawing elements on the map:
        updateMapDrawing();
    }


    /**
     * Starts tracking the student's location.
     */
    private void startLocationTracking() {

        // Building the location request:
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LocationTrackingService.LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(LocationTrackingService.LOCATION_UPDATE_INTERVAL)
                .build();

        // Setting a callback launched when we have a new location:
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {

                // Getting the new location:
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLocation = location;

                    // Rotating map:
                    MapUtils.rotateStudentMap(map, currentLocation);

                    // Uploading location to Firestore is made by LocationTrackingService.

                    // Checking distance to next milestones:
                    checkProximityToMilestones();
                }
            }
        };

        // Requesting next location update:
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

            // Starting background sending location service:
            if (currentSession != null && currentStudent != null) {
                Intent serviceIntent = new Intent(requireContext(), LocationTrackingService.class);
                serviceIntent.putExtra(LocationTrackingService.SESSION_ID_BUNDLE, currentSession.getId());
                ContextCompat.startForegroundService(requireContext(), serviceIntent);
            }
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), R.string.location_error, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Draws the milestones and route lines depending on route settings and student progression.
     */
    private void updateMapDrawing() {
        if (map == null || currentRoute == null || allMilestones == null || currentStudent == null) {
            return;
        }

        // Getting previously visited milestones:
        List<String> visitedIds = currentStudent.getVisitedMilestones() != null ? currentStudent.getVisitedMilestones() : new ArrayList<>();

        // Evaluating visible milestones depending on route in-order configuration:
        List<Milestone> visibleMilestones = new ArrayList<>();

        // IN-ORDER ROUTE - Adding visited milestones and the next one milestone:
        if (currentRoute.isInOrder()) {
            boolean stop = false;
            for (int i = 0; i < allMilestones.size() && !stop; i++) {
                visibleMilestones.add(allMilestones.get(i));
                if (!visitedIds.contains(allMilestones.get(i).getId())) {
                    stop = true;
                }
            }
        }
        // FREE ROUTE - Adding all milestones:
        else {
            visibleMilestones.addAll(allMilestones);
        }

        // Drawing markers with specific colors, depending on its visitation status:
        List<Marker> milestoneMarkers = MapUtils.drawMilestonesOnMap(activity, map, visibleMilestones, false);
        for (Marker marker : milestoneMarkers) {
            Milestone m = (Milestone) marker.getTag();
            if (m != null && visitedIds.contains(m.getId())) {
                MapUtils.updateMarkerColor(activity, marker, ContextCompat.getColor(activity, R.color.completed));
            } else {
                MapUtils.updateMarkerColor(activity, marker, ContextCompat.getColor(activity, R.color.pending));
            }
        }

        //Performing Student guide:
        if (!isGuideShowing) {
            performGuide();
        }

        // Re-evaluating milestones proximity:
        checkProximityToMilestones();
    }


    /**
     * Computes distance from current location to unvisited milestones to trigger action button.
     */
    private void checkProximityToMilestones() {
        if (currentLocation == null || allMilestones == null || currentStudent == null) {
            return;
        }

        // Getting previously visited milestones:
        List<String> visitedIds = currentStudent.getVisitedMilestones() != null ? currentStudent.getVisitedMilestones() : new ArrayList<>();

        nearbyMilestone = null;
        Milestone nextTarget = null;
        Milestone nearestMilestone = null;
        Milestone focusTargetMilestone;
        float closestDistance = Float.MAX_VALUE;

        Location mLoc = new Location("");

        // IN-ORDER route - Computing distance to the next milestone:
        if (currentRoute.isInOrder()) {
            if (visitedIds.size() < allMilestones.size()) {
                nextTarget = allMilestones.get(visitedIds.size());
                mLoc.setLatitude(nextTarget.getCoordinates().getLatitude());
                mLoc.setLongitude(nextTarget.getCoordinates().getLongitude());
                closestDistance = currentLocation.distanceTo(mLoc);
                if (closestDistance <= REACHABLE_MILESTONE_RADIUS) {
                    nearbyMilestone = nextTarget;
                }
            }
            focusTargetMilestone = nextTarget;
        }
        // FREE route - Searching the nearest milestone:
        else {
            for (Milestone m : allMilestones) {
                if (!visitedIds.contains(m.getId())) {
                    mLoc.setLatitude(m.getCoordinates().getLatitude());
                    mLoc.setLongitude(m.getCoordinates().getLongitude());
                    float distance = currentLocation.distanceTo(mLoc);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        nearestMilestone = m;
                    }
                }
            }

            // Only activating button if the nearest is <= 15m:
            if (nearestMilestone != null && closestDistance <= REACHABLE_MILESTONE_RADIUS) {
                nearbyMilestone = nearestMilestone;
            }
            focusTargetMilestone = nearestMilestone;
        }

        // Updating UI distance text:
        if (currentRoute.isInOrder()) {
            if (nextTarget != null) {
                binding.textviewNextMilestoneDistance.setText(getString(R.string.distance_to_milestone, (int) closestDistance));
            } else {
                binding.textviewNextMilestoneDistance.setText(getString(R.string.all_milestones_visited));
                if (!globalUIViewModel.isSoundsTrack("finish")) {
                    UIUtils.playShortSound(requireContext(), R.raw.sound_route_done);
                    globalUIViewModel.setSoundsTrack("finish");
                }
            }
        } else {
            if (visitedIds.size() == allMilestones.size()) {
                binding.textviewNextMilestoneDistance.setText(getString(R.string.all_milestones_visited));
                if (!globalUIViewModel.isSoundsTrack("finish")) {
                    UIUtils.playShortSound(requireContext(), R.raw.sound_route_done);
                    globalUIViewModel.setSoundsTrack("finish");
                }
            } else if (nearestMilestone != null) {
                binding.textviewNextMilestoneDistance.setText(getString(R.string.nearest_milestone, nearestMilestone.getName(), (int) closestDistance));
            } else {
                binding.textviewNextMilestoneDistance.setText(R.string.search_next_milestone);
            }
        }

        // Refreshing milestone action button label:
        refreshActionButtonUI();

        // Focusing on the next milestone, if the target has changed:
        if (currentLocation != null && focusTargetMilestone != null) {
            if (lastFocusedMilestoneId == null || !lastFocusedMilestoneId.equals(focusTargetMilestone.getId())) {
                LatLng studentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                MapUtils.focusMapOnStudentAndMilestone(map, studentLatLng, focusTargetMilestone);
                lastFocusedMilestoneId = focusTargetMilestone.getId();
            }
        }
        // Focusing on the full route, when the route is done:
        else if (visitedIds.size() == allMilestones.size() && (lastFocusedMilestoneId == null || !lastFocusedMilestoneId.equals("finish"))) {
            MapUtils.focusMapOnStudentAndRoute(map,
                    currentLocation != null ?
                            new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()) : null,
                    allMilestones);
            lastFocusedMilestoneId = "finish";
        }
    }


    /**
     * Updates the action button visibility and text depending on student proximity or manual selection.
     */
    private void refreshActionButtonUI() {
        if (currentStudent == null) {
            return;
        }

        List<String> visitedIds = currentStudent.getVisitedMilestones() != null ? currentStudent.getVisitedMilestones() : new ArrayList<>();

        // When student selects a visited milestone on the map:
        if (selectedMilestone != null && visitedIds.contains(selectedMilestone.getId())) {
            binding.buttonViewMilestone.setVisibility(View.VISIBLE);
            binding.buttonViewMilestone.setText(getString(R.string.review_milestone, selectedMilestone.getName()));
        }

        // Proximity to an unvisited milestone:
        else if (nearbyMilestone != null) {
            binding.buttonViewMilestone.setVisibility(View.VISIBLE);
            binding.buttonViewMilestone.setText(getString(R.string.enter_milestone, nearbyMilestone.getName()));
            if (!globalUIViewModel.isSoundsTrack(nearbyMilestone.getId())) {
                UIUtils.playShortSound(requireContext(), R.raw.sound_milestone_reached);
                globalUIViewModel.setSoundsTrack(nearbyMilestone.getId());
            }
        }

        // Otherwise, hide the button:
        else {
            binding.buttonViewMilestone.setVisibility(View.GONE);
        }
    }


    /**
     * Computes if the student has finished the route (all milestones visited & all activities done).
     *
     * @return True if the student has finished the route.
     */
    private boolean isSessionFinished() {
        if (currentStudent == null || allMilestones == null || allMilestones.isEmpty()) {
            return false;
        }

        // Counting total activities:
        int totalActivitiesCount = 0;
        for (Milestone m : allMilestones) {
            if (m.getActivities() != null) {
                totalActivitiesCount += m.getActivities().size();
            }
        }

        // Revising student work:
        int visitedCount = (currentStudent.getVisitedMilestones() != null) ?
                currentStudent.getVisitedMilestones().size() : 0;
        int answersCount = (currentStudent.getAnswers() != null) ?
                currentStudent.getAnswers().size() : 0;

        // Checking completion
        return (visitedCount == allMilestones.size()) && (answersCount == totalActivitiesCount);
    }


    /**
     * Performs automatic de-selection of milestone after 5 seconds.
     */
    private void startDeselectionTimer() {
        stopDeselectionTimer();

        // When 5 seconds pass:
        deselectionRunnable = () -> {
            selectedMilestone = null;
            milestoneViewModel.setSelectedMilestone(null);
            refreshActionButtonUI();
        };
        deselectionHandler.postDelayed(deselectionRunnable, DESELECTION_TIME);
    }


    /**
     * Allows to detach automatic de-selection of milestone.
     */
    private void stopDeselectionTimer() {
        if (deselectionRunnable != null) {
            deselectionHandler.removeCallbacks(deselectionRunnable);
        }
    }

    /**
     * Performs the button to send an SOS message from the student to the teacher.
     */
    private void SOSMessageButton() {

        if (currentStudent == null) {
            Toast.makeText(requireContext(), R.string.data_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        // Launching dialog:
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sos)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(R.string.send_sos_message)
                .setPositiveButton(R.string.send, (dialog, which) -> {

                    Session currentSession = sessionViewModel.getCurrentSession().getValue();
                    if (currentSession == null) {
                        Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    } else {
                        // Requesting message sending to the current student:
                        sessionViewModel.sendAlert(currentSession.getId(), currentStudent.getNick());
                        // Adding SOS message to student local log:
                        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                        NotifUtils.appendToMessageLog(requireContext(),
                                time + " - " + getString(R.string.sos_sent_log),
                                currentSession.getId());
                        // Refreshing log:
                        String updatedLog = NotifUtils.getMessageLog(requireContext(), currentSession.getId());
                        if (updatedLog != null && !updatedLog.isEmpty()) {
                            binding.textviewMessageLog.setText(updatedLog);
                            UIUtils.scrollLogToBottom(binding.scrollviewLog);
                        }

                        Toast.makeText(requireContext(), R.string.sos_sent_success, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .show();
    }


    /**
     * Performs the Student Guide.
     */
    private void performGuide() {
        isGuideShowing = true;
        StudentActivity activity = (StudentActivity) requireActivity();
        // STEP 1 - Next milestone indicator.
        if (StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_1_NEXT_MILESTONE_INDICATOR) {
            StudentTourManager.checkMilestoneIndicatorTour(activity, binding.textviewNextMilestoneDistance, this::performGuide);
        }

        // STEP 2 - Milestone action button.
        else if (StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_2_MILESTONE_ACTION_BUTTON) {
            int buttonVisibility = binding.buttonViewMilestone.getVisibility();
            CharSequence buttonText = binding.buttonViewMilestone.getText();
            binding.buttonViewMilestone.setVisibility(View.VISIBLE);
            binding.buttonViewMilestone.setText(R.string.guide_milestone_button_text);
            binding.buttonViewMilestone.post(() ->
                    StudentTourManager.checkMilestoneActionButtonTour(activity, binding.buttonViewMilestone, () -> {
                        binding.buttonViewMilestone.setVisibility(buttonVisibility);
                        binding.buttonViewMilestone.setText(buttonText);
                        performGuide();
                    })
            );
        }

        else if (StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_3_LOG) {
            StudentTourManager.checkMyLogTour(activity, binding.scrollviewLog, this::performGuide);
        }

        // STEPS 4-5-6-7 (involve Toolbar buttons):
        else if (StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_4_MY_PROGRESS ||
                StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_5_SETTINGS ||
                StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_6_SEND_ALERT ||
                StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_7_LOGOUT) {

            Toolbar toolbar = activity.findViewById(R.id.studentToolbar);
            if (toolbar != null) {
                toolbar.post(() -> {
                    // Getting the 'More options' button:
                    ArrayList<View> outViews = new ArrayList<>();
                    String overflowDescription = getString(androidx.appcompat.R.string.abc_action_menu_overflow_description);
                    toolbar.findViewsWithText(outViews, overflowDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                    View overflowMenuButton = outViews.isEmpty() ? null : outViews.get(0);

                    // STEP 4 - See my progress.
                    if (StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_4_MY_PROGRESS) {
                        View toolbarButton = activity.findViewById(R.id.action_student_progress);
                        if (toolbarButton != null) {
                            StudentTourManager.checkMyProgressTour(activity, toolbarButton, this::performGuide);
                        } else if (overflowMenuButton != null) {
                            StudentTourManager.checkMyProgressTour(activity, overflowMenuButton, this::performGuide);
                        }
                    }

                    // STEP 5 - Profile settings.
                    else if (StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_5_SETTINGS) {
                        View toolbarButton = activity.findViewById(R.id.action_student_settings);
                        if (toolbarButton != null) {
                            StudentTourManager.checkStudentProfileTour(activity, toolbarButton, this::performGuide);
                        } else if (overflowMenuButton != null) {
                            StudentTourManager.checkStudentProfileTour(activity, overflowMenuButton, this::performGuide);
                        }
                    }

                    // STEP 6 - Send alert.
                    else if (StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_6_SEND_ALERT) {
                        View toolbarButton = activity.findViewById(R.id.action_student_alert);
                        if (toolbarButton != null) {
                            StudentTourManager.checkSendAlertTour(activity, toolbarButton, this::performGuide);
                        } else if (overflowMenuButton != null) {
                            StudentTourManager.checkSendAlertTour(activity, overflowMenuButton, this::performGuide);
                        }
                    }

                    // STEP 7 - Logout.
                    else if (StudentTourManager.getCurrentStep() == StudentTourManager.TourStep.STEP_7_LOGOUT) {
                        View toolbarButton = activity.findViewById(R.id.action_leave_session);
                        if (toolbarButton != null) {
                            StudentTourManager.checkLogoutTour(activity, toolbarButton, this::performGuide);
                        } else if (overflowMenuButton != null) {
                            StudentTourManager.checkLogoutTour(activity, overflowMenuButton, this::performGuide);
                        }
                    }
                });
            }
        }
    }


    /**
     * Called when the view is going to be destroyed.
     * Used to stop de-selection timer, notifications' receiver and location tracking.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stopping milestone de-selection timer:
        stopDeselectionTimer();

        // Stopping notifications' receiver:
        if (messageReceiver != null) {
            requireContext().unregisterReceiver(messageReceiver);
        }

        // Cleaning camera re-focus to next milestone:
        currentLocation = null;
        lastFocusedMilestoneId = null;

        // Stopping location tracking:
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Intent serviceIntent = new Intent(requireContext(), LocationTrackingService.class);
        requireContext().stopService(serviceIntent);

        binding = null;
    }
}