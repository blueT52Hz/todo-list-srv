# Settings Tab & Home-screen Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Settings tab (3rd BottomNav item) with "add widget" + "test notification" actions, and a home-screen collection App Widget listing pending tasks, to the existing Tasca Java Android To-do app.

**Architecture:** Settings is an action-only Fragment hosted by the existing `MainActivity` fragment-swap. The widget is a collection widget: `TasksWidgetProvider` (AppWidgetProvider) + `TasksWidgetService` (RemoteViewsService) + `TasksRemoteViewsFactory` reading tasks synchronously from Room. Task writes trigger `WidgetUpdater.refresh(...)` so placed widgets reload. Test notification posts immediately through the existing `reminders` channel.

**Tech Stack:** Java, Room, AppWidget (RemoteViews), NotificationCompat, ViewBinding, Material BottomNavigationView.

## Global Constraints

- Package `com.example.todolist`; Java; `minSdk 34`, `targetSdk 36`. No login.
- Notification channel id is `"reminders"` (constant `TodoApp.CHANNEL_ID`) — reuse, do not create a new channel.
- Task-open intent extra key is `"task_id"` (constant `ReminderScheduler.EXTRA_TASK_ID`); `TaskDetailActivity` reads `getLongExtra("task_id", -1)`. Widget row clicks MUST use this key.
- Test notification uses a fixed id `9001` (distinct from reminder ids which use `(int) taskId`), so it never collides with a real reminder.
- RemoteViews supports only a limited view set — widget layouts use `LinearLayout`/`TextView`/`ImageView`/`ListView` only. NO bare `<View>` (unsupported); the topic dot is an `ImageView` tinted via `setInt(id,"setColorFilter",color)`.
- `updatePeriodMillis` is `0`; the widget refreshes only via `WidgetUpdater.refresh(...)`, never on a timer.
- Colors come from the existing Tasca palette in `res/values/colors.xml`: `canvas_mist`, `charcoal_ink`, `muted_steel`, `whisper_border` (already defined). `bg_topic_dot` drawable already exists.
- Per user override: **no automated tests** unless requested. Each task's verification is a successful `assembleDebug` build plus the described manual check.

---

### Task 1: Data getters + widget/settings resources

**Files:**
- Modify: `app/src/main/java/com/example/todolist/data/TaskDao.java`
- Modify: `app/src/main/java/com/example/todolist/data/TopicDao.java`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_settings.xml`
- Create: `app/src/main/res/drawable/ic_widget.xml`
- Create: `app/src/main/res/drawable/ic_bell.xml`
- Create: `app/src/main/res/drawable/bg_widget.xml`
- Create: `app/src/main/res/xml/tasks_widget_info.xml`
- Create: `app/src/main/res/layout/widget_tasks.xml`
- Create: `app/src/main/res/layout/widget_item_task.xml`

**Interfaces:**
- Produces:
  - `TaskDao.getPendingSync() : List<Task>` — all tasks where `done = 0`, ordered by due.
  - `TopicDao.getAllSync() : List<Topic>` — all topics (sync).
  - Layout ids: `R.id.widget_list` (ListView), `R.id.widget_empty` (TextView), `R.id.widget_header` (TextView); item ids `R.id.widget_item_root`, `R.id.widget_item_dot` (ImageView), `R.id.widget_item_title`, `R.id.widget_item_due`.
  - String keys: `settings`, `add_widget`, `test_notification`, `test_notification_body`, `notif_on`, `notif_off`, `notif_permission_needed`, `widget_pin_unsupported`, `widget_empty`. (`app_name` already exists.)
  - Drawables: `ic_settings`, `ic_widget`, `ic_bell`, `bg_widget`.
  - XML: `@xml/tasks_widget_info`.

- [ ] **Step 1: Add `getPendingSync()` to `TaskDao`**

Add this method inside the `TaskDao` interface (next to `getPendingWithDueSync`):

```java
@Query("SELECT * FROM tasks WHERE done = 0 ORDER BY dueAt IS NULL, dueAt ASC, createdAt DESC")
List<Task> getPendingSync();
```

- [ ] **Step 2: Add `getAllSync()` to `TopicDao`**

Add this method inside the `TopicDao` interface:

```java
@Query("SELECT * FROM topics ORDER BY createdAt ASC") List<Topic> getAllSync();
```

- [ ] **Step 3: Add strings**

Add to `app/src/main/res/values/strings.xml` (inside `<resources>`):

```xml
<string name="settings">Settings</string>
<string name="add_widget">Thêm widget vào màn hình chính</string>
<string name="test_notification">Test thông báo</string>
<string name="test_notification_body">Đây là thông báo thử</string>
<string name="notif_on">Thông báo: đang bật</string>
<string name="notif_off">Thông báo: đã tắt</string>
<string name="notif_permission_needed">Cần bật quyền thông báo trong Cài đặt</string>
<string name="widget_pin_unsupported">Launcher không hỗ trợ thêm tự động — kéo widget thủ công</string>
<string name="widget_empty">Không có việc nào</string>
```

- [ ] **Step 4: Create `ic_settings.xml`**

`app/src/main/res/drawable/ic_settings.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/charcoal_ink"
        android:pathData="M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94l-0.36,-2.54c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41l-0.36,2.54c-0.59,0.24 -1.13,0.57 -1.62,0.94l-2.39,-0.96c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87c-0.12,0.21 -0.08,0.47 0.12,0.61l2.03,1.58c-0.05,0.3 -0.09,0.63 -0.09,0.94s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z"/>
