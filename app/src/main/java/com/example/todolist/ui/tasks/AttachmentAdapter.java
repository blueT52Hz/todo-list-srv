package com.example.todolist.ui.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.todolist.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal strip of attachment thumbnails, each with a remove button, followed
 * by a trailing dashed "ADD" tile. The last position is always the add tile.
 */
public class AttachmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_THUMB = 0;
    private static final int TYPE_ADD = 1;

    public interface Listener {
        void onAdd();
        void onRemove(String path);
    }

    private final List<String> paths = new ArrayList<>();
    private final Listener listener;

    public AttachmentAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<String> newPaths) {
        paths.clear();
        if (newPaths != null) paths.addAll(newPaths);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return paths.size() + 1; // + add tile
    }

    @Override
    public int getItemViewType(int position) {
        return position == paths.size() ? TYPE_ADD : TYPE_THUMB;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            View v = inf.inflate(R.layout.item_attachment_add, parent, false);
            return new AddVH(v);
        }
        View v = inf.inflate(R.layout.item_attachment_thumb, parent, false);
        return new ThumbVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddVH) {
            holder.itemView.setOnClickListener(v -> listener.onAdd());
            return;
        }
        ThumbVH h = (ThumbVH) holder;
        String path = paths.get(position);
        Glide.with(h.thumb).load(new File(path)).centerCrop().into(h.thumb);
        h.remove.setOnClickListener(v -> listener.onRemove(path));
    }

    static class ThumbVH extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final ImageView remove;
        ThumbVH(@NonNull View v) {
            super(v);
            thumb = v.findViewById(R.id.thumb);
            remove = v.findViewById(R.id.btn_remove);
        }
    }

    static class AddVH extends RecyclerView.ViewHolder {
        AddVH(@NonNull View v) { super(v); }
    }
}
