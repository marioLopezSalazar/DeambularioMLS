package com.iesaguadulce.deambulario.dashboard;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.adapters.RouteListAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentRoutesListBinding;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.model.repository.callback.ReposSingleCallback;
import com.iesaguadulce.deambulario.notifications.NotifUtils;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import java.util.Arrays;


/**
 * Fragment that displays the list of routes using a RecyclerView.
 *
 * @author Mario López Salazar
 */
public class RoutesListFragment extends Fragment implements RouteListAdapter.OnRouteClickListener {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentRoutesListBinding binding;

    /*
     * ViewModels to manage the data access.
     */
    private RouteViewModel routeViewModel;
    private MilestoneViewModel milestoneViewModel;
    private SessionViewModel sessionViewModel;

    /*
     * ViewModel to manage global UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;


    /**
     * Called to inflate the fragment's interface view.
     *
     * @param inflater           The LayoutInflater object used to inflate any views in the fragment.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRoutesListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up the fragment as an observer of the ViewModel, performs the UI and launches the route list loading.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initializing the RouteListAdapter, setting up this fragment as the CardViews buttons listener:
        RouteListAdapter adapter = new RouteListAdapter(this);

        // Setting up the layout's RecyclerView with the RouteListAdapter:
        binding.recyclerviewRoutes.setAdapter(adapter);

        // Getting the RouteViewModel:
        routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        routeViewModel.setSelectedRoute(null);
        sessionViewModel.setCurrentSession(null);

        // Getting the GlobalUIViewModel:
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Avoiding locks when session finishes:
        sessionViewModel.resetLoading();
        globalUIViewModel.hideLoading();

        // Setting up this fragment as an observer of the Route LiveData changes:
        routeViewModel.getRoutes().observe(getViewLifecycleOwner(), routes -> {

            // When list changes, we'll pass the new list to the RouteListAdapter:
            adapter.submitList(routes);

            // Managing the Empty-routes-list indicator visibility:
            if (routes != null && !routes.isEmpty()) {
                binding.recyclerviewRoutes.setVisibility(android.view.View.VISIBLE);
                binding.layoutEmptyStateRoutes.setVisibility(android.view.View.GONE);
            } else {
                binding.layoutEmptyStateRoutes.setVisibility(android.view.View.VISIBLE);
                binding.recyclerviewRoutes.setVisibility(android.view.View.GONE);
            }
        });
        routeViewModel.getError().observe(getViewLifecycleOwner(), exception -> {
            if (exception != null) {
                Snackbar.make(binding.getRoot(), getResources().getString(R.string.error_network), Snackbar.LENGTH_LONG).show();
                routeViewModel.clearError();
            }
        });

        // Setting up this fragment as an observer of the Session LiveData changes:
        sessionViewModel.getNavigateToLobbyEvent().observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (shouldNavigate != null && shouldNavigate) {
                sessionViewModel.doneNavigatingToLobby();
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_route_list_to_session_lobby);
            }
        });
        sessionViewModel.getError().observe(getViewLifecycleOwner(), exception -> {
            if (exception != null) {
                Snackbar.make(binding.getRoot(), R.string.error_when_creating_session, Snackbar.LENGTH_LONG).show();
                sessionViewModel.clearError();
            }
        });

        // Managing UI lock on loading operations:
        observeLoadingStates();

        // Setting up the AddRoute OnClick listener:
        binding.buttonAddRoute.setOnClickListener(v -> {
            routeViewModel.setSelectedRoute(null);
            Navigation.findNavController(binding.getRoot()).navigate(R.id.action_route_list_to_route_form);
        });

        // Performing teacher guide starting:
        performGuide();

        // Setting up the route SearchView:
        setUpRouteSearching();
    }


    /**
     * Locks or unlocks the UI depending on the loading data operations status.
     * Performs this fragment as an observer of the 'isLoading' flags of all the DataViewModels.
     */
    private void observeLoadingStates() {

        // Creating a common observer which performs UI lock-unlock:
        Observer<Boolean> loadingObserver = isLoading ->
                UIUtils.updateLoadingState(
                        Arrays.asList(routeViewModel, milestoneViewModel, sessionViewModel),
                        globalUIViewModel);

        // Launching that common observer when some DataViewModel loading flag changes:
        routeViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        milestoneViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        sessionViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
    }


