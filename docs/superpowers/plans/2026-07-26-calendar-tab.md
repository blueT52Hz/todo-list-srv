# Calendar Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Calendar tab (4th BottomNav item) to the existing Tasca Java Android To-do app: a custom month-grid calendar that dots days having tasks (by `dueAt`), and shows the tapped day's task list right below the grid.

**Architecture:** Custom `RecyclerView` + `GridLayoutManager(7)` month grid (no calendar library). `CalendarViewModel` holds `month` (YearMonth) and `selected` (LocalDate) as LiveData; `daysWithTasks` (Set<LocalDate> for dots) and `dayTasks` (List<Task>) derive via Transformations from two new range queries. `CalendarFragment` observes and rebuilds the cell list; the day list reuses the existing `TaskAdapter`. Uses `java.time` (minSdk 34).

**Tech Stack:** Java, Room, ViewBinding, RecyclerView (GridLayoutManager), java.time, existing TaskAdapter.

## Global Constraints

- Package `com.example.todolist`; Java; `minSdk 34`, `targetSdk 36`. No login. No new Gradle dependencies (custom grid).
- Week starts **Monday** (order: T2 T3 T4 T5 T6 T7 CN). Use ISO `DayOfWeek.getValue()` (Mon=1…Sun=7); leading blanks = `value - 1`.
- Calendar dots + day list include tasks whether `done` or not. Tasks with `dueAt == null` never appear on the calendar.
- Date↔millis conversion uses `ZoneId.systemDefault()`: day start = `date.atStartOfDay(zone).toInstant().toEpochMilli()`; a day range is `[dayStart, nextDayStart)`; a month range is `[ym.atDay(1) start, ym.plusMonths(1).atDay(1) start)`.
- Task-open intent extra key is `"task_id"` (matches `TaskDetailActivity.getLongExtra("task_id",-1)`).
- Toggling done follows the existing pattern: `task.done = !task.done; viewModel.update(task); ReminderScheduler.schedule(ctx, task);`.
- Reuse existing resources/palette: colors `canvas_mist`, `charcoal_ink`, `muted_steel`, `muted_sage`, `whisper_border`, `white`; reuse `TaskAdapter` (ui.tasks), `DateUtils`, `ic_chevron_right` (exists). Do NOT modify existing Tasks/Topics/Settings/widget/reminder logic.
- Nav is id-based (not order-based); inserting `nav_calendar` before `nav_settings` in the menu is safe.
- Per user override: **no automated tests** unless requested. Each task's verification is a successful `assembleDebug` build plus the described manual check.

---

### Task 1: Range queries + calendar resources

**Files:**
- Modify: `app/src/main/java/com/example/todolist/data/TaskDao.java`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_calendar.xml`
- Create: `app/src/main/res/drawable/ic_chevron_left.xml`
- Create: `app/src/main/res/drawable/bg_calendar_selected.xml`
- Create: `app/src/main/res/drawable/bg_calendar_today.xml`
- Create: `app/src/main/res/drawable/bg_calendar_dot.xml`
- Create: `app/src/main/res/layout/item_calendar_day.xml`
- Create: `app/src/main/res/layout/fragment_calendar.xml`

**Interfaces:**
- Produces:
  - `TaskDao.getDueAtInRange(long from, long to) : LiveData<List<Long>>`
  - `TaskDao.getByDueRange(long from, long to) : LiveData<List<Task>>`
  - Layout ids: `btn_prev`, `btn_next`, `month_label`, `calendar_grid` (RecyclerView), `day_title`, `day_tasks` (RecyclerView), `day_empty`; item ids `day_cell`, `day_number`, `day_dot`.
  - Strings: `calendar`, `prev_month`, `next_month`, `day_empty`.
  - Drawables: `ic_calendar`, `ic_chevron_left`, `bg_calendar_selected`, `bg_calendar_today`, `bg_calendar_dot`.

- [ ] **Step 1: Add the two range queries to `TaskDao`**

Add inside the `TaskDao` interface (near the other `@Query` methods):

```java
@Query("SELECT dueAt FROM tasks WHERE dueAt >= :from AND dueAt < :to")
LiveData<List<Long>> getDueAtInRange(long from, long to);

@Query("SELECT * FROM tasks WHERE dueAt >= :from AND dueAt < :to ORDER BY dueAt ASC")
LiveData<List<Task>> getByDueRange(long from, long to);
```

- [ ] **Step 2: Add strings**

Add to `app/src/main/res/values/strings.xml` (inside `<resources>`):

```xml
<string name="calendar">Calendar</string>
<string name="prev_month">Tháng trước</string>
<string name="next_month">Tháng sau</string>
<string name="day_empty">Không có việc nào</string>
```

- [ ] **Step 3: Create `ic_calendar.xml`**

`app/src/main/res/drawable/ic_calendar.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/charcoal_ink"
        android:pathData="M19,4h-1V2h-2v2H8V2H6v2H5C3.89,4 3,4.9 3,6v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V6C21,4.9 20.1,4 19,4zM19,20H5V10h14V20zM19,8H5V6h14V8z"/>
