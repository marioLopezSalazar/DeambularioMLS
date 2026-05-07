package com.iesaguadulce.deambulario.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.adapters.FaqListAdapter;
import com.iesaguadulce.deambulario.databinding.FragmentFaqBinding;
import com.iesaguadulce.deambulario.model.pojos.FaqItem;
import com.iesaguadulce.deambulario.utils.FilesUtils;

import java.util.List;

/**
 * Fragment that displays a list of Frequently Asked Questions (FAQ) for the teacher.
 * Uses an accordion-style interface to show and hide answers.
 *
 * @author Mario López Salazar
 */
public class TeacherFAQFragment extends Fragment {

    /*
     * ViewBinding to perform the fragment view.
     */
    private FragmentFaqBinding binding;


    /**
     * Called to inflate the fragment's interface view.
     *
     * @param inflater           The LayoutInflater object used to inflate any views in the fragment.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The created view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFaqBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Prepares the FAQ data and sets up the RecyclerView.
     *
     * @param view               The View, returned by onCreateView method.
     * @param savedInstanceState If non-null, this fragment is being re-construct from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Generating the static FAQ data:
        List<FaqItem> faqList = loadFaqData();

        // Setting up the adapter:
        FaqListAdapter faqListAdapter = new FaqListAdapter(faqList);
        binding.recyclerviewFaqItems.setAdapter(faqListAdapter);
    }


    /**
     * Generates the static list of Frequently Asked Questions.
     *
     * @return A list containing the FAQ items.
     */
    private List<FaqItem> loadFaqData() {
        return FilesUtils.loadFaqsFromRaw(requireContext(), R.raw.teacher_faqs);
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