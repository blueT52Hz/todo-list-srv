# Tasca To-do List (Android/Java) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete offline Android To-do List app (Java) matching the "Tasca" Stitch design, with no login — launches straight into the app.

**Architecture:** Single `MainActivity` hosting a `BottomNavigationView` with two tabs (Tasks, Topics) as Fragments. Data flows Room DAO → Repository → AndroidViewModel (LiveData) → UI. Add/Edit use `BottomSheetDialogFragment`; task detail is a separate Activity. Reminders via AlarmManager exact alarms → BroadcastReceiver → notification.

**Tech Stack:** Java, Room, ViewBinding, RecyclerView (ListAdapter+DiffUtil), Material Components 3, Glide, Android Photo Picker, AlarmManager.

## Global Constraints

- Language: **Java**. `minSdk = 34`, `targetSdk = 36`, `compileSdk = 36`, Java 11.
- Package root: `com.example.todolist`.
- **No login** — `MainActivity` is the launcher, opens tabs directly.
- **No subtasks / nested tasks / checklists** — tasks are strictly single-level.
- **Fonts:** system only — `sans-serif` for text, `monospace` for numbers/dates. No font files.
- **Testing (user override):** do NOT write automated tests unless later asked. Each task's verification is **manual** — build + run in Android Studio. Do not auto-run build/lint from the agent.
- **Commits (user override):** commit **local only, never push**. Only commit when the user has agreed; otherwise leave changes staged/unstaged and tell the user.
- **1 image per task.** Task 1-level only. Colors from Tasca palette (see Task 1).
- Keep each source file under ~200 lines; split adapters/viewmodels into their own files.

---

### Task 1: Project foundation — deps, ViewBinding, colors, theme, strings

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `gradle/libs.versions.toml` (add Room/Glide/RecyclerView versions)
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/AndroidManifest.xml` (permissions)

**Interfaces:**
- Produces: color resources (`@color/canvas_mist`, `@color/muted_sage`, etc.), `viewBinding` enabled, Room/Glide/RecyclerView on classpath. Later tasks consume these.

- [ ] **Step 1: Add version entries to `gradle/libs.versions.toml`**

Under `[versions]` add:
```toml
room = "2.6.1"
glide = "4.16.0"
recyclerview = "1.3.2"
```
Under `[libraries]` add:
```toml
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }
recyclerview = { group = "androidx.recyclerview", name = "recyclerview", version.ref = "recyclerview" }
```

- [ ] **Step 2: Update `app/build.gradle.kts`**

Add `buildFeatures { viewBinding = true }` inside `android { }`. Append to `dependencies`:
```kotlin
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.glide)
    implementation(libs.recyclerview)
```

- [ ] **Step 3: Fill `colors.xml` with the Tasca palette**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="canvas_mist">#F7F8F7</color>
    <color name="pure_surface">#FFFFFF</color>
    <color name="charcoal_ink">#1C1F1E</color>
    <color name="muted_steel">#6B7270</color>
    <color name="faint_steel">#A0A5A3</color>
    <color name="muted_sage">#2F7A6F</color>
    <color name="sage_wash">#1A2F7A6F</color>
    <color name="soft_clay">#B4593F</color>
    <color name="whisper_border">#141C1F1E</color>
</resources>
```

- [ ] **Step 4: Set theme colors in `themes.xml`**

Inside `Base.Theme.TodoList`:
```xml
<item name="colorPrimary">@color/muted_sage</item>
<item name="colorOnPrimary">@color/pure_surface</item>
<item name="android:colorBackground">@color/canvas_mist</item>
<item name="colorSurface">@color/pure_surface</item>
<item name="colorOnSurface">@color/charcoal_ink</item>
```

- [ ] **Step 5: Add strings**

In `strings.xml` add: `tasks`, `topics`, `today`, `no_tasks_yet` ("No tasks yet — add your first"), `add_a_task`, `new_task`, `edit_task`, `save_task`, `title`, `topic`, `due_reminder`, `attachment`, `all`, `delete`, `edit`, `new_topic`, `topic_name`.

- [ ] **Step 6: Add permissions to `AndroidManifest.xml`**

Before `<application>`:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

- [ ] **Step 7: Manual verify** — Gradle sync succeeds in Android Studio, project builds. No runtime change yet.

- [ ] **Step 8: Commit (local only, if user agrees)** — `chore: add deps, tasca palette, permissions`

---

### Task 2: Room layer — entities, DAOs, database, seed topics

**Files:**
- Create: `app/src/main/java/com/example/todolist/data/Topic.java`
- Create: `app/src/main/java/com/example/todolist/data/Task.java`
- Create: `app/src/main/java/com/example/todolist/data/TopicDao.java`
- Create: `app/src/main/java/com/example/todolist/data/TaskDao.java`
- Create: `app/src/main/java/com/example/todolist/data/AppDatabase.java`

