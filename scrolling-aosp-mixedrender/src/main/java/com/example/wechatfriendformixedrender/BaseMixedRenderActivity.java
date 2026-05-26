package com.example.wechatfriendformixedrender;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loadconfig.LoadType;
import com.example.loadconfig.ScrollLoadGate;

/**
 * Base Activity demonstrating mixed rendering:
 * - Top: PureRenderAnimationView (SurfaceView with dedicated render thread)
 * - Bottom: RecyclerView (standard UI + RenderThread pipeline)
 * 
 * In systrace you will see both rendering patterns simultaneously.
 */
public abstract class BaseMixedRenderActivity extends AppCompatActivity {

    protected PureRenderAnimationView animationView;
    protected RecyclerView recyclerView;
    protected ListAdapter adapter;
    private RecyclerView.OnScrollListener scrollLoadListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixed_render);

        int loadType = getLoadType();

        // Setup pure render animation view
        animationView = findViewById(R.id.animation_view);
        animationView.setLoadType(loadType);

        // Setup RecyclerView with standard rendering
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ListAdapter();
        adapter.setLoadType(loadType);
        recyclerView.setAdapter(adapter);

        scrollLoadListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@androidx.annotation.NonNull RecyclerView recyclerView, int newState) {
                boolean loadEnabled = ScrollLoadGate.isInertiaState(newState);
                adapter.setLoadEnabled(loadEnabled);
                animationView.setLoadEnabled(loadEnabled);
            }
        };
        recyclerView.addOnScrollListener(scrollLoadListener);
    }

    protected abstract @LoadType.Type int getLoadType();

    @Override
    protected void onDestroy() {
        if (recyclerView != null && scrollLoadListener != null) {
            recyclerView.removeOnScrollListener(scrollLoadListener);
        }
        if (adapter != null) {
            adapter.release();
        }
        if (animationView != null) {
            animationView.release();
        }
        super.onDestroy();
    }
}
