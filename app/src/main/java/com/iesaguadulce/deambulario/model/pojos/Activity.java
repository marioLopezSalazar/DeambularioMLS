package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.List;

/**
 * Class representing an activity to be done by the student at the milestone.
 * Mapped from the array of maps "actividades" in Firestore.
 *
 * @author Mario López Salazar
 */
public class Activity {

    // --- OBJECT ATTRIBUTES ---

    /*
     * Identifier of the activity.
     */
    @PropertyName(ACTIVITY_ID_FIELD)
    private String activityId;

    /*
     * Activity type in Firestore.
     */
    @PropertyName(ACTIVITY_TYPE)
    private String type;

    /*
     * Text description of the activity.
     */
    @PropertyName(ACTIVITY_TEXT)
    private String text;

    /*
     * List of possible answers in multiple-choice activities (can be null).
     */
    @PropertyName(ACTIVITY_OPTIONS)
    private List<String> options;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    public Activity() {
    }

    /**
     * Constructor for building a milestone activity.
     *
     * @param activityId Identifier of the activity.
     * @param type       Activity type.
     * @param text       Text description of the activity.
     * @param options    List of possible answers in multiple-choice activities (can be null).
     */
    public Activity(@NonNull String activityId, @NonNull ActivityType type, @NonNull String text, List<String> options) {
        this.activityId = activityId;
        this.type = type.getFirestoreValue();
        this.text = text;
        if (options != null && options.isEmpty()) {
            options = null;
        }
        this.options = options;
    }

    // --- GETTERS AND SETTERS ---

    /**
     * Gets the identifier of the activity.
     *
     * @return Activity identifier.
     */
    @PropertyName(ACTIVITY_ID_FIELD)
    public String getActivityId() {
        return activityId;
    }

    /**
     * Allows to set the identifier of the activity.
     *
     * @param activityId Activity identifier.
     */
    @PropertyName(ACTIVITY_ID_FIELD)
    public void setActivityId(@NonNull String activityId) {
        this.activityId = activityId;
    }

    /**
     * Gets the type of the activity.
     *
     * @return Activity type.
     */
    @Exclude
    public ActivityType getType() {
        return ActivityType.fromString(type);
    }

    /**
     * Gets the type of the activity on Firebase format.
     *
     * @return Activity type on Firebase format.
     */
    @PropertyName(ACTIVITY_TYPE)
    public String getTypeString() {
        return type;
    }

    /**
     * Allows to set the type of the activity.
     *
     * @param type Activity type.
     */
    @Exclude
    public void setType(@NonNull ActivityType type) {
        this.type = type.getFirestoreValue();
    }

    /**
     * Allows to set the type of the activity using Firebase format.
     *
     * @param type Activity type on Firebase format.
     */
    @PropertyName(ACTIVITY_TYPE)
    public void setTypeString(@NonNull String type) {
        this.type = type;
    }

    /**
     * Gets the text description of the activity.
     *
     * @return Text description of the activity.
     */
    @PropertyName(ACTIVITY_TEXT)
    public String getText() {
        return text;
    }

    /**
     * Allows to set the text description of the activity.
     *
     * @param text Text description of the activity.
     */
    @PropertyName(ACTIVITY_TEXT)
    public void setText(@NonNull String text) {
        this.text = text;
    }

    /**
     * Gets the list of possible answers for multiple-choice activities.
     *
     * @return List of possible answers (can be null).
     */
    @PropertyName(ACTIVITY_OPTIONS)
    public List<String> getOptions() {
        return options;
    }

    /**
     * Allows to set the list of possible answers for multiple-choice activities.
     *
     * @param options List of possible answers.
     */
    @PropertyName(ACTIVITY_OPTIONS)
    public void setOptions(List<String> options) {
        if (options != null && options.isEmpty()) {
            options = null;
        }
        this.options = options;
    }

    /**
     * Enum containing different kinds of activities.
     */
    public enum ActivityType {
        /**
         * Open question activity.
         */
        QUESTION(ACTIVITY_QUESTION),

        /**
         * 'Take a photo' activity.
         */
        PHOTO(ACTIVITY_PHOTO),

        /**
         * 'Take a video' activity.
         */
        VIDEO(ACTIVITY_VIDEO),

        /**
         * Test activity.
         */
        TEST(ACTIVITY_TEST);



        /*
         * Firestore string value corresponding to the activity type.
         */
        private final String firestoreValue;


        /**
         * ActivityType constructor from a string.
         *
         * @param firestoreValue Firestore string value.
         */
        ActivityType(@NonNull String firestoreValue) {
            this.firestoreValue = firestoreValue;
        }

        /**
         * Allows to recover the Firestore value of an ActivityType.
         *
         * @return The Firestore value of the ActivityType.
         */
        public String getFirestoreValue() {
            return firestoreValue;
        }

        /**
         * Allows to get an ActivityType from a Firestore string.
         *
         * @param text String from Firestore indicating the type of the activity.
         * @return The corresponding ActivityType, or null if not matches.
         */
        public static ActivityType fromString(String text) {
            for (ActivityType type : ActivityType.values()) {
                if (type.firestoreValue.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            return null;
        }
    }
}