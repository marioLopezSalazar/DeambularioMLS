package com.iesaguadulce.deambulario.settings;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Utils class containing the keys used on SharedPreferences.
 *
 * @author Mario López Salazar.
 */
public class Constants {

    /**
     * Allows to get the name of the SharedPreferences file.
     *
     * @return The name of the SharedPreferences file.
     */
    public static String PREF_NAME() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return "deambulario_prefs_" + user.getUid();
        }
        return "deambulario_prefs_default";
    }


    /**
     * Key to get the FCM user token.
     */
    public static final String KEY_FCM_TOKEN = "fcm_token";
    /**
     * Key to get the current session of a student.
     */
    public static final String KEY_SESSION = "session";
    /**
     * Key to get the pin of the current session.
     */
    public static final String KEY_PIN = "pin";
    /**
     * Key to get the nick of the student.
     */
    public static final String KEY_NICK = "nick";
    /**
     * Key to get the FirebaseAuth user UID.
     */
    public static final String KEY_UID = "uid";
    /**
     * Key to store the session message log.
     */
    public static final String KEY_MESSAGE_LOG = "message_log_";

}