**Interfaces:**
- Produces:
  - `Topic(long id, String name, String colorHex, long createdAt)` fields public.
  - `Task(long id, String title, String note, Long topicId, Long dueAt, boolean done, String imagePath, long createdAt)` fields public.
  - `TopicDao`: `LiveData<List<Topic>> getAll()`, `long insert(Topic)`, `void update(Topic)`, `void delete(Topic)`, `Topic getByIdSync(long)`.
  - `TaskDao`: `LiveData<List<Task>> getAll()`, `LiveData<List<Task>> getByTopic(long topicId)`, `Task getByIdSync(long)`, `List<Task> getPendingWithDueSync()`, `long insert(Task)`, `void update(Task)`, `void delete(Task)`, `void clearTopic(long topicId)`.
  - `AppDatabase.getInstance(Context)` singleton; `topicDao()`, `taskDao()`; a `databaseWriteExecutor` (`ExecutorService`, single thread) for background writes.

- [ ] **Step 1: Create `Topic.java`**

```java
package com.example.todolist.data;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "topics")
public class Topic {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String colorHex;
    public long createdAt;
}
```

- [ ] **Step 2: Create `Task.java`**

```java
package com.example.todolist.data;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks",
    foreignKeys = @ForeignKey(entity = Topic.class, parentColumns = "id",
        childColumns = "topicId", onDelete = ForeignKey.SET_NULL),
    indices = {@Index("topicId")})
public class Task {
    @PrimaryKey(autoGenerate = true) public long id;
    public String title;
    @Nullable public String note;
    @Nullable public Long topicId;
    @Nullable public Long dueAt;
    public boolean done;
    @Nullable public String imagePath;
    public long createdAt;
}
```

- [ ] **Step 3: Create `TopicDao.java`**

```java
package com.example.todolist.data;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY createdAt ASC") LiveData<List<Topic>> getAll();
    @Query("SELECT * FROM topics WHERE id = :id") Topic getByIdSync(long id);
    @Insert long insert(Topic topic);
    @Update void update(Topic topic);
    @Delete void delete(Topic topic);
}
```

- [ ] **Step 4: Create `TaskDao.java`**

```java
package com.example.todolist.data;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
    LiveData<List<Task>> getAll();
    @Query("SELECT * FROM tasks WHERE topicId = :topicId ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
    LiveData<List<Task>> getByTopic(long topicId);
    @Query("SELECT * FROM tasks WHERE id = :id") Task getByIdSync(long id);
    @Query("SELECT * FROM tasks WHERE done = 0 AND dueAt IS NOT NULL") List<Task> getPendingWithDueSync();
    @Insert long insert(Task task);
    @Update void update(Task task);
    @Delete void delete(Task task);
}
```

- [ ] **Step 5: Create `AppDatabase.java` with singleton + seed callback**

```java
package com.example.todolist.data;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.*;

@Database(entities = {Task.class, Topic.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract TopicDao topicDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public static AppDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                            AppDatabase.class, "tasca.db")
                        .addCallback(SEED)
                        .build();
                }
            }
        }
        return INSTANCE;
    }

    // Seed default topics on first create
    private static final Callback SEED = new Callback() {
        @Override public void onCreate(@NonNull SupportSQLiteDatabase db) {
            databaseWriteExecutor.execute(() -> {
                TopicDao dao = INSTANCE.topicDao();
                long now = System.currentTimeMillis();
                seed(dao, "Work", "#2F7A6F", now);
                seed(dao, "Personal", "#B4593F", now + 1);
                seed(dao, "Study", "#6B7270", now + 2);
                seed(dao, "Errands", "#A0A5A3", now + 3);
            });
        }
    };
    private static void seed(TopicDao dao, String name, String hex, long t) {
        Topic tp = new Topic(); tp.name = name; tp.colorHex = hex; tp.createdAt = t; dao.insert(tp);
    }
}
```

- [ ] **Step 6: Manual verify** — build succeeds (Room annotation processor generates classes). Optionally run app once; DB file `tasca.db` created (no UI yet).

- [ ] **Step 7: Commit (local, if agreed)** — `feat: room entities, daos, db with seed topics`

---

### Task 3: Repositories + ViewModels

**Files:**
- Create: `app/src/main/java/com/example/todolist/data/TaskRepository.java`
- Create: `app/src/main/java/com/example/todolist/data/TopicRepository.java`
- Create: `app/src/main/java/com/example/todolist/ui/tasks/TasksViewModel.java`
- Create: `app/src/main/java/com/example/todolist/ui/topics/TopicsViewModel.java`

