package com.iesaguadulce.deambulario.map_and_location;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.GeoPoint;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.model.repository.SessionRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.utils.UIUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Service that handles location tracking when the application is running
 * in the background or minimized, sending real-time coordinates to Firestore.
 *
 * @author Mario López Salazar.
 */
public class LocationTrackingService extends Service {

    /**
     * Notification channel for the real-time location Service.
     */
    public static final String CHANNEL_ID = "LocationServiceChannel";

    /**
     * ID constant for the real-time location Service.
     */
    public static final int NOTIFICATION_ID = 1;

    /**
     * Identifier for passing the session ID to this service through bundle.
     */
    public static final String SESSION_ID_BUNDLE = "SESSION_ID";

    /**
     * Time interval to request location updates (in milliseconds).
     */
    public static final long LOCATION_UPDATE_INTERVAL = 10000L;


    /*
     * Client and callback to manage real-time location updates.
     */
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    /*
     * Variables holding the current session and student identifiers to update the database.
     */
    private String sessionId;


    /**
     * Called by the system when the service is first created.
     * Initializes the location client.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }


    /**
     * Called by the system every time the service starts.
     * Sets up the foreground notification and starts the location tracking.
     *
     * @param intent  The Intent supplied to startService, containing the session and student IDs.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's current started state.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Retrieving data passed from the Fragment:
        if (intent != null) {
            sessionId = intent.getStringExtra(SESSION_ID_BUNDLE);
        }

        // Creating and displaying the foreground notification:
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.sending_location_to_teacher))
                .setSmallIcon(R.drawable.icon_routes)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        // Starting background location tracking:
        startLocationUpdates();

        // If the system kills the service, it will be recreated with the original intent:
        return START_REDELIVER_INTENT;
    }


    /**
     * Configures the LocationRequest and starts requesting location updates
     * from the FusedLocationProviderClient.
     */
    private void startLocationUpdates() {

        // Building the location request:
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(LocationTrackingService.LOCATION_UPDATE_INTERVAL)
                .build();

        // Setting a callback launched when a new location is retrieved:
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();

                if (location != null && sessionId != null) {
                    // Uploading location to Firestore, if there's network connection:
                    if (isNetworkAvailable(LocationTrackingService.this)) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                        SessionRepository.getInstance().updateStudentLocation(sessionId, geoPoint, new ReposVoidCallback() {
                            @Override
                            public void onSuccess() { }
                            @Override
                            public void onError(Exception e) { }
                        });
                    }
                }
            }
        };

        // Requesting location updates if permissions are granted:
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }


    /**
     * Called when the user removes the app from the recent apps list.
     * Used to perform the 'logout' operation on Firebase before the process is completely killed.
     *
     * @param rootIntent The original root Intent that was used to launch the task.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (sessionId != null) {

            UIUtils.playShortSound(this, R.raw.sound_logout);

            // Marking on Firebase as app closed:
            SessionRepository.getInstance().studentClosedApp(sessionId, new ReposVoidCallback() {
                @Override
                public void onSuccess() {}
                @Override
                public void onError(Exception e){}
            });
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }


    /**
     * Called by the system when the service is not used and is being removed.
     * Stops location tracking to prevent battery drain.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


    /**
     * Return the communication channel to the service.
     * This service is not designed to be bound, so it returns null.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return null.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * Creates the NotificationChannel.
     */
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.sending_location_to_teacher),
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }


    /**
     * Checks if there's currently network connection.
     * @return True, if there's currently network connection, false if not.
     */
    public static boolean isNetworkAvailable(@NotNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
        }
        return false;
    }
}