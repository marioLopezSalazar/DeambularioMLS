package com.iesaguadulce.deambulario.utils;

import android.app.Activity;
import android.content.Context;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.iesaguadulce.deambulario.viewmodel.DataViewModel;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * Tools class containing methods for validate UI fields, perform the end of a form behavior,
 * locking UI while data loading, playing sounds and an enum to perform recyclerview behavior.
 *
 * @author Mario López Salazar.
 */
public abstract class UIUtils {


    /**
     * Validates if the user has introduced some text on a TextInputLayout (Material Design).
     *
     * @param textInputLayout The TextInputLayout wrapping the EditText.
     * @param errorText       The error message.
     * @return The content on the EditText if not empty, null otherwise.
     */
    public static String validateNotEmpty(@NonNull TextInputLayout textInputLayout, @NonNull String errorText) {

        String text = "";
        if (textInputLayout.getEditText() != null) {
            text = String.valueOf(textInputLayout.getEditText().getText()).trim();
        }

        if (TextUtils.isEmpty(text)) {
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
     * Validates if the user has introduced a minimum-length text on a TextInputLayout (Material Design).
     *
     * @param textInputLayout The TextInputLayout wrapping the EditText.
     * @param errorText       The error message.
     * @param length          The minimum length allowed.
     * @return The content on the EditText if it has the minimum length, null otherwise.
     */
    public static String validateMinLength(@NonNull TextInputLayout textInputLayout, @NonNull String errorText, int length) {

        String text = "";
        if (textInputLayout.getEditText() != null) {
            text = String.valueOf(textInputLayout.getEditText().getText()).trim();
        }

        if (TextUtils.isEmpty(text) || text.length() < length) {
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
     * Validates if the user has introduced non-empty natural number on a TextInputLayout (Material Design).
     *
     * @param textInputLayout The TextInputLayout wrapping the EditText.
     * @param errorText       The error message.
     * @return The natural number typed on the EditText if not empty, -1 otherwise.
     */
    public static int validateNotEmptyNatural(@NonNull TextInputLayout textInputLayout, @NonNull String errorText) {

        String text = "";
        if (textInputLayout.getEditText() != null) {
            text = String.valueOf(textInputLayout.getEditText().getText()).trim();
        }

        if (TextUtils.isEmpty(text)) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(errorText);
            return -1;
        } else
            try {
                int number = Integer.parseInt(text);
                if (number > 0) {
                    textInputLayout.setErrorEnabled(false);
                    textInputLayout.setError(null);
                    return number;
                } else {
                    textInputLayout.setErrorEnabled(true);
                    textInputLayout.setError(errorText);
                    return -1;
                }
            } catch (NumberFormatException e) {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(errorText);
                return -1;
            }
    }


    /**
     * Scrolls a TextView with ScrollingMovementMethod to the bottom.
     */
    public static void scrollLogToBottom(@NonNull ScrollView scrollView) {
        scrollView.postDelayed(() -> scrollView.fullScroll(View.FOCUS_DOWN), 1000);
    }


    //==============================================================================================


    /**
     * Hides the keyboard.
     *
     * @param activity Current activity.
     */
    public static void hideKeyboard(@NonNull Activity activity) {
        // Getting the focused view (or a default view, if none has the focus):
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }

        // Hiding the keyboard:
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    /**
     * Used to set up the final EditText and Button on a form.
     * When user pushes the Enter button on the keyboard, this method hides the keyboard and
     * launches the listener. Also, if the user pushes the button.
     *
     * @param activity Current Activity.
     * @param editText The final EditText on the form.
     * @param button   The final Button on the form.
     * @param listener The OnClick Listener which has to be launched.
     */
    public static void setEndFormOnClickListener(@NonNull Activity activity, EditText editText, Button button, View.OnClickListener listener) {

        // Setting up the EditText (if not null):
        if (editText != null) {
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard(activity);
                    if (listener != null) {
                        listener.onClick(v);
                    }
                }
                return true;
            });
        }
        // Setting up the Button:
        if (button != null) {
            button.setOnClickListener(v -> {
                hideKeyboard(activity);
                if (listener != null) {
                    listener.onClick(v);
                }
            });
        }
    }


