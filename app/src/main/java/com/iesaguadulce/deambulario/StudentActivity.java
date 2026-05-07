package com.iesaguadulce.deambulario;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.iesaguadulce.deambulario.auth.AuthActivity;
import com.iesaguadulce.deambulario.auth.AuthUtils;
import com.iesaguadulce.deambulario.databinding.ActivityStudentBinding;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.notifications.FCMTokenCallback;
import com.iesaguadulce.deambulario.notifications.NotifUtils;
import com.iesaguadulce.deambulario.settings.Constants;
import com.iesaguadulce.deambulario.settings.StudentTourManager;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * The Main Activity for the Student profile.
 * Manages user interactions, UI setup, data loading, navigation and holds session credentials.
 *
 * @author Mario López Salazar
 */
public class StudentActivity extends AppCompatActivity {

    /*
     * Binding object to access views.
     */
    private ActivityStudentBinding binding;

    /*
     * NavController to handle navigation.
     */
    private NavController navController;

    /*
     * Object to set up the Toolbar with the NavController.
     */
    private AppBarConfiguration appBarConfiguration;

    /*
     * Student and session identifiers:
     */
    private String studentUid;
    private int sessionPin;
    private String studentNick;

    /*
     * ViewModel to manage the UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;


    /**
     * This method is called before the Activity is created. Used to perform the font size.
     *
     * @param newBase The activity previous context.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        // Requesting font size preference:
        SharedPreferences prefs = newBase.getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        String fontSize = prefs.getString("pref_student_font_size", "medium");

        // Setting font scale:
        float scale = 0.9f;
        if ("small".equals(fontSize)) scale = 0.75f;
        if ("large".equals(fontSize)) scale = 1.25f;

        // Creating new configuration:
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.fontScale = scale;

        // Applying new configuration:
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }


    /**
     * Called when the activity is created.
     * Sets up the UI, sets up permissions and notification channel, and performs navigation.
     *
     * @param savedInstanceState If the activity is being re-initialized, contains the data from the last instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Getting student Shared Preferences:
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);

        // Applying student preferred color:
        applyPalette(prefs.getString("pref_student_palette", "forest"));

        // Checking if the Student Guide has been shown:
        boolean studentGuideShown = prefs.getBoolean("pref_student_guide_shown", false);
        if (!studentGuideShown) {
            StudentTourManager.setCurrentStep(StudentTourManager.TourStep.STEP_1_NEXT_MILESTONE_INDICATOR);
        }

        super.onCreate(savedInstanceState);

        // Getting student and session identifiers:
        Intent intent = getIntent();
        if (intent != null) {
            studentUid = intent.getStringExtra(Constants.KEY_UID);
            sessionPin = intent.getIntExtra(Constants.KEY_PIN, 0);
            studentNick = intent.getStringExtra(Constants.KEY_NICK);
        }

        // Inflating the main view:
        binding = ActivityStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Getting FCM token for notifications:
        NotifUtils.fetchFCMToken(new FCMTokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                // Saving token on Shared Preferences:
                NotifUtils.saveLocalFCMToken(StudentActivity.this, token);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(StudentActivity.this, R.string.teacher_notification_permission_denied, Toast.LENGTH_SHORT).show();
            }
        });

        // Creating notification channel:
        NotifUtils.createNotificationChannel(this, false);

        // Initiating the LiveData which manages some global UI appearance elements:
        globalUIViewModel = new ViewModelProvider(this).get(GlobalUIViewModel.class);
        globalUIViewModel.getIsLoading().observe(this, isLoading ->
                binding.globalLoading.setVisibility(
                        (isLoading != null && isLoading)
                                ? View.VISIBLE
                                : View.GONE));
        globalUIViewModel.getToolbarTitle().observe(this, customizedTitle -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(customizedTitle);
            }
        });

        // Setting up permissions:
        setupPermissions();

        // Setting up the ToolBar as the default ActionBar:
        setSupportActionBar(binding.studentToolbar);

        // Getting the NavController:
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(binding.studentFragmentContainer.getId());
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Defining top-level fragments:
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.student_lobby_fragment,
                R.id.student_play_fragment
        ).build();

        // Linking the Toolbar with the NavController:
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Showing-hiding the toolbar:
        navController.addOnDestinationChangedListener((navController, destination, bundle) -> {
            if (getSupportActionBar() == null) {
                return;
            }
            if (destination.getId() == R.id.student_lobby_fragment) {
                getSupportActionBar().hide();
            } else {
                getSupportActionBar().show();
            }
        });
    }


    /**
     * Applies the color theme.
     *
     * @param palette Literal from the 'palette_values' entry on the res/values/arrays.xml file.
     */
    public void applyPalette(String palette) {
        switch (palette) {
            case "coast":
                setTheme(R.style.Theme_Deambulario_Student_Coast);
                break;
            case "city":
                setTheme(R.style.Theme_Deambulario_Student_City);
                break;
            case "forest":
            default:
                setTheme(R.style.Theme_Deambulario_Student_Forest);
                break;
        }
    }


