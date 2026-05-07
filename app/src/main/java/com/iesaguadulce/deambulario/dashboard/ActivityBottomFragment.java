package com.iesaguadulce.deambulario.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.SheetAddactivityBinding;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Fragment to manage the Bottom Sheet used to perform Milestone Activity operations.
 *
 * @author Mario López Salazar.
 */
public class ActivityBottomFragment extends BottomSheetDialogFragment {

    /*
     * ViewBinding to manage view components.
     */
    private SheetAddactivityBinding binding;

    /*
     * ViewModel and variables to manage it.
     */
    private MilestoneViewModel milestoneViewModel;
    private Milestone milestone;
    private int activityIndex;
    private Activity activity;

    /*
     * Flags to indicate if the Bottom Sheet is opened on edit/creating/read-only mode.
     */
    private boolean isEditMode = false;
    private boolean isReadOnly;



    /**
     * Inflates the bottom sheet view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetAddactivityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Initializes the setting up of the view.
     *
     * @param view               The View returned by the onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Getting the ViewModel and the current milestone:
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        GlobalUIViewModel globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);
        milestone = Objects.requireNonNull(milestoneViewModel.getSelectedMilestone().getValue());

        // Getting the read-only flag value, to perform the Test Activity UI details:
        Boolean readOnly = globalUIViewModel.isReadOnlyMode().getValue();
        this.isReadOnly = (readOnly != null && readOnly);

        // Setting up the listener for the ChipGroup:
        setupChipGroupListener();

        // Setting up the appearance of the UI and filling previous data values:
        determineModeAndFillData();

        // Applying read-only visual restrictions if needed:
        if (this.isReadOnly) {
            applyReadOnlyMode();
        }

        // Setting up buttons listeners:
        binding.buttonAddOptionTest.setOnClickListener(v -> addOptionView(""));
        binding.buttonSaveActivity.setOnClickListener(v -> saveActivity());
        binding.buttonDeleteActivity.setOnClickListener(v -> showDeleteConfirmationDialog());
    }


    /**
     * Performs form appearance depending on the kind of activity selected on ChipGroup.
     */
    private void setupChipGroupListener() {
        binding.chipGroupActivity.setOnCheckedStateChangeListener((group, checkedIds) -> {

            // Getting the selected chip:
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            // Test activity:
            if (checkedId == R.id.chip_test) {
                binding.layoutTestOptions.setVisibility(View.VISIBLE);

                // Blanking two answer options, when there is no options performed:
                if (binding.layoutAnswers.getChildCount() == 0 && !isReadOnly) {
                    addOptionView("");
                    addOptionView("");
                }
            }
            // Other kinds of activities:
            else {
                binding.layoutTestOptions.setVisibility(View.GONE);
            }
        });
    }


    /**
     * Determines if we are creating a new activity editing an existing one,
     * and populates the UI accordingly.
     */
    private void determineModeAndFillData() {

        // Getting the current activity index from the ViewModel, if selected:
        activityIndex = Objects.requireNonNull(milestoneViewModel.getSelectedActivity().getValue());

        // Edit mode (there is a selected activity):
        if (activityIndex >= 0) {
            isEditMode = true;
            binding.textViewActivitySheetTitle.setText(R.string.edit);
            binding.buttonDeleteActivity.setVisibility(View.VISIBLE);

            // Getting the current activity:
            Activity originalActivity = Objects.requireNonNull(milestone.getActivities().get(activityIndex));

            // Need to create a clone of the Activity object (to assure recyclerview redrawing on exit):
            activity = new Activity();
            activity.setActivityId(originalActivity.getActivityId());
            activity.setType(originalActivity.getType());
            activity.setText(originalActivity.getText());
            activity.setOptions(originalActivity.getOptions());

            // Filling the activity text:
            binding.textActivityPrompt.setText(activity.getText());

            // Setting UI appearance depending on the kind of activity:
            if (activity.getType() != null) {
                switch (activity.getType()) {
                    case QUESTION:
                        binding.chipGroupActivity.check(R.id.chip_open_question);
                        break;
                    case PHOTO:
                        binding.chipGroupActivity.check(R.id.chip_photo_task);
                        break;
                    case VIDEO:
                        binding.chipGroupActivity.check(R.id.chip_video_task);
                        break;
                    case TEST:
                        binding.chipGroupActivity.check(R.id.chip_test);
                        // Showing test answers:
                        binding.layoutAnswers.removeAllViews();
                        if (activity.getOptions() != null) {
                            for (String option : activity.getOptions()) {
                                addOptionView(option);
                            }
                        }
                        break;
                }
            }
        }

        // Creation mode (there isn't a selected content):
        else {
            isEditMode = false;

            // Creating a new activity:
            activity = new Activity();

            // Setting UI appearance (open question activity as default):
            binding.textViewActivitySheetTitle.setText(R.string.add_activity);
            binding.buttonDeleteActivity.setVisibility(View.GONE);
            binding.chipGroupActivity.check(R.id.chip_open_question);
        }
    }

