package com.iesaguadulce.deambulario.adapters;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.utils.UIUtils.AdapterMode;
import com.iesaguadulce.deambulario.databinding.ItemResourceBinding;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter class for displaying a list of activities in the Activity RecyclerView.
 * Supports different display modes (read-only and reorderable).
 *
 * @author Mario López Salazar
 */
public class ActivityListAdapter extends ListAdapter<Activity, ActivityListAdapter.ActivityViewHolder> {

    /*
     * Current display mode of the adapter.
     */
    private final AdapterMode mode;

    /*
     * Listeners to manage clicks on the milestone CardView.
     */
    private final OnStartDragListener dragStartListener;
    private final OnActivityClickListener clickListener;


    /**
     * Constructs a new ActivityAdapter.
     *
     * @param mode              The display mode for the adapter items (READ_ONLY or REORDERABLE).
     * @param clickListener     Listener to manage clicks on the activity CardView.
     * @param dragStartListener Listener to manage the drag-and-drop operation.
     */
    public ActivityListAdapter(AdapterMode mode, OnActivityClickListener clickListener, OnStartDragListener dragStartListener) {
        super(ACTIVITIES_CHANGES_CALLBACK);
        this.mode = mode;
        this.clickListener = clickListener;
        this.dragStartListener = dragStartListener;
    }

    /**
     * Called when RecyclerView needs a new ActivityViewHolder to display an activity.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return ActivityViewHolder referencing an inflated item_activity.
     */
    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflating a new CardView:
        ItemResourceBinding binding = ItemResourceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        // Creating a ViewHolder which maintains reference to the CardView and knows the mode:
        return new ActivityViewHolder(binding, mode, dragStartListener, clickListener);
    }


    /**
     * Called when RecyclerView needs to show an activity in an ActivityViewHolder.
     *
     * @param holder   The ViewHolder which should be updated to represent the activity.
     * @param position The position of the activity within the activity List.
     */
    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        //Retrieving the activity and setting its information into the CardView:
        holder.bind(getItem(position));
    }

    /**
     * Swaps two elements on the recyclerview, if the indicated positions are on list limits.
     *
     * @param fromPosition Initial position.
     * @param toPosition   Final position
     */
    public void swapItem(int fromPosition, int toPosition) {
        try {
            List<Activity> currentList = new ArrayList<>(getCurrentList());
            Collections.swap(currentList, fromPosition, toPosition);
            submitList(currentList);
        } catch (IndexOutOfBoundsException ignore) {}
    }


    //==============================================================================================


    /**
     * Interface to act when the item_activity card is clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnActivityClickListener {
        /**
         * Actions to do when the user clicks on the activity CardView.
         *
         * @param activity Current activity.
         * @param position Position of the activity in the list.
         */
        void onActivityClick(Activity activity, int position);
    }

    /**
     * Interface to act when the item_activity card drag-and-drop button is clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnStartDragListener {
        /**
         * Actions to do when the user clicks on the drag-and-drop button activity CardView.
         *
         * @param viewHolder The touch ViewHolder.
         */
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }


    //==============================================================================================


    /**
     * Callback for calculating the differences between two activities in the underlying list.
     */
    private static final DiffUtil.ItemCallback<Activity> ACTIVITIES_CHANGES_CALLBACK = new DiffUtil.ItemCallback<>() {
        /**
         * Indicates if two activities are the same.
         *
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the activities in the old and new list are the same.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Activity oldItem, @NonNull Activity newItem) {
            return oldItem.equals(newItem);
        }

        /**
         * Indicates two activities have the same type and text.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the activity objects have the same core data.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Activity oldItem, @NonNull Activity newItem) {
            return oldItem.getType().equals(newItem.getType()) && oldItem.getText().equals(newItem.getText());
        }
    };


    //==============================================================================================


    /**
     * Maintains reference to the visual elements of a CardView corresponding to an Activity.
     * Also allows setting the model data into the views depending on the AdapterMode.
     *
     * @author Mario López Salazar
     */
    public static class ActivityViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemResourceBinding binding;

        /*
         * The mode defining which UI elements should be visible.
         */
        private final AdapterMode mode;

        /*
         * Listeners to manage the click operations.
         */
        private final OnStartDragListener dragStartListener;
        private final OnActivityClickListener clickListener;

        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding           ViewBinding corresponding to the CardView.
         * @param mode              The visual mode of the adapter.
         * @param clickListener     The card-view click listener.
         * @param dragStartListener The drag-and-drop listener.
         */
        public ActivityViewHolder(ItemResourceBinding binding, AdapterMode mode, OnStartDragListener dragStartListener, OnActivityClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.mode = mode;
            this.dragStartListener = dragStartListener;
            this.clickListener = clickListener;
        }


        /**
         * Sets the Activity data into the visual elements of the CardView.
         *
         * @param activity The Activity object to be attached on the CardView.
         */
        public void bind(Activity activity) {

            if (activity.getType() != null) {
                switch (activity.getType()) {
                    case QUESTION:
                        binding.textItemType.setText(R.string.question);
                        break;
                    case PHOTO:
                        binding.textItemType.setText(R.string.take_photo);
                        break;
                    case VIDEO:
                        binding.textItemType.setText(R.string.take_video);
                        break;
                    case TEST:
                        binding.textItemType.setText(R.string.test);
                        break;
                }
                binding.textItemContent.setText(activity.getText());
                binding.mediaContainer.setVisibility(View.GONE);
            }

            // Avoiding reordering on read-only mode:
            if (mode == UIUtils.AdapterMode.READ_ONLY) {
                binding.buttonDrag.setVisibility(View.GONE);
            } else {
                binding.buttonDrag.setVisibility(View.VISIBLE);
            }

            // Setting up the OnClick listener for access to details:
            binding.getRoot().setOnClickListener(v ->
                    clickListener.onActivityClick(activity, getBindingAdapterPosition()));

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