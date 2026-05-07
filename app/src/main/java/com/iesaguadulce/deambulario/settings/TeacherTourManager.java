package com.iesaguadulce.deambulario.settings;

import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.widget.Toolbar;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;


/**
 * Performs an interactive User Guide for the teacher.
 *
 * @author Mario López Salazar.
 */
public class TeacherTourManager {

    /**
     * Defines the steps of the Teacher User Guide.
     */
    public enum TourStep {
        STEP_1_WELCOME_AND_ROUTES_LIST,
        STEP_2_ROUTE_INFO_AND_ADD_MILESTONES_BUTTON,
        STEP_3_MILESTONE_TITLE_AND_POSITION,
        STEP_4_ADD_CONTENT,
        STEP_5_ADD_ACTIVITY,
        STEP_6_AUTOSAVE,
        STEP_7_MILESTONE_LIST,
        STEP_8_MILESTONES_EDITION,
        STEP_9_IN_ORDER_ROUTE,
        STEP_10_GEOFENCE,
        STEP_11_ENABLE_GEOFENCE,
        STEP_12_AUTOSAVE,
        STEP_13_ROUTE_DETAIL_ACCESS,
        STEP_14_CREATE_SESSION,
        STEP_15_SESSION_LOBBY,
        STEP_16_SESSION_TRACK,
        STEP_17_PROGRESSION,
        STEP_18_LOG,
        STEP_19_BROADCAST,
        STEP_20_END_SESSION,
        STEP_21_SESSIONS_LIST,
        STEP_22_SESSION_MANAGEMENT,
        STEP_23_SETTINGS,
        FINISHED
    }

    /*
     * The current guide step being shown to the teacher.
     */
    private static TourStep currentStep = TourStep.FINISHED;


    /**
     * Allows to know if the app is showing the Teacher Guide.
     *
     * @return True if the app is showing the Teacher Guide, false otherwise.
     */
    public static boolean onGuideMode(){
        return TeacherTourManager.getCurrentStep() != TeacherTourManager.TourStep.FINISHED;
    }

    /**
     * Allows to get the current step on the Teacher Guide.
     *
     * @return The current step on the Teacher Guide.
     */
    public static TourStep getCurrentStep() {
        return currentStep;
    }

    /**
     * Allows to set a new step the Teacher Guide. Use only to jump to start, or to mark it finished.
     *
     * @param step The new step for the Teacher Guide.
     */
    public static void setCurrentStep(TourStep step) {
        currentStep = step;
    }

