# Tasks Date Filter (replace Calendar tab) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the (unmerged) Calendar tab and instead give the Tasks tab a "Chọn ngày" button that opens a custom month-calendar picker (with dots on days that have tasks); picking a day filters the task list to that day, with an × to clear.

**Architecture:** Work continues on branch `feat/calendar-tab`. Delete the calendar-tab-only pieces; keep the reusable `CalendarCell`/`CalendarDayAdapter`/`item_calendar_day.xml`/calendar drawables/`getDueAtInRange`/`getByDueRange`. Add `DatePickerDialogFragment` (a DialogFragment in `ui.calendar` reusing the grid, returning the picked date via FragmentResult). Extend `TasksViewModel` with a `dateFilter` dimension that (when set) replaces the topic filter.

**Tech Stack:** Java, Room, ViewBinding, RecyclerView (GridLayoutManager), java.time, Material Button, FragmentResult API.

## Global Constraints

- Package `com.example.todolist`; Java; `minSdk 34`, `targetSdk 36`. No new Gradle dependencies. Work on branch `feat/calendar-tab` (do NOT create a new branch).
- Week starts **Monday**; leading blanks = `atDay(1).getDayOfWeek().getValue() - 1` (Mon→0 … Sun→6).
- Date↔millis via `ZoneId.systemDefault()`; day range `[date, date.plusDays(1))`; month range `[ym.atDay(1), ym.plusMonths(1).atDay(1))` (half-open; NULL `dueAt` excluded). `startMillis(d) = d.atStartOfDay(zone).toInstant().toEpochMilli()`.
- Date filter and topic filter are **mutually exclusive**: setting a date leaves it as the active filter; `setFilter(topicId)` clears the date.
- Picker dots + day list include tasks whether `done` or not.
- `CalendarCell` and `CalendarDayAdapter` are package-private in `com.example.todolist.ui.calendar` — `DatePickerDialogFragment` MUST live in that same package to use them.
- Reuse existing palette/resources: colors `canvas_mist`, `charcoal_ink`, `muted_steel`, `muted_sage`, `whisper_border`, `pure_surface`, `white`; drawables `ic_calendar` (reused as button icon), `ic_chevron_left`, `ic_chevron_right`, `bg_calendar_selected/today/dot`, layout `item_calendar_day.xml`; strings `prev_month`, `next_month`. Do NOT modify widget/reminder/topics logic or the data model.
- Per user override: **no automated tests** unless requested. Each task's verification is a successful `assembleDebug` build plus the described manual check.

---

### Task 1: Remove the Calendar tab

**Files:**
- Delete: `app/src/main/java/com/example/todolist/ui/calendar/CalendarFragment.java`
- Delete: `app/src/main/java/com/example/todolist/ui/calendar/CalendarViewModel.java`
- Delete: `app/src/main/res/layout/fragment_calendar.xml`
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/java/com/example/todolist/MainActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Keep (do NOT delete — reused later):** `ui/calendar/CalendarCell.java`, `ui/calendar/CalendarDayAdapter.java`, `res/layout/item_calendar_day.xml`, `res/drawable/bg_calendar_selected.xml`, `bg_calendar_today.xml`, `bg_calendar_dot.xml`, `ic_chevron_left.xml`, `ic_calendar.xml`; `TaskDao.getDueAtInRange`/`getByDueRange`; strings `prev_month`, `next_month`.

**Interfaces:**
- Produces: a 3-tab app (Tasks · Topics · Settings). `CalendarCell`/`CalendarDayAdapter` remain but are temporarily unreferenced (compile fine).

- [ ] **Step 1: Delete the three calendar-tab files**

Delete `CalendarFragment.java`, `CalendarViewModel.java`, `fragment_calendar.xml` (via `git rm` or file delete). Do not touch other `ui/calendar` files.

- [ ] **Step 2: Remove `nav_calendar` from the menu**

In `app/src/main/res/menu/bottom_nav_menu.xml`, delete the entire `<item android:id="@+id/nav_calendar" .../>` block. Result: items are `nav_tasks`, `nav_topics`, `nav_settings` (in that order).

