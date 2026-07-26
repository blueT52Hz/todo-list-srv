package com.example.todolist.ui.tasks;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.todolist.R;
import com.example.todolist.data.Task;
import com.example.todolist.data.Topic;
import com.example.todolist.databinding.BottomsheetEditTaskBinding;
import com.example.todolist.reminder.ReminderScheduler;
import com.example.todolist.util.DateUtils;
import com.example.todolist.util.ImageStorage;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/** Add or edit a task (title, topic, due date+time reminder, one image). */
public class AddEditTaskBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID = "task_id";

    private BottomsheetEditTaskBinding b;
    private TasksViewModel vm;

    private long editingId = -1;
    private long createdAt = -1;
    private boolean done = false;
    private Long selectedTopicId = null;
    private Long selectedDueAt = null;
    private String imagePath = null;
    private String oldImagePath = null;

    private final ActivityResultLauncher<String> picker =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) onImagePicked(uri);
        });

    public static AddEditTaskBottomSheet newInstance(@Nullable Long taskId) {
        AddEditTaskBottomSheet f = new AddEditTaskBottomSheet();
        Bundle args = new Bundle();
        if (taskId != null) args.putLong(ARG_ID, taskId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = BottomsheetEditTaskBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(TasksViewModel.class);

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_ID)) {
            editingId = args.getLong(ARG_ID);
            b.sheetTitle.setText(getString(R.string.edit_task));
            vm.getTaskById(editingId, this::prefill);
        }

        vm.getTopics().observe(getViewLifecycleOwner(), this::buildTopicChips);

        b.rowDue.setOnClickListener(v -> pickDate());
        b.btnClearDue.setOnClickListener(v -> setDue(null));
        b.attach.setOnClickListener(v -> picker.launch("image/*"));
        b.btnSave.setOnClickListener(v -> onSave());
    }

    private void prefill(Task t) {
        if (t == null || b == null) return;
        createdAt = t.createdAt;
        done = t.done;
        selectedTopicId = t.topicId;
        oldImagePath = t.imagePath;
        imagePath = t.imagePath;
        b.etTitle.setText(t.title);
        setDue(t.dueAt);
        if (t.imagePath != null) showPreview(t.imagePath);
        // re-check the matching topic chip if chips already built
        checkSelectedChip();
    }

    private void buildTopicChips(List<Topic> topics) {
        b.topicChips.removeAllViews();

        Chip none = makeChip(getString(R.string.all)); // "All" acts as "no topic" here
        none.setTag(null);
        b.topicChips.addView(none);

        for (Topic t : topics) {
            Chip chip = makeChip(t.name);
            chip.setTag(t.id);
            b.topicChips.addView(chip);
        }

        b.topicChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) { selectedTopicId = null; return; }
            Chip checked = group.findViewById(checkedIds.get(0));
            selectedTopicId = checked == null ? null : (Long) checked.getTag();
        });
        checkSelectedChip();
    }

    private void checkSelectedChip() {
        if (b == null) return;
        for (int i = 0; i < b.topicChips.getChildCount(); i++) {
            Chip chip = (Chip) b.topicChips.getChildAt(i);
            Long tag = (Long) chip.getTag();
            boolean match = (selectedTopicId == null && tag == null)
                || (selectedTopicId != null && selectedTopicId.equals(tag));
            chip.setChecked(match);
        }
    }

    private Chip makeChip(String text) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        return chip;
    }

    private void pickDate() {
        MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker()
            .setSelection(selectedDueAt != null ? selectedDueAt : MaterialDatePicker.todayInUtcMilliseconds())
            .build();
        dp.addOnPositiveButtonClickListener(this::pickTime);
        dp.show(getChildFragmentManager(), "date");
    }

    private void pickTime(long dateUtcMillis) {
        MaterialTimePicker tp = new MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(9).setMinute(0)
            .build();
        tp.addOnPositiveButtonClickListener(v -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(dateUtcMillis);
            Calendar local = Calendar.getInstance();
            local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH),
                tp.getHour(), tp.getMinute(), 0);
            local.set(Calendar.MILLISECOND, 0);
            setDue(local.getTimeInMillis());
        });
        tp.show(getChildFragmentManager(), "time");
    }

    private void setDue(Long due) {
        selectedDueAt = due;
        if (due == null) {
            b.tvDue.setText("—");
            b.btnClearDue.setVisibility(View.GONE);
        } else {
            b.tvDue.setText(DateUtils.formatDue(due));
            b.btnClearDue.setVisibility(View.VISIBLE);
        }
    }

    private void onImagePicked(Uri uri) {
        ImageStorage.copyToInternal(requireContext(), uri, path -> {
            if (path != null && b != null) {
                imagePath = path;
                showPreview(path);
            }
        });
    }

    private void showPreview(String path) {
        b.preview.setVisibility(View.VISIBLE);
        b.attachPlaceholder.setVisibility(View.GONE);
        Glide.with(this).load(new File(path)).centerCrop().into(b.preview);
    }

    private void onSave() {
        String title = b.etTitle.getText() == null ? "" : b.etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            b.etTitle.setError(getString(R.string.title));
            return;
        }
        Task t = new Task();
        t.title = title;
        t.topicId = selectedTopicId;
        t.dueAt = selectedDueAt;
        t.imagePath = imagePath;
        t.done = done;

        // if the image was replaced during edit, delete the old file
        if (oldImagePath != null && !oldImagePath.equals(imagePath)) {
            ImageStorage.delete(oldImagePath);
        }

        if (editingId >= 0) {
            t.id = editingId;
            t.createdAt = createdAt;
            vm.update(t);
            ReminderScheduler.schedule(requireContext(), t);
            dismiss();
        } else {
            t.createdAt = System.currentTimeMillis();
            vm.insert(t, id -> {
                t.id = id;
                ReminderScheduler.schedule(requireContext(), t);
                dismiss();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
