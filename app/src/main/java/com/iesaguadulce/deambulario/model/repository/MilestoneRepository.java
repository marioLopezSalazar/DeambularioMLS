package com.iesaguadulce.deambulario.model.repository;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.*;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.iesaguadulce.deambulario.model.pojos.Content;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.repository.callback.ReposListCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposSingleCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing Milestone data in Firestore and multimedia contents in FirebaseStorage.
 * Milestones are stored as a subcollection within a specific Route document.
 * Implements the singleton pattern.
 * Firestore path: rutas/{routeId}/hitos/{milestoneId}
 *
 * @author Mario López Salazar
 */
public class MilestoneRepository {

    /*
     * Reference for singleton pattern.
     */
    private static MilestoneRepository instance;

    /*
     * Reference to the Firestore database. It's the entry point for all Milestone Firestore operations.
     */
    private final FirebaseFirestore db;


    /**
     * Constructs a new MilestoneRepository object.
     * Create a new Firestore database representation.
     */
    private MilestoneRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Gets the MilestoneRepository.
     *
     * @return The MilestoneRepository instance.
     */
    public static synchronized MilestoneRepository getInstance() {
        if (instance == null) {
            instance = new MilestoneRepository();
        }
        return instance;
    }

    /**
     * Gets the reference of the milestones subcollection of a route.
     *
     * @param routeID Route ID.
     * @return Reference of the milestones subcollection.
     */
    @NonNull
    private CollectionReference getMilestonesCollection(@NonNull String routeID) {
        return db.collection(COLLECTION_ROUTES)
                .document(routeID)
                .collection(COLLECTION_MILESTONES);
    }


