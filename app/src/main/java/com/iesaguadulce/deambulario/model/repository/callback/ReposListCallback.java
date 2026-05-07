package com.iesaguadulce.deambulario.model.repository.callback;

import java.util.List;

/**
 * Interface for repository operations that return a list of objects.
 *
 * @param <T> The type of the objects in the list.
 * @author Mario López Salazar
 */
public interface ReposListCallback<T> {

    /**
     * Method that should be executed after successful operation result.
     *
     * @param result List of objects returned by the repository operation.
     */
    void onSuccess(List<T> result);

    /**
     * Method that should be executed after failed operation result.
     *
     * @param e Exception describing the failure.
     */
    void onError(Exception e);
}