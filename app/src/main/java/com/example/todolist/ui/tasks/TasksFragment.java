package com.example.todolist.ui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.todolist.R;
import com.example.todolist.data.Topic;
import com.example.todolist.databinding.FragmentTasksBinding;
import com.example.todolist.reminder.ReminderScheduler;
import com.example.todolist.ui.calendar.DatePickerDialogFragment;
import com.example.todolist.ui.detail.TaskDetailActivity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        viewModel.getTopics().observe(getViewLifecycleOwner(), this::applyTopicColors);

        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            adapter.submitList(tasks);
            binding.count.setText(tasks.size() + " tasks");
            boolean empty = tasks.isEmpty();
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        binding.fabAddTask.setOnClickListener(v -> openEditor(null));
        binding.btnEmptyAdd.setOnClickListener(v -> openEditor(null));

        // Date filter
        binding.btnPickDate.setOnClickListener(v -> {
            LocalDate init = viewModel.getDate().getValue();
            DatePickerDialogFragment.newInstance(init != null ? init : LocalDate.now())
                .show(getChildFragmentManager(), "date_picker");
        });
        getChildFragmentManager().setFragmentResultListener(
            DatePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                long epochDay = bundle.getLong(DatePickerDialogFragment.RESULT_EPOCH_DAY);
                viewModel.setDate(LocalDate.ofEpochDay(epochDay));
            });

        viewModel.getDate().observe(getViewLifecycleOwner(), date -> {
            binding.btnPickDate.setText(date != null
                ? date.getDayOfMonth() + "/" + date.getMonthValue()
                : getString(R.string.pick_date));
            updateClearVisibility();
        });

        // Category filter
        binding.btnCategory.setOnClickListener(v ->
            CategoryFilterBottomSheet.newInstance(viewModel.getCategories().getValue())
                .show(getChildFragmentManager(), "category_filter"));

        getChildFragmentManager().setFragmentResultListener(
            CategoryFilterBottomSheet.REQUEST_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                long[] ids = bundle.getLongArray(CategoryFilterBottomSheet.RESULT_IDS);
                Set<Long> set = new HashSet<>();
                if (ids != null) for (long id : ids) set.add(id);
                viewModel.setCategories(set);
            });

        viewModel.getCategories().observe(getViewLifecycleOwner(), cats -> {
            int n = cats == null ? 0 : cats.size();
            binding.btnCategory.setText(n > 0
                ? getString(R.string.category_count, n)
                : getString(R.string.category));
            updateClearVisibility();
        });

        // Status filter (all / not done / done) — segmented pill
        View.OnClickListener statusClick = v -> {
            int s = v.getId() == R.id.btn_status_pending ? 1
                  : v.getId() == R.id.btn_status_done ? 2 : 0;
            viewModel.setStatus(s);
        };
        binding.btnStatusAll.setOnClickListener(statusClick);
        binding.btnStatusPending.setOnClickListener(statusClick);
        binding.btnStatusDone.setOnClickListener(statusClick);
        viewModel.getStatus().observe(getViewLifecycleOwner(), s -> {
            int v = s != null ? s : 0;
            binding.btnStatusAll.setSelected(v == 0);
            binding.btnStatusPending.setSelected(v == 1);
            binding.btnStatusDone.setSelected(v == 2);
            updateClearVisibility();
        });

        // Shared "clear filters" button — clears date + category only.
        // Status is a persistent view toggle, not cleared here.
        binding.btnClearFilters.setOnClickListener(v -> {
            viewModel.clearDate();
            viewModel.setCategories(Collections.emptySet());
        });
    }

    /** Show the shared clear button whenever the date or category filter is active. */
    private void updateClearVisibility() {
        if (binding == null) return;
        Set<Long> cats = viewModel.getCategories().getValue();
        boolean active = viewModel.getDate().getValue() != null
            || (cats != null && !cats.isEmpty());
        binding.btnClearFilters.setVisibility(active ? View.VISIBLE : View.GONE);
    }

    private void applyTopicColors(List<Topic> topics) {
        Map<Long, String> colors = new HashMap<>();
        for (Topic t : topics) colors.put(t.id, t.colorHex);
        adapter.setTopicColors(colors);
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
