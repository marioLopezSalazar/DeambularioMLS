package com.iesaguadulce.deambulario.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.settings.Constants;

import org.jetbrains.annotations.NotNull;


/**
 * Tools class containing reusable constants and methods related with notifications.
 *
 * @author Mario López Salazar.
 */
public abstract class NotifUtils {

    /**
     * The name of the 'Deambulario' student's alerts channel.
     */
    public static final String STUDENT_CHANNEL = "student_channel";

    /**
     * The name of the 'Deambulario' teacher's messages channel.
     */
    public static final String TEACHER_CHANNEL = "teacher_channel";

    /**
     * Action to broadcast a new internal message to the UI.
     */
    public static final String ACTION_NEW_MESSAGE = "com.iesaguadulce.deambulario.NEW_MESSAGE";

    /**
     * Key to extract the message body from the broadcast intent.
     */
    public static final String EXTRA_MESSAGE_BODY = "extra_message_body";



    /**
     * Request Firebase the device Cloud Messaging token.
     *
     * @param callback To manage the result of the FCM token requesting.
     */
    public static void fetchFCMToken(@NonNull FCMTokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {

                    // Token wasn't available:
                    if (!task.isSuccessful()) {
                        callback.onError(task.getException());
                        return;
                    }

                    // Token available:
                    String token = task.getResult();
                    callback.onTokenReceived(token);
                });
    }


    /**
     * Saves the FCM token on Shared Preferences.
     *
     * @param context The context in which this method is called.
     * @param token   The device FCM token.
     */
    public static void saveLocalFCMToken(@NonNull Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.KEY_FCM_TOKEN, token).apply();
    }


    /**
     * Gets the FCM token from Shared Preferences.
     *
     * @param context The context in which this method is called.
     * @return The device FCM token if stored on Shared Preferences, otherwise null.
     */
    public static String getLocalFCMToken(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        return prefs.getString(Constants.KEY_FCM_TOKEN, null);
    }


    /**
     * Creates the notification channels for the App.
     *
     * @param context   The application context.
     * @param isTeacher True if creating channel for the teacher, false for student.
     */
    public static void createNotificationChannel(@NotNull Context context, boolean isTeacher) {

        // Requiring system notification manager:
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            NotificationChannel channel;

            // Student receives messages from the teacher:
            if (!isTeacher) {
                channel = new NotificationChannel(STUDENT_CHANNEL, context.getString(R.string.session_messages), NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(context.getString(R.string.session_messages));
            }
            // Teacher receives SOS messages from students:
            else {
                channel = new NotificationChannel(TEACHER_CHANNEL, context.getString(R.string.SOS_messages), NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(context.getString(R.string.SOS_messages));
            }

            // Creating the channel on the system:
            notificationManager.createNotificationChannel(channel);
        }
    }


    /**
     * Appends a new message to the local SharedPreferences log.
     *
     * @param context   The application context.
     * @param message   The formatted message to append.
     * @param sessionId The session ID.
     */
    public static void appendToMessageLog(@NonNull Context context, @NonNull String message, @NonNull String sessionId) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        String currentLog = prefs.getString(Constants.KEY_MESSAGE_LOG + sessionId, "");
        String newLog = currentLog.isEmpty() ? message : currentLog + "\n" + message;
        prefs.edit().putString(Constants.KEY_MESSAGE_LOG + sessionId, newLog).apply();
    }


    /**
     * Retrieves the complete message log from SharedPreferences.
     *
     * @param context   The application context.
     * @param sessionId The session ID.
     * @return The stored message log, or null if it's empty.
     */
    public static String getMessageLog(@NonNull Context context, @NonNull String sessionId) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        return prefs.getString(Constants.KEY_MESSAGE_LOG + sessionId, null);
    }


    /**
     * Clears the message log from SharedPreferences.
     * Should be called when the student leaves or finishes the session.
     *
     * @param context   The application context.
     * @param sessionId The session ID.
     */
    public static void clearMessageLog(@NonNull Context context, @NonNull String sessionId) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        prefs.edit().remove(Constants.KEY_MESSAGE_LOG + sessionId).apply();
    }
}
