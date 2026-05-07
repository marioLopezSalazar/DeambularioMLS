package com.iesaguadulce.deambulario.playing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.adapters.LearningListAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentStudentMilestoneBinding;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Answer;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.utils.FilesUtils;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment to manage the displaying of a milestone for the student, including
 * educative contents and interactive activities.
 *
 * @author Mario López Salazar
 */
public class StudentMilestoneFragment extends Fragment {

    /*
     * ViewBinding to manage view components.
     */
    private FragmentStudentMilestoneBinding binding;

    /*
     * ViewModel to manage data.
     */
    private MilestoneViewModel milestoneViewModel;
    private SessionViewModel sessionViewModel;
    private Student student;
    private Activity activity;

    /*
     * ViewModel to manage the UI appearance.
     */
    GlobalUIViewModel globalUIViewModel;

    /*
     * Adapter for the RecyclerView displaying contents and activities.
     */
    private LearningListAdapter adapter;

    /*
     * Launchers for the native Android multimedia.
     */
    private ActivityResultLauncher<PickVisualMediaRequest> mediaPickerLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<Uri> captureVideoLauncher;

    /*
     * Stores the temporary Uri where the camera will save the captured media.
     */
    private Uri currentCameraUri;

    /*
     * To track if there is a pending multimedia saving operation, and if it can be done now (used to wait media operation finish).
     */
    private boolean userMustWaitToSave = false;
    private final List<Answer> pendingMediaAnswers = new ArrayList<>();