</vector>
```

- [ ] **Step 4: Create `ic_chevron_left.xml`**

`app/src/main/res/drawable/ic_chevron_left.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/charcoal_ink"
        android:pathData="M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z"/>
</vector>
```

- [ ] **Step 5: Create `bg_calendar_selected.xml`**

`app/src/main/res/drawable/bg_calendar_selected.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@color/muted_sage" />
</shape>
```

- [ ] **Step 6: Create `bg_calendar_today.xml`**

`app/src/main/res/drawable/bg_calendar_today.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@android:color/transparent" />
    <stroke android:width="1.5dp" android:color="@color/muted_sage" />
</shape>
```

- [ ] **Step 7: Create `bg_calendar_dot.xml`**

`app/src/main/res/drawable/bg_calendar_dot.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@color/muted_sage" />
</shape>
```

- [ ] **Step 8: Create `item_calendar_day.xml`**

`app/src/main/res/layout/item_calendar_day.xml` (square-ish cell; day_cell is the 40dp circle target):

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="48dp">

    <LinearLayout
        android:id="@+id/day_cell"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:layout_gravity="center"
        android:orientation="vertical"
        android:gravity="center">

        <TextView
            android:id="@+id/day_number"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/charcoal_ink"
            android:textSize="14sp" />

        <View
            android:id="@+id/day_dot"
            android:layout_width="5dp"
            android:layout_height="5dp"
            android:layout_marginTop="2dp"
            android:background="@drawable/bg_calendar_dot"
            android:visibility="invisible" />
    </LinearLayout>
</FrameLayout>
```

- [ ] **Step 9: Create `fragment_calendar.xml`**

`app/src/main/res/layout/fragment_calendar.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/canvas_mist"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingBottom="24dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:paddingHorizontal="12dp"
            android:paddingTop="20dp"
            android:paddingBottom="8dp">
            <ImageView
                android:id="@+id/btn_prev"
                android:layout_width="44dp"
                android:layout_height="44dp"
                android:padding="10dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:src="@drawable/ic_chevron_left"
                android:contentDescription="@string/prev_month" />
            <TextView
                android:id="@+id/month_label"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:gravity="center"
                android:textColor="@color/charcoal_ink"
                android:textSize="18sp"
                android:textStyle="bold" />
            <ImageView
                android:id="@+id/btn_next"
                android:layout_width="44dp"
                android:layout_height="44dp"
                android:padding="10dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:src="@drawable/ic_chevron_right"
                android:contentDescription="@string/next_month" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:paddingHorizontal="8dp"
            android:paddingBottom="4dp">
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:fontFamily="monospace" android:textColor="@color/muted_steel" android:textSize="12sp" android:text="T2" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:fontFamily="monospace" android:textColor="@color/muted_steel" android:textSize="12sp" android:text="T3" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:fontFamily="monospace" android:textColor="@color/muted_steel" android:textSize="12sp" android:text="T4" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:fontFamily="monospace" android:textColor="@color/muted_steel" android:textSize="12sp" android:text="T5" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:fontFamily="monospace" android:textColor="@color/muted_steel" android:textSize="12sp" android:text="T6" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:fontFamily="monospace" android:textColor="@color/muted_steel" android:textSize="12sp" android:text="T7" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:fontFamily="monospace" android:textColor="@color/muted_steel" android:textSize="12sp" android:text="CN" />
        </LinearLayout>

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/calendar_grid"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingHorizontal="8dp"
            android:nestedScrollingEnabled="false" />

        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:layout_marginTop="8dp"
            android:layout_marginHorizontal="20dp"
            android:background="@color/whisper_border" />

        <TextView
            android:id="@+id/day_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:paddingHorizontal="20dp"
            android:paddingTop="16dp"
            android:paddingBottom="4dp"
            android:textColor="@color/charcoal_ink"
            android:textSize="15sp"
            android:textStyle="bold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/day_tasks"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:nestedScrollingEnabled="false" />

        <TextView
            android:id="@+id/day_empty"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:paddingVertical="32dp"
            android:text="@string/day_empty"
            android:textColor="@color/muted_steel"
            android:visibility="gone" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
```

