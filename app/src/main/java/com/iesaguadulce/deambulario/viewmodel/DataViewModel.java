package com.iesaguadulce.deambulario.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


/**
 * Class representing a ViewModel on the App. It offers flags methods to know the current loading and error statuses.
 * The rest of DataViewModel classes on this app must extend this class.
 *
 * @author Mario López Salazar.
 */
public abstract class DataViewModel extends ViewModel {

    /*
     * Indicates if there is a current synchronizing data operation with the database.
     */
    final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    /*
     * Indicates if there are current multimedia synchronizing data operations with FirebaseStorage.
     */
    final MutableLiveData<Boolean> loadingMediaLiveData = new MutableLiveData<>(false);

    /*
     * Indicates if there were an error on the previous repository operation.
     */
    final MutableLiveData<Exception> errorLiveData = new MutableLiveData<>(null);


    /**
     * Allows to know if there is a current synchronizing data operation with the database.
     *
     * @return True if there is a current synchronizing data operation with the database.
     */
    public LiveData<Boolean> isLoading() {
        return loadingLiveData;
    }


    /**
     * Allows to consume the loading flag from the previous synchronizing data operation.
     */
    public void resetLoading() {
        loadingLiveData.setValue(false);
    }


    /**
     * Allows to know if there is a current multimedia synchronizing data operation with the database.
     *
     * @return True if there is a current multimedia synchronizing data operation with the database.
     */
    public LiveData<Boolean> isMediaLoading() {
        return loadingMediaLiveData;
    }


    /**
     * Allows to know if there were an error on the previous repository operation.
     *
     * @return An exception if there were an error on the previous repository operation, or null otherwise.
     */
    public LiveData<Exception> getError() {
        return errorLiveData;
    }


    /**
     * Allows to consume an error from the previous synchronizing data operation.
     */
    public void clearError() {
        errorLiveData.setValue(null);
    }
}
