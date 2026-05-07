package com.iesaguadulce.deambulario.playing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.SheetLearningTestBinding;
import com.iesaguadulce.deambulario.model.pojos.Activity;

import java.util.ArrayList;

/**
 * Fragment to manage the Bottom Sheet used to display and answer a Test Activity.
 *
 * @author Mario López Salazar
 */
public class StudentMilestoneBottomTestFragment extends BottomSheetDialogFragment {

    /**
     * Key for manage the ActivityID on the fragment input bundle.
     */
    private static final String ARG_ACTIVITY_ID = "arg_activity_id";
    /**
     * Key for manage the Activity Text on the fragment input bundle.
     */
    private static final String ARG_ACTIVITY_TEXT = "arg_activity_text";
    /**
     * Key for manage the Activity Options on the fragment input bundle.
     */
    private static final String ARG_ACTIVITY_OPTIONS = "arg_activity_options";
    /**
     * Key for manage the Activity previous set answer on the fragment input bundle.
     */
    private static final String ARG_PREVIOUS_ANSWER = "arg_previous_answer";

    /**
     * Key for manage the fragment result.
     */
    public static final String REQUEST_KEY = "test_answer_request";
    /**
     * Key for manage the Activity ID on the fragment result.
     */
    public static final String BUNDLE_KEY_ACTIVITY_ID = "activity_id";
    /**
     * Key for manage the Activity selected option on the fragment result.
     */
    public static final String BUNDLE_KEY_SELECTED_OPTION = "selected_option";


    //==============================================================================================


    /*
     * ViewBinding to manage view components.
     */
    private SheetLearningTestBinding binding;


    /**
     * Creates a new instance of the dialog, passing the activity and the previous set answer.
     *
     * @param activity       The activity object containing the question and options.
     * @param previousAnswer The previous answer set by the student.
     * @return A new instance of StudentMilestoneBottomTestFragment.
     */
    public static StudentMilestoneBottomTestFragment newInstance(Activity activity, String previousAnswer) {
        StudentMilestoneBottomTestFragment fragment = new StudentMilestoneBottomTestFragment();
        Bundle args = new Bundle();

        args.putString(ARG_ACTIVITY_ID, activity.getActivityId());
        args.putString(ARG_ACTIVITY_TEXT, activity.getText());
        args.putStringArrayList(ARG_ACTIVITY_OPTIONS, new ArrayList<>(activity.getOptions()));
        args.putString(ARG_PREVIOUS_ANSWER, previousAnswer);

        fragment.setArguments(args);
        return fragment;
    }


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
        binding = SheetLearningTestBinding.inflate(inflater, container, false);
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

        // Security check: dismiss if no data was provided:
        if (getArguments() == null) {
            dismiss();
            return;
        }

        // Setting up the appearance of the UI and filling the question data:
        populateQuestionData();

        // Setting up buttons listeners:
        setupSubmitButtonListener();
    }


    /**
     * Fills the UI with the question text and dynamically generates the radio buttons for the options.
     */
    private void populateQuestionData() {

        // Getting activity data:
        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        String questionText = args.getString(ARG_ACTIVITY_TEXT);
        ArrayList<String> options = args.getStringArrayList(ARG_ACTIVITY_OPTIONS);
        String previousAnswer = args.getString(ARG_PREVIOUS_ANSWER);

        // Setting vertical padding for radio buttons:
        int padding = (int) (16 * getResources().getDisplayMetrics().density);

        // Setting the prompt text:
        binding.textviewStudTestQuestion.setText(questionText);

        // Performing activity options:
        if (options != null) {
            for (String optionText : options) {
                MaterialRadioButton radioButton = new MaterialRadioButton(requireContext());
                radioButton.setId(View.generateViewId());
                radioButton.setText(optionText);
                radioButton.setTextSize(16f);
                radioButton.setPadding(0, padding, 0, padding);
                binding.radioGroupStudTestOptions.addView(radioButton);
                if (optionText.equals(previousAnswer)) {
                    radioButton.setChecked(true);
                }
            }
        }
    }


    /**
     * Sets up the logic for the submit button, validating the selection and returning the answer.
     */
    private void setupSubmitButtonListener() {
        binding.buttonStudSubmitTest.setOnClickListener(v -> {
            // Getting the selected option ID:
            int selectedId = binding.radioGroupStudTestOptions.getCheckedRadioButtonId();
            if (selectedId == -1) {
                // Showing warning if no option is selected:
                Toast.makeText(requireContext(), R.string.select_an_option, Toast.LENGTH_SHORT).show();
            } else {
                // Retrieving the selected text:
                RadioButton selectedRadioButton = binding.radioGroupStudTestOptions.findViewById(selectedId);
                String selectedAnswer = selectedRadioButton.getText().toString();
                String activityId = requireArguments().getString(ARG_ACTIVITY_ID);

                // Communicating the answer to the parent fragment:
                Bundle result = new Bundle();
                result.putString(BUNDLE_KEY_ACTIVITY_ID, activityId);
                result.putString(BUNDLE_KEY_SELECTED_OPTION, selectedAnswer);
                getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);

                // Hiding the bottom sheet:
                dismiss();
            }
        });
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