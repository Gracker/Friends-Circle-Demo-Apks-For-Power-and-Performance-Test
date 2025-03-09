package com.example.wechatfriendforperformance;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

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
 * Heavy Load Activity, each frame calculation is large, heavy load during sliding
 */
public class HeavyLoadActivity extends AppCompatActivity {

    private static final String TAG = "HeavyLoadActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private LinearLayout titleBar;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY; // 默认为高负载

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("HeavyLoadActivity_onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_load);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY);
            Log.d(TAG, "onCreate: 从Intent获取负载类型 = " + mLoadType);
        }
        
        // 使用Toast显示当前负载类型
        String loadTypeStr = "高负载";
        switch (mLoadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                loadTypeStr = "轻负载";
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                loadTypeStr = "中负载";
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                loadTypeStr = "高负载";
                break;
        }
        Toast.makeText(this, "当前模式: " + loadTypeStr, Toast.LENGTH_SHORT).show();

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
        Trace.beginSection("HeavyLoadActivity_onResume");
        super.onResume();
        
        // 清空缓存，确保使用正确的负载类型
        PerformanceDataCenter.getInstance().clearCachedData();
        
        // 确保数据已根据正确的负载类型生成
        if (adapter != null) {
            // 刷新数据，确保显示正确的点赞和评论数量
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans(mLoadType));
            Log.d(TAG, "onResume: 重新加载负载类型 = " + mLoadType + " 的数据");
        }
        
        Trace.endSection();
    }

    private void initRecyclerView() {
        Trace.beginSection("HeavyLoadActivity_initRecyclerView");
        
        // Set layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Create adapter, using specified load mode
        adapter = new PerformanceFriendCircleAdapter(this, recyclerView, mLoadType);
        
        // Add header view
        View headerView = getLayoutInflater().inflate(R.layout.item_header_view, recyclerView, false);
        adapter.setHeaderView(headerView);
        
        recyclerView.setAdapter(adapter);
        
        // Load data based on load type
        List<FriendCircleBean> data = PerformanceDataCenter.getInstance().getFriendCircleBeans(mLoadType);
        adapter.setFriendCircleBeans(data);
        Log.d(TAG, "initRecyclerView: 加载负载类型 = " + mLoadType + " 的数据, 数据条数 = " + (data != null ? data.size() : 0));
        
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.stopContinuousLoadSimulation();
        }
    }
} 