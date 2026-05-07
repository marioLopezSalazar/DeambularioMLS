package com.iesaguadulce.deambulario.playing;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.StudentActivity;
import com.iesaguadulce.deambulario.databinding.FragmentStudentLobbyBinding;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.repository.SessionRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.notifications.FCMTokenCallback;
import com.iesaguadulce.deambulario.notifications.NotifUtils;
import com.iesaguadulce.deambulario.settings.Constants;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

/**
 * Fragment that acts as the 'Waiting Lobby' for the Student.
 * Handles the session joining process, UI state, and navigation to the main game.
 *
 * @author Mario López Salazar
 */
public class StudentLobbyFragment extends Fragment {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentStudentLobbyBinding binding;

    /*
     * ViewModel to manage the data access.
     */
    private SessionViewModel sessionViewModel;

    /*
     * ViewModel to manage global UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;

    /*
     * Main student activity.
     */
    private StudentActivity activity;

    /*
     * Flag to prevent multiple navigation events.
     */
    private boolean hasNavigatedToActive = false;

    /*
     * Flag to ensure real-time listener is only started once.
     */
    private boolean isListeningToSession = false;


    /**
     * Called to inflate the fragment's interface view.
     *
     * @param inflater           The LayoutInflater object used to inflate any views in the fragment.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentLobbyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Performs the UI, launches the observing of ViewModel and the joining process.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Getting the activity:
        activity = (StudentActivity) requireActivity();

        // Setting up the view models:
        sessionViewModel = new ViewModelProvider(activity).get(SessionViewModel.class);
        globalUIViewModel = new ViewModelProvider(activity).get(GlobalUIViewModel.class);

        // Leaving button:
        binding.buttonLeaveLobby.setOnClickListener(v -> {
            Session currentSession = sessionViewModel.getCurrentSession().getValue();
            if (currentSession != null) {
                globalUIViewModel.showLoading();

                // Deleting user document on Firebase:
                sessionViewModel.stopListeningToMyStatus();
                sessionViewModel.kickStudent(currentSession.getId(), activity.getStudentUid(), new ReposVoidCallback() {
                    @Override
                    public void onSuccess() {
                        // Exiting from the StudentActivity:
                        activity.kickStudentToAuth(R.string.kick_as_your_desire);
                    }

                    @Override
                    public void onError(Exception e) {
                        globalUIViewModel.hideLoading();
                        sessionViewModel.startListeningToMyStatus(currentSession.getId());
                        Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Exiting, by default:
                activity.kickStudentToAuth(R.string.kick_as_your_desire);
            }
        });

        // Observing the SessionViewModel:
        observeViewModel();
    }


    /**
     * Initiates the process to find the session and join the student.
     */
    private void joinSession() {

        // Requesting the session PIN:
        int pin = activity.getSessionPin();
        if (pin == -1) {
            activity.kickStudentToAuth(R.string.session_not_found);
            return;
        }

        // Creating the Student object:
        String uid = activity.getStudentUid();
        String nick = activity.getStudentNick();
        String fcmToken = NotifUtils.getLocalFCMToken(requireContext());
        Student student = new Student(nick, fcmToken, null, null, null);
        student.setId(uid);

        // Requesting the SessionViewModel to join (result managed by observers):
        sessionViewModel.joinSessionByPin(pin, student);
    }


    /**
     * Observes the ViewModel for permissions, session state updates and errors.
     */
    private void observeViewModel() {

        // Observing if permissions are granted previously joining:
        globalUIViewModel.getPermissionsGranted().observe(getViewLifecycleOwner(), granted -> {
            if (granted != null && granted) {
                joinSession();
                globalUIViewModel.setPermissionsGranted(false);
            }
        });

        // When session doesn't exist or the nick is duplicated:
        sessionViewModel.getError().observe(getViewLifecycleOwner(), exception -> {
            if (exception == null) {
                return;
            }
            if (exception.getMessage() != null && exception.getMessage().equals("DUPLICATE_NICK")) {
                activity.kickStudentToAuth(R.string.nickname_already_in_use);
            } else {
                activity.kickStudentToAuth(R.string.session_not_found);
            }
            sessionViewModel.clearError();
        });

        sessionViewModel.getCurrentSession().observe(getViewLifecycleOwner(), session -> {
            if (session == null) {
                return;
            }

            // Saving session ID on SharedPreferences (used for updating student FCM Token on Firebase):
            SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
            prefs.edit().putString(Constants.KEY_SESSION, session.getId()).apply();

            // Saving FCM token on Firebase:
            Context appContext = requireContext().getApplicationContext();
            NotifUtils.fetchFCMToken(new FCMTokenCallback() {
                @Override
                public void onTokenReceived(String token) {
                    NotifUtils.saveLocalFCMToken(appContext, token);
                    SessionRepository.getInstance().updateStudentToken(session.getId(), activity.getStudentUid(), token,
                            new ReposVoidCallback() {
                                @Override
                                public void onSuccess() {
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (isAdded() && getContext() != null) {
                                        Toast.makeText(activity, R.string.teacher_notification_permission_denied, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                    );
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(activity, R.string.teacher_notification_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });


            // Listeners to ViewModel:
            if (!isListeningToSession) {

                // Listening to session status changes:
                sessionViewModel.startListeningToSession(session.getId());
                // Listening student status (to manage kicking from teacher):
                sessionViewModel.startListeningToMyStatus(session.getId());
                // Activating listening flag:
                isListeningToSession = true;
            }

            // Actions depending on the session status:
            if (session.getState() == Session.SessionState.CLOSED) {
                activity.kickStudentToAuth(R.string.session_closed);
            } else if (session.getState() == Session.SessionState.WAITING) {
                String routeTitle = session.getTitleSnapshot() != null ? session.getTitleSnapshot() : "";
                binding.textviewStudLobbyRouteName.setText(routeTitle);
                binding.textviewStudLobbyStudentName.setText(activity.getStudentNick());
            } else if (session.getState() == Session.SessionState.ACTIVE) {
                if (!hasNavigatedToActive) {
                    hasNavigatedToActive = true;
                    UIUtils.playShortSound(requireContext(), R.raw.sound_first_entering);
                    Navigation.findNavController(binding.getRoot()).navigate(R.id.action_student_lobby_to_student_play);
                }
            }
        });

        // Managing kicking from teacher:
        sessionViewModel.isKicked().observe(getViewLifecycleOwner(), isKicked -> {
            if (isKicked != null && isKicked) {
                sessionViewModel.clearKickedStatus();
                activity.kickStudentToAuth(R.string.kicked_by_teacher);
            }
        });

        // Performing UI lock-unlock appearance when loading data:
        Observer<Boolean> loadingObserver = isLoading ->
                UIUtils.updateLoadingState(
                        java.util.Collections.singletonList(sessionViewModel),
                        globalUIViewModel);
        sessionViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
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