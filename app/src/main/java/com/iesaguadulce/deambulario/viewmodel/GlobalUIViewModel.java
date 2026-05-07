package com.iesaguadulce.deambulario.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing the global appearance of the StudentActivity and TeacherActivity.
 *
 * @author Mario López Salazar
 */
public class GlobalUIViewModel extends ViewModel {

    /*
     * Indicates if there is a current synchronizing data operation with the database.
     */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    /*
     * Indicates if the MilestoneFragment and its descendants must engine on read-only mode.
     */
    private final MutableLiveData<Boolean> readOnlyMode = new MutableLiveData<>(false);

    /*
     * Indicates a customized title for the toolbar.
     */
    private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>(null);

    /*
     * Allows to know if student has granted location and notification permissions.
     */
    private final MutableLiveData<Boolean> permissionsGranted = new MutableLiveData<>(false);

    /*
     * Allows to know if students milestone sounds have been performed.
     */
    private final List<String> soundsTrack = new ArrayList<>();




    /**
     * Allows to know if there is a current synchronizing data operation with the database.
     *
     * @return True if there is a current synchronizing data operation with the database.
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Allows to indicate that there is a current synchronizing data operation with the database.
     */
    public void showLoading() {
        isLoading.setValue(true);
    }

    /**
     * Allows to indicate that there is NOT a current synchronizing data operation with the database.
     */
    public void hideLoading() {
        isLoading.setValue(false);
    }


    /**
     * Allows to know if the MilestoneFragment and its descendants must engine on read-only mode.
     *
     * @return True, if must engine on read-only mode, false otherwise.
     */
    public LiveData<Boolean> isReadOnlyMode() {
        return readOnlyMode;
    }

    /**
     * Allows to indicate if the MilestoneFragment and its descendants must engine on read-only mode.
     *
     * @param isReadOnly True, if must engine on read-only mode.
     */
    public void setReadOnlyMode(boolean isReadOnly) {
        readOnlyMode.setValue(isReadOnly);
    }


    /**
     * Allows to get the customized ToolBar title.
     */
    public LiveData<String> getToolbarTitle() {
        return toolbarTitle;
    }

    /**
     * Allows to establish a new ToolBar title.
     *
     * @param title Customized ToolBar title.
     */
    public void setToolbarTitle(String title) {
        toolbarTitle.setValue(title);
    }


    /**
     * Allows to know if the student has granted location and notification permissions.
     *
     * @return If the student has granted location and notification permissions.
     */
    public LiveData<Boolean> getPermissionsGranted() {
        return permissionsGranted;
    }

    /**
     * Allows to establish if the student has granted location and notification permissions.
     *
     * @param isGranted If both permissions are granted.
     */
    public void setPermissionsGranted(boolean isGranted) {
        permissionsGranted.setValue(isGranted);
    }


    /**
     * Allows to know if the student has heard some milestone-reached sound, or the end-of-route sound.
     *
     * @param  id The ID of the milestone, or 'finish' to refer to the end-of-route.
     * @return True if the sound has been performed.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSoundsTrack(String id) {
        return soundsTrack.contains(id);
    }

    /**
     * Allows to set if the sound of some milestone, or the end-of-route sound, is heard.
     *
     * @param id The ID of the milestone, or 'finish' to refer to the end-of-route.
     */
    public void setSoundsTrack(String id){
        if(!soundsTrack.contains(id)){
            soundsTrack.add(id);
        }
    }
}
