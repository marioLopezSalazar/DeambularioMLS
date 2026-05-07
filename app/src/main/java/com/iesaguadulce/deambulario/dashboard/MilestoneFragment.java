package com.iesaguadulce.deambulario.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.adapters.MilestonePagerAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentMilestoneBinding;
import com.iesaguadulce.deambulario.map_and_location.MapUtils;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Route;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Fragment to manage CRUD operations on a milestone.
 *
 * @author Mario López Salazar
 */
public class MilestoneFragment extends Fragment {

    /*
     * ViewBinding to handle the view and access its elements.
     */
    private FragmentMilestoneBinding binding;

    /*
     * ViewModel to manage global UI appearance.
     */
    private GlobalUIViewModel globalUIViewModel;

    /*
     * ViewModel to manage the data access.
     */
    private RouteViewModel routeViewModel;
    private MilestoneViewModel milestoneViewModel;
    private Route currentRoute;
    private Milestone currentMilestone;

    /*
     * To track if we are editing an existing route or creating a new one.
     */
    private boolean existsMilestone = false;

    /*
     * To track if we're on the read-only mode.
     */
    private boolean readOnly;

    /*
     * To track where to navigate after the saving operation.
     */
    private enum NavigateTo {
        BACK, NONE, MAP
    }

    private NavigateTo navigateTo;

    /*
     * To track if there is a pending saving operation, and if it can be done now (used to wait media operation finish).
     */
    private boolean userMustWaitToSave = false;


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
        binding = FragmentMilestoneBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Performs the UI depending on the mode and sets up this fragment as an observer of the ViewModel
     * (that includes effective navigation other fragments depending on the state of saving operations).
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-construct from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Getting the ViewModels:
        routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Determining if we're on read-only mode:
        Boolean globalReadOnly = globalUIViewModel.isReadOnlyMode().getValue();
        readOnly = (globalReadOnly != null) && globalReadOnly;

