# Design System: Tasca — Minimal To-do List

> Single source of truth for Google Stitch screen generation. Android mobile app.
> Atmosphere baseline — Density 4, Variance 5, Motion 5.

## 1. Visual Theme & Atmosphere

A calm, gallery-airy task manager that feels like a well-lit paper desk — clinical yet warm. Generous negative space frames one clear action at a time; nothing shouts. Surfaces are near-white with a single confident accent that only appears where the user must act or where completion is celebrated. The mood is focused and quiet: an app you open to think, not to be sold to. Layouts breathe with soft asymmetry — a left-aligned heading, a floating primary action — never a rigid centered stack. Motion is subtle spring-physics, present only to confirm intent (a task checking off, a sheet rising).

## 2. Color Palette & Roles

- **Canvas Mist** (#F7F8F7) — Primary app background, the paper surface
- **Pure Surface** (#FFFFFF) — Card, sheet, and list-row fill
- **Charcoal Ink** (#1C1F1E) — Primary text, headings (Zinc-950 depth, never pure black)
- **Muted Steel** (#6B7270) — Secondary text: dates, topic labels, metadata
- **Faint Steel** (#A0A5A3) — Tertiary: placeholders, disabled, timestamp captions
- **Whisper Border** (rgba(28,31,30,0.08)) — 1px row dividers, input outlines, card edges
- **Muted Sage** (#2F7A6F) — SINGLE accent: primary CTA fill, active filter chip, focus ring, checked task
- **Sage Wash** (rgba(47,122,111,0.10)) — Accent tint: selected topic background, subtle hover
- **Soft Clay** (#B4593F) — Reserved semantic only: overdue reminder text / delete confirm (never decorative)

Rules: exactly ONE accent (Muted Sage, saturation < 60%). No purple/blue neon, no glows, no gradients. Soft Clay is a functional signal, not a second brand color.

## 3. Typography Rules

- **Display / Headings:** `Satoshi` — track-tight, weight-driven hierarchy (Bold for screen titles, Medium for section labels). Controlled scale, never oversized.
- **Body / Task titles:** `Satoshi` — Regular/Medium, relaxed leading, comfortable line length. Task text is the content — give it clarity.
- **Mono:** `JetBrains Mono` — dates, reminder times, counts, topic tallies. All numeric metadata is mono for calm alignment.
- **Banned:** `Inter`, generic system sans, any serif (dashboards/utility UI = sans only). No emoji anywhere. Headline scale via `clamp()`, body min `1rem`.

## 4. Component Stylings

- **Task Row (list item):** White surface, separated by Whisper Border dividers (NOT boxed cards — high airiness). Left: circular checkbox (Whisper Border ring → fills Muted Sage + checkmark when done, checked title gets Faint Steel + strikethrough). Center: task title + mono date/reminder caption below. Right: small topic dot/label + thumbnail chip if image attached. Tactile: row press = subtle Sage Wash background.
- **Primary Button / FAB:** Muted Sage fill, white text, generously rounded (1.25rem). Flat — no outer glow. Active state = tactile 1px translate-down. Floating "+" FAB bottom-right for add-task, min 44px tap target.
- **Topic Chip (filter):** Pill, ghost by default (Whisper Border outline, Muted Steel text). Selected = Sage Wash fill + Muted Sage text + dot. Horizontal scroll row, never wrapped 3-column grid.
- **Inputs (add/edit task & topic):** Label above field, field is underline or soft-outlined box, focus ring in Muted Sage. Error text (Soft Clay) below. Date/reminder picker opens as bottom sheet. Image attach = dashed-border drop zone → shows thumbnail once added.
- **Bottom Sheet (add/edit):** Rises from bottom with spring motion, rounded top corners (1.5rem), Whisper Border top handle. Single primary CTA at bottom.
- **Loaders:** Skeletal shimmer rows matching task-row dimensions. No circular spinners.
- **Empty States:** Composed illustration + one calm line ("No tasks yet — add your first") + single primary action. Never bare "No data".
- **Overdue / Reminder:** Soft Clay mono timestamp + small dot; inline, never a red banner.

## 5. Layout Principles

- Grid-first, mobile single-column (this is an Android phone app — one column always, no exceptions).
- Screen header left-aligned (title + optional count), NOT centered. Filter chip row directly beneath.
- Task list = flat dividers over boxed cards to maximize airiness at Density 4.
- Generous internal padding (screen edges ≥ 20px, row vertical ≥ 16px). Vertical rhythm consistent.
- Full-height sheets use `min-h-[100dvh]` logic, never `h-screen`.
- No overlapping elements — thumbnail, checkbox, text each own a clean spatial zone.
- Max content width respected on tablets (centered ~600px), single column preserved.

## 6. Motion & Interaction

- Spring physics default (`stiffness: 100, damping: 20`) — weighty, premium. No linear easing.
- Task check: checkbox fills + gentle scale pop, then title fades to strikethrough (staggered, not instant).
- List reveal: staggered cascade on first load (small delays per row), never all-at-once mount.
- Bottom sheets slide up with spring; backdrop fades opacity only.
- Filter switch: chip fill transitions, list re-filters with a soft cross-fade.
- Animate `transform` and `opacity` only — never `top/left/width/height`.

## 7. Anti-Patterns (Banned)

- No emojis anywhere
- No `Inter`, no serif fonts, no generic system sans for headings
- No pure black (#000000) — use Charcoal Ink
- No neon / outer-glow shadows, no gradient text, no oversaturated accents
- No purple/blue "AI" aesthetic
- No 3-equal-card feature rows; topics use a horizontal scroll chip row
- No centered screen headers (variance > 4 → left-aligned)
- No overlapping elements or absolute-stacked content
- No generic placeholder names ("John Doe", "Acme") or fake round numbers
- No AI copywriting clichés ("Elevate", "Seamless", "Unleash")
- No "Scroll to explore", bouncing chevrons, or scroll arrows
- No circular loading spinners — skeletal shimmer only
- No broken image links — use `picsum.photos` for task-attachment thumbnails
- No subtasks / nested tasks / checklists inside a task — tasks are strictly single-level (flat). Never add sub-item lists, sub-checkboxes, or task hierarchies on any screen.

## Screens to Generate (feature map)

1. **Task List (home)** — header + count, horizontal topic-filter chips, flat task-row list with checkboxes / dates / topic dots / image thumbnails, FAB "+".
2. **Add / Edit Task** — bottom sheet: title input, topic selector, date & reminder picker, image attach drop-zone, primary Save.
3. **Task Detail** — full task view: title, topic, reminder time, attached image (large), edit/delete actions. Single-level task only — NO subtask list.
4. **Topics Manager** — list of topics with color dots, add/edit/delete rows, add-topic sheet.
5. **Filtered List (by topic)** — task list state with one topic chip active, Sage Wash selected.
6. **Empty State** — composed empty task list with single "Add your first task" action.
