package com.iesaguadulce.deambulario.model.repository.callback;

/**
 * Interface for repository operations with no return value.
 *
 * @author Mario López Salazar
 */
public interface ReposVoidCallback {

    /**
     * Method that should be executed after successful operation.
     */
    void onSuccess();

    /**
     * Method that should be executed after failed operation.
     *
     * @param e Exception describing the failure.
     */
    void onError(Exception e);
}