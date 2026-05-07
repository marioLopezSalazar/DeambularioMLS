package com.iesaguadulce.deambulario.playing;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.adapters.StudentListAdapter;
import com.iesaguadulce.deambulario.databinding.DialogMessageLogBinding;
import com.iesaguadulce.deambulario.databinding.DialogSendMessageBinding;
import com.iesaguadulce.deambulario.databinding.FragmentSessionTrackBinding;
import com.iesaguadulce.deambulario.map_and_location.MapUtils;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.repository.SessionRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.notifications.FCMTokenCallback;
import com.iesaguadulce.deambulario.notifications.NotifUtils;
import com.iesaguadulce.deambulario.settings.Constants;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment that allows user to track the session development.
 *
 * @author Mario López Salazar
 */
public class SessionTrackFragment extends Fragment implements OnMapReadyCallback, StudentListAdapter.OnStudentClickListener {

    /**
     * Minutes to consider that a student is without connection.
     */
    public static final int MAX_DELAY_MINUTES = 2;

    /*
     * Students markers on the map.
     */
    private final Map<String, Marker> studentMarkers = new HashMap<>();

    /*
     * Objects to schedule periodic revisions of the students position and status from a different thread.
     */
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentSessionTrackBinding binding;

    /*
     * ViewModel to manage the data access.
     */
    private SessionViewModel sessionViewModel;
    private RouteViewModel routeViewModel;
    private MilestoneViewModel milestoneViewModel;
    private Session currentSession;
    private Route route;
    private List<Milestone> milestones;

    /*
     * ViewModel to manage global UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;

    /*
     * Variables to manage the message log.
     */
    private BroadcastReceiver messageReceiver;
    private AlertDialog messageLogDialog;
    private TextView dialogMessageTextView;
    private boolean hasUnreadMessages = false;

    /*
     * Map to show the students and milestones location.
     */
    private GoogleMap map;

    /*
     * Adapter to manage the students list.
     */
    private StudentListAdapter adapter;
    private Student tappedStudent = null;

