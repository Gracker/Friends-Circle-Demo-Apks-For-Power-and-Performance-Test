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
import com.example.wechatfriendforperformance.interfaces.OnPraiseOrCommentClickListener;

import java.util.List;

/**
 * Heavy Load Activity, each frame calculation is heavy, significant load during sliding
 */
public class HeavyLoadActivity extends AppCompatActivity implements OnPraiseOrCommentClickListener {

    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private LinearLayout titleBar;

    private RequestBuilder<Drawable> imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("HeavyLoadActivity_onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_load);

        // Process image name, remove possible file extension
        imageLoader = Glide.with(this).asDrawable().apply(
                new RequestOptions().centerCrop()
        );

        // Try to load from resource ID, note that resource names must be all lowercase
        titleBar = findViewById(R.id.title_bar);
        findViewById(R.id.back_button).setOnClickListener(v -> onBackPressed());

        // Load placeholder image
        Glide.with(this)
                .load(R.drawable.avatar_placeholder)
                .preload();

        // If error, use placeholder image
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        Trace.endSection();
    }

    @Override
    protected void onResume() {
        Trace.beginSection("HeavyLoadActivity_onResume");
        super.onResume();
        Trace.endSection();
    }

    private void initRecyclerView() {
        Trace.beginSection("HeavyLoadActivity_initRecyclerView");
        
        // Set layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Create adapter, using heavy load mode
        adapter = new PerformanceFriendCircleAdapter(this, recyclerView, PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY);
        
        // Add header view
        View headerView = getLayoutInflater().inflate(R.layout.item_header_view, recyclerView, false);
        adapter.setHeaderView(headerView);
        
        recyclerView.setAdapter(adapter);
        
        // Load data
        adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans());
        
        // Preload images
        preloadImages();
        
        Trace.endSection();
    }

    private void preloadImages() {
        Trace.beginSection("HeavyLoadActivity_preloadImages");
        List<FriendCircleBean> beans = PerformanceDataCenter.getInstance().getFriendCircleBeans();
        
        // Preload avatars
        for (FriendCircleBean bean : beans) {
            if (bean.getUserBean() != null && bean.getUserBean().getUserAvatarUrl() != null) {
                String avatarUrl = bean.getUserBean().getUserAvatarUrl();
                if (avatarUrl.contains(".")) {
                    avatarUrl = avatarUrl.substring(0, avatarUrl.lastIndexOf("."));
                }
                preloadImage(this, avatarUrl);
            }
        }
        
        // Preload images
        for (FriendCircleBean bean : beans) {
            if (bean.getImageUrls() != null) {
                for (String imageUrl : bean.getImageUrls()) {
                    preloadImage(this, imageUrl);
                }
            }
        }
        Trace.endSection();
    }

    private void preloadImage(Context context, String imageName) {
        try {
            int resourceId = context.getResources().getIdentifier(
                    imageName.toLowerCase(), "drawable", context.getPackageName());
            if (resourceId != 0) {
                Glide.with(context).load(resourceId).preload();
            }
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPraiseClick(int position) {
        // Like functionality, performance test version doesn't need actual functionality
        Trace.beginSection("HeavyLoadActivity_onPraiseClick");
        // Simulate like operation
        Trace.endSection();
    }

    @Override
    public void onCommentClick(View view, int position) {
        // Comment functionality, performance test version doesn't need actual functionality
        Trace.beginSection("HeavyLoadActivity_onCommentClick");
        // Simulate comment operation
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