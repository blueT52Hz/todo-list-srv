# Tasca — Calendar Tab Design Spec

**Date:** 2026-07-26
**Status:** Approved (brainstorming)
**Scope:** Thêm tab Lịch (thứ 4 trên BottomNav) vào app To-do List "Tasca" hiện có: lịch tháng, ngày có task (theo `dueAt`) hiện chấm, bấm 1 ngày → danh sách task của ngày ngay dưới lịch. Không đụng logic Room/reminder/widget cũ.

## 1. Mục tiêu

- Tab **Calendar** hiển thị lịch tháng.
- Ngày nào có task đến hạn (`dueAt` rơi vào ngày đó) → hiện 1 chấm dưới số ngày. Tính cả task đã `done`.
- Bấm 1 ngày → danh sách task của ngày đó hiện ngay dưới lịch (cùng màn), gồm cả done + chưa xong.
- Bấm 1 task trong danh sách → mở `TaskDetailActivity` (tái dùng). Tick done cập nhật như tab Tasks.

## 2. Điều hướng

- Thêm item thứ 4 `nav_calendar` (icon `ic_calendar`, title `@string/calendar`) vào `res/menu/bottom_nav_menu.xml`. BottomNav thứ tự: **Tasks · Topics · Calendar · Settings** (Calendar chèn trước Settings).
- Tạo `ui/calendar/CalendarFragment.java`. Thêm nhánh `nav_calendar` vào `setOnItemSelectedListener` trong `MainActivity.java` (dùng `show(new CalendarFragment())`).
- BottomNavigationView có 4 item vẫn hiện label bình thường (tối đa 5 có label).

## 3. Ngày & timezone

- `Task.dueAt` là epoch millis (nullable). Task không có `dueAt` KHÔNG lên lịch (chỉ ở tab Tasks).
- Dùng `java.time` (khả dụng vì minSdk 34): quy `dueAt` → `LocalDate` theo `ZoneId.systemDefault()`:
  `Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()).toLocalDate()`.
- Khoảng millis của 1 ngày `d`: `from = d.atStartOfDay(zone).toInstant().toEpochMilli()`, `to = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()`; truy vấn `dueAt >= from AND dueAt < to`.
- Khoảng millis của 1 tháng `YearMonth ym`: `from = ym.atDay(1)` đầu ngày; `to = ym.plusMonths(1).atDay(1)` đầu ngày.

## 4. Dữ liệu (Room — thêm vào `TaskDao`)

```java
@Query("SELECT dueAt FROM tasks WHERE dueAt >= :from AND dueAt < :to")
LiveData<List<Long>> getDueAtInRange(long from, long to);

@Query("SELECT * FROM tasks WHERE dueAt >= :from AND dueAt < :to ORDER BY dueAt ASC")
LiveData<List<Task>> getByDueRange(long from, long to);
```

- `getDueAtInRange`: mọi `dueAt` trong khoảng tháng (gồm task done) → ViewModel map thành `Set<LocalDate>` để biết ngày nào có chấm.
- `getByDueRange`: task của 1 ngày (gồm done), sort theo giờ.
- Không đổi query/hàm cũ.

## 5. `CalendarViewModel` (AndroidViewModel + LiveData)

- Repo: dùng `TaskRepository` + `TopicRepository` (hoặc DAO trực tiếp qua `AppDatabase`) như các ViewModel khác.
- State:
  - `MutableLiveData<YearMonth> month` (khởi tạo = tháng hiện tại).
  - `MutableLiveData<LocalDate> selected` (khởi tạo = hôm nay).
- Dẫn xuất:
  - `LiveData<Set<LocalDate>> daysWithTasks` = `Transformations.switchMap(month, ym -> map(getDueAtInRange(monthFrom, monthTo) → Set<LocalDate>))`. Dùng `Transformations.map` lồng để đổi `List<Long>` → `Set<LocalDate>`.
  - `LiveData<List<Task>> dayTasks` = `switchMap(selected, d -> getByDueRange(dayFrom, dayTo))`.
  - `getTopics()` (LiveData) để nạp màu topic cho `TaskAdapter`.
- API: `setMonth(YearMonth)`, `prevMonth()`, `nextMonth()`, `select(LocalDate)`, getters cho các LiveData trên; `update(Task)` (toggle done) và `getTaskById`/không cần — toggle gọi `TaskRepository.update`.

## 6. UI