    /**
     * Builds a base prompt to perform a step for the Teacher Guide.
     * Guide finishes if the user touches out of the target view.
     *
     * @param activity The TeacherActivity.
     * @param target The target UI view for the step.
     * @param primaryText The title to be shown.
     * @param secondaryText The explicative text to be shown.
     * @param nextStep The next step.
     * @param onStepCompleted Actions to do when the step will be completed.
     *
     * @return A base prompt to perform a step for the Teacher Guide.
     */
    private static MaterialTapTargetPrompt.Builder createBasePrompt(
            TeacherActivity activity,
            View target,
            String primaryText,
            String secondaryText,
            TourStep nextStep,
            Runnable onStepCompleted) {

        return new MaterialTapTargetPrompt.Builder(activity, R.style.Guide_teacher)
                .setTarget(target)
                .setPrimaryText(primaryText)
                .setSecondaryText(secondaryText)
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
     * Teacher Guide, step 1 - Routes list & Create route button.
     *
     * @param activity        The teacher activity.
     * @param addRouteButton  The 'Add Route' button.
     * @param onStepCompleted Actions to do when the teacher clicks on the 'add route' button. Navigates to step 2.
     */
    public static void startManageTeacherTour(TeacherActivity activity, View addRouteButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_1_WELCOME_AND_ROUTES_LIST) {
            createBasePrompt(activity, addRouteButton,
                    activity.getString(R.string.guide_teacher_title_1),
                    activity.getString(R.string.guide_teacher_text_1),
                    TourStep.STEP_2_ROUTE_INFO_AND_ADD_MILESTONES_BUTTON,
                    onStepCompleted)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 2 - Route basics.
     *
     * @param activity           The teacher activity.
     * @param addMilestoneButton The 'Add milestone' button.
     * @param onStepCompleted    Actions to do when the teacher clicks on the 'Add milestone' button. Navigates to step 3.
     */
    public static void checkCreateRouteTour(TeacherActivity activity, View addMilestoneButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_2_ROUTE_INFO_AND_ADD_MILESTONES_BUTTON) {
            createBasePrompt(activity, addMilestoneButton,
                    activity.getString(R.string.guide_teacher_title_2),
                    activity.getString(R.string.guide_teacher_text_2),
                    TourStep.STEP_3_MILESTONE_TITLE_AND_POSITION,
                    onStepCompleted)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 3 - Milestone basics.
     *
     * @param activity               The teacher activity.
     * @param setUpCoordinatesButton The 'Add milestone' button.
     * @param onStepCompleted        Actions to do when the teacher clicks on the 'Set Coordinates' button. Navigates to step 4.
     */
    public static void checkMilestoneDetailsTour(TeacherActivity activity, View setUpCoordinatesButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_3_MILESTONE_TITLE_AND_POSITION) {
            createBasePrompt(activity, setUpCoordinatesButton,
                    activity.getString(R.string.guide_teacher_title_3),
                    activity.getString(R.string.guide_teacher_text_3),
                    TourStep.STEP_4_ADD_CONTENT,
                    onStepCompleted)
                    .setPromptFocal(new RectanglePromptFocal())
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 4 - Milestone add content
     *
     * @param activity         The teacher activity.
     * @param addContentButton The 'Add milestone' button.
     * @param onStepCompleted  Actions to do when the teacher clicks on the 'Add content' button. Navigates to step 5.
     */
    public static void checkAddContentsTour(TeacherActivity activity, View addContentButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_4_ADD_CONTENT) {
            createBasePrompt(activity, addContentButton,
                    activity.getString(R.string.guide_teacher_title_4),
                    activity.getString(R.string.guide_teacher_text_4),
                    TourStep.STEP_5_ADD_ACTIVITY,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 5 - Milestone add activity
     *
     * @param activity          The teacher activity.
     * @param addActivityButton The 'Add milestone' button.
     * @param onStepCompleted   Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 6.
     */
    public static void checkAddActivityTour(TeacherActivity activity, View addActivityButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_5_ADD_ACTIVITY) {
            createBasePrompt(activity, addActivityButton,
                    activity.getString(R.string.guide_teacher_title_5),
                    activity.getString(R.string.guide_teacher_text_5),
                    TourStep.STEP_6_AUTOSAVE,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 6 - Autosave
     *
     * @param activity        The teacher activity.
     * @param toolbar         The Toolbar containing the back button.
     * @param onStepCompleted Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 7.
     */
    public static void checkFinishMilestoneTour(TeacherActivity activity, Toolbar toolbar, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_6_AUTOSAVE) {

            // Getting the 'Back' button on the Toolbar:
            View back = null;
            boolean found = false;
            for (int i = 0; !found && i < toolbar.getChildCount(); i++) {
                if (toolbar.getChildAt(i) instanceof ImageButton) {
                    back = toolbar.getChildAt(i);
                    found = true;
                }
            }
            if (back == null) {
                back = toolbar;
            }

            createBasePrompt(activity, back,
                    activity.getString(R.string.guide_teacher_title_6),
                    activity.getString(R.string.guide_teacher_text_6),
                    TourStep.STEP_7_MILESTONE_LIST,
                    onStepCompleted)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 7 - Milestones list.
     *
     * @param activity        The teacher activity.
     * @param dragButton      A drag button on the milestones list.
     * @param onStepCompleted Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 8.
     */
    public static void checkMilestonesListTour(TeacherActivity activity, View dragButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_7_MILESTONE_LIST) {
            createBasePrompt(activity, dragButton,
                    activity.getString(R.string.guide_teacher_title_7),
                    activity.getString(R.string.guide_teacher_text_7),
                    TourStep.STEP_8_MILESTONES_EDITION,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 8 - Access to milestone details.
     *
     * @param activity        The teacher activity.
     * @param detailsButton   A details button on the milestones list.
     * @param onStepCompleted Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 9.
     */
    public static void checkMilestoneEditTour(TeacherActivity activity, View detailsButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_8_MILESTONES_EDITION) {
            createBasePrompt(activity, detailsButton,
                    activity.getString(R.string.guide_teacher_title_8),
                    activity.getString(R.string.guide_teacher_text_8),
                    TourStep.STEP_9_IN_ORDER_ROUTE,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 9 - In order route.
     *
     * @param activity        The teacher activity.
     * @param inOrderSwitch   The switch to perform the route to be walked in order.
     * @param onStepCompleted Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 10.
     */
    public static void checkRouteInOrderTour(TeacherActivity activity, View inOrderSwitch, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_9_IN_ORDER_ROUTE) {
            createBasePrompt(activity, inOrderSwitch,
                    activity.getString(R.string.guide_teacher_title_9),
                    activity.getString(R.string.guide_teacher_text_9),
                    TourStep.STEP_10_GEOFENCE,
                    onStepCompleted)
                    .show();
        }
    }


    /**
     * Teacher Guide, step 10 - Set geofence.
     *
     * @param activity          The teacher activity.
     * @param setGeofenceButton The 'Set geofence' button.
     * @param onStepCompleted   Actions to do when the teacher clicks on the 'Set geofence' button. Navigates to step 11.
     */
    public static void checkGeofenceTour(TeacherActivity activity, View setGeofenceButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_10_GEOFENCE) {
            createBasePrompt(activity, setGeofenceButton,
                    activity.getString(R.string.guide_teacher_title_10),
                    activity.getString(R.string.guide_teacher_text_10),
                    TourStep.STEP_11_ENABLE_GEOFENCE,
                    onStepCompleted)
                    .setPromptFocal(new RectanglePromptFocal())
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, step 11 - Enable geofence.
     *
     * @param activity        The teacher activity.
     * @param geofenceSwitch  The 'Enable geofence' switch.
     * @param onStepCompleted Actions to do when the teacher clicks on the 'Set geofence' button. Navigates to step 12.
     */
    public static void checkEnableGeofenceTour(TeacherActivity activity, View geofenceSwitch, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_11_ENABLE_GEOFENCE) {
            createBasePrompt(activity, geofenceSwitch,
                    activity.getString(R.string.guide_teacher_title_11),
                    activity.getString(R.string.guide_teacher_text_11),
                    TourStep.STEP_12_AUTOSAVE,
                    onStepCompleted)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 12 - Autosave
     *
     * @param activity The teacher activity.
     * @param toolbar  The Toolbar containing the back button.
     */
    public static void checkFinishRouteTour(TeacherActivity activity, Toolbar toolbar) {
        if (currentStep == TourStep.STEP_12_AUTOSAVE) {

            // Getting the 'Back' button on the Toolbar:
            View back = null;
            boolean found = false;
            for (int i = 0; !found && i < toolbar.getChildCount(); i++) {
                if (toolbar.getChildAt(i) instanceof ImageButton) {
                    back = toolbar.getChildAt(i);
                    found = true;
                }
            }
            if (back == null) {
                back = toolbar;
            }

            createBasePrompt(activity, back,
                    activity.getString(R.string.guide_teacher_title_12),
                    activity.getString(R.string.guide_teacher_text_12),
                    TourStep.STEP_13_ROUTE_DETAIL_ACCESS,
                    null)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 13 - Access to route details.
     *
     * @param activity            The teacher activity.
     * @param createSessionButton A details button on the routes list.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 14.
     */
    public static void checkRouteEditTour(TeacherActivity activity, View createSessionButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_13_ROUTE_DETAIL_ACCESS) {
            createBasePrompt(activity, createSessionButton,
                    activity.getString(R.string.guide_teacher_title_13),
                    activity.getString(R.string.guide_teacher_text_13),
                    TourStep.STEP_14_CREATE_SESSION,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 14 - Create session.
     *
     * @param activity            The teacher activity.
     * @param createSessionButton A 'Create session' button on the routes list.
     */
    public static void checkCreateSessionTour(TeacherActivity activity, View createSessionButton) {
        if (currentStep == TourStep.STEP_14_CREATE_SESSION) {
            createBasePrompt(activity, createSessionButton,
                    activity.getString(R.string.guide_teacher_title_14),
                    activity.getString(R.string.guide_teacher_text_14),
                    TourStep.STEP_15_SESSION_LOBBY,
                    null)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 15 - Start session.
     *
     * @param activity            The teacher activity.
     * @param startSessionButton  The 'Start session' button on the routes list.
     */
    public static void checkStartSessionTour(TeacherActivity activity, View startSessionButton) {
        if (currentStep == TourStep.STEP_15_SESSION_LOBBY) {
            createBasePrompt(activity, startSessionButton,
                    activity.getString(R.string.guide_teacher_title_15),
                    activity.getString(R.string.guide_teacher_text_15),
                    TourStep.STEP_16_SESSION_TRACK,
                    null)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 16 - Student status.
     *
     * @param activity            The teacher activity.
     * @param studentCard  A student card on the session track.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 17.
     */
    public static void checkStudentStatusTour(TeacherActivity activity, View studentCard, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_16_SESSION_TRACK) {
            createBasePrompt(activity, studentCard,
                    activity.getString(R.string.guide_teacher_title_16),
                    activity.getString(R.string.guide_teacher_text_16),
                    TourStep.STEP_17_PROGRESSION,
                    onStepCompleted)
                    .setPromptFocal(new RectanglePromptFocal())
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 17 - Student progression.
     *
     * @param activity            The teacher activity.
     * @param studentCard         A student card on the session track.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 18.
     */
    public static void checkStudentProgressionTour(TeacherActivity activity, View studentCard, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_17_PROGRESSION) {
            createBasePrompt(activity, studentCard,
                    activity.getString(R.string.guide_teacher_title_17),
                    activity.getString(R.string.guide_teacher_text_17),
                    TourStep.STEP_18_LOG,
                    onStepCompleted)
                    .setPromptFocal(new RectanglePromptFocal())
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 18 - Messages log.
     *
     * @param activity            The teacher activity.
     * @param logButton           The button to see the messages log.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 19.
     */
    public static void checkLogTour(TeacherActivity activity, View logButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_18_LOG) {
            createBasePrompt(activity, logButton,
                    activity.getString(R.string.guide_teacher_title_18),
                    activity.getString(R.string.guide_teacher_text_18),
                    TourStep.STEP_19_BROADCAST,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 19 - Broadcast.
     *
     * @param activity            The teacher activity.
     * @param broadcastButton           The button to send a broadcast message.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 20.
     */
    public static void checkBroadcastTour(TeacherActivity activity, View broadcastButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_19_BROADCAST) {
            createBasePrompt(activity, broadcastButton,
                    activity.getString(R.string.guide_teacher_title_19),
                    activity.getString(R.string.guide_teacher_text_19),
                    TourStep.STEP_20_END_SESSION,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 20 - End session.
     *
     * @param activity            The teacher activity.
     * @param endSessionButton    The button to end session.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 20.
     */
    public static void checkEndSessionTour(TeacherActivity activity, View endSessionButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_20_END_SESSION) {
            createBasePrompt(activity, endSessionButton,
                    activity.getString(R.string.guide_teacher_title_20),
                    activity.getString(R.string.guide_teacher_text_20),
                    TourStep.STEP_21_SESSIONS_LIST,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 21 - Sessions list.
     *
     * @param activity            The teacher activity.
     * @param sessionsButton      The 'Sessions' button on the Bottom navigation bar.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 22.
     */
    public static void checkSessionListTour(TeacherActivity activity, View sessionsButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_21_SESSIONS_LIST) {
            createBasePrompt(activity, sessionsButton,
                    activity.getString(R.string.guide_teacher_title_21),
                    activity.getString(R.string.guide_teacher_text_21),
                    TourStep.STEP_22_SESSION_MANAGEMENT,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }

    /**
     * Teacher Guide, steps 22 - Sessions management.
     *
     * @param activity            The teacher activity.
     * @param sessionButton       The action button of a session.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to step 23.
     */
    public static void checkSessionManagementTour(TeacherActivity activity, View sessionButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_22_SESSION_MANAGEMENT) {
            createBasePrompt(activity, sessionButton,
                    activity.getString(R.string.guide_teacher_title_22),
                    activity.getString(R.string.guide_teacher_text_22),
                    TourStep.STEP_23_SETTINGS,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }


    /**
     * Teacher Guide, steps 23 - Teacher settings.
     *
     * @param activity            The teacher activity.
     * @param settingsButton      The 'Sessions' button on the Bottom navigation bar.
     * @param onStepCompleted     Actions to do when the teacher clicks on the 'Add activity' button. Navigates to Home.
     */
    public static void checkSettingsTour(TeacherActivity activity, View settingsButton, Runnable onStepCompleted) {
        if (currentStep == TourStep.STEP_23_SETTINGS) {
            createBasePrompt(activity, settingsButton,
                    activity.getString(R.string.guide_teacher_title_23),
                    activity.getString(R.string.guide_teacher_text_23),
                    TourStep.FINISHED,
                    onStepCompleted)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
        }
    }
}