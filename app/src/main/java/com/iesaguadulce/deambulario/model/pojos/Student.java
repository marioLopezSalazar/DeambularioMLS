package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * POJO class representing a Student participating in a session.
 *
 * @author Mario López Salazar
 */
@SuppressWarnings("unused")
public class Student {

    // --- OBJECT ATTRIBUTES ---

    /*
     * Student ID (student Firebase Authentication UID).
     */
    @DocumentId
    private String id;

    /*
     * Student's nickname chosen to join the session.
     */
    @PropertyName(NICK_FIELD)
    private String nick;

    /*
     * FCM Token of the student's device to receive push notifications.
     */
    @PropertyName(STUDENT_FCM_TOKEN_FIELD)
    private String fcmToken;

    /*
     * Student's last known location and timestamp (can be null).
     */
    @PropertyName(LOCATION_FIELD)
    private StudentLocation location;

    /*
     * List of milestone IDs that the student has already visited (can be null).
     */
    @PropertyName(VISITED_FIELD)
    private List<String> visitedMilestones;

    /*
     * List of answers given by the student to the different activities (can be null).
     */
    @PropertyName(ANSWERS_FIELD)
    private List<Answer> answers;

    /*
     * Current status of the student during the route.
     */
    @Exclude
    private LiveStatus liveStatus;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    public Student() {
    }

    /**
     * Constructor for building a new Student participant.
     *
     * @param nick              Student's nickname.
     * @param fcmToken          Student's FCM token for notifications (can be null if not yet generated).
     * @param location          Student's last known location (can be null).
     * @param visitedMilestones List of milestone IDs visited by the student (can be null).
     * @param answers           List of answers given by the student (can be null).
     */
    public Student(@NonNull String nick, String fcmToken, StudentLocation location, List<String> visitedMilestones, List<Answer> answers) {
        this.nick = nick;
        this.fcmToken = fcmToken;
        this.location = location;

        if (visitedMilestones != null && visitedMilestones.isEmpty()) {
            visitedMilestones = null;
        }
        this.visitedMilestones = visitedMilestones;

        if (answers != null && answers.isEmpty()) {
            answers = null;
        }
        this.answers = answers;
        this.liveStatus = null;
    }