    /**
     * Add a new answer on a test activity.
     *
     * @param text The text of the answer.
     */
    private void addOptionView(String text) {

        // Inflating a new item layout and requesting its elements:
        View optionView = getLayoutInflater().inflate(R.layout.item_test, binding.layoutAnswers, false);
        TextInputEditText editText = optionView.findViewById(R.id.text_input_test_option);
        Button btnRemove = optionView.findViewById(R.id.button_remove_answer);

        // Setting the answer text, if set:
        if (editText != null) {
            editText.setText(text);
        }

        // Setting up the view components depending on the UI mode:
        if (!isReadOnly) {
            btnRemove.setOnClickListener(v -> {
                UIUtils.hideKeyboard(requireActivity());
                binding.layoutAnswers.removeView(optionView);
            });
        } else {
            if (editText != null) {
                editText.setFocusable(false);
            }
            btnRemove.setVisibility(View.GONE);
        }

        // Adding the answer layout to the bottom sheet:
        binding.layoutAnswers.addView(optionView);
    }


    /**
     * Locks the UI to prevent any modifications when in read-only mode.
     */
    private void applyReadOnlyMode() {
        isReadOnly = true;
        binding.buttonSaveActivity.setVisibility(View.GONE);
        binding.buttonDeleteActivity.setVisibility(View.GONE);
        binding.buttonAddOptionTest.setVisibility(View.GONE);
        binding.chipGroupActivity.setVisibility(View.GONE);
        binding.textActivityPrompt.setFocusable(false);
        binding.textViewActivitySheetTitle.setText(R.string.details);
    }


    /**
     * Validates input data, updates the current activity object, and sends it to the ViewModel to be saved.
     */
    private void saveActivity() {

        // Getting the activity type:
        int checkedId = binding.chipGroupActivity.getCheckedChipId();
        if (checkedId == R.id.chip_open_question) {
            activity.setType(Activity.ActivityType.QUESTION);
        } else if (checkedId == R.id.chip_photo_task) {
            activity.setType(Activity.ActivityType.PHOTO);
        } else if (checkedId == R.id.chip_video_task) {
            activity.setType(Activity.ActivityType.VIDEO);
        } else if (checkedId == R.id.chip_test) {
            activity.setType(Activity.ActivityType.TEST);
        }

        // Getting the activity text:
        String text = UIUtils.validateNotEmpty(binding.textInputActivityPrompt, getString(R.string.required_field));
        if (text == null || text.isEmpty()) {
            return;
        }
        activity.setText(text);

        // Getting the Test answers:
        if (activity.getType() == Activity.ActivityType.TEST) {
            List<String> options = new ArrayList<>();
            for (int i = 0; i < binding.layoutAnswers.getChildCount(); i++) {
                TextInputEditText et = binding.layoutAnswers.getChildAt(i).findViewById(R.id.text_input_test_option);
                if (et != null && et.getText() != null) {
                    String optionText = et.getText().toString().trim();
                    if (!optionText.isEmpty()) {
                        options.add(optionText);
                    }
                }
            }

            // Checking the minimum number of options in Test activity:
            if (options.size() < 2) {
                Toast.makeText(getContext(), R.string.test_needs_two_or_more_options, Toast.LENGTH_SHORT).show();
                return;
            }
            activity.setOptions(options);

        }
        // In other kind of activities (not Test), there is no options:
        else {
            activity.setOptions(null);
        }

        // Saving on ViewModel:
        List<Activity> activities;
        if (milestone.getActivities() == null) {
            activities = new ArrayList<>();
        } else {
            activities = new ArrayList<>(milestone.getActivities());
        }
        if (isEditMode) {
            activities.set(activityIndex, activity);
        } else {
            // Creating Activity ID:
            activity.setActivityId(java.util.UUID.randomUUID().toString());
            activities.add(activity);
        }
        milestone.setActivities(activities);
        milestoneViewModel.setSelectedMilestone(milestone);

        // Hiding the bottom sheet:
        dismiss();
    }


    /**
     * Shows a confirmation dialog before deleting the activity.
     */
    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(getString(R.string.confirm_activity_deletion))
                .setPositiveButton(getString(R.string.delete),
                        (dialog, which) -> deleteActivity())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    /**
     * Deletes the current activity from the milestone and updates the ViewModel.
     */
    private void deleteActivity() {

        // Activity deletion:
        List<Activity> activities = new ArrayList<>(milestone.getActivities());
        activities.remove(activityIndex);
        milestone.setActivities(activities);
        milestoneViewModel.setSelectedMilestone(milestone);

        // Hiding the bottom sheet:
        dismiss();
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