</vector>
```

- [ ] **Step 5: Create `ic_widget.xml`**

`app/src/main/res/drawable/ic_widget.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/charcoal_ink"
        android:pathData="M3,3h8v8H3zM13,3h8v8h-8zM13,13h8v8h-8zM3,13h8v8H3z"/>
</vector>
```

- [ ] **Step 6: Create `ic_bell.xml`**

`app/src/main/res/drawable/ic_bell.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/charcoal_ink"
        android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.9,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32L13.5,4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z"/>
</vector>
```

- [ ] **Step 7: Create `bg_widget.xml`**

`app/src/main/res/drawable/bg_widget.xml`:

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/canvas_mist" />
    <corners android:radius="20dp" />
</shape>
```

- [ ] **Step 8: Create `tasks_widget_info.xml`**

`app/src/main/res/xml/tasks_widget_info.xml`:

```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="110dp"
    android:targetCellWidth="3"
    android:targetCellHeight="2"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:initialLayout="@layout/widget_tasks"
    android:previewImage="@drawable/ic_widget"
    android:updatePeriodMillis="0" />
```

- [ ] **Step 9: Create `widget_tasks.xml`**

`app/src/main/res/layout/widget_tasks.xml`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@drawable/bg_widget"
    android:padding="12dp">

    <TextView
        android:id="@+id/widget_header"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/app_name"
        android:textColor="@color/charcoal_ink"
        android:textStyle="bold"
        android:textSize="16sp"
        android:paddingBottom="8dp" />

    <TextView
        android:id="@+id/widget_empty"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:text="@string/widget_empty"
        android:textColor="@color/muted_steel"
        android:visibility="gone" />

    <ListView
        android:id="@+id/widget_list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:divider="@color/whisper_border"
        android:dividerHeight="1dp" />
</LinearLayout>
```

- [ ] **Step 10: Create `widget_item_task.xml`**

`app/src/main/res/layout/widget_item_task.xml` (dot is `ImageView`, not `<View>` — RemoteViews constraint):

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_item_root"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingVertical="10dp"
    android:paddingHorizontal="4dp">

    <ImageView
        android:id="@+id/widget_item_dot"
        android:layout_width="8dp"
        android:layout_height="8dp"
        android:layout_marginEnd="10dp"
        android:src="@drawable/bg_topic_dot"
        android:contentDescription="@null" />

    <TextView
        android:id="@+id/widget_item_title"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:maxLines="1"
        android:ellipsize="end"
        android:textColor="@color/charcoal_ink"
        android:textSize="14sp" />

    <TextView
        android:id="@+id/widget_item_due"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:fontFamily="monospace"
        android:textColor="@color/muted_steel"
        android:textSize="12sp" />
</LinearLayout>
```

