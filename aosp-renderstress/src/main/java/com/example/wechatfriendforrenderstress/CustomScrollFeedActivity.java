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

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Core Activity for displaying RenderThread stress test list.
 * 使用统一的 LoadSimulator 执行负载
 */
@AndroidEntryPoint
public class CustomScrollFeedActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    public static final String EXTRA_LOAD_TYPE = "extra_load_type";

    private ActivityCustomScrollFeedBinding binding;
    private CustomScrollViewModel viewModel;
    private FriendCircleItemRenderer itemRenderer;
    private int loadType = LoadType.LIGHT;
    
    // Background task management
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Choreographer choreographer;
    private LoadSimulator mLoadSimulator;
    private boolean isTaskSchedulingEnabled = false;
    private boolean isScrolling = false;
    private final Random random = new Random(LoadConfig.TASK_INTERVAL_SEED);
    
    // Long frame state
    private long scrollStartTime = 0;
    private int longFrameTriggerCount = 0;
    private int currentLongFrameIndex = 0;
    private long[] longFrameTriggerTimes;
    private long lastLongFrameTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomScrollFeedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CustomScrollViewModel.class);
        itemRenderer = new FriendCircleItemRenderer(this);

        loadType = getIntent().getIntExtra(EXTRA_LOAD_TYPE, LoadType.LIGHT);

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
        
        // Initialize LoadSimulator and choreographer
        mLoadSimulator = new LoadSimulator();
        
        if (LoadType.isBetweenFramesLoad(loadType) || LoadType.isMixedLoad(loadType) 
                || LoadType.isLongFrameLoad(loadType)) {
            choreographer = Choreographer.getInstance();
            isTaskSchedulingEnabled = true;
            setupScrollListener();
        }
    }
    
    private void setupScrollListener() {
        CustomTimelineView timelineView = binding.customTimelineView;
        timelineView.setScrollCallback(new CustomTimelineView.ScrollCallback() {
            @Override
            public void onScrollStart() {
                if (!isScrolling && isTaskSchedulingEnabled) {
                    isScrolling = true;
                    if (LoadType.isLongFrameLoad(loadType)) {
                        startLongFrameCycle();
                        choreographer.postFrameCallback(CustomScrollFeedActivity.this);
                    } else {
                        scheduleNextBetweenFrameTask();
                        if (LoadType.isMixedLoad(loadType)) {
                            choreographer.postFrameCallback(CustomScrollFeedActivity.this);
                            scheduleNextDoFrameTask();
                        }
                    }
                }
            }
            
            @Override
            public void onScrollStop() {
                isScrolling = false;
                resetLongFrameState();
                handler.removeCallbacksAndMessages(null);
                if (choreographer != null) {
                    choreographer.removeFrameCallback(CustomScrollFeedActivity.this);
                }
            }
        });
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
        if (!isTaskSchedulingEnabled || !isScrolling) return;
        
        if (LoadType.isLongFrameLoad(loadType)) {
            checkAndExecuteLongFrame();
            choreographer.postFrameCallback(this);
        } else if (LoadType.isMixedLoad(loadType)) {
            choreographer.postFrameCallback(this);
        }
    }
    
    private void startLongFrameCycle() {
        scrollStartTime = System.currentTimeMillis();
        longFrameTriggerCount = LoadConfig.getLongFrameTriggerCount();
        longFrameTriggerTimes = LoadConfig.getLongFrameTriggerTimes(longFrameTriggerCount);
        currentLongFrameIndex = 0;
        lastLongFrameTime = 0;
    }
    
    private void resetLongFrameState() {
        scrollStartTime = 0;
        currentLongFrameIndex = 0;
        longFrameTriggerTimes = null;
    }
    
    private void checkAndExecuteLongFrame() {
        if (currentLongFrameIndex >= longFrameTriggerCount || longFrameTriggerTimes == null) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - scrollStartTime;
        
        if (elapsedTime >= longFrameTriggerTimes[currentLongFrameIndex]) {
            if (currentTime - lastLongFrameTime >= LoadConfig.LONG_FRAME_MIN_INTERVAL_MS) {
                mLoadSimulator.executeLongFrameLoad("RenderStress_longFrameLoad_" + (currentLongFrameIndex + 1));
                lastLongFrameTime = currentTime;
                currentLongFrameIndex++;
            }
        }
        
        if (elapsedTime >= LoadConfig.LONG_FRAME_SCROLL_PERIOD_MS) {
            startLongFrameCycle();
        }
    }
    
    private void scheduleNextDoFrameTask() {
        if (!isTaskSchedulingEnabled || !isScrolling || !LoadType.isMixedLoad(loadType)) return;
        
        int intervalMs = LoadConfig.MIN_TASK_INTERVAL_MS + random.nextInt(LoadConfig.MAX_TASK_INTERVAL_MS - LoadConfig.MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled || !isScrolling) return;
            mLoadSimulator.executeDoFrameLoad(loadType, "RenderStress_doFrameLoad");
            scheduleNextDoFrameTask();
        }, intervalMs);
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled || !isScrolling) return;
        if (!LoadType.isBetweenFramesLoad(loadType) && !LoadType.isMixedLoad(loadType)) return;
        
        int intervalMs = LoadConfig.MIN_TASK_INTERVAL_MS + random.nextInt(LoadConfig.MAX_TASK_INTERVAL_MS - LoadConfig.MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled || !isScrolling) return;
            if (LoadType.isBetweenFramesLoad(loadType)) {
                mLoadSimulator.executePureBetweenFrameLoad(loadType, "RenderStress_betweenFrameLoad");
            } else if (LoadType.isMixedLoad(loadType)) {
                mLoadSimulator.executeBetweenFrameLoad(loadType, "RenderStress_betweenFrameLoad");
            }
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }

    @Override
    protected void onDestroy() {
        isTaskSchedulingEnabled = false;
        handler.removeCallbacksAndMessages(null);
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
        super.onDestroy();
    }
}
