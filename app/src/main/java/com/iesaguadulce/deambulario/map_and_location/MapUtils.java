package com.iesaguadulce.deambulario.map_and_location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.GeoPoint;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.pojos.Student.LiveStatus;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Tools class to perform actions on the route map.
 *
 * @author Mario López Salazar.
 */
public abstract class MapUtils {


    /**
     * Milestones default marker.
     */
    public static final int MARKER_MILESTONE_ICON = R.drawable.icon_milestone;

    /**
     * Geofence center marker.
     */
    public static final int MARKER_GEOFENCE_CENTER_ICON = R.drawable.icon_geofence_center;

    /**
     * Default size (dp) of markers.
     */
    public static final int STANDARD_MARKER_SIZE_DP = 36;

    /**
     * Default markers color.
     */
    public static final int DEFAULT_COLOR = R.color.deambulario_purple;

    /*
     * Cache to store generated BitmapDescriptors for milestone markers.
     */
    private static final Map<Integer, BitmapDescriptor> milestoneMarkerCache = new HashMap<>();

    /*
     * Cache to store generated BitmapDescriptors for geofence center.
     */
    private static BitmapDescriptor geofenceCenterCache = null;

    /*
     * Cache to store generated BitmapDescriptors for text labels (like START and END).
     */
    private static final Map<String, BitmapDescriptor> textLabelCache = new HashMap<>();

    /*
     * Cache to store generated BitmapDescriptors for students tracking markers.
     */
    private static final Map<String, BitmapDescriptor> markerCache = new HashMap<>();


    //==============================================================================================


    /**
     * Default GeoPoint location, when user location is not available. It's the geographic center of Andalusia.
     */
    public static final GeoPoint DEFAULT_GEOPOINT = new GeoPoint(37.463350, -4.575648);

    /**
     * Interface to get the default GeoPoint asynchronously.
     */
    public interface DefaultGeoPointCallback {
        /**
         * Implements the actions to do when the default GeoPoint is available.
         *
         * @param defaultGeoPoint The default GeoPoint.
         */
        void onPointReceived(GeoPoint defaultGeoPoint);
    }