- [ ] **Step 11: Build to verify resources + DAO compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Room processes the two new `@Query` methods; all new XML resources compile (R fields generated).

- [ ] **Step 12: Commit**

```bash
git add app/src/main/java/com/example/todolist/data/TaskDao.java \
        app/src/main/java/com/example/todolist/data/TopicDao.java \
        app/src/main/res/values/strings.xml \
        app/src/main/res/drawable/ic_settings.xml app/src/main/res/drawable/ic_widget.xml \
        app/src/main/res/drawable/ic_bell.xml app/src/main/res/drawable/bg_widget.xml \
        app/src/main/res/xml/tasks_widget_info.xml \
        app/src/main/res/layout/widget_tasks.xml app/src/main/res/layout/widget_item_task.xml
git commit -m "feat: add widget/settings resources and sync DAO getters"
```

---

### Task 2: Widget backend (provider + service + factory)

**Files:**
- Create: `app/src/main/java/com/example/todolist/widget/TasksWidgetProvider.java`
- Create: `app/src/main/java/com/example/todolist/widget/TasksWidgetService.java`
- Create: `app/src/main/java/com/example/todolist/widget/TasksRemoteViewsFactory.java`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `TaskDao.getPendingSync()`, `TopicDao.getAllSync()` (Task 1); `AppDatabase.getInstance(ctx)`, `AppDatabase.databaseWriteExecutor`; `DateUtils.formatDue(long)`; `ReminderScheduler.EXTRA_TASK_ID` (= `"task_id"`); `TaskDetailActivity`, `MainActivity`; layout/ids from Task 1.
- Produces: `TasksWidgetProvider` (referenced by `WidgetUpdater` and `SettingsFragment` in later tasks).

- [ ] **Step 1: Create `TasksWidgetService.java`**

```java
package com.example.todolist.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class TasksWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TasksRemoteViewsFactory(getApplicationContext());
    }
}
```

- [ ] **Step 2: Create `TasksRemoteViewsFactory.java`**

```java
package com.example.todolist.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.todolist.R;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.Task;
import com.example.todolist.data.Topic;
import com.example.todolist.reminder.ReminderScheduler;
import com.example.todolist.util.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads pending tasks synchronously from Room and renders each widget row. */
class TasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context ctx;
    private final List<Task> tasks = new ArrayList<>();
    private final Map<Long, String> topicColors = new HashMap<>();

    TasksRemoteViewsFactory(Context ctx) { this.ctx = ctx; }

    @Override public void onCreate() { }

    @Override
    public void onDataSetChanged() {
        AppDatabase db = AppDatabase.getInstance(ctx);
        tasks.clear();
        tasks.addAll(db.taskDao().getPendingSync());
        topicColors.clear();
        for (Topic t : db.topicDao().getAllSync()) {
            topicColors.put(t.id, t.colorHex);
        }
    }

    @Override public void onDestroy() { tasks.clear(); topicColors.clear(); }

    @Override public int getCount() { return tasks.size(); }

    @Override
    public RemoteViews getViewAt(int position) {
        Task t = tasks.get(position);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_item_task);

        rv.setTextViewText(R.id.widget_item_title, t.title);

        if (t.dueAt != null) {
            rv.setTextViewText(R.id.widget_item_due, DateUtils.formatDue(t.dueAt));
            rv.setViewVisibility(R.id.widget_item_due, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.widget_item_due, View.GONE);
        }

        String hex = t.topicId != null ? topicColors.get(t.topicId) : null;
        if (hex != null) {
            try {
                rv.setInt(R.id.widget_item_dot, "setColorFilter", Color.parseColor(hex));
                rv.setViewVisibility(R.id.widget_item_dot, View.VISIBLE);
            } catch (IllegalArgumentException e) {
                rv.setViewVisibility(R.id.widget_item_dot, View.GONE);
            }
        } else {
            rv.setViewVisibility(R.id.widget_item_dot, View.GONE);
        }

        Intent fill = new Intent().putExtra(ReminderScheduler.EXTRA_TASK_ID, t.id);
        rv.setOnClickFillInIntent(R.id.widget_item_root, fill);
        return rv;
    }

    @Override public RemoteViews getLoadingView() { return null; }
    @Override public int getViewTypeCount() { return 1; }
    @Override public long getItemId(int position) { return tasks.get(position).id; }
    @Override public boolean hasStableIds() { return true; }
}
```

