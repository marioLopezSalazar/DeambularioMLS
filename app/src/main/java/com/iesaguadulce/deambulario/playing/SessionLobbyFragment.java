package com.iesaguadulce.deambulario.playing;

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
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.GeoPoint;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.adapters.StudentListAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentSessionLobbyBinding;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Answer;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.repository.callback.ReposListCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that displays the lobby for a created session.
 * Teacher can wait here for students to join via PIN before starting.
 *
 * @author Mario López Salazar
 */
public class SessionLobbyFragment extends Fragment implements StudentListAdapter.OnStudentClickListener {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentSessionLobbyBinding binding;

    /*
     * ViewModel to manage the data access.
     */
    private SessionViewModel sessionViewModel;
    private Session session;
    private boolean isListeningStudents = false;
    private MilestoneViewModel milestoneViewModel;
    private Route route;

    /*
     * ViewModel to manage global UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;

    /*
     * Adapter to manage the list of students joined to the session.
     */
    private StudentListAdapter adapter;


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
        binding = FragmentSessionLobbyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up the students RecyclerView, performs actions to do when de ViewModel changes and sets up buttons listeners.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initializing ViewModels:
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        RouteViewModel routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Creating and attaching an adapter for the student list (in lobby mode):
        adapter = new StudentListAdapter(true, this);
        binding.recyclerviewLobbyStudents.setAdapter(adapter);

        // Observing the current session on ViewModel:
        sessionViewModel.getCurrentSession().observe(getViewLifecycleOwner(), session -> {
            if (session != null) {
                this.session = session;

                // Filling in the session info on the UI:
                binding.textSessionRouteTitle.setText(session.getTitleSnapshot());
                binding.textSessionTeacherPin.setText(String.format(Locale.getDefault(), "%06d", session.getPin()));

                // Performing Teacher guide:
                if (TeacherTourManager.onGuideMode()) {
                    performGuide();
                }
                // When not on Teacher Guide, start listening to joined students, only if not listening yet:
                else if (!isListeningStudents) {
                    sessionViewModel.startListeningToStudents(session.getId());
                    isListeningStudents = true;
                }

                // Requesting the full route info (for allow navigating to route details):
                route = routeViewModel.getSelectedRoute().getValue();
                if (route == null || !route.getId().equals(session.getRouteId())) {
                    routeViewModel.loadRouteById(session.getRouteId());
                }
            }

            // When session deletion:
            else if (getView() != null) {
                Navigation.findNavController(binding.getRoot()).navigateUp();
            }
        });

        // Observing the list of students - Updating the counter and the list on the recyclerview:
        sessionViewModel.getStudents().observe(getViewLifecycleOwner(), students -> {
            adapter.submitList(students);
            int count = (students != null) ? students.size() : 0;
            binding.textStudentCount.setText(String.format(Locale.getDefault(), getString(R.string.students_joined), count));
        });

