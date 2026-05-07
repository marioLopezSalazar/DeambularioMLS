package com.iesaguadulce.deambulario.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.FragmentAuthTeacherEmailBinding;
import com.iesaguadulce.deambulario.utils.UIUtils;

import java.util.List;


/**
 * Fragment to manage the Teacher login process using Email and Password.
 *
 * @author Mario López Salazar
 */
public class TeacherLoginEmailFragment extends Fragment {

    /*
     * Binding object to access the views.
     */
    private FragmentAuthTeacherEmailBinding binding;

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
        binding = FragmentAuthTeacherEmailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Initializes the FirebaseAuth object and sets up button listeners.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        binding.btnForgottenPassword.setOnClickListener(this::forgottenPassword);
        binding.btnGoToRegister.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_teacherLoginEmailFragment_to_teacherLoginEmailRegisterFragment)
        );
        UIUtils.setEndFormOnClickListener(
                requireActivity(),
                binding.password,
                binding.btnLogin,
                this::signInWithEmail
        );
    }



    /**
     * Validates the credentials typed by teacher and launches the login attempt.
     *
     * @param v The current view.
     */
    private void signInWithEmail(View v) {

        // Validating the typed credentials:
        String email = AuthUtils.validateEmail(
                binding.emailInputLayout,
                getResources().getString(R.string.email));
        String password = UIUtils.validateMinLength(
                binding.passwordInputLayout,
                getResources().getString(R.string.password_minimum_6),
                6);

        // Launching teacher login:
        if (email != null && password != null) {
            loginTeacher(email, password);
        }
    }


    /**
     * Authenticates the user with Firebase using Email and Password.
     * On successful authentication, navigates to the Teacher main activity.
     *
     * @param email    User email.
     * @param password User password.
     */
    private void loginTeacher(String email, String password) {

        // Performing the UI to wait until the auth process will finish:
        AuthUtils.setInProgress(
                true,
                binding.progressBar,
                List.of(binding.btnLogin, binding.btnForgottenPassword, binding.btnGoToRegister));

        // Launching the authentication attempt:
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task -> {

            // Login successful:
            if (task.isSuccessful() && task.getResult().getUser() != null) {
                // Navigating to the Teacher main activity:
                ((AuthActivity) requireActivity()).navigateToTeacherActivity(
                        task.getResult().getUser().getUid()
                );

            }
            // Login failed:
            else {
                AuthUtils.setInProgress(
                        false,
                        binding.progressBar,
                        List.of(binding.btnLogin, binding.btnForgottenPassword, binding.btnGoToRegister));
                Toast.makeText(requireContext(), AuthUtils.getAuthErrorMessage(task.getException()), Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Allows user to be sent a recovery password email.
     * @param view The current view.
     */
    private void forgottenPassword(View view) {

        // Validating the typed email:
        String email = AuthUtils.validateEmail(
                binding.emailInputLayout,
                getResources().getString(R.string.email));

        if(email != null) {
            // Performing the UI to wait until the auth process will finish:
            AuthUtils.setInProgress(
                    true,
                    binding.progressBar,
                    List.of(binding.btnLogin, binding.btnForgottenPassword, binding.btnGoToRegister));

            //Attempting to send the recovery email:
            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                AuthUtils.setInProgress(
                        false,
                        binding.progressBar,
                        List.of(binding.btnLogin, binding.btnForgottenPassword, binding.btnGoToRegister));
                if (task.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.recoveryPasswordSent, Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(requireContext(), AuthUtils.getAuthErrorMessage(task.getException()), Toast.LENGTH_SHORT).show();
                }
            });
        }
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