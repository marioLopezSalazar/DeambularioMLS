package com.iesaguadulce.deambulario.adapters;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.utils.UIUtils.AdapterMode;
import com.iesaguadulce.deambulario.databinding.ItemMilestoneBinding;
import com.iesaguadulce.deambulario.model.pojos.Milestone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Adapter class for displaying a list of milestones in the Milestones RecyclerView.
 * Supports different display modes (read-only and reorderable).
 *
 * @author Mario López Salazar
 */
public class MilestoneListAdapter extends ListAdapter<Milestone, MilestoneListAdapter.MilestoneViewHolder> {

    /*
     * Listeners to manage clicks on the milestone CardView.
     */
    private final OnMilestoneClickListener tapListener;
    private final OnMilestoneDetailClickListener clickListener;
    private final OnStartDragListener dragStartListener;

    /*
     * Current display mode of the adapter.
     */
    private final AdapterMode mode;

    /*
     * Indicates the last selected milestone.
     */
    private String currentSelectedMilestoneId = null;


    /**
     * Constructs a new MilestoneAdapter.
     *
     * @param mode              The display mode for the adapter items (READ_ONLY or REORDERABLE).
     * @param tapListener       Listener to manage taps on the milestone CardView.
     * @param clickListener     Listener to manage clicks on the details milestone CardView.
     * @param dragStartListener Listener to manage clicks on the drag-and-drop button.
     */
    public MilestoneListAdapter(@NonNull AdapterMode mode, OnMilestoneClickListener tapListener, OnMilestoneDetailClickListener clickListener, OnStartDragListener dragStartListener) {
        super(MILESTONE_CHANGES_CALLBACK);
        this.mode = mode;
        this.tapListener = tapListener;
        this.clickListener = clickListener;
        this.dragStartListener = dragStartListener;
    }