**Interfaces:**
- Consumes: DAOs + `databaseWriteExecutor` from Task 2.
- Produces:
  - `TaskRepository(Context)`: `LiveData<List<Task>> getAll()`, `LiveData<List<Task>> getByTopic(long)`, `void insert(Task, OnId)`, `void update(Task)`, `void delete(Task)`, `void getByIdAsync(long, OnTask)`. Callback ifaces: `interface OnId { void onId(long id); }`, `interface OnTask { void onTask(Task t); }` (invoked on main thread via `Handler(Looper.getMainLooper())`).
  - `TopicRepository(Context)`: `LiveData<List<Topic>> getAll()`, `void insert(Topic)`, `void update(Topic)`, `void delete(Topic)`.
  - `TasksViewModel extends AndroidViewModel`: `LiveData<List<Task>> getTasks()`, `void setFilter(Long topicId)` (null = All), `LiveData<List<Topic>> getTopics()`, plus passthrough `insert/update/delete`. Uses `Transformations.switchMap` on a `MutableLiveData<Long> filter`.
  - `TopicsViewModel extends AndroidViewModel`: `LiveData<List<Topic>> getTopics()`, passthrough CRUD.

- [ ] **Step 1: Create `TaskRepository.java`**

```java
package com.example.todolist.data;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import java.util.List;

public class TaskRepository {
    public interface OnId { void onId(long id); }
    public interface OnTask { void onTask(Task t); }
    private final TaskDao dao;
    private final Handler main = new Handler(Looper.getMainLooper());

    public TaskRepository(Context ctx) { dao = AppDatabase.getInstance(ctx).taskDao(); }

    public LiveData<List<Task>> getAll() { return dao.getAll(); }
    public LiveData<List<Task>> getByTopic(long id) { return dao.getByTopic(id); }

    public void insert(Task t, OnId cb) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = dao.insert(t);
            if (cb != null) main.post(() -> cb.onId(id));
        });
    }
    public void update(Task t) { AppDatabase.databaseWriteExecutor.execute(() -> dao.update(t)); }
    public void delete(Task t) { AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(t)); }
    public void getByIdAsync(long id, OnTask cb) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Task t = dao.getByIdSync(id);
            main.post(() -> cb.onTask(t));
        });
    }
}
```

- [ ] **Step 2: Create `TopicRepository.java`** (same pattern, no callbacks needed)

```java
package com.example.todolist.data;
import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;

public class TopicRepository {
    private final TopicDao dao;
    public TopicRepository(Context ctx) { dao = AppDatabase.getInstance(ctx).topicDao(); }
    public LiveData<List<Topic>> getAll() { return dao.getAll(); }
    public void insert(Topic t) { AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(t)); }
    public void update(Topic t) { AppDatabase.databaseWriteExecutor.execute(() -> dao.update(t)); }
    public void delete(Topic t) { AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(t)); }
}
```

- [ ] **Step 3: Create `TasksViewModel.java`**

```java
package com.example.todolist.ui.tasks;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.example.todolist.data.*;
import java.util.List;

public class TasksViewModel extends AndroidViewModel {
    private final TaskRepository taskRepo;
    private final TopicRepository topicRepo;
    private final MutableLiveData<Long> filter = new MutableLiveData<>(null); // null = All
    private final LiveData<List<Task>> tasks;

    public TasksViewModel(@NonNull Application app) {
        super(app);
        taskRepo = new TaskRepository(app);
        topicRepo = new TopicRepository(app);
        tasks = Transformations.switchMap(filter, id ->
            id == null ? taskRepo.getAll() : taskRepo.getByTopic(id));
    }
    public LiveData<List<Task>> getTasks() { return tasks; }
    public LiveData<List<Topic>> getTopics() { return topicRepo.getAll(); }
    public void setFilter(Long topicId) { filter.setValue(topicId); }
    public Long getFilter() { return filter.getValue(); }
    public void insert(Task t, TaskRepository.OnId cb) { taskRepo.insert(t, cb); }
    public void update(Task t) { taskRepo.update(t); }
    public void delete(Task t) { taskRepo.delete(t); }
}
```

- [ ] **Step 4: Create `TopicsViewModel.java`**

```java
package com.example.todolist.ui.topics;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.example.todolist.data.*;
import java.util.List;

public class TopicsViewModel extends AndroidViewModel {
    private final TopicRepository repo;
    public TopicsViewModel(@NonNull Application app) { super(app); repo = new TopicRepository(app); }
    public LiveData<List<Topic>> getTopics() { return repo.getAll(); }
    public void insert(Topic t) { repo.insert(t); }
    public void update(Topic t) { repo.update(t); }
    public void delete(Topic t) { repo.delete(t); }
}
```

- [ ] **Step 5: Manual verify** — build succeeds.
- [ ] **Step 6: Commit (local, if agreed)** — `feat: repositories and viewmodels`

---

### Task 4: MainActivity shell — BottomNav + two Fragments

