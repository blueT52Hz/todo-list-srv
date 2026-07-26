package com.example.todolist.ui.calendar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.Task;
import com.example.todolist.data.TaskDao;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.data.Topic;
import com.example.todolist.data.TopicDao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarViewModel extends AndroidViewModel {

    private final TaskDao taskDao;
    private final TopicDao topicDao;
    private final TaskRepository repo;
    private final ZoneId zone = ZoneId.systemDefault();

    private final MutableLiveData<YearMonth> month = new MutableLiveData<>(YearMonth.now());
    private final MutableLiveData<LocalDate> selected = new MutableLiveData<>(LocalDate.now());

    private final LiveData<Set<LocalDate>> daysWithTasks;
    private final LiveData<List<Task>> dayTasks;

    public CalendarViewModel(@NonNull Application app) {
        super(app);
        AppDatabase db = AppDatabase.getInstance(app);
        taskDao = db.taskDao();
        topicDao = db.topicDao();
        repo = new TaskRepository(app);

        daysWithTasks = Transformations.switchMap(month, ym ->
            Transformations.map(
                taskDao.getDueAtInRange(startMillis(ym.atDay(1)),
                                        startMillis(ym.plusMonths(1).atDay(1))),
                this::toLocalDateSet));

        dayTasks = Transformations.switchMap(selected, d ->
            taskDao.getByDueRange(startMillis(d), startMillis(d.plusDays(1))));
    }

    private long startMillis(LocalDate d) {
        return d.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    private Set<LocalDate> toLocalDateSet(List<Long> millis) {
        Set<LocalDate> set = new HashSet<>();
        if (millis != null) {
            for (Long m : millis) {
                if (m != null) set.add(Instant.ofEpochMilli(m).atZone(zone).toLocalDate());
            }
        }
        return set;
    }

    public LiveData<YearMonth> getMonth() { return month; }
    public LiveData<LocalDate> getSelected() { return selected; }
    public LiveData<Set<LocalDate>> getDaysWithTasks() { return daysWithTasks; }
    public LiveData<List<Task>> getDayTasks() { return dayTasks; }
    public LiveData<List<Topic>> getTopics() { return topicDao.getAll(); }

    public void prevMonth() {
        YearMonth m = month.getValue();
        month.setValue((m == null ? YearMonth.now() : m).minusMonths(1));
    }

    public void nextMonth() {
        YearMonth m = month.getValue();
        month.setValue((m == null ? YearMonth.now() : m).plusMonths(1));
    }

    public void select(LocalDate d) { selected.setValue(d); }

    public void update(Task t) { repo.update(t); }
}