    /**
     * Fetch all the route Milestones.
     *
     * @param routeID  The route whose milestones we want to get.
     * @param callback It must implement the both methods (onSuccess and onError) which must be executed after fetching operation finishes.
     */
    public void getAllMilestones(@NonNull String routeID, @NonNull ReposListCallback<Milestone> callback) {

        // 'Get' operation on Firestore:
        getMilestonesCollection(routeID).orderBy(ORDER_FIELD, Query.Direction.ASCENDING).get().addOnCompleteListener(task -> {

            // When successful, build the Milestone list from recovered documents:
            if (task.isSuccessful() && task.getResult() != null) {
                List<Milestone> list = new ArrayList<>();
                for (QueryDocumentSnapshot result : task.getResult()) {
                    // Mapping model attributes:
                    Milestone milestone = result.toObject(Milestone.class);
                    list.add(milestone);
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
     * Persists a new Milestone on the Firestore database, associated to a route.
     *
     * @param milestone The Milestone to persist.
     * @param routeId   The route in which to include the milestone.
     * @param callback  It must implement the both methods (onSuccess and onError) which must be executed after persisting operation finishes.
     */
    public void createMilestone(@NonNull Milestone milestone, @NonNull String routeId, @NonNull ReposSingleCallback<String> callback) {

        getMilestonesCollection(routeId).add(milestone)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }


    /**
     * Updates an existing Milestone on the Firestore database.
     *
     * @param milestone The Milestone to update.
     * @param routeId   The route containing the milestone.
     * @param callback  It must implement the both methods (onSuccess and onError) which must be executed after persisting operation finishes.
     */
    public void updateMilestone(@NonNull Milestone milestone, @NonNull String routeId, @NonNull ReposVoidCallback callback) {
        getMilestonesCollection(routeId).document(milestone.getId())
                .set(milestone, SetOptions.merge())
                .addOnSuccessListener(x -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Removes a Milestone on the Firestore database, including the multimedia of its contents.
     *
     * @param routeId   The ID of the route containing the milestone.
     * @param milestone The Milestone to delete.
     * @param callback  It must implement the both methods (onSuccess and onError) which must be executed after deleting operation finishes.
     */
    public void deleteMilestone(@NonNull Milestone milestone, @NonNull String routeId, @NonNull ReposVoidCallback callback) {

        // Checking multimedia contents:
        if (milestone.getContents() != null) {
            for (Content content : milestone.getContents()) {
                if (content.getType() == Content.ContentType.PICTURE || content.getType() == Content.ContentType.VIDEO) {

                    // Launching silent multimedia deletion:
                    deleteMediaFile(content.getValue(), new ReposVoidCallback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(Exception ignored) {
                        }
                    });
                }
            }
        }

        // Deletion of the milestone:
        getMilestonesCollection(routeId).document(milestone.getId()).delete()
                .addOnSuccessListener(x -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Updates all Milestones of a specific route on the Firestore database.
     * Uses batch deletion in order to avoid multiple requirements to Firestore.
     *
     * @param milestones The list of new Milestone objects to update.
     * @param routeId    The ID of the route whose milestones want to delete.
     * @param callback   It must implement both methods (onSuccess and onError) which will be executed after the batch delete operation finishes.
     */
    public void updateAllMilestones(@NonNull List<Milestone> milestones, @NonNull String routeId, @NonNull ReposVoidCallback callback) {

        // Preparing the milestone updating batch:
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        for (Milestone milestone : milestones) {
            DocumentReference docRef = getMilestonesCollection(routeId).document(milestone.getId());
            batch.update(docRef, ORDER_FIELD, milestone.getOrder());
        }

        // Executing the updating batch:
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Removes all Milestones of a specific route on the Firestore database, including the multimedia of its contents.
     * Uses batch deletion in order to avoid multiple requirements to Firestore.
     *
     * @param routeId  The ID of the route whose milestones want to delete.
     * @param callback It must implement both methods (onSuccess and onError) which will be executed after the batch delete operation finishes.
     */
    public void deleteAllMilestones(@NonNull String routeId, @NonNull ReposVoidCallback callback) {
        getMilestonesCollection(routeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // Preparing the milestone deletion batch:
                    WriteBatch batch = FirebaseFirestore.getInstance().batch();

                    // For each milestone:
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {

                        // Checking multimedia contents:
                        Milestone milestone = snapshot.toObject(Milestone.class);
                        if (milestone != null && milestone.getContents() != null) {
                            for (Content content : milestone.getContents()) {
                                if (content.getType() == Content.ContentType.PICTURE || content.getType() == Content.ContentType.VIDEO) {

                                    // Launching silent multimedia deletion:
                                    deleteMediaFile(content.getValue(), new ReposVoidCallback() {
                                        @Override
                                        public void onSuccess() {
                                        }

                                        @Override
                                        public void onError(Exception ignored) {
                                        }
                                    });
                                }
                            }
                        }

                        // Adding milestone to deletion batch:
                        batch.delete(snapshot.getReference());
                    }

                    // Executing the deletion batch:
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }


    /**
     * Uploads a media file to Firebase Storage and returns its download URL.
     *
     * @param localUri The local URI of the file selected by the user.
     * @param callback Callback to return the download URL (String) or an error.
     */
    public void uploadMediaFile(Uri localUri, ReposSingleCallback<String> callback) {
        // Generating random file name:
        String uniqueFileName = UUID.randomUUID().toString();

        // Setting milestone directory on Firebase Storage:
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference().child(MILESTONES_MEDIA_PATH + uniqueFileName);

        // Uploading the file:
        storageRef.putFile(localUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // On success, request resource URL:
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> callback.onSuccess(downloadUri.toString()))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }


    /**
     * Drops a multimedia stored on Firebase Storage.
     *
     * @param fileUrl  Multimedia URL on Firebase.
     * @param callback Callback to return the operation result.
     */
    public void deleteMediaFile(@NonNull String fileUrl, @NonNull ReposVoidCallback callback) {

        // Getting FirebaseStorage reference:
        FirebaseStorage storage = FirebaseStorage.getInstance();

        try {
            // Getting Storage reference from URL:
            StorageReference ref = storage.getReferenceFromUrl(fileUrl);

            // Launching the deletion:
            ref.delete()
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(callback::onError);

        } catch (IllegalArgumentException e) {
            callback.onError(e);
        }
    }

}