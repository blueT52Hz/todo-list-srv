# Category Filter Dropdown Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thay bộ lọc category (Topic) ở tab Tasks từ chip chọn-đơn sang BottomSheet chọn-nhiều (search + checkbox + Áp dụng), lọc OR theo nhiều category và kết hợp AND với bộ lọc ngày.

**Architecture:** Thêm 1 query Room gộp `getFiltered(hasDate, from, to, hasCats, cats)` lọc đồng thời khoảng ngày + `topicId IN (...)` bằng cờ. `TasksViewModel` gộp 2 nguồn (dateFilter, categories) qua `MediatorLiveData<FilterState>` → `switchMap` sang query. UI là `CategoryFilterBottomSheet` (BottomSheetDialogFragment) trả tập id đã chọn về `TasksFragment` qua FragmentResult; Fragment thay chip bằng nút "Danh mục ▾".

**Tech Stack:** Java, Android, Room, LiveData/ViewModel, ViewBinding, Material Components, RecyclerView, BottomSheetDialogFragment.

## Global Constraints

- Ngôn ngữ Java; minSdk 34; ViewBinding; Room; Material Components. Theo pattern sẵn có trong `com.example.todolist`.
- "category" = entity **Topic** (`topics` table). Tạo/sửa category KHÔNG đụng (vẫn ở tab Topics).
- **KHÔNG viết unit test** (override của user). Verify mỗi task = biên dịch `./gradlew assembleDebug` (chạy trong subagent). Runtime test thủ công ở Task 3.
- **Empty-IN workaround:** SQLite lỗi với `IN ()`. Khi `hasCats = 0`, truyền `cats = Collections.singletonList(-1L)`; cờ `hasCats = 0` vô hiệu mệnh đề IN.
- Lọc ngày × category = **kết hợp AND**; trong chiều category là OR (`topicId IN`).
- FragmentResult: sheet show qua `getChildFragmentManager()`; kết quả gửi bằng `getParentFragmentManager().setFragmentResult(...)` và listener đăng ký trên `getChildFragmentManager()` của Fragment (cùng 1 manager) — giống `DatePickerDialogFragment`.
- `CategoryFilterBottomSheet` + `CategoryCheckAdapter` đặt trong package `com.example.todolist.ui.tasks`.
- Bash/git/gradle chỉ chạy qua subagent. Commit **local**, KHÔNG push.
- Commit message: conventional commits, không nhắc AI.

---

### Task 1: Data layer — query lọc gộp `getFiltered`

**Files:**
- Modify: `app/src/main/java/com/example/todolist/data/TaskDao.java`
- Modify: `app/src/main/java/com/example/todolist/data/TaskRepository.java`

**Interfaces:**
- Consumes: `Task` entity (fields `dueAt: Long`, `topicId: Long`, `done`, `createdAt`), existing `AppDatabase`, `TaskDao`.
- Produces:
  - `TaskDao.getFiltered(int hasDate, long from, long to, int hasCats, List<Long> cats): LiveData<List<Task>>`
  - `TaskRepository.getFiltered(int hasDate, long from, long to, int hasCats, List<Long> cats): LiveData<List<Task>>`

**Ghi chú:** Chỉ THÊM method mới ở task này; KHÔNG xoá `getByTopic`/`getByDueRange` (còn caller trong `TasksViewModel` cho tới Task 3). Giữ nguyên `getDueAtInRange`.

- [ ] **Step 1: Thêm query `getFiltered` vào `TaskDao`**

Thêm vào interface `TaskDao` (đặt cạnh `getByDueRange`):

```java
@Query("SELECT * FROM tasks " +
       "WHERE (:hasDate = 0 OR (dueAt >= :from AND dueAt < :to)) " +
       "AND (:hasCats = 0 OR topicId IN (:cats)) " +
       "ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
LiveData<List<Task>> getFiltered(int hasDate, long from, long to, int hasCats, List<Long> cats);
```

- [ ] **Step 2: Thêm wrapper vào `TaskRepository`**

Thêm method (đặt cạnh `getByDueRange`):

```java
public LiveData<List<Task>> getFiltered(int hasDate, long from, long to, int hasCats, List<Long> cats) {
    return dao.getFiltered(hasDate, from, to, hasCats, cats);
}
```

