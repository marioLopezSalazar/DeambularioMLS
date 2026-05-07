package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Objects;

/**
 * POJO class representing an educative Route.
 *
 * @author Mario López Salazar
 */
public class Route {

    // --- OBJECT ATTRIBUTES ---

    /*
     * Route ID.
     */
    @DocumentId
    private String id;

    /*
     * Route teacher owner ID.
     */
    @PropertyName(TEACHER_UID_FIELD)
    private String teacherId;

    /*
     * Route title.
     */
    @PropertyName(TITLE_FIELD)
    private String title;

    /*
     * Route educative level target (can be null).
     */
    @PropertyName(LEVEL_FIELD)
    private String level;

    /*
     * Route curricular description (can be null).
     */
    @PropertyName(CURRICULA_FIELD)
    private String curriculum;

    /*
     * Indicates if the route milestones must be gone over in order.
     */
    @PropertyName(IN_ORDER_FIELD)
    private boolean inOrder;

    /*
     * Indicates if the geofence is enabled.
     */
    @PropertyName(ACTIVE_GEOFENCE_FIELD)
    private boolean geofenceEnabled;

    /*
     * Collection of circles (center-radius) which define the set-up geofence for the route (can be null).
     */
    @PropertyName(GEOFENCE_FIELD)
    private List<Geofence> geofences;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    public Route() {
    }

