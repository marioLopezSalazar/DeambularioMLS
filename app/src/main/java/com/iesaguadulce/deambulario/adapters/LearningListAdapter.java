package com.iesaguadulce.deambulario.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.ItemLearningActivityBinding;
import com.iesaguadulce.deambulario.databinding.ItemLearningLinkBinding;
import com.iesaguadulce.deambulario.databinding.ItemLearningPictureBinding;
import com.iesaguadulce.deambulario.databinding.ItemLearningTextBinding;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Answer;
import com.iesaguadulce.deambulario.model.pojos.Content;
import com.iesaguadulce.deambulario.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for displaying mixed milestone items (contents and activities) in a RecyclerView.
 *
 * @author Mario López Salazar
 */
public class LearningListAdapter extends ListAdapter<LearningListAdapter.LearningItem, RecyclerView.ViewHolder> {

    /*
     * Types of learning items.
     */
    private static final int TYPE_TEXT = 0;
    private static final int TYPE_LINK = 1;
    private static final int TYPE_PICTURE = 2;
    private static final int TYPE_ACTIVITY = 3;

    /*
     * Callback for click events on activities and links.
     */
    private final OnActivityClickListener listener;


    /**
     * Constructs a new MilestoneItemAdapter.
     *
     * @param listener Callback for click events.
     */
    public LearningListAdapter(OnActivityClickListener listener) {
        super(MILESTONE_ITEM_DIFF_CALLBACK);
        this.listener = listener;
    }


    /**
     * Updates the underlying data list by flattening contents and activities.
     *
     * @param contents       List of milestone contents.
     * @param activities     List of milestone activities.
     * @param studentAnswers List of answers currently given by the student.
     */
    public void submitData(List<Content> contents, List<Activity> activities, List<Answer> studentAnswers) {
        List<LearningItem> combinedList = new ArrayList<>();

        // Adding contents:
        if (contents != null) {
            for (Content c : contents) {
                combinedList.add(new LearningItem(c));
            }
        }

        // Adding activities:
        if (activities != null) {
            for (Activity a : activities) {
                boolean completed = isActivityCompleted(a.getActivityId(), studentAnswers);
                combinedList.add(new LearningItem(a, completed));
            }
        }

        // Submitting underlying data list:
        submitList(combinedList);
    }