    /**
     * Gets the default GeoPoint, i.e. the user position if available, or the center of Andalusia if not.
     *
     * @param context  The context in which this method is called.
     * @param callback Implements the method to be executed when the default location is available.
     */
    public static void getDefaultPoint(Context context, DefaultGeoPointCallback callback) {

        // Checking if the app has location permissions:
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Getting the user position:
            LocationServices.getFusedLocationProviderClient(context).getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            // Returning user location:
                            callback.onPointReceived(new GeoPoint(location.getLatitude(), location.getLongitude()));
                        } else {
                            // User location not available:
                            callback.onPointReceived(DEFAULT_GEOPOINT);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // User location not available:
                        callback.onPointReceived(DEFAULT_GEOPOINT);
                    });
        }
        // When no location permissions, return the center of Andalusia:
        else {
            callback.onPointReceived(DEFAULT_GEOPOINT);
        }
    }


    //==============================================================================================


    /**
     * Draws milestones as markers.
     * Must be called after Google Maps is ready. The milestones are tagged on the markers.
     *
     * @param context    The context in which this method is called.
     * @param map        The map.
     * @param milestones The list of milestones to draw.
     * @param moveCamera Indicates if the camera must focus the entire markets area after drawing them.
     * @return List of map markers.
     */
    public static List<Marker> drawMilestonesOnMap(Context context, GoogleMap map, List<Milestone> milestones, boolean moveCamera) {

        // Assuring resources are available:
        List<Marker> markers = new ArrayList<>();
        if (map == null || milestones == null || milestones.isEmpty()) {
            if (map != null) {
                map.clear();
            }
            return markers;
        }

        // Clearing the map:
        map.clear();

        // Initializing map camera:
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Initializing markers icon:
        BitmapDescriptor standardIcon = getMarkerBitmapDescriptor(context, null);

        // Building and drawing markers from milestones:
        for (Milestone milestone : milestones) {
            LatLng position = new LatLng(milestone.getCoordinates().getLatitude(), milestone.getCoordinates().getLongitude());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(milestone.getName())
                    .icon(standardIcon)
                    .anchor(0.5f, 1f);
            Marker marker = map.addMarker(markerOptions);
            if (marker != null) {
                marker.setTag(milestone);
                markers.add(marker);
            }

            // Including the marker in the bounding box:
            builder.include(position);
        }

        if (!moveCamera) {
            return markers;
        }

        // Adjusting camera:
        map.setOnMapLoadedCallback(() -> {
            try {
                LatLngBounds bounds = builder.build();

                // Particular case - single milestone:
                if (milestones.size() == 1) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).getPosition(), 20f));
                }
                // General case:
                else {
                    int padding = (int) (60 * context.getResources().getDisplayMetrics().density);
                    map.setPadding(0, padding, 0, 0);
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80));
                }
            } catch (IllegalStateException e) {
                // On error, moving to the first milestone:
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).getPosition(), 18f));
            }
        });

        return markers;
    }


    /**
     * Focus the map on the current student position and some marker.
     *
     * @param map             The map to perform.
     * @param studentLocation Student location.
     * @param targetMilestone Milestone to focus.
     */
    public static void focusMapOnStudentAndMilestone(GoogleMap map, LatLng studentLocation, Milestone targetMilestone) {
        if (map == null || studentLocation == null || targetMilestone == null) {
            return;
        }

        // Creating limits object:
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Including student and milestone locations:
        builder.include(studentLocation);
        LatLng milestonePos = new LatLng(
                targetMilestone.getCoordinates().getLatitude(),
                targetMilestone.getCoordinates().getLongitude()
        );
        builder.include(milestonePos);

        // Creating limits:
        LatLngBounds bounds = builder.build();

        // Animating the camera:
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }


    /**
     * Focus the map on the current student position (if available) and a list of milestones.
     *
     * @param map              The map to perform.
     * @param studentLocation  Student location (can be null).
     * @param targetMilestones List of milestones to focus.
     */
    public static void focusMapOnStudentAndRoute(GoogleMap map, LatLng studentLocation, List<Milestone> targetMilestones) {
        if (map == null) {
            return;
        }

        // Creating limits object:
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasPoints = false;

        // Including student location, if available:
        if (studentLocation != null) {
            builder.include(studentLocation);
            hasPoints = true;
        }

        // Including all milestones' location:
        if (targetMilestones != null && !targetMilestones.isEmpty()) {
            for (Milestone milestone : targetMilestones) {
                if (milestone != null && milestone.getCoordinates() != null) {
                    LatLng milestonePos = new LatLng(
                            milestone.getCoordinates().getLatitude(),
                            milestone.getCoordinates().getLongitude()
                    );
                    builder.include(milestonePos);
                    hasPoints = true;
                }
            }
        }
        if (!hasPoints) {
            return;
        }

        // Creating limits:
        LatLngBounds bounds = builder.build();

        // Animating the camera:
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }


    /**
     * Rotates the map during the student session playing.
     *
     * @param map      The student map.
     * @param location The current student location.
     */
    public static void rotateStudentMap(GoogleMap map, Location location) {
        if (map == null || location == null || !location.hasBearing()) {
            return;
        }
        CameraPosition currentPos = map.getCameraPosition();
        CameraPosition newPos = new CameraPosition.Builder(currentPos)
                .bearing(location.getBearing())
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(newPos));
    }


    /**
     * Converts and resizes a Vector Drawable into a Google Maps BitmapDescriptor to perform a milestone marker.
     *
     * @param context Context in which the method is called.
     * @param color   The ARGB color to perform the icon.
     * @return The BitmapDescriptor object to build the map using the drawable as its icon.
     */
    public static BitmapDescriptor getMarkerBitmapDescriptor(Context context, Integer color) {

        // Getting the color:
        int finalColor = color == null ? ContextCompat.getColor(context, DEFAULT_COLOR) : color;
        if (milestoneMarkerCache.containsKey(finalColor)) {
            return milestoneMarkerCache.get(finalColor);
        }

        // Getting the drawable (returning the default marker if not available):
        Drawable vectorDrawable = ContextCompat.getDrawable(context, MARKER_MILESTONE_ICON);
        if (vectorDrawable == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }

        // Setting the color:
        Drawable drawable = DrawableCompat.wrap(vectorDrawable).mutate();
        DrawableCompat.setTint(drawable, finalColor);

        // Converting the vector drawable to pixels map:
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (STANDARD_MARKER_SIZE_DP * density);
        drawable.setBounds(0, 0, sizePx, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        drawable.draw(new Canvas(bitmap));

        // Converting the pixels map to BitmapDescriptor:
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);

        // Saving to cache:
        milestoneMarkerCache.put(finalColor, descriptor);

        return descriptor;
    }


    /**
     * Converts and resizes a Vector Drawable into a Google Maps BitmapDescriptor to perform a geofence center.
     *
     * @param context Context in which the method is called.
     * @return The BitmapDescriptor object to build the map using the drawable as its icon.
     */
    public static BitmapDescriptor getGeofenceCenterBitmapDescriptor(Context context) {

        // Getting the color:
        if (geofenceCenterCache != null) {
            return geofenceCenterCache;
        }

        // Getting the drawable (returning the default marker if not available):
        Drawable vectorDrawable = ContextCompat.getDrawable(context, MARKER_GEOFENCE_CENTER_ICON);
        if (vectorDrawable == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }

        // Setting the color:
        Drawable drawable = DrawableCompat.wrap(vectorDrawable).mutate();
        DrawableCompat.setTint(drawable, Color.BLACK);

        // Converting the vector drawable to pixels map:
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (STANDARD_MARKER_SIZE_DP * density);
        drawable.setBounds(0, 0, sizePx, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        drawable.draw(new Canvas(bitmap));

        // Converting the pixels map to BitmapDescriptor:
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);

        // Saving to cache:
        geofenceCenterCache = descriptor;

        return descriptor;
    }


    /**
     * Gets the appropriate marker icon depending on the real-time student status, using a cache for performance.
     *
     * @param context The application context.
     * @param status  The multidimensional live status of the student.
     * @return The BitmapDescriptor for the Google Map marker.
     */
    public static BitmapDescriptor getStudentBitmapDescriptor(Context context, @NonNull LiveStatus status) {
        int drawableId;
        int colorId;

        // Determining the icon and color using the priority:
        if (status.isOutOfGeofence()) {
            drawableId = R.drawable.icon_geofence;
            colorId = R.color.deambulario_orange_dark;
        } else if (status.getConnection() == Student.LiveStatus.Connection.LOST_SIGNAL) {
            drawableId = R.drawable.icon_signal_lost;
            colorId = R.color.student_lost_signal;
        } else if (status.isFinished()) {
            drawableId = R.drawable.icon_ok;
            colorId = R.color.completed;
        } else if (status.getConnection() == Student.LiveStatus.Connection.ONLINE) {
            drawableId = R.drawable.icon_playing;
            colorId = R.color.student_active;
        } else {
            // Disconnected or Abandoned
            return null;
        }

        // Generating a unique cache key:
        String cacheKey = drawableId + "_" + colorId;

        // Checking if we already generated this icon:
        if (markerCache.containsKey(cacheKey)) {
            return markerCache.get(cacheKey);
        }
        // If not cached, we generate it:
        Drawable vectorDrawable = ContextCompat.getDrawable(context, drawableId);
        if (vectorDrawable == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }

        vectorDrawable.setTint(ContextCompat.getColor(context, colorId));
        int width = vectorDrawable.getIntrinsicWidth() * 2;
        int height = vectorDrawable.getIntrinsicHeight() * 2;
        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        vectorDrawable.draw(new Canvas(bitmap));
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);

        // Saving to cache for future uses:
        markerCache.put(cacheKey, descriptor);

        return descriptor;
    }


    /**
     * Updates the color of a marker.
     *
     * @param context The context in which the method is called.
     * @param marker  The marker to color.
     * @param color   Color to perform on the marker.
     */
    public static void updateMarkerColor(@NonNull Context context, @NonNull Marker marker, Integer color) {
        marker.setIcon(getMarkerBitmapDescriptor(context, color));
    }


    //==============================================================================================


    /**
     * Draws a polygonal line throw the milestones, following their order on the list.
     *
     * @param context    The context in which this method is called.
     * @param map        Maps in which to draw the line.
     * @param milestones List of route milestones.
     * @return Polyline drawn on the map.
     */
    @Contract("_, _, null -> null")
    public static Polyline drawRouteLine(@NonNull Context context, GoogleMap map, List<Milestone> milestones) {

        // Only drawing when the map is available and the route has, at least, 2 milestones:
        if (map == null || milestones == null || milestones.size() < 2) {
            return null;
        }

        // Setting polyline appearance:
        PolylineOptions polylineOptions = new PolylineOptions()
                .width(12f)
                .color(ContextCompat.getColor(context, R.color.deambulario_orange))
                .geodesic(true)
                .jointType(JointType.ROUND);

        // Adding milestones-vertex to the polyline:
        for (Milestone m : milestones) {
            LatLng position = new LatLng(m.getCoordinates().getLatitude(), m.getCoordinates().getLongitude());
            polylineOptions.add(position);
        }

        // Drawing polyline on map:
        return map.addPolyline(polylineOptions);
    }


    /**
     * Builds START and END marker labels to be performed on the map of an ordered route.
     * String "START" or "END" is tagged to that additional markers.
     *
     * @param context          The context where this method is called.
     * @param map              The map where add the labels.
     * @param milestones       The ordered list of milestones of the route. New marker labels will be attached on the first and last milestones.
     * @param startLabelMarker The previous START label drawn on the map. Can be null.
     * @param endLabelMarker   The previous END label drawn on the map. Can be null.
     * @return An array of two Markers, containing the new START and END labels.
     */
    @Contract("_, _, null, null, null -> null")
    public static Marker[] addStartAndEnd(@NonNull Context context, @NonNull GoogleMap map, List<Milestone> milestones, Marker startLabelMarker, Marker endLabelMarker) {

        // Deleting previous labels, if exist:
        if (startLabelMarker != null) {
            startLabelMarker.remove();
        }
        if (endLabelMarker != null) {
            endLabelMarker.remove();
        }

        // Only show START and END when there are, at least, two milestones:
        if (milestones == null || milestones.size() < 2) {
            return null;
        }

        // Getting first and last milestones:
        Milestone firstMilestone = milestones.get(0);
        Milestone lastMilestone = milestones.get(milestones.size() - 1);

        // Building START marker-label:
        MarkerOptions startOptions = new MarkerOptions()
                .position(new LatLng(firstMilestone.getCoordinates().getLatitude(), firstMilestone.getCoordinates().getLongitude()))
                .icon(MapUtils.createTextLabel(context, context.getString(R.string.START)))
                .anchor(0.5f, 2.25f)
                .zIndex(50f);
        startLabelMarker = map.addMarker(startOptions);
        Objects.requireNonNull(startLabelMarker).setTag("START");

        // Building END marker-label:
        MarkerOptions endOptions = new MarkerOptions()
                .position(new LatLng(lastMilestone.getCoordinates().getLatitude(), lastMilestone.getCoordinates().getLongitude()))
                .icon(MapUtils.createTextLabel(context, context.getString(R.string.END)))
                .anchor(0.5f, 2.25f)
                .zIndex(50f);
        endLabelMarker = map.addMarker(endOptions);
        Objects.requireNonNull(endLabelMarker).setTag("END");

        return new Marker[]{startLabelMarker, endLabelMarker};
    }


    /**
     * Internal method to build text-labeled markers to be drawn on map. Used to show "START" and "END" labels on ordered routes.
     *
     * @param context The context in which this method is called.
     * @param text    The text to be shown on the text marker.
     * @return the BitMapDescriptor that can be used to build the marker.
     */
    @NonNull
    private static BitmapDescriptor createTextLabel(@NonNull Context context, String text) {

        // Checking if we already generated a label for this text:
        if (textLabelCache.containsKey(text)) {
            return Objects.requireNonNull(textLabelCache.get(text));
        }

        // Building a TextView containing the text:
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(14f);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPadding(24, 12, 24, 12);
        textView.setBackgroundColor(ContextCompat.getColor(context, R.color.deambulario_orange));
        textView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());

        // Drawing the TextView on a BitMap:
        Bitmap bitmap = Bitmap.createBitmap(
                textView.getMeasuredWidth(),
                textView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        textView.draw(new Canvas(bitmap));

        // Converting the BitMap on a BitMapDescriptor:
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);

        // Saving to cache:
        textLabelCache.put(text, descriptor);

        return descriptor;
    }


    //==============================================================================================


    /**
     * Draw geofence bubbles on the map.
     * Geofence on map visually differs depending on the enabled-disabled status.
     *
     * @param context  The context in which this method is called.
     * @param map      The map in which to draw the geofence bubbles.
     * @param geofence List of Geofence objects (behave as circles) which compose the route geofence.
     * @param enabled  Indicates if the non-null geofence is enabled by user.
     * @return List of Circle objets drawn on the map.
     */
    @Contract("_, null, _, _ -> null")
    public static List<Circle> drawGeofence(@NonNull Context context, GoogleMap map, List<Route.Geofence> geofence, boolean enabled) {

        // Checking that the map is available:
        if (map == null) {
            return null;
        }

        List<Circle> bubbles = new ArrayList<>();

        // Going over all Geofence objects:
        for (Route.Geofence bubble : geofence) {

            // Building a Circle object depending on center-radius and enabled-disabled status:
            LatLng center = new LatLng(bubble.getCenter().getLatitude(), bubble.getCenter().getLongitude());
            Circle circle = map.addCircle(new CircleOptions()
                    .center(center)
                    .radius(bubble.getRadius())
                    .strokeWidth(5f)
                    .strokeColor(ContextCompat.getColor(context, R.color.deambulario_orange_ultra_dark))
                    .fillColor(enabled ? ContextCompat.getColor(context, R.color.deambulario_orange_light) & 0x40FFFFFF : 0x00000000));
            bubbles.add(circle);
        }
        return bubbles;
    }


    //==============================================================================================


    /**
     * Checks if a list of markers are covered on a geofence region.
     * This method changes the visual appearance (color) of each marker, depending on its coverage.
     *
     * @param map       The map in which the markers are performed.
     * @param markers   List of markers to check if it's covered.
     * @param geofences List of Geofence objects which compose the geofence of the route.
     * @return True if all the milestones are covered, false otherwise.
     */
    @Contract("_, null, _, _ -> false; _, !null, null, _ -> false")
    public static boolean validateMilestonesCoverage(Context context, GoogleMap map, List<Marker> markers, List<Route.Geofence> geofences) {

        // Checking if resources are available:
        if (map == null || markers == null || markers.isEmpty()) {
            return false;
        }

        // Going over all markers:
        boolean covered = true;
        for (int i = 0; i < markers.size(); i++) {
            Marker marker = markers.get(i);

            // Checking if this marker is covered by geofence and performing its color:
            if (MapUtils.validatePointCoverage(marker.getPosition(), geofences)) {
                MapUtils.updateMarkerColor(context, marker, ContextCompat.getColor(context, R.color.geo_covered));
            } else {
                MapUtils.updateMarkerColor(context, marker, ContextCompat.getColor(context, R.color.geo_uncovered));
                covered = false;
            }
        }
        return covered;
    }


    /**
     * Checks if a point is covered on a geofence region.
     *
     * @param point     The point to check.
     * @param geofences List of Geofence objects which compose the geofence of the route.
     */
    @Contract("_, null -> false")
    public static boolean validatePointCoverage(@NonNull LatLng point, @Nullable List<Route.Geofence> geofences) {

        // Checking if there is at least one geofence bubble:
        if (geofences == null || geofences.isEmpty()) {
            return false;
        }
        float[] distanceResult = new float[1];

        // Going over all geofence bubbles:
        for (Route.Geofence geofence : geofences) {
            // Getting the center of the bubble:
            GeoPoint center = geofence.getCenter();
            // Calculating the distance between the point and the center of the bubble:
            Location.distanceBetween(
                    point.latitude, point.longitude,
                    center.getLatitude(), center.getLongitude(),
                    distanceResult);
            // Checking if the point is inside the bubble:
            if (distanceResult[0] <= geofence.getRadius()) {
                return true;
            }
        }

        // When all bubbles gone over but the point wasn't inside none:
        return false;
    }


    //==============================================================================================


    /**
     * Creates two near locations from a base Milestone. Used to perform the Teacher Guide.
     *
     * @param milestoneBase The base Milestone.
     * @return The location for two near invented milestones.
     */
    public static GeoPoint[] inventMilestones(Milestone milestoneBase) {
        if (milestoneBase == null || milestoneBase.getCoordinates() == null) {
            return new GeoPoint[0];
        }

        double baseLat = milestoneBase.getCoordinates().getLatitude();
        double baseLng = milestoneBase.getCoordinates().getLongitude();
        GeoPoint point1 = new GeoPoint(baseLat + 0.0025, baseLng + 0.0005);
        GeoPoint point2 = new GeoPoint(baseLat - 0.0015, baseLng + 0.002);

        return new GeoPoint[]{point1, point2};
    }


    /**
     * Creates a simple geofence from a list of milestones. Used to perform the Teacher Guide.
     *
     * @param milestoneList The base list of 3 Milestones to cover.
     * @return The performed geofence.
     */
    public static List<Route.Geofence> inventGeofenceFromMilestones(List<Milestone> milestoneList) {
        List<Route.Geofence> geofences = new ArrayList<>();
        if (milestoneList == null || milestoneList.size() != 3) {
            return geofences;
        }

        GeoPoint basePoint = milestoneList.get(0).getCoordinates();
        GeoPoint point1 = milestoneList.get(1).getCoordinates();
        GeoPoint point2 = milestoneList.get(2).getCoordinates();

        geofences.add(createCoveringGeofence(basePoint, point1));
        geofences.add(createCoveringGeofence(basePoint, point2));
        return geofences;
    }


    /**
     * Creates a bubble geofence that covers two GeoPoints.
     *
     * @param p1 One GeoPoint to cover.
     * @param p2 Another one GeoPoint to cover.
     * @return A Geofence bubble covering both GeoPoints.
     */
    private static Route.Geofence createCoveringGeofence(GeoPoint p1, GeoPoint p2) {

        // Center (middle point):
        double midLat = (p1.getLatitude() + p2.getLatitude()) / 2.0;
        double midLng = (p1.getLongitude() + p2.getLongitude()) / 2.0;
        GeoPoint center = new GeoPoint(midLat, midLng);

        // Radius:
        float[] results = new float[1];
        Location.distanceBetween(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), results);
        int radius = (int) Math.ceil((results[0] / 2.0f) * 1.15f);

        // Creating bubble:
        Route.Geofence geofence = new Route.Geofence();
        geofence.setCenter(center);
        geofence.setRadius(radius);
        return geofence;
    }

}