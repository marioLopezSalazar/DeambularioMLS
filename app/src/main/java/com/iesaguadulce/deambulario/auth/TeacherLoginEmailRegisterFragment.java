package com.iesaguadulce.deambulario.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.FragmentAuthTeacherEmailRegisterBinding;
import com.iesaguadulce.deambulario.utils.UIUtils;

import java.util.List;


/**
 * Fragment to manage the Teacher sign-up process using Email and Password.
 *
 * @author Mario López Salazar
 */
public class TeacherLoginEmailRegisterFragment extends Fragment {

    /*
     * Binding object to access the views.
     */
    private FragmentAuthTeacherEmailRegisterBinding binding;

    /*
     * Object to manage the Firebase Authentication.
     */
    private FirebaseAuth auth;


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
        binding = FragmentAuthTeacherEmailRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Initializes the FirebaseAuth object and sets up button listener.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        UIUtils.setEndFormOnClickListener(
                requireActivity(),
                binding.passwordRegister,
                binding.btnRegister,
                this::signUpWithEmail);
    }


    /**
     * Validates the credentials typed by teacher and launches the sign-up and login attempt.
     *
     * @param v The current view.
     */
    private void signUpWithEmail(View v) {

        // Validating the typed credentials:
        String name = UIUtils.validateNotEmpty(
                binding.nameInputLayout,
                getResources().getString(R.string.full_name));
        String email = AuthUtils.validateEmail(
                binding.emailRegisterInputLayout,
                getResources().getString(R.string.email));
        String password = UIUtils.validateMinLength(
                binding.passwordRegisterInputLayout,
                getResources().getString(R.string.password_minimum_6),
                6);

        // Launching teacher sign-up:
        if (name != null && email != null && password != null) {
            registerTeacher(name, email, password);
        }
    }

    /**
     * Sign-up and sign-in the user with Firebase using Email and Password.
     * On successful authentication, navigates to the Teacher main activity.
     *
     * @param name     User name.
     * @param email    User email.
     * @param password User password.
     */
    private void registerTeacher(String name, String email, String password) {

        // Performing the UI to wait until the auth process will finish:
        AuthUtils.setInProgress(true, binding.progressBar, List.of(binding.btnRegister));

        // Launching the sign-up attempt:
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), taskSignUp -> {

            // Sign-up and login successful:
            if (taskSignUp.isSuccessful() && taskSignUp.getResult().getUser() != null) {

                // Storing the teacher name on FirebaseAuth:
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();
                taskSignUp.getResult().getUser().updateProfile(profileUpdates)
                        .addOnCompleteListener(taskFillName -> {
                            if (taskFillName.isSuccessful()) {

                                // Navigating to the teacher main activity:
                                ((AuthActivity) requireActivity()).navigateToTeacherActivity(
                                        taskSignUp.getResult().getUser().getUid());
                            }
                        });

            }
            // Sign-up and login failed:
            else {
                AuthUtils.setInProgress(false, binding.progressBar, List.of(binding.btnRegister));
                Toast.makeText(requireContext(), AuthUtils.getAuthErrorMessage(taskSignUp.getException()), Toast.LENGTH_SHORT).show();
            }
        });
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