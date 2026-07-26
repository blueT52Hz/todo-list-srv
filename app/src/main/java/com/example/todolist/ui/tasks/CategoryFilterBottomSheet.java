package com.example.todolist.ui.tasks;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.todolist.data.TopicRepository;
import com.example.todolist.databinding.BottomsheetCategoryFilterBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashSet;
import java.util.Set;

/** Multi-select category filter. Returns picked topic ids (long[]) via FragmentResult under RESULT_IDS. */
public class CategoryFilterBottomSheet extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "category_filter";
    public static final String RESULT_IDS = "selected_ids";
    private static final String ARG_SELECTED = "arg_selected";

    private BottomsheetCategoryFilterBinding b;
    private CategoryCheckAdapter adapter;

    public static CategoryFilterBottomSheet newInstance(@Nullable Set<Long> selected) {
        CategoryFilterBottomSheet f = new CategoryFilterBottomSheet();
        Bundle args = new Bundle();
        args.putLongArray(ARG_SELECTED, toArray(selected));
        f.setArguments(args);
        return f;
    }

    private static long[] toArray(Set<Long> set) {
        if (set == null) return new long[0];
        long[] a = new long[set.size()];
        int i = 0;
        for (Long v : set) a[i++] = v;
        return a;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = BottomsheetCategoryFilterBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new CategoryCheckAdapter();
        b.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvCategories.setAdapter(adapter);

        Set<Long> initial = new HashSet<>();
        long[] ids = requireArguments().getLongArray(ARG_SELECTED);
        if (ids != null) for (long v : ids) initial.add(v);
        adapter.setChecked(initial);

        new TopicRepository(requireContext()).getAll().observe(getViewLifecycleOwner(), topics -> {
            adapter.setItems(topics);
            boolean empty = topics == null || topics.isEmpty();
            b.emptyHint.setVisibility(empty ? View.VISIBLE : View.GONE);
            b.rvCategories.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        b.search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void afterTextChanged(Editable s) { adapter.filter(s.toString()); }
        });

        b.btnClear.setOnClickListener(v -> adapter.clearChecks());
        b.btnApply.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putLongArray(RESULT_IDS, toArray(adapter.getChecked()));
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
