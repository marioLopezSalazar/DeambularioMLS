package com.iesaguadulce.deambulario.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.model.repository.RouteRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposListCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposSingleCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel responsible for managing Route data for the route fragments.
 * It observes the RouteRepository and exposes LiveData to the Fragments.
 *
 * @author Mario López Salazar
 */
public class RouteViewModel extends DataViewModel {

    /*
     * Route repository, which manages routes in database.
     */
    private final RouteRepository repository;

    /*
     * Current routes list container.
     */
    private final MutableLiveData<List<Route>> routesLiveData = new MutableLiveData<>();

    /*
     * Additional current routes list, used for save original data when filtering.
     */
    private List<Route> routesList = new ArrayList<>();

    /*
     * Indicates a Route which is currently affected for a CRUD operation.
     */
    private final MutableLiveData<Route> selectedRouteLiveData = new MutableLiveData<>();




    // --- CONSTRUCTOR ---

    /**
     * Creates a new RouteViewModel object.
     * Connects with the RouteRepository and loads all teacher routes.
     * Not used directly, it's used through a ViewModelProvider.
     */
    private RouteViewModel() {
        this.repository = RouteRepository.getInstance();
        loadRoutes();
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the LiveData Route List containing the current routes list.
     *
     * @return The current routes list container.
     */
    public LiveData<List<Route>> getRoutes() {
        return routesLiveData;
    }

    /**
     * Allows to set a Route which is being to be affected by a CRUD operation.
     * @param route The Route which is being affected.
     */
    public void setSelectedRoute(Route route) {
        selectedRouteLiveData.setValue(route);
    }

    /**
     * Allows to know the Route which is being affected by a CRUD operation.
     * @return The current Route affected by the operation, or null if none.
     */
    public LiveData<Route> getSelectedRoute() {
        return selectedRouteLiveData;
    }





    // ---DATA OPERATIONS---

    /**
     * Loads from the repository all routes of the currently logged-in teacher.
     */
    public void loadRoutes() {
        loadingLiveData.setValue(true);
        repository.getAllRoutes(new ReposListCallback<>() {

            /**
             * Refreshes LiveData with load operation results when success.
             * @param result List of objects returned by the repository operation.
             */
            @Override
            public void onSuccess(List<Route> result) {
                routesList = new ArrayList<>(result);
                routesLiveData.setValue(result);
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with load operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                routesLiveData.setValue(null);
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Filters the list of routes based on a search query and updates the LiveData.
     *
     * @param query The text to search for within the route titles.
     */
    public void filterRoutes(String query) {

        // When empty query, user wants full list:
        if (query == null || query.trim().isEmpty()) {
            routesLiveData.setValue(new ArrayList<>(routesList));
            return;
        }

        // Creating the filtered list:
        List<Route> filteredList = new ArrayList<>();
        query = query.toLowerCase().trim();
        for (Route route : routesList) {
            if (route.getTitle() != null && route.getTitle().toLowerCase().contains(query)) {
                filteredList.add(route);
            }
        }

        // Showing the filtered route to LiveData observers:
        routesLiveData.setValue(filteredList);
    }


    /**
     * Gets a route from its ID, if exists, and establishes it as the current route.
     * @param routeId The ID of the route.
     */
    public void loadRouteById(@NonNull String routeId) {
        loadingLiveData.setValue(true);
        repository.getRouteById(routeId, new ReposSingleCallback<>() {

            /**
             * Refreshes LiveData with load operation results when success.
             * @param route Object returned by the repository operation.
             */
            @Override
            public void onSuccess(Route route) {
                loadingLiveData.setValue(false);
                setSelectedRoute(route);
            }

            /**
             * Refreshes LiveData with load operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(e);
            }
        });
    }


    /**
     * Saves a new Route on the repository.
     *
     * @param route The new Route object to be saved.
     */
    public void saveRoute(Route route) {

        // Avoiding launch insertion when route list is not available:
        if (routesLiveData.getValue() == null) {
            return;
        }

        loadingLiveData.setValue(true);
        repository.createRoute(route, new ReposSingleCallback<>() {

            /**
             * Refreshes LiveData with save operation results when success.
             * @param routeId The ID of the saved route.
             */
            @Override
            public void onSuccess(String routeId) {
                route.setId(routeId);
                routesList.add(route);
                routesLiveData.setValue(new ArrayList<>(routesList));
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
     * Updates a Route on the repository.
     *
     * @param route The Route object to be updated.
     */
    public void updateRoute(Route route) {

        // Avoiding launch updating when Route list is not available:
        if (routesLiveData.getValue() == null) {
            return;
        }

        loadingLiveData.setValue(true);
        repository.updateRoute(route, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with update operation results when success.
             * Updates the Route on the list.
             */
            @Override
            public void onSuccess() {
                routesList.set(routesList.indexOf(route), route);
                routesLiveData.setValue(new ArrayList<>(routesList));
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
     * Deletes a Route on the repository.
     *
     * @param route The Route object to be deleted.
     */
    public void deleteRoute(Route route) {

        // Avoiding launch deletion when Route list is not available:
        if (routesLiveData.getValue() == null) {
            return;
        }

        loadingLiveData.setValue(true);
        repository.deleteRoute(route, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with delete operation results when success.
             * Deletes the Route on the list.
             */
            @Override
            public void onSuccess() {
                routesList.remove(route);
                routesLiveData.setValue(new ArrayList<>(routesList));
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
     * Deletes all the Routes of the teacher on the repository.
     */
    public void deleteAllRoutes(){
        loadingLiveData.setValue(true);
        repository.deleteAllRoutes(new ReposVoidCallback() {
            /**
             * Launched when the deletion is done. Not necessarily indicates success.
             */
            @Override
            public void onSuccess() {
                loadingLiveData.setValue(false);
            }

            /**
             * Not used on this implementation.
             *
             * @param e Not used.
             */
            @Override
            public void onError(Exception e) {
            }
        });
    }
}