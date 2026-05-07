package com.iesaguadulce.deambulario.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.ItemStudentBinding;
import com.iesaguadulce.deambulario.map_and_location.MapUtils;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.pojos.Student.LiveStatus;

import java.util.List;
import java.util.Objects;

/**
 * Adapter class for displaying a list of students on a session.
 *
 * @author Mario López Salazar
 */
public class StudentListAdapter extends ListAdapter<Student, StudentListAdapter.StudentViewHolder> {

    /*
     * Indicates if the recyclerview is attached on the lobby or on the track fragment.
     */
    private final boolean isLobbyMode;

    /*
     * Listeners to manage click actions on the student card (tap on card or click on button).
     */
    private final OnStudentClickListener listener;

    /*
     * Last student selected by the user.
     */
    private String currentSelectedStudentId = null;


    /**
     * Constructs a new StudentListAdapter.
     *
     * @param isLobbyMode Indicates if the list is attached on lobby or tracking fragment, in order to perform the cards' appearance.
     * @param listener    Listener for manage user actions on the cards.
     */
    public StudentListAdapter(boolean isLobbyMode, OnStudentClickListener listener) {
        super(STUDENT_CHANGES_CALLBACK);
        this.isLobbyMode = isLobbyMode;
        this.listener = listener;
    }


