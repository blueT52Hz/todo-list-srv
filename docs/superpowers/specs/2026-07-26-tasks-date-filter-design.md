# Tasca — Tasks Date Filter (thay tab Calendar) Design Spec

**Date:** 2026-07-26
**Status:** Approved (brainstorming)
**Scope:** Bỏ tab Calendar (chưa merge), thay bằng: trong tab **Tasks** có nút "Chọn ngày" mở picker lịch tự dựng (có chấm đánh dấu ngày có task); chọn 1 ngày → list chỉ hiện task ngày đó; nút × để bỏ, quay lại xem toàn bộ. Làm trên **cùng nhánh `feat/calendar-tab`** (cùng task lịch).

## 1. Mục tiêu

- Không còn tab Calendar riêng.
- Tab Tasks có nút "Chọn ngày" → mở dialog lịch tháng tự dựng, ngày có task (`dueAt`) hiện chấm dưới số (gồm cả done).
- Chọn 1 ngày → danh sách task lọc về đúng ngày đó (mọi topic, gồm done + chưa xong).
- Nút × bỏ lọc ngày → về danh sách toàn bộ (theo topic như cũ).
- Ngày và topic **loại trừ nhau**: chọn ngày thì bỏ lọc topic; bấm chip topic thì bỏ ngày.

## 2. Git & tái dùng

Làm trên nhánh `feat/calendar-tab` hiện có (đang chứa code tab Calendar, chưa merge vào main).

**Giữ lại / tái dùng (đã có trên nhánh):**
- `ui/calendar/CalendarCell.java`, `ui/calendar/CalendarDayAdapter.java`
- `res/layout/item_calendar_day.xml`
- `res/drawable/bg_calendar_selected.xml`, `bg_calendar_today.xml`, `bg_calendar_dot.xml`, `ic_chevron_left.xml`
- `TaskDao.getDueAtInRange(long,long)`, `TaskDao.getByDueRange(long,long)`
- `res/values/strings.xml`: giữ `prev_month`, `next_month`; **bỏ** `calendar`, `day_empty` (không còn dùng); thêm strings mới ở mục 6.

**Xoá (chỉ phục vụ tab Calendar):**
- `ui/calendar/CalendarFragment.java`, `ui/calendar/CalendarViewModel.java`
- `res/layout/fragment_calendar.xml`
- `res/drawable/ic_calendar.xml`
- Item `nav_calendar` trong `res/menu/bottom_nav_menu.xml` + nhánh `nav_calendar` + import `CalendarFragment` trong `MainActivity.java`.
- String `calendar`, `day_empty` (nếu không còn tham chiếu).

## 3. `DatePickerDialogFragment` (picker tự dựng có chấm)

- `androidx.fragment.app.DialogFragment`, package **`ui.calendar`** (cùng package với `CalendarCell`/`CalendarDayAdapter` vốn package-private — đặt cùng package để truy cập được; `ui.calendar` giờ là "package picker lịch tái dùng").
- Layout `res/layout/dialog_date_picker.xml`: header `‹` (`ic_chevron_left`) + TextView tên tháng + `›` (`ic_chevron_right`); hàng thứ T2 T3 T4 T5 T6 T7 CN (tuần bắt đầu Thứ Hai); `RecyclerView` id `picker_grid` (`GridLayoutManager(7)`). Nền Canvas Mist, bo góc. Không có list task.
- State nội bộ: `YearMonth month` (khởi tạo tháng hiện tại) + `LocalDate selected` (truyền vào khi mở, mặc định hôm nay).
- Chấm: observe `taskDao.getDueAtInRange(monthStart, monthEnd)` cho tháng đang xem → `Set<LocalDate>` → dựng cell qua `CalendarDayAdapter` (giống logic `rebuildGrid`: blank dẫn đầu = `atDay(1).getDayOfWeek().getValue()-1`, cell 1..lengthOfMonth, cờ `hasTask`/`selected`/`today`). Đổi tháng bằng `‹ ›` → observe lại tháng mới.
- Truy cập DAO: qua `AppDatabase.getInstance(ctx).taskDao()`; observe bằng `getViewLifecycleOwner()`/`this`.
- Bấm 1 ngày → trả kết quả về Tasks qua **FragmentResult** (`setFragmentResult(REQUEST_KEY, bundleOf(RESULT_EPOCH_DAY → date.toEpochDay()))`) rồi `dismiss()`.
- Hằng số public: `REQUEST_KEY = "date_picker"`, `RESULT_EPOCH_DAY = "epoch_day"`; `newInstance(LocalDate initial)` set arg `epochDay`.

