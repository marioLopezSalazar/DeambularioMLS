package com.iesaguadulce.deambulario.model.repository;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.COLLECTION_ROUTES;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.TEACHER_UID_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.TITLE_FIELD;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.model.repository.callback.ReposListCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposSingleCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing route data in Firestore. Implements the singleton pattern.
 * Firestore path: rutas/
 *
 * @author Mario López Salazar
 */
public class RouteRepository {

    /*
     * Reference for singleton pattern.
     */
    private static RouteRepository instance;

    /*
     * Reference to the Firestore database. It's the entry point for all Route Firestore operations.
     */
    private final FirebaseFirestore db;


    /**
     * Constructs a new RouteRepository object.
     * Create a new Firestore database representation and gets the Teacher UID.
     */
    private RouteRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Gets the RouteRepository.
     *
     * @return The RouteRepository instance.
     */
    public static synchronized RouteRepository getInstance() {
        if (instance == null) {
            instance = new RouteRepository();
        }
        return instance;
    }


    /**
     * Fetch all the teacher routes.
     *
     * @param callback It must implement the both methods (onSuccess and onError) which must be executed after fetching operation finishes.
     */
    public void getAllRoutes(@NonNull ReposListCallback<Route> callback) {

        // Getting the teacherUid:
        FirebaseUser teacher = FirebaseAuth.getInstance().getCurrentUser();
        if (teacher == null) {
            callback.onError(new SecurityException());
            return;
        }
        String teacherUid = teacher.getUid();

        // 'Get' operation on Firestore, filtered by teacherUid:
        db.collection(COLLECTION_ROUTES)
                .whereEqualTo(TEACHER_UID_FIELD, teacherUid)
                .orderBy(TITLE_FIELD)
                .get()
                .addOnCompleteListener(task -> {

                    // When successful, build the route list from recovered documents:
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Route> list = new ArrayList<>();
                        for (QueryDocumentSnapshot result : task.getResult()) {
                            // Mapping model attributes:
                            Route route = result.toObject(Route.class);
                            list.add(route);
                        }
                        // Notifying the successful operation:
                        callback.onSuccess(list);

                    } else {
                        // Notifying the failure operation:
                        callback.onError(task.getException());
                    }
                });
    }


    /**
     * Retrieves a Route from its ID.
     *
     * @param routeId  The ID of the route.
     * @param callback It must implement the both methods (onSuccess and onError) which must be executed after fetching operation finishes.
     */
    public void getRouteById(@NonNull String routeId, @NonNull ReposSingleCallback<Route> callback) {
        db.collection(COLLECTION_ROUTES)
                .document(routeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Route route = documentSnapshot.toObject(Route.class);
                        callback.onSuccess(route);
                    } else {
                        callback.onError(new Exception());
                    }
                })
                .addOnFailureListener(callback::onError);
    }


    /**
     * Persists a new Route on the Firestore database, associated to the logged-in teacher.
     *
     * @param route    The route to persist.
     * @param callback It must implement the both methods (onSuccess and onError) which must be executed after persisting operation finishes.
     */
    public void createRoute(@NonNull Route route, @NonNull ReposSingleCallback<String> callback) {

        // Getting the teacherUid:
        FirebaseUser teacher = FirebaseAuth.getInstance().getCurrentUser();
        if (teacher == null) {
            callback.onError(new SecurityException());
            return;
        }
        String teacherUid = teacher.getUid();

        // Creating the route:
        route.setTeacherId(teacherUid);
        db.collection(COLLECTION_ROUTES).add(route)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }


    /**
     * Update an existing Route on the Firestore database.
     *
     * @param route    The Route to update.
     * @param callback It must implement the both methods (onSuccess and onError) which must be executed after persisting operation finishes.
     */
    public void updateRoute(@NonNull Route route, @NonNull ReposVoidCallback callback) {
        db.collection(COLLECTION_ROUTES).document(route.getId())
                .set(route, SetOptions.merge())
                .addOnSuccessListener(x -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Removes a Route on the Firestore database.
     *
     * @param route    The Route to delete.
     * @param callback It must implement the both methods (onSuccess and onError) which must be executed after deleting operation finishes.
     */
    public void deleteRoute(@NonNull Route route, @NonNull ReposVoidCallback callback) {
        db.collection(COLLECTION_ROUTES).document(route.getId()).delete()
                .addOnSuccessListener(x -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Deletes all teacher routes. Used when deleting user account, so errors are treated on silent.
     *
     * @param finished Used only to know when the operation has finished.
     */
    public void deleteAllRoutes(@NonNull ReposVoidCallback finished) {

        // Getting all teacher routes:
        getAllRoutes(new ReposListCallback<>() {
                         @Override
                         public void onSuccess(List<Route> routesList) {

                             // When no routes:
                             if (routesList == null || routesList.isEmpty()) {
                                 finished.onSuccess();
                                 return;
                             }

                             MilestoneRepository milestoneRepository = MilestoneRepository.getInstance();
                             final int[] counter = {0};
                             final int total = routesList.size();

                             // For each teacher route:
                             for (Route route : routesList) {

                                 // Deleting route milestones:
                                 milestoneRepository.deleteAllMilestones(route.getId(), new ReposVoidCallback() {
                                     @Override
                                     public void onSuccess() {
                                         // Deleting route:
                                         deleteRoute(route, createFinalCallback());
                                     }

                                     @Override
                                     public void onError(Exception e) {
                                         // Deleting route, even when milestones deletion fails:
                                         deleteRoute(route, createFinalCallback());
                                     }

                                     /**
                                      * Gives a callback for each route deletion. Success result is given only when all routes are deleted.
                                      *
                                      * @return A callback indicating that all deletions are done.
                                      */
                                     @NonNull
                                     private ReposVoidCallback createFinalCallback() {
                                         return new ReposVoidCallback() {
                                             @Override
                                             public void onSuccess() { checkIfDone(); }
                                             @Override
                                             public void onError(Exception e) { checkIfDone(); }

                                             /**
                                              * Controls if all deletions are done.
                                              */
                                             private void checkIfDone() {
                                                 counter[0]++;
                                                 if (counter[0] == total) {
                                                     // End of operation:
                                                     finished.onSuccess();
                                                 }
                                             }
                                         };
                                     }
                                 });
                             }
                         }

                         @Override
                         public void onError(Exception e) {
                             // End of operation:
                             finished.onSuccess();
                         }
                     }
        );
    }
}