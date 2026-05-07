package com.iesaguadulce.deambulario.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.model.repository.SessionRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.settings.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Service that handles Firebase Cloud Messaging (FCM) events.
 * Manages token updates and receiving notifications while the app is in the foreground.
 *
 * @author Mario López Salazar
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    /**
     * Triggered when Firebase assigns or renews the FCM token for this device.
     * Updates both local SharedPreferences and the active session in Firestore.
     *
     * @param token The new device token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // Saving token on SharedPreferences:
        NotifUtils.saveLocalFCMToken(this, token);

        // Requesting the current session ID from SharedPreferences:
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        String sessionId = prefs.getString(Constants.KEY_SESSION, null);

        // Saving FCM token on Firebase:
        if (sessionId != null) {
            String studentId = prefs.getString(Constants.KEY_UID, null);

            // If I'm a teacher:
            if (studentId == null) {
                SessionRepository.getInstance().updateTeacherToken(sessionId, token, new ReposVoidCallback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                    }
                });
            }

            // If I'm a student:
            else {
                SessionRepository.getInstance().updateStudentToken(sessionId, studentId, token, new ReposVoidCallback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                    }
                });
            }
        }
    }


    /**
     * Triggered when a message is received.
     * Builds and displays a system notification and updates local log.
     *
     * @param message The message received from Firebase.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        // Verifying if the message contains a data payload (instead of notification payload):
        if (!message.getData().isEmpty()) {

            // Getting the notification content from the DATA payload:
            String title = message.getData().get("title");
            String body = message.getData().get("body");
            String type = message.getData().get("type"); // "sos_alert" or "teacher_message"

            // Determining the type of message based on the injected type:
            boolean isSOS = "sos_alert".equals(type);

            // Determining the notification channel:
            String channelId = isSOS ? NotifUtils.TEACHER_CHANNEL : NotifUtils.STUDENT_CHANNEL;

            // Creating an intent for the notification:
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            // Creating the notification manually:
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.icon_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent);

            // Launching notification:
            try {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());

                // Sending message to the UI & Log:
                if (body != null) {
                    // Formatting time and message:
                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                    String formattedMessage;
                    if (!isSOS) {
                        formattedMessage = time + " - " + body;
                    } else {
                        formattedMessage = time + " - " + title + ": " + body;
                    }

                    // Saving to SharedPreferences log:
                    SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
                    String sessionId = prefs.getString(Constants.KEY_SESSION, "");
                    NotifUtils.appendToMessageLog(this, formattedMessage, sessionId);

                    // Broadcasting to the Fragment:
                    Intent localIntent = new Intent(NotifUtils.ACTION_NEW_MESSAGE);
                    localIntent.putExtra(NotifUtils.EXTRA_MESSAGE_BODY, formattedMessage);
                    localIntent.setPackage(getPackageName());
                    sendBroadcast(localIntent);
                }

            } catch (SecurityException ignore) {
            }
        }
    }
}