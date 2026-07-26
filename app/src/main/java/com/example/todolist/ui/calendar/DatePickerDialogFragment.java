package com.example.todolist.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.TaskDao;
import com.example.todolist.databinding.DialogDatePickerBinding;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Month-grid date picker with dots on days that have tasks. Returns the picked date via FragmentResult. */
public class DatePickerDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "date_picker";
    public static final String RESULT_EPOCH_DAY = "epoch_day";
    private static final String ARG_INITIAL = "initial_epoch_day";

    public static DatePickerDialogFragment newInstance(LocalDate initial) {
        DatePickerDialogFragment f = new DatePickerDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_INITIAL, (initial != null ? initial : LocalDate.now()).toEpochDay());
        f.setArguments(args);
        return f;
    }

    private DialogDatePickerBinding b;
    private CalendarDayAdapter adapter;
    private TaskDao taskDao;
    private final ZoneId zone = ZoneId.systemDefault();
    private final Locale vi = new Locale("vi");
    private YearMonth month;
    private LocalDate selected;
    private Set<LocalDate> dots = new HashSet<>();
    private LiveData<List<Long>> monthSource;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = DialogDatePickerBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        taskDao = AppDatabase.getInstance(requireContext()).taskDao();
        long init = requireArguments().getLong(ARG_INITIAL, LocalDate.now().toEpochDay());
        selected = LocalDate.ofEpochDay(init);
        month = YearMonth.from(selected);

        adapter = new CalendarDayAdapter(this::onDayPicked);
        b.pickerGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        b.pickerGrid.setAdapter(adapter);

        b.btnPrev.setOnClickListener(v -> changeMonth(month.minusMonths(1)));
        b.btnNext.setOnClickListener(v -> changeMonth(month.plusMonths(1)));

        observeMonth();
    }

    private void changeMonth(YearMonth m) {
        month = m;
        observeMonth();
    }

    private void observeMonth() {
        if (monthSource != null) monthSource.removeObservers(getViewLifecycleOwner());
        bindMonthLabel();
        long from = startMillis(month.atDay(1));
        long to = startMillis(month.plusMonths(1).atDay(1));
        monthSource = taskDao.getDueAtInRange(from, to);
        monthSource.observe(getViewLifecycleOwner(), millis -> {
            dots = toSet(millis);
            rebuild();
        });
    }

    private long startMillis(LocalDate d) {
        return d.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    private Set<LocalDate> toSet(List<Long> millis) {
        Set<LocalDate> s = new HashSet<>();
        if (millis != null) {
            for (Long m : millis) {
                if (m != null) s.add(Instant.ofEpochMilli(m).atZone(zone).toLocalDate());
            }
        }
        return s;
    }

    private void bindMonthLabel() {
        String name = month.getMonth().getDisplayName(TextStyle.FULL, vi);
        b.monthLabel.setText(name + " " + month.getYear());
    }

    private void rebuild() {
        LocalDate today = LocalDate.now();
        List<CalendarCell> cells = new ArrayList<>();
        int lead = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < lead; i++) cells.add(new CalendarCell(null, false, false, false));
        int len = month.lengthOfMonth();
        for (int day = 1; day <= len; day++) {
            LocalDate date = month.atDay(day);
            cells.add(new CalendarCell(date, dots.contains(date),
                date.equals(selected), date.equals(today)));
        }
        adapter.submit(cells);
    }

    private void onDayPicked(LocalDate date) {
        Bundle result = new Bundle();
        result.putLong(RESULT_EPOCH_DAY, date.toEpochDay());
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