## 4. Sửa `TasksFragment` + `fragment_tasks.xml`

- `fragment_tasks.xml`: thêm 1 hàng ở đầu (trên/ cạnh hàng chip topic): nút `btn_pick_date` (icon lịch + text, mặc định "Chọn ngày") + `btn_clear_date` (ImageView ×, `visibility=gone`). Dùng view/nút hợp phong cách Tasca (không CardView).
- `TasksFragment`:
  - `btn_pick_date` → `DatePickerDialogFragment.newInstance(currentSelectedOrToday).show(getChildFragmentManager(), "date_picker")`.
  - Đăng ký `getChildFragmentManager().setFragmentResultListener(REQUEST_KEY, ...)` trong `onViewCreated`: đọc `epochDay` → `LocalDate.ofEpochDay(...)` → `viewModel.setDate(date)`; đổi text `btn_pick_date` thành ngày (vd "26/07"), hiện `btn_clear_date`.
  - `btn_clear_date` → `viewModel.clearDate()`; text nút về "Chọn ngày"; ẩn ×.
  - Khi bấm chip topic (listener sẵn có gọi `viewModel.setFilter(...)`): ViewModel tự `clearDate()` → cần đồng bộ UI nút: quan sát `viewModel.getDate()` để reset nhãn/× khi date về null.
  - `count` cập nhật theo list như hiện tại.
- Observe `viewModel.getDate()` (LiveData<LocalDate>) để cập nhật trạng thái nút (nhãn + hiện/ẩn ×) — 1 nguồn sự thật, tránh lệch khi date bị clear gián tiếp qua chọn topic.

## 5. Sửa `TasksViewModel`

- Thêm `MutableLiveData<LocalDate> dateFilter` (khởi tạo `null`), `ZoneId zone = ZoneId.systemDefault()`.
- Giữ nguồn lọc topic hiện tại (đặt tên nội bộ vd `topicTasks` = switchMap trên filter topic → `getAll()`/`getByTopic()`).
- `tasks` (nguồn UI observe) đổi thành:
  `Transformations.switchMap(dateFilter, d -> d == null ? topicTasks : taskDao.getByDueRange(startMillis(d), startMillis(d.plusDays(1))))`.
- `startMillis(LocalDate d) = d.atStartOfDay(zone).toInstant().toEpochMilli()`.
- API mới: `setDate(LocalDate)`, `clearDate()` (= `setDate(null)` hoặc set null), `getDate() : LiveData<LocalDate>`.
- `setFilter(Long topicId)` (hiện có) thêm dòng `dateFilter.setValue(null)` để chọn topic thì bỏ ngày.
- Giữ nguyên `getTopics`, `getFilter`, `insert/update/delete/getTaskById`.

**Lưu ý:** danh sách ngày (`getByDueRange`) gồm cả done, sort theo `dueAt` (query đã có). Danh sách topic/all giữ hành vi cũ (done ở cuối).

## 6. Strings

- Thêm: `pick_date` ("Chọn ngày"), `clear_date` ("Bỏ lọc ngày" — content description nút ×).
- Bỏ (nếu không còn tham chiếu sau khi xoá tab): `calendar`, `day_empty`.

## 7. Kiểm thử

Theo override user (không viết test khi chưa yêu cầu): **kiểm thử thủ công** —
1. Không còn tab Calendar trên BottomNav (còn 3 tab: Tasks · Topics · Settings).
2. Tab Tasks có nút "Chọn ngày"; bấm → dialog lịch, ngày có task hiện chấm.
3. `‹ ›` đổi tháng, chấm đúng theo tháng.
4. Bấm 1 ngày → dialog đóng, list chỉ còn task ngày đó (gồm done), nút hiện ngày + ×.
5. Bấm × → về danh sách toàn bộ; bấm chip topic khi đang lọc ngày → bỏ ngày, lọc theo topic, nút về "Chọn ngày".
6. Thêm/sửa/xoá task xong, mở lại picker → chấm cập nhật.

## 8. Rủi ro & lưu ý

- Đồng bộ nhãn nút với `dateFilter`: dùng observe `getDate()` làm nguồn sự thật (mục 4) để không lệch khi date bị clear gián tiếp.
- `DialogFragment` với RecyclerView grid: đặt chiều rộng dialog hợp lý (match_parent theo % màn) để 7 cột cân.
- Timezone: quy đổi theo `ZoneId.systemDefault()` như phần lịch cũ.
- Không đụng widget/reminder/topics; data model không đổi.

## 9. Câu hỏi chưa giải quyết

- Không có.
