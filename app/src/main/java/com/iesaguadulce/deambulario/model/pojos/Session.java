package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * POJO class representing a session of a route.
 *
 * @author Mario López Salazar
 */
public class Session {

    // --- OBJECT ATTRIBUTES ---

    /*
     * Session ID in Firestore.
     */
    @DocumentId
    private String id;

    /*
     * ID of the teacher owner of the session.
     */
    @PropertyName(TEACHER_UID_FIELD)
    private String teacherId;

    /*
     * FCM Token of the teacher's device to receive SOS notifications.
     */
    @PropertyName(TEACHER_FCM_TOKEN_FIELD)
    private String teacherFcmToken;

    /*
     * ID of the route associated with this session.
     */
    @PropertyName(ROUTE_ID_FIELD)
    private String routeId;

    /*
     * PIN code to allow students to join the session.
     */
    @PropertyName(PIN_FIELD)
    private int pin;

    /*
     * Current state of the session in Firestore.
     */
    @PropertyName(STATUS_FIELD)
    private String state;

    /*
     * Date of the session creation or execution.
     */
    @PropertyName(DATE_FIELD)
    private Date date;

    /*
     * Snapshot of the route title to preserve it even if the original route is modified.
     */
    @PropertyName(TITLE_SNAPSHOT_FIELD)
    private String titleSnapshot;

    /*
     * Snapshot of the activities to preserve them even if the original route is modified.
     */
    @PropertyName(ACTIVITIES_SNAPSHOT_FIELD)
    private List<Activity> activitiesSnapshot;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    public Session() {
    }

