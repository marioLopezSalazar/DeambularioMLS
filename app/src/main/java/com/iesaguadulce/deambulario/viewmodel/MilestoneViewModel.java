package com.iesaguadulce.deambulario.viewmodel;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.iesaguadulce.deambulario.model.pojos.Content;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.repository.MilestoneRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposListCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposSingleCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel responsible for managing Milestone data.
 * It observes the MilestoneRepository and exposes LiveData to the Fragments.
 *
 * @author Mario López Salazar
 */
public class MilestoneViewModel extends DataViewModel {

    /*
     * Milestone repository, which manages milestone in database.
     */
    private final MilestoneRepository repository;

    /*
     * Route whose milestones list are currently loaded.
     */
    private String currentRouteId = null;

    /*
     * Current milestones list container (LiveData an additional for internal operations).
     */
    private final MutableLiveData<List<Milestone>> milestonesLiveData = new MutableLiveData<>();
    private List<Milestone> milestonesList = new ArrayList<>();

    /*
     * Indicates a Milestone which is currently affected for a CRUD operation.
     */
    private final MutableLiveData<Milestone> selectedMilestoneLiveData = new MutableLiveData<>();

    /*
     * Indicates the position of the Content which is currently affected for a CRUD operation.
     */
    private final MutableLiveData<Integer> selectedContentLiveData = new MutableLiveData<>();

    /*
     * Indicates the position of the Activity which is currently affected for a CRUD operation.
     */
    private final MutableLiveData<Integer> selectedActivityLiveData = new MutableLiveData<>();

    /*
     * Variables for manage multimedia synchronizing data operations with FirebaseStorage.
     */
    private final List<String> pendingMediaOperations = new ArrayList<>();
    private final List<String> cancelledMediaUploads = new ArrayList<>();



    // --- CONSTRUCTOR ---

