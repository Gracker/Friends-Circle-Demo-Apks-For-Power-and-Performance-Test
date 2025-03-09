package com.example.wechatfriendforperformance;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Trace;
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
 * Medium Load Activity, each frame calculation is medium, medium load during sliding
 */
public class MediumLoadActivity extends AppCompatActivity {

    private static final String TAG = "MediumLoadActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private LinearLayout titleBar;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medium_load);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM);
        }
        
        // 使用Toast显示当前负载类型
        String loadTypeStr = "中负载";
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 清空缓存，确保使用正确的负载类型
        PerformanceDataCenter.getInstance().clearCachedData();
        
        // 确保数据已根据正确的负载类型生成
        if (adapter != null) {
            // 刷新数据，确保显示正确的点赞和评论数量
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }
    }

    private void initRecyclerView() {
        // Set layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Create adapter, using specified load mode
        adapter = new PerformanceFriendCircleAdapter(this, recyclerView, mLoadType);
        
        // Add header view - 使用include_title_bar_view.xml而不是item_header_view.xml
        View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
        adapter.setHeaderView(headerView);
        
        recyclerView.setAdapter(adapter);
        
        // 直接在Activity中生成对应负载类型的数据，不依赖DataCenter的缓存
        List<FriendCircleBean> data = PerformanceDataCenter.getInstance().generateDataForLoadType(mLoadType);
        adapter.setFriendCircleBeans(data);
    }

    private String getLoadTypeString(int loadType) {
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                return "轻负载";
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                return "中负载";
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                return "高负载";
            default:
                return "未知负载";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (adapter != null) {
            adapter.stopContinuousLoadSimulation();
        }
        recyclerView.setAdapter(null);
        adapter = null;
        imageLoader = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
} 