    /**
     * Manages clicks on the Play button of a route CardView.
     * Launches existing milestones query, and allows to create a new a session if the route has at least one milestone.
     *
     * @param route The route corresponding to the clicked CardView.
     */
    @Override
    public void onPlayClick(Route route) {
        milestoneViewModel.checkIfRouteHasMilestones(route.getId(), new ReposSingleCallback<>() {
            @Override
            public void onSuccess(Boolean hasMilestones) {
                if (hasMilestones) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.new_session)
                            .setIcon(R.mipmap.icon_deambulario)
                            .setMessage(R.string.new_session_confirm)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                routeViewModel.setSelectedRoute(route);
                                sessionViewModel.createSession(
                                        NotifUtils.getLocalFCMToken(requireContext()),
                                        route.getId(),
                                        route.getTitle());
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.no_milestones_on_this_route, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Manages clicks on the Details button of a route CardView.
     * Sets up the clicked Route on the RouteViewModel as the current selected route, and navigates up to the details fragment.
     *
     * @param route The route corresponding to the clicked CardView.
     */
    @Override
    public void onDetailClick(Route route) {
        routeViewModel.setSelectedRoute(route);
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_route_list_to_route_view);
    }


    /**
     * Manages the routes filtering using the SearchView.
     */
    private void setUpRouteSearching() {

        // Piping the Search TextView real-time text changes to the RouteViewModel:
        binding.textSearchRoutes.addTextChangedListener(new TextWatcher() {
            /**
             * Used to know that a substring of the text is about to be replaced. Not needed for this implementation.
             * @param s     The watched text.
             * @param start Position where replacing starts.
             * @param count Quantity of original characters.
             * @param after Quantity of new characters.
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            /**
             * Used to know that a substring of the text has just been replaced.
             * On this implementation, pipes the search query to the RouteViewModel on every keystroke.
             * @param s      The watched text.
             * @param start  Position where replacing starts.
             * @param before Quantity of old characters.
             * @param count  Quantity of new characters.
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null) {
                    routeViewModel.filterRoutes(s.toString());
                }
            }

            /**
             * Used to know that the text has been changed. Not needed for this implementation.
             * @param s The watched text.
             */
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Managing searching end actions:
        binding.searchLayoutRoutes.setEndIconOnClickListener(v -> {
            binding.textSearchRoutes.setText("");
            UIUtils.hideKeyboard(requireActivity());
            binding.textSearchRoutes.clearFocus();
        });

    }


    /**
     * Performs steps 1-13-14 of the Teacher guide.
     */
    private void performGuide(){

        // STEP 1 - Create route:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_1_WELCOME_AND_ROUTES_LIST) {

            // Highlighting the 'Add route' button:
            TeacherTourManager.startManageTeacherTour((TeacherActivity) requireActivity(), binding.buttonAddRoute, null);
        }

        // STEP 13 - Route edition and deletion:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_13_ROUTE_DETAIL_ACCESS) {

            // Highlighting a 'Route detail' button:
            binding.recyclerviewRoutes.post(() -> {
                RecyclerView.ViewHolder holder = binding.recyclerviewRoutes.findViewHolderForAdapterPosition(0);
                if (holder != null) {
                    View button = holder.itemView.findViewById(R.id.button_route_detail);
                    if (button != null) {
                        TeacherTourManager.checkRouteEditTour((TeacherActivity) requireActivity(), button, this::performGuide);
                    }
                }
            });
        }

        // STEP 14 - Create session:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_14_CREATE_SESSION) {

            // Highlighting a 'Create session' button:
            binding.recyclerviewRoutes.post(() -> {
                RecyclerView.ViewHolder holder = binding.recyclerviewRoutes.findViewHolderForAdapterPosition(0);
                if (holder != null) {
                    View button = holder.itemView.findViewById(R.id.button_play_route);
                    if (button != null) {
                        TeacherTourManager.checkCreateSessionTour((TeacherActivity) requireActivity(), button);
                    }
                }
            });
        }
    }


    /**
     * Called when the Fragment is being destroyed. Avoids view binding memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}