**`fragment_calendar.xml`** (nền Canvas Mist, `NestedScrollView` hoặc `LinearLayout` dọc):
- Header tháng: `‹`  (ImageView `ic_chevron_left`/`ic_back`) + TextView tên tháng (vd "Tháng 7 2026") + `›` (`ic_chevron_right`, đã có).
- Hàng thứ: 7 TextView cố định — T2 T3 T4 T5 T6 T7 CN (tuần bắt đầu **Thứ Hai**). Muted Steel, monospace nhỏ.
- Lưới ngày: `RecyclerView` id `calendar_grid`, `GridLayoutManager(7)`, cao theo nội dung (`nestedScrollingEnabled=false`).
- Tiêu đề ngày chọn: TextView (vd "Thứ Bảy, 26 Th7") — Charcoal Ink.
- Danh sách task ngày: `RecyclerView` id `day_tasks` (tái dùng `TaskAdapter`). TextView rỗng id `day_empty` ("Không có việc nào") hiện khi list trống.

**`item_calendar_day.xml`** (ô ngày, layout thường — bare `<View>` OK vì không phải RemoteViews):
- FrameLayout/LinearLayout vuông; TextView số ngày (id `day_number`) căn giữa; `View` chấm (id `day_dot`, 6dp, `bg_calendar_dot`) dưới số.
- Trạng thái do adapter set: ngày chọn → nền `bg_calendar_selected` (tròn sage, chữ trắng); hôm nay (không chọn) → nền `bg_calendar_today` (viền sage); ngoài tháng → ẩn (ô trống, không bắt click); có task → hiện `day_dot`, không → ẩn.

**Drawable mới:**
- `ic_calendar.xml` (vector, `fillColor=@color/charcoal_ink`).
- `bg_calendar_selected.xml` (oval solid `muted_sage`).
- `bg_calendar_today.xml` (oval stroke `muted_sage`, solid trong suốt).
- `bg_calendar_dot.xml` (oval solid `muted_sage`) — hoặc tái dùng `bg_topic_dot` + tint; ưu tiên file riêng cho rõ.

## 7. `CalendarDayAdapter` + `CalendarCell`

- `CalendarCell` (model): `LocalDate date` (nullable = ô trống dẫn đầu/cuối lưới), `boolean hasTask`, `boolean selected`, `boolean today`, `boolean inMonth`.
- Fragment dựng danh sách cell mỗi khi `month`/`daysWithTasks`/`selected` đổi: tính số ô trống dẫn đầu = (thứ của ngày 1 − Thứ Hai) mod 7, rồi các ngày 1..lengthOfMonth; đánh dấu `hasTask` theo `Set<LocalDate>`, `selected`/`today` so khớp.
- `CalendarDayAdapter` (RecyclerView.Adapter): bind số + chấm + nền trạng thái; click ô có `date != null` → `viewModel.select(date)`.

## 8. Tương tác

- Mở tab → `month` = tháng nay, `selected` = hôm nay; list hiện task hôm nay.
- Bấm `‹`/`›` → `prevMonth()`/`nextMonth()`; lưới dựng lại; `selected` giữ nguyên (list dưới không đổi cho tới khi bấm ngày khác).
- Bấm 1 ngày → `select(date)`; ô đổi highlight; list dưới cập nhật.
- Bấm task trong list → `TaskDetailActivity` (`putExtra("task_id", id)`). Tick done → `viewModel.update(task)` (kéo theo `WidgetUpdater.refresh` sẵn có trong repo).
- Chấm/list phản ánh dữ liệu realtime qua LiveData (thêm/sửa/xóa task ở tab khác → quay lại thấy cập nhật).

## 9. Tái dùng

- `TaskAdapter` (ui.tasks) dùng lại cho `day_tasks`: cần `setTopicColors(Map<Long,String>)` + `Listener{ onToggle, onOpen }` như `TasksFragment`. Nạp màu topic từ `getTopics()`.
- `DateUtils.formatDue` cho caption giờ trong item task (đã có).

## 10. Kiểm thử

Theo override user (không viết test khi chưa yêu cầu): **kiểm thử thủ công** —
1. Tab Calendar hiện lịch tháng nay, hôm nay được chọn.
2. Ngày có task (due) hiện chấm; ngày không có thì không.
3. Bấm ngày khác → list dưới đổi đúng task ngày đó (gồm done gạch ngang).
4. `‹`/`›` đổi tháng, chấm đúng theo tháng.
5. Bấm task → mở detail; tick done → cập nhật, chấm/list phản ánh.
6. Task không có `dueAt` không xuất hiện trên lịch.

## 11. Rủi ro & lưu ý

- Chiều cao lưới: đặt `RecyclerView` trong scroll dọc cần `nestedScrollingEnabled=false` + `wrap_content` để không cuộn lồng.
- Đổi timezone/DST: quy đổi theo `ZoneId.systemDefault()` tại thời điểm hiển thị — chấp nhận cho phạm vi này.
- Ô ngày vuông: set chiều cao ô = chiều rộng (đo trong adapter/onBindViewHolder hoặc layout square) để lưới cân đối.

## 12. Câu hỏi chưa giải quyết

- Không có.
