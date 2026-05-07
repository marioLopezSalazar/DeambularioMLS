package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;

/**
 * POJO class representing a student's answer to a specific milestone activity.
 * Mapped from the array of maps "respuestas" in Firestore.
 *
 * @author Mario López Salazar
 */
public class Answer {

    // --- OBJECT ATTRIBUTES ---

    /*
     * ID of the specific activity being answered.
     */
    @PropertyName(ACTIVITY_ID_FIELD)
    private String activityId;

    /*
     * The answer given by the student.
     * Kept as a String for versatility (text, URL, selected option...).
     */
    @PropertyName(ANSWER_FIELD)
    private String givenAnswer;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    @SuppressWarnings("unused")
    public Answer() {
    }

    /**
     * Constructor for building a student's answer record.
     *
     * @param activityId  ID of the specific activity being answered.
     * @param givenAnswer The text, URL, or option given as an answer (can be null).
     */
    public Answer(@NonNull String activityId, String givenAnswer) {
        this.activityId = activityId;
        if (givenAnswer != null && givenAnswer.trim().isEmpty()) {
            givenAnswer = null;
        }
        this.givenAnswer = givenAnswer;
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
     * Gets the student answer.
     *
     * @return The student answer.
     */
    @PropertyName(ANSWER_FIELD)
    public String getGivenAnswer() {
        return givenAnswer;
    }

    /**
     * Allows to set the student answer.
     *
     * @param givenAnswer The student answer.
     */
    @PropertyName(ANSWER_FIELD)
    public void setGivenAnswer(String givenAnswer) {
        if (givenAnswer != null && givenAnswer.trim().isEmpty()) {
            givenAnswer = null;
        }
        this.givenAnswer = givenAnswer;
    }
}