- [ ] **Step 3: Biên dịch kiểm tra Room annotation processing**

Chạy (qua subagent): `./gradlew :app:compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL. Room sinh implementation cho `getFiltered` không lỗi cú pháp SQL. Nếu lỗi bind `IN`, xác nhận signature dùng `List<Long> cats`.

- [ ] **Step 4: Commit (local)**

```bash
git add app/src/main/java/com/example/todolist/data/TaskDao.java \
        app/src/main/java/com/example/todolist/data/TaskRepository.java
git commit -m "feat(data): add combined date+category task query"
```

---

### Task 2: `CategoryFilterBottomSheet` + adapter + resource

**Files:**
- Create: `app/src/main/java/com/example/todolist/ui/tasks/CategoryCheckAdapter.java`
- Create: `app/src/main/java/com/example/todolist/ui/tasks/CategoryFilterBottomSheet.java`
- Create: `app/src/main/res/layout/item_category_check.xml`
- Create: `app/src/main/res/layout/bottomsheet_category_filter.xml`
- Create: `app/src/main/res/drawable/ic_search.xml`
- Create: `app/src/main/res/drawable/ic_arrow_drop_down.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `Topic` (`id`, `name`, `colorHex`), `TopicRepository.getAll(): LiveData<List<Topic>>`, drawable `bg_topic_dot`, color `charcoal_ink`/`muted_steel`/`muted_sage`/`pure_surface`/`canvas_mist`.
- Produces:
  - `CategoryCheckAdapter` với `setItems(List<Topic>)`, `setChecked(Set<Long>)`, `filter(String)`, `clearChecks()`, `getChecked(): Set<Long>`.
  - `CategoryFilterBottomSheet` với `public static final String REQUEST_KEY = "category_filter"`, `RESULT_IDS = "selected_ids"`, `newInstance(Set<Long> selected)`. Kết quả trả về là `long[]` dưới key `RESULT_IDS`.

**Ghi chú:** Task này chỉ tạo file mới + thêm string/drawable → không đụng wiring cũ, biên dịch độc lập.

- [ ] **Step 1: Thêm strings**

Thêm vào `app/src/main/res/values/strings.xml` (trong `<resources>`):

```xml
<string name="category">Danh mục</string>
<string name="category_count">Danh mục (%d)</string>
<string name="search_category">Tìm danh mục...</string>
<string name="apply">Áp dụng</string>
<string name="clear_selection">Bỏ chọn</string>
<string name="clear_category">Bỏ lọc danh mục</string>
<string name="no_category_hint">Chưa có danh mục — tạo ở tab Topics</string>
```

- [ ] **Step 2: Thêm drawable `ic_search.xml`**

`app/src/main/res/drawable/ic_search.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="@color/muted_steel">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z" />
</vector>
```

- [ ] **Step 3: Thêm drawable `ic_arrow_drop_down.xml`**

`app/src/main/res/drawable/ic_arrow_drop_down.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="@color/muted_sage">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M7,10l5,5 5,-5z" />
</vector>
```

- [ ] **Step 4: Tạo layout dòng `item_category_check.xml`**

`app/src/main/res/layout/item_category_check.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingHorizontal="20dp"
    android:paddingVertical="10dp"
    android:background="?attr/selectableItemBackground">

    <CheckBox
        android:id="@+id/check"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:clickable="false"
        android:focusable="false" />

    <View
        android:id="@+id/dot"
        android:layout_width="10dp"
        android:layout_height="10dp"
        android:layout_marginStart="8dp"
        android:layout_marginEnd="12dp"
        android:background="@drawable/bg_topic_dot" />

    <TextView
        android:id="@+id/name"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textColor="@color/charcoal_ink"
        android:textSize="16sp" />
</LinearLayout>
```

- [ ] **Step 5: Tạo layout sheet `bottomsheet_category_filter.xml`**

