package com.example.todolist.ui.topics;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.todolist.data.Topic;
import com.example.todolist.databinding.BottomsheetEditTopicBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/** Add or edit a topic. Pass a Topic to {@link #newInstance} to edit; null to create. */
public class AddEditTopicBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id", ARG_NAME = "name", ARG_COLOR = "color", ARG_CREATED = "created";
    private static final String[] PALETTE = {
        "#2F7A6F", "#B4593F", "#6B7270", "#A0A5A3", "#4A6FA5", "#7A6F2F"
    };

    private BottomsheetEditTopicBinding b;
    private TopicsViewModel vm;
    private final List<View> swatches = new ArrayList<>();
    private String selectedColor = PALETTE[0];
    private long editingId = -1;
    private long createdAt = -1;

    public static AddEditTopicBottomSheet newInstance(@Nullable Topic t) {
        AddEditTopicBottomSheet f = new AddEditTopicBottomSheet();
        Bundle args = new Bundle();
        if (t != null) {
            args.putLong(ARG_ID, t.id);
            args.putString(ARG_NAME, t.name);
            args.putString(ARG_COLOR, t.colorHex);
            args.putLong(ARG_CREATED, t.createdAt);
        }
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = BottomsheetEditTopicBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(TopicsViewModel.class);

        Bundle args = getArguments();
        boolean editing = args != null && args.containsKey(ARG_ID);
        if (editing) {
            editingId = args.getLong(ARG_ID);
            createdAt = args.getLong(ARG_CREATED);
            selectedColor = args.getString(ARG_COLOR, PALETTE[0]);
            b.etName.setText(args.getString(ARG_NAME, ""));
            b.sheetTitle.setText(getString(com.example.todolist.R.string.edit));
            b.btnDelete.setVisibility(View.VISIBLE);
        }

        buildSwatches();

        b.btnSave.setOnClickListener(v -> onSave());
        b.btnDelete.setOnClickListener(v -> onDelete());
    }

    private void buildSwatches() {
        int size = dp(32);
        int margin = dp(8);
        for (String hex : PALETTE) {
            View sw = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(margin);
            sw.setLayoutParams(lp);
            sw.setTag(hex);
            sw.setOnClickListener(v -> {
                selectedColor = (String) v.getTag();
                refreshSwatches();
            });
            swatches.add(sw);
            b.colorRow.addView(sw);
        }
        refreshSwatches();
    }

    private void refreshSwatches() {
        for (View sw : swatches) {
            String hex = (String) sw.getTag();
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(Color.parseColor(hex));
            if (hex.equals(selectedColor)) {
                d.setStroke(dp(3), Color.parseColor("#1C1F1E"));
            }
            sw.setBackground(d);
        }
    }

    private void onSave() {
        String name = b.etName.getText() == null ? "" : b.etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            b.etName.setError(getString(com.example.todolist.R.string.topic_name));
            return;
        }
        Topic t = new Topic();
        t.name = name;
        t.colorHex = selectedColor;
        if (editingId >= 0) {
            t.id = editingId;
            t.createdAt = createdAt;
            vm.update(t);
        } else {
            t.createdAt = System.currentTimeMillis();
            vm.insert(t);
        }
        dismiss();
    }

    private void onDelete() {
        if (editingId >= 0) {
            Topic t = new Topic();
            t.id = editingId;
            t.name = b.etName.getText() == null ? "" : b.etName.getText().toString();
            t.colorHex = selectedColor;
            t.createdAt = createdAt;
            vm.delete(t);
        }
        dismiss();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
