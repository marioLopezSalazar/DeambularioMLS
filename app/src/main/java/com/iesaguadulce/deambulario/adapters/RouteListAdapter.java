package com.iesaguadulce.deambulario.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.iesaguadulce.deambulario.databinding.ItemRouteBinding;
import com.iesaguadulce.deambulario.model.pojos.Route;

import java.util.Objects;


/**
 * Adapter class for displaying a list of routes in the Routes RecyclerView.
 *
 * @author Mario López Salazar
 */
public class RouteListAdapter extends ListAdapter<Route, RouteListAdapter.RouteViewHolder> {

    /*
     * Listener for manage clicks on the route CardView buttons.
     */
    private final OnRouteClickListener listener;


    /**
     * Constructs a new RouteListAdapter.
     *
     * @param listener Listener for manage clicks on the route CardView buttons.
     */
    public RouteListAdapter(OnRouteClickListener listener) {
        super(ROUTE_CHANGES_CALLBACK);
        this.listener = listener;
    }


    /**
     * Called when RecyclerView needs a new RouteViewHolder to display a route.
     * Inflates a new CardView and maintains its reference into the new RouteViewHolder.
     * Note that it can be reused in the future to show some other route.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View (normally CardView).
     * @return RouteViewHolder referencing an inflated item_route.
     */
    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflating a new CardView:
        ItemRouteBinding binding = ItemRouteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        // Creating a ViewHolder which maintains reference to the CardView:
        return new RouteViewHolder(binding);
    }



    /**
     * Called when RecyclerView needs to show a route in a RouteViewHolder.
     * Sets the route information into the CardView referenced by the RouteViewHolder.
     *
     * @param holder   The ViewHolder which should be updated to represent the route.
     * @param position The position of the route within the route List.
     */
    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {

        // Retrieving the route:
        Route currentRoute = getItem(position);

        // Setting the route information and the listener into the CardView:
        holder.bind(currentRoute, listener);
    }


    //==============================================================================================


    /**
     * Interface to act when the item_route CardView buttons are clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnRouteClickListener {

        /**
         * Actions to do when the user clicks on the Play button of the item_route.
         *
         * @param route Current route.
         */
        void onPlayClick(Route route);

        /**
         * Actions to do when the user clicks on the Details button of the item_route.
         *
         * @param route Current route.
         */
        void onDetailClick(Route route);
    }


//==============================================================================================


    /**
     * Callback for calculating the differences between two Routes in the underlying Route list.
     */
    private static final DiffUtil.ItemCallback<Route> ROUTE_CHANGES_CALLBACK = new DiffUtil.ItemCallback<>() {

        /**
         * Indicates if two routes are the same, used when the underlying Route list might have changed.
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the Route in the old and new list are the same.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Route oldItem, @NonNull Route newItem) {
            if (oldItem.getId() == null && newItem.getId() == null) {
                return oldItem == newItem;
            }
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        /**
         * Indicates if the basic information (title+level) of two routes are the same, used when the underlying Routes list might have changed.
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the Route objects in the old and new list have the same title and level.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Route oldItem, @NonNull Route newItem) {
            return
                    Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                    Objects.equals(oldItem.getLevel(), newItem.getLevel());
        }
    };


    //==============================================================================================


    /**
     * Maintains reference to the visual elements of a CardView corresponding to a route.
     * Also allows setting the model data into the views.
     *
     * @author Mario López Salazar
     */
    public static class RouteViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemRouteBinding binding;


        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding ViewBinding corresponding to the CardView that will be stored in the new ViewHolder.
         */
        public RouteViewHolder(ItemRouteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }


        /**
         * Sets the Route data into the visual elements of the CardView.
         *
         * @param route The Route object to be attached on the CardView.
         */
        public void bind(Route route, @NonNull OnRouteClickListener listener) {

            // Filling in the fields with the route's information:
            binding.textRouteTitle.setText(route.getTitle());
            binding.textRouteLevel.setText(route.getLevel());

            // Setting up the OnClick listener for the CardView buttons:
            binding.buttonPlayRoute.setOnClickListener(v -> listener.onPlayClick(route));
            binding.buttonRouteDetail.setOnClickListener(v -> listener.onDetailClick(route));
        }
    }
}