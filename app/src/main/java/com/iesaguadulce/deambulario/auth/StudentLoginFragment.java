package com.iesaguadulce.deambulario.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.FragmentAuthStudentBinding;
import com.iesaguadulce.deambulario.utils.UIUtils;

import java.util.List;

/**
 * Fragment to manage the Student login process.
 * It allows to enter the Session PIN and launches the anonymous login on Firebase.
 *
 * @author Mario López Salazar
 */
public class StudentLoginFragment extends Fragment {

    /*
     * Binding object to access the views.
     */
    private FragmentAuthStudentBinding binding;

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
        binding = FragmentAuthStudentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Initializes the FirebaseAuth object and sets up the end-of-form actions.
     * This method is called after the view has been created.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initializing FirebaseAuth:
        auth = FirebaseAuth.getInstance();

        // Setting on the end-of-form actions:
        UIUtils.setEndFormOnClickListener(
                requireActivity(),
                binding.nickTextField,
                binding.btnEnterSession,
                v -> {

                    // Validating the PIN (XML layout enforces only numbers entering):
                    int pin = UIUtils.validateNotEmptyNatural(
                            binding.pinInputLayout,
                            getResources().getString(R.string.sessionPin));

                    String nick = UIUtils.validateNotEmpty(
                            binding.nickInputLayout,
                            getResources().getString(R.string.your_nick));

                    // Launching Anonymous login:
                    loginAnonymously(pin, nick);
                });
    }


    /**
     * Launches the anonymous authentication for the Student, using the introduced PIN.
     *
     * @param pin  The PIN introduced by the Student.
     * @param nick The nick introduced by the Student.
     */
    private void loginAnonymously(int pin, String nick) {

        // Performing the UI to wait until the auth process will finish:
        AuthUtils.setInProgress(
                true,
                binding.progressBar,
                List.of(binding.btnEnterSession));

        // Launching anonymous FirebaseAuth login:
        auth.signInAnonymously()
                .addOnCompleteListener(requireActivity(), task -> {

                    // Login successful:
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        ((AuthActivity) requireActivity()).navigateToStudentActivity(
                                task.getResult().getUser().getUid(), pin, nick);

                    }
                    // Login failed:
                    else {
                        AuthUtils.setInProgress(
                                false,
                                binding.progressBar,
                                List.of(binding.btnEnterSession));
                        Toast.makeText(requireContext(), AuthUtils.getAuthErrorMessage(task.getException()), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /**
     * Called when the Fragment is being destroyed.
     * Avoids view binding memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}