- [ ] **Step 3: Create `TasksWidgetProvider.java`**

```java
package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.ui.detail.TaskDetailActivity;

public class TasksWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);

            Intent svc = new Intent(context, TasksWidgetService.class);
            svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            rv.setRemoteAdapter(R.id.widget_list, svc);
            rv.setEmptyView(R.id.widget_list, R.id.widget_empty);

            Intent openApp = new Intent(context, MainActivity.class);
            PendingIntent appPi = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.widget_header, appPi);

            Intent openTask = new Intent(context, TaskDetailActivity.class);
            openTask.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent taskPi = PendingIntent.getActivity(context, 1, openTask,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            rv.setPendingIntentTemplate(R.id.widget_list, taskPi);

            mgr.updateAppWidget(id, rv);
        }
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
    }
}
```

- [ ] **Step 4: Register receiver + service in Manifest**

In `app/src/main/AndroidManifest.xml`, inside `<application>` (alongside the existing `ReminderReceiver`/`BootReceiver`), add:

```xml
<receiver
    android:name=".widget.TasksWidgetProvider"
    android:exported="false">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/tasks_widget_info" />
</receiver>

<service
    android:name=".widget.TasksWidgetService"
    android:permission="android.permission.BIND_REMOTEVIEWS"
    android:exported="false" />
```

- [ ] **Step 5: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Manual verify**

Install on emulator. Long-press home screen → Widgets → find "Tasca" → drop the widget. It shows the current pending tasks (title, due time monospace, topic dot color). Empty DB state shows "Không có việc nào". Tap a row → opens that task's `TaskDetailActivity`. Tap the header → opens the app.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/todolist/widget/ app/src/main/AndroidManifest.xml
git commit -m "feat: home-screen widget listing pending tasks"
```

---

### Task 3: Widget refresh on task writes

**Files:**
- Create: `app/src/main/java/com/example/todolist/widget/WidgetUpdater.java`
- Modify: `app/src/main/java/com/example/todolist/data/TaskRepository.java`

**Interfaces:**
- Consumes: `TasksWidgetProvider` (Task 2); `R.id.widget_list` (Task 1); `AppWidgetManager`.
- Produces: `WidgetUpdater.refresh(Context)` (used by `SettingsFragment` indirectly via task writes; called from `TaskRepository`).

- [ ] **Step 1: Create `WidgetUpdater.java`**

```java
package com.example.todolist.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import com.example.todolist.R;

/** Notifies any placed Tasks widgets to reload their list. No-op when none are placed. */
public final class WidgetUpdater {
    private WidgetUpdater() {}

    public static void refresh(Context ctx) {
        Context app = ctx.getApplicationContext();
        AppWidgetManager mgr = AppWidgetManager.getInstance(app);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(app, TasksWidgetProvider.class));
        if (ids.length > 0) {
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
        }
    }
}
```

- [ ] **Step 2: Store application context in `TaskRepository`**

In `TaskRepository.java`, add an `appContext` field and set it in the constructor. Replace the current field block + constructor:

```java
    private final TaskDao dao;
    private final Context appContext;
    private final Handler main = new Handler(Looper.getMainLooper());

    public TaskRepository(Context ctx) {
        appContext = ctx.getApplicationContext();
        dao = AppDatabase.getInstance(ctx).taskDao();
    }