    /**
     * Inflates the fragment view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentMilestoneBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Initializes the setting up of the view, adapters, and media launchers.
     *
     * @param view               The View returned by the onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Getting the ViewModel:
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);

        // Setting up ActivityResultLaunchers for media processing:
        setupMediaLaunchers();

        // Setting up the RecyclerView and Adapter:
        setupRecyclerView();

        // Observing ViewModel data to update the UI:
        observeViewModel();

        // Registering results listener for the test bottom sheet fragment:
        getChildFragmentManager().setFragmentResultListener(StudentMilestoneBottomTestFragment.REQUEST_KEY, getViewLifecycleOwner(),
                (requestKey, bundle) -> {

                    // Getting the bottom sheet result data:
                    String activityId = bundle.getString(StudentMilestoneBottomTestFragment.BUNDLE_KEY_ACTIVITY_ID);
                    String selectedOption = bundle.getString(StudentMilestoneBottomTestFragment.BUNDLE_KEY_SELECTED_OPTION);

                    // Saving the answer:
                    if (activityId != null && selectedOption != null) {
                        saveOrUpdateAnswer(activityId, selectedOption);
                    }
                }
        );

        // Managing back button to prevent leaving while uploading:
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (userMustWaitToSave) {
                    Snackbar.make(binding.getRoot(), R.string.pending_multimedia_uploading, Snackbar.LENGTH_SHORT).show();
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }


    /**
     * Registers the required ActivityResultLaunchers for gallery and camera operations.
     */
    private void setupMediaLaunchers() {

        // Launcher for gallery:
        mediaPickerLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null && activity != null) {
                processStudentMedia(uri, activity);
            }
        });

        // Launchers for camera:
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && currentCameraUri != null && activity != null) {
                processStudentMedia(currentCameraUri, activity);
            }
        });
        captureVideoLauncher = registerForActivityResult(new ActivityResultContracts.CaptureVideo(), success -> {
            if (success && currentCameraUri != null && activity != null) {
                processStudentMedia(currentCameraUri, activity);
            }
        });
    }


    /**
     * Configures the RecyclerView and initializes the LearningListAdapter.
     */
    private void setupRecyclerView() {

        // Creating the adapter and setting the listener:
        adapter = new LearningListAdapter(new LearningListAdapter.OnActivityClickListener() {
            @Override
            public void onActivityClick(Activity activity) {
                handleActivityClick(activity);
            }

            @Override
            public void onLinkClick(String url) {
                openWebLink(url);
            }
        });

        // Attaching the listener to the recyclerview:
        binding.recyclerviewMilestoneItems.setAdapter(adapter);
    }


    /**
     * Observes data from the ViewModel and pushes updates to the adapter.
     */
    private void observeViewModel() {

        // Observing milestone and student data:
        milestoneViewModel.getSelectedMilestone().observe(getViewLifecycleOwner(), milestone -> {
            if (milestone != null && student != null) {
                globalUIViewModel.setToolbarTitle(milestone.getName());
                updateEmptyStateVisibility(milestone);
                adapter.submitData(milestone.getContents(), milestone.getActivities(), student.getAnswers());
            }
        });
        sessionViewModel.getCurrentStudent().observe(getViewLifecycleOwner(), currentStudent -> {
            this.student = currentStudent;
            Milestone currentMilestone = milestoneViewModel.getSelectedMilestone().getValue();
            if (currentMilestone != null) {
                globalUIViewModel.setToolbarTitle(currentMilestone.getName());
                updateEmptyStateVisibility(currentMilestone);
                adapter.submitData(currentMilestone.getContents(), currentMilestone.getActivities(), student.getAnswers());
            }
        });

        // Managing the UI lock-unlock during data operations:
        Observer<Boolean> loadingObserver = isLoading ->
                UIUtils.updateLoadingState(
                        Arrays.asList(milestoneViewModel, sessionViewModel),
                        globalUIViewModel
                );
        milestoneViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);
        sessionViewModel.isLoading().observe(getViewLifecycleOwner(), loadingObserver);

        // Observing multimedia uploads:
        sessionViewModel.isMediaLoading().observe(getViewLifecycleOwner(), isMediaLoading -> {
            boolean wasWaiting = userMustWaitToSave;
            userMustWaitToSave = (isMediaLoading != null && isMediaLoading);

            // If a media operation starts and there wasn't another one:
            if (userMustWaitToSave && !wasWaiting) {
                Snackbar.make(binding.getRoot(), R.string.media_uploading_start, Snackbar.LENGTH_SHORT).show();
            }
            // If previous media operations has finished:
            else if (!userMustWaitToSave && wasWaiting) {

                // Success message, avoiding it when the multimedia couldn't be updated:
                if (!pendingMediaAnswers.isEmpty()) {
                    Snackbar.make(binding.getRoot(), R.string.media_uploading_done, Snackbar.LENGTH_LONG).show();
                }

                // Saving all valid answers:
                for (Answer pending : pendingMediaAnswers) {
                    saveOrUpdateAnswer(pending.getActivityId(), pending.getGivenAnswer());
                }
                pendingMediaAnswers.clear();
            }
        });

        // Advising student when multimedia couldn't be uploaded:
        sessionViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.error_upload_title)
                        .setIcon(R.mipmap.icon_deambulario)
                        .setMessage(R.string.error_upload_instructions)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                pendingMediaAnswers.clear();
                userMustWaitToSave = false;
                sessionViewModel.clearError();
            }
        });
    }


    /**
     * Manages the visibility of the RecyclerView and the empty state view.
     *
     * @param milestone The current milestone.
     */
    private void updateEmptyStateVisibility(@NotNull Milestone milestone) {
        boolean hasContents = milestone.getContents() != null && !milestone.getContents().isEmpty();
        boolean hasActivities = milestone.getActivities() != null && !milestone.getActivities().isEmpty();

        if (!hasContents && !hasActivities) {
            binding.recyclerviewMilestoneItems.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerviewMilestoneItems.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Delegates the action to be taken based on the ActivityType clicked by the student.
     *
     * @param activity The activity that was clicked.
     */
    private void handleActivityClick(Activity activity) {

        switch (activity.getType()) {
            case TEST:
                // Getting the previous student answer, if any:
                String previousTestAnswer = getPreviousAnswer(activity.getActivityId());

                // Creating the bottom sheet fragment:
                StudentMilestoneBottomTestFragment bottomSheet = StudentMilestoneBottomTestFragment.newInstance(activity, previousTestAnswer);
                Fragment prev = getChildFragmentManager().findFragmentByTag("TestBottomSheet");
                if (prev == null) {
                    bottomSheet.show(getChildFragmentManager(), "TestBottomSheet");
                }
                break;

            case PHOTO:
            case VIDEO:
                showMediaSourceDialog(activity);
                break;

            case QUESTION:
            default:
                showOpenQuestionDialog(activity);
                break;
        }
    }


    /**
     * Shows an AlertDialog with an EditText for answering open questions.
     *
     * @param activity The question activity to answer.
     */
    private void showOpenQuestionDialog(Activity activity) {

        // Creating the dialog:
        View view = getLayoutInflater().inflate(R.layout.dialog_open_question, null);
        TextInputLayout textInputLayout = view.findViewById(R.id.text_input_layout_answer);
        TextInputEditText editText = view.findViewById(R.id.edit_text_answer);

        // Loading previous answer:
        String previousAnswer = getPreviousAnswer(activity.getActivityId());
        if (previousAnswer != null) {
            editText.setText(previousAnswer);
        }

        // Launching the dialog:
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(activity.getText())
                .setIcon(R.mipmap.icon_deambulario)
                .setView(view)
                .setPositiveButton(getString(R.string.send), (dialog, which) -> {
                    String answerText = UIUtils.validateNotEmpty(textInputLayout, getString(R.string.answer_cannot_be_empty));
                    if (answerText != null) {
                        saveOrUpdateAnswer(activity.getActivityId(), answerText);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    /**
     * Shows a dialog allowing the student to choose between Camera or Gallery for media tasks.
     *
     * @param activity The multimedia activity to perform.
     */
    private void showMediaSourceDialog(Activity activity) {

        // Retrieving previous answer:
        String previousMediaUri = getPreviousAnswer(activity.getActivityId());
        boolean hasPrevious = previousMediaUri != null && !previousMediaUri.isEmpty();

        String[] options;
        if (hasPrevious) {
            options = new String[]{getString(R.string.camera), getString(R.string.gallery), getString(R.string.view_current)};
        } else {
            options = new String[]{getString(R.string.camera), getString(R.string.gallery)};
        }

        int titleResId = (activity.getType() == Activity.ActivityType.PHOTO) ? R.string.take_photo : R.string.take_video;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleResId)
                .setIcon(R.mipmap.icon_deambulario)
                .setItems(options, (dialog, which) -> {
                    this.activity = activity;
                    if (which == 0) {
                        launchCameraForStudent(activity.getType());
                    } else if (which == 1) {
                        launchMediaPickerForStudent(activity.getType());
                    } else if (which == 2) {
                        openWebLink(previousMediaUri);
                        this.activity = null; //
                    }
                })
                .show();
    }


    /**
     * Launches the native Android visual media picker filtering by Photo or Video.
     *
     * @param type Determines if the gallery should show only images or only videos.
     */
    private void launchMediaPickerForStudent(Activity.ActivityType type) {
        ActivityResultContracts.PickVisualMedia.VisualMediaType mediaType =
                (type == Activity.ActivityType.PHOTO)
                        ? ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE
                        : ActivityResultContracts.PickVisualMedia.VideoOnly.INSTANCE;
        mediaPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(mediaType)
                .build());
    }


    /**
     * Creates a temporary file and launches the appropriate camera intent.
     *
     * @param type Determines if the intent should capture a Photo or a Video.
     */
    private void launchCameraForStudent(Activity.ActivityType type) {
        try {
            // Creating a temporary file in the app's cache directory:
            String prefix = (type == Activity.ActivityType.PHOTO) ? "IMG_" : "VID_";
            String suffix = (type == Activity.ActivityType.PHOTO) ? ".jpg" : ".mp4";
            File mediaFile = File.createTempFile(prefix, suffix, requireContext().getCacheDir());

            // Getting the file Uri:
            String authority = requireContext().getPackageName() + ".fileprovider";
            currentCameraUri = FileProvider.getUriForFile(requireContext(), authority, mediaFile);

            // Launching the corresponding intent:
            if (type == Activity.ActivityType.PHOTO) {
                takePictureLauncher.launch(currentCameraUri);
            } else {
                captureVideoLauncher.launch(currentCameraUri);
            }

        } catch (IOException e) {
            Toast.makeText(requireContext(), R.string.error_saving, Toast.LENGTH_SHORT).show();
            activity = null;
        }
    }


    /**
     * Processes the selected media from either gallery or camera, validates size/compress,
     * and delegates uploading to ViewModel.
     *
     * @param selectedMediaUri The URI of the selected or captured media.
     * @param activity         The activity associated with this media submission.
     */
    private void processStudentMedia(Uri selectedMediaUri, Activity activity) {
        Uri uriToUpload = selectedMediaUri;
        userMustWaitToSave = true;

        // Validating video size:
        if (activity.getType() == Activity.ActivityType.VIDEO) {
            double sizeMB = FilesUtils.getFileSizeInMB(requireContext(), selectedMediaUri);
            if (sizeMB > 50) {
                Toast.makeText(requireContext(), getString(R.string.too_large_video_student), Toast.LENGTH_LONG).show();
                this.activity = null;
                return;
            }
        }
        // Compressing photo:
        else {
            uriToUpload = FilesUtils.compressImage(requireContext(), selectedMediaUri);
        }

        // Creating the answer:
        Answer mediaAnswer = new Answer(activity.getActivityId(), uriToUpload.toString());
        pendingMediaAnswers.add(mediaAnswer);

        // Uploading the media
        sessionViewModel.uploadMedia(uriToUpload, mediaAnswer);

        // Releasing the temporary variable:
        this.activity = null;
    }


    /**
     * Opens an external web link using the system's default browser.
     *
     * @param url The target URL.
     */
    private void openWebLink(String url) {
        try {
            // Adding https prefix if missing:
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            requireContext().startActivity(browserIntent);

        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Retrieves the previously saved answer for a given activity, if any.
     *
     * @param activityId The ID of the activity.
     * @return The previous answer value, or null if not answered yet.
     */
    @Nullable
    String getPreviousAnswer(String activityId) {
        if (student == null || student.getAnswers() == null) {
            return null;
        }

        for (Answer ans : student.getAnswers()) {
            if (ans.getActivityId().equals(activityId)) {
                return ans.getGivenAnswer();
            }
        }
        return null;
    }


    /**
     * Saves or updates a student's answer avoiding duplicates.
     */
    private void saveOrUpdateAnswer(String activityId, String answerValue) {
        List<Answer> studentAnswers = student.getAnswers();

        // This is the first answer:
        if (studentAnswers == null) {
            studentAnswers = new ArrayList<>();
        }
        // Avoiding update local multimedia URIs, when network is not available.
        else if (answerValue.startsWith("content://") || answerValue.startsWith("file://")) {
            return;
        }
        // Removing previous answer, if exists:
        else {
            studentAnswers.removeIf(ans -> ans.getActivityId().equals(activityId));
        }

        // Adding the new answer:
        studentAnswers.add(new Answer(activityId, answerValue));

        // Saving answer:
        Session session = sessionViewModel.getCurrentSession().getValue();
        if (session != null) {
            sessionViewModel.updateStudentAnswers(session.getId(), studentAnswers);
            student.setAnswers(studentAnswers);
            sessionViewModel.setCurrentStudent(student);
            UIUtils.playShortSound(requireContext(), R.raw.sound_activity_done);
        } else {
            Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
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