    /**
     * Automatically types a String on a TextInputEditText, letter by letter.
     * @param textViews         The text views where type the text.
     * @param texts             The texts to type.
     * @param onTypingFinished  Actions to do when the typing finishes.
     */
    public static void animateTyping(TextInputEditText[] textViews, String[] texts, Runnable onTypingFinished) {
        if (textViews == null || texts == null || textViews.length != texts.length || textViews.length == 0) {
            if (onTypingFinished != null){
                onTypingFinished.run();
            }
            return;
        }

        // Performing timer for the next-letter typing:
        final long typeDelay = 70;
        final long fieldDelay = 300;
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            int viewIndex = 0;
            int charIndex = 0;

            @Override
            public void run() {
                String currentText = texts[viewIndex];
                TextInputEditText currentView = textViews[viewIndex];
                if (currentView == null || !currentView.isAttachedToWindow()) {
                    return;
                }

                // Flushing text view:
                if (charIndex == 0) {
                    currentView.setText("");
                }

                // Typing on current field:
                if (charIndex < currentText.length()) {
                    currentView.append(String.valueOf(currentText.charAt(charIndex)));
                    charIndex++;
                    handler.postDelayed(this, typeDelay);
                }
                // Current field ended:
                else {
                    viewIndex++;
                    charIndex = 0;
                    if (viewIndex < textViews.length) {
                        textViews[viewIndex].requestFocus();
                        handler.postDelayed(this, fieldDelay);
                    }
                    // All fields filled:
                    else {
                        if (onTypingFinished != null) {
                            onTypingFinished.run();
                        }
                    }
                }
            }
        });
    }


    //==============================================================================================

    /**
     * Manages the lock-unlock of the UI when data loading.
     *
     * @param viewModels        The list of DataViewModels whose loading status must be checked.
     * @param globalUIViewModel The ViewModel controlling the UI appearance.
     */
    public static void updateLoadingState(@NotNull List<DataViewModel> viewModels, @NotNull GlobalUIViewModel globalUIViewModel) {

        boolean lockUI = false;

        for (int i = 0; i < viewModels.size() && !lockUI; i++) {
            DataViewModel viewModel = viewModels.get(i);

            if (viewModel.isLoading() != null && Boolean.TRUE.equals(viewModel.isLoading().getValue())) {
                lockUI = true;
            }
        }

        if (lockUI) {
            globalUIViewModel.showLoading();
        } else {
            globalUIViewModel.hideLoading();
        }
    }


    //==============================================================================================


    /*
     * SoundPool to manage and play audio resources.
     */
    private static SoundPool soundPool;
    /*
     * Map of integers-integers, to stand a sounds cache.
     */
    private static SparseIntArray soundCache;


    /**
     * Plays a short sound.
     *
     * @param context  The context in which this method is called.
     * @param rawResId The ID of the sound resource.
     */
    public static void playShortSound(Context context, int rawResId) {
        // Initializing the pool and the cache, if nulls:
        if (soundPool == null) {
            soundPool = new SoundPool.Builder().setMaxStreams(3).build();
            soundCache = new SparseIntArray();
        }

        // Getting the sound from cache, if was:
        int soundId = soundCache.get(rawResId);

        // When the sound wasn't on cache:
        if (soundId == 0) {
            // Performing actions to do when the sound will be loaded:
            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status == 0) {
                    // Playing sound:
                    pool.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f);
                }
            });

            // Launching the sound loading:
            soundId = soundPool.load(context.getApplicationContext(), rawResId, 1);
            soundCache.put(rawResId, soundId);
        }
        // When the sound was on cache:
        else {
            // Playing sound directly:
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }


    //==============================================================================================


    /**
     * Defines the visual and functional mode of the adapter items.
     */
    public enum AdapterMode {

        /**
         * Read-only list.
         */
        READ_ONLY,

        /**
         * Reorderable list.
         */
        REORDERABLE
    }


    /**
     * Field separator on link contents, for separate the title and the URL on Activity object.
     */
    public static final String LINK_SEPARATOR = "<><>";
}
