package com.example.wechatfriendforcustomscroller;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.wechatfriendforcustomscroller.databinding.ActivityCustomScrollFeedBinding;
import com.example.wechatfriendforcustomscroller.ui.state.CustomScrollUiState;
import com.example.wechatfriendforcustomscroller.ui.timeline.CustomTimelineView;
import com.example.wechatfriendforcustomscroller.ui.timeline.FriendCircleItemRenderer;
import com.example.loadconfig.LoadType;
import com.example.wechatfriendforcustomscroller.ui.timeline.LoadStressSimulator;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Core Activity for displaying custom scroller list.
 * Supports all 10 load types including minimal, in-frame, between-frame, and mixed loads.
 */
@AndroidEntryPoint
public class CustomScrollFeedActivity extends AppCompatActivity {

    public static final String EXTRA_LOAD_TYPE = "extra_load_type";

    private ActivityCustomScrollFeedBinding binding;
    private CustomScrollViewModel viewModel;
    private FriendCircleItemRenderer itemRenderer;
    private int loadType = LoadType.LIGHT;

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

        View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, timelineView, false);
        itemRenderer.bindHeaderView(headerView, loadType, () -> getOnBackPressedDispatcher().onBackPressed());
        timelineView.setHeaderView(headerView);

        viewModel.getUiState().observe(this, this::renderState);

        if (savedInstanceState == null) {
            viewModel.loadFeed(loadType);
        }
        
        // Start background tasks for between-frame, mixed, and long-frame loads
        if (LoadType.isBetweenFramesLoad(loadType) || LoadType.isMixedLoad(loadType) 
                || LoadType.isLongFrameLoad(loadType)) {
            LoadStressSimulator.getInstance().startBackgroundTasks(loadType);
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
    protected void onDestroy() {
        // Stop background tasks
        LoadStressSimulator.getInstance().stopBackgroundTasks();
        super.onDestroy();
    }
}
