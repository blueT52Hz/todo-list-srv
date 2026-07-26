package com.example.todolist.ui.topics;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.data.Topic;

public class TopicAdapter extends ListAdapter<Topic, TopicAdapter.VH> {

    public interface OnTopicClick { void onClick(Topic topic); }

    private final OnTopicClick listener;

    public TopicAdapter(OnTopicClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Topic> DIFF = new DiffUtil.ItemCallback<Topic>() {
        @Override public boolean areItemsTheSame(@NonNull Topic a, @NonNull Topic b) { return a.id == b.id; }
        @Override public boolean areContentsTheSame(@NonNull Topic a, @NonNull Topic b) {
            return a.name.equals(b.name) && a.colorHex.equals(b.colorHex);
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Topic topic = getItem(position);
        holder.name.setText(topic.name);
        try {
            holder.dot.getBackground().setTint(Color.parseColor(topic.colorHex));
        } catch (IllegalArgumentException ignored) { /* bad hex → keep default */ }
        holder.itemView.setOnClickListener(v -> listener.onClick(topic));
    }

    static class VH extends RecyclerView.ViewHolder {
        final View dot;
        final TextView name;
        VH(@NonNull View itemView) {
            super(itemView);
            dot = itemView.findViewById(R.id.dot);
            name = itemView.findViewById(R.id.name);
        }
    }
}
