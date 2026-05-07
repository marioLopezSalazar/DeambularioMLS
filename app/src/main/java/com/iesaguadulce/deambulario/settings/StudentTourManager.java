package com.iesaguadulce.deambulario.settings;

import android.graphics.Typeface;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.StudentActivity;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;

/**
 * Performs an interactive User Guide for the student.
 *
 * @author Mario López Salazar.
 */
public class StudentTourManager {

    /**
     * Defines the steps of the Student User Guide.
     */
    public enum TourStep {
        STEP_1_NEXT_MILESTONE_INDICATOR,
        STEP_2_MILESTONE_ACTION_BUTTON,
        STEP_3_LOG,
        STEP_4_MY_PROGRESS,
        STEP_5_SETTINGS,
        STEP_6_SEND_ALERT,
        STEP_7_LOGOUT,
        FINISHED
    }

    /*
     * The current guide step being shown to the student.
     */
    private static TourStep currentStep = TourStep.FINISHED;


    /**
     * Allows to get the current step on the Student Guide.
     *
     * @return The current step on the Student Guide.
     */
    public static TourStep getCurrentStep() {
        return currentStep;
    }

    /**
     * Allows to set a new step the Student Guide. Use only to jump to start, or to mark it finished.
     *
     * @param step The new step for the Student Guide.
     */
    public static void setCurrentStep(TourStep step) {
        currentStep = step;
    }

    /**
     * Builds a base prompt to perform a step for the Student Guide.
     * Guide finishes if the user touches out of the target view.
     *
     * @param activity        The StudentActivity.
     * @param target          The target UI view for the step.
     * @param primaryText     The title to be shown.
     * @param secondaryText   The explicative text to be shown.
     * @param nextStep        The next step.
     * @param onStepCompleted Actions to do when the step will be completed.
     * @return A base prompt to perform a step for the Student Guide.
     */
    private static MaterialTapTargetPrompt.Builder createBasePrompt(
            StudentActivity activity,
            View target,
            String primaryText,
            String secondaryText,
            TourStep nextStep,
            Runnable onStepCompleted) {

        Typeface studentFont = ResourcesCompat.getFont(activity, R.font.student_font);
        return new MaterialTapTargetPrompt.Builder(activity, R.style.Guide_student)
                .setTarget(target)
                .setPrimaryText(primaryText)
                .setSecondaryText(secondaryText)
                .setPrimaryTextTypeface(studentFont)
                .setSecondaryTextTypeface(studentFont)
                .setPromptStateChangeListener((prompt, state) -> {
                    if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                        currentStep = nextStep;
                        if (nextStep == TourStep.FINISHED) {
                            activity.setGuideAsShown();
                        }
                        if (onStepCompleted != null) {
                            onStepCompleted.run();
                        }
                    } else if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                        currentStep = TourStep.FINISHED;
                        activity.setGuideAsShown();
                    }
                });
    }


    /**
     * Student Guide, step 1 - Next milestone indicator.
     *
     * @param activity        The student activity.
     * @param indicator       The next milestone indicator text view.
     * @param onStepCompleted Actions to do when the student clicks on the target. Navigates to step 2.
     */
    public static void checkMilestoneIndicatorTour(StudentActivity activity, View indicator, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_1_NEXT_MILESTONE_INDICATOR) {
            createBasePrompt(activity, indicator,
                    activity.getString(R.string.guide_student_title_1),
                    activity.getString(R.string.guide_student_text_1),
                    TourStep.STEP_2_MILESTONE_ACTION_BUTTON,
                    onStepCompleted)
                    .setPromptFocal(new RectanglePromptFocal())
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Student Guide, steps 2 - Milestone action button.
     *
     * @param activity        The student activity.
     * @param actionButton    The Milestone action button.
     * @param onStepCompleted Actions to do when the student clicks on the target. Navigates to step 3.
     */
    public static void checkMilestoneActionButtonTour(StudentActivity activity, View actionButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_2_MILESTONE_ACTION_BUTTON) {
            createBasePrompt(activity, actionButton,
                    activity.getString(R.string.guide_student_title_2),
                    activity.getString(R.string.guide_student_text_2),
                    TourStep.STEP_3_LOG,
                    onStepCompleted)
                    .setPromptFocal(new RectanglePromptFocal())
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Student Guide, steps 3 - Message log.
     *
     * @param activity        The student activity.
     * @param logTextView     The text view to see the messages log.
     * @param onStepCompleted Actions to do when the student clicks on the target. Navigates to step 4.
     */
    public static void checkMyLogTour(StudentActivity activity, View logTextView, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_3_LOG) {
            createBasePrompt(activity, logTextView,
                    activity.getString(R.string.guide_student_title_3),
                    activity.getString(R.string.guide_student_text_3),
                    TourStep.STEP_4_MY_PROGRESS,
                    onStepCompleted)
                    .setPromptFocal(new RectanglePromptFocal())
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Student Guide, steps 4 - See my progress.
     *
     * @param activity         The student activity.
     * @param myProgressButton The button to see the own progress on the route.
     * @param onStepCompleted  Actions to do when the student clicks on the target. Navigates to step 5.
     */
    public static void checkMyProgressTour(StudentActivity activity, View myProgressButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_4_MY_PROGRESS) {
            createBasePrompt(activity, myProgressButton,
                    activity.getString(R.string.guide_student_title_4),
                    activity.getString(R.string.guide_student_text_4),
                    TourStep.STEP_5_SETTINGS,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Student Guide, steps 5 - Profile settings.
     *
     * @param activity        The student activity.
     * @param profileButton   The button to perform student profile.
     * @param onStepCompleted Actions to do when the student clicks on the target. Navigates to step 5.
     */
    public static void checkStudentProfileTour(StudentActivity activity, View profileButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_5_SETTINGS) {
            createBasePrompt(activity, profileButton,
                    activity.getString(R.string.guide_student_title_5),
                    activity.getString(R.string.guide_student_text_5),
                    TourStep.STEP_6_SEND_ALERT,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Student Guide, steps 6 - Send alert.
     *
     * @param activity        The student activity.
     * @param alertButton     The button to send an alert message.
     * @param onStepCompleted Actions to do when the student clicks on the target. Navigates to step 5.
     */
    public static void checkSendAlertTour(StudentActivity activity, View alertButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_6_SEND_ALERT) {
            createBasePrompt(activity, alertButton,
                    activity.getString(R.string.guide_student_title_6),
                    activity.getString(R.string.guide_student_text_6),
                    TourStep.STEP_7_LOGOUT,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Student Guide, steps 7 - Logout.
     *
     * @param activity         The student activity.
     * @param endSessionButton The button to logout.
     * @param onStepCompleted  Actions to do when the student clicks on the target. Finishes guide.
     */
    public static void checkLogoutTour(StudentActivity activity, View endSessionButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_7_LOGOUT) {
            createBasePrompt(activity, endSessionButton,
                    activity.getString(R.string.guide_student_title_7),
                    activity.getString(R.string.guide_student_text_7),
                    TourStep.FINISHED,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }
}