- [ ] **Step 10: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Room processes the two new queries; new resources compile).

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/example/todolist/data/TaskDao.java \
        app/src/main/res/values/strings.xml \
        app/src/main/res/drawable/ic_calendar.xml app/src/main/res/drawable/ic_chevron_left.xml \
        app/src/main/res/drawable/bg_calendar_selected.xml app/src/main/res/drawable/bg_calendar_today.xml \
        app/src/main/res/drawable/bg_calendar_dot.xml \
        app/src/main/res/layout/item_calendar_day.xml app/src/main/res/layout/fragment_calendar.xml
git commit -m "feat: add calendar range queries and calendar resources"
```

---

### Task 2: Calendar cell model + grid adapter

**Files:**
- Create: `app/src/main/java/com/example/todolist/ui/calendar/CalendarCell.java`
- Create: `app/src/main/java/com/example/todolist/ui/calendar/CalendarDayAdapter.java`

**Interfaces:**
- Consumes: `R.layout.item_calendar_day` and its ids (Task 1); drawables `bg_calendar_selected`/`bg_calendar_today` (Task 1); colors `white`, `muted_sage`, `charcoal_ink`.
- Produces:
  - `CalendarCell(LocalDate date, boolean hasTask, boolean selected, boolean today)` — `date == null` means a blank leading cell.
  - `CalendarDayAdapter` with `void submit(List<CalendarCell>)` and constructor `CalendarDayAdapter(CalendarDayAdapter.OnDayClick)`; `interface OnDayClick { void onDay(LocalDate date); }`.

- [ ] **Step 1: Create `CalendarCell.java`**

```java
package com.example.todolist.ui.calendar;

import java.time.LocalDate;

/** One cell in the month grid. date == null → blank leading cell (no day). */
class CalendarCell {
    final LocalDate date;
    final boolean hasTask;
    final boolean selected;
    final boolean today;