    /**
     * Copy constructor to create an independent instance of a Student.
     *
     * @param student The Student object to copy.
     */
    public Student(@NonNull Student student) {
        this.id = student.id;
        this.nick = student.nick;
        this.fcmToken = student.fcmToken;
        this.liveStatus = student.liveStatus;
        if (student.location != null) {
            Date clonedDate = student.location.getTimestamp() != null
                    ? (Date) student.location.getTimestamp().clone()
                    : null;
            this.location = new StudentLocation(student.location.getCoordinates(), clonedDate);
        }
        if (student.visitedMilestones != null) {
            this.visitedMilestones = new ArrayList<>(student.visitedMilestones);
        }
        if (student.answers != null) {
            this.answers = new ArrayList<>(student.answers);
        }
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the ID of the student.
     *
     * @return Student ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Allows to set the ID of the student.
     *
     * @param id Student ID.
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Gets the nickname of the student.
     *
     * @return Student nickname.
     */
    @PropertyName(NICK_FIELD)
    public String getNick() {
        return nick;
    }

    /**
     * Allows to set the nickname of the student.
     *
     * @param nick Student nickname.
     */
    @PropertyName(NICK_FIELD)
    public void setNick(@NonNull String nick) {
        this.nick = nick;
    }

    /**
     * Gets the FCM token of the student.
     *
     * @return FCM token string.
     */
    @PropertyName(STUDENT_FCM_TOKEN_FIELD)
    public String getFcmToken() {
        return fcmToken;
    }

    /**
     * Allows to set the FCM token of the student.
     *
     * @param fcmToken FCM token string.
     */
    @PropertyName(STUDENT_FCM_TOKEN_FIELD)
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * Gets the last known location of the student.
     *
     * @return Student location (can be null).
     */
    @PropertyName(LOCATION_FIELD)
    public StudentLocation getLocation() {
        return location;
    }

    /**
     * Allows to set the last known location of the student.
     *
     * @param location Student location.
     */
    @PropertyName(LOCATION_FIELD)
    public void setLocation(StudentLocation location) {
        this.location = location;
    }

    /**
     * Gets the list of milestone IDs visited by the student.
     *
     * @return List of visited milestone IDs (can be null).
     */
    @PropertyName(VISITED_FIELD)
    public List<String> getVisitedMilestones() {
        return visitedMilestones;
    }

    /**
     * Allows to set the list of milestone IDs visited by the student.
     *
     * @param visitedMilestones List of visited milestone IDs.
     */
    @PropertyName(VISITED_FIELD)
    public void setVisitedMilestones(List<String> visitedMilestones) {
        if (visitedMilestones != null && visitedMilestones.isEmpty()) {
            visitedMilestones = null;
        }
        this.visitedMilestones = visitedMilestones;
    }

    /**
     * Gets the list of answers given by the student.
     *
     * @return List of answers (can be null).
     */
    @PropertyName(ANSWERS_FIELD)
    public List<Answer> getAnswers() {
        return answers;
    }

    /**
     * Allows to set the list of answers given by the student.
     *
     * @param answers List of answers.
     */
    @PropertyName(ANSWERS_FIELD)
    public void setAnswers(List<Answer> answers) {
        if (answers != null && answers.isEmpty()) {
            answers = null;
        }
        this.answers = answers;
    }

    /**
     * Gets the current student status during the route.
     *
     * @return The computed live status of the student.
     */
    @Exclude
    public LiveStatus getLiveStatus() {
        return liveStatus;
    }

    /**
     * Sets the computed status.
     *
     * @param liveStatus The current status of the student.
     */
    @Exclude
    public void setLiveStatus(LiveStatus liveStatus) {
        this.liveStatus = liveStatus;
    }



    // --- UTILS METHODS ---

    /**
     * Checks if this student is equal to another object.
     * Two students are considered equal if their IDs are the same.
     *
     * @param o The object to compare with.
     * @return True if the object is a Student with the same ID, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Student)) {
            return false;
        }
        Student student = (Student) o;
        return Objects.equals(id, student.id);
    }


    /**
     * Obtains a hashcode for the student object.
     *
     * @return Hashcode, based on the Student.getId() result.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }



    // ======================================== //
    // STUDENT STATUS AND PROGRESS ON SESSION   //
    // ======================================== //

    /**
     * Represents the real-time status of a student during a session,
     * computed from connectivity, progress, and spatial constraints.
     */
    public static class LiveStatus {

        /**
         * Enum for the connectivity state of the student.
         */
        public enum Connection {
            /**
             * Student is active and sending location updates normally.
             */
            ONLINE,

            /**
             * Student has not sent location updates for a significant period (e.g., > 2 mins).
             */
            LOST_SIGNAL,

            /**
             * Student has closed the app or the activity has been destroyed,
             * but has not explicitly abandoned the session.
             */
            DISCONNECTED,

            /**
             * Student has explicitly left the session (location and token are null).
             */
            ABANDONED
        }

        /*
         * Status private attributes:
         */
        private final Connection connection;
        private final boolean isFinished;
        private final boolean isOutOfGeofence;

        /**
         * Constructor for the computed live status.
         *
         * @param connection      The current connectivity state.
         * @param isFinished      True if all milestones and activities are completed.
         * @param isOutOfGeofence True if the student is outside the established session area.
         */
        public LiveStatus(@NonNull Connection connection, boolean isFinished, boolean isOutOfGeofence) {
            this.connection = connection;
            this.isFinished = isFinished;
            this.isOutOfGeofence = isOutOfGeofence;
        }

        /**
         * Allows to know the current connectivity state of the student.
         *
         * @return The current connectivity state of the student.
         */
        public Connection getConnection() {
            return connection;
        }

        /**
         * Allows to know if the student has finished all session requirements.
         *
         * @return True if the student has finished all session requirements.
         */
        public boolean isFinished() {
            return isFinished;
        }

        /**
         * Allows to know if the student is currently out of the geofence.
         *
         * @return True if the student is currently out of the geofence.
         */
        public boolean isOutOfGeofence() {
            return isOutOfGeofence;
        }

        /**
         * Checks if a LiveStatus is equal to another object.
         *
         * @param o The object to compare with.
         * @return True if the two LiveStatuses are the same.
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LiveStatus)) {
                return false;
            }
            LiveStatus liveStatus = (LiveStatus) o;
            return this.connection == liveStatus.connection && this.isFinished == liveStatus.isFinished && this.isOutOfGeofence == liveStatus.isOutOfGeofence;
        }


        /**
         * Obtains a hashcode for the LiveStatus object.
         *
         * @return A hashcode for the LiveStatus.
         */
        @Override
        public int hashCode() {
            return Objects.hash(connection, isFinished, isOutOfGeofence);
        }
    }



    /**
     * Enum for label the completion status of a milestone activities during a session.
     */
    public enum MilestoneProgress {
        /**
         * Milestone not visited yet.
         */
        PENDING,

        /**
         * Visited milestone but there is at least an activity not done.
         */
        INCOMPLETE,

        /**
         * Milestone visited and its activities are all done (if exist).
         */
        COMPLETED}



    // ====================================== //
    // NESTED CLASS FOR STUDENT LAST LOCATION //
    // ====================================== //

    /**
     * Class representing the student's geographic location and the exact time it was registered.
     * Mapped from the LOCATION_FIELD map in Firestore.
     *
     * @author Mario López Salazar
     */
    public static class StudentLocation {

        // --- OBJECT ATTRIBUTES ---

        /*
         * LAT-LEN of the student.
         */
        @PropertyName(LOC_FIELD)
        private GeoPoint coordinates;

        /*
         * Exact time the location was registered.
         */
        @PropertyName(TIMEST_FIELD)
        private Date timestamp;



        /**
         * Empty constructor required by Firestore for automatic data mapping.
         */
        public StudentLocation() {
        }

        /**
         * Constructor for building a student location record.
         *
         * @param coordinates Latitude and longitude of the student.
         * @param timestamp   Exact time the location was registered.
         */
        public StudentLocation(GeoPoint coordinates, Date timestamp) {
            this.coordinates = coordinates;
            this.timestamp = timestamp;
        }


        /**
         * Gets the coordinates of the student.
         *
         * @return Latitude and longitude of the student.
         */
        @PropertyName(LOC_FIELD)
        public GeoPoint getCoordinates() {
            return coordinates;
        }

        /**
         * Allows to set the coordinates of the student.
         *
         * @param coordinates Latitude and longitude of the student.
         */
        @PropertyName(LOC_FIELD)
        public void setCoordinates(@NonNull GeoPoint coordinates) {
            this.coordinates = coordinates;
        }

        /**
         * Gets the exact time the location was registered.
         *
         * @return Timestamp of the location record.
         */
        @PropertyName(TIMEST_FIELD)
        public Date getTimestamp() {
            return timestamp;
        }

        /**
         * Allows to set the exact time the location was registered.
         *
         * @param timestamp Timestamp of the location record.
         */
        @PropertyName(TIMEST_FIELD)
        public void setTimestamp(@NonNull Date timestamp) {
            this.timestamp = timestamp;
        }
    }
}