package com.iesaguadulce.deambulario.model.pojos;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;


/**
 * Class representing multimedia content associated with a milestone.
 * Mapped from the array of maps "contenidos" in Firestore.
 *
 * @author Mario López Salazar
 */
public class Content {

    // --- OBJECT ATTRIBUTES ---

    /*
     * Content type in Firestore.
     */
    @PropertyName(CONTENT_TYPE_FIELD)
    private String type;

    /*
     * Text string or URL of the content.
     */
    @PropertyName(CONTENT_VALUE_FIELD)
    private String value;


    // --- CONSTRUCTORS ---

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    public Content() {
    }

    /**
     * Constructor for building a multimedia content.
     *
     * @param type  Content type in Firestore.
     * @param value Text string or URL of the content.
     */
    @SuppressWarnings("unused")
    public Content(@NonNull ContentType type, @NonNull String value) {
        this.type = type.getFirestoreValue();
        this.value = value;
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the type of the content.
     *
     * @return Content type.
     */
    @Exclude
    public ContentType getType() {
        return ContentType.fromString(type);
    }

    /**
     * Gets the type of the content on Firebase format.
     *
     * @return Content type on Firebase format.
     */
    @PropertyName(CONTENT_TYPE_FIELD)
    public String getTypeString() {
        return type;
    }

    /**
     * Allows to set the type of the content.
     *
     * @param type Content type.
     */
    @Exclude
    public void setType(@NonNull ContentType type) {
        this.type = type.getFirestoreValue();
    }

    /**
     * Allows to set the type of the content using Firebase format.
     *
     * @param type Content type on Firebase format.
     */
    @PropertyName(CONTENT_TYPE_FIELD)
    public void setTypeString(@NonNull String type) {
        this.type = type;
    }

    /**
     * Gets the text string or URL of the content.
     *
     * @return Text string or URL.
     */
    @PropertyName(CONTENT_VALUE_FIELD)
    public String getValue() {
        return value;
    }

    /**
     * Allows to set the text string or URL of the content.
     *
     * @param value Text string or URL.
     */
    @PropertyName(CONTENT_VALUE_FIELD)
    public void setValue(@NonNull String value) {
        this.value = value;
    }


    /**
     * Enum containing different kinds of content.
     */
    public enum ContentType {
        /**
         * Text content.
         */
        TEXT(CONTENT_TEXT),

        /**
         * Picture content.
         */
        PICTURE(CONTENT_PICTURE),

        /**
         * Video content.
         */
        VIDEO(CONTENT_VIDEO),

        /**
         * Link content.
         */
        URL(CONTENT_URL);


        /*
         * Firestore string value corresponding to the content type.
         */
        private final String firestoreValue;


        /**
         * ContentType constructor from a string.
         *
         * @param firestoreValue Firestore string value.
         */
        ContentType(@NonNull String firestoreValue) {
            this.firestoreValue = firestoreValue;
        }

        /**
         * Allows to recover the Firestore value of a ContentType.
         *
         * @return The Firestore value of the ContentType.
         */
        public String getFirestoreValue() {
            return firestoreValue;
        }

        /**
         * Allows to get a ContentType from a Firestore string.
         *
         * @param text String from Firestore indicating the type of the content.
         * @return The corresponding ContentType, or null if not matches.
         */
        public static ContentType fromString(String text) {
            for (ContentType type : ContentType.values()) {
                if (type.firestoreValue.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            return null;
        }
    }
}