`app/src/main/res/layout/bottomsheet_category_filter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/canvas_mist"
    android:paddingTop="16dp"
    android:paddingBottom="12dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        android:paddingBottom="12dp"
        android:text="@string/category"
        android:textColor="@color/charcoal_ink"
        android:textSize="18sp"
        android:textStyle="bold" />

    <EditText
        android:id="@+id/search"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="20dp"
        android:drawableStart="@drawable/ic_search"
        android:drawablePadding="8dp"
        android:hint="@string/search_category"
        android:inputType="text"
        android:maxLines="1"
        android:importantForAutofill="no"
        android:textColor="@color/charcoal_ink"
        android:textColorHint="@color/muted_steel" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_categories"
        android:layout_width="match_parent"
        android:layout_height="320dp"
        android:layout_marginTop="8dp"
        android:clipToPadding="false" />

    <TextView
        android:id="@+id/empty_hint"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="24dp"
        android:gravity="center"
        android:text="@string/no_category_hint"
        android:textColor="@color/muted_steel"
        android:visibility="gone" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="end"
        android:paddingHorizontal="12dp"
        android:paddingTop="8dp">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_clear"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/clear_selection"
            android:textColor="@color/muted_steel" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_apply"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:text="@string/apply"
            android:textColor="@color/pure_surface"
            app:backgroundTint="@color/muted_sage"
            app:cornerRadius="20dp" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 6: Tạo `CategoryCheckAdapter.java`**

`app/src/main/java/com/example/todolist/ui/tasks/CategoryCheckAdapter.java`:

```java
package com.example.todolist.ui.tasks;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.data.Topic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Checkable category list with in-memory name search; checked-state is independent of the search query. */
public class CategoryCheckAdapter extends RecyclerView.Adapter<CategoryCheckAdapter.VH> {

    private final List<Topic> full = new ArrayList<>();
    private final List<Topic> shown = new ArrayList<>();
    private final Set<Long> checked = new HashSet<>();
    private String query = "";

    public void setItems(List<Topic> items) {
        full.clear();
        if (items != null) full.addAll(items);
        applyFilter();
    }

    public void setChecked(Set<Long> ids) {
        checked.clear();
        if (ids != null) checked.addAll(ids);
        notifyDataSetChanged();
    }

    public void filter(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    private void applyFilter() {
        shown.clear();
        for (Topic t : full) {
            if (query.isEmpty() || t.name.toLowerCase(Locale.getDefault()).contains(query)) {
                shown.add(t);
            }
        }
        notifyDataSetChanged();
    }

    public void clearChecks() {
        checked.clear();
        notifyDataSetChanged();
    }

    public Set<Long> getChecked() {
        return new HashSet<>(checked);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_category_check, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Topic t = shown.get(position);
        h.name.setText(t.name);
        // detach listener before setChecked to avoid spurious callbacks during recycle
        h.box.setOnCheckedChangeListener(null);
        h.box.setChecked(checked.contains(t.id));
        h.box.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) checked.add(t.id); else checked.remove(t.id);
        });
        try { h.dot.getBackground().setTint(Color.parseColor(t.colorHex)); }
        catch (IllegalArgumentException ignored) { }
        h.itemView.setOnClickListener(v -> h.box.toggle());
    }

    @Override
    public int getItemCount() { return shown.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final CheckBox box; final View dot; final TextView name;
        VH(@NonNull View v) {
            super(v);
            box = v.findViewById(R.id.check);
            dot = v.findViewById(R.id.dot);
            name = v.findViewById(R.id.name);
        }
    }
}
```

- [ ] **Step 7: Tạo `CategoryFilterBottomSheet.java`**

`app/src/main/java/com/example/todolist/ui/tasks/CategoryFilterBottomSheet.java`:

```java
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
```

- [ ] **Step 8: Biên dịch**

Chạy (qua subagent): `./gradlew :app:compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL. ViewBinding sinh `BottomsheetCategoryFilterBinding` (các id `search`, `rvCategories`, `emptyHint`, `btnClear`, `btnApply`). Nếu thiếu id → kiểm tra lại layout Step 5.

- [ ] **Step 9: Commit (local)**

```bash
git add app/src/main/java/com/example/todolist/ui/tasks/CategoryCheckAdapter.java \
        app/src/main/java/com/example/todolist/ui/tasks/CategoryFilterBottomSheet.java \
        app/src/main/res/layout/item_category_check.xml \
        app/src/main/res/layout/bottomsheet_category_filter.xml \
        app/src/main/res/drawable/ic_search.xml \
        app/src/main/res/drawable/ic_arrow_drop_down.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat(tasks): add category filter bottom sheet"
```

