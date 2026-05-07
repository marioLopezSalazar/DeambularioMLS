package com.iesaguadulce.deambulario.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.iesaguadulce.deambulario.StudentActivity;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.databinding.ActivityAuthBinding;
import com.iesaguadulce.deambulario.settings.Constants;

/**
 * Activity to manage the authentication on the app. It divides the flow depending on Teacher or Student profile,
 * and performs the login process on different manners.
 *
 * @author Mario López Salazar
 */
public class AuthActivity extends AppCompatActivity {

    /**
     * Called when the activity is created. Sets up the UI.
     *
     * @param savedInstanceState If the activity is being re-initialized, contains the data from the last instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityAuthBinding binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }


    /**
     * Called when the AuthActivity becomes visible to the user.
     * This method is used to recognize and restore a previously initiated session.
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Attempting to retrieve an initially logged-in user, if available:
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {

            if (!user.isAnonymous()) {
                // Teacher user logged-in:
                navigateToTeacherActivity(user.getUid());
            } else {
                // Student user logged-in:
                navigateToStudentActivity(
                        user.getUid(),
                        recoverStoredPin(),
                        recoverStoredNick());
            }
        }
    }


    /**
     * Used to finish this AuthActivity and start the TeacherActivity.
     * Includes intent flags to avoid phantom screens when creating the new activity.
     *
     * @param uid The teacher FirebaseAuth UID.
     */
    void navigateToTeacherActivity(String uid) {
        Intent intent = new Intent(this, TeacherActivity.class);
        intent.putExtra(Constants.KEY_UID, uid);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }


    /**
     * Used to finish this AuthActivity and start the StudentActivity.
     * Saves the user PIN to allow quick reauthentication if the APP is closed while an active session.
     * Includes intent flags to avoid phantom screens when creating the new activity.
     *
     * @param uid The student FirebaseAuth UID.
     * @param pin The session pin.
     * @param nick The student nick.
     */
    void navigateToStudentActivity(String uid, int pin, String nick) {
        // Saving the PIN, ID and Nick on SharedPreferences:
        saveStoredStudentData(pin, uid, nick);

        //Navigating to Student main activity:
        Intent intent = new Intent(this, StudentActivity.class);
        intent.putExtra(Constants.KEY_UID, uid);
        intent.putExtra(Constants.KEY_PIN, pin);
        intent.putExtra(Constants.KEY_NICK, nick);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }


    /**
     * Saves the session PIN and student nick on SharedPreferences.
     *
     * @param pin  The introduced session PIN.
     * @param id   The Firebase ID of the student.
     * @param nick The introduced student nick.
     */
    public void saveStoredStudentData(int pin, String id, String nick) {
        SharedPreferences sharedPref = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.KEY_PIN, pin);
        editor.putString(Constants.KEY_NICK, nick);
        editor.putString(Constants.KEY_UID, id);
        editor.apply();
    }


    /**
     * Recovers a previously stored Session PIN from SharedPreferences.
     *
     * @return The stored Session PIN if exists, otherwise null.
     */
    private int recoverStoredPin() {
        SharedPreferences sharedPref = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        return sharedPref.getInt(Constants.KEY_PIN, 0);
    }


    /**
     * Recovers a previously stored student nick from SharedPreferences.
     *
     * @return The stored student nick if exists, otherwise null.
     */
    private String recoverStoredNick() {
        SharedPreferences sharedPref = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        return sharedPref.getString(Constants.KEY_NICK, null);
    }

}