    /**
     * Called when RecyclerView needs a new StudentViewHolder to display a student.
     * Inflates a new CardView and maintains its reference into the new StudentViewHolder.
     * Note that it can be reused in the future to show some other student.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View (normally CardView).
     * @return StudentViewHolder referencing an inflated item_student.
     */
    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Inflating a new CardView:
        ItemStudentBinding binding = ItemStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        // Creating a ViewHolder which maintains reference to the CardView:
        return new StudentViewHolder(binding);
    }


    /**
     * Called when RecyclerView needs to show a student in a StudentViewHolder.
     * Sets the student track information into the CardView referenced by the StudentViewHolder.
     *
     * @param holder   The ViewHolder which should be updated to represent the student.
     * @param position The position of the student within the student List.
     */
    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {

        // Retrieving the student:
        Student student = getItem(position);

        // Setting the student information and the listener into the CardView:
        holder.bind(
                student,
                student.getId().equals(currentSelectedStudentId),
                student.getLiveStatus());
    }

    /**
     * Called when RecyclerView needs to update a student status or selection.
     * Only updates the corresponding card appearance.
     *
     * @param holder   The ViewHolder which should be updated to represent the student.
     * @param position The position of the student within the student List.
     * @param payloads Indicates the changes to perform.
     */
    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position, @NonNull List<Object> payloads) {

        // When no payloads, performing full binding:
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        }
        // When payloads, updating card appearance (color and icon):
        else {
            for (Object payload : payloads) {
                if (payload.equals("STATUS_CHANGED") || payload.equals("SELECTION_CHANGED")) {
                    Student student = getItem(position);
                    holder.updateCardButtons(
                            student.getId().equals(currentSelectedStudentId),
                            student.getLiveStatus());
                }
            }
        }
    }


    /**
     * Updates the selected student and refreshes the list to highlight it.
     *
     * @param newSelectedId The ID of the new student selected.
     */
    public void setSelectedStudentId(@NonNull String newSelectedId) {
        if (newSelectedId.equals(currentSelectedStudentId)) {
            return;
        }

        // Updating selected student variable:
        String oldSelectedId = currentSelectedStudentId;
        currentSelectedStudentId = newSelectedId;

        // Notifying changes to perform its appearance:
        List<Student> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            String id = currentList.get(i).getId();
            if ((id.equals(oldSelectedId) || id.equals(newSelectedId))) {
                notifyItemChanged(i, "SELECTION_CHANGED");
            }
        }
    }

    /**
     * Computes the real position of a student on the underlying list.
     *
     * @param studentId The ID of the searched student.
     * @return The position of the student on the underlying list, if found. -1 otherwise.
     */
    public int getStudentPosition(String studentId) {
        List<Student> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getId().equals(studentId)) {
                return i;
            }
        }
        return -1;
    }


    //==============================================================================================


    /**
     * Interface to handle student clicks (tap on card or click on button).
     *
     * @author Mario López Salazar.
     */
    public interface OnStudentClickListener {
        /**
         * Actions to do when the teacher clicks on the student card button.
         *
         * @param student The student whose button has been clicked.
         */
        void onStudentButtonClick(Student student);

        /**
         * Actions to do when the teacher clicks on out-of-geofence card button.
         *
         * @param student The student whose button has been clicked.
         */
        void onStudentOutOfGeofenceClick(Student student);

        /**
         * Actions to do when the teacher taps on the student card.
         *
         * @param student The student whose card has been tapped.
         */
        void onStudentCardClick(Student student);
    }


    //==============================================================================================


    /**
     * Callback for calculating the differences between two students in the underlying Student list.
     */
    private static final DiffUtil.ItemCallback<Student> STUDENT_CHANGES_CALLBACK = new DiffUtil.ItemCallback<>() {

        /**
         * Indicates if two students are the same, used when the underlying Student list might have changed.
         * @param oldItem The item in the old List.
         * @param newItem The item in the new List.
         * @return True, if the Student in the old and new list are the same.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        /**
         * Indicates if two students objects are the same.
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True, if the students objects in the old and new list are the same.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
            return oldItem.equals(newItem) &&
                    Objects.equals(oldItem.getLiveStatus(), newItem.getLiveStatus());
        }

        /**
         * Computes the differences between two Student objects. Used to update when student status changes.
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return A payload containing the differences.
         */
        @Override
        public Object getChangePayload(@NonNull Student oldItem, @NonNull Student newItem) {
            if (!Objects.equals(oldItem.getLiveStatus(), newItem.getLiveStatus())) {
                return "STATUS_CHANGED";
            }
            return super.getChangePayload(oldItem, newItem);
        }
    };


    //==============================================================================================


    /**
     * Maintains reference to the visual elements of a CardView corresponding to a student.
     * Also allows setting the model data into the views.
     *
     * @author Mario López Salazar
     */
    public class StudentViewHolder extends RecyclerView.ViewHolder {

        /*
         * Reference to the visual elements of the CardView.
         */
        private final ItemStudentBinding binding;


        /**
         * Constructs a new ViewHolder object.
         *
         * @param binding ViewBinding corresponding to the CardView that will be stored in the new ViewHolder.
         */
        public StudentViewHolder(ItemStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }


        /**
         * Sets the Student data into the visual elements of the CardView.
         * Changes components visibility depending on the fragment in which the list is attached.
         *
         * @param student    The Student object to be attached on the CardView.
         * @param isSelected Indicates if this student has been selected by the teacher, in order to highlight its card.
         * @param status     The current status of the student.
         */
        public void bind(@NonNull Student student, boolean isSelected, LiveStatus status) {

            // Student nick:
            binding.textUserName.setText(student.getNick());

            // Performing buttons visibility depending on the fragment, selection and student status:
            updateCardButtons(isSelected, status);

            // Setting up the tap and button-click listeners:
            if (isLobbyMode) {
                binding.buttonKickUser.setOnClickListener(v ->
                        listener.onStudentButtonClick(student));
            } else {
                binding.iconAlert.setOnClickListener(v ->
                        listener.onStudentOutOfGeofenceClick(student));
                binding.getRoot().setOnClickListener(v ->
                        listener.onStudentCardClick(student));
            }
        }


        /**
         * Performs buttons visibility depending on the fragment, selection and student status.
         *
         * @param isSelected Indicates if this student has been selected by the teacher, in order to highlight its card.
         * @param status     The current status of the student.
         */
        private void updateCardButtons(boolean isSelected, LiveStatus status) {

            // On lobby, shows kick button:
            if (isLobbyMode) {
                binding.buttonKickUser.setVisibility(View.VISIBLE);
                binding.imageStudentStatus.setVisibility(View.GONE);
                binding.iconAlert.setVisibility(View.GONE);
                binding.iconProgress.setVisibility(View.GONE);
                binding.getRoot().setBackgroundColor(Color.TRANSPARENT);
            }
            // On tracking, shows status indicators and hides kick button:
            else {
                binding.buttonKickUser.setVisibility(View.GONE);
                binding.imageStudentStatus.setVisibility(View.VISIBLE);
                MaterialCardView card = binding.cardStudent;

                // Performing card appearance depending on if the student is selected:
                if (isSelected) {
                    int colorPrimary = MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary);
                    card.setCardBackgroundColor(colorPrimary);
                    int colorOnPrimary = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOnPrimary);
                    binding.textUserName.setTextColor(colorOnPrimary);
                } else {
                    card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.deambulario_orange_ultra_light));
                    int colorOnSurface = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOnSurface);
                    binding.textUserName.setTextColor(colorOnSurface);
                }


                // Painting the 3 dimensions if status is available:
                if (status != null) {
                    // Connection:
                    ColorStateList color = ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), MapUtils.DEFAULT_COLOR));
                    switch (status.getConnection()) {
                        case ONLINE:
                            binding.imageStudentStatus.setImageDrawable(ContextCompat.getDrawable(card.getContext(), R.drawable.icon_playing));
                            color = ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), R.color.student_active));
                            break;
                        case LOST_SIGNAL:
                            binding.imageStudentStatus.setImageDrawable(ContextCompat.getDrawable(card.getContext(), R.drawable.icon_signal_lost));
                            color = ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), R.color.student_lost_signal));
                            break;
                        case DISCONNECTED:
                            binding.imageStudentStatus.setImageDrawable(ContextCompat.getDrawable(card.getContext(), R.drawable.icon_disconnected));
                            color = ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), R.color.student_disconnected));
                            break;
                        case ABANDONED:
                            binding.imageStudentStatus.setImageDrawable(ContextCompat.getDrawable(card.getContext(), R.drawable.icon_logout));
                            color = ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), R.color.student_abandoned));
                            break;
                    }
                    binding.imageStudentStatus.setImageTintList(color);

                    // Route completion:
                    binding.iconProgress.setVisibility(status.isFinished() ? View.VISIBLE : View.INVISIBLE);

                    // Out of geofence:
                    binding.iconAlert.setVisibility(status.isOutOfGeofence() ? View.VISIBLE : View.INVISIBLE);

                } else {
                    binding.imageStudentStatus.setVisibility(View.INVISIBLE);
                    binding.iconAlert.setVisibility(View.INVISIBLE);
                    binding.iconProgress.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

}