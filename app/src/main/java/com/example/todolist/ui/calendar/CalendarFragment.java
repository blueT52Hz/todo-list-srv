package com.example.todolist.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.todolist.data.Task;
import com.example.todolist.data.Topic;
import com.example.todolist.databinding.FragmentCalendarBinding;
import com.example.todolist.reminder.ReminderScheduler;
import com.example.todolist.ui.detail.TaskDetailActivity;
import com.example.todolist.ui.tasks.TaskAdapter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarFragment extends Fragment implements TaskAdapter.Listener {

    private FragmentCalendarBinding b;
    private CalendarViewModel vm;
    private CalendarDayAdapter dayAdapter;
    private TaskAdapter taskAdapter;
    private final Locale vi = new Locale("vi");

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentCalendarBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(CalendarViewModel.class);

        dayAdapter = new CalendarDayAdapter(vm::select);
        b.calendarGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        b.calendarGrid.setAdapter(dayAdapter);
        b.calendarGrid.setNestedScrollingEnabled(false);

        taskAdapter = new TaskAdapter(this);
        b.dayTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.dayTasks.setAdapter(taskAdapter);
        b.dayTasks.setNestedScrollingEnabled(false);

        b.btnPrev.setOnClickListener(v -> vm.prevMonth());
        b.btnNext.setOnClickListener(v -> vm.nextMonth());

        vm.getTopics().observe(getViewLifecycleOwner(), this::bindTopicColors);
        vm.getMonth().observe(getViewLifecycleOwner(), m -> { bindMonthLabel(m); rebuildGrid(); });
        vm.getDaysWithTasks().observe(getViewLifecycleOwner(), s -> rebuildGrid());
        vm.getSelected().observe(getViewLifecycleOwner(), d -> { bindDayTitle(d); rebuildGrid(); });

        vm.getDayTasks().observe(getViewLifecycleOwner(), tasks -> {
            taskAdapter.submitList(tasks);
            boolean empty = tasks == null || tasks.isEmpty();
            b.dayEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            b.dayTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void bindTopicColors(List<Topic> topics) {
        Map<Long, String> colors = new HashMap<>();
        for (Topic t : topics) colors.put(t.id, t.colorHex);
        taskAdapter.setTopicColors(colors);
    }

    private void bindMonthLabel(YearMonth m) {
        if (m == null) return;
        String name = m.getMonth().getDisplayName(TextStyle.FULL, vi);
        b.monthLabel.setText(name + " " + m.getYear());
    }

    private void bindDayTitle(LocalDate d) {
        if (d == null) return;
        String dow = d.getDayOfWeek().getDisplayName(TextStyle.FULL, vi);
        b.dayTitle.setText(dow + ", " + d.getDayOfMonth() + "/" + d.getMonthValue());
    }

    /** Rebuild the 7-col cell list from current month + dots + selection. */
    private void rebuildGrid() {
        if (b == null) return;
        YearMonth m = vm.getMonth().getValue();
        if (m == null) return;
        Set<LocalDate> dots = vm.getDaysWithTasks().getValue();
        if (dots == null) dots = new HashSet<>();
        LocalDate sel = vm.getSelected().getValue();
        LocalDate today = LocalDate.now();

        List<CalendarCell> cells = new ArrayList<>();
        int lead = m.atDay(1).getDayOfWeek().getValue() - 1; // Mon→0 … Sun→6
        for (int i = 0; i < lead; i++) cells.add(new CalendarCell(null, false, false, false));
        int len = m.lengthOfMonth();
        for (int day = 1; day <= len; day++) {
            LocalDate date = m.atDay(day);
            cells.add(new CalendarCell(date, dots.contains(date),
                date.equals(sel), date.equals(today)));
        }
        dayAdapter.submit(cells);
    }

    @Override
    public void onOpen(Task task) {
        startActivity(new Intent(requireContext(), TaskDetailActivity.class).putExtra("task_id", task.id));
    }

    @Override
    public void onToggle(Task task) {
        task.done = !task.done;
        vm.update(task);
        ReminderScheduler.schedule(requireContext(), task);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
