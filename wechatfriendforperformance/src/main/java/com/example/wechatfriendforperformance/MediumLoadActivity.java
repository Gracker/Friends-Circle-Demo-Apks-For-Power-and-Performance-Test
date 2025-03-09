package com.example.wechatfriendforperformance;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Trace;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.example.wechatfriendforperformance.adapters.PerformanceFriendCircleAdapter;
import com.example.wechatfriendforperformance.beans.FriendCircleBean;

import java.util.List;

/**
 * Medium Load Activity, each frame calculation is medium, medium load during sliding
 */
public class MediumLoadActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private LinearLayout titleBar;

    private RequestBuilder<Drawable> imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("MediumLoadActivity_onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medium_load);

        // Process image name, remove possible file extension
        imageLoader = Glide.with(this).asDrawable().apply(
                new RequestOptions().centerCrop()
        );

        // Try to load from resource ID, note that resource names must be all lowercase
        titleBar = findViewById(R.id.title_bar);
        findViewById(R.id.back_button).setOnClickListener(v -> onBackPressed());

        // If error, use placeholder image
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        Trace.endSection();
    }

    @Override
    protected void onResume() {
        Trace.beginSection("MediumLoadActivity_onResume");
        super.onResume();
        Trace.endSection();
    }

    private void initRecyclerView() {
        Trace.beginSection("MediumLoadActivity_initRecyclerView");
        
        // Set layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Create adapter, using medium load mode
        adapter = new PerformanceFriendCircleAdapter(this, recyclerView, PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM);
        
        // Add header view
        View headerView = getLayoutInflater().inflate(R.layout.item_header_view, recyclerView, false);
        adapter.setHeaderView(headerView);
        
        recyclerView.setAdapter(adapter);
        
        // Load data based on load type
        adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans(PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM));
        
        Trace.endSection();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
} 