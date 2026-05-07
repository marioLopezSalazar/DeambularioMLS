package com.iesaguadulce.deambulario.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.FragmentAuthSelectBinding;


/**
 * Fragment to allow the user to select his/her profile: Teacher or Student.
 *
 * @author Mario López Salazar
 */
public class SelectProfileFragment extends Fragment {

    /*
     * Binding object to access the views.
     */
    private FragmentAuthSelectBinding binding;


    /**
     * Called to inflate the fragment's interface view.
     *
     * @param inflater           The LayoutInflater object used to inflate any views in the fragment.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAuthSelectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Sets up the button OnClick listeners, navigating to Teacher or Student login fragment.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnTeacher.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_selectProfileFragment_to_teacherLoginFragment));
        binding.btnStudent.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_selectProfileFragment_to_studentLoginFragment));
        binding.textviewPrivacyPolicy.setOnClickListener( v ->
                showPrivacyDialog());
    }


    /**
     * Shows a dialog containing the PRIVACY POLICY AND LEGAL NOTICE of the app.
     */
    private void showPrivacyDialog(){
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_message_log, null);
        TextView textView = dialogView.findViewById(R.id.text_message_log);
        textView.setText(getString(R.string.privacy_policy));
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.privacy_policy_label)
                .setIcon(R.mipmap.icon_deambulario)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, null)
                .create();
        dialog.show();
    }


    /**
     * Called when the Fragment is being destroyed. Avoids view binding memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}