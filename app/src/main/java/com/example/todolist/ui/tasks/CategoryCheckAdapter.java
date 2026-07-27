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