    /**
     * Evaluates the wrapper object to determine the specific ViewType to inflate.
     *
     * @param position Position to query.
     * @return Integer representing the ViewType (TEXT, LINK, PICTURE, or ACTIVITY).
     */
    @Override
    public int getItemViewType(int position) {
        LearningItem item = getItem(position);

        if (item.isActivity()) {
            return TYPE_ACTIVITY;
        } else {
            switch (item.content.getType()) {
                case URL:
                    return TYPE_LINK;
                case PICTURE:
                case VIDEO:
                    return TYPE_PICTURE;
                case TEXT:
                default:
                    return TYPE_TEXT;
            }
        }
    }


    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     * Inflates a new CardView using ViewBinding based on the ViewType.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return Specific ViewHolder referencing the inflated layout.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflating a card depending on the type of content/activity:
        switch (viewType) {
            case TYPE_LINK:
                return new LinkViewHolder(ItemLearningLinkBinding.inflate(inflater, parent, false), listener);
            case TYPE_PICTURE:
                return new PictureViewHolder(ItemLearningPictureBinding.inflate(inflater, parent, false));
            case TYPE_ACTIVITY:
                return new ActivityViewHolder(ItemLearningActivityBinding.inflate(inflater, parent, false), listener);
            case TYPE_TEXT:
            default:
                return new TextViewHolder(ItemLearningTextBinding.inflate(inflater, parent, false));
        }
    }


    /**
     * Called when RecyclerView needs to show data at the specified position.
     * Sets the information into the CardView referenced by the ViewHolder.
     *
     * @param holder   The ViewHolder which should be updated.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LearningItem item = getItem(position);

        // Binding depending on the type of the content/activity:
        if (holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).bind(item.content);
        } else if (holder instanceof LinkViewHolder) {
            ((LinkViewHolder) holder).bind(item.content);
        } else if (holder instanceof PictureViewHolder) {
            ((PictureViewHolder) holder).bind(item.content);
        } else if (holder instanceof ActivityViewHolder) {
            ((ActivityViewHolder) holder).bind(item.activity, item.isCompleted);
        }
    }


    /**
     * Checks if a specific activity has an answer registered by the student.
     *
     * @param activityId     The ID of the activity to check.
     * @param studentAnswers List of answers currently given by the student.
     * @return True if completed, false otherwise.
     */
    private boolean isActivityCompleted(String activityId, List<Answer> studentAnswers) {
        if (studentAnswers == null) {
            return false;
        }
        for (Answer answer : studentAnswers) {
            if (answer.getActivityId().equals(activityId) && answer.getGivenAnswer() != null) {
                return true;
            }
        }
        return false;
    }


    //==============================================================================================


    /**
     * Wrapper class to combine a content or an activity (with its completion status) into a single object.
     *
     * @author Mario López Salazar
     */
    public static class LearningItem {
        /**
         * Educative content (can be null if this item represents an activity).
         */
        final Content content;

        /**
         * Activity (can be null if this item represents a content).
         */
        final Activity activity;

        /**
         * The completion status of the student on the activity.
         */
        final boolean isCompleted;

        /**
         * Builds a new wrapper object for an educative content.
         *
         * @param content The content object.
         */
        public LearningItem(Content content) {
            this.content = content;
            this.activity = null;
            this.isCompleted = false;
        }

        /**
         * Builds a new wrapper object for an activity.
         *
         * @param activity    The activity object.
         * @param isCompleted True if the student has already answered this activity.
         */
        public LearningItem(Activity activity, boolean isCompleted) {
            this.content = null;
            this.activity = activity;
            this.isCompleted = isCompleted;
        }

        /**
         * Checks whether this wrapper contains an activity or a content.
         *
         * @return True if it contains an activity, false if it contains a content.
         */
        public boolean isActivity() {
            return activity != null;
        }
    }


    //==============================================================================================

    /**
     * DiffUtil callback to calculate differences between the old and new lists.
     */
    private static final DiffUtil.ItemCallback<LearningItem> MILESTONE_ITEM_DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {

        /**
         * Indicates if two wrapper objects represent the same logical item.
         *
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the learning items in the old and new list are the same.
         */
        @Override
        public boolean areItemsTheSame(@NonNull LearningItem oldItem, @NonNull LearningItem newItem) {
            if (oldItem.isActivity() != newItem.isActivity()) {
                return false;
            }
            if (oldItem.isActivity()) {
                return oldItem.activity.getActivityId().equals(newItem.activity.getActivityId());
            } else {
                return oldItem.content.getValue().equals(newItem.content.getValue());
            }
        }

        /**
         * Indicates if two wrapper objects contain the same visual information.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the learning item objects have the same core data.
         */
        @Override
        public boolean areContentsTheSame(@NonNull LearningItem oldItem, @NonNull LearningItem newItem) {
            if (oldItem.isActivity()) {
                return oldItem.isCompleted == newItem.isCompleted &&
                        oldItem.activity.getText().equals(newItem.activity.getText());
            } else {
                return oldItem.content.getValue().equals(newItem.content.getValue());
            }
        }
    };


    //==============================================================================================


    /**
     * Interface to handle activity clicks from the Fragment.
     *
     * @author Mario López Salazar
     */
    public interface OnActivityClickListener {
        /**
         * Performs the actions to do when student clicks on the activity action button.
         *
         * @param activity The clicked activity.
         */
        void onActivityClick(Activity activity);

        /**
         * Actions to do when the student clicks on a URL content.
         *
         * @param url The URL of the content.
         */
        void onLinkClick(String url);
    }


    //==============================================================================================


    /**
     * Maintains reference to the visual elements of a CardView representing a Text content.
     *
     * @author Mario López Salazar
     */
    public static class TextViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemLearningTextBinding binding;

        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding ViewBinding corresponding to the CardView.
         */
        public TextViewHolder(@NonNull ItemLearningTextBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Sets the text content data into the visual elements of the CardView.
         *
         * @param content The text content object to be attached on the CardView.
         */
        public void bind(@NonNull Content content) {
            binding.textviewContentText.setText(content.getValue());
        }
    }


    /**
     * Maintains reference to the visual elements of a CardView representing a Link content.
     *
     * @author Mario López Salazar
     */
    public static class LinkViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemLearningLinkBinding binding;

        /*
         * Listener to manage the click operations.
         */
        private final OnActivityClickListener listener;

        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding  ViewBinding corresponding to the CardView.
         * @param listener The card-view click listener.
         */
        public LinkViewHolder(@NonNull ItemLearningLinkBinding binding, OnActivityClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        /**
         * Sets the link content data into the visual elements of the CardView.
         *
         * @param content The link content object to be attached on the CardView.
         */
        public void bind(@NonNull Content content) {

            String title, url;
            String[] parts = content.getValue().split(UIUtils.LINK_SEPARATOR, 2);
            if (parts.length == 2) {
                title = parts[0];
                url = parts[1];
            } else {
                title = content.getValue();
                url = content.getValue();
            }
            binding.textviewContentLink.setText(title);
            binding.textviewContentLink.setPaintFlags(binding.textviewContentLink.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            binding.getRoot().setOnClickListener(v -> listener.onLinkClick(url));
        }
    }


    /**
     * Maintains reference to the visual elements of a CardView representing a Picture content.
     *
     * @author Mario López Salazar
     */
    public static class PictureViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemLearningPictureBinding binding;

        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding ViewBinding corresponding to the CardView.
         */
        public PictureViewHolder(@NonNull ItemLearningPictureBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Sets the picture content data into the visual elements of the CardView.
         *
         * @param content The picture content object to be attached on the CardView.
         */
        public void bind(@NonNull Content content) {
            Context context = binding.getRoot().getContext();
            Glide.with(context)
                    .load(content.getValue())
                    .into(binding.imageviewContentImage);
        }
    }


    /**
     * Maintains reference to the visual elements of a CardView representing an Activity task.
     *
     * @author Mario López Salazar
     */
    public static class ActivityViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemLearningActivityBinding binding;

        /*
         * Listener to manage the click operations.
         */
        private final OnActivityClickListener listener;

        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding  ViewBinding corresponding to the CardView.
         * @param listener The card-view click listener.
         */
        public ActivityViewHolder(@NonNull ItemLearningActivityBinding binding, OnActivityClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        /**
         * Sets the activity data into the visual elements of the CardView.
         *
         * @param activity    The activity content object to be attached on the CardView.
         * @param isCompleted Indicates if the activity has been completed by the student.
         */
        public void bind(@NonNull Activity activity, boolean isCompleted) {
            Context context = binding.getRoot().getContext();

            // Setting texts depending on activity type:
            binding.textviewActivityTitle.setText(getActivityTitle(context, activity.getType()));
            binding.textviewActivitySubtitle.setText(activity.getText());

            // Applying status icon and color:
            if (isCompleted) {
                binding.imageviewActivityStatus.setImageResource(R.drawable.icon_ok);
                binding.imageviewActivityStatus.setColorFilter(ContextCompat.getColor(context, R.color.completed));
            } else {
                binding.imageviewActivityStatus.setImageResource(R.drawable.icon_circle);
                binding.imageviewActivityStatus.setColorFilter(ContextCompat.getColor(context, R.color.pending));
            }

            // Click event:
            binding.getRoot().setOnClickListener(v -> listener.onActivityClick(activity));
        }

        /**
         * Gets a readable title based on the ActivityType.
         *
         * @param context The context in which this method is called.
         * @param type    The type of the activity.
         */
        private String getActivityTitle(Context context, Activity.ActivityType type) {
            if (type == null) {
                return "";
            }
            switch (type) {
                case PHOTO:
                    return context.getString(R.string.take_photo);
                case VIDEO:
                    return context.getString(R.string.take_video);
                case TEST:
                    return context.getString(R.string.test);
                case QUESTION:
                default:
                    return context.getString(R.string.question_to_student);
            }
        }
    }
}