```

- [ ] **Step 3: Call `WidgetUpdater.refresh` after each write**

Add `import com.example.todolist.widget.WidgetUpdater;` at the top. Then in each write method, call refresh on the executor thread after the DAO call:

```java
    public void insert(Task t, OnId cb) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = dao.insert(t);
            WidgetUpdater.refresh(appContext);
            if (cb != null) main.post(() -> cb.onId(id));
        });
    }

    public void update(Task t) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.update(t);
            WidgetUpdater.refresh(appContext);
        });
    }

    public void delete(Task t) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.delete(t);
            WidgetUpdater.refresh(appContext);
        });
    }
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual verify**

With the widget on the home screen: add a task in the app → it appears in the widget. Toggle a task done → it disappears from the widget (widget shows only pending). Delete a task → it disappears. Edit a task's title/due → the widget row updates.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/todolist/widget/WidgetUpdater.java \
        app/src/main/java/com/example/todolist/data/TaskRepository.java
git commit -m "feat: refresh widget when tasks change"
```

**Note (out of scope, minor):** deleting a *topic* sets its tasks' `topicId=null` via Room FK `SET_NULL` but does not route through `TaskRepository`, so the widget's dot color updates only on the next task write. Acceptable per spec (cosmetic).

---

### Task 4: Settings tab (nav + fragment + actions)

**Files:**
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/java/com/example/todolist/MainActivity.java`
- Create: `app/src/main/java/com/example/todolist/ui/settings/SettingsFragment.java`
- Create: `app/src/main/res/layout/fragment_settings.xml`

**Interfaces:**
- Consumes: `R.id.nav_host` (fragment host), `binding.bottomNav`, `show(Fragment)` in `MainActivity`; `TodoApp.CHANNEL_ID`; `TasksWidgetProvider` (Task 2); strings + `ic_widget`/`ic_bell` (Task 1).
- Produces: `SettingsFragment`, nav item `R.id.nav_settings`.

- [ ] **Step 1: Add nav item**

In `app/src/main/res/menu/bottom_nav_menu.xml`, add after the `nav_topics` item:

```xml
<item
    android:id="@+id/nav_settings"
    android:icon="@drawable/ic_settings"
    android:title="@string/settings" />
```

- [ ] **Step 2: Wire the nav branch in `MainActivity`**

Add the import `import com.example.todolist.ui.settings.SettingsFragment;`. In the `setOnItemSelectedListener` block, add before the closing `return false;`:

```java
            } else if (id == R.id.nav_settings) {
                show(new SettingsFragment());
                return true;
```

- [ ] **Step 3: Create `fragment_settings.xml`**

`app/src/main/res/layout/fragment_settings.xml`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/canvas_mist">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/settings"
        android:textColor="@color/charcoal_ink"
        android:textSize="28sp"
        android:textStyle="bold"
        android:paddingHorizontal="20dp"
        android:paddingTop="24dp"
        android:paddingBottom="16dp" />

    <LinearLayout
        android:id="@+id/row_add_widget"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:background="?attr/selectableItemBackground"
        android:minHeight="60dp"
        android:paddingHorizontal="20dp"
        android:paddingVertical="16dp">
        <ImageView
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:layout_marginEnd="16dp"
            android:src="@drawable/ic_widget"
            android:contentDescription="@string/add_widget" />
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/add_widget"
            android:textColor="@color/charcoal_ink"
            android:textSize="16sp" />
    </LinearLayout>

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginStart="20dp"
        android:background="@color/whisper_border" />

    <LinearLayout
        android:id="@+id/row_test_notification"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:background="?attr/selectableItemBackground"
        android:minHeight="60dp"
        android:paddingHorizontal="20dp"
        android:paddingVertical="16dp">
        <ImageView
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:layout_marginEnd="16dp"
            android:src="@drawable/ic_bell"
            android:contentDescription="@string/test_notification" />
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/test_notification"
            android:textColor="@color/charcoal_ink"
            android:textSize="16sp" />
    </LinearLayout>

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginStart="20dp"
        android:background="@color/whisper_border" />

    <TextView
        android:id="@+id/notif_status"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        android:paddingTop="16dp"
        android:textColor="@color/muted_steel"
        android:textSize="13sp" />
