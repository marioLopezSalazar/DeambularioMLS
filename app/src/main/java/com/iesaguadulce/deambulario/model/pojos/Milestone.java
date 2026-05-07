package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Objects;


/**
 * POJO class representing a Milestone in an educative Route.
 *
 * @author Mario López Salazar
 */
public class Milestone {

    // --- OBJECT ATTRIBUTES ---

    /*
     * Milestone ID.
     */
    @DocumentId
    private String id;

    /*
     * Position that the milestone occupies in the route sequence, in case the route must be done in order.
     * Must be positive.
     */
    @PropertyName(ORDER_FIELD)
    private int order;

    /*
     * Milestone title.
     */
    @PropertyName(NAME_FIELD)
    private String name;

    /*
     * LAT-LEN of the marker on the map.
     */
    @PropertyName(COORDINATES_FIELD)
    private GeoPoint coordinates;

    /*
     * List of multimedia contents associated with a milestone for student consultation (can be null).
     */
    @PropertyName(CONTENTS_FIELD)
    private List<Content> contents;

    /*
     * List of activities associated with a milestone (can be null).
     */
    @PropertyName(ACTIVITIES_FIELD)
    private List<Activity> activities;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    public Milestone() {
    }

    /**
     * Constructor for building a milestone.
     * @param order       Position of the milestone in the route sequence. Must be positive.
     * @param name        Milestone title.
     * @param coordinates Latitude and longitude of the marker on the map.
     * @param contents    List of multimedia contents associated with a milestone (can be null).
     * @param activities  List of activities associated with a milestone (can be null).
     * @throws IllegalArgumentException If order is zero or negative.
     */
    public Milestone(int order, @NonNull String name, @NonNull GeoPoint coordinates, List<Content> contents, List<Activity> activities) throws IllegalArgumentException {
        if(order <= 0) {
            throw new IllegalArgumentException("Order cannot be negative.");
        }
        this.order = order;
        this.name = name;
        this.coordinates = coordinates;
        if (contents != null && contents.isEmpty()) {
            contents = null;
        }
        this.contents = contents;
        if (activities != null && activities.isEmpty()) {
            activities = null;
        }
        this.activities = activities;
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the ID of the milestone.
     * @return Milestone ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Allows to set the ID of the milestone.
     * @param id ID of the milestone.
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Gets the sequence order of the milestone.
     * @return Position that the milestone occupies in the route sequence.
     */
    @PropertyName(ORDER_FIELD)
    public int getOrder() {
        return order;
    }

    /**
     * Allows to set the sequence order of the milestone.
     * @param order Position that the milestone occupies in the route sequence.
     * @throws IllegalArgumentException If order is zero or negative.
     */
    @PropertyName(ORDER_FIELD)
    public void setOrder(int order) throws IllegalArgumentException {
        if(order <= 0){
            throw new IllegalArgumentException("Order cannot be negative.");
        }
        this.order = order;
    }

    /**
     * Gets the title of the milestone.
     * @return The title of the milestone.
     */
    @PropertyName(NAME_FIELD)
    public String getName() {
        return name;
    }

    /**
     * Allows to set the title of the milestone.
     * @param name The title of the milestone.
     */
    @PropertyName(NAME_FIELD)
    public void setName(@NonNull String name) {
        this.name = name;
    }

    /**
     * Gets the map coordinates of the milestone.
     * @return Latitude and longitude of the marker on the map.
     */
    @PropertyName(COORDINATES_FIELD)
    public GeoPoint getCoordinates() {
        return coordinates;
    }

    /**
     * Allows to set the map coordinates of the milestone.
     * @param coordinates Latitude and longitude of the marker on the map.
     */
    @PropertyName(COORDINATES_FIELD)
    public void setCoordinates(@NonNull GeoPoint coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Gets the list of multimedia contents associated with the milestone.
     * @return List of multimedia contents.
     */
    @PropertyName(CONTENTS_FIELD)
    public List<Content> getContents() {
        return contents;
    }

    /**
     * Allows to set the list of multimedia contents associated with the milestone.
     * @param contents List of multimedia contents.
     */
    @PropertyName(CONTENTS_FIELD)
    public void setContents(List<Content> contents) {
        if (contents != null && contents.isEmpty()) {
            contents = null;
        }
        this.contents = contents;
    }

    /**
     * Gets the list of activities associated with the milestone.
     * @return List of activities.
     */
    @PropertyName(ACTIVITIES_FIELD)
    public List<Activity> getActivities() {
        return activities;
    }

    /**
     * Allows to set the list of activities associated with the milestone.
     * @param activities List of activities.
     */
    @PropertyName(ACTIVITIES_FIELD)
    public void setActivities(List<Activity> activities) {
        if (activities != null && activities.isEmpty()) {
            activities = null;
        }
        this.activities = activities;
    }


    // --- UTILS METHODS ---

    /**
     * Checks if this milestone is equal to another object.
     * Two milestones are considered equal if their IDs are the same.
     *
     * @param o The object to compare with.
     * @return True if the object is a Milestone with the same ID, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Milestone)) {
            return false;
        }
        Milestone milestone = (Milestone) o;
        return Objects.equals(id, milestone.id);
    }


    /**
     * Obtains a hashcode for the Milestone object.
     *
     * @return Hashcode, based on the Milestone.getId() result.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}