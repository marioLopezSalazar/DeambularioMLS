package com.iesaguadulce.deambulario.model.repository.callback;

/**
 * Interface for repository operations that return a single object.
 *
 * @param <T> The type of the returned object.
 * @author Mario López Salazar
 */
public interface ReposSingleCallback<T> {

    /**
     * Method that should be executed after successful operation result.
     *
     * @param result Returned object of the repository operation.
     */
    void onSuccess(T result);

    /**
     * Method that should be executed after failed operation result.
     *
     * @param e Customized exception.
     */
    void onError(Exception e);
}