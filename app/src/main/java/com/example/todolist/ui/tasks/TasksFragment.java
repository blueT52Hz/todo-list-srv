package com.example.todolist.ui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.todolist.R;
import com.example.todolist.data.Topic;
import com.example.todolist.databinding.FragmentTasksBinding;
import com.example.todolist.reminder.ReminderScheduler;
import com.example.todolist.ui.detail.TaskDetailActivity;
import com.example.todolist.ui.calendar.DatePickerDialogFragment;
import com.google.android.material.chip.Chip;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasksFragment extends Fragment implements TaskAdapter.Listener {
    private FragmentTasksBinding binding;
    private TasksViewModel viewModel;
    private TaskAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(TasksViewModel.class);
        adapter = new TaskAdapter(this);
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(adapter);

        viewModel.getTopics().observe(getViewLifecycleOwner(), this::buildChips);

        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            adapter.submitList(tasks);
            binding.count.setText(tasks.size() + " tasks");
            boolean empty = tasks.isEmpty();
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        binding.fabAddTask.setOnClickListener(v -> openEditor(null));
        binding.btnEmptyAdd.setOnClickListener(v -> openEditor(null));

        binding.btnPickDate.setOnClickListener(v -> {
            LocalDate init = viewModel.getDate().getValue();
            DatePickerDialogFragment.newInstance(init != null ? init : LocalDate.now())
                .show(getChildFragmentManager(), "date_picker");
        });
        binding.btnClearDate.setOnClickListener(v -> viewModel.clearDate());

        getChildFragmentManager().setFragmentResultListener(
            DatePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                long epochDay = bundle.getLong(DatePickerDialogFragment.RESULT_EPOCH_DAY);
                viewModel.setDate(LocalDate.ofEpochDay(epochDay));
            });

        viewModel.getDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null) {
                binding.btnPickDate.setText(date.getDayOfMonth() + "/" + date.getMonthValue());
                binding.btnClearDate.setVisibility(View.VISIBLE);
            } else {
                binding.btnPickDate.setText(R.string.pick_date);
                binding.btnClearDate.setVisibility(View.GONE);
            }
        });
    }

    private void buildChips(List<Topic> topics) {
        binding.chipGroup.removeAllViews();
        Map<Long, String> colors = new HashMap<>();

        Chip all = makeChip(getString(R.string.all));
        all.setTag(null);
        all.setChecked(viewModel.getFilter() == null);
        binding.chipGroup.addView(all);

        for (Topic t : topics) {
            colors.put(t.id, t.colorHex);
            Chip chip = makeChip(t.name);
            chip.setTag(t.id);
            chip.setChecked(viewModel.getFilter() != null && viewModel.getFilter() == t.id);
            binding.chipGroup.addView(chip);
        }
        adapter.setTopicColors(colors);

        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip checked = group.findViewById(checkedIds.get(0));
            viewModel.setFilter(checked == null ? null : (Long) checked.getTag());
        });
    }

    private Chip makeChip(String text) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_bg_color));
        chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.nav_item_color));
        chip.setChipStrokeColorResource(R.color.whisper_border);
        chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
        return chip;
    }

    private void openEditor(@Nullable Long taskId) {
        AddEditTaskBottomSheet.newInstance(taskId).show(getChildFragmentManager(), "edit_task");
    }

    @Override
    public void onOpen(com.example.todolist.data.Task task) {
        startActivity(new Intent(requireContext(), TaskDetailActivity.class).putExtra("task_id", task.id));
    }

    @Override
    public void onToggle(com.example.todolist.data.Task task) {
        task.done = !task.done;
        viewModel.update(task);
        ReminderScheduler.schedule(requireContext(), task);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
