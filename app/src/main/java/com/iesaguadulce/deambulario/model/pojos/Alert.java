package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import java.util.Date;
import java.util.Objects;

/**
 * POJO class representing an SOS Alert triggered by a student.
 * Mapped from the "alertas" subcollection in Firestore.
 *
 * @author Mario López Salazar
 */
public class Alert {

    // --- OBJECT ATTRIBUTES ---

    /*
     * Alert ID in Firestore.
     */
    @DocumentId
    private String id;

    /*
     * Nickname of the student who triggered the SOS.
     */
    @PropertyName(NICK_FIELD)
    private String nick;

    /*
     * Exact time the alert was triggered.
     */
    @PropertyName(DATE_FIELD)
    private Date timestamp;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    @SuppressWarnings("unused")
    public Alert() {
    }

    /**
     * Constructor for building a new SOS Alert.
     * @param nick      Nickname of the student in danger.
     * @param timestamp Exact time the alert was triggered.
     */
    public Alert(@NonNull String nick, @NonNull Date timestamp) {
        this.nick = nick;
        this.timestamp = timestamp;
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the ID of the alert.
     * @return Alert ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Allows to set the ID of the alert.
     * @param id Alert ID.
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Gets the nickname of the student who triggered the alert.
     * @return Student nickname.
     */
    @PropertyName(NICK_FIELD)
    public String getNick() {
        return nick;
    }

    /**
     * Allows to set the nickname of the student.
     * @param nick Student nickname.
     */
    @PropertyName(NICK_FIELD)
    public void setNick(@NonNull String nick) {
        this.nick = nick;
    }

    /**
     * Gets the exact time the alert was triggered.
     * @return Timestamp of the alert.
     */
    @PropertyName(DATE_FIELD)
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Allows to set the exact time the alert was triggered.
     * @param timestamp Timestamp of the alert.
     */
    @PropertyName(DATE_FIELD)
    public void setTimestamp(@NonNull Date timestamp) {
        this.timestamp = timestamp;
    }


    // --- UTILS METHODS ---

    /**
     * Checks if this alert is equal to another object.
     * Two alerts are considered equal if their IDs are the same.
     *
     * @param o The object to compare with.
     * @return True if the object is an Alert with the same ID, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Alert)) {
            return false;
        }
        Alert alert = (Alert) o;
        return Objects.equals(id, alert.id);
    }

    /**
     * Obtains a hashcode for the Alert object.
     *
     * @return Hashcode, based on the Alert.getId() result.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}