---

### Task 3: Wire filter — ViewModel + Fragment + layout (swap chip → dropdown)

**Files:**
- Modify (rewrite): `app/src/main/java/com/example/todolist/ui/tasks/TasksViewModel.java`
- Modify (rewrite): `app/src/main/java/com/example/todolist/ui/tasks/TasksFragment.java`
- Modify: `app/src/main/res/layout/fragment_tasks.xml` (bỏ ChipGroup, thêm nút danh mục)
- Modify: `app/src/main/java/com/example/todolist/data/TaskDao.java` (xoá `getByTopic`/`getByDueRange` nếu hết caller)
- Modify: `app/src/main/java/com/example/todolist/data/TaskRepository.java` (xoá wrapper tương ứng)

**Interfaces:**
- Consumes: `TaskRepository.getFiltered(int, long, long, int, List<Long>)` (Task 1); `CategoryFilterBottomSheet.REQUEST_KEY`, `RESULT_IDS`, `newInstance(Set<Long>)` (Task 2).
- Produces: `TasksViewModel.setCategories(Set<Long>)`, `getCategories(): LiveData<Set<Long>>`; giữ `getTasks()`, `getTopics()`, `setDate/clearDate/getDate`, `insert/update/delete/getTaskById`. **Bỏ** `setFilter(Long)`, `getFilter()`.

**Ghi chú compile-order:** Task này swap đồng thời ViewModel + Fragment + layout để không gãy biên dịch (Fragment cũ gọi `setFilter/getFilter` sẽ chết nếu chỉ đổi ViewModel). Việc xoá `getByTopic`/`getByDueRange` làm CUỐI cùng sau khi ViewModel hết dùng.

- [ ] **Step 1: Rewrite `TasksViewModel.java`**

Thay toàn bộ nội dung `app/src/main/java/com/example/todolist/ui/tasks/TasksViewModel.java`:

```java
package com.example.todolist.ui.tasks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.todolist.data.Task;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.data.Topic;
import com.example.todolist.data.TopicRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TasksViewModel extends AndroidViewModel {
    private final TaskRepository taskRepo;
    private final TopicRepository topicRepo;
    private final ZoneId zone = ZoneId.systemDefault();

    /** null = no date filter. */
    private final MutableLiveData<LocalDate> dateFilter = new MutableLiveData<>(null);
    /** empty = all categories. */
    private final MutableLiveData<Set<Long>> categories = new MutableLiveData<>(new HashSet<>());
    private final MediatorLiveData<FilterState> state = new MediatorLiveData<>();
    private final LiveData<List<Task>> tasks;

    /** Snapshot of both filter dimensions; drives the combined query. */
    static final class FilterState {
        final LocalDate date;
        final Set<Long> cats;
        FilterState(LocalDate date, Set<Long> cats) { this.date = date; this.cats = cats; }
    }

    public TasksViewModel(@NonNull Application app) {
        super(app);
        taskRepo = new TaskRepository(app);
        topicRepo = new TopicRepository(app);

        state.setValue(new FilterState(null, new HashSet<>()));
        state.addSource(dateFilter, d -> recompute());
        state.addSource(categories, c -> recompute());

        tasks = Transformations.switchMap(state, s -> {
            int hasDate = s.date != null ? 1 : 0;
            long from = hasDate == 1 ? startMillis(s.date) : 0L;
            long to = hasDate == 1 ? startMillis(s.date.plusDays(1)) : 0L;
            boolean hasCats = s.cats != null && !s.cats.isEmpty();
            List<Long> cats = hasCats ? new ArrayList<>(s.cats) : Collections.singletonList(-1L);
            return taskRepo.getFiltered(hasDate, from, to, hasCats ? 1 : 0, cats);
        });
    }

    private void recompute() {
        state.setValue(new FilterState(dateFilter.getValue(), categories.getValue()));
    }

    private long startMillis(LocalDate d) {
        return d.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public LiveData<List<Task>> getTasks() { return tasks; }

    public LiveData<List<Topic>> getTopics() { return topicRepo.getAll(); }

    public void setCategories(Set<Long> ids) {
        categories.setValue(ids != null ? new HashSet<>(ids) : new HashSet<>());
    }

    public LiveData<Set<Long>> getCategories() { return categories; }

    public void setDate(LocalDate d) { dateFilter.setValue(d); }

    public void clearDate() { dateFilter.setValue(null); }

    public LiveData<LocalDate> getDate() { return dateFilter; }

    public void insert(Task t, TaskRepository.OnId cb) { taskRepo.insert(t, cb); }

    public void update(Task t) { taskRepo.update(t); }

    public void delete(Task t) { taskRepo.delete(t); }

    public void getTaskById(long id, TaskRepository.OnTask cb) { taskRepo.getByIdAsync(id, cb); }
}
```