        // Setting the ViewPager:
        MilestonePagerAdapter pagerAdapter = new MilestonePagerAdapter(this);
        binding.viewPagerMilestone.setAdapter(pagerAdapter);
        new TabLayoutMediator(binding.tabMilestone, binding.viewPagerMilestone, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.position);
                    break;
                case 1:
                    tab.setText(R.string.content);
                    break;
                case 2:
                    tab.setText(R.string.activities);
                    break;
            }
        }
        ).attach();

        // Determining mode (Creation vs Edition) and populating previous info:
        determineModeAndShowMilestoneData(readOnly);

        // Preparing the ToolBar menu:
        setUpToolbarMenu(readOnly);

        // Setting up this fragment as an observer of the data loading status:
        milestoneViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {

            // Performing UI lock-unlock status:
            UIUtils.updateLoadingState(Collections.singletonList(milestoneViewModel), globalUIViewModel);

            // Navigating if proceeds:
            if (isLoading != null && !isLoading) {
                // Performing pending navigations after a saving operation:
                if (navigateTo == NavigateTo.BACK) {
                    navigateTo = NavigateTo.NONE;
                    Navigation.findNavController(requireView()).navigateUp();
                } else if (navigateTo == NavigateTo.MAP) {
                    navigateTo = NavigateTo.NONE;
                    Navigation.findNavController(requireView()).navigate(R.id.action_milestone_fragment_to_location_select_fragment);
                }
            }
        });

        // Setting up this fragment as an observer of the multimedia loading status:
        milestoneViewModel.isMediaLoading().observe(getViewLifecycleOwner(), isMediaLoading -> {
            boolean userWasWaiting = userMustWaitToSave;

            // If a media operation has just started, we must wait until it ends before saving milestone:
            userMustWaitToSave = (isMediaLoading != null && isMediaLoading);

            // Loading starts:
            if (userMustWaitToSave && !userWasWaiting) {
                Snackbar.make(binding.getRoot(), R.string.media_uploading_start, Snackbar.LENGTH_SHORT).show();
            }

            // If no media operation in course and user waiting to save, we can notify user:
            else if (!userMustWaitToSave && userWasWaiting) {
                Snackbar.make(binding.getRoot(), R.string.media_uploading_done, Snackbar.LENGTH_LONG).show();
            }
        });

        // Notifying errors on operations:
        milestoneViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                milestoneViewModel.clearError();
            }
        });

        // Setup 'enter' on title textview:
        UIUtils.setEndFormOnClickListener(requireActivity(), binding.textInputMilestoneTitle, null, null);

        // Managing back button:
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateTo = NavigateTo.BACK;
                save();
            }
        });
    }


    /**
     * Sets up the ToolBar menu containing the Delete button.
     */
    private void setUpToolbarMenu(boolean isReadOnly) {

        requireActivity().addMenuProvider(
                new MenuProvider() {

                    /**
                     * Creates the Delete menu on the Toolbar, when we're on edit mode.
                     *
                     * @param menu         The Delete menu.
                     * @param menuInflater The inflater to be used to inflate the menu.
                     */
                    @Override
                    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                        if (existsMilestone && !isReadOnly) {
                            menuInflater.inflate(R.menu.menu_delete, menu);
                        }
                    }

                    /**
                     * Manages the OnClick event on the ToolBar menu button.
                     *
                     * @param menuItem The ToolBar Delete menu.
                     * @return True if the event has been consumed.
                     */
                    @Override
                    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.action_delete) {
                            confirmDelete();
                            return true;
                        } else if (menuItem.getItemId() == android.R.id.home) {
                            navigateTo = NavigateTo.BACK;
                            save();
                            return true;
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }


    /**
     * Sets up the UI components depending on the mode (edit-creation-readOnly).
     *
     * @param isReadOnly Indicates if the UI must be shown on read-only mode.
     */
    private void determineModeAndShowMilestoneData(boolean isReadOnly) {

        // Getting the current route and milestone:
        currentRoute = routeViewModel.getSelectedRoute().getValue();
        currentMilestone = milestoneViewModel.getSelectedMilestone().getValue();

        // EDIT MODE (there's a current milestone):
        if (currentMilestone != null) {
            existsMilestone = true;
            // Population data:
            binding.textInputMilestoneTitle.setText(currentMilestone.getName());
            if (isReadOnly) {
                binding.textInputMilestoneTitle.setFocusable(false);
            }

        }
        // CREATION MODE (there is NOT a current milestone):
        else {
            existsMilestone = false;
            // KEY POINT: Creating a new milestone with default coordinates:
            currentMilestone = new Milestone();
            MapUtils.getDefaultPoint(requireContext(), point -> {
                currentMilestone.setCoordinates(point);
                milestoneViewModel.setSelectedMilestone(currentMilestone);
            });

            // Performing teacher guide:
            performGuide();
        }
    }


    /**
     * Called by child fragment LocationFragment to request navigation to the map after a successful save.
     */
    public void requestNavigationToSelectPosition() {
        this.navigateTo = NavigateTo.MAP;
        save();
    }


    /**
     * Manages the saving milestone, if proceeds. Also updates flags masking-save and navigationTo variables.
     */
    private void save() {

        // If 'pending saving' operation when multimedia uploading is on course:
        if (userMustWaitToSave) {
            Snackbar.make(binding.getRoot(), getString(R.string.pending_multimedia_uploading), Snackbar.LENGTH_SHORT).show();
            navigateTo = NavigateTo.NONE;
            return;
        }

        // When read-only mode, we go back without saving (navigate directly because no changes on ViewModel will occur):
        if (readOnly) {
            Navigation.findNavController(requireView()).navigateUp();
            return;
        }

        // Checking if creating but non-info introduced (navigate directly because no changes on ViewModel will occur):
        if (Objects.requireNonNull(binding.textInputMilestoneTitle.getText()).toString().trim().isEmpty()
                && (currentMilestone.getContents() == null || currentMilestone.getContents().isEmpty())
                && (currentMilestone.getActivities() == null || currentMilestone.getActivities().isEmpty())
                && navigateTo == NavigateTo.BACK) {
            Navigation.findNavController(requireView()).navigateUp();
            return;
        }

        // At this point, some milestone info has been added.
        // Checking title is not blank:
        String title = UIUtils.validateNotEmpty(binding.inputMilestoneTitle, getString(R.string.title_cannot_be_empty));
        if (title == null || title.isBlank()) {
            binding.inputMilestoneTitle.requestFocus();
            return;
        }
        currentMilestone.setName(title);


        // Saving on EDIT mode:
        if (existsMilestone) {
            milestoneViewModel.updateMilestone(currentRoute.getId(), currentMilestone);
        }
        // Saving on CREATING mode:
        else {
            // Setting the order:
            List<Milestone> milestones = milestoneViewModel.getMilestones().getValue();
            int order = milestones != null
                    ? milestones.size() + 1
                    : 1;
            currentMilestone.setOrder(order);

            milestoneViewModel.saveMilestone(currentRoute.getId(), currentMilestone);
        }
    }


    /**
     * Shows a confirmation dialog before deleting the milestone.
     * If confirmation is given, the milestone is deleted and the UI navigates back to the Route view/form.
     */
    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_milestone)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(R.string.delete_milestone_confirming)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    milestoneViewModel.deleteMilestone(
                            Objects.requireNonNull(routeViewModel.getSelectedRoute().getValue()).getId(),
                            currentMilestone);
                    navigateTo = NavigateTo.BACK;
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    /**
     * Performs steps 3-4-5-6 of the Teacher guide.
     */
    void performGuide() {

        // STEP 3 - Milestone title and position:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_3_MILESTONE_TITLE_AND_POSITION) {

            // Filling in title:
            UIUtils.animateTyping(
                    new TextInputEditText[]{binding.textInputMilestoneTitle},
                    new String[]{getString(R.string.guide_milestone_title)}, () -> {
                        // Assuring fragment is alive after filling text fields:
                        if (!isAdded() || isDetached() || getView() == null) return;
                        // Highlighting the 'Set location' on the LocationFragment (on ViewPager):
                        LocationFragment locationFragment = getChild(LocationFragment.class);
                        if (locationFragment != null) {
                            locationFragment.performGuide();
                        }
                    });
        }

        // STEP 4 - Milestone contents:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_4_ADD_CONTENT) {

            // Showing contents tab:
            binding.viewPagerMilestone.setCurrentItem(1);
            // Delegating this step to the ContentsFragment:
            binding.viewPagerMilestone.post(() -> {
                ContentsFragment contentFragment = getChild(ContentsFragment.class);
                if (contentFragment != null) {
                    contentFragment.performGuide();
                }
            });
        }

        // STEP 5 - Milestone activities:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_5_ADD_ACTIVITY) {

            // Showing activities tab:
            binding.viewPagerMilestone.setCurrentItem(2);
            // Delegating this step to the ActivitiesFragment:
            binding.viewPagerMilestone.post(() -> {
                ActivitiesFragment activitiesFragment = getChild(ActivitiesFragment.class);
                if (activitiesFragment != null) {
                    activitiesFragment.performGuide();
                }
            });
        }

        // STEP 6 - Auto-saving:
        else if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_6_AUTOSAVE) {

            // Getting the Toolbar:
            Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                // Highlighting the 'Back' button:
                TeacherTourManager.checkFinishMilestoneTour(
                        (TeacherActivity) requireActivity(),
                        toolbar,
                        null
                );
            }
        }
    }


    /**
     * Finds a child fragment. Used to perform the Teacher Guide.
     *
     * @param fragmentClass The found fragment class.
     * @return The child fragment of the indicated class, or null if not found.
     */
    @SuppressWarnings("unchecked")
    private <T extends Fragment> T getChild(Class<T> fragmentClass) {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragmentClass.isInstance(fragment))
                return (T) fragment;
        }
        return null;
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
