package com.iesaguadulce.deambulario.settings;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;

/**
 * Class for manage the student settings related with sound volume, look & feel, help & appInfo.
 *
 * @author Mario López Salazar.
 */
public class StudentSettingsFragment extends PreferenceFragmentCompat {

    /*
     * Variables to link the 'volume' preference with the system volume.
     */
    private AudioManager audioManager;
    private VolumeObserver volumeObserver;


    /**
     * Creates the settings view and manages user interactions with it.
     *
     * @param savedInstanceState The previous state if the fragment is being re-created.
     * @param rootKey            The parent preference screen (i.e. null in this case).
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Setting shared preferences file:
        getPreferenceManager().setSharedPreferencesName(Constants.PREF_NAME());
        // Inflating preferences XML file:
        setPreferencesFromResource(R.xml.fragment_preferences_student, rootKey);


        //==========================================================================================


        // SOUNDS VOLUME
        SeekBarPreference volumePref = findPreference("pref_student_volume");
        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        if (volumePref != null && audioManager != null) {

            // Getting system volume values:
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumePref.setMin(0);
            volumePref.setMax(maxVolume);
            volumePref.setValue(currentVolume);

            // Performing new pref value:
            volumePref.setOnPreferenceChangeListener((preference, newValue) -> {
                int volume = (Integer) newValue;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                return true;
            });
        }


        //==========================================================================================


        // FONT SIZE (managed by StudentActivity)
        Preference fontSizePref = findPreference("pref_student_font_size");
        if (fontSizePref != null) {
            fontSizePref.setOnPreferenceChangeListener((preference, newValue) -> {
                requireActivity().recreate();
                return true;
            });
        }


        // COLOR PALETTE (managed by StudentActivity)
        Preference palettePref = findPreference("pref_student_palette");
        if (palettePref != null) {
            palettePref.setOnPreferenceChangeListener((preference, newValue) -> {
                requireActivity().recreate();
                return true;
            });
        }


        // LANGUAGE (en/es)
        Preference languagePref = findPreference("pref_student_language");
        if (languagePref != null) {
            languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String langCode = (String) newValue;
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode));
                return true;
            });
        }


        //==========================================================================================


        // INTERACTIVE GUIDE
        Preference guidePref = findPreference("pref_student_guide");
        if (guidePref != null) {
            guidePref.setOnPreferenceClickListener(preference -> {
                StudentTourManager.setCurrentStep(StudentTourManager.TourStep.STEP_1_NEXT_MILESTONE_INDICATOR);
                Navigation.findNavController(requireView()).popBackStack(R.id.student_play_fragment, false);
                return true;
            });
        }


        // FAQ
        Preference faqPref = findPreference("pref_student_faq");
        if (faqPref != null) {
            faqPref.setOnPreferenceClickListener(preference -> {
                Navigation.findNavController(requireView()).navigate(R.id.action_student_settings_to_student_faq);
                return true;
            });
        }


        //==========================================================================================


        // APP INFO
        Preference aboutPref = findPreference("pref_student_about");
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
     * Used to update the volume seek bar with the system volume,
     * and establish the toolbar title after an activity recreation.
     */
    @Override
    public void onResume() {
        super.onResume();

        // Updating volume preference according to the system volume changes:
        if (audioManager != null) {
            SeekBarPreference volumePref = findPreference("pref_student_volume");
            if (volumePref != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                volumePref.setValue(currentVolume);
            }
        }

        // Starting listening system volume changes:
        if (volumeObserver == null) {
            volumeObserver = new VolumeObserver(new Handler(Looper.getMainLooper()));
        }
        requireContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver);

        // Establishing toolbar title:
        GlobalUIViewModel globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);
        globalUIViewModel.setToolbarTitle(ContextCompat.getString(requireActivity(), R.string.profile));
    }


    /**
     * Used to stop listening system volume changes.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (volumeObserver != null) {
            requireContext().getContentResolver().unregisterContentObserver(volumeObserver);
        }
    }


    /**
     * Class to observe the system volume changes.
     *
     * @author Mario López Salazar.
     */
    private class VolumeObserver extends ContentObserver {

        /**
         * Creates a new VolumeObserver.
         * @param handler Handler to repeat action.
         */
        public VolumeObserver(Handler handler) {
            super(handler);
        }

        /**
         * Called when system volume changes.
         *
         * @param selfChange True if this is a self-change notification.
         */
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (audioManager != null) {
                SeekBarPreference volumePref = findPreference("pref_student_volume");
                if (volumePref != null) {
                    // Getting system volume:
                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                    // Updating preference, when changes:
                    if (volumePref.getValue() != currentVolume) {
                        volumePref.setValue(currentVolume);
                    }
                }
            }
        }
    }
}