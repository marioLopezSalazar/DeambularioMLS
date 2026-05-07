package com.iesaguadulce.deambulario.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.ItemFaqBinding;
import com.iesaguadulce.deambulario.model.pojos.FaqItem;

import java.util.List;

/**
 * Adapter class for displaying a list of Frequently Asked Questions (FAQ).
 * Features an accordion-style expand/collapse behavior.
 *
 * @author Mario López Salazar
 */
public class FaqListAdapter extends RecyclerView.Adapter<FaqListAdapter.FaqViewHolder> {

    /*
     * List containing the FAQ data.
     */
    private final List<FaqItem> faqList;

    /**
     * Constructs a new FaqListAdapter.
     *
     * @param faqList The static list of FAQ items to display.
     */
    public FaqListAdapter(List<FaqItem> faqList) {
        this.faqList = faqList;
    }

    /**
     * Called when RecyclerView needs a new FaqViewHolder to display an FAQ item.
     * Inflates a new CardView and maintains its reference into the new FaqViewHolder.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return FaqViewHolder referencing an inflated item_faq.
     */
    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflating a new CardView:
        ItemFaqBinding binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        // Creating a ViewHolder which maintains reference to the CardView:
        return new FaqViewHolder(binding);
    }

    /**
     * Called when RecyclerView needs to show an FAQ item in a FaqViewHolder.
     * Sets the question, answer, and the expanded state into the CardView.
     *
     * @param holder   The ViewHolder which should be updated to represent the FAQ.
     * @param position The position of the FAQ within the list.
     */
    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {

        // Retrieving the current FAQ:
        FaqItem currentFaq = faqList.get(position);

        // Setting the FAQ information and click behavior:
        holder.bind(currentFaq);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of FAQs.
     */
    @Override
    public int getItemCount() {
        return faqList != null ? faqList.size() : 0;
    }


    //==============================================================================================


    /**
     * Maintains reference to the visual elements of a CardView corresponding to an FAQ.
     * Also handles the expand/collapse logic natively.
     *
     * @author Mario López Salazar.
     */
    public static class FaqViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemFaqBinding binding;

        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding ViewBinding corresponding to the CardView that will be stored in the new ViewHolder.
         */
        public FaqViewHolder(ItemFaqBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Sets the FAQ data into the visual elements of the CardView and manages clicks.
         *
         * @param faqItem The FAQ object to be attached on the CardView.
         */
        public void bind(FaqItem faqItem) {

            // Filling in the texts:
            binding.textFaqQuestion.setText(faqItem.getQuestion());
            binding.textFaqAnswer.setText(faqItem.getAnswer());

            // Restoring the expanded/collapsed state to prevent RecyclerView recycling bugs:
            updateVisualState(faqItem.isExpanded());

            // Setting up the OnClick listener for the entire CardView:
            binding.getRoot().setOnClickListener(v -> {
                // Toggle the state in the model:
                boolean isCurrentlyExpanded = faqItem.isExpanded();
                faqItem.setExpanded(!isCurrentlyExpanded);
                updateVisualState(!isCurrentlyExpanded);
            });
        }

        /**
         * Updates the visibility of the answer and the arrow icon based on the state.
         * * @param isExpanded True if the answer should be visible.
         */
        private void updateVisualState(boolean isExpanded) {
            // Update visibility:
            binding.textFaqAnswer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            // Update the arrow:
            int iconResId = isExpanded ? R.drawable.icon_collapse : R.drawable.icon_expand;
            binding.textFaqQuestion.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, iconResId, 0);
        }
    }
}