    /**
     * Called when RecyclerView needs a new MilestoneViewHolder to display a milestone.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return MilestoneViewHolder referencing an inflated item_milestone.
     */
    @NonNull
    @Override
    public MilestoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflating a new CardView:
        ItemMilestoneBinding binding = ItemMilestoneBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        // Creating a ViewHolder which maintains reference to the CardView and knows the mode:
        return new MilestoneViewHolder(binding, mode, clickListener, dragStartListener);
    }


    /**
     * Called when RecyclerView needs to show a milestone in a MilestoneViewHolder.
     *
     * @param holder   The ViewHolder which should be updated to represent the milestone.
     * @param position The position of the touched card (not used because milestone reordering).
     */
    @Override
    public void onBindViewHolder(@NonNull MilestoneViewHolder holder, int position) {

        // Retrieving the milestone:
        Milestone currentMilestone = getItem(position);

        // Setting the milestone information into the CardView:
        holder.bind(
                currentMilestone,
                currentMilestone.getId().equals(currentSelectedMilestoneId));

        // Setting the tap listener:
        holder.itemView.setOnClickListener(v -> {
            // Requesting real milestone position on adapter:
            int currentPos = holder.getBindingAdapterPosition();
            tapListener.onMilestoneClick(getItem(currentPos), currentPos);
        });

    }


    /**
     * Swaps two elements on the recyclerview, if the indicated positions are on list limits.
     *
     * @param fromPosition Initial position.
     * @param toPosition   Final position
     */
    public void swapItem(int fromPosition, int toPosition){
        try {
            List<Milestone> currentList = new ArrayList<>(getCurrentList());
            Collections.swap(currentList, fromPosition, toPosition);
            submitList(currentList);
        } catch (IndexOutOfBoundsException ignore) {}
    }


    /**
     * Updates the selected milestone and refreshes the list to highlight it.
     *
     * @param newPosition The position in the recyclerview of the new milestone selected.
     */
    public void setSelectedPosition(int newPosition) {
        if (newPosition == RecyclerView.NO_POSITION || newPosition >= getCurrentList().size()) {
            return;
        }

        // Getting the ID of the new selected milestone:
        String newSelectedId = getCurrentList().get(newPosition).getId();
        if (newSelectedId.equals(currentSelectedMilestoneId)) {
            return;
        }

        // Updating selected milestone variable:
        String oldSelectedId = currentSelectedMilestoneId;
        currentSelectedMilestoneId = newSelectedId;

        // Notifying changes to perform its appearance:
        List<Milestone> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            String id = currentList.get(i).getId();
            if ((id.equals(oldSelectedId) || id.equals(newSelectedId))) {
                notifyItemChanged(i);
            }
        }
    }


    //==============================================================================================

    /**
     * Interface to act when the item_milestone card is touched.
     *
     * @author Mario López Salazar
     */
    public interface OnMilestoneClickListener {
        /**
         * Actions to do when the user touches the milestone CardView.
         *
         * @param milestone Current milestone.
         * @param position  Position of the milestone in the list.
         */
        void onMilestoneClick(Milestone milestone, int position);
    }

    /**
     * Interface to act when the item_milestone card detail-button is clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnMilestoneDetailClickListener {
        /**
         * Actions to do when the user clicks on the detail-button milestone CardView.
         *
         * @param milestone Current milestone.
         */
        void onMilestoneDetailClick(Milestone milestone);
    }

    /**
     * Interface to act when the item_milestone card drag-and-drop button is clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnStartDragListener {
        /**
         * Actions to do when the user clicks on the drag-and-drop button milestone CardView.
         *
         * @param viewHolder The touch ViewHolder.
         */
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }


    //==============================================================================================

    /**
     * Callback for calculating the differences between two Milestones in the underlying list.
     */
    private static final DiffUtil.ItemCallback<Milestone> MILESTONE_CHANGES_CALLBACK = new DiffUtil.ItemCallback<>() {

        /**
         * Indicates if two milestones are the same based on their ID.
         *
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the Milestone in the old and new list have the same ID.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Milestone oldItem, @NonNull Milestone newItem) {
            if (oldItem.getId() == null && newItem.getId() == null) {
                return oldItem == newItem;
            }
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        /**
         * Indicates if the name of two milestones are the same.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the Milestone objects have the same core data.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Milestone oldItem, @NonNull Milestone newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName())
                    && oldItem.getOrder() == newItem.getOrder();
        }
    };


    //==============================================================================================

    /**
     * Maintains reference to the visual elements of a CardView corresponding to a milestone.
     * Also allows setting the model data into the views depending on the AdapterMode.
     *
     * @author Mario López Salazar
     */
    public static class MilestoneViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemMilestoneBinding binding;

        /*
         * The mode defining which UI elements should be visible.
         */
        private final AdapterMode mode;

        /*
         * Listeners to manage the click operations.
         */
        private final OnMilestoneDetailClickListener clickListener;
        private final OnStartDragListener dragStartListener;


        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding           ViewBinding corresponding to the CardView.
         * @param mode              The visual mode of the adapter.
         * @param dragStartListener The drag-and-drop listener.
         */
        public MilestoneViewHolder(ItemMilestoneBinding binding, AdapterMode mode, OnMilestoneDetailClickListener clickListener, OnStartDragListener dragStartListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.mode = mode;
            this.clickListener = clickListener;
            this.dragStartListener = dragStartListener;
        }


        /**
         * Sets the Milestone data into the visual elements of the CardView.
         *
         * @param milestone The Milestone object to be attached on the CardView.
         * @param selected  If that milestone is selected on Google Maps.
         */
        public void bind(Milestone milestone, boolean selected) {

            // Filling in the fields with the milestone's information:
            binding.textviewName.setText(milestone.getName());

            // Performing appearance of selected milestone:
            if (selected) {
                int colorPrimary = MaterialColors.getColor(binding.cardMilestone, androidx.appcompat.R.attr.colorPrimary);
                binding.cardMilestone.setCardBackgroundColor(colorPrimary);
                int colorOnPrimary = MaterialColors.getColor(binding.textviewName, com.google.android.material.R.attr.colorOnPrimary);
                binding.textviewName.setTextColor(colorOnPrimary);
            } else {
                binding.cardMilestone.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.deambulario_orange_ultra_light));
                int colorOnSurface = MaterialColors.getColor(binding.textviewName, com.google.android.material.R.attr.colorOnSurface);
                binding.textviewName.setTextColor(colorOnSurface);
            }

            // Adjusting UI elements visibility based on the mode:
            switch (mode) {
                case READ_ONLY:
                    binding.buttonDrag.setVisibility(View.GONE);
                    break;
                case REORDERABLE:
                    binding.buttonDrag.setVisibility(View.VISIBLE);
                    break;
            }

            // Setting up the OnClick listener for the detail button:
            binding.buttonDetailArrow.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMilestoneDetailClick(milestone);
                }
            });

            // Setting up the drag-and-drop listener:
            binding.buttonDrag.setOnTouchListener(this::drag);
        }


        /**
         * Handles touch events to initiate a drag operation.
         *
         * @param view        The view receiving the touch event.
         * @param motionEvent The captured motion event.
         * @return True if the event is consumed (on ACTION_DOWN or ACTION_UP), false otherwise.
         */
        private boolean drag(View view, @NonNull MotionEvent motionEvent) {
            if (motionEvent.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                if (dragStartListener != null) {
                    dragStartListener.onStartDrag(this);
                }
                return true;
            } else if (motionEvent.getAction() == android.view.MotionEvent.ACTION_UP) {
                view.performClick();
                return true;
            }
            return false;
        }
    }

}