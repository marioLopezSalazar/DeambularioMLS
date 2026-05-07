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
import com.iesaguadulce.deambulario.model.pojos.Content;
import com.iesaguadulce.deambulario.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Adapter class for displaying a list of contents in the Content RecyclerView.
 * Supports different display modes (read-only and reorderable).
 *
 * @author Mario López Salazar
 */
public class ContentListAdapter extends ListAdapter<Content, ContentListAdapter.ContentViewHolder> {

    /*
     * Listeners to manage clicks on the milestone CardView.
     */
    private final OnStartDragListener dragStartListener;
    private final OnContentClickListener clickListener;

    /*
     * Current display mode of the adapter.
     */
    private final AdapterMode mode;


    /**
     * Constructs a new ContentAdapter.
     *
     * @param mode              The display mode for the adapter items (READ_ONLY or REORDERABLE).
     * @param clickListener     Listener to manage clicks on the content CardView.
     * @param dragStartListener Listener to manage the drag-and-drop operation.
     */
    public ContentListAdapter(AdapterMode mode, OnContentClickListener clickListener, OnStartDragListener dragStartListener) {
        super(CONTENT_CHANGES_CALLBACK);
        this.mode = mode;
        this.clickListener = clickListener;
        this.dragStartListener = dragStartListener;
    }


    /**
     * Called when RecyclerView needs a new ContentViewHolder to display a content.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return ContentViewHolder referencing an inflated item_content.
     */
    @NonNull
    @Override
    public ContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflating a new CardView:
        ItemResourceBinding binding = ItemResourceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        // Creating a ViewHolder which maintains reference to the CardView and knows the mode:
        return new ContentViewHolder(binding, mode, clickListener, dragStartListener);
    }


    /**
     * Called when RecyclerView needs to show a content in a ContentViewHolder.
     *
     * @param holder   The ViewHolder which should be updated to represent the content.
     * @param position The position of the content within the content List.
     */
    @Override
    public void onBindViewHolder(@NonNull ContentViewHolder holder, int position) {
        //Retrieving the content and setting its information into the CardView:
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
            List<Content> currentList = new ArrayList<>(getCurrentList());
            Collections.swap(currentList, fromPosition, toPosition);
            submitList(currentList);
        } catch (IndexOutOfBoundsException ignore) {}
    }


    //==============================================================================================


    /**
     * Interface to act when the item_content card is clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnContentClickListener {
        /**
         * Actions to do when the user clicks on the content CardView.
         *
         * @param content  Current content.
         * @param position Position of the content in the list.
         */
        void onContentClick(Content content, int position);
    }

    /**
     * Interface to act when the item_content card drag-and-drop button is clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnStartDragListener {
        /**
         * Actions to do when the user clicks on the drag-and-drop button content CardView.
         *
         * @param viewHolder The touch ViewHolder.
         */
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

//==============================================================================================


    /**
     * Callback for calculating the differences between two contents in the underlying list.
     */
    private static final DiffUtil.ItemCallback<Content> CONTENT_CHANGES_CALLBACK = new DiffUtil.ItemCallback<>() {
        /**
         * Indicates if two contents are the same.
         *
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the contents in the old and new list are the same.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Content oldItem, @NonNull Content newItem) {
            return oldItem.equals(newItem);
        }

        /**
         * Indicates two contents have the same type and value.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the content objects have the same core data.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Content oldItem, @NonNull Content newItem) {
            return oldItem.getType().equals(newItem.getType()) && oldItem.getValue().equals(newItem.getValue());
        }
    };


    //==============================================================================================


    /**
     * Maintains reference to the visual elements of a CardView corresponding to a content.
     * Also allows setting the model data into the views depending on the AdapterMode.
     *
     * @author Mario López Salazar
     */
    public static class ContentViewHolder extends RecyclerView.ViewHolder {

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
        private final OnContentClickListener clickListener;


        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding           ViewBinding corresponding to the CardView.
         * @param mode              The visual mode of the adapter.
         * @param clickListener     The card-view click listener.
         * @param dragStartListener The drag-and-drop listener.
         */
        public ContentViewHolder(ItemResourceBinding binding, AdapterMode mode, OnContentClickListener clickListener, OnStartDragListener dragStartListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.mode = mode;
            this.dragStartListener = dragStartListener;
            this.clickListener = clickListener;
        }


        /**
         * Sets the Content data into the visual elements of the CardView.
         *
         * @param content The Content object to be attached on the CardView.
         */
        public void bind(Content content) {

            if (content.getType() != null) {
                switch (content.getType()) {
                    case TEXT:
                        binding.textItemType.setText(R.string.text);
                        binding.textItemContent.setText(content.getValue());
                        // Removing underlined, if needed:
                        binding.textItemContent.setPaintFlags(binding.textItemContent.getPaintFlags() & (~android.graphics.Paint.UNDERLINE_TEXT_FLAG));
                        binding.textItemContent.setVisibility(View.VISIBLE);
                        binding.mediaContainer.setVisibility(View.GONE);
                        break;

                    case URL:
                        binding.textItemType.setText(R.string.link);
                        String urlContent = content.getValue();
                        if (urlContent != null) {
                            String[] parts = urlContent.split(UIUtils.LINK_SEPARATOR, 2);
                            if (parts.length == 2) {
                                binding.textItemContent.setText(parts[0]);
                            } else {
                                binding.textItemContent.setText(urlContent);
                            }
                        }
                        // Underlined:
                        binding.textItemContent.setPaintFlags(binding.textItemContent.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                        binding.textItemContent.setVisibility(View.VISIBLE);
                        binding.mediaContainer.setVisibility(View.GONE);
                        break;

                    case PICTURE:
                        binding.textItemType.setText(R.string.picture);
                        binding.textItemContent.setVisibility(View.GONE);
                        binding.mediaContainer.setVisibility(View.VISIBLE);
                        // Setting picture miniature:
                        if (content.getValue() != null && !content.getValue().isEmpty()) {
                            com.bumptech.glide.Glide.with(binding.getRoot().getContext())
                                    .load(content.getValue())
                                    .centerCrop()
                                    .into(binding.contentImage);
                        }
                        break;

                    case VIDEO:
                        binding.textItemType.setText(R.string.video);
                        binding.textItemContent.setVisibility(View.GONE);
                        binding.mediaContainer.setVisibility(View.VISIBLE);
                        // Setting video miniature:
                        if (content.getValue() != null && !content.getValue().isEmpty()) {
                            com.bumptech.glide.Glide.with(binding.getRoot().getContext())
                                    .load(content.getValue())
                                    .centerCrop()
                                    .into(binding.contentImage);
                        }
                        break;
                }

                // Avoiding reordering on read-only mode:
                if (mode == UIUtils.AdapterMode.READ_ONLY) {
                    binding.buttonDrag.setVisibility(View.GONE);
                } else {
                    binding.buttonDrag.setVisibility(View.VISIBLE);
                }

                // Setting up the OnClick listener for access to details:
                binding.getRoot().setOnClickListener(v ->
                        clickListener.onContentClick(content, getBindingAdapterPosition()));

                // Setting up the drag-and-drop listener:
                binding.buttonDrag.setOnTouchListener(this::drag);
            }
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