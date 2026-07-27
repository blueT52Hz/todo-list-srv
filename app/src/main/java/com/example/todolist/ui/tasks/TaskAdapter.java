package com.example.todolist.ui.tasks;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.todolist.R;
import com.example.todolist.data.Task;
import com.example.todolist.util.DateUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.VH> {

    public interface Listener {
        void onOpen(Task task);
        void onToggle(Task task);
    }

    private final Listener listener;
    private Map<Long, String> topicColors = new HashMap<>();

    public TaskAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setTopicColors(Map<Long, String> colors) {
        this.topicColors = colors != null ? colors : new HashMap<>();
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<Task> DIFF = new DiffUtil.ItemCallback<Task>() {
        @Override public boolean areItemsTheSame(@NonNull Task a, @NonNull Task b) { return a.id == b.id; }
        @Override public boolean areContentsTheSame(@NonNull Task a, @NonNull Task b) {
            return a.done == b.done
                && Objects.equals(a.title, b.title)
                && Objects.equals(a.dueAt, b.dueAt)
                && Objects.equals(a.topicId, b.topicId)
                && Objects.equals(a.imagePath, b.imagePath);
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Task t = getItem(position);

        h.title.setText(t.title);
        if (t.done) {
            h.title.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.faint_steel));
            h.title.setPaintFlags(h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.check.setBackgroundResource(R.drawable.bg_checkbox_filled);
            h.check.setImageResource(R.drawable.ic_check);
        } else {
            h.title.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.charcoal_ink));
            h.title.setPaintFlags(h.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            h.check.setBackgroundResource(R.drawable.bg_checkbox_ring);
            h.check.setImageDrawable(null);
        }

        // Due caption (mono); overdue → soft clay when still pending
        if (t.dueAt != null) {
            h.due.setVisibility(View.VISIBLE);
            h.due.setText(DateUtils.formatDue(t.dueAt));
            int color = (!t.done && DateUtils.isOverdue(t.dueAt)) ? R.color.soft_clay : R.color.muted_steel;
            h.due.setTextColor(ContextCompat.getColor(h.itemView.getContext(), color));
        } else {
            h.due.setVisibility(View.GONE);
        }

        // Topic dot
        String hex = t.topicId != null ? topicColors.get(t.topicId) : null;
        if (hex != null) {
            h.dot.setVisibility(View.VISIBLE);
            try { h.dot.getBackground().setTint(Color.parseColor(hex)); }
            catch (IllegalArgumentException ignored) { }
        } else {
            h.dot.setVisibility(View.GONE);
        }

        // Thumbnail (first attachment)
        String firstImage = com.example.todolist.util.ImagePaths.first(t.imagePath);
        if (firstImage != null) {
            h.thumb.setVisibility(View.VISIBLE);
            Glide.with(h.itemView).load(new File(firstImage)).centerCrop().into(h.thumb);
        } else {
            h.thumb.setVisibility(View.GONE);
            Glide.with(h.itemView).clear(h.thumb);
        }

        h.check.setOnClickListener(v -> listener.onToggle(t));
        h.itemView.setOnClickListener(v -> listener.onOpen(t));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView check, thumb;
        final TextView title, due;
        final View dot;
        VH(@NonNull View v) {
            super(v);
            check = v.findViewById(R.id.check);
            thumb = v.findViewById(R.id.thumb);
            title = v.findViewById(R.id.title);
            due = v.findViewById(R.id.due);
            dot = v.findViewById(R.id.dot);
        }
    }
}
