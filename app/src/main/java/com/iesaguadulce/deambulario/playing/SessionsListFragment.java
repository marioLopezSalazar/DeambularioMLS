package com.iesaguadulce.deambulario.playing;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.adapters.SessionListAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentSessionsListBinding;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.utils.FilesUtils;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

/**
 * Fragment that displays the list of teacher sessions using a RecyclerView.
 *
 * @author Mario López Salazar
 */
public class SessionsListFragment extends Fragment implements SessionListAdapter.OnSessionClickListener {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentSessionsListBinding binding;

    /*
     * ViewModel to manage the data access.
     */
    private SessionViewModel viewModel;

    /*
     * ViewModel to manage global UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;
    private boolean onDownloadingResults = false;



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
        binding = FragmentSessionsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up the sessions RecyclerView with the Session LiveData list.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initializing the SessionListAdapter, setting up this fragment as the CardViews buttons listener:
        SessionListAdapter adapter = new SessionListAdapter(this);

        // Setting up the layout's RecyclerView with the SessionListAdapter:
        binding.recyclerviewSessions.setAdapter(adapter);

        // Getting the SessionViewModel:
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        // Getting the GlobalUIViewModel:
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Setting up this fragment as an observer of the LiveData changes:
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                globalUIViewModel.showLoading();
            } else {
                globalUIViewModel.hideLoading();
            }
        });
        viewModel.getTeacherSessions().observe(
                getViewLifecycleOwner(),
                sessions -> {
                    // When list changes, we'll pass the new list to the SessionListAdapter:
                    adapter.submitList(sessions);

                    if (sessions != null && !sessions.isEmpty()) {
                        binding.recyclerviewSessions.setVisibility(View.VISIBLE);
                        binding.layoutEmptyStateSessions.setVisibility(View.GONE);
                    } else {
                        binding.layoutEmptyStateSessions.setVisibility(View.VISIBLE);
                        binding.recyclerviewSessions.setVisibility(View.GONE);
                    }

                    // Performing Teacher Guide:
                    performGuide();
                });

        viewModel.getStudents().observe(getViewLifecycleOwner(), students -> {
            if (onDownloadingResults) {
                onDownloadingResults = false;
                FilesUtils.exportAndShareCsv(requireContext(), viewModel.getCurrentSession().getValue(), students);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), exception -> {
            if (exception != null) {
                Snackbar.make(
                        binding.getRoot(),
                        getResources().getString(R.string.error_network),
                        Snackbar.LENGTH_LONG
                ).show();
                viewModel.clearError();
            }
        });

        // Initial session loading:
        if (viewModel.getTeacherSessions().getValue() == null) {
            viewModel.loadTeacherSessions();
        }

        // Setting up the session SearchBar:
        setUpSessionSearching();
    }


    /**
     * Manages clicks on the Action button of a session CardView.
     * Navigates to different fragments or downloads session answers, depending on the Session state.
     *
     * @param session The session corresponding to the clicked CardView.
     */
    @Override
    public void onSessionActionClick(Session session) {

        // Setting the current session on the ViewModel (used on the next fragments or to download session answers):
        viewModel.setCurrentSession(session);

        switch (session.getState()) {
            case WAITING:
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_session_list_fragment_to_session_lobby_fragment);
                break;
            case ACTIVE:
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_session_list_fragment_to_session_track_fragment);
                break;
            case CLOSED:
                // Launching students loading (result will be managed on observer):
                onDownloadingResults = true;
                viewModel.loadSessionStudents();
                Snackbar.make(binding.getRoot(), R.string.getting_answers, Snackbar.LENGTH_SHORT).show();
                break;
        }
    }


    /**
     * Manages long clicks on a session CardView to delete it.
     * Shows a confirmation dialog before proceeding with deletion.
     *
     * @param session The session corresponding to the clicked CardView.
     */
    @Override
    public void onSessionLongClick(Session session) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_session)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(R.string.delete_session_confirming)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    // Deleting the Session and its Students:
                    viewModel.deleteSession(session);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    /**
     * Manages the sessions filtering using the SearchBar.
     */
    private void setUpSessionSearching() {
        // Piping the Search text real-time changes to the SessionViewModel:
        binding.textSearchSession.addTextChangedListener(new TextWatcher() {
            /**
             * Used to know that a substring of the text is about to be replaced. Not needed for this implementation.
             * @param s     The watched text.
             * @param start Position where replacing starts.
             * @param count Quantity of original characters.
             * @param after Quantity of new characters.
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            /**
             * Used to know that a substring of the text has just been replaced.
             * On this implementation, pipes the search query to the RouteViewModel on every keystroke.
             * @param s      The watched text.
             * @param start  Position where replacing starts.
             * @param before Quantity of old characters.
             * @param count  Quantity of new characters.
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null) {
                    viewModel.filterTeacherSessions(s.toString());
                }
            }

            /**
             * Used to know that the text has been changed. Not needed for this implementation.
             * @param s The watched text.
             */
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Managing searching end actions:
        binding.searchLayoutSessions.setEndIconOnClickListener(v -> {
            binding.textSearchSession.setText("");
            UIUtils.hideKeyboard(requireActivity());
            binding.textSearchSession.clearFocus();
        });
    }


    /**
     * Performs steps 21-22 of the Teacher guide.
     */
    private void performGuide() {

        // STEP 21 - Sessions list:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_21_SESSIONS_LIST) {
            // Getting the 'Sessions' bottom navigation button:
            View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            bottomNav.post(() -> {
                View targetTab = bottomNav.findViewById(R.id.session_list_fragment);
                if (targetTab != null) {
                    TeacherTourManager.checkSessionListTour((TeacherActivity) requireActivity(), targetTab, this::performGuide);
                }
            });
        }


        // STEP 22 - Sessions management:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_22_SESSION_MANAGEMENT) {
            // Getting first session card:
            binding.recyclerviewSessions.postDelayed(() -> {
                binding.recyclerviewSessions.scrollToPosition(0);
                binding.recyclerviewSessions.post(() -> {
                    RecyclerView.ViewHolder holder = binding.recyclerviewSessions.findViewHolderForAdapterPosition(0);
                    if (holder != null) {
                        TeacherTourManager.checkSessionManagementTour((TeacherActivity) requireActivity(),
                                holder.itemView.findViewById(R.id.btn_session_action),
                                () -> Navigation.findNavController(requireView()).navigate(R.id.teacher_settings_fragment)
                        );
                    }
                });
            }, 200);
        }
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