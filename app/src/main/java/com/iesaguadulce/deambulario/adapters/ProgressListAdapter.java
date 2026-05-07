package com.iesaguadulce.deambulario.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.ItemProgressBinding;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Student;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter class for displaying the milestone progression of a student during a session.
 *
 * @author Mario López Salazar
 */
public class ProgressListAdapter extends ListAdapter<ProgressListAdapter.ProgressItem, ProgressListAdapter.MilestoneViewHolder> {


    /**
     * Constructs a new ProgressListAdapter.
     */
    public ProgressListAdapter() {
        super(PROGRESS_DIFF_CALLBACK);
    }


    /**
     * Updates the underlying data list of student progress across the milestones.
     *
     * @param milestones List of route milestones.
     * @param progress   Progress of the student across the milestones.
     */
    public void submitData(@NotNull List<Milestone> milestones, Map<String, Student.MilestoneProgress> progress) {
        List<ProgressItem> combinedList = new ArrayList<>();

        // For each milestone:
        for (Milestone milestone : milestones) {
            // Getting its status (PENDING by default):
            Student.MilestoneProgress status = progress.get(milestone.getId());
            if (status == null) {
                status = Student.MilestoneProgress.PENDING;
            }
            combinedList.add(new ProgressItem(milestone, status));
        }

        // Submitting underlying data list:
        submitList(combinedList);
    }


    /**
     * Called when RecyclerView needs a new ProgressViewHolder to display a milestone progress.
     * Inflates a new CardView and maintains its reference into the new ProgressViewHolder.
     * Note that it can be reused in the future to show some other milestone progress.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View (normally CardView).
     * @return ProgressViewHolder referencing an inflated item_progress.
     */
    @NonNull
    @Override
    public MilestoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflating a new CardView:
        ItemProgressBinding binding = ItemProgressBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        // Creating a ViewHolder which maintains reference to the CardView:
        return new MilestoneViewHolder(binding);
    }


    /**
     * Called when RecyclerView needs to show a milestone progress in a ProgressViewHolder.
     * Sets the progress information into the CardView referenced by the ProgressViewHolder.
     *
     * @param holder   The ViewHolder which should be updated to represent the milestone progress.
     * @param position The position of the milestone within the progress List.
     */
    @Override
    public void onBindViewHolder(@NonNull MilestoneViewHolder holder, int position) {
        // Retrieving the milestone progress:
        ProgressItem item = getItem(position);
        // Setting the progress information into the CardView:
        holder.bind(item.milestone, item.progress);
    }


    //==============================================================================================


    /**
     * Wrapper class to combine a milestone and its corresponding progress into a single object.
     *
     * @author Mario López Salazar
     */
    public static class ProgressItem {

        /**
         * Milestone.
         */
        final Milestone milestone;
        /**
         * The progress of the student on the milestone.
         */
        final Student.MilestoneProgress progress;

        /**
         * Builds a new wrapper object ProgressItem.
         *
         * @param milestone The milestone object.
         * @param progress  The progress of the student on the milestone.
         */
        public ProgressItem(Milestone milestone, Student.MilestoneProgress progress) {
            this.milestone = milestone;
            this.progress = progress;
        }
    }


    /**
     * DiffUtil callback to efficiently calculate differences between the old and new lists.
     */
    private static final DiffUtil.ItemCallback<ProgressItem> PROGRESS_DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {

        /**
         * Indicates if two wrapper progress objects refer to the same milestone.
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the progress in the old and new list refer to the same milestone.
         */
        @Override
        public boolean areItemsTheSame(@NonNull ProgressItem oldItem, @NonNull ProgressItem newItem) {
            return oldItem.milestone.getId().equals(newItem.milestone.getId());
        }

        /**
         * Indicates if two wrapper progress objects contain the same information.
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the wrapper progress objects in the old and new list are the same.
         */
        @Override
        public boolean areContentsTheSame(@NonNull ProgressItem oldItem, @NonNull ProgressItem newItem) {
            return oldItem.progress.equals(newItem.progress) &&
                    oldItem.milestone.getName().equals(newItem.milestone.getName());
        }
    };


    /**
     * Maintains reference to the visual elements of a CardView corresponding to the progress of a student on a milestone.
     * Also allows setting the model data into the views.
     *
     * @author Mario López Salazar
     */
    public static class MilestoneViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemProgressBinding binding;

        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding ViewBinding corresponding to the CardView that will be stored in the new ViewHolder.
         */
        public MilestoneViewHolder(@NonNull ItemProgressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Sets the progress data into the visual elements of the CardView.
         *
         * @param milestone The milestone, to get its name.
         * @param status    The progress of the student on that milestone.
         */
        public void bind(@NonNull Milestone milestone, @NonNull Student.MilestoneProgress status) {

            // Requesting the context, to get the colors:
            Context context = binding.getRoot().getContext();

            // Filling milestone name:
            binding.textviewMilestoneName.setText(milestone.getName());

            // Setting the icon and its color, depending on the progress:
            switch (status) {
                case COMPLETED:
                    binding.imageStatusIcon.setImageResource(R.drawable.icon_ok);
                    binding.imageStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.completed));
                    binding.textviewMilestoneSubtitle.setVisibility(View.GONE);
                    break;

                case INCOMPLETE:
                    binding.imageStatusIcon.setImageResource(R.drawable.icon_incomplete_activities);
                    binding.imageStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.incompleted));
                    binding.textviewMilestoneSubtitle.setVisibility(View.VISIBLE);
                    binding.textviewMilestoneSubtitle.setText(R.string.incomplete_activities);
                    break;

                case PENDING:
                default:
                    binding.imageStatusIcon.setImageResource(R.drawable.icon_milestone_not_visited);
                    binding.imageStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.pending));
                    binding.textviewMilestoneSubtitle.setVisibility(View.VISIBLE);
                    binding.textviewMilestoneSubtitle.setText(R.string.no_visited);
                    break;
            }
        }
    }
}