    /**
     * Creates a new MilestoneViewModel object. Connects with the MilestoneRepository.
     * Not used directly, it's used through a ViewModelProvider.
     */
    private MilestoneViewModel() {
        this.repository = MilestoneRepository.getInstance();
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the LiveData Milestone List containing the current milestones list.
     *
     * @return The current milestones list container.
     */
    public LiveData<List<Milestone>> getMilestones() {
        return milestonesLiveData;
    }


    /**
     * Allows to set a Milestone which is being to be affected by a CRUD operation.
     *
     * @param milestone The Milestone which is being affected.
     */
    public void setSelectedMilestone(Milestone milestone) {
        selectedMilestoneLiveData.setValue(milestone);
    }

    /**
     * Allows to know the Milestone which is being affected by a CRUD operation.
     *
     * @return The current Milestone affected by the operation, or null if none.
     */
    public LiveData<Milestone> getSelectedMilestone() {
        return selectedMilestoneLiveData;
    }


    /**
     * Allows to set the position of the Content which is being to be affected by a CRUD operation.
     *
     * @param position The position of the Content which is being affected.
     */
    public void setSelectedContent(int position) {
        selectedContentLiveData.setValue(position);
    }

    /**
     * Allows to know the position of the Content which is being affected by a CRUD operation.
     *
     * @return The position of the current Content affected by the operation, or null if none.
     */
    public LiveData<Integer> getSelectedContent() {
        return selectedContentLiveData;
    }

    /**
     * Allows to set the position of the Content which is being to be affected by a CRUD operation.
     *
     * @param position The position of the Content which is being affected.
     */
    public void setSelectedActivity(int position) {
        selectedActivityLiveData.setValue(position);
    }

    /**
     * Allows to know the position of the Activity which is being affected by a CRUD operation.
     *
     * @return The position of the current Activity affected by the operation, or null if none.
     */
    public LiveData<Integer> getSelectedActivity() {
        return selectedActivityLiveData;
    }





    // ---DATA OPERATIONS---

    /**
     * Asks the repository for all milestones of a route.
     *
     * @param routeId The ID of the route whose milestones we want.
     */
    public void loadMilestones(String routeId) {

        // Avoiding repository petitions when the route has not changed:
        if (currentRouteId != null && currentRouteId.equals(routeId)) {
            return;
        }

        // Launching the loading operation:
        loadingLiveData.setValue(true);
        repository.getAllMilestones(routeId, new ReposListCallback<>() {

            /**
             * Refreshes LiveData with load operation results when success.
             * @param result List of objects returned by the repository operation.
             */
            @Override
            public void onSuccess(List<Milestone> result) {
                currentRouteId = routeId;
                milestonesList = new ArrayList<>(result);
                milestonesLiveData.setValue(result);
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with load operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                currentRouteId = null;
                milestonesLiveData.setValue(null);
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Asks the repository if a route has at least one milestone.
     * This method uses a callback for give the result and doesn't update the List of Milestone live data.
     *
     * @param routeId  The ID of the route to check.
     * @param callback To get the result of the query.
     */
    public void checkIfRouteHasMilestones(String routeId, ReposSingleCallback<Boolean> callback) {

        // Launching the loading operation:
        loadingLiveData.setValue(true);
        repository.getAllMilestones(routeId, new ReposListCallback<>() {

            /**
             * Refreshes LiveData with load operation results when success.
             * @param result List of objects returned by the repository operation.
             */
            @Override
            public void onSuccess(List<Milestone> result) {
                callback.onSuccess(result != null && !result.isEmpty());
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with load operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                callback.onError(e);
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Loads route milestones when starting a session.
     * This method uses a callback for give the result and doesn't update the List of Milestone live data.
     *
     * @param routeId  The ID of the route whose milestones to get.
     * @param callback To get the result of the query.
     */
    public void getMilestonesForSnapshot(String routeId, ReposListCallback<Milestone> callback) {
        repository.getAllMilestones(routeId, new ReposListCallback<>() {
            @Override
            public void onSuccess(List<Milestone> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }


    /**
     * Saves a new Milestone on the repository.
     *
     * @param routeId   The ID of the route to add the milestone.
     * @param milestone The new Milestone object to be saved.
     */
    public void saveMilestone(String routeId, Milestone milestone) {

        // Avoiding launch insertion when Route list is not available:
        if (milestonesLiveData.getValue() == null) {
            return;
        }

        // Launching the saving operation:
        loadingLiveData.setValue(true);
        repository.createMilestone(milestone, routeId, new ReposSingleCallback<>() {

            /**
             * Refreshes LiveData with save operation results when success.
             * @param milestoneId The ID of the saved milestone.
             */
            @Override
            public void onSuccess(String milestoneId) {
                milestone.setId(milestoneId);
                milestonesList.add(milestone);
                milestonesLiveData.setValue(new ArrayList<>(milestonesList));
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with save operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Updates a Milestone on the repository.
     *
     * @param routeId   The ID of the route to update the milestone.
     * @param milestone The Milestone object to be updated.
     */
    public void updateMilestone(String routeId, Milestone milestone) {

        // Avoiding launch updating when Milestone list is not available:
        if (milestonesLiveData.getValue() == null) {
            return;
        }

        // Launching the updating operation:
        loadingLiveData.setValue(true);
        repository.updateMilestone(milestone, routeId, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with update operation results when success.
             * Updates the Milestone on the list.
             */
            @Override
            public void onSuccess() {
                milestonesList.set(milestonesList.indexOf(milestone), milestone);
                milestonesLiveData.setValue(new ArrayList<>(milestonesList));
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with update operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Update all Milestones associated with a specific route.
     *
     * @param routeId    The ID of the route whose milestones will be saved.
     * @param milestones New milestones list.
     */
    public void updateAllMilestonesFromRoute(String routeId, List<Milestone> milestones) {

        // Avoiding launch updating when Milestone list is not available:
        if (milestonesLiveData.getValue() == null) {
            return;
        }

        // Launching the milestone updating:
        loadingLiveData.setValue(true);
        repository.updateAllMilestones(milestones, routeId, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with saving operation results when success.
             */
            @Override
            public void onSuccess() {
                milestonesList = new ArrayList<>(milestones);
                milestonesLiveData.setValue(milestonesList);
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with update operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Deletes a Milestone on the repository, and updates the position of the rest of milestones.
     *
     * @param routeId   The ID of the route to delete the milestone.
     * @param milestone The Milestone object to be deleted.
     */
    public void deleteMilestone(String routeId, Milestone milestone) {

        // Avoiding launch deletion when Milestone list is not available:
        if (milestonesLiveData.getValue() == null) {
            return;
        }

        // Launching the milestone deletion:
        loadingLiveData.setValue(true);
        repository.deleteMilestone(milestone, routeId, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with delete operation results when success.
             * Deletes the Milestone on the list and updates the order of the rest of milestones.
             */
            @Override
            public void onSuccess() {
                milestonesList.remove(milestone);

                // Updating order of the rest of milestones:
                int pos = milestone.getOrder();
                for (int i = pos - 1; i < milestonesList.size(); i++) {
                    milestonesList.get(i).setOrder(i + 1);
                }
                updateAllMilestonesFromRoute(routeId, milestonesList);
            }

            /**
             * Refreshes LiveData with update operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Deletes all Milestones associated with a specific route.
     *
     * @param routeId            The ID of the route whose milestones will be deleted.
     * @param onCompleteCallback Used to know when the milestones have been deleted (to proceed to delete de route).
     */
    public void deleteAllMilestonesFromRoute(String routeId, ReposVoidCallback onCompleteCallback) {

        // Avoiding launch deletion when Milestone list is not available:
        if (milestonesLiveData.getValue() == null) {
            return;
        }

        // Launching the milestone deletion:
        loadingLiveData.setValue(true);
        repository.deleteAllMilestones(routeId, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with delete operation results when success.
             * Clears the Milestones list.
             */
            @Override
            public void onSuccess() {
                clear();
                currentRouteId = null;
                milestonesList = new ArrayList<>();
                milestonesLiveData.setValue(milestonesList);
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);

                if (onCompleteCallback != null) {
                    onCompleteCallback.onSuccess();
                }
            }

            /**
             * Refreshes LiveData with update operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);

                if (onCompleteCallback != null) {
                    onCompleteCallback.onError(e);
                }
            }
        });
    }


    /**
     * Clears the current milestone list.
     */
    public void clear() {
        milestonesList = new ArrayList<>();
        milestonesLiveData.setValue(new ArrayList<>(milestonesList));
    }


    /**
     * Uploads a multimedia content file to Firebase and updates content URL.
     *
     * @param localUri Local URI of the content.
     * @param content  Current content.
     */
    public void uploadMedia(@NonNull Uri localUri, Content content) {

        // Initializing the media flags:
        loadingMediaLiveData.setValue(true);
        pendingMediaOperations.add(localUri.toString());

        // Launching the uploading:
        repository.uploadMediaFile(localUri, new ReposSingleCallback<>() {

            /**
             * Manages the success end of a media uploading.
             * It replaces the local URI with the Firebase media URL. If the media was required to be deleted during
             * the uploading, a deletion is launched.
             * @param downloadUrl Returned object of the repository operation.
             */
            @Override
            public void onSuccess(String downloadUrl) {

                // Updating the media flag to indicate this media is just uploaded:
                pendingMediaOperations.remove(localUri.toString());

                // Checking if the content was required to be deleted during the uploading:
                if (cancelledMediaUploads.contains(localUri.toString())) {
                    // Deleting on the pending deletions list:
                    cancelledMediaUploads.remove(localUri.toString());
                    // Launching deletion:
                    deleteMedia(downloadUrl);
                }
                // Otherwise (regular case), replace local URI with Firebase Storage URL:
                else {
                    content.setValue(downloadUrl);
                }

                // Updating the media flag operations if there's no pending operations:
                if (pendingMediaOperations.isEmpty()) {
                    loadingMediaLiveData.setValue(false);
                }
            }

            /**
             * Manages the error on a media uploading.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                pendingMediaOperations.remove(localUri.toString());
                cancelledMediaUploads.remove(localUri.toString());
                if (pendingMediaOperations.isEmpty()) {
                    loadingMediaLiveData.setValue(false);
                }
            }
        });
    }


    /**
     * Deletes a media file, either stored in Firebase Storage or pending to upload.
     *
     * @param fileUrl The URL of the file in Firebase Storage.
     */
    public void deleteMedia(@NonNull String fileUrl) {

        // A deletion process launched when the uploading has not finished:
        if (!fileUrl.startsWith("https://firebasestorage")) {
            cancelledMediaUploads.add(fileUrl);
        }

        // A deletion process launched on a media stored on Firebase:
        else {
            // Initializing the media flags:
            loadingMediaLiveData.setValue(true);
            pendingMediaOperations.add(fileUrl);

            // Launching the deletion:
            repository.deleteMediaFile(fileUrl, new ReposVoidCallback() {

                /**
                 * Manages the success end of a deleting operation.
                 */
                @Override
                public void onSuccess() {
                    pendingMediaOperations.remove(fileUrl);
                    if (pendingMediaOperations.isEmpty()) {
                        loadingMediaLiveData.setValue(false);
                    }
                }

                /**
                 * Manages the error end of a deleting operation.
                 */
                @Override
                public void onError(Exception e) {
                    errorLiveData.setValue(e);
                    pendingMediaOperations.remove(fileUrl);
                    if (pendingMediaOperations.isEmpty()) {
                        loadingMediaLiveData.setValue(false);
                    }
                }
            });
        }
    }

}