    /**
     * Constructor for building a new Session.
     *
     * @param teacherId          ID of the teacher who owns the session.
     * @param teacherFcmToken    FCM Token to send push notifications to the teacher.
     * @param routeId            ID of the route associated with this session.
     * @param pin                PIN code to allow students to join.
     * @param state              Current state of the session.
     * @param date               Date of the session.
     * @param titleSnapshot      Snapshot of the title of the route.
     * @param activitiesSnapshot Snapshot of the activities of the route.
     */
    public Session(@NonNull String teacherId, String teacherFcmToken, @NonNull String routeId, int pin, @NonNull SessionState state, @NonNull Date date, @NonNull String titleSnapshot, List<Activity> activitiesSnapshot) {
        this.teacherId = teacherId;
        this.teacherFcmToken = teacherFcmToken;
        this.routeId = routeId;
        this.pin = pin;
        this.state = state.getFirestoreValue();
        this.date = date;
        this.titleSnapshot = titleSnapshot;
        this.activitiesSnapshot = activitiesSnapshot;
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the ID of the session.
     *
     * @return Session ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Allows to set the ID of the session.
     *
     * @param id Session ID.
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Gets the ID of the teacher who owns the session.
     *
     * @return Teacher ID.
     */
    @PropertyName(TEACHER_UID_FIELD)
    public String getTeacherId() {
        return teacherId;
    }

    /**
     * Allows to set the ID of the teacher who owns the session.
     *
     * @param teacherId Teacher ID.
     */
    @PropertyName(TEACHER_UID_FIELD)
    public void setTeacherId(@NonNull String teacherId) {
        this.teacherId = teacherId;
    }

    /**
     * Gets the FCM token of the teacher who owns the session.
     *
     * @return FCM token string.
     */
    @PropertyName(TEACHER_FCM_TOKEN_FIELD)
    public String getTeacherFcmToken() {
        return teacherFcmToken;
    }

    /**
     * Allows to set the FCM token of the teacher who owns the session.
     *
     * @param teacherFcmToken FCM token string.
     */
    @PropertyName(TEACHER_FCM_TOKEN_FIELD)
    public void setTeacherFcmToken(String teacherFcmToken) {
        this.teacherFcmToken = teacherFcmToken;
    }

    /**
     * Gets the ID of the route associated with this session.
     *
     * @return Route ID.
     */
    @PropertyName(ROUTE_ID_FIELD)
    public String getRouteId() {
        return routeId;
    }

    /**
     * Allows to set the ID of the route associated with this session.
     *
     * @param routeId Route ID.
     */
    @PropertyName(ROUTE_ID_FIELD)
    public void setRouteId(@NonNull String routeId) {
        this.routeId = routeId;
    }

    /**
     * Gets the PIN code of the session.
     *
     * @return Session PIN code.
     */
    @PropertyName(PIN_FIELD)
    public int getPin() {
        return pin;
    }

    /**
     * Allows to set the PIN code of the session.
     *
     * @param pin Session PIN code.
     */
    @PropertyName(PIN_FIELD)
    public void setPin(int pin) {
        this.pin = pin;
    }

    /**
     * Gets the state of the session.
     *
     * @return Session state as Enum.
     */
    @Exclude
    public SessionState getState() {
        return SessionState.fromString(state);
    }

    /**
     * Gets the state of the session in Firebase format.
     *
     * @return Session state string.
     */
    @PropertyName(STATUS_FIELD)
    public String getStateString() {
        return state;
    }

    /**
     * Allows to set the state of the session safely using the Enum.
     *
     * @param state Session state Enum.
     */
    @Exclude
    public void setState(@NonNull SessionState state) {
        this.state = state.getFirestoreValue();
    }

    /**
     * Allows to set the state of the session using Firebase format.
     *
     * @param state Session state string.
     */
    @PropertyName(STATUS_FIELD)
    public void setStateString(@NonNull String state) {
        this.state = state;
    }

    /**
     * Gets the date of the session.
     *
     * @return Session date string.
     */
    @PropertyName(DATE_FIELD)
    public Date getDate() {
        return date;
    }

    /**
     * Allows to set the date of the session.
     *
     * @param date Session date string.
     */
    @PropertyName(DATE_FIELD)
    public void setDate(@NonNull Date date) {
        this.date = date;
    }

    /**
     * Gets the snapshot of the route title for this session.
     *
     * @return Title of the route.
     */
    @PropertyName(TITLE_SNAPSHOT_FIELD)
    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    /**
     * Allows to set the snapshot of the title.
     *
     * @param titleSnapshot Title of the route.
     */
    @PropertyName(TITLE_SNAPSHOT_FIELD)
    public void setTitleSnapshot(String titleSnapshot) {
        this.titleSnapshot = titleSnapshot;
    }

    /**
     * Gets the snapshot of activities for this session.
     *
     * @return List of activities.
     */
    @PropertyName(ACTIVITIES_SNAPSHOT_FIELD)
    public List<Activity> getActivitiesSnapshot() {
        return activitiesSnapshot;
    }

    /**
     * Allows to set the snapshot of activities.
     *
     * @param activitiesSnapshot List of activities.
     */
    @PropertyName(ACTIVITIES_SNAPSHOT_FIELD)
    public void setActivitiesSnapshot(List<Activity> activitiesSnapshot) {
        this.activitiesSnapshot = activitiesSnapshot;
    }


    // ====================== //
    // ENUM FOR SESSION STATE //
    // ====================== //

    /**
     * Enum containing different states of a session.
     */
    public enum SessionState {
        /**
         * Session on lobby, waiting for student login, not started yet.
         */
        WAITING(STATUS_WAITING),

        /**
         * Started session. Students can do milestones and teacher can track.
         */
        ACTIVE(STATUS_ACTIVE),

        /**
         * Closed session. Teacher can download students answers.
         */
        CLOSED(STATUS_CLOSED);


        /*
         * Firestore string value.
         */
        private final String firestoreValue;

        /**
         * SessionState constructor from a string.
         *
         * @param firestoreValue Firestore string value.
         */
        SessionState(@NonNull String firestoreValue) {
            this.firestoreValue = firestoreValue;
        }

        /**
         * Allows to recover the Firestore value of a SessionState.
         *
         * @return The Firestore value of the SessionState.
         */
        public String getFirestoreValue() {
            return firestoreValue;
        }

        /**
         * Allows to get a SessionState from a Firestore string.
         *
         * @param text String from Firestore indicating the state of the session.
         * @return The corresponding SessionState, or null if not matches.
         */
        public static SessionState fromString(String text) {
            for (SessionState state : SessionState.values()) {
                if (state.firestoreValue.equalsIgnoreCase(text)) {
                    return state;
                }
            }
            return null;
        }
    }


    // --- UTILS METHODS ---

    /**
     * Checks if this session is equal to another object.
     * Two sessions are considered equal if their IDs are the same.
     *
     * @param o The object to compare with.
     * @return True if the object is a Session with the same ID, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Session)) {
            return false;
        }
        Session session = (Session) o;
        return Objects.equals(id, session.id);
    }


    /**
     * Obtains a hashcode for the Session object.
     *
     * @return Hashcode, based on the Session.getId() result.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}