- [ ] **Step 2: Rewrite `TasksFragment.java`**

Thay toàn bộ nội dung `app/src/main/java/com/example/todolist/ui/tasks/TasksFragment.java`:

```java
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

        // Category filter
        binding.btnCategory.setOnClickListener(v ->
            CategoryFilterBottomSheet.newInstance(viewModel.getCategories().getValue())
                .show(getChildFragmentManager(), "category_filter"));
        binding.btnClearCategory.setOnClickListener(v ->
            viewModel.setCategories(Collections.emptySet()));

        getChildFragmentManager().setFragmentResultListener(
            CategoryFilterBottomSheet.REQUEST_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                long[] ids = bundle.getLongArray(CategoryFilterBottomSheet.RESULT_IDS);
                Set<Long> set = new HashSet<>();
                if (ids != null) for (long id : ids) set.add(id);
                viewModel.setCategories(set);
            });

        viewModel.getCategories().observe(getViewLifecycleOwner(), cats -> {
            int n = cats == null ? 0 : cats.size();
            if (n > 0) {
                binding.btnCategory.setText(getString(R.string.category_count, n));
                binding.btnClearCategory.setVisibility(View.VISIBLE);
            } else {
                binding.btnCategory.setText(R.string.category);
                binding.btnClearCategory.setVisibility(View.GONE);
            }
        });
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
```

- [ ] **Step 3: Sửa `fragment_tasks.xml` — bỏ ChipGroup, thêm nút danh mục**

Trong `app/src/main/res/layout/fragment_tasks.xml`, thay **cả hai** khối: (a) `LinearLayout` chứa `btn_pick_date`/`btn_clear_date` và (b) `HorizontalScrollView` chứa `chip_group` — bằng **một** `HorizontalScrollView` gộp bên dưới. Tức là xoá đoạn từ `<LinearLayout ... android:orientation="horizontal" ...>` (khối filter ngày) tới hết `</HorizontalScrollView>` của `chip_group`, thay bằng:

```xml
        <HorizontalScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingHorizontal="20dp"
            android:paddingBottom="8dp"
            android:scrollbars="none"
            android:clipToPadding="false">

            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical">

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

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_category"
                    style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:text="@string/category"
                    android:textColor="@color/muted_sage"
                    app:icon="@drawable/ic_arrow_drop_down"
                    app:iconGravity="textEnd"
                    app:iconTint="@color/muted_sage"
                    app:strokeColor="@color/whisper_border" />

                <ImageView
                    android:id="@+id/btn_clear_category"
                    android:layout_width="40dp"
                    android:layout_height="40dp"
                    android:layout_marginStart="4dp"
                    android:padding="8dp"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:contentDescription="@string/clear_category"
                    android:src="@drawable/ic_close"
                    android:visibility="gone" />
            </LinearLayout>
        </HorizontalScrollView>
```

Giữ nguyên các phần khác của layout (tiêu đề, `count`, `FrameLayout` chứa `rv_tasks`/`empty`, FAB).

- [ ] **Step 4: Biên dịch (feature hoàn chỉnh)**

