package com.android.wechatfriendforpower;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.android.wechatfriendforpower.adapters.PowerFriendCircleAdapter;
import com.android.wechatfriendforpower.beans.FriendCircleBean;
import com.android.wechatfriendforpower.interfaces.OnPraiseOrCommentClickListener;
import com.bumptech.glide.Glide;
import com.stfalcon.imageviewer.loader.ImageLoader;

import java.util.List;

/**
 * Power测试专用的主Activity
 */
public class PowerMainActivity extends AppCompatActivity implements OnPraiseOrCommentClickListener {

    private PowerFriendCircleAdapter mFriendCircleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        
        // 创建图片加载器，从本地资源加载图片
        ImageLoader<String> imageLoader = new ImageLoader<String>() {
            @Override
            public void loadImage(ImageView imageView, String imageUrl) {
                int resourceId = imageView.getContext().getResources().getIdentifier(
                    imageUrl, "drawable", imageView.getContext().getPackageName());
                if (resourceId != 0) {
                    Glide.with(imageView.getContext())
                         .load(resourceId)
                         .into(imageView);
                }
            }
        };
        
        // 设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        
        // 创建适配器
        mFriendCircleAdapter = new PowerFriendCircleAdapter(this, recyclerView, imageLoader);
        
        // 添加顶部视图
        View headerView = LayoutInflater.from(this).inflate(R.layout.include_title_bar_view, recyclerView, false);
        mFriendCircleAdapter.setHeaderView(headerView);
        
        recyclerView.setAdapter(mFriendCircleAdapter);
        
        // 加载数据
        loadData();
        
        // 预加载图片
        preloadImages();
    }
    
    /**
     * 加载朋友圈数据
     */
    private void loadData() {
        List<FriendCircleBean> friendCircleBeans = PowerDataCenter.makeFriendCircleBeans(this);
        mFriendCircleAdapter.setFriendCircleBeans(friendCircleBeans);
    }
    
    /**
     * 预加载图片
     */
    private void preloadImages() {
        for (String imageUrl : PowerConstants.SINGLE_IMAGE_URLS) {
            int resourceId = getResources().getIdentifier(
                imageUrl, "drawable", getPackageName());
            if (resourceId != 0) {
                Glide.with(this)
                     .load(resourceId)
                     .preload();
            }
        }
        
        // 预加载头像
        for (String avatarUrl : PowerDataCenter.AVATAR_URLS) {
            int resourceId = getResources().getIdentifier(
                avatarUrl, "drawable", getPackageName());
            if (resourceId != 0) {
                Glide.with(this)
                     .load(resourceId)
                     .preload();
            }
        }
        
        // 预加载固定资源
        int mainAvatarId = getResources().getIdentifier("main_avatar", "drawable", getPackageName());
        if (mainAvatarId != 0) {
            Glide.with(this).load(mainAvatarId).preload();
        }
        
        int mainBgId = getResources().getIdentifier("main_bg", "drawable", getPackageName());
        if (mainBgId != 0) {
            Glide.with(this).load(mainBgId).preload();
        }
    }

    @Override
    public void onPraiseClick(int position) {
        // 点赞功能，Power测试版本不需要实际功能
    }

    @Override
    public void onCommentClick(int position) {
        // 评论功能，Power测试版本不需要实际功能
    }
} 