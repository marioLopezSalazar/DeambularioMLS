package com.iesaguadulce.deambulario.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.ItemSessionBinding;
import com.iesaguadulce.deambulario.model.pojos.Session;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter class for displaying a list of sessions in the Sessions RecyclerView.
 *
 * @author Mario López Salazar
 */
public class SessionListAdapter extends ListAdapter<Session, SessionListAdapter.SessionViewHolder> {

    /*
     * Listener for manage clicks on the session CardView and its buttons.
     */
    private final OnSessionClickListener listener;


    /**
     * Constructs a new SessionListAdapter.
     *
     * @param listener Listener for manage clicks on the session CardView and its buttons.
     */
    public SessionListAdapter(OnSessionClickListener listener) {
        super(SESSION_CHANGES_CALLBACK);
        this.listener = listener;
    }


    /**
     * Called when RecyclerView needs a new SessionViewHolder to display a session.
     * Inflates a new CardView and maintains its reference into the new SessionViewHolder.
     * Note that it can be reused in the future to show some other session.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View (normally CardView).
     * @return SessionViewHolder referencing an inflated item_session.
     */
    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflating a new CardView:
        ItemSessionBinding binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        // Creating a ViewHolder which maintains reference to the CardView:
        return new SessionViewHolder(binding);
    }


    /**
     * Called when RecyclerView needs to show a session in a SessionViewHolder.
     * Sets the session information into the CardView referenced by the SessionViewHolder.
     *
     * @param holder   The ViewHolder which should be updated to represent the session.
     * @param position The position of the session within the session List.
     */
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {

        // Retrieving the session:
        Session currentSession = getItem(position);

        // Setting the session information and the listener into the CardView:
        holder.bind(currentSession, listener);
    }


    /**
     * Interface to act when the item_session CardView or its buttons are clicked.
     *
     * @author Mario López Salazar
     */
    public interface OnSessionClickListener {

        /**
         * Actions to do when the user clicks on the main action button of the item_session.
         * The action depends on the session state (Lobby, Map or Download).
         *
         * @param session Current session.
         */
        void onSessionActionClick(Session session);

        /**
         * Actions to do when the user performs a long click on the item_session CardView (e.g. Delete).
         *
         * @param session Current session.
         */
        void onSessionLongClick(Session session);
    }


    //==============================================================================================


    /**
     * Callback for calculating the differences between two Sessions in the underlying Session list.
     */
    private static final DiffUtil.ItemCallback<Session> SESSION_CHANGES_CALLBACK = new DiffUtil.ItemCallback<>() {

        /**
         * Indicates if two sessions are the same, used when the underlying Session list might have changed.
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the Session in the old and new list are the same.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Session oldItem, @NonNull Session newItem) {
            if (oldItem.getId() == null && newItem.getId() == null) {
                return oldItem == newItem;
            }
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        /**
         * Indicates if the basic information (state+date) of two sessions are the same.
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the Session objects in the old and new list have the same state and date.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Session oldItem, @NonNull Session newItem) {
            return
                    Objects.equals(oldItem.getState(), newItem.getState()) &&
                            Objects.equals(oldItem.getDate(), newItem.getDate());
        }
    };


    //==============================================================================================


    /**
     * Maintains reference to the visual elements of a CardView corresponding to a session.
     * Also allows setting the model data into the views.
     *
     * @author Mario López Salazar
     */
    public static class SessionViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemSessionBinding binding;


        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding ViewBinding corresponding to the CardView that will be stored in the new ViewHolder.
         */
        public SessionViewHolder(ItemSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }


        /**
         * Sets the Session data into the visual elements of the CardView.
         * Changes visual styles depending on the Session State.
         *
         * @param session  The Session object to be attached on the CardView.
         * @param listener The listener for click events.
         */
        public void bind(@NonNull Session session, @NonNull OnSessionClickListener listener) {

            Context context = binding.getRoot().getContext();

            binding.textSessionTitle.setText(session.getTitleSnapshot());
            SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.date_formatter), Locale.getDefault());
            binding.textSessionDate.setText(sdf.format(session.getDate()));

            // Setting up visual styles depending on Session State:
            ColorStateList colorPrimary = ColorStateList.valueOf(MaterialColors.getColor(binding.cardSession, androidx.appcompat.R.attr.colorPrimary));
            ColorStateList colorOnSurfaceVariant = ColorStateList.valueOf(MaterialColors.getColor(binding.cardSession, com.google.android.material.R.attr.colorOnSurfaceVariant));
            ColorStateList colorDefault = ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), R.color.deambulario_orange_ultra_light));
            ColorStateList colorOnSecondary = ColorStateList.valueOf(MaterialColors.getColor(binding.cardSession, com.google.android.material.R.attr.colorOnSecondary));
            ColorStateList transparent = ColorStateList.valueOf(Color.TRANSPARENT);

            switch (session.getState()) {
                case WAITING:
                    binding.btnSessionAction.setIconResource(R.drawable.icon_joining);
                    binding.btnSessionAction.setIconTint(colorOnSecondary);
                    binding.btnSessionAction.setStrokeColor(colorOnSecondary);
                    binding.cardSession.setCardBackgroundColor(colorPrimary);
                    break;

                case ACTIVE:
                    binding.btnSessionAction.setIconResource(R.drawable.icon_play);
                    binding.btnSessionAction.setIconTint(colorOnSecondary);
                    binding.btnSessionAction.setStrokeColor(colorOnSecondary);
                    binding.cardSession.setCardBackgroundColor(colorOnSurfaceVariant);

                    break;

                case CLOSED:
                    binding.btnSessionAction.setIconResource(R.drawable.icon_download_results);
                    binding.btnSessionAction.setBackgroundTintList(transparent);
                    binding.btnSessionAction.setIconTint(colorOnSurfaceVariant);
                    binding.btnSessionAction.setStrokeColor(colorOnSurfaceVariant);
                    binding.cardSession.setCardBackgroundColor(colorDefault);
                    break;
            }

            // Setting up the OnClick listeners for the CardView elements:
            binding.btnSessionAction.setOnClickListener(v -> listener.onSessionActionClick(session));

            binding.cardSession.setOnLongClickListener(v -> {
                listener.onSessionLongClick(session);
                return true;
            });
        }
    }
}