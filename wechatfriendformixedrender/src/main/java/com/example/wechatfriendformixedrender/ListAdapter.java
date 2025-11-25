package com.example.wechatfriendformixedrender;

import android.graphics.Color;
import android.os.Trace;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Random;

/**
 * Standard RecyclerView adapter that renders using normal UI + RenderThread pipeline.
 * Combined with PureRenderAnimationView, this creates mixed rendering scenario.
 */
public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    
    private int loadType = LoadProfile.LOAD_TYPE_MINIMAL;
    private final Random random = new Random(12345L);
    
    private static final int ITEM_COUNT = 100;
    private static final String[] NAMES = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry"};
    private static final String[] CONTENTS = {
            "Working on new features today!",
            "Just finished a great book",
            "Coffee break time ☕",
            "Meeting went well",
            "Weekend plans!",
            "Learning something new",
            "Project update coming",
            "Beautiful day outside"
    };

    public void setLoadType(@LoadProfile.LoadType int loadType) {
        this.loadType = loadType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trace.beginSection("MixedRender_onBindViewHolder");
        
        // Set data
        holder.nameText.setText(NAMES[position % NAMES.length]);
        holder.contentText.setText(CONTENTS[position % CONTENTS.length]);
        holder.timeText.setText((position % 60) + " min ago");
        
        // Set avatar color
        int hue = (position * 37) % 360;
        holder.avatarView.setBackgroundColor(Color.HSVToColor(new float[]{hue, 0.5f, 0.8f}));
        
        // Execute load
        executeLoad();
        
        Trace.endSection();
    }

    @Override
    public int getItemCount() {
        return ITEM_COUNT;
    }
    
    private void executeLoad() {
        int iterations;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_MINIMAL:
                iterations = 0;
                break;
            case LoadProfile.LOAD_TYPE_LIGHT:
            case LoadProfile.LOAD_TYPE_LIGHT_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                iterations = 50;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM:
            case LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                iterations = 250;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY:
            case LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                iterations = 1000;
                break;
            default:
                iterations = 0;
        }
        
        if (iterations == 0) return;
        
        Trace.beginSection("MixedRender_UIThread_load");
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        Trace.endSection();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View avatarView;
        TextView nameText;
        TextView contentText;
        TextView timeText;

        ViewHolder(View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.avatar);
            nameText = itemView.findViewById(R.id.name);
            contentText = itemView.findViewById(R.id.content);
            timeText = itemView.findViewById(R.id.time);
        }
    }
}