    CalendarCell(LocalDate date, boolean hasTask, boolean selected, boolean today) {
        this.date = date;
        this.hasTask = hasTask;
        this.selected = selected;
        this.today = today;
    }
}
```

- [ ] **Step 2: Create `CalendarDayAdapter.java`**

```java
package com.example.todolist.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.VH> {

    interface OnDayClick { void onDay(LocalDate date); }

    private final List<CalendarCell> cells = new ArrayList<>();
    private final OnDayClick clickListener;

    CalendarDayAdapter(OnDayClick clickListener) { this.clickListener = clickListener; }

    void submit(List<CalendarCell> newCells) {
        cells.clear();
        cells.addAll(newCells);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_calendar_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CalendarCell c = cells.get(position);
        if (c.date == null) {
            h.number.setText("");
            h.dot.setVisibility(View.INVISIBLE);
            h.cell.setBackground(null);
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);
            return;
        }
        h.number.setText(String.valueOf(c.date.getDayOfMonth()));
        h.dot.setVisibility(c.hasTask && !c.selected ? View.VISIBLE : View.INVISIBLE);

        int textColor;
        if (c.selected) {
            h.cell.setBackgroundResource(R.drawable.bg_calendar_selected);
            textColor = R.color.white;
        } else if (c.today) {
            h.cell.setBackgroundResource(R.drawable.bg_calendar_today);
            textColor = R.color.muted_sage;
        } else {
            h.cell.setBackground(null);
            textColor = R.color.charcoal_ink;
        }
        h.number.setTextColor(ContextCompat.getColor(h.itemView.getContext(), textColor));
        h.itemView.setOnClickListener(v -> clickListener.onDay(c.date));
    }

    @Override public int getItemCount() { return cells.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final View cell, dot;
        final TextView number;
        VH(@NonNull View v) {
            super(v);
            cell = v.findViewById(R.id.day_cell);
            number = v.findViewById(R.id.day_number);
            dot = v.findViewById(R.id.day_dot);
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Confirm `R.color.white` exists in `res/values/colors.xml` (it does — palette includes black/white); if the color name differs, use the actual white color name from colors.xml.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/todolist/ui/calendar/CalendarCell.java \
        app/src/main/java/com/example/todolist/ui/calendar/CalendarDayAdapter.java
git commit -m "feat: calendar cell model and month grid adapter"
```

---

### Task 3: CalendarViewModel

**Files:**
- Create: `app/src/main/java/com/example/todolist/ui/calendar/CalendarViewModel.java`

**Interfaces:**
- Consumes: `TaskDao.getDueAtInRange`, `TaskDao.getByDueRange` (Task 1); `AppDatabase.getInstance(app)`; `TaskRepository(Context)` with `update(Task)`; `TopicDao.getAll()`.
- Produces: `CalendarViewModel` (AndroidViewModel) with getters `getMonth()`, `getSelected()`, `getDaysWithTasks()`, `getDayTasks()`, `getTopics()`, and actions `prevMonth()`, `nextMonth()`, `select(LocalDate)`, `update(Task)`.

- [ ] **Step 1: Create `CalendarViewModel.java`**

```java
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
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (`Transformations.switchMap`/`map` are the same APIs `TasksViewModel` already uses.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/todolist/ui/calendar/CalendarViewModel.java
git commit -m "feat: calendar view model with month and day-selection state"
```

---

### Task 4: CalendarFragment + nav wiring

**Files:**
- Create: `app/src/main/java/com/example/todolist/ui/calendar/CalendarFragment.java`
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/java/com/example/todolist/MainActivity.java`

**Interfaces:**
- Consumes: `CalendarViewModel` (Task 3); `CalendarCell`, `CalendarDayAdapter` (Task 2); `FragmentCalendarBinding` from `fragment_calendar.xml` (Task 1) — ViewBinding fields `btnPrev`, `btnNext`, `monthLabel`, `calendarGrid`, `dayTitle`, `dayTasks`, `dayEmpty`; `TaskAdapter` + `TaskAdapter.Listener` (ui.tasks); `ReminderScheduler.schedule`; `TaskDetailActivity`; `R.id.nav_host`, `binding.bottomNav`, `show(Fragment)` in `MainActivity`; `ic_calendar` + `@string/calendar` (Task 1).
- Produces: `CalendarFragment`, nav item `R.id.nav_calendar`.

- [ ] **Step 1: Create `CalendarFragment.java`**

```java
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
```

- [ ] **Step 2: Add nav item**

In `app/src/main/res/menu/bottom_nav_menu.xml`, add BEFORE the `nav_settings` item (so order is Tasks · Topics · Calendar · Settings):

```xml
<item
    android:id="@+id/nav_calendar"
    android:icon="@drawable/ic_calendar"
    android:title="@string/calendar" />
```

- [ ] **Step 3: Wire the nav branch in `MainActivity`**

Add the import `import com.example.todolist.ui.calendar.CalendarFragment;`. In the `setOnItemSelectedListener` block, add a branch (before the closing `return false;`):

```java
            } else if (id == R.id.nav_calendar) {
                show(new CalendarFragment());
                return true;
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. ViewBinding generates `FragmentCalendarBinding` with `btnPrev`, `btnNext`, `monthLabel`, `calendarGrid`, `dayTitle`, `dayTasks`, `dayEmpty`.

- [ ] **Step 5: Manual verify**

Launch app → tap **Calendar** tab. Current month shows; today has the ring; days with due tasks show a dot; today is selected and its tasks list below. Tap another day → list updates (done tasks show struck-through). Tap `‹`/`›` → month changes, dots track the month. Tap a task → opens `TaskDetailActivity`. Tick a task done → it updates. A task with no due date never appears.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/todolist/ui/calendar/CalendarFragment.java \
        app/src/main/res/menu/bottom_nav_menu.xml \
        app/src/main/java/com/example/todolist/MainActivity.java
git commit -m "feat: calendar tab with month grid and per-day task list"
```

---

## Self-Review

**Spec coverage:**
- 4th nav tab Calendar (menu + MainActivity branch) → Task 4. ✓
- Month grid, week starts Monday, leading blanks = `getValue()-1` → Task 4 `rebuildGrid`. ✓
- Dot on days with tasks (incl. done) → `getDueAtInRange` (Task 1) → `daysWithTasks` Set (Task 3) → cell `hasTask` (Task 4). ✓
- Tap day → list below (incl. done) → `getByDueRange` (Task 1) → `dayTasks` (Task 3) → reused `TaskAdapter` (Task 4). ✓
- Selected/today/dot styling → drawables + adapter (Tasks 1–2). ✓
- Month nav arrows keep selection → `prevMonth`/`nextMonth` don't touch `selected` (Task 3). ✓
- Open detail via `task_id`; toggle done pattern → Task 4. ✓
- Tasks without dueAt excluded → range queries filter `dueAt >= from` (NULL excluded). ✓
- No new dependency; no changes to existing feature logic. ✓

**Placeholder scan:** No TBD/TODO; every code step is complete. ✓

**Type consistency:** `getDueAtInRange`/`getByDueRange` defined Task 1, used Task 3. `CalendarCell(date,hasTask,selected,today)` + `CalendarDayAdapter.submit`/`OnDayClick` defined Task 2, used Task 4. `CalendarViewModel` getters/actions defined Task 3, used Task 4. Binding fields (`btnPrev`,`btnNext`,`monthLabel`,`calendarGrid`,`dayTitle`,`dayTasks`,`dayEmpty`) match layout ids (`btn_prev`,`btn_next`,`month_label`,`calendar_grid`,`day_title`,`day_tasks`,`day_empty`). Item ids (`day_cell`,`day_number`,`day_dot`) match adapter VH lookups. `TaskAdapter.Listener{onOpen,onToggle}` matches reused adapter. ✓
