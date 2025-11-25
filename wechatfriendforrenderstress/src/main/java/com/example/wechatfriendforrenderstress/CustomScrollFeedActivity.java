package com.example.wechatfriendforrenderstress;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.wechatfriendforrenderstress.databinding.ActivityCustomScrollFeedBinding;
import com.example.wechatfriendforrenderstress.ui.state.CustomScrollUiState;
import com.example.wechatfriendforrenderstress.ui.timeline.CustomTimelineView;
import com.example.wechatfriendforrenderstress.ui.timeline.FriendCircleItemRenderer;

import java.util.Random;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Core Activity for displaying RenderThread stress test list.
 * Supports all 10 load types including minimal, in-frame, between-frame, and mixed loads.
 */
@AndroidEntryPoint
public class CustomScrollFeedActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    public static final String EXTRA_LOAD_TYPE = "extra_load_type";

    private ActivityCustomScrollFeedBinding binding;
    private CustomScrollViewModel viewModel;
    private FriendCircleItemRenderer itemRenderer;
    private int loadType = LoadProfile.LOAD_TYPE_LIGHT;
    
    // Background task management
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Choreographer choreographer;
    private boolean isTaskSchedulingEnabled = false;
    private final Random random = new Random(12345L);
    
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomScrollFeedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CustomScrollViewModel.class);
        itemRenderer = new FriendCircleItemRenderer(this);

        loadType = getIntent().getIntExtra(EXTRA_LOAD_TYPE, LoadProfile.LOAD_TYPE_LIGHT);

        CustomTimelineView timelineView = binding.customTimelineView;
        timelineView.setItemRenderer(itemRenderer);
        timelineView.setLoadProfile(loadType);
        timelineView.setRenderStressOverlay(binding.renderStressOverlay);

        View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, timelineView, false);
        itemRenderer.bindHeaderView(headerView, loadType, () -> getOnBackPressedDispatcher().onBackPressed());
        timelineView.setHeaderView(headerView);

        viewModel.getUiState().observe(this, this::renderState);

        if (savedInstanceState == null) {
            viewModel.loadFeed(loadType);
        }
        
        // Start background tasks for between-frame and mixed loads
        if (LoadProfile.isBetweenFramesLoad(loadType) || LoadProfile.isMixedLoad(loadType)) {
            choreographer = Choreographer.getInstance();
            isTaskSchedulingEnabled = true;
            scheduleNextBetweenFrameTask();
            
            if (LoadProfile.isMixedLoad(loadType)) {
                choreographer.postFrameCallback(this);
                scheduleNextDoFrameTask();
            }
        }
    }

    private void renderState(CustomScrollUiState state) {
        if (state == null) {
            return;
        }
        switch (state.getStatus()) {
            case LOADING:
                showStateOverlay(true, getString(R.string.loading_feed));
                break;
            case SUCCESS:
                if (state.getLoadType() == loadType) {
                    binding.customTimelineView.submitData(state.getData(), loadType);
                    showStateOverlay(false, null);
                }
                break;
            case ERROR:
                showStateOverlay(true, getString(R.string.load_failed));
                Toast.makeText(this,
                        state.getError() != null ? state.getError().getMessage() : getString(R.string.load_failed),
                        Toast.LENGTH_SHORT).show();
                break;
            case IDLE:
            default:
                showStateOverlay(false, null);
                break;
        }
    }

    private void showStateOverlay(boolean visible, @Nullable String message) {
        binding.stateContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible && message != null) {
            binding.txtState.setText(message);
        }
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (isTaskSchedulingEnabled && LoadProfile.isMixedLoad(loadType)) {
            choreographer.postFrameCallback(this);
        }
    }
    
    private void scheduleNextDoFrameTask() {
        if (!isTaskSchedulingEnabled || !LoadProfile.isMixedLoad(loadType)) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            executeDoFrameLoad();
            scheduleNextDoFrameTask();
        }, intervalMs);
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            executeBetweenFrameLoad();
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeDoFrameLoad() {
        int intensity;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                intensity = 1000;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                intensity = 2000;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                intensity = 4000;
                break;
            default:
                return;
        }
        
        double sum = 0;
        for (int i = 0; i < intensity; i++) {
            sum += Math.sin(i * 0.1) + Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
    }
    
    private void executeBetweenFrameLoad() {
        int intensity;
        boolean isHeavy = false;
        
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_LIGHT_BETWEEN_FRAMES:
                intensity = 200;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
                intensity = 400;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
                intensity = 800;
                isHeavy = true;
                break;
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                intensity = 120;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                intensity = 160;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                intensity = 107;
                isHeavy = true;
                break;
            default:
                return;
        }
        
        double sum = 0;
        for (int i = 1; i <= intensity; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i);
            if (isHeavy) {
                sum += Math.log(i) + Math.tan(i * 0.01);
            }
        }
    }

    @Override
    protected void onDestroy() {
        isTaskSchedulingEnabled = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
