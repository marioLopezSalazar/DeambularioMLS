package com.iesaguadulce.deambulario.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.ItemGeofenceBinding;
import com.iesaguadulce.deambulario.model.pojos.Route;

import java.util.Locale;

/**
 * Adapter class for displaying a horizontal list of Geofences in the GeofenceEditorFragment.
 * Supports visual selection of items.
 *
 * @author Mario López Salazar
 */
public class GeofenceAdapter extends ListAdapter<Route.Geofence, GeofenceAdapter.GeofenceViewHolder> {

    /**
     * Callback for calculating the differences between two geofences in the underlying list.
     */
    private static final DiffUtil.ItemCallback<Route.Geofence> GEOFENCE_CHANGES_CALLBACK = new DiffUtil.ItemCallback<>() {
        /**
         * Indicates if two geofences are the same.
         * Since Geofence doesn't have an ID, we compare their centers.
         *
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the geofences have the exact same center coordinates.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Route.Geofence oldItem, @NonNull Route.Geofence newItem) {
            return oldItem.getCenter().equals(newItem.getCenter());
        }

        /**
         * Indicates if two geofences have the same content (radius).
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the geofences have the same radius.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Route.Geofence oldItem, @NonNull Route.Geofence newItem) {
            return oldItem.getRadius() == newItem.getRadius();
        }
    };
    /*
     * Listener to manage clicks on the geofence CardView.
     */
    private final OnGeofenceClickListener clickListener;
    /*
     * Stores the currently selected position to highlight it in the UI.
     */
    private int selectedPosition = -1;

    /**
     * Constructs a new GeofenceListAdapter.
     *
     * @param clickListener Listener to manage clicks on the geofence CardView.
     */
    public GeofenceAdapter(OnGeofenceClickListener clickListener) {
        super(GEOFENCE_CHANGES_CALLBACK);
        this.clickListener = clickListener;
    }

    /**
     * Called when RecyclerView needs a new GeofenceViewHolder to display a geofence.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return GeofenceViewHolder referencing an inflated item_geofence.
     */
    @NonNull
    @Override
    public GeofenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflating a new CardView:
        ItemGeofenceBinding binding = ItemGeofenceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new GeofenceViewHolder(binding);
    }

    /**
     * Called when RecyclerView needs to show a geofence in a GeofenceViewHolder.
     *
     * @param holder   The ViewHolder which should be updated to represent the geofence.
     * @param position The position of the geofence within the geofence List.
     */
    @Override
    public void onBindViewHolder(@NonNull GeofenceViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Route.Geofence geofence = getItem(position);
        boolean isSelected = (selectedPosition == position);

        holder.bind(position, isSelected);

        // Setting up the OnClick listener for selection:
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;

            // Refreshing only the changed items for performance
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            clickListener.onGeofenceClick(geofence, position);
        });
    }

    /**
     * Clears the current visual selection in the adapter.
     */
    public void clearSelection() {
        int previous = selectedPosition;
        selectedPosition = -1;
        if (previous != -1) {
            notifyItemChanged(previous);
        }
    }

    //==============================================================================================

    /**
     * Sets the selected position manually (e.g., when adding a new item from the fragment).
     * * @param position The position to select.
     */
    public void setSelectedPosition(int position) {
        int previousSelected = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousSelected);
        notifyItemChanged(selectedPosition);
    }


    //==============================================================================================


    /**
     * Interface to act when the item_geofence card is clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnGeofenceClickListener {
        /**
         * Actions to do when the user clicks on the geofence CardView.
         *
         * @param geofence Current geofence.
         * @param position Position of the geofence in the list.
         */
        void onGeofenceClick(Route.Geofence geofence, int position);
    }


    //==============================================================================================

    /**
     * Maintains reference to the visual elements of a CardView corresponding to a Geofence.
     *
     * @author Mario López Salazar
     */
    public static class GeofenceViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemGeofenceBinding binding;

        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding ViewBinding corresponding to the CardView.
         */
        public GeofenceViewHolder(ItemGeofenceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Sets the Geofence data into the visual elements of the CardView.
         *
         * @param position   The position to display the generic name (Valla 1, Valla 2...).
         * @param isSelected True if the item is currently selected.
         */
        public void bind(int position, boolean isSelected) {
            binding.chipGeofenceItem.setText(String.format(Locale.getDefault(),
                    "%s %d",
                    ContextCompat.getString(binding.getRoot().getContext(), R.string.fence),
                    position + 1));
            binding.chipGeofenceItem.setChecked(isSelected);
        }
    }
}