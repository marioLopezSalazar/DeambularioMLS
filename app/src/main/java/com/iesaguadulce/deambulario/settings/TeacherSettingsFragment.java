package com.iesaguadulce.deambulario.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.TeacherActivity;
import com.iesaguadulce.deambulario.auth.AuthUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;

/**
 * Class for manage the teacher settings related with account, permissions, appearance, help & appInfo.
 *
 * @author Mario López Salazar
 */
public class TeacherSettingsFragment extends PreferenceFragmentCompat {

    /**
     * Creates the settings view and manages user interactions with it.
     *
     * @param savedInstanceState The previous state if the fragment is being re-created.
     * @param rootKey            The parent preference screen (i.e. null in this case).
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Perform Teacher Guide:
        performGuide();

        // Setting shared preferences file:
        getPreferenceManager().setSharedPreferencesName(Constants.PREF_NAME());
        // Inflating preferences XML file:
        setPreferencesFromResource(R.xml.fragment_preferences_teacher, rootKey);


        //==========================================================================================


        // CHANGE NAME AND PASSWORD (only when email-password user):
        EditTextPreference namePref = findPreference("pref_teacher_name");
        Preference passwordPref = findPreference("pref_teacher_change_password");

        if (!AuthUtils.isEmailUser()) {
            if (namePref != null) {
                namePref.setVisible(false);
            }
            if (passwordPref != null) {
                passwordPref.setVisible(false);
            }
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            // Change name setting:
            if (user != null && namePref != null) {
                namePref.setSummary(user.getDisplayName());
                namePref.setText(user.getDisplayName());
                namePref.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                    editText.setHint(R.string.full_name);
                    editText.selectAll();
                });
                namePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String newName = (String) newValue;
                    if (newName.isEmpty()) {
                        return false;
                    }
                    AuthUtils.updateProfileName(requireContext(), newName, namePref);
                    return true;
                });
            }

            // Change password setting:
            if (user != null && passwordPref != null) {
                passwordPref.setOnPreferenceClickListener(preference -> {
                    String email = user.getEmail();
                    if (email == null) {
                        return false;
                    }
                    AuthUtils.sendPasswordReset(requireContext());
                    return true;
                });
            }

        }


        // LOGOUT
        Preference logoutPref = findPreference("pref_teacher_logout");
        if (logoutPref != null) {
            logoutPref.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.pref_logout)
                        .setIcon(R.mipmap.icon_deambulario)
                        .setMessage(R.string.logout_confirmation_question)
                        .setPositiveButton(R.string.ok, (dialog, which) ->
                                AuthUtils.teacherLogout(requireActivity()))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            });
        }


        // DELETE ACCOUNT
        Preference deleteAccountPref = findPreference("pref_teacher_delete_account");
        if (deleteAccountPref != null) {
            deleteAccountPref.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.pref_delete_account)
                        .setIcon(R.mipmap.icon_deambulario)
                        .setMessage(R.string.delete_account_message)
                        .setPositiveButton(R.string.delete_account_definitively, (dialog, which) ->
                                AuthUtils.initiateAccountDeletion(requireActivity()))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            });
        }


        //==========================================================================================


        // PERMISSIONS
        Preference permissionsPref = findPreference("pref_permissions");
        if (permissionsPref != null) {
            permissionsPref.setOnPreferenceClickListener(preference -> {

                // Opening Android permissions manager:
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", requireActivity().getPackageName(), null));
                startActivity(intent);
                return true;
            });
        }


        //==========================================================================================


        // THEME (managed by TeacherActivity)
        Preference themePref = findPreference("pref_teacher_theme");
        if (themePref != null) {
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String themeValue = (String) newValue;
                ((TeacherActivity) requireActivity()).applyTheme(themeValue);
                return true;
            });
        }


        // LANGUAGE (en/es)
        Preference languagePref = findPreference("pref_teacher_language");
        if (languagePref != null) {
            languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String langCode = (String) newValue;
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode));
                return true;
            });
        }


        //==========================================================================================


        // INTERACTIVE GUIDE
        Preference guidePref = findPreference("pref_teacher_guide");
        if (guidePref != null) {
            guidePref.setOnPreferenceClickListener(preference -> {
                TeacherTourManager.setCurrentStep(TeacherTourManager.TourStep.STEP_1_WELCOME_AND_ROUTES_LIST);
                Navigation.findNavController(requireView()).popBackStack(R.id.route_list_fragment, false);
                return true;
            });
        }


        // FAQ
        Preference faqPref = findPreference("pref_teacher_faq");
        if (faqPref != null) {
            faqPref.setOnPreferenceClickListener(preference -> {
                Navigation.findNavController(requireView()).navigate(R.id.action_teacher_settings_to_teacher_faq);
                return true;
            });
        }


        //==========================================================================================


        // APP INFO
        Preference aboutPref = findPreference("pref_teacher_about");
        if (aboutPref != null) {
            aboutPref.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.about_deambulario)
                        .setIcon(R.mipmap.icon_deambulario)
                        .setMessage(R.string.developed_by_and_version)
                        .setPositiveButton(R.string.close, null)
                        .show();
                return true;
            });
        }
    }


    /**
     * Used to establish the toolbar title after an activity recreation.
     */
    @Override
    public void onResume() {
        super.onResume();
        GlobalUIViewModel globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);
        globalUIViewModel.setToolbarTitle(ContextCompat.getString(requireActivity(), R.string.profile));
    }


    /**
     * Performs step 23 of the Teacher guide.
     */
    private void performGuide() {

        // STEP 23 - Settings:
        if (TeacherTourManager.getCurrentStep() == TeacherTourManager.TourStep.STEP_23_SETTINGS) {
            // Getting the 'Settings' bottom navigation button:
            View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            bottomNav.post(() -> {
                View targetTab = bottomNav.findViewById(R.id.teacher_settings_fragment);
                if (targetTab != null) {
                    TeacherTourManager.checkSettingsTour((TeacherActivity) requireActivity(), targetTab, () ->
                            Navigation.findNavController(requireView()).popBackStack(R.id.route_list_fragment, false)
                    );
                }
            });
        }
    }
}