    /**
     * Constructor for building a route.
     *
     * @param teacherId  Route teacher owner ID.
     * @param title      Route title.
     * @param level      Route educative level target (can be null).
     * @param curriculum Route curricular description (can be null).
     * @param inOrder    Indicates if the route milestones must be gone over in order.
     * @param geofences  Collection of circles (center-radius) which define the set-up geofence for the route (can be null).
     */
    public Route(@NonNull String teacherId, @NonNull String title, String level, String curriculum, boolean inOrder, boolean geofenceEnabled, List<Geofence> geofences) {
        this.teacherId = teacherId;
        this.title = title;
        this.level = level;
        this.curriculum = curriculum;
        this.inOrder = inOrder;
        this.geofenceEnabled = geofenceEnabled;
        // Avoiding empty circles list:
        if (geofences != null && geofences.isEmpty()) {
            geofences = null;
        }
        this.geofences = geofences;
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the ID of the route.
     *
     * @return Route ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Allows to set the ID of the route.
     *
     * @param id ID of the route.
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Gets the ID of the teacher route owner.
     *
     * @return The ID of the teacher route owner.
     */
    @PropertyName(TEACHER_UID_FIELD)
    public String getTeacherId() {
        return teacherId;
    }

    /**
     * Allows to set the ID of the teacher route owner.
     *
     * @param teacherId ID of the teacher route owner.
     */
    @PropertyName(TEACHER_UID_FIELD)
    public void setTeacherId(@NonNull String teacherId) {
        this.teacherId = teacherId;
    }

    /**
     * Gets the title of the route.
     *
     * @return The title of the route.
     */
    @PropertyName(TITLE_FIELD)
    public String getTitle() {
        return title;
    }

    /**
     * Allows to set the title of the route.
     *
     * @param title The title of the route.
     */
    @PropertyName(TITLE_FIELD)
    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    /**
     * Gets the educative level target of the route.
     *
     * @return The educative level target of the route (can be null).
     */
    @PropertyName(LEVEL_FIELD)
    public String getLevel() {
        return level;
    }

    /**
     * Allows to set educative level target of the route.
     *
     * @param level The educative level target of the route.
     */
    @PropertyName(LEVEL_FIELD)
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Gets the route curricular description.
     *
     * @return The route curricular description (can be null).
     */
    @PropertyName(CURRICULA_FIELD)
    public String getCurriculum() {
        return curriculum;
    }

    /**
     * Allows to set the route curricular description.
     *
     * @param curriculum The route curricular description.
     */
    @PropertyName(CURRICULA_FIELD)
    public void setCurriculum(String curriculum) {
        this.curriculum = curriculum;
    }

    /**
     * Indicates if the route geofence is enabled.
     *
     * @return If the route geofence is enabled.
     */
    @PropertyName(IN_ORDER_FIELD)
    public boolean isInOrder() {
        return inOrder;
    }

    /**
     * Allows to set up if the route milestones must be gone over in order.
     *
     * @param inOrder True if the route milestones must be gone over in order. False otherwise.
     */
    @PropertyName(IN_ORDER_FIELD)
    public void setInOrder(boolean inOrder) {
        this.inOrder = inOrder;
    }

    /**
     * Indicates if the route geofence is enabled.
     *
     * @return If the route geofence is enabled.
     */
    @PropertyName(ACTIVE_GEOFENCE_FIELD)
    public boolean isGeofenceEnabled() {
        return geofenceEnabled;
    }

    /**
     * Allows to set up if the route geofence is enabled or disabled.
     *
     * @param geofenceEnabled True if the route geofence must be enabled. False otherwise.
     */
    @PropertyName(ACTIVE_GEOFENCE_FIELD)
    public void setGeofenceEnabled(boolean geofenceEnabled) {
        this.geofenceEnabled = geofenceEnabled;
    }

    /**
     * Gets a collection of circles (center-radius) which define a geofence for the route.
     *
     * @return A collection of circles (center-radius) which define a geofence for the route (can be null).
     */
    @PropertyName(GEOFENCE_FIELD)
    public List<Geofence> getGeofences() {
        return geofences;
    }

    /**
     * Allows to set up a collection of circles (center-radius) which define a geofence for the route.
     *
     * @param geofences A collection of circles (center-radius) which define a geofence for the route.
     */
    @PropertyName(GEOFENCE_FIELD)
    public void setGeofences(List<Geofence> geofences) {
        // Avoiding empty circles list:
        if (geofences != null && geofences.isEmpty()) {
            geofences = null;
        }
        this.geofences = geofences;
    }


    // --- UTILS METHODS ---

    /**
     * Checks if this route is equal to another object.
     * Two routes are considered equal if their IDs are the same.
     *
     * @param o The object to compare with.
     * @return True if the object is a Route with the same ID, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Route)) {
            return false;
        }
        Route route = (Route) o;
        return Objects.equals(id, route.id);
    }


    /**
     * Obtains a hashcode for the Route object.
     *
     * @return Hashcode, based on the Route.getId() result.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }




    // ========================= //
    // NESTED CLASS FOR GEOFENCE //
    // ========================= //

    /**
     * Class representing a circular geofence area.
     *
     * @author Mario López Salazar
     */
    public static class Geofence {

        // --- OBJECT ATTRIBUTES ---

        /*
         * LAT-LEN coordinates of the center of the circular area.
         */
        @PropertyName(CENTER_FIELD)
        private GeoPoint center;

        /*
         * Radius (metres) of the circular area. Must be non-negative.
         */
        @PropertyName(RADIUS_FIELD)
        private int radius;


        // --- CONSTRUCTORS ---

        /**
         * Empty constructor required by Firestore for automatic data mapping.
         */
        @SuppressWarnings("unused")
        public Geofence() {
        }

        /**
         * Constructor for build a geofence circle.
         *
         * @param center LAT-LEN coordinates of the center of the circular area.
         * @param radius Radius (metres) of the circular area. Must be non-negative.
         * @throws IllegalArgumentException If radius is negative.
         */
        public Geofence(@NonNull GeoPoint center, int radius) throws IllegalArgumentException {
            this.center = center;

            // Avoiding negative radius:
            if (radius < 0) {
                throw new IllegalArgumentException("Radius cannot be negative.");
            }
            this.radius = radius;
        }


        // --- GETTERS AND SETTERS ---

        /**
         * Gets the center of the geofence circle.
         *
         * @return LAT-LEN coordinates of the center of the circular area.
         */
        @PropertyName(CENTER_FIELD)
        public GeoPoint getCenter() {
            return center;
        }

        /**
         * Allows to set the center of the geofence circle.
         *
         * @param center LAT-LEN coordinates of the center of the circular area.
         */
        @PropertyName(CENTER_FIELD)
        public void setCenter(@NonNull GeoPoint center) {
            this.center = center;
        }

        /**
         * Gets the radius (metres) of the circular area.
         *
         * @return The radius (metres) of the circular area.
         */
        @PropertyName(RADIUS_FIELD)
        public int getRadius() {
            return radius;
        }

        /**
         * Allows to set the radius (metres) of the circular area.
         *
         * @param radius Radius (metres) of the circular area. Must be non-negative.
         * @throws IllegalArgumentException If radius is negative.
         */
        @PropertyName(RADIUS_FIELD)
        public void setRadius(int radius) throws IllegalArgumentException {
            // Avoiding negative radius:
            if (radius < 0) {
                throw new IllegalArgumentException("Radius cannot be negative.");
            }
            this.radius = radius;
        }
    }
}