**Files:**
- Create: `app/src/main/res/menu/bottom_nav_menu.xml`
- Create: `app/src/main/res/layout/fragment_tasks.xml` (placeholder: just a TextView "Tasks")
- Create: `app/src/main/res/layout/fragment_topics.xml` (placeholder: TextView "Topics")
- Create: `app/src/main/java/com/example/todolist/ui/tasks/TasksFragment.java` (minimal)
- Create: `app/src/main/java/com/example/todolist/ui/topics/TopicsFragment.java` (minimal)
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/example/todolist/MainActivity.java`
- Add vector icons: `res/drawable/ic_tasks.xml`, `ic_topics.xml`, `ic_add.xml`, `ic_check.xml`, `ic_back.xml` (use Android Studio's built-in Material vector assets: `task_alt`, `folder`, `add`, `check`, `arrow_back`).

**Interfaces:**
- Produces: working 2-tab shell. `TasksFragment` and `TopicsFragment` exist and inflate via ViewBinding.

- [ ] **Step 1: `bottom_nav_menu.xml`** — two items `@+id/nav_tasks` (icon `ic_tasks`, title `@string/tasks`), `@+id/nav_topics` (icon `ic_topics`, title `@string/topics`).

- [ ] **Step 2: Add vector drawables** via Android Studio Vector Asset (Material icons listed above), tint `@color/muted_steel` by default.

- [ ] **Step 3: `activity_main.xml`** — root `LinearLayout` (vertical) with id `@+id/main`: a `FragmentContainerView` id `@+id/nav_host` (`layout_weight=1`) above a `com.google.android.material.bottomnavigation.BottomNavigationView` id `@+id/bottom_nav`, `app:menu="@menu/bottom_nav_menu"`, background `@color/pure_surface`.

- [ ] **Step 4: Minimal fragment layouts** — each a `FrameLayout` with a centered `TextView` for now.

- [ ] **Step 5: `TasksFragment.java` and `TopicsFragment.java`** — standard `Fragment` subclasses using ViewBinding in `onCreateView`:

```java
// TasksFragment.java
package com.example.todolist.ui.tasks;
import android.os.Bundle; import android.view.*;
import androidx.annotation.*; import androidx.fragment.app.Fragment;
import com.example.todolist.databinding.FragmentTasksBinding;

public class TasksFragment extends Fragment {
    private FragmentTasksBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        b = FragmentTasksBinding.inflate(i, c, false); return b.getRoot();
    }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
```
(TopicsFragment identical with `FragmentTopicsBinding`.)

- [ ] **Step 6: `MainActivity.java`** — inflate `ActivityMainBinding`, manage fragment swap on nav select, default Tasks:

```java
package com.example.todolist;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.databinding.ActivityMainBinding;
import com.example.todolist.ui.tasks.TasksFragment;
import com.example.todolist.ui.topics.TopicsFragment;

