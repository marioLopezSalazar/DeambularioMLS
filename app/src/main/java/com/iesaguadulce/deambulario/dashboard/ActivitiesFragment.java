package com.iesaguadulce.deambulario.dashboard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.adapters.ActivityListAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentMilestoneResourcesBinding;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.settings.TeacherTourManager;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment to manage the Activities tab corresponding to the ViewPager in Milestone view.
 *
 * @author Mario López Salazar.
 */
public class ActivitiesFragment extends Fragment implements ActivityListAdapter.OnStartDragListener, ActivityListAdapter.OnActivityClickListener {

    /*
     * ViewBinding to perform the fragment view.
     */
    private FragmentMilestoneResourcesBinding binding;

    /*
     * ViewModel of milestone.
     */
    private MilestoneViewModel milestoneViewModel;

    /*
     * Objects to manage the activities recyclerview.
     */
    private ActivityListAdapter adapter;
    private ItemTouchHelper itemTouchHelper;


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
        binding = FragmentMilestoneResourcesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up the fragment as an observer of the ViewModel and performs the UI.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-construct from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Getting the ViewModel:
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);

        // Computing if we're on read-only mode:
        GlobalUIViewModel globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);
        Boolean globalReadOnly = globalUIViewModel.isReadOnlyMode().getValue();
        boolean readOnly = (globalReadOnly != null) && globalReadOnly;

        // Setting up the recyclerview:
        adapter = new ActivityListAdapter(
                readOnly ? UIUtils.AdapterMode.READ_ONLY : UIUtils.AdapterMode.REORDERABLE,
                this, this);
        binding.recyclerviewMilestoneItems.setAdapter(adapter);
        setupDragAndDrop();

        // Observing the viewmodel:
        milestoneViewModel.getSelectedMilestone().observe(getViewLifecycleOwner(), milestone -> {
            if (milestone != null && milestone.getActivities() != null) {
                adapter.submitList(milestone.getActivities());
            }
        });

        // Setting up the add button:
        binding.buttonAddItem.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        binding.buttonAddItem.setOnClickListener(v -> {
            milestoneViewModel.setSelectedActivity(-1);
            ActivityBottomFragment bottomSheet = new ActivityBottomFragment();
            bottomSheet.show(getChildFragmentManager(), "ActivityBottomSheet");
        });
    }


    /**
     * Sets up the reorder operation on the milestones recyclerview.
     */
    private void setupDragAndDrop() {

        // Creating a callback to manage user interactions with the CardView:
        ItemTouchHelper.SimpleCallback dragCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            /**
             * Manages the long-press. In this implementation, it does nothing.
             * @return If long-press was managed.
             */
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            /**
             * Manages the long-press. In this implementation, it does nothing.
             * @return If long-press was managed.
             */
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();

                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false;
                }
                adapter.swapItem(fromPosition, toPosition);
                return true;
            }

            /**
             * Performs actions when the user left-right swipes an element on the recyclerview. Non actions implemented.
             * @param viewHolder The ViewHolder which has been swiped by the user.
             * @param direction  The direction to which the ViewHolder is swiped.
             */
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            /**
             * Performs the actions to do when de interaction ends.
             * Updates the LiveData Activity list on ViewModel with the new order.
             * @param recyclerView The RecyclerView which is controlled by the ItemTouchHelper.
             * @param viewHolder   The View that was interacted by the user.
             */
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                Milestone currentMilestone = milestoneViewModel.getSelectedMilestone().getValue();
                if (currentMilestone != null) {
                    currentMilestone.setActivities(adapter.getCurrentList());
                    milestoneViewModel.setSelectedMilestone(currentMilestone);
                }
            }
        };

        // Creating a ItemTouchHelper and attaching int to the Recyclerview:
        itemTouchHelper = new ItemTouchHelper(dragCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerviewMilestoneItems);
    }


    /**
     * Method override from interface ActivityListAdapter.OnStartDragListener.
     * Implements actions to do when the user clicks on the drag-and-drop button content CardView.
     * Starts drag-and-drop operation of CardViews using the ItemTouchHelper.
     *
     * @param viewHolder The touch ViewHolder.
     */
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }


    /**
     * Method override from interface ActivityListAdapter.OnActivityClickListener.
     * Performs the actions to do when the user taps on the CardView.
     * Opens the bottom sheet with Activity details.
     *
     * @param activity Current activity.
     * @param position Position of the activity in the list.
     */
    @Override
    public void onActivityClick(Activity activity, int position) {
        milestoneViewModel.setSelectedActivity(position);
        ActivityBottomFragment bottomSheet = new ActivityBottomFragment();
        bottomSheet.show(getChildFragmentManager(), "ActivityBottomSheet");
    }


    /**
     * Performs step 5 of the Teacher guide.
     * Creates two example activities and highlights the 'Add activities' button.
     */
    void performGuide() {
        Milestone currentMilestone = milestoneViewModel.getSelectedMilestone().getValue();
        if (currentMilestone != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                int i = 1;

                @Override
                public void run() {
                    // Assuring fragment is alive:
                    if (!isAdded() || isDetached() || getView() == null) return;

                    // Creating two example activities:
                    if (i==1) {
                        Activity a1 = new Activity(java.util.UUID.randomUUID().toString(), Activity.ActivityType.QUESTION, getString(R.string.guide_activity1), null);
                        currentMilestone.setActivities(new ArrayList<>(List.of(a1)));
                        adapter.submitList(currentMilestone.getActivities());
                        i++;
                        handler.postDelayed(this, 500);
                    } else if (i==2){
                        Activity a2 = new Activity(java.util.UUID.randomUUID().toString(), Activity.ActivityType.TEST, getString(R.string.guide_activity2), Arrays.asList(getString(R.string.guide_activity2_answer1), getString(R.string.guide_activity2_answer2), getString(R.string.guide_activity2_answer3)));
                        List<Activity> updatedList = new ArrayList<>(currentMilestone.getActivities());
                        updatedList.add(a2);
                        currentMilestone.setActivities(updatedList);
                        adapter.submitList(updatedList);
                        i++;
                        handler.postDelayed(this, 500);
                    }

                    // Highlighting the 'Add activity' button:
                    else {
                        TeacherTourManager.checkAddActivityTour(
                                (TeacherActivity) requireActivity(),
                                binding.buttonAddItem,
                                () -> {
                                    Fragment parent = getParentFragment();
                                    if (parent instanceof MilestoneFragment) {
                                        ((MilestoneFragment) parent).performGuide();
                                    }
                                }
                        );
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