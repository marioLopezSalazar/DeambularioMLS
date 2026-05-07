package com.iesaguadulce.deambulario.auth;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.FragmentAuthTeacherBinding;
import java.util.List;


/**
 * Fragment to manage the Teacher login process, allowing either Google or Email options.
 * The Google login is managed on this fragment.
 * When Email login option is selected, it navigates to TeacherLoginEmailFragment.
 *
 * @author Mario López Salazar
 */
public class TeacherLoginFragment extends Fragment {

    /*
     * Binding object to access the views.
     */
    private FragmentAuthTeacherBinding binding;

    /*
     * Objects to manage the Firebase Authentication.
     */
    private FirebaseAuth auth;
    private CredentialManager credentialManager;


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
        binding = FragmentAuthTeacherBinding.inflate(inflater, container, false);
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
        credentialManager = CredentialManager.create(requireContext());

        binding.btnEmailSignIn.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_teacherLoginFragment_to_teacherLoginEmailFragment));
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }


    /**
     * Initiates the Google Sign-In flow.
     * It launches the native Android panel to allow the user to select the Google account.
     */
    private void signInWithGoogle() {
        // Setting up the credential request:
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Launching the native Android panel to select the Google account:
        credentialManager.getCredentialAsync(
                requireActivity(),
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(requireContext()),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // Managing the Google authentication result:
                        handleSignInResult(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Toast.makeText(requireContext(), R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }


    /**
     * Processes the Google Sign-In credential to extract its ID token.
     * On success, delegates the token to FirebaseAuth with Google.
     *
     * @param credential The credential obtained from the Android Credential Manager.
     */
    private void handleSignInResult(Credential credential) {
        // Getting the Google authentication token:
        if (credential instanceof CustomCredential &&
                credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            try {
                String idToken = GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();

                // Launching FirebaseAuth with Google:
                firebaseAuthWithGoogle(idToken);

            } catch (Exception e) {
                Toast.makeText(requireContext(), R.string.authentication_failed, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), R.string.authentication_failed, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Authenticates the user with Firebase using the provided Google ID token.
     * On successful authentication, navigates to the Teacher main activity.
     *
     * @param idToken The Google ID token obtained from the Credential Manager.
     */
    private void firebaseAuthWithGoogle(String idToken) {

        // Performing the UI to wait until the auth process will finish:
        AuthUtils.setInProgress(
                true,
                binding.progressBar,
                List.of(binding.btnEmailSignIn, binding.btnGoogleSignIn));

        // Launching the authentication attempt:
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(requireActivity(), task -> {

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
                        List.of(binding.btnEmailSignIn, binding.btnGoogleSignIn));
                Toast.makeText(requireContext(), AuthUtils.getAuthErrorMessage(task.getException()), Toast.LENGTH_SHORT).show();
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

