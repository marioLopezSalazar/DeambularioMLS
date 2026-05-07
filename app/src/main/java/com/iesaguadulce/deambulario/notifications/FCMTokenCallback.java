package com.iesaguadulce.deambulario.notifications;

/**
 * Interface to manage the result of requesting the Firebase Cloud Messaging token.
 *
 * @author Mario López Salazar.
 */
public interface FCMTokenCallback {

    /**
     * Actions to do when the FCM token is ready.
     *
     * @param token The device FCM token.
     */
    void onTokenReceived(String token);

    /**
     * Actions to do when the FCM token was unavailable.
     *
     * @param e Error.
     */
    void onError(Exception e);
}