- [ ] **Step 3: Remove the calendar branch from `MainActivity`**

In `app/src/main/java/com/example/todolist/MainActivity.java`, delete the import `import com.example.todolist.ui.calendar.CalendarFragment;` and remove the entire `else if (id == R.id.nav_calendar) { show(new CalendarFragment()); return true; }` branch. Leave the `nav_tasks`/`nav_topics`/`nav_settings` branches intact.

- [ ] **Step 4: Remove now-unused strings**

In `app/src/main/res/values/strings.xml`, delete the `<string name="calendar">…</string>` and `<string name="day_empty">…</string>` entries (both were used only by the removed tab). Keep `prev_month` and `next_month`.

- [ ] **Step 5: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. No unresolved references to `CalendarFragment`, `nav_calendar`, `@string/calendar`, `@string/day_empty`, or `@layout/fragment_calendar`. (If the build reports `CalendarDayAdapter`/`CalendarCell` as errors, they should NOT — they are self-contained; only fix genuine dangling references left by the deletions.)

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: remove calendar tab (superseded by tasks date filter)"
```

---

### Task 2: Add date-filter dimension to TasksViewModel

**Files:**
- Modify: `app/src/main/java/com/example/todolist/data/TaskRepository.java`
- Modify: `app/src/main/java/com/example/todolist/ui/tasks/TasksViewModel.java`

**Interfaces:**
- Consumes: `TaskDao.getByDueRange(long,long)` (already exists).
- Produces:
  - `TaskRepository.getByDueRange(long from, long to) : LiveData<List<Task>>`.
  - `TasksViewModel.setDate(LocalDate)`, `clearDate()`, `getDate() : LiveData<LocalDate>`; `getTasks()` now reflects the date filter when set; `setFilter(Long)` also clears the date.

- [ ] **Step 1: Add `getByDueRange` passthrough to `TaskRepository`**

In `TaskRepository.java`, add next to `getAll()`/`getByTopic()`:

```java
public LiveData<List<Task>> getByDueRange(long from, long to) {
    return dao.getByDueRange(from, to);
}
```

- [ ] **Step 2: Rewrite `TasksViewModel` to add the date dimension**

Replace the field block + constructor and add the new methods. Add imports `java.time.LocalDate` and `java.time.ZoneId`. Full updated file:

```java
package com.example.todolist.ui.tasks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.todolist.data.Task;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.data.Topic;
import com.example.todolist.data.TopicRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class TasksViewModel extends AndroidViewModel {
    private final TaskRepository taskRepo;
    private final TopicRepository topicRepo;
    private final ZoneId zone = ZoneId.systemDefault();
    /** null = show all topics. */
    private final MutableLiveData<Long> filter = new MutableLiveData<>(null);
    /** null = no date filter (fall back to topic filter). */
    private final MutableLiveData<LocalDate> dateFilter = new MutableLiveData<>(null);
    private final LiveData<List<Task>> tasks;

    public TasksViewModel(@NonNull Application app) {
        super(app);
        taskRepo = new TaskRepository(app);
        topicRepo = new TopicRepository(app);

        LiveData<List<Task>> topicTasks = Transformations.switchMap(filter, id ->
            id == null ? taskRepo.getAll() : taskRepo.getByTopic(id));

        tasks = Transformations.switchMap(dateFilter, d ->
            d == null ? topicTasks
                      : taskRepo.getByDueRange(startMillis(d), startMillis(d.plusDays(1))));
    }

    private long startMillis(LocalDate d) {
        return d.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public LiveData<List<Task>> getTasks() { return tasks; }

    public LiveData<List<Topic>> getTopics() { return topicRepo.getAll(); }

    public void setFilter(Long topicId) {
        filter.setValue(topicId);
        dateFilter.setValue(null); // choosing a topic clears the date filter
    }

    public Long getFilter() { return filter.getValue(); }

    public void setDate(LocalDate d) { dateFilter.setValue(d); }

    public void clearDate() { dateFilter.setValue(null); }

    public LiveData<LocalDate> getDate() { return dateFilter; }

    public void insert(Task t, TaskRepository.OnId cb) { taskRepo.insert(t, cb); }

    public void update(Task t) { taskRepo.update(t); }

    public void delete(Task t) { taskRepo.delete(t); }

    public void getTaskById(long id, TaskRepository.OnTask cb) { taskRepo.getByIdAsync(id, cb); }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (Nested `switchMap`: a topic-filter change still propagates because `switchMap` forwards emissions from the currently-mapped source `topicTasks`.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/todolist/data/TaskRepository.java \
        app/src/main/java/com/example/todolist/ui/tasks/TasksViewModel.java
git commit -m "feat: add date-filter dimension to tasks view model"
```

---

### Task 3: DatePickerDialogFragment + dialog layout

**Files:**
- Create: `app/src/main/res/layout/dialog_date_picker.xml`
- Create: `app/src/main/java/com/example/todolist/ui/calendar/DatePickerDialogFragment.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `CalendarCell`, `CalendarDayAdapter` (same package); `R.layout.item_calendar_day`; `TaskDao.getDueAtInRange`; `AppDatabase.getInstance`; drawables `ic_chevron_left`/`ic_chevron_right`.
- Produces: `DatePickerDialogFragment` with public constants `REQUEST_KEY="date_picker"`, `RESULT_EPOCH_DAY="epoch_day"`, and `newInstance(LocalDate initial)`. On a day tap it calls `setFragmentResult(REQUEST_KEY, {RESULT_EPOCH_DAY: date.toEpochDay()})` on its parent FragmentManager, then dismisses.
- Strings: `pick_date`, `clear_date`.

- [ ] **Step 1: Add strings**

Add to `app/src/main/res/values/strings.xml`:

```xml
<string name="pick_date">Chọn ngày</string>
<string name="clear_date">Bỏ lọc ngày</string>
```

- [ ] **Step 2: Create `dialog_date_picker.xml`**

`app/src/main/res/layout/dialog_date_picker.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/pure_surface"
    android:padding="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingBottom="4dp">
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
        android:id="@+id/picker_grid"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</LinearLayout>
```

- [ ] **Step 3: Create `DatePickerDialogFragment.java`**

`app/src/main/java/com/example/todolist/ui/calendar/DatePickerDialogFragment.java`:

```java
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
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. ViewBinding generates `DialogDatePickerBinding` (`btnPrev`, `btnNext`, `monthLabel`, `pickerGrid`). `CalendarCell`/`CalendarDayAdapter` are now referenced again from the same package.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/dialog_date_picker.xml \
        app/src/main/java/com/example/todolist/ui/calendar/DatePickerDialogFragment.java \
        app/src/main/res/values/strings.xml
git commit -m "feat: month date picker dialog with task dots"
```

---

### Task 4: Wire the date picker into the Tasks tab

**Files:**
- Create: `app/src/main/res/drawable/ic_close.xml`
- Modify: `app/src/main/res/layout/fragment_tasks.xml`
- Modify: `app/src/main/java/com/example/todolist/ui/tasks/TasksFragment.java`

**Interfaces:**
- Consumes: `DatePickerDialogFragment` (Task 3); `TasksViewModel.setDate/clearDate/getDate` (Task 2); binding fields `btnPickDate`, `btnClearDate` from `fragment_tasks.xml`; strings `pick_date`, `clear_date`; drawables `ic_calendar`, `ic_close`.
- Produces: the full working feature.

- [ ] **Step 1: Create `ic_close.xml`**

`app/src/main/res/drawable/ic_close.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/muted_steel"
        android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"/>
</vector>
```

- [ ] **Step 2: Add the date-picker row to `fragment_tasks.xml`**

Insert this `LinearLayout` in `fragment_tasks.xml` BETWEEN the `count` TextView (ends at line ~32) and the `HorizontalScrollView` (starts at line ~34) — i.e., as a sibling directly after the `count` TextView inside the vertical `LinearLayout`:

```xml
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:paddingHorizontal="20dp"
            android:paddingBottom="8dp">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_pick_date"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/pick_date"
                android:textColor="@color/muted_sage"
                app:icon="@drawable/ic_calendar"
                app:iconTint="@color/muted_sage"
                app:strokeColor="@color/whisper_border" />

            <ImageView
                android:id="@+id/btn_clear_date"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:layout_marginStart="4dp"
                android:padding="8dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/clear_date"
                android:src="@drawable/ic_close"
                android:visibility="gone" />
        </LinearLayout>
```

Ensure `xmlns:app="http://schemas.android.com/apk/res-auto"` is present on the root (it already is). Do not change any existing view.

- [ ] **Step 3: Wire `TasksFragment`**

In `TasksFragment.java`, add imports `import java.time.LocalDate;` and `import com.example.todolist.ui.calendar.DatePickerDialogFragment;`. At the end of `onViewCreated` (after the existing FAB/empty-add listeners), add:

```java
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
```

(The existing chip listener calls `viewModel.setFilter(...)`, which clears the date; the `getDate()` observer above then resets the button — so no extra chip code is needed.)

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. ViewBinding exposes `btnPickDate` (MaterialButton) and `btnClearDate` (ImageView).

- [ ] **Step 5: Manual verify**

Launch app. BottomNav has 3 tabs (no Calendar). In Tasks, tap **Chọn ngày** → the month dialog opens; days with tasks show a dot; `‹ ›` change month. Tap a day → dialog closes, list shows only that day's tasks (done included), the button now reads e.g. "26/7" with an × beside it. Tap × → full list returns, button reads "Chọn ngày". With a date active, tap a topic chip → date clears and the list filters by topic (button resets). Add/edit/delete a task, reopen the picker → dots reflect the change.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/drawable/ic_close.xml \
        app/src/main/res/layout/fragment_tasks.xml \
        app/src/main/java/com/example/todolist/ui/tasks/TasksFragment.java
git commit -m "feat: date-picker button and date filtering in tasks tab"
```

---

## Self-Review

**Spec coverage:**
- Remove Calendar tab (files + nav + strings) → Task 1. ✓
- Keep reusable calendar pieces + DAO queries → Task 1 (keep list). ✓
- `DatePickerDialogFragment` in `ui.calendar` with dots, month nav, FragmentResult → Task 3. ✓
- Tasks tab button + × + open picker + apply/clear date + button synced via `getDate()` → Task 4. ✓
- `TasksViewModel` date dimension; date replaces topic filter; `setFilter` clears date → Task 2. ✓
- Reuse `ic_calendar` as button icon; new `ic_close` for × → Tasks 1 (keep) + 4. ✓
- Manual-test only (no automated tests) → each task's verify step. ✓

**Placeholder scan:** No TBD/TODO; all code steps complete. ✓

**Type consistency:** `TaskRepository.getByDueRange(long,long):LiveData<List<Task>>` defined Task 2, consumed by `TasksViewModel` same task; `TaskDao.getByDueRange`/`getDueAtInRange` pre-exist (kept in Task 1). `DatePickerDialogFragment.REQUEST_KEY`/`RESULT_EPOCH_DAY`/`newInstance` defined Task 3, consumed Task 4. `setDate`/`clearDate`/`getDate` defined Task 2, consumed Task 4. Binding fields `btnPickDate`/`btnClearDate` (ids `btn_pick_date`/`btn_clear_date`) match layout + code; `btnPrev`/`btnNext`/`monthLabel`/`pickerGrid` (ids `btn_prev`/`btn_next`/`month_label`/`picker_grid`) match dialog layout + fragment. `CalendarDayAdapter(OnDayClick)` + `submit(List<CalendarCell>)` + `CalendarCell(date,hasTask,selected,today)` match the reused classes. FragmentResult: dialog `show(getChildFragmentManager())` ↔ listener on `getChildFragmentManager()` ↔ dialog `getParentFragmentManager().setFragmentResult(...)` (same manager). ✓

**Note (minor, non-blocking):** while a date filter is active, a topic chip may still appear visually checked (chip UI state isn't reset), though the list shows date results. Tapping any chip clears the date and re-applies topic filtering. Acceptable per KISS.