    /*
     * Variables to check the route completion from students.
     */
    private int totalMilestonesCount = 0;
    private int totalActivitiesCount = 0;
    private final Runnable statusRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            // Updating students list and markers:
            refreshStudentOnFragment(sessionViewModel.getStudents().getValue());
            // Scheduling the next revision:
            timerHandler.postDelayed(this, 5000);
        }
    };


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
        binding = FragmentSessionTrackBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up this fragment as an observer of the ViewModel and performs the UI.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initializing the ViewModels:
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Getting session ID from SharedPreferences (if the sessionViewModel was destroyed during app stop)
        if (sessionViewModel.getCurrentSession().getValue() == null) {
            SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
            String savedSessionId = prefs.getString(Constants.KEY_SESSION, null);
            if (savedSessionId != null) {
                sessionViewModel.startListeningToSession(savedSessionId);
            }
        }

        // Setting un ap adapter for the students recyclerview (not on lobby mode):
        adapter = new StudentListAdapter(false, this);
        binding.recyclerviewTrackStudents.setAdapter(adapter);

        // Requesting map initializing:
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.session_map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setting up the toolbar:
        setupToolbar();

        // Performing actions to do when the ViewModel changes:
        observeData();

        // Setting up the receiver for incoming messages:
        setupBroadcastReceiver();
    }


    /**
     * Called when the fragment is visible to the teacher.
     * Used to start the periodic students revisions and start the message receiver.
     */
    @Override
    public void onResume() {
        super.onResume();
        timerHandler.post(statusRefreshRunnable);
        ContextCompat.registerReceiver(requireContext(), messageReceiver, new IntentFilter(NotifUtils.ACTION_NEW_MESSAGE), ContextCompat.RECEIVER_NOT_EXPORTED);
    }


    /**
     * Performs the actions to do when the ViewModel changes.
     */
    private void observeData() {

        // When the current session changes:
        sessionViewModel.getCurrentSession().observe(getViewLifecycleOwner(), session -> {
            this.currentSession = session;
            if (session != null && session.getId() != null) {

                // Showing session PIN:
                binding.textSessionPin.setText(String.format(Locale.getDefault(), getString(R.string.pin_on_track), session.getPin()));

                // Saving session ID on SharedPreferences:
                SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
                prefs.edit().putString(Constants.KEY_SESSION, session.getId()).apply();

                // Saving FCM token on Firebase:
                NotifUtils.fetchFCMToken(new FCMTokenCallback() {
                    @Override
                    public void onTokenReceived(String token) {
                        NotifUtils.saveLocalFCMToken(requireContext(), token);
                        SessionRepository.getInstance().updateTeacherToken(session.getId(), token,
                                new ReposVoidCallback() {
                                    @Override
                                    public void onSuccess() {
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Toast.makeText(requireContext(), R.string.notifications_unavailable, Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(), R.string.notifications_unavailable, Toast.LENGTH_SHORT).show();
                    }
                });

                // When session starts:
                if (session.getState() == Session.SessionState.ACTIVE) {

                    // Performing Teacher guide:
                    if (TeacherTourManager.onGuideMode()) {
                        performGuide();
                    }
                    // When not on Teacher Guide, start listening to students objects changes:
                    else {
                        sessionViewModel.startListeningToStudents(session.getId());
                    }

                    // Loading full route information (to allow user to consult it):
                    route = routeViewModel.getSelectedRoute().getValue();
                    if (route == null || !route.getId().equals(session.getRouteId())) {
                        routeViewModel.loadRouteById(session.getRouteId());
                    }

                    // Getting the number of route activities (in order to perform students route-ending status):
                    totalActivitiesCount = session.getActivitiesSnapshot().size();
                }

                // When session has been required to be closed:
                else if (session.getState() == Session.SessionState.CLOSED) {
                    // Clearing the local messages log:
                    NotifUtils.clearMessageLog(requireContext(), currentSession.getId());
                    // Navigating out to route list:
                    Navigation.findNavController(requireView()).popBackStack(R.id.route_list_fragment, false);
                }
            }
        });

        // When students objects change:
        // Updating students list and markers:
        sessionViewModel.getStudents().observe(getViewLifecycleOwner(), this::refreshStudentOnFragment);

        // When the route full-info is loaded:
        routeViewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            if (route != null) {
                this.route = route;
                // Requesting route milestones:
                milestoneViewModel.loadMilestones(route.getId());
                // Drawing milestones, polyline and geofence on the map:
                drawStaticMapElements();
            }
        });

        // When the route milestones are loaded:
        milestoneViewModel.getMilestones().observe(getViewLifecycleOwner(), milestones -> {
            if (milestones != null) {
                this.milestones = milestones;
                // Getting the number of route milestones (in order to perform students route-ending status):
                totalMilestonesCount = milestones.size();
                // Drawing milestones, polyline and geofence on the map:
                drawStaticMapElements();
            }
        });

        // Performing UI lock when data loading:
        Observer<Boolean> loadingObserver = isLoading ->
                UIUtils.updateLoadingState(
                        Arrays.asList(routeViewModel, milestoneViewModel, sessionViewModel),
                        globalUIViewModel
                );
        routeViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        milestoneViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        sessionViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);

    }


    /**
     * Sets up the ToolBar menu containing the Message log, Broadcast message, Cancel session and Route details options.
     */
    private void setupToolbar() {
        requireActivity().addMenuProvider(new MenuProvider() {

            /**
             * Creates the Broadcast-Cancel-Detail menu on the Toolbar.
             * @param menu         The Log-Broadcast-Cancel-Detail menu.
             * @param menuInflater The inflater to be used to inflate the menu.
             */
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_session, menu);
            }

            /**
             * Updates the menu with a budge, when a message is received.
             * @param menu The Log-Broadcast-Cancel-Detail menu.
             */
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuItem logItem = menu.findItem(R.id.log);
                if (logItem != null) {
                    if (hasUnreadMessages) {
                        logItem.setIcon(R.drawable.icon_new_notification);
                    } else {
                        logItem.setIcon(R.drawable.icon_notification);
                    }
                }
            }

            /**
             * Manages the OnClick event on the ToolBar menu buttons.
             * @param menuItem The ToolBar Log-Broadcast-Cancel-Detail menu.
             * @return True if the event has been consumed.
             */
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.log) {
                    showMessageLogDialog();
                    return true;
                } else if (menuItem.getItemId() == R.id.broadcast) {
                    sendBroadcast();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_route_details) {
                    Navigation.findNavController(requireView()).navigate(R.id.action_session_track_to_route_view);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_end_session) {
                    confirmEndSession();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }


    /**
     * Sets up the receiver for incoming students messages.
     */
    private void setupBroadcastReceiver() {
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NotifUtils.ACTION_NEW_MESSAGE.equals(intent.getAction())) {

                    if (currentSession == null) {
                        return;
                    }

                    // If the log alert dialog is open, update its content:
                    if (dialogMessageTextView != null) {
                        String fullLog = NotifUtils.getMessageLog(requireContext(), currentSession.getId());
                        dialogMessageTextView.setText(fullLog != null
                                ? fullLog
                                : getString(R.string.no_messages_yet));
                        ScrollView scrollView = (ScrollView) dialogMessageTextView.getParent();
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    }
                    // If the log alert dialog is closed, show a budge on the toolbar:
                    else {
                        hasUnreadMessages = true;
                        requireActivity().invalidateOptionsMenu();
                    }
                }
            }
        };
    }


    /**
     * Performs the actions to do when the Google Map is loaded.
     *
     * @param googleMap The loaded Google Map.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Setting map appearance:
        map.getUiSettings().setMapToolbarEnabled(false);
        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_deambulario));
        } catch (Exception ignored) {
        }
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        map.setOnMarkerClickListener(marker -> {

            // Student markers click listener:
            if (marker.getTag() instanceof Student) {

                // Attaching the student object to the market tag:
                Student student = ((Student) marker.getTag());

                if (tappedStudent == null || !tappedStudent.equals(student)) {
                    // Remarking and selecting the student on the recyclerview:
                    tappedStudent = student;
                    adapter.setSelectedStudentId(student.getId());
                    int position = adapter.getStudentPosition(student.getId());
                    if (position != -1) {
                        binding.recyclerviewTrackStudents.smoothScrollToPosition(position);
                    }
                    marker.showInfoWindow();
                } else {
                    //Second time on student selection:
                    openProgressBottomSheet(student);
                }
                return true;
            }

            // Milestone markers click listener (no actions done):
            return false;
        });

        // Showing teacher marker (if permission allowed on login):
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        }

        // Drawing milestones, polyline and geofence on the map:
        drawStaticMapElements();
    }


    /**
     * Draws the milestones, polyline and geofence on the map. Also refreshes students markers, if available.
     */
    private void drawStaticMapElements() {
        if (map == null || route == null || milestones == null) {
            return;
        }

        map.clear();
        studentMarkers.clear();

        // Drawing milestones:
        MapUtils.drawMilestonesOnMap(requireContext(), map, milestones, true);

        // If the route must be walked on order, drawing the polyline and the 'start'-'end' labels:
        if (route.isInOrder()) {
            MapUtils.drawRouteLine(requireContext(), map, milestones);
            MapUtils.addStartAndEnd(requireContext(), map, milestones, null, null);
        }

        // If the route has geofence established and enabled, drawing bubbles on the map:
        if (route.isGeofenceEnabled() && route.getGeofences() != null) {
            MapUtils.drawGeofence(requireContext(), map, route.getGeofences(), route.isGeofenceEnabled());
        }

        // Updating students list and markers:
        refreshStudentOnFragment(sessionViewModel.getStudents().getValue());

    }


    /**
     * Draws or updates the students markers positions and appearance (depending on the student status) on the map.
     *
     * @param students List of students objects.
     */
    private void updateStudentMarkers(List<Student> students) {
        if (map == null) {
            return;
        }

        // Covering all students:
        for (Student student : students) {
            Student.LiveStatus status = student.getLiveStatus();
            if (status != null) {

                // Removing disconnected or abandoned students from map:
                if (student.getLocation() == null || student.getLocation().getCoordinates() == null) {
                    if (studentMarkers.containsKey(student.getId())) {
                        Marker marker = studentMarkers.get(student.getId());
                        if (marker != null) {
                            marker.remove();
                        }
                        studentMarkers.remove(student.getId());
                    }
                } else {
                    // Getting the student position:
                    LatLng position = new LatLng(student.getLocation().getCoordinates().getLatitude(), student.getLocation().getCoordinates().getLongitude());

                    // Building the BitMapDescriptor for the icon, depending on the status:
                    BitmapDescriptor icon = MapUtils.getStudentBitmapDescriptor(requireContext(), student.getLiveStatus());

                    // If student previously appeared on map, only update the position and the icon:
                    if (studentMarkers.containsKey(student.getId())) {
                        Marker marker = studentMarkers.get(student.getId());
                        if (marker != null) {
                            marker.setPosition(position);
                            marker.setIcon(icon);
                        }
                    }
                    // If student wasn't on map, adding its marker:
                    else {
                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(position)
                                .title(student.getNick())
                                .icon(icon)
                                .zIndex(100.0f)
                        );
                        if (marker != null) {
                            marker.setTag(student);
                            studentMarkers.put(student.getId(), marker);
                        }
                    }
                }
            }
        }
    }


    /**
     * Computes current status for each student and refreshes students list on the recyclerview and markers on the map.
     *
     * @param students Current list of students.
     */
    private void refreshStudentOnFragment(List<Student> students) {

        // No students' indicator:
        if (students == null || students.isEmpty()) {
            binding.recyclerviewTrackStudents.setVisibility(View.GONE);
            binding.layoutEmptyStateStudents.setVisibility(View.VISIBLE);
            return;
        }
        binding.recyclerviewTrackStudents.setVisibility(View.VISIBLE);
        binding.layoutEmptyStateStudents.setVisibility(View.GONE);

        // Creating a new list of students (for recyclerview refreshing):
        List<Student> clonedStudents = new ArrayList<>();
        for (Student s : students) {
            clonedStudents.add(new Student(s));
        }

        // Computing and injecting LiveStatus into each student object:
        computeStatuses(clonedStudents);

        // Sorting students using the priority logic (when same priority, ordering by alphabetic):
        clonedStudents.sort((s1, s2) -> {
            int priority1 = getStudentPriority(s1);
            int priority2 = getStudentPriority(s2);
            if (priority1 != priority2) {
                return Integer.compare(priority1, priority2);
            }
            String nick1 = s1.getNick() != null ? s1.getNick() : "";
            String nick2 = s2.getNick() != null ? s2.getNick() : "";
            return nick1.compareToIgnoreCase(nick2);
        });

        // Updating the recyclerview adapter:
        adapter.submitList(clonedStudents);

        // Updating the students markers:
        updateStudentMarkers(clonedStudents);

        // Updating current student:
        Student current = sessionViewModel.getCurrentStudent().getValue();
        if (current != null) {
            int index = students.indexOf(current);
            if (index != -1) {
                sessionViewModel.setCurrentStudent(students.get(index));
            }
        }
    }


    /**
     * Computes the status of each student depending on location, token, and progression.
     *
     * @param students Current list of students.
     */
    private void computeStatuses(List<Student> students) {
        if (students == null) {
            return;
        }

        // Getting the current time:
        long currentTime = System.currentTimeMillis();

        // Time to consider that the student is without connection:
        long maxDelay = MAX_DELAY_MINUTES * 60000;

        for (Student student : students) {

            // --> Student progress:
            boolean isFinished = checkIfFinished(student);

            // --> Out of geofence:
            boolean isOutOfGeofence = false;
            if (student.getLocation() != null && student.getLocation().getCoordinates() != null) {
                if (route != null && route.isGeofenceEnabled() && route.getGeofences() != null) {
                    LatLng studentPos = new LatLng(
                            student.getLocation().getCoordinates().getLatitude(),
                            student.getLocation().getCoordinates().getLongitude());

                    isOutOfGeofence = !MapUtils.validatePointCoverage(studentPos, route.getGeofences());
                }
            }

            // --> Connection:
            Student.LiveStatus.Connection connection;

            // Location not available:
            if (student.getLocation() == null || student.getLocation().getCoordinates() == null) {
                // When neither location nor FCM token, the status is ABANDONED (voluntary logout):
                if (student.getFcmToken() == null || student.getFcmToken().isEmpty()) {
                    connection = Student.LiveStatus.Connection.ABANDONED;
                }
                // When no location but FCM token available, the status is DISCONNECTED (app closed):
                else {
                    connection = Student.LiveStatus.Connection.DISCONNECTED;
                }
            }
            // Location available:
            else {
                Date timestampDate = student.getLocation().getTimestamp();

                // When location delay, the status is LOST_SIGNAL:
                long studentTimestamp = (timestampDate != null) ? timestampDate.getTime() : 0;
                if (currentTime - studentTimestamp > maxDelay) {
                    connection = Student.LiveStatus.Connection.LOST_SIGNAL;
                }
                // When location on time, the status is ONLINE:
                else {
                    connection = Student.LiveStatus.Connection.ONLINE;
                }
            }

            // Injecting the computed status into the POJO:
            student.setLiveStatus(new Student.LiveStatus(connection, isFinished, isOutOfGeofence));
        }
    }


    /**
     * Computes if a student has walked all the milestones and done all the activities.
     *
     * @param student The student to compute his/her finishing status.
     * @return True if the student has walked all the milestones and done all the activities, false otherwise.
     */
    private boolean checkIfFinished(Student student) {
        if (totalMilestonesCount == 0) {
            return false;
        }

        // Checking visited milestones:
        int visitedCount = (student.getVisitedMilestones() != null) ?
                student.getVisitedMilestones().size() : 0;

        // Checking answered questions:
        int answersCount = (student.getAnswers() != null) ?
                student.getAnswers().size() : 0;

        return (visitedCount == totalMilestonesCount) && (answersCount == totalActivitiesCount);
    }


    /**
     * Internal method used to order the students cards, depending on their statuses.
     * Converts the status into a priority order.
     *
     * @return The priority number to show the card respect of the top of the recyclerview.
     */
    static int getStudentPriority(@NonNull Student student) {
        Student.LiveStatus status = student.getLiveStatus();
        if (status == null) {
            return 99;  // Shown on the bottom if not computed yet
        }

        // Priority 1: URGENT - Out of bounds
        if (status.isOutOfGeofence())
            return 1;

        // Priority 2: WARNING - Signal lost but still supposed to be walking
        if (status.getConnection() == Student.LiveStatus.Connection.LOST_SIGNAL)
            return 2;

        // Priority 3: NORMAL - Walking fine
        if (status.getConnection() == Student.LiveStatus.Connection.ONLINE && !status.isFinished())
            return 3;

        // Priority 4: SUCCESS - Finished tasks (whether app is open or not)
        if (status.isFinished())
            return 4;

        // Priority 5: OFFLINE - App closed
        if (status.getConnection() == Student.LiveStatus.Connection.DISCONNECTED)
            return 5;

        // Priority 6: GONE - Abandoned voluntarily
        if (status.getConnection() == Student.LiveStatus.Connection.ABANDONED)
            return 6;

        return 99;
    }


    /**
     * Opens an AlertDialog containing the message log.
     */
    private void showMessageLogDialog() {
        Context context = requireContext();
        if (currentSession == null) {
            Toast.makeText(context, R.string.data_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        // Creating the layout:
        DialogMessageLogBinding logBinding = DialogMessageLogBinding.inflate(getLayoutInflater());
        dialogMessageTextView = logBinding.textMessageLog;

        // Getting the log:
        String log = NotifUtils.getMessageLog(context, currentSession.getId());
        if (log == null || log.isEmpty()) {
            dialogMessageTextView.setText(R.string.no_messages_yet);
        } else {
            dialogMessageTextView.setText(log);
        }

        // Creating the AlertDialog:
        messageLogDialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.message_log)
                .setIcon(R.mipmap.icon_deambulario)
                .setView(logBinding.getRoot())
                .setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialog -> {
                    messageLogDialog = null;
                    dialogMessageTextView = null;
                })
                .create();
        messageLogDialog.show();
        logBinding.getRoot().post(() -> logBinding.getRoot().fullScroll(View.FOCUS_DOWN));

        // Hiding toolbar advise:
        hasUnreadMessages = false;
        requireActivity().invalidateOptionsMenu();
    }


    /**
     * Creates a dialog to allow the teacher send a broadcast notification to all the students.
     */
    private void sendBroadcast() {

        // Creating custom dialog:
        DialogSendMessageBinding dialogBinding = DialogSendMessageBinding.inflate(getLayoutInflater());

        // Launching dialog:
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.broadcast_message)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(R.string.broadcast_advise)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.send_message, (dialog, which) -> {

                    String message = UIUtils.validateNotEmpty(dialogBinding.textInputLayoutMessage, getString(R.string.empty_message));
                    if (message != null) {
                        // Requesting message sending:
                        sessionViewModel.sendTeacherMessage(currentSession.getId(), "todos", message);
                        // Adding message to local log:
                        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                        NotifUtils.appendToMessageLog(requireContext(),
                                time + " - " + R.string.broadcast_message + ": " + message,
                                currentSession.getId());
                        Toast.makeText(requireContext(), R.string.message_sent, Toast.LENGTH_SHORT).show();


                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .show();
    }


    /**
     * Shows a dialog to request confirmation to end the current session.
     */
    private void confirmEndSession() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.end_session)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(R.string.end_session_track)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (currentSession != null) {
                        // Requesting the ViewModel to close the current session:
                        sessionViewModel.closeSession(currentSession.getId());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    /**
     * Performs the actions to do when the user taps on a student card in the recyclerview.
     * On the first time, highlights the card and focus the map on the student's marker.
     * On the second time, opens the progression bottom sheet.
     *
     * @param student The student whose card has been tapped.
     */
    @Override
    public void onStudentCardClick(@NonNull Student student) {

        // First time tapped:
        if (tappedStudent == null || !tappedStudent.equals(student)) {
            tappedStudent = student;

            // Highlighting the card on the recyclerview:
            adapter.setSelectedStudentId(student.getId());

            // Focusing the map on the student's marker:
            Marker marker = studentMarkers.get(student.getId());
            if (marker != null && map != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 18f));
                marker.showInfoWindow();
            }
        }

        // Second time tapped:
        else {
            openProgressBottomSheet(student);
        }
    }


    /**
     * Used to show a bottom sheet containing the student progression on the route.
     *
     * @param student The selected student.
     */
    private void openProgressBottomSheet(Student student) {
        // Refreshing the student info:
        List<Student> currentStudents = sessionViewModel.getStudents().getValue();
        Student freshStudent = student;
        if (currentStudents != null) {
            int index = currentStudents.indexOf(student);
            if (index != -1) {
                freshStudent = currentStudents.get(index);
            }
        }
        // Establishing the current student on the ViewModel:
        sessionViewModel.setCurrentStudent(freshStudent);
        // Opening the bottom sheet (avoiding re-opening):
        TrackBottomFragment bottomSheet = new TrackBottomFragment();
        Fragment prev = getChildFragmentManager().findFragmentByTag("ProgressBottomSheet");
        if (prev == null) {
            bottomSheet.show(getChildFragmentManager(), "ProgressBottomSheet");
        }
    }


    /**
     * Actions to do when the teacher clicks on the student card button. No usage on this implementation.
     *
     * @param student The student whose button has been clicked (button hidden here).
     */
    @Override
    public void onStudentButtonClick(Student student) {
    }


    /**
     * Performs the actions to do when the user clicks on the out-of-geofence button in the card of a student.
     * Used to show an alert dialog to offer the possibility to send a requirement message to the student.
     *
     * @param student The student whose button has been clicked.
     */
    @Override
    public void onStudentOutOfGeofenceClick(Student student) {

        String message = getString(R.string.out_of_geofence_message);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.geofence_alert_title))
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(getString(R.string.geofence_alert_message, student.getNick()))
                .setPositiveButton(R.string.send_message, (dialog, which) -> {

                    // Sending message:
                    sessionViewModel.sendTeacherMessage(currentSession.getId(), student.getId(), message);
                    // Adding message to teacher local log:
                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                    NotifUtils.appendToMessageLog(requireContext(),
                            time + " - " + student.getNick() + ": " + message,
                            currentSession.getId());
                    Toast.makeText(requireContext(), R.string.message_sent, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    /**
     * Performs steps 16-17-18-19-20 of the Teacher guide.
     */
    private void performGuide() {

        // STEP 16 - Session track:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_16_SESSION_TRACK) {

            // Getting first student card:
            binding.recyclerviewTrackStudents.postDelayed(() -> {
                binding.recyclerviewTrackStudents.scrollToPosition(0);
                binding.recyclerviewTrackStudents.post(() -> {
                    RecyclerView.ViewHolder holder = binding.recyclerviewTrackStudents.findViewHolderForAdapterPosition(0);
                    if (holder != null) {
                        TeacherTourManager.checkStudentStatusTour((TeacherActivity) requireActivity(), holder.itemView, this::performGuide);
                    }
                });
            }, 200);
        }

        // STEP 17 - Student track:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_17_PROGRESSION) {

            // Getting done student card:
            binding.recyclerviewTrackStudents.postDelayed(() -> {
                binding.recyclerviewTrackStudents.scrollToPosition(4);
                binding.recyclerviewTrackStudents.post(() -> {
                    RecyclerView.ViewHolder holder = binding.recyclerviewTrackStudents.findViewHolderForAdapterPosition(4);
                    if (holder != null) {
                        holder.itemView.performClick();
                        TeacherTourManager.checkStudentProgressionTour((TeacherActivity) requireActivity(), holder.itemView, this::performGuide);
                    }
                });
            }, 200);
        }

        // STEPS 18, 19, 20 - Toolbar options:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_18_LOG ||
                TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_19_BROADCAST ||
                TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_20_END_SESSION) {

            Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.post(() -> {
                    // Getting the 'More options' button:
                    ArrayList<View> outViews = new ArrayList<>();
                    String overflowDescription = getString(androidx.appcompat.R.string.abc_action_menu_overflow_description);
                    toolbar.findViewsWithText(outViews, overflowDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                    View overflowMenuButton = outViews.isEmpty() ? null : outViews.get(0);

                    // STEP 18 - Message log:
                    if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_18_LOG) {

                        // Getting messages log button:
                        View toolbarButton = requireActivity().findViewById(R.id.log);
                        if (toolbarButton != null) {
                            TeacherTourManager.checkLogTour((TeacherActivity) requireActivity(), toolbarButton, this::performGuide);
                        } else if (overflowMenuButton != null) {
                            TeacherTourManager.checkLogTour((TeacherActivity) requireActivity(), overflowMenuButton, this::performGuide);
                        }
                    }

                    // STEP 19 - Message log:
                    else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_19_BROADCAST) {

                        // Getting broadcast button:
                        View toolbarButton = requireActivity().findViewById(R.id.broadcast);
                        if (toolbarButton != null) {
                            TeacherTourManager.checkBroadcastTour((TeacherActivity) requireActivity(), toolbarButton, this::performGuide);
                        } else if (overflowMenuButton != null) {
                            TeacherTourManager.checkBroadcastTour((TeacherActivity) requireActivity(), overflowMenuButton, this::performGuide);
                        }
                    }

                    // STEP 20 - End session:
                    else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_20_END_SESSION) {

                        // Getting end session button:
                        View toolbarButton = requireActivity().findViewById(R.id.action_end_session);

                        //Preparing navigation to sessions list:
                        NavController navController = Navigation.findNavController(requireView());
                        NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.route_list_fragment, false).build();

                        // Flushing the session viewmodel:
                        sessionViewModel.loadTeacherSessions();

                        if (toolbarButton != null) {
                            TeacherTourManager.checkEndSessionTour((TeacherActivity) requireActivity(), toolbarButton, () -> navController.navigate(R.id.session_list_fragment, null, navOptions));
                        } else if (overflowMenuButton != null) {
                            TeacherTourManager.checkEndSessionTour((TeacherActivity) requireActivity(), overflowMenuButton, () -> navController.navigate(R.id.session_list_fragment, null, navOptions));
                        }
                    }
                });
            }
        }
    }


    /**
     * Called when the fragment is not visible.
     * Used to pause the timer which controls the students tracking.
     */
    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(statusRefreshRunnable);
    }


    /**
     * Called when the view is going to be destroyed.
     * Used to stop listening students list and the notifications' receiver.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sessionViewModel.stopListeningToStudents();
        if (messageReceiver != null) {
            requireContext().unregisterReceiver(messageReceiver);
        }
        binding = null;
    }
}