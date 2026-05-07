package com.iesaguadulce.deambulario.dashboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.databinding.SheetAddcontentBinding;
import com.iesaguadulce.deambulario.model.pojos.Content;
import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.utils.FilesUtils;
import com.iesaguadulce.deambulario.utils.UIUtils;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;
import com.iesaguadulce.deambulario.viewmodel.MilestoneViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fragment to manage the Bottom Sheet used to perform Milestone Content operations.
 *
 * @author Mario López Salazar.
 */
public class ContentBottomFragment extends BottomSheetDialogFragment {

    /*
     * ViewBinding to manage view components.
     */
    private SheetAddcontentBinding binding;

    /*
     * ViewModel and variables to manage it.
     */
    private MilestoneViewModel milestoneViewModel;
    private Milestone milestone;
    private int contentIndex;
    private Content content;

    /*
     * Flag to indicate if the Bottom Sheet is opened on edit or creating mode.
     */
    private boolean isEditMode = false;

    /*
     * Specific variables to perform the selection of photo-video from the device gallery.
     */
    private ActivityResultLauncher<PickVisualMediaRequest> mediaPickerLauncher;
    private Uri selectedMediaUri;



    /**
     * Inflates the bottom sheet view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetAddcontentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Initializes the setting up of the view.
     *
     * @param view               The View returned by the onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Registering the result of the Activity for select the media from device gallery:
        mediaPickerLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                selectedMediaUri = uri;
                showMediaPreview(uri);
            }
        });

        // Getting the ViewModel and the current milestone:
        milestoneViewModel = new ViewModelProvider(requireActivity()).get(MilestoneViewModel.class);
        GlobalUIViewModel globalUIViewModel = new ViewModelProvider(requireActivity()).get(GlobalUIViewModel.class);
        milestone = Objects.requireNonNull(milestoneViewModel.getSelectedMilestone().getValue());

        // Setting up the listener for the ChipGroup:
        setupChipGroupListener();

        // Setting up the appearance of the UI and filling previous data values:
        determineModeAndPopulate();

        // Getting the read-only flag value:
        Boolean readOnly = globalUIViewModel.isReadOnlyMode().getValue();
        if (readOnly != null && readOnly) {
            applyReadOnlyMode();
        }

        // Setting up buttons listeners:
        binding.buttonSaveContent.setOnClickListener(v -> saveContent());
        binding.buttonDeleteContent.setOnClickListener(v -> showDeleteConfirmationDialog());
        binding.buttonSelectMedia.setOnClickListener(v -> launchMediaPicker());
        binding.buttonTryUrl.setOnClickListener(v -> tryUrl());
    }


    /**
     * Performs form appearance depending on the kind of content selected on ChipGroup.
     */
    private void setupChipGroupListener() {
        binding.chipGroupContent.setOnCheckedStateChangeListener((group, checkedIds) -> {

            // Getting the selected chip:
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);

            // Text content:
            if (checkedId == R.id.chip_text) {
                binding.textInputPrimaryInput.setVisibility(View.VISIBLE);
                binding.textInputPrimaryInput.setHint(getString(R.string.text));
                binding.textInputUrl.setVisibility(View.GONE);
                binding.layoutMediaSelector.setVisibility(View.GONE);
                binding.buttonTryUrl.setVisibility(View.GONE);
            }
            // Link content:
            else if (checkedId == R.id.chip_link) {
                binding.textInputPrimaryInput.setVisibility(View.VISIBLE);
                binding.textInputPrimaryInput.setHint(getString(R.string.title));
                binding.textInputUrl.setVisibility(View.VISIBLE);
                binding.layoutMediaSelector.setVisibility(View.GONE);
                binding.buttonTryUrl.setVisibility(View.VISIBLE);
            }
            // Photo or video content:
            else if (checkedId == R.id.chip_media) {
                binding.textInputPrimaryInput.setVisibility(View.GONE);
                binding.textInputUrl.setVisibility(View.GONE);
                binding.layoutMediaSelector.setVisibility(View.VISIBLE);
                binding.buttonTryUrl.setVisibility(View.GONE);
            }
        });
    }


    /**
     * Determines if we are creating a new content or editing an existing one,
     * and populates the UI accordingly.
     */
    private void determineModeAndPopulate() {

        // Getting the current content index from the ViewModel, if selected:
        contentIndex = Objects.requireNonNull(milestoneViewModel.getSelectedContent().getValue());

        // Edit mode (there is a selected content):
        if (contentIndex >= 0) {
            isEditMode = true;
            binding.textViewContentSheetTitle.setText(R.string.edit);
            binding.buttonDeleteContent.setVisibility(View.VISIBLE);

            // Getting the current content:
            Content originalContent = Objects.requireNonNull(milestone.getContents().get(contentIndex));

            // Need to create a clone of the Content object (to assure recyclerview redrawing on exit):
            content = new Content();
            content.setType(originalContent.getType());
            content.setValue(originalContent.getValue());

            // Setting UI appearance and filling views:
            switch (content.getType()) {
                case TEXT:
                    binding.chipGroupContent.check(R.id.chip_text);
                    binding.textPrimaryInput.setText(content.getValue());
                    break;
                case URL:
                    binding.chipGroupContent.check(R.id.chip_link);
                    String urlContent = content.getValue();
                    String[] parts = urlContent.split(UIUtils.LINK_SEPARATOR, 2);
                    if (parts.length == 2) {
                        binding.textPrimaryInput.setText(parts[0]);
                        binding.textUrl.setText(parts[1]);
                    } else {
                        binding.textPrimaryInput.setText(urlContent);
                        binding.textUrl.setText(urlContent);
                    }
                    binding.buttonTryUrl.setVisibility(View.VISIBLE);
                    break;
                case PICTURE:
                case VIDEO:
                    binding.chipGroupContent.check(R.id.chip_media);
                    binding.chipGroupContent.getChildAt(0).setEnabled(false);
                    binding.chipGroupContent.getChildAt(1).setEnabled(false);
                    showMediaPreview(Uri.parse(content.getValue()));
                    binding.imageMediaPreview.setVisibility(View.VISIBLE);
                    binding.buttonSaveContent.setVisibility(View.GONE);
                    binding.buttonSelectMedia.setVisibility(View.GONE);
                    break;
            }
        }

        // Creation mode (there isn't a selected content):
        else {
            isEditMode = false;

            // Creating a new content:
            content = new Content();

            // Setting UI appearance (text content as default):
            binding.textViewContentSheetTitle.setText(R.string.add_content);
            binding.buttonDeleteContent.setVisibility(View.GONE);
            binding.chipGroupContent.check(R.id.chip_text);
        }
    }


    /**
     * Locks the UI to prevent any modifications when in read-only mode.
     */
    private void applyReadOnlyMode() {
        binding.buttonSaveContent.setVisibility(View.GONE);
        binding.buttonDeleteContent.setVisibility(View.GONE);
        binding.buttonSelectMedia.setVisibility(View.GONE);
        binding.chipGroupContent.setVisibility(View.GONE);
        binding.textPrimaryInput.setFocusable(false);
        binding.textUrl.setFocusable(false);
        binding.textViewContentSheetTitle.setText(R.string.details);
    }


    /**
     * Launches the Internet navigator to try a URL content texted on the corresponding UI text field.
     */
    private void tryUrl() {

        // Getting the content of the text field:
        Editable url = binding.textUrl.getText();
        if (url == null) {
            return;
        }
        String fullUrl = url.toString();

        // Adding https prefix, is needed:
        if (!fullUrl.startsWith("http://") && !fullUrl.startsWith("https://")) {
            fullUrl = "https://" + fullUrl;
        }

        // Launching Internet navigation intent:
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl));
            requireContext().startActivity(browserIntent);
        } catch (Exception a) {
            Toast.makeText(requireContext(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Launches the native Android visual media picker.
     */
    private void launchMediaPicker() {
        mediaPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                .build());
    }


    /**
     * Shows the media preview on photo-video content.
     *
     * @param uri The URI of the image or video to display.
     */
    private void showMediaPreview(Uri uri) {
        binding.imageMediaPreview.setVisibility(View.VISIBLE);
        Glide.with(requireContext())
                .load(uri)
                .centerCrop()
                .into(binding.imageMediaPreview);
    }


    /**
     * Validates input data, updates the current content object, and sends it to the ViewModel to be saved.
     */
    private void saveContent() {

        // Getting the content type:
        int checkedId = binding.chipGroupContent.getCheckedChipId();

        // Text content:
        if (checkedId == R.id.chip_text) {
            String text = UIUtils.validateNotEmpty(binding.textInputPrimaryInput, getString(R.string.cannot_be_empty));
            if (text == null || text.isEmpty()) {
                return;
            }
            content.setType(Content.ContentType.TEXT);
            content.setValue(text);
        }
        // Link content:
        else if (checkedId == R.id.chip_link) {
            String title = UIUtils.validateNotEmpty(binding.textInputPrimaryInput, getString(R.string.cannot_be_empty));
            String url = UIUtils.validateNotEmpty(binding.textInputUrl, getString(R.string.cannot_be_empty));
            if (title == null || url == null) {
                return;
            }
            content.setType(Content.ContentType.URL);
            content.setValue(title + UIUtils.LINK_SEPARATOR + url);
        }
        // Photo or video content:
        else if (checkedId == R.id.chip_media) {

            // Checking if some media selected:
            if (selectedMediaUri == null) {
                Toast.makeText(requireContext(), R.string.please_select_photo_or_video, Toast.LENGTH_SHORT).show();
                binding.buttonSelectMedia.requestFocus();
                return;
            }

            // Guessing the Mime type of the content:
            String mimeType = requireContext().getContentResolver().getType(selectedMediaUri);
            Uri uriToUpload;

            // Video content:
            if (mimeType != null && mimeType.startsWith("video")) {
                // Checking max video size:
                double sizeMB = FilesUtils.getFileSizeInMB(requireContext(), selectedMediaUri);
                if (sizeMB > 50) {
                    Toast.makeText(requireContext(), getString(R.string.too_large_video), Toast.LENGTH_LONG).show();
                    return;
                }
                content.setType(Content.ContentType.VIDEO);

                // Establishing local video to be updated:
                uriToUpload = selectedMediaUri;
            }
            // Picture content:
            else {
                content.setType(Content.ContentType.PICTURE);

                // Compressing the picture:
                uriToUpload = FilesUtils.compressImage(requireContext(), selectedMediaUri);
            }

            // Setting local URI as the provisional URI:
            content.setValue(uriToUpload.toString());

            // Launching media uploading:
            milestoneViewModel.uploadMedia(uriToUpload, content);
        }

        // Saving on ViewModel:
        List<Content> contents;
        if (milestone.getContents() == null) {
            contents = new ArrayList<>();
        } else {
            contents = new ArrayList<>(milestone.getContents());
        }
        if (isEditMode) {
            contents.set(contentIndex, content);
        } else {
            contents.add(content);
        }
        milestone.setContents(contents);
        milestoneViewModel.setSelectedMilestone(milestone);

        // Hiding the bottom sheet:
        dismiss();
    }


    /**
     * Shows a confirmation dialog before deleting the content.
     */
    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setIcon(R.mipmap.icon_deambulario)
                .setMessage(getString(R.string.confirm_content_deletion))
                .setPositiveButton(getString(R.string.delete),
                        (dialog, which) -> deleteContent())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    /**
     * Deletes the current content from the milestone and updates the ViewModel.
     */
    private void deleteContent() {

        // Multimedia deletion:
        if (content.getType() == Content.ContentType.PICTURE || content.getType() == Content.ContentType.VIDEO) {
            milestoneViewModel.deleteMedia(content.getValue());
        }

        // Content deletion:
        List<Content> contents = new ArrayList<>(milestone.getContents());
        contents.remove(contentIndex);
        milestone.setContents(contents);
        milestoneViewModel.setSelectedMilestone(milestone);

        // Hiding the bottom sheet:
        dismiss();
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