    /**
     * Sets up and requests the location and notification permissions, one after the other one.
     * KEY POINT: When both permissions are granted, the GlobalUIViewModel launches the session joining process
     * on StudentLobbyFragment's GlobalUIViewModel observer.
     * If both permissions are not granted, the student is kicked immediately.
     */
    private void setupPermissions() {

        // Registering the permissions request launcher:
        ActivityResultLauncher<String[]> launcher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean locationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean notificationsGranted =
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    ? result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false)
                                    : Boolean.TRUE;

                    // When both permissions granted:
                    if (Boolean.TRUE.equals(locationGranted) && Boolean.TRUE.equals(notificationsGranted)) {
                        globalUIViewModel.setPermissionsGranted(true);
                    }
                    // When NOT both permissions granted, the student is kicked:
                    else {
                        kickStudentToAuth(R.string.permissions_required_to_play);
                    }
                }
        );

        // Preparing permissions request:
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Launching permissions request:
        launcher.launch(permissions.toArray(new String[0]));
    }


    /**
     * Triggered when the activity receives a new intent.
     * Used to manage teacher notifications.
     *
     * @param intent The new intent.
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    /**
     * Kicks the student out of the session if the session doesn't exist or if essential permissions are denied.
     * Clears stored credentials and navigates back to AuthActivity.
     */
    public void kickStudentToAuth(int messageID) {

        // Notifying student:
        Toast.makeText(this, messageID, Toast.LENGTH_LONG).show();

        // Stop viewmodel listeners:
        SessionViewModel viewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        viewModel.stopListeningToSession();
        viewModel.stopListeningToStudents();
        viewModel.stopListeningToMyStatus();

        // Dropping stored credentials:
        getSharedPreferences(Constants.PREF_NAME(), MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Closing session and deleting user on Firebase Auth:
        globalUIViewModel.showLoading();
        AuthUtils.deleteStudentUser(new ReposVoidCallback() {
            @Override
            public void onSuccess() {
                executeNavigationToAuth();
            }

            @Override
            public void onError(Exception e) {
                executeNavigationToAuth();
            }
        });
    }


    /**
     * Finishes StudentActivity and navigates to AuthActivity. Used after user deletion.
     */
    private void executeNavigationToAuth() {
        globalUIViewModel.hideLoading();
        Intent intent = new Intent(StudentActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    /**
     * Called when the user pushes the Up button in the ActionBar.
     * This method delegates the user's "Up requirement" to the NavController.
     *
     * @return True if navigation back was successful, false otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    /**
     * Allows to set the Student Guide as shown.
     */
    public void setGuideAsShown() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        prefs.edit().putBoolean("pref_student_guide_shown", true).apply();
    }


    /**
     * Gets the Firebase UID of the logged-in student.
     *
     * @return The Firebase UID of the logged-in student.
     */
    public String getStudentUid() {
        return studentUid;
    }

    /**
     * Gets the current session pin.
     *
     * @return The current session pin.
     */
    public int getSessionPin() {
        return sessionPin;
    }

    /**
     * Gets the nick introduced by the student on the login activity.
     *
     * @return The nick of the student.
     */
    public String getStudentNick() {
        return studentNick;
    }
}