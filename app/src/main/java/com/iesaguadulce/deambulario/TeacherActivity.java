package com.iesaguadulce.deambulario;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.iesaguadulce.deambulario.databinding.ActivityTeacherBinding;
import com.iesaguadulce.deambulario.notifications.FCMTokenCallback;
import com.iesaguadulce.deambulario.notifications.NotifUtils;
import com.iesaguadulce.deambulario.settings.Constants;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * The Main Activity for the Teacher profile.
 * Manages user interactions, UI setup, data loading, and navigation between fragments.
 *
 * @author Mario López Salazar
 */
public class TeacherActivity extends AppCompatActivity {

    /*
     * Binding object to access views.
     */
    private ActivityTeacherBinding binding;

    /*
     * NavController to handle navigation.
     */
    private NavController navController;

    /*
     * Object to set up the Toolbar with the NavController.
     */
    AppBarConfiguration appBarConfiguration;

    /**
     * Fragments in where the Toolbar is hidden.
     */
    public final Set<Integer> TOOLBAR_HIDDEN_FRAGMENTS = new HashSet<>(Arrays.asList(
            R.id.route_list_fragment,
            R.id.session_list_fragment));

    /**
     * Fragments in where the BottomNavigation is visible.
     */
    public final Set<Integer> BOTTOM_NAVIGATION_VISIBLE_FRAGMENTS = new HashSet<>(Arrays.asList(
            R.id.route_list_fragment,
            R.id.session_list_fragment,
            R.id.teacher_settings_fragment));



    /**
     * Called when the activity is created.
     * Sets up the UI, sets up permissions and notification channel, and performs navigation.
     *
     * @param savedInstanceState If the activity is being re-initialized, contains the data from the last instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Requiring shared preferences:
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);

        // Setting theme:
        String themeValue = prefs.getString("pref_teacher_theme", "system");
        applyTheme(themeValue);

        // Checking if the Teacher Guide has been shown:
        boolean teacherGuideShown = prefs.getBoolean("pref_teacher_guide_shown", false);
        if(!teacherGuideShown) {
            TeacherTourManager.setCurrentStep(TeacherTourManager.TourStep.STEP_1_WELCOME_AND_ROUTES_LIST);
        }


        super.onCreate(savedInstanceState);

        // Inflating the main view:
        binding = ActivityTeacherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setting up permissions:
        setupPermissions();

        // Getting FCM token for notifications:
        NotifUtils.fetchFCMToken(new FCMTokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                NotifUtils.saveLocalFCMToken(TeacherActivity.this, token);
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(TeacherActivity.this, R.string.student_notification_permission_denied, Toast.LENGTH_SHORT).show();
            }
        });

        // Creating notification channel:
        NotifUtils.createNotificationChannel(this, true);

        // Initiating the LiveData which manages some global UI appearance elements:
        GlobalUIViewModel globalUIViewModel = new ViewModelProvider(this).get(GlobalUIViewModel.class);

        // Setting this Activity as an observer of the GlobalUI LiveData:
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

        // Setting up the ToolBar as the default ActionBar:
        setSupportActionBar(binding.toolbar);

        // Getting the NavController:
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(binding.fragmentContainer.getId());
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Defining top-level fragment:
        appBarConfiguration = new AppBarConfiguration.Builder(R.id.route_list_fragment).build();

        // Linking the BottomNavigation and the Toolbar with the NavController:
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Managing the visibility of the ActionBar and the BottomNavigation:
        navController.addOnDestinationChangedListener(this::onDestinationChanged);
    }


    /**
     * Manages changes in navigation destination, updating the ToolbarBar and BottomNavigation visibility.
     *
     * @param navController NavController managing the navigation.
     * @param destination   The current NavDestination.
     * @param bundle        Additional arguments passed to the fragment.
     */
    private void onDestinationChanged(NavController navController, @NotNull NavDestination destination, Bundle bundle) {
        int destinationId = destination.getId();
        ActionBar actionBar = getSupportActionBar();

        // Showing/hiding the Toolbar:
        if (actionBar != null) {
            if (TOOLBAR_HIDDEN_FRAGMENTS.contains(destinationId)) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
        }

        // Showing/hiding the BottomNavigation:
        binding.bottomNavigation.setVisibility(
                BOTTOM_NAVIGATION_VISIBLE_FRAGMENTS.contains(destinationId)
                        ? View.VISIBLE
                        : View.GONE);
    }


    /**
     * Called when the user pushes the Up button in the ActionBar.
     * This method delegates the user's "Up requirement" to the NavController.
     *
     * @return True if navigation back was successful; False otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    /**
     * Sets up and requests the location, and notification permissions on Android 13+.
     * If both permissions are not granted, the full functionalities will not be allowed to teacher.
     */
    private void setupPermissions() {

        // Registering permissions requesting launcher (for location and notification):
        ActivityResultLauncher<String[]> launcher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {

                    Boolean locationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean notificationsGranted =
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    ? result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false)
                                    : Boolean.TRUE;

                    StringBuilder notification = new StringBuilder();
                    if (Boolean.FALSE.equals(locationGranted) && Boolean.FALSE.equals(notificationsGranted)) {
                        notification.append(getString(R.string.teacher_permissions_denied));
                    } else if (Boolean.FALSE.equals(locationGranted)) {
                        notification.append(getString(R.string.location_permission_denied));
                    } else if (Boolean.FALSE.equals(notificationsGranted)) {
                        notification.append(getString(R.string.student_notification_permission_denied));
                    }
                    String notificationString = notification.toString();
                    if (!notificationString.isEmpty()) {
                        Toast.makeText(this, notificationString, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Preparing permissions request:
        List<String> permissionsToRequest = new ArrayList<>();
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Launching permissions request:
        launcher.launch(permissionsToRequest.toArray(new String[0]));
    }


    /**
     * Triggered when the activity receives a new intent.
     * Used to manage students alerts.
     *
     * @param intent The new intent.
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    /**
     * Applies the preferred theme for the Teacher Activity.
     *
     * @param themeValue The preferred theme, following literals on the 'res/values/arrays.xml' file.
     */
    public void applyTheme(String themeValue) {
        switch (themeValue) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Allows to set the Teacher Guide as shown.
     */
    public void setGuideAsShown(){
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME(), Context.MODE_PRIVATE);
        prefs.edit().putBoolean("pref_teacher_guide_shown", true).apply();
    }
}
