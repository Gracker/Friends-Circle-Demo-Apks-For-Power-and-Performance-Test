package com.example.wechatfriendforperformance;

import android.os.Bundle;
import android.os.Trace;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechatfriendforperformance.adapters.PerformanceFriendCircleAdapter;
import com.example.wechatfriendforperformance.beans.FriendCircleBean;
import com.example.wechatfriendforperformance.interfaces.OnPraiseOrCommentClickListener;
import com.bumptech.glide.Glide;
import com.stfalcon.imageviewer.loader.ImageLoader;

import java.util.List;

/**
 * 轻负载Activity，每一帧的计算量较小，滑动过程中每帧负载轻
 */
public class LightLoadActivity extends AppCompatActivity implements OnPraiseOrCommentClickListener {

    private PerformanceFriendCircleAdapter mFriendCircleAdapter;

    private ImageLoader<String> mImageLoader = new ImageLoader<String>() {
        @Override
        public void loadImage(ImageView imageView, String imageUrl) {
            Trace.beginSection("LightLoad_loadImage");
            try {
                // 处理图片名称，去掉可能的文件扩展名
                String imageName = imageUrl;
                if (imageName.contains(".")) {
                    imageName = imageName.substring(0, imageName.lastIndexOf("."));
                }
                
                // 尝试从资源ID加载，注意资源名称必须全小写
                int resourceId = getResources().getIdentifier(
                    imageName.toLowerCase(), "drawable", getPackageName());
                
                if (resourceId != 0) {
                    Glide.with(LightLoadActivity.this)
                        .load(resourceId)
                        .centerCrop()
                        .into(imageView);
                } else {
                    // 加载占位图
                    Glide.with(LightLoadActivity.this)
                        .load(R.drawable.img_placeholder)
                        .centerCrop()
                        .into(imageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 如果出错，使用占位图
                Glide.with(LightLoadActivity.this)
                    .load(R.drawable.img_placeholder)
                    .centerCrop()
                    .into(imageView);
            }
            Trace.endSection();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("LightLoadActivity_onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_friend_circle);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        
        // 设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        
        // 创建适配器，使用轻负载模式
        mFriendCircleAdapter = new PerformanceFriendCircleAdapter(
            this, recyclerView, mImageLoader, PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT);
        
        // 添加顶部视图
        View headerView = LayoutInflater.from(this).inflate(R.layout.include_title_bar_view, recyclerView, false);
        mFriendCircleAdapter.setHeaderView(headerView);
        
        recyclerView.setAdapter(mFriendCircleAdapter);
        
        // 加载数据
        loadData();
        
        // 预加载图片
        preloadImages();
        Trace.endSection();
    }
    
    @Override
    protected void onResume() {
        Trace.beginSection("LightLoadActivity_onResume");
        super.onResume();
        Trace.endSection();
    }
    
    /**
     * 加载朋友圈数据
     */
    private void loadData() {
        Trace.beginSection("LightLoadActivity_loadData");
        List<FriendCircleBean> friendCircleBeans = PerformanceDataCenter.makeFriendCircleBeans(this);
        mFriendCircleAdapter.setFriendCircleBeans(friendCircleBeans);
        Trace.endSection();
    }
    
    /**
     * 预加载图片，以提高渲染性能
     */
    private void preloadImages() {
        Trace.beginSection("LightLoadActivity_preloadImages");
        // 预加载头像
        for (String avatar : PerformanceDataCenter.AVATAR_URLS) {
            int avatarResourceId = getResources().getIdentifier(
                avatar, "drawable", getPackageName());
            if (avatarResourceId != 0) {
                Glide.with(this).load(avatarResourceId).preload();
            }
        }
        
        // 预加载图片
        for (String image : PerformanceConstants.SINGLE_IMAGE_URLS) {
            int imageResourceId = getResources().getIdentifier(
                image, "drawable", getPackageName());
            if (imageResourceId != 0) {
                Glide.with(this).load(imageResourceId).preload();
            }
        }
        Trace.endSection();
    }

    @Override
    public void onPraiseClick(int position) {
        Trace.beginSection("LightLoadActivity_onPraiseClick");
        // 点赞功能，性能测试版本不需要实际功能
        Trace.endSection();
    }

    @Override
    public void onCommentClick(int position) {
        Trace.beginSection("LightLoadActivity_onCommentClick");
        // 评论功能，性能测试版本不需要实际功能
        Trace.endSection();
    }
} 