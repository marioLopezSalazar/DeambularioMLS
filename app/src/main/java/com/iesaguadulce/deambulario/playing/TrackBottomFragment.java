package com.iesaguadulce.deambulario.playing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.StudentActivity;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.adapters.ProgressListAdapter;
import com.iesaguadulce.deambulario.databinding.DialogSendMessageBinding;
import com.iesaguadulce.deambulario.databinding.SheetProgressBinding;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Answer;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.pojos.Student.LiveStatus;
import com.iesaguadulce.deambulario.notifications.NotifUtils;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Fragment that allows to see the progress of a student during a session, and sending individual messages.
 *
 * @author Mario López Salazar
 */
public class TrackBottomFragment extends BottomSheetDialogFragment {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private SheetProgressBinding binding;

    /*
     * ViewModel to manage the data access.
     */
    private SessionViewModel sessionViewModel;
    private Student currentStudent;
    private List<Milestone> currentMilestones;

    /*
     * Adapter to manage the milestones progression list.
     */
    private ProgressListAdapter adapter;
    private boolean mustPlayEndSound = true;


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
        binding = SheetProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes ViewModels, sets up the RecyclerView adapter, observes data changes to update the UI,
     * and configures the action buttons based on the host Activity profile (teacher or student).
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setting up the ViewModel:
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        MilestoneViewModel milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);

        // Attaching an adapter to the progress recyclerview:
        adapter = new ProgressListAdapter();
        binding.recyclerviewMilestoneProgress.setAdapter(adapter);

        // Setting the fragment as an observer of the ViewModels, and updating the UI when they change:
        sessionViewModel.getCurrentStudent().observe(getViewLifecycleOwner(), student -> {
            this.currentStudent = student;
            refreshUI();

            if (requireActivity() instanceof TeacherActivity && student != null && student.getLiveStatus() != null) {
                boolean hasAbandoned = student.getLiveStatus().getConnection() == LiveStatus.Connection.ABANDONED;
                binding.buttonSendMessage.setEnabled(!hasAbandoned);
                binding.buttonSendMessage.setAlpha(hasAbandoned ? 0.5f : 1.0f);
            }
        });
        milestoneViewModel.getMilestones().observe(getViewLifecycleOwner(), milestones -> {
            this.currentMilestones = milestones;
            refreshUI();
        });

        // Setting up the 'Send message' button, depending on the profile:
        if (requireActivity() instanceof TeacherActivity) {
            setupSendMessageButton();
        } else if (requireActivity() instanceof StudentActivity) {
            binding.buttonSendMessage.setVisibility(View.GONE);
        }
    }


    /**
     * Performs the button to send a message from the teacher to the student.
     */
    private void setupSendMessageButton() {

        // Setting button text:
        binding.buttonSendMessage.setText(R.string.send_message);
        //Setting button click listener:
        binding.buttonSendMessage.setOnClickListener(v -> {

            if (currentStudent == null) {
                Toast.makeText(requireContext(), R.string.data_loading, Toast.LENGTH_SHORT).show();
                return;
            }

            // Creating message dialog:
            DialogSendMessageBinding dialogBinding = DialogSendMessageBinding.inflate(getLayoutInflater());

            // Launching dialog:
            AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.send_message)
                    .setIcon(R.mipmap.icon_deambulario)
                    .setMessage(R.string.send_message)
                    .setView(dialogBinding.getRoot())
                    .setPositiveButton(R.string.send, null)
                    .setNegativeButton(R.string.cancel, (d, which) -> d.cancel())
                    .create();
            dialog.show();

            // Performing positive button behavior:
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String message = UIUtils.validateNotEmpty(dialogBinding.textInputLayoutMessage, getString(R.string.empty_message));
                if (message != null) {
                    Session currentSession = sessionViewModel.getCurrentSession().getValue();
                    if (currentSession == null) {
                        Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    } else {
                        sessionViewModel.sendTeacherMessage(currentSession.getId(), currentStudent.getId(), message);
                        // Adding message to teacher local log:
                        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                        NotifUtils.appendToMessageLog(requireContext(),
                                time + " - " + currentStudent.getNick() + ": " + message,
                                currentSession.getId());
                        Toast.makeText(requireContext(), R.string.message_sent, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        dismiss();
                    }
                }
            });
        });
    }


    /**
     * Shows the student progression it on the UI.
     */
    private void refreshUI() {
        if (currentStudent == null || currentMilestones == null) {
            return;
        }

        // Showing student nick:
        binding.textviewStudentName.setText(currentStudent.getNick());

        // Computing progression for each milestone:
        Map<String, Student.MilestoneProgress> progressMap = calculateMilestoneProgress(currentStudent, currentMilestones);

        // Counting completed milestones:
        int completed = 0;
        for (Student.MilestoneProgress status : progressMap.values()) {
            if (status == Student.MilestoneProgress.COMPLETED) {
                completed++;
            }
        }
        int totalMilestones = currentMilestones.size();
        if (totalMilestones > 0) {
            int progressPercentage = (completed * 100) / totalMilestones;
            binding.progressGeneral.setProgress(progressPercentage, true);
        } else {
            binding.progressGeneral.setProgress(0);
        }

        // Showing progress on the UI:
        binding.textviewProgressText.setText(getString(R.string.milestones_progress, completed, totalMilestones));
        adapter.submitData(currentMilestones, progressMap);
    }


    /**
     * Computes the student progression.
     *
     * @param student    The student whose progression must be computed.
     * @param milestones The list of milestones in the route.
     * @return A map containing the milestoneID and its progression.
     */
    private Map<String, Student.MilestoneProgress> calculateMilestoneProgress(Student student, List<Milestone> milestones) {
        Map<String, Student.MilestoneProgress> progressMap = new HashMap<>();

        // Getting student visited milestones:
        List<String> visitedIds = student.getVisitedMilestones();

        // When no milestone has been visited:
        if (visitedIds == null) {
            for (Milestone m : milestones) {
                progressMap.put(m.getId(), Student.MilestoneProgress.PENDING);
            }
            return progressMap;
        }

        // Getting the ID of the activities answered by the student:
        List<Answer> answers = student.getAnswers();
        Set<String> answeredActivityIds = new HashSet<>();
        if (answers != null) {
            for (Answer a : answers) {
                answeredActivityIds.add(a.getActivityId());
            }
        }

        // For each milestone, checking if visited and if all its activities done:
        for (Milestone m : milestones) {
            String milestoneId = m.getId();

            // Visited?
            if (!visitedIds.contains(milestoneId)) {
                progressMap.put(milestoneId, Student.MilestoneProgress.PENDING);
                mustPlayEndSound = false;
            }
            // No activities in the milestone?
            else if (m.getActivities() == null || m.getActivities().isEmpty()) {
                progressMap.put(milestoneId, Student.MilestoneProgress.COMPLETED);
            }
            // All activities in the milestone done?
            else {
                boolean allAnswered = true;
                List<Activity> activities = m.getActivities();
                for (int i = 0; i < activities.size() && allAnswered; i++) {
                    if (!answeredActivityIds.contains(activities.get(i).getActivityId())) {
                        allAnswered = false;
                    }
                }
                if (allAnswered) {
                    progressMap.put(milestoneId, Student.MilestoneProgress.COMPLETED);
                } else {
                    progressMap.put(milestoneId, Student.MilestoneProgress.INCOMPLETE);
                    mustPlayEndSound = false;
                }
            }
        }
        if (requireActivity() instanceof StudentActivity && mustPlayEndSound){
            UIUtils.playShortSound(requireContext(), R.raw.sound_route_done);
            mustPlayEndSound = false;
        }

        return progressMap;
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