Chạy (qua subagent): `./gradlew :app:compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL. ViewBinding `FragmentTasksBinding` có `btnCategory`, `btnClearCategory`; không còn tham chiếu `chipGroup`. Nếu lỗi "cannot find symbol chipGroup" → còn sót code chip trong Fragment.

- [ ] **Step 5: Dọn dead code trong data layer**

Grep xác nhận không còn caller: `git grep -n "getByTopic\b"` và `git grep -n "getByDueRange\b"` (chỉ nên còn định nghĩa trong DAO/repo). Nếu sạch:
- Xoá khỏi `TaskDao.java`:
  ```java
  @Query("SELECT * FROM tasks WHERE topicId = :topicId ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
  LiveData<List<Task>> getByTopic(long topicId);
  ...
  @Query("SELECT * FROM tasks WHERE dueAt >= :from AND dueAt < :to ORDER BY dueAt ASC")
  LiveData<List<Task>> getByDueRange(long from, long to);
  ```
- Xoá khỏi `TaskRepository.java`:
  ```java
  public LiveData<List<Task>> getByTopic(long topicId) { return dao.getByTopic(topicId); }
  public LiveData<List<Task>> getByDueRange(long from, long to) { return dao.getByDueRange(from, to); }
  ```
**Giữ** `getDueAtInRange` (date picker dots) và `getAll`. Nếu grep còn caller khác ngoài dự kiến → KHÔNG xoá, báo lại.

- [ ] **Step 6: Biên dịch lại sau khi dọn**

Chạy (qua subagent): `./gradlew :app:compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Kiểm thử thủ công (build + run trên emulator/thiết bị)**

Chạy `./gradlew :app:assembleDebug`, cài và test theo spec §9:
1. Nút "Danh mục ▾" hiện ở tab Tasks; bấm → mở bottom sheet (search + list checkbox + Bỏ chọn/Áp dụng).
2. Tích 2 category + Áp dụng → chỉ còn task thuộc 1 trong 2; nút hiện "Danh mục (2)" + ×.
3. Bấm × → về tất cả; nút về "Danh mục".
4. Gõ search → lọc theo tên; tích/bỏ tích rồi xoá search vẫn giữ trạng thái tích.
5. Chọn 1 ngày + tích category → chỉ còn task khớp cả ngày lẫn category (kết hợp AND).
6. "Bỏ chọn" trong sheet → bỏ hết tích, sheet vẫn mở.
7. Không có category → sheet hiện gợi ý rỗng.

(Bước run thủ công tuỳ điều kiện subagent; nếu không chạy được emulator, tối thiểu phải `assembleDebug` thành công và report để user tự chạy.)

- [ ] **Step 8: Commit (local)**

```bash
git add app/src/main/java/com/example/todolist/ui/tasks/TasksViewModel.java \
        app/src/main/java/com/example/todolist/ui/tasks/TasksFragment.java \
        app/src/main/res/layout/fragment_tasks.xml \
        app/src/main/java/com/example/todolist/data/TaskDao.java \
        app/src/main/java/com/example/todolist/data/TaskRepository.java
git commit -m "feat(tasks): replace single-topic chips with multi-select category filter"
```

---

## Self-Review (đã chạy)

**1. Spec coverage:**
- §3 data query → Task 1 ✓
- §4 ViewModel (categories + FilterState + switchMap, bỏ single filter) → Task 3 Step 1 ✓
- §5 CategoryFilterBottomSheet + CategoryCheckAdapter + layouts → Task 2 ✓
- §6 TasksFragment + fragment_tasks.xml (bỏ chip, nút danh mục, ×) → Task 3 Steps 2-3 ✓
- §7 strings + drawable → Task 2 Steps 1-3 ✓
- §8 edge cases (empty-IN, empty hint, search giữ tích) → Task 1 (workaround), Task 2 (adapter/sheet) ✓

**2. Placeholder scan:** Không có TBD/TODO; mọi step code có block cụ thể.

**3. Type consistency:** `getFiltered(int, long, long, int, List<Long>)` khớp giữa DAO/repo (Task 1) và caller ViewModel (Task 3). `REQUEST_KEY`/`RESULT_IDS`/`newInstance(Set<Long>)` khớp giữa sheet (Task 2) và Fragment (Task 3). `setCategories(Set<Long>)`/`getCategories()` khớp ViewModel↔Fragment. `long[]` là kiểu truyền qua FragmentResult ở cả sheet và Fragment ✓.

## Câu hỏi chưa giải quyết
- Không có.
