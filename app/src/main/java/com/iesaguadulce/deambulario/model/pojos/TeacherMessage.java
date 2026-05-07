package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import java.util.Date;
import java.util.Objects;

/**
 * POJO class representing a message sent by the teacher to a student or to all students.
 *
 * @author Mario López Salazar
 */

public class TeacherMessage {

    // --- OBJECT ATTRIBUTES ---

    /*
     * Message ID in Firestore.
     */
    @DocumentId
    private String id;

    /*
     * Target student ID, or "todos" for multicast messages.
     */
    @PropertyName(STUDENT_ID_FIELD)
    private String targetStudentId;

    /*
     * Text content of the message.
     */
    @PropertyName(MESSAGE_FIELD)
    private String text;

    /*
     * Exact time the message was sent.
     */
    @PropertyName(DATE_FIELD)
    private Date timestamp;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    @SuppressWarnings("unused")
    public TeacherMessage() {
    }

    /**
     * Constructor for building a new Teacher Message.
     * @param targetStudentId ID of the student, or "todos" for all students.
     * @param text            Body of the message.
     * @param timestamp       Exact time the message was sent.
     */
    public TeacherMessage(@NonNull String targetStudentId, @NonNull String text, @NonNull Date timestamp) {
        this.targetStudentId = targetStudentId;
        this.text = text;
        this.timestamp = timestamp;
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the ID of the message.
     * @return Message ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Allows to set the ID of the message.
     * @param id Message ID.
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Gets the target ID of the message.
     * @return Student ID or "todos".
     */
    @PropertyName(STUDENT_ID_FIELD)
    public String getTargetStudentId() {
        return targetStudentId;
    }

    /**
     * Allows to set the target ID of the message.
     * @param targetStudentId Student ID or "todos".
     */
    @PropertyName(STUDENT_ID_FIELD)
    public void setTargetStudentId(@NonNull String targetStudentId) {
        this.targetStudentId = targetStudentId;
    }

    /**
     * Gets the text content of the message.
     * @return Message text.
     */
    @PropertyName(MESSAGE_FIELD)
    public String getText() {
        return text;
    }

    /**
     * Allows to set the text content of the message.
     * @param text Message text.
     */
    @PropertyName(MESSAGE_FIELD)
    public void setText(@NonNull String text) {
        this.text = text;
    }

    /**
     * Gets the exact time the message was sent.
     * @return Timestamp of the message.
     */
    @PropertyName(DATE_FIELD)
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Allows to set the exact time the message was sent.
     * @param timestamp Timestamp of the message.
     */
    @PropertyName(DATE_FIELD)
    public void setTimestamp(@NonNull Date timestamp) {
        this.timestamp = timestamp;
    }


    // --- UTILS METHODS ---

    /**
     * Checks if this message is equal to another object.
     * Two messages are considered equal if their IDs are the same.
     *
     * @param o The object to compare with.
     * @return True if the object is a TeacherMessage with the same ID, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TeacherMessage)) {
            return false;
        }
        TeacherMessage message = (TeacherMessage) o;
        return Objects.equals(id, message.id);
    }

    /**
     * Obtains a hashcode for the TeacherMessage object.
     *
     * @return Hashcode, based on the TeacherMessage.getId() result.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}