        // Observing the CurrentRoute:
        routeViewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route ->
                this.route = route);

        // Managing UI lock-unlock during data loading:
        observeLoadingStates();

        // Managing errors on SessionViewModel:
        sessionViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                sessionViewModel.clearError();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        // Setting a listener for the StartSession button:
        binding.buttonStartSession.setOnClickListener(v -> {
            if (route != null && session != null) {

                globalUIViewModel.showLoading();
                milestoneViewModel.getMilestonesForSnapshot(route.getId(), new ReposListCallback<>() {
                    @Override
                    public void onSuccess(List<Milestone> milestones) {
                        globalUIViewModel.hideLoading();

                        if (milestones == null) {
                            milestones = new ArrayList<>();
                        }
                        List<Activity> activities = createActivitySnapshot(milestones);
                        sessionViewModel.startSession(session.getId(), activities);
                        Snackbar.make(binding.getRoot(), R.string.initiating_session, Snackbar.LENGTH_SHORT).show();
                        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_session_lobby_fragment_to_session_track_fragment);
                    }

                    @Override
                    public void onError(Exception e) {
                        globalUIViewModel.hideLoading();
                        Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });

        // Setting up the toolbar:
        setupToolbar();
    }


    /**
     * Locks or unlocks the UI depending on the loading data operations status.
     */
    private void observeLoadingStates() {
        Observer<Boolean> loadingObserver = isLoading ->
                UIUtils.updateLoadingState(
                        Arrays.asList(sessionViewModel, milestoneViewModel),
                        globalUIViewModel);

        sessionViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        milestoneViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
    }


    /**
     * Sets up the ToolBar menu containing the Cancel session and Route details options.
     */
    private void setupToolbar() {
        requireActivity().addMenuProvider(
                new MenuProvider() {

                    /**
                     * Creates the Cancel-Detail menu on the Toolbar.
                     * @param menu         The Cancel-Detail menu.
                     * @param menuInflater The inflater to be used to inflate the menu.
                     */
                    @Override
                    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                        menuInflater.inflate(R.menu.menu_session, menu);
                        // Avoiding log message and multicast messages options on lobby:
                        menu.removeItem(R.id.log);
                        menu.removeItem(R.id.broadcast);
                    }

                    /**
                     * Manages the OnClick event on the ToolBar menu buttons.
                     * @param menuItem The ToolBar Cancel-Detail menu.
                     * @return True if the event has been consumed.
                     */
                    @Override
                    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                        int id = menuItem.getItemId();

                        if (id == R.id.action_route_details) {
                            Navigation.findNavController(requireView()).navigate(R.id.action_session_lobby_fragment_to_route_view_fragment);
                            return true;
                        } else if (id == R.id.action_end_session) {
                            confirmCancelSession();
                            return true;
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }


    /**
     * Creates a single list of activities from a list of milestones.
     *
     * @param milestones The list of milestones whose activities we want.
     * @return The plain list of activities of all milestones.
     */
    @NonNull
    private static List<Activity> createActivitySnapshot(@NonNull List<Milestone> milestones) {
        List<Activity> activities = new ArrayList<>();
        for (Milestone m : milestones) {
            List<Activity> milestoneActivities = m.getActivities();
            if (milestoneActivities != null) {
                for (Activity a : milestoneActivities) {
                    activities.add(new Activity(
                            a.getActivityId(),
                            a.getType(),
                            a.getText(),
                            a.getOptions() != null ? new ArrayList<>(a.getOptions()) : null));
                }
            }
        }
        return activities;
    }


    /**
     * Shows a dialog to request confirmation to cancel the current session.
     */
    private void confirmCancelSession() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.end_session)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(R.string.end_session_lobby_confirm)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Session currentSession = sessionViewModel.getCurrentSession().getValue();
                    if (currentSession != null) {
                        globalUIViewModel.showLoading();
                        //Navigating back:
                        Navigation.findNavController(requireView()).popBackStack();
                        // Requesting the ViewModel to delete the session:
                        sessionViewModel.deleteSession(currentSession);
                        globalUIViewModel.hideLoading();
                    }
                })
                .setNegativeButton(R.string.continue_session, null)
                .show();
    }


    /**
     * Sets up a listener on the 'kick' button of the cards in the students recyclerview.
     *
     * @param student The student to be kicked from the session.
     */
    @Override
    public void onStudentButtonClick(Student student) {
        if (session != null) {
            sessionViewModel.kickStudent(session.getId(), student.getId(), new ReposVoidCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onError(Exception e) {
                }
            });
        } else {
            Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Actions to do when the teacher clicks on the out-of-geofence student button (hidden on this fragment).
     * No actions done.
     *
     * @param student The student whose button has been clicked.
     */
    @Override
    public void onStudentOutOfGeofenceClick(Student student) {
    }


    /**
     * Sets up a listener to act when the user taps on a student card. No actions performed on this implementation.
     *
     * @param student The tapped student.
     */
    @Override
    public void onStudentCardClick(Student student) {
    }


    /**
     * Performs step 15 of the Teacher guide.
     */
    private void performGuide() {

        // STEP 15 - Student lobby:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_15_SESSION_LOBBY) {

            // Loading milestones:
            milestoneViewModel.loadMilestones(session.getRouteId());
            milestoneViewModel.getMilestones().observe(getViewLifecycleOwner(), new Observer<>() {
                @Override
                public void onChanged(List<Milestone> milestones) {
                    if (milestones != null && !milestones.isEmpty()) {
                        milestoneViewModel.getMilestones().removeObserver(this);

                        // Creating 6 invented students:
                        List<Student> inventedStudents = inventStudentsForDemo(milestones);
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            int i = 0;

                            @Override
                            public void run() {
                                if (!isAdded() || isDetached() || getView() == null) return;

                                // Adding students to session:
                                if (i <= 5) {
                                    List<Student> currentStudents = sessionViewModel.getStudents().getValue();
                                    if (currentStudents == null)
                                        currentStudents = new ArrayList<>();
                                    List<Student> updatedStudents = new ArrayList<>(currentStudents);
                                    updatedStudents.add(inventedStudents.get(i));
                                    sessionViewModel.setStudents(updatedStudents);
                                    adapter.submitList(updatedStudents);
                                    i++;
                                    handler.postDelayed(this, 250);

                                } else if (i == 6) {
                                    TeacherTourManager.checkStartSessionTour((TeacherActivity) requireActivity(), binding.buttonStartSession);
                                }
                            }
                        });
                    }
                }
            });
        }
    }


    /**
     * Creates six invented students near of some milestones. Used to perform the Teacher Guide.
     *
     * @param milestones The milestones list.
     * @return List of six invented students.
     */
    private List<Student> inventStudentsForDemo(List<Milestone> milestones) {
        Milestone milestoneBase = milestones.get(0);
        List<Student> students = new ArrayList<>();
        if (milestoneBase == null || milestoneBase.getCoordinates() == null) {
            return students;
        }

        double baseLat = milestoneBase.getCoordinates().getLatitude();
        double baseLng = milestoneBase.getCoordinates().getLongitude();

        // Teacher real time:
        long now = System.currentTimeMillis();

        // Student 1 - Online, on geofence:
        Student s1 = new Student();
        s1.setNick(getString(R.string.guide_student1));
        s1.setLocation(new Student.StudentLocation(
                new GeoPoint(baseLat + 0.0001, baseLng - 0.0001),
                new Date(now + 240000)
        ));
        s1.setId(getString(R.string.guide_student1));
        students.add(s1);

        // Student 2 - Online, on geofence:
        Student s2 = new Student();
        s2.setNick(getString(R.string.guide_student2));
        s2.setLocation(new Student.StudentLocation(
                new GeoPoint(baseLat + 0.0026, baseLng + 0.0006),
                new Date(now + 240000)
        ));
        s2.setId(getString(R.string.guide_student2));
        students.add(s2);

        // Student 3 - Disconnected, on geofence:
        Student s3 = new Student();
        s3.setNick(getString(R.string.guide_student3));
        s3.setLocation(new Student.StudentLocation(
                new GeoPoint(baseLat - 0.0015, baseLng + 0.0020),
                new Date(now - 240000)
        ));
        s3.setId(getString(R.string.guide_student3));
        students.add(s3);

        // Student 4 - Abandoned:
        Student s4 = new Student();
        s4.setNick(getString(R.string.guide_student4));
        s4.setLocation(null);
        s4.setId(getString(R.string.guide_student4));
        students.add(s4);

        // Student 5 - Online, but out of geofence:
        Student s5 = new Student();
        s5.setNick(getString(R.string.guide_student5));
        s5.setLocation(new Student.StudentLocation(
                new GeoPoint(baseLat + 0.0015, baseLng + 0.0030),
                new Date(now + 240000)
        ));
        s5.setId(getString(R.string.guide_student5));
        students.add(s5);

        // Student 6 - Done:
        Student s6 = new Student();
        s6.setNick(getString(R.string.guide_student6));
        s6.setLocation(new Student.StudentLocation(
                new GeoPoint(baseLat - 0.0003, baseLng + 0.0012),
                new Date(now + 240000)
        ));
        List<String> visitedMilestones = new ArrayList<>();
        for(Milestone m : milestones) visitedMilestones.add(m.getId());
        s6.setVisitedMilestones(visitedMilestones);
        if (milestoneBase.getActivities() != null && milestoneBase.getActivities().size() >= 2 && milestoneBase.getActivities().get(0) != null && milestoneBase.getActivities().get(1) != null) {
            s6.setAnswers(new ArrayList<>(List.of(
                    new Answer(milestoneBase.getActivities().get(0).getActivityId(), getString(R.string.guide_activity1_answer)),
                    new Answer(milestoneBase.getActivities().get(1).getActivityId(), getString(R.string.guide_activity2_answer2))
            )));
        }
        s6.setId(getString(R.string.guide_student6));
        students.add(s6);

        return students;
    }


    /**
     * Called when the view is going to be destroyed. Used to stop listening students list.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sessionViewModel.stopListeningToStudents();
        isListeningStudents = false;
        binding = null;
    }

}