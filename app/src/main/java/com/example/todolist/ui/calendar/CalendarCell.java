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
