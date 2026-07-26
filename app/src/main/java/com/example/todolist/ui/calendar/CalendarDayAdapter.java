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
