package com.iesaguadulce.deambulario.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.RouteViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * Tools class containing reusable methods related with authentication.
 *
 * @author Mario López Salazar
 */
public abstract class AuthUtils {


    /**
     * Validates if the user has introduced some EMAIL on a TextInputLayout (Material Design).
     *
     * @param textInputLayout The TextInputLayout wrapping the EditText.
     * @param errorText       The error message.
     * @return The content on the EditText if is an EMAIL, null otherwise.
     */
    @Nullable
    public static String validateEmail(@NonNull TextInputLayout textInputLayout, @NonNull String errorText) {

        String text = "";
        if (textInputLayout.getEditText() != null) {
            text = String.valueOf(textInputLayout.getEditText().getText()).trim();
        }

        if (TextUtils.isEmpty(text) || !Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(errorText);
            return null;
        } else {
            textInputLayout.setErrorEnabled(false);
            textInputLayout.setError(null);
            return text;
        }
    }


    /**
     * Gets a descriptive message for users from FirebaseAuth exceptions.
     *
     * @param exception FirebaseAuth exception.
     * @return Error message for users.
     */
    @Contract(pure = true)
    static int getAuthErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return R.string.error_invalid_credentials;
        } else if (exception instanceof FirebaseAuthInvalidUserException) {
            return R.string.error_user_not_found;
        } else if (exception instanceof FirebaseAuthUserCollisionException) {
            return R.string.error_email_already_in_use;
        } else if (exception instanceof FirebaseNetworkException) {
            return R.string.error_network;
        } else {
            return R.string.authentication_failed;
        }
    }


    /**
     * Allows to perform the Auth fragments UI where a long-time task is executing.
     * Shows/hides a ProgressIndicator and disables/enables a list of buttons.
     *
     * @param inProgress        True to indicate the start of the long-time task.
     * @param progressIndicator The ProgressIndicator to be shown/hidden.
     * @param buttons           The buttons to be disabled/enabled.
     */
    public static void setInProgress(boolean inProgress, @NonNull CircularProgressIndicator progressIndicator, @NonNull List<Button> buttons) {
        progressIndicator.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        for (Button b : buttons) {
            b.setEnabled(!inProgress);
        }
    }


    /**
     * Allows to know if the current user was logged-in using email & password.
     *
     * @return True if the current user was logged-in using email & password, false otherwise or when error.
     */
    public static boolean isEmailUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return false;
        }

        for (int i = 0; i < user.getProviderData().size(); i++) {
            if (user.getProviderData().get(i).getProviderId().equals(EmailAuthProvider.PROVIDER_ID)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Allows to change the username of a teacher logged-in with user & password.
     * When done, injects the new username on an EditTextPreference.
     *
     * @param context  The context in which this method is called.
     * @param newName  The new username.
     * @param namePref The EditTextPreference where inject the new username.
     */
    public static void updateProfileName(Context context, String newName, EditTextPreference namePref) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || newName.isEmpty()) {
            Toast.makeText(context, R.string.username_updating_error, Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(newName).build();
        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, R.string.username_updated, Toast.LENGTH_SHORT).show();
                namePref.setSummary(newName);
            } else {
                Toast.makeText(context, R.string.username_updating_error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Allows to change the password of a teacher logged-in with user & password.
     *
     * @param context The context in which this method is called.
     */
    public static void sendPasswordReset(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, R.string.password_updating_error, Toast.LENGTH_SHORT).show();
            return;
        }
        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(context, R.string.password_updating_error, Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.pref_change_password)
                        .setMessage(context.getString(R.string.change_password_message, email))
                        .setIcon(R.mipmap.icon_deambulario)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            } else {
                Toast.makeText(context, R.string.password_updating_error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Deletes the current student user from Firebase Authentication.
     *
     * @param callback Callback to notify when the operation is complete.
     */
    public static void deleteStudentUser(ReposVoidCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(task.getException());
                }
            });
        } else {
            callback.onSuccess();
        }
    }


    /**
     * Allows teacher user to logout.
     *
     * @param activity The activity in which this method is called.
     */
    public static void teacherLogout(Activity activity) {

        // Logging out on Firebase:
        FirebaseAuth.getInstance().signOut();

        // Cleaning the CredentialManager:
        CredentialManager.create(activity).clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(Void result) {
                        // Navigating to AuthActivity:
                        kickToAuthActivity(activity);
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        // Navigating to AuthActivity:
                        kickToAuthActivity(activity);
                    }
                }
        );
    }


    /**
     * Allows to initiate the teacher account deletion.
     * Inquires the login mode (Google or email) and launches the corresponding inner method.
     *
     * @param activity The Teacher activity.
     */
    public static void initiateAccountDeletion(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        if (isEmailUser()) {
            showPasswordDialogForDeletion(activity);
        } else {
            reauthenticateWithGoogle(activity);
        }
    }


    /**
     * Inner method to start the deletion of a teacher logged with email.
     * Requires the password to reauthenticate before launching user deletion.
     *
     * @param activity The Teacher activity.
     */
    private static void showPasswordDialogForDeletion(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return;
        }

        // Creating the dialog:
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_password_verification, null);
        TextInputLayout inputLayout = view.findViewById(R.id.text_input_layout_password);
        TextInputEditText input = view.findViewById(R.id.edit_text_password);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.security_verification)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(R.string.type_your_password_for_security)
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
        input.requestFocus();

        // Performing password getting, reauthentication and deletion:
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = UIUtils.validateNotEmpty(inputLayout, activity.getString(R.string.empty_password));
            if (password != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), Objects.requireNonNull(password));
                performReauthenticationAndDeletion((FragmentActivity) activity, credential);
                dialog.dismiss();
            }
        });
    }

    /**
     * Inner method to start the deletion of a teacher logged with Google.
     *
     * @param activity The Teacher activity.
     */
    private static void reauthenticateWithGoogle(@NotNull Activity activity) {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build();
        CredentialManager.create(activity).getCredentialAsync(activity, request, new CancellationSignal(), ContextCompat.getMainExecutor(activity), new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            try {
                                String idToken = GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
                                AuthCredential authCredential = GoogleAuthProvider.getCredential(idToken, null);
                                performReauthenticationAndDeletion((FragmentActivity) activity, authCredential);
                            } catch (Exception e) {
                                Toast.makeText(activity, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Toast.makeText(activity, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }


    /**
     * Performs the user reauthentication and deletion.
     * Includes the deletion of all routes and sessions of the teacher.
     *
     * @param activity   The Teacher activity.
     * @param credential The FirebaseAuth user credential.
     */
    private static void performReauthenticationAndDeletion(FragmentActivity activity, @NotNull AuthCredential credential) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(activity, R.string.error_when_account_deletion, Toast.LENGTH_SHORT).show();
            return;
        }

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                // Getting data ViewModels:
                RouteViewModel routeViewModel = new ViewModelProvider(activity).get(RouteViewModel.class);
                SessionViewModel sessionViewModel = new ViewModelProvider(activity).get(SessionViewModel.class);
                GlobalUIViewModel globalUIViewModel = new ViewModelProvider(activity).get(GlobalUIViewModel.class);
                globalUIViewModel.showLoading();

                // Deleting user routes and sessions:
                routeViewModel.deleteAllRoutes();
                sessionViewModel.deleteAllSessions();

                // Locking UI while data deletion:
                Observer<Boolean> loadingObserver = isLoading ->
                        UIUtils.updateLoadingState(
                                Arrays.asList(routeViewModel, sessionViewModel),
                                globalUIViewModel);
                routeViewModel.isLoading().observe(activity, loadingObserver);
                sessionViewModel.isLoading().observe(activity, loadingObserver);

                // Performing actions when data deletion will end:
                globalUIViewModel.getIsLoading().observe(activity, new Observer<>() {
                    @Override
                    public void onChanged(Boolean loading) {
                        if (Boolean.FALSE.equals(loading)) {

                            // Avoiding double user deletion:
                            globalUIViewModel.getIsLoading().removeObserver(this);

                            // Deleting the user:
                            user.delete().addOnCompleteListener(deleteTask -> {
                                if (deleteTask.isSuccessful()) {
                                    Toast.makeText(activity, R.string.deleted_account, Toast.LENGTH_SHORT).show();
                                    teacherLogout(activity);
                                } else {
                                    Toast.makeText(activity, R.string.error_when_account_deletion, Toast.LENGTH_SHORT).show();
                                    globalUIViewModel.hideLoading();
                                }
                            });
                        }
                    }
                });
            } else {
                Toast.makeText(activity, R.string.error_when_account_deletion, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Used to navigate back from the TeacherActivity to the AuthActivity.
     *
     * @param activity The running TeacherActivity.
     */
    private static void kickToAuthActivity(Activity activity) {
        Intent intent = new Intent(activity, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

}