public class MainActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivityMainBinding b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (s == null) show(new TasksFragment());
        b.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tasks) show(new TasksFragment());
            else if (id == R.id.nav_topics) show(new TopicsFragment());
            return true;
        });
    }
    private void show(androidx.fragment.app.Fragment f) {
        getSupportFragmentManager().beginTransaction().replace(R.id.nav_host, f).commit();
    }
}
```

- [ ] **Step 7: Manual verify** — run app: two tabs switch between "Tasks"/"Topics" placeholders. Status bar / bottom inset acceptable.
- [ ] **Step 8: Commit (local, if agreed)** — `feat: bottom nav shell with two fragments`

---

### Task 5: Topics tab — list + add/edit/delete

**Files:**
- Create: `res/layout/item_topic.xml`, `res/layout/bottomsheet_edit_topic.xml`
- Create: `app/src/main/java/com/example/todolist/ui/topics/TopicAdapter.java`
- Create: `app/src/main/java/com/example/todolist/ui/topics/AddEditTopicBottomSheet.java`
- Create: `res/drawable/bg_topic_dot.xml` (oval shape, tinted at runtime)
- Modify: `res/layout/fragment_topics.xml` (RecyclerView + FAB)
- Modify: `TopicsFragment.java` (wire ViewModel, adapter, FAB)

**Interfaces:**
- Consumes: `TopicsViewModel`, `Topic`.
- Produces: `AddEditTopicBottomSheet.newInstance(@Nullable Long topicId)` static factory; edits when id given, creates when null. `TopicAdapter` with `interface OnTopicClick { void onClick(Topic t); }`.

- [ ] **Step 1: `fragment_topics.xml`** — `CoordinatorLayout` (canvas_mist bg) containing header TextView "Topics" (sans-serif bold) + mono count, a `RecyclerView` id `@+id/rv_topics`, and a `FloatingActionButton` id `@+id/fab_add_topic` (icon `ic_add`, tint muted_sage) anchored bottom-end.

- [ ] **Step 2: `item_topic.xml`** — horizontal row: `View` id `@+id/dot` (12dp, `@drawable/bg_topic_dot`), name TextView (sans-serif), mono caption TextView "N tasks" (later just topic name; count optional — show topic name + colorHex for now), bottom 1dp divider `@color/whisper_border`. Min height 56dp.

- [ ] **Step 3: `bg_topic_dot.xml`** — `<shape android:shape="oval"><solid android:color="#2F7A6F"/></shape>`; tinted at bind time via `dot.getBackground().setTint(Color.parseColor(colorHex))`.

- [ ] **Step 4: `TopicAdapter.java`** — `ListAdapter<Topic, VH>` with DiffUtil (compare `id`, then `name`+`colorHex`), binds dot color + name, row click → `OnTopicClick`.

- [ ] **Step 5: `bottomsheet_edit_topic.xml`** — title "New Topic", `TextInputLayout`+`TextInputEditText` for name, a horizontal row of ~6 color swatches (`View`s with preset hex from palette; selected shows ring), primary "Save" button (muted_sage), and (edit mode) a "Delete" text button (soft_clay).

- [ ] **Step 6: `AddEditTopicBottomSheet.java`** — `BottomSheetDialogFragment`. Reads optional `arg_topic_id`. If editing, prefill from `TopicsViewModel.getTopics()` match (or pass name/color via args). Save → build `Topic`, `insert`/`update`. Delete → `delete`. Dismiss on done.

```java
public static AddEditTopicBottomSheet newInstance(@Nullable Long id) {
    AddEditTopicBottomSheet f = new AddEditTopicBottomSheet();
    Bundle b = new Bundle();
    if (id != null) b.putLong("arg_topic_id", id);
    f.setArguments(b); return f;
}
```

- [ ] **Step 7: Wire `TopicsFragment`** — get `TopicsViewModel` via `new ViewModelProvider(this)`, set adapter, observe `getTopics()` → `submitList`, FAB opens `AddEditTopicBottomSheet.newInstance(null)`, row click opens edit sheet.

- [ ] **Step 8: Manual verify** — Topics tab shows 4 seeded topics with colored dots; add a topic (appears), edit a topic name/color (updates), delete a topic (disappears).
- [ ] **Step 9: Commit (local, if agreed)** — `feat: topics crud`

---

### Task 6: Tasks tab — list, filter chips, empty state

**Files:**
- Create: `res/layout/item_task.xml`, `res/drawable/bg_checkbox_circle.xml`, `res/drawable/bg_chip_filter.xml` (or use Material `Chip`)
- Create: `app/src/main/java/com/example/todolist/ui/tasks/TaskAdapter.java`
- Create: `app/src/main/java/com/example/todolist/util/DateUtils.java`
- Modify: `res/layout/fragment_tasks.xml` (header + ChipGroup + RecyclerView + empty view + FAB)
- Modify: `TasksFragment.java`

**Interfaces:**
- Consumes: `TasksViewModel`, `Task`, `Topic`, Glide.
- Produces:
  - `DateUtils.formatDue(Long dueAt)` → String like `Jul 28 · 09:00` (mono display); `DateUtils.isOverdue(Long dueAt)` → boolean.
  - `TaskAdapter` with `interface Listener { void onOpen(Task t); void onToggle(Task t); }`. Binds title (strikethrough+faint if done), mono due caption (soft_clay if overdue & not done), topic dot color (looked up from a `Map<Long,String> topicColors` setter `setTopicColors(Map)`), image thumbnail via Glide when `imagePath != null`.

- [ ] **Step 1: `DateUtils.java`**

```java
package com.example.todolist.util;
import java.text.SimpleDateFormat; import java.util.*;
public final class DateUtils {
    private static final SimpleDateFormat F = new SimpleDateFormat("MMM d · HH:mm", Locale.getDefault());
    public static String formatDue(Long dueAt) { return dueAt == null ? "" : F.format(new Date(dueAt)); }
    public static boolean isOverdue(Long dueAt) { return dueAt != null && dueAt < System.currentTimeMillis(); }
    private DateUtils() {}
}
```

- [ ] **Step 2: `bg_checkbox_circle.xml`** — oval stroke `@color/whisper_border` (unchecked); checked state handled by swapping to a checked drawable or tinting + showing `ic_check`. Simplest: `ImageView` with `ic_check`, tint muted_sage when done, else show ring background only.

- [ ] **Step 3: `item_task.xml`** — horizontal: circular check `ImageView` id `@+id/check` (44dp touch), middle vertical block (title TextView `@+id/title` sans-serif + due caption TextView `@+id/due` monospace muted_steel), right side: topic dot `View` id `@+id/dot` + optional thumbnail `ImageView` id `@+id/thumb` (40dp rounded). Bottom 1dp divider whisper_border. Vertical padding 16dp.

- [ ] **Step 4: `TaskAdapter.java`** — `ListAdapter<Task, VH>` + DiffUtil (id; then title/done/dueAt/imagePath/topicId). Bind rules: done → title strikethrough + `@color/faint_steel`, check tinted muted_sage; not done → charcoal_ink. Due caption via `DateUtils.formatDue`; if `isOverdue && !done` set text color soft_clay. Dot: `setTint(Color.parseColor(topicColors.get(topicId)))` or GONE if topicId null. Thumb: `Glide.with(...).load(new File(imagePath)).into(thumb)` else GONE. Check click → `listener.onToggle`; row click → `listener.onOpen`.

- [ ] **Step 5: `fragment_tasks.xml`** — CoordinatorLayout (canvas_mist): header "Today" (sans-serif bold ~28sp) + mono count TextView `@+id/count`; a `HorizontalScrollView`>`com.google.android.material.chip.ChipGroup` id `@+id/chip_group` (single-line, singleSelection) for filter chips; `RecyclerView` id `@+id/rv_tasks`; an empty-state `LinearLayout` id `@+id/empty` (centered: illustration `ImageView` using `ic_tasks` tinted faint_steel, "No tasks yet — add your first" text, "Add a task" button) initially GONE; `FloatingActionButton` id `@+id/fab_add_task`.

- [ ] **Step 6: Wire `TasksFragment`** —
  - `TasksViewModel` + `TaskAdapter`.
  - Observe `getTopics()`: build filter chips ("All" first + one Chip per topic; `All` checked by default), build `Map<Long,String>` topic colors → `adapter.setTopicColors(map)`. Chip check → `vm.setFilter(topicIdOrNull)`.
  - Observe `getTasks()`: `submitList`, update mono count text (`N tasks`), toggle empty view visibility when list empty.
  - Check toggle: `t.done = !t.done; vm.update(t)` and (reminder reschedule handled in Task 9 — leave a TODO hook `ReminderScheduler` call to be added there).
  - FAB → open `AddEditTaskBottomSheet.newInstance(null)` (created in Task 7). Until Task 7 exists, FAB can be a no-op stub; add the call in Task 7.
  - Row open → start `TaskDetailActivity` (created in Task 8); stub until then.

- [ ] **Step 7: Manual verify** — Tasks tab shows empty state (no tasks yet); chips render with "All" + seeded topics; count shows "0 tasks". (Add/detail wired in later tasks.)
- [ ] **Step 8: Commit (local, if agreed)** — `feat: tasks list, filter chips, empty state`

---

### Task 7: Add/Edit Task bottom sheet — title, topic, date/time, image

**Files:**
- Create: `res/layout/bottomsheet_edit_task.xml`
- Create: `app/src/main/java/com/example/todolist/ui/tasks/AddEditTaskBottomSheet.java`
- Create: `app/src/main/java/com/example/todolist/util/ImageStorage.java`
- Modify: `TasksFragment.java` (open sheet from FAB)

**Interfaces:**
- Consumes: `TasksViewModel`, `Topic`, `Task`, Photo Picker, `MaterialDatePicker`/`MaterialTimePicker`, Glide.
- Produces:
  - `ImageStorage.copyToInternal(Context, Uri)` → `String absolutePath` (copies picked image into `filesDir/images/<uuid>.jpg`; runs on caller's background or wrap in executor — here do it on `AppDatabase.databaseWriteExecutor` then callback main). Provide `interface OnSaved { void onSaved(String path); }`. Also `ImageStorage.delete(String path)`.
  - `AddEditTaskBottomSheet.newInstance(@Nullable Long taskId)`.

- [ ] **Step 1: `ImageStorage.java`**

```java
package com.example.todolist.util;
import android.content.Context; import android.net.Uri;
import android.os.Handler; import android.os.Looper;
import com.example.todolist.data.AppDatabase;
import java.io.*; import java.util.UUID;
public final class ImageStorage {
    public interface OnSaved { void onSaved(String path); }
    public static void copyToInternal(Context ctx, Uri uri, OnSaved cb) {
        Handler main = new Handler(Looper.getMainLooper());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String path = null;
            try {
                File dir = new File(ctx.getFilesDir(), "images");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, UUID.randomUUID() + ".jpg");
                try (InputStream in = ctx.getContentResolver().openInputStream(uri);
                     OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192]; int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
                path = out.getAbsolutePath();
            } catch (Exception e) { path = null; }
            String finalPath = path;
            main.post(() -> cb.onSaved(finalPath));
        });
    }
    public static void delete(String path) {
        if (path != null) { File f = new File(path); if (f.exists()) f.delete(); }
    }
    private ImageStorage() {}
}
```

- [ ] **Step 2: `bottomsheet_edit_task.xml`** — rounded-top sheet (canvas_mist/pure_surface): drag handle, title "New Task"; `TextInputLayout` for title; a `ChipGroup` id `@+id/topic_chips` (singleSelection) for topic pick; a tappable row `@+id/row_due` showing mono due value + calendar icon (with a clear "×" to unset); an attachment area `@+id/attach` = dashed-border FrameLayout showing `ic_add` when empty or an `ImageView` `@+id/preview` thumbnail when set; a full-width primary Button `@+id/btn_save` "Save task" (muted_sage).

- [ ] **Step 3: `AddEditTaskBottomSheet.java`** — key logic:
  - Fields: `Long editingId`, `Long selectedTopicId`, `Long selectedDueAt`, `String imagePath`.
  - Photo picker: `registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> { if(uri!=null) ImageStorage.copyToInternal(requireContext(), uri, p -> { imagePath=p; Glide.with(this).load(new File(p)).into(preview); }); })`; launch with `PickVisualMediaRequest` image-only.
  - Due row click: `MaterialDatePicker` → on date, `MaterialTimePicker` → combine into epoch millis → `selectedDueAt`, update mono label.
  - Populate topic chips from `TasksViewModel.getTopics()` observe (one chip per topic; check the one matching `selectedTopicId`).
  - Edit mode: `newInstance(id)`; load task via `TaskRepository.getByIdAsync` (or pass a `getByIdAsync` through the VM — add `void getTaskById(long, OnTask)` to `TasksViewModel` delegating to repo) and prefill.
  - Save: validate title non-empty (else `TextInputLayout` error). Build `Task` (set createdAt when new, keep when edit). `vm.insert(task, id -> { task.id=id; scheduleReminder(task); dismiss(); })` or `vm.update(task); scheduleReminder(task); dismiss();`. `scheduleReminder` is a stub method here; Task 9 fills it via `ReminderScheduler`.
  - Add to `TasksViewModel`: `public void getTaskById(long id, TaskRepository.OnTask cb){ taskRepo.getByIdAsync(id, cb); }`.

- [ ] **Step 4: Wire FAB** in `TasksFragment` → `AddEditTaskBottomSheet.newInstance(null).show(getChildFragmentManager(), "add")`.

- [ ] **Step 5: Manual verify** — FAB opens sheet; create a task with title + topic + due + image → appears in list with thumbnail, topic dot, mono due; edit it → changes persist; overdue due shows soft_clay.
- [ ] **Step 6: Commit (local, if agreed)** — `feat: add/edit task sheet with image and datetime`

---

### Task 8: Task Detail activity

**Files:**
- Create: `res/layout/activity_task_detail.xml`
- Create: `app/src/main/java/com/example/todolist/ui/detail/TaskDetailActivity.java`
- Modify: `AndroidManifest.xml` (register activity)
- Modify: `TasksFragment.java` (open detail on row click)

**Interfaces:**
- Consumes: `TaskRepository`/`TasksViewModel`, `Task`, `Topic`, Glide.
- Produces: `TaskDetailActivity` launched via intent extra `"task_id"` (long). Provides Edit (opens `AddEditTaskBottomSheet`) and Delete (removes task + its image + cancels reminder — reminder cancel added in Task 9).

- [ ] **Step 1: `activity_task_detail.xml`** — back chevron top; large title (sans-serif bold); metadata row (topic dot + name, mono reminder line); large rounded image `ImageView` `@+id/image` (GONE if none); note paragraph (muted_steel); bottom primary "Edit" button + "Delete" text button (soft_clay).

- [ ] **Step 2: `TaskDetailActivity.java`** — read `task_id`, load via `TaskRepository.getByIdAsync`, bind fields (topic name/color via `TopicDao.getByIdSync` on executor). Edit → open `AddEditTaskBottomSheet.newInstance(id)` (host in a `FragmentManager`; since this is an Activity, use `getSupportFragmentManager()`), refresh on dismiss (re-query in `onResume`). Delete → confirm dialog → `ImageStorage.delete(imagePath)` + `repo.delete(task)` + finish.

- [ ] **Step 3: Register in manifest** — `<activity android:name=".ui.detail.TaskDetailActivity" android:exported="false"/>`.

- [ ] **Step 4: Wire row open** in `TasksFragment` → `startActivity(new Intent(ctx, TaskDetailActivity.class).putExtra("task_id", task.id))`.

- [ ] **Step 5: Manual verify** — tap a task → detail shows title, topic, mono reminder, large image, note; Edit updates; Delete removes task and its image file.
- [ ] **Step 6: Commit (local, if agreed)** — `feat: task detail with edit and delete`

---

### Task 9: Reminders — notification channel, scheduler, receivers, permission

**Files:**
- Create: `app/src/main/java/com/example/todolist/reminder/ReminderScheduler.java`
- Create: `app/src/main/java/com/example/todolist/reminder/ReminderReceiver.java`
- Create: `app/src/main/java/com/example/todolist/reminder/BootReceiver.java`
- Create: `app/src/main/java/com/example/todolist/TodoApp.java` (Application — create channel)
- Modify: `AndroidManifest.xml` (application name, receivers)
- Modify: `MainActivity.java` (request POST_NOTIFICATIONS)
- Modify: `AddEditTaskBottomSheet.java`, `TasksFragment.java`, `TaskDetailActivity.java` (replace reminder stubs with real calls)

**Interfaces:**
- Consumes: `Task`, `TaskDao.getPendingWithDueSync()`, `AlarmManager`.
- Produces:
  - `ReminderScheduler.schedule(Context, Task)` — sets exact alarm at `task.dueAt` if non-null, in the future, and `!task.done`; else calls `cancel`.
  - `ReminderScheduler.cancel(Context, long taskId)`.
  - Notification channel id `"reminders"`.

- [ ] **Step 1: `TodoApp.java`** — `Application` creating notification channel "reminders" (importance HIGH) in `onCreate`. Register in manifest `android:name=".TodoApp"`.

- [ ] **Step 2: `ReminderScheduler.java`**

```java
package com.example.todolist.reminder;
import android.app.*; import android.content.*; import android.os.Build;
import com.example.todolist.data.Task;
public final class ReminderScheduler {
    public static void schedule(Context ctx, Task t) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (t.dueAt == null || t.done || t.dueAt <= System.currentTimeMillis()) { cancel(ctx, t.id); return; }
        Intent i = new Intent(ctx, ReminderReceiver.class).putExtra("task_id", t.id);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, (int) t.id, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.dueAt, pi); }
        catch (SecurityException e) { am.set(AlarmManager.RTC_WAKEUP, t.dueAt, pi); }
    }
    public static void cancel(Context ctx, long taskId) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        Intent i = new Intent(ctx, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, (int) taskId, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
    private ReminderScheduler() {}
}
```

- [ ] **Step 3: `ReminderReceiver.java`** — `onReceive`: read `task_id`, load task on `databaseWriteExecutor` via `getByIdSync`; if still pending, post a notification (channel "reminders", small icon `ic_check`, title = task.title, tap → `TaskDetailActivity` with extra). Use `NotificationManagerCompat`; guard POST_NOTIFICATIONS.

- [ ] **Step 4: `BootReceiver.java`** — on `BOOT_COMPLETED`, load `getPendingWithDueSync()` on executor, `ReminderScheduler.schedule` each. Register receiver in manifest with `RECEIVE_BOOT_COMPLETED` intent-filter, `android:exported="true"`; `ReminderReceiver` `android:exported="false"`.

- [ ] **Step 5: Request POST_NOTIFICATIONS** in `MainActivity` (Android 13+) via `ActivityResultContracts.RequestPermission` on first launch.

- [ ] **Step 6: Replace stubs** — call `ReminderScheduler.schedule(ctx, task)` after insert/update in `AddEditTaskBottomSheet` and after toggle done in `TasksFragment`; `ReminderScheduler.cancel(ctx, id)` after delete in `TaskDetailActivity`.

- [ ] **Step 7: Manual verify** — create task with due ~2 min ahead → notification fires at time (even if app backgrounded); mark done before → no notification; delete → no notification; reboot with pending task → still fires (optional if testable).
- [ ] **Step 8: Commit (local, if agreed)** — `feat: exact-alarm reminders with notifications`

---

### Task 10: Polish & final pass

**Files:** touch as needed across UI.

- [ ] **Step 1:** Verify image file deletion also happens when a task's image is replaced during edit (delete old `imagePath` if changed) in `AddEditTaskBottomSheet`.
- [ ] **Step 2:** Verify topic filter chips update live when topics added/removed; "All" resets filter.
- [ ] **Step 3:** Verify empty state appears only when filtered list is truly empty; count text matches list size (mono).
- [ ] **Step 4:** Confirm no pure-black usage; backgrounds are canvas_mist; primary actions muted_sage; overdue soft_clay only.
- [ ] **Step 5: Manual verify** full flow end-to-end on device/emulator: topics CRUD, tasks CRUD, filter, image attach/detail, reminder.
- [ ] **Step 6: Commit (local, if agreed)** — `chore: polish and final verification`

---

## Self-Review notes

- **Spec coverage:** add/edit/delete task (T6–T8), list view (T6), add/edit/delete topic (T5), image attach (T7), filter by topic (T6), reminder at due datetime (T9), no login (T4 launcher), seed topics (T2), Tasca colors/fonts (T1/T6). All covered.
- **Types consistent:** `TaskRepository.OnId`/`OnTask`, `TasksViewModel.getTaskById`, `ReminderScheduler.schedule/cancel`, `ImageStorage.copyToInternal/delete`, `DateUtils.formatDue/isOverdue` used consistently across tasks.
- **Deferred hooks:** reminder calls are stubbed in T6–T8 and concretely wired in T9 (explicitly noted) to avoid forward-dependency on AlarmManager code.

## Open question

- Seeding 4 default topics on first run is included (Task 2). If undesired, remove the `SEED` callback.