</LinearLayout>
```

- [ ] **Step 4: Create `SettingsFragment.java`**

`app/src/main/java/com/example/todolist/ui/settings/SettingsFragment.java`:

```java
package com.example.todolist.ui.settings;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.todolist.R;
import com.example.todolist.TodoApp;
import com.example.todolist.databinding.FragmentSettingsBinding;
import com.example.todolist.widget.TasksWidgetProvider;

public class SettingsFragment extends Fragment {

    private static final int TEST_NOTIFICATION_ID = 9001;
    private FragmentSettingsBinding b;

    private final ActivityResultLauncher<String> notifPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) postTestNotification();
            else toast(getString(R.string.notif_permission_needed));
            updatePermissionCaption();
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentSettingsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.rowAddWidget.setOnClickListener(v -> requestPinWidget());
        b.rowTestNotification.setOnClickListener(v -> onTestNotification());
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissionCaption();
    }

    private void requestPinWidget() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(requireContext());
        ComponentName provider = new ComponentName(requireContext(), TasksWidgetProvider.class);
        if (mgr.isRequestPinAppWidgetSupported()) {
            mgr.requestPinAppWidget(provider, null, null);
        } else {
            toast(getString(R.string.widget_pin_unsupported));
        }
    }

    private void onTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        postTestNotification();
    }

    private void postTestNotification() {
        NotificationCompat.Builder n =
            new NotificationCompat.Builder(requireContext(), TodoApp.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.test_notification_body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(requireContext()).notify(TEST_NOTIFICATION_ID, n.build());
    }

    private void updatePermissionCaption() {
        boolean on = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
        b.notifStatus.setText(getString(on ? R.string.notif_on : R.string.notif_off));
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
```

- [ ] **Step 5: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. ViewBinding generates `FragmentSettingsBinding` with `rowAddWidget`, `rowTestNotification`, `notifStatus`.

- [ ] **Step 6: Manual verify**

Launch app → tap **Settings** tab. Caption shows notification on/off. Tap "Test thông báo" → if permission not granted, the system dialog appears; on grant, a "Tasca / Đây là thông báo thử" notification posts immediately via the reminders channel. Tap "Thêm widget vào màn hình chính" → supported launcher shows the pin dialog; unsupported → Toast. Switching between all three tabs works.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/menu/bottom_nav_menu.xml \
        app/src/main/java/com/example/todolist/MainActivity.java \
        app/src/main/java/com/example/todolist/ui/settings/SettingsFragment.java \
        app/src/main/res/layout/fragment_settings.xml
git commit -m "feat: settings tab with add-widget and test-notification actions"
```

---

## Self-Review

**Spec coverage:**
- Settings tab as 3rd BottomNav item → Task 4 (menu + MainActivity branch). ✓
- Add-widget action (`requestPinAppWidget` + unsupported Toast) → Task 4. ✓
- Test-notification (immediate, channel `reminders`, POST_NOTIFICATIONS flow, id 9001) → Task 4. ✓
- Permission-status caption → Task 4 (`updatePermissionCaption` in `onResume`). ✓
- Collection widget of pending tasks (provider/service/factory, layouts, info xml, manifest) → Tasks 1+2. ✓
- Row click opens `TaskDetailActivity` via `task_id`; header opens app → Task 2. ✓
- `WidgetUpdater.refresh` on task writes → Task 3. ✓
- DAO sync getters → Task 1. ✓

**Placeholder scan:** No TBD/TODO; every code step has full content. ✓

**Type consistency:** `getPendingSync()`/`getAllSync()` defined Task 1, used Task 2. `WidgetUpdater.refresh(Context)` defined Task 3, no earlier reference. `TasksWidgetProvider` defined Task 2, referenced Tasks 3+4. Intent key `ReminderScheduler.EXTRA_TASK_ID` (="task_id") matches `TaskDetailActivity`'s `getLongExtra("task_id", -1)`. Binding ids `rowAddWidget`/`rowTestNotification`/`notifStatus` match layout ids `row_add_widget`/`row_test_notification`/`notif_status`. ✓
