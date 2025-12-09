package com.example.wechatfriendfordouyin;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.wechatfriendfordouyin.view.VerticalVideoScroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 抖音风格视频播放主界面
 * 
 * 内存优化：
 * - 只维护最多3个播放器（当前页、前一页、后一页）
 * - 页面切换时自动释放远离的播放器
 * - 使用SparseArray管理播放器与页面的对应关系
 */
public class DouyinMainActivity extends AppCompatActivity {
    private static final String TAG = "DouyinMainActivity";
    
    // 最多同时保留的播放器数量
    private static final int MAX_PLAYERS = 3;

    private VerticalVideoScroller mVideoScroller;
    private final List<VideoData> mVideoDataList = new ArrayList<>();
    
    // 使用SparseArray管理播放器，key为页面索引
    private final SparseArray<ExoPlayer> mPlayerMap = new SparseArray<>();
    // 保存每个页面的View引用
    private final SparseArray<View> mPageViewMap = new SparseArray<>();
    
    private int mCurrentPlayingIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏沉浸模式
        setupFullScreen();
        
        setContentView(R.layout.activity_douyin_main);

        initVideoData();
        initViews();
    }

    /**
     * 设置全屏模式
     */
    private void setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        
        WindowInsetsControllerCompat windowInsetsController = 
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            // 使用浅色状态栏图标（因为背景是深色）
            windowInsetsController.setAppearanceLightStatusBars(false);
        }
    }

    /**
     * 初始化视频数据
     */
    private void initVideoData() {
        // 从raw目录读取视频
        String[] videoAuthors = {"@LS.", "@奶盖", "@骑行车队", "@户外运动"};
        String[] videoDescriptions = {
            "2025格兰芬多·鏖战腾冲 赛事第一视角🏆 #比赛现场 #公路...",
            "暖阳#阳光是最好的滤镜 #晒太阳 #好天气",
            "买了一台汽车送给维塔利每当他在路上看到这台汽车他都会说太美了，非常经典的一台车。所以我们决定为他寻找一台属于他的汽车。",
            "骑行在路上，风景在心中 #骑行日记 #户外运动"
        };

        Random random = new Random(42L); // 使用固定种子确保数据可重现
        
        // 添加4个视频数据
        mVideoDataList.add(new VideoData(
            "android.resource://" + getPackageName() + "/" + R.raw.video1,
            videoAuthors[0],
            videoDescriptions[0],
            7361, 363, 189, 705
        ));

        mVideoDataList.add(new VideoData(
            "android.resource://" + getPackageName() + "/" + R.raw.video2,
            videoAuthors[1],
            videoDescriptions[1],
            random.nextInt(5000) + 1000,
            random.nextInt(500) + 100,
            random.nextInt(300) + 50,
            random.nextInt(400) + 100
        ));

        mVideoDataList.add(new VideoData(
            "android.resource://" + getPackageName() + "/" + R.raw.video3,
            videoAuthors[2],
            videoDescriptions[2],
            random.nextInt(5000) + 1000,
            random.nextInt(500) + 100,
            random.nextInt(300) + 50,
            random.nextInt(400) + 100
        ));

        mVideoDataList.add(new VideoData(
            "android.resource://" + getPackageName() + "/" + R.raw.video4,
            videoAuthors[3],
            videoDescriptions[3],
            random.nextInt(5000) + 1000,
            random.nextInt(500) + 100,
            random.nextInt(300) + 50,
            random.nextInt(400) + 100
        ));
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        mVideoScroller = findViewById(R.id.video_scroller);
        
        // 设置适配器
        mVideoScroller.setAdapter(new VerticalVideoScroller.VideoPageAdapter() {
            @Override
            public int getItemCount() {
                return mVideoDataList.size();
            }

            @Override
            public View createView(ViewGroup parent, int position) {
                View view = LayoutInflater.from(DouyinMainActivity.this)
                    .inflate(R.layout.item_video_page, parent, false);
                // 保存页面View引用
                mPageViewMap.put(position, view);
                return view;
            }

            @Override
            public void bindView(View view, int position) {
                VideoData data = mVideoDataList.get(position);
                
                // 设置文字信息
                TextView tvAuthor = view.findViewById(R.id.tv_author);
                TextView tvDescription = view.findViewById(R.id.tv_description);
                TextView tvLikeCount = view.findViewById(R.id.tv_like_count);
                TextView tvCommentCount = view.findViewById(R.id.tv_comment_count);
                TextView tvFavoriteCount = view.findViewById(R.id.tv_favorite_count);
                TextView tvShareCount = view.findViewById(R.id.tv_share_count);
                
                tvAuthor.setText(data.author);
                tvDescription.setText(data.description);
                tvLikeCount.setText(formatCount(data.likeCount));
                tvCommentCount.setText(formatCount(data.commentCount));
                tvFavoriteCount.setText(formatCount(data.favoriteCount));
                tvShareCount.setText(formatCount(data.shareCount));
                
                // 注意：不在这里创建播放器，而是在页面切换时按需创建
            }
        });

        // 设置页面切换监听
        mVideoScroller.setOnPageChangeListener(new VerticalVideoScroller.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "Page selected: " + position);
                onPageChanged(position);
            }
        });

        // 初始播放第一个视频（延迟执行确保视图已创建）
        mVideoScroller.post(() -> onPageChanged(0));
    }

    /**
     * 页面切换处理
     * @param position 新的页面位置
     */
    @OptIn(markerClass = UnstableApi.class)
    private void onPageChanged(int position) {
        Log.d(TAG, "onPageChanged: " + mCurrentPlayingIndex + " -> " + position);
        
        // 1. 暂停之前播放的视频
        if (mCurrentPlayingIndex >= 0 && mCurrentPlayingIndex != position) {
            ExoPlayer oldPlayer = mPlayerMap.get(mCurrentPlayingIndex);
            if (oldPlayer != null) {
                oldPlayer.pause();
                Log.d(TAG, "Paused player at position: " + mCurrentPlayingIndex);
            }
        }
        
        // 2. 确保当前页面有播放器
        ExoPlayer player = getOrCreatePlayer(position);
        if (player != null) {
            player.seekTo(0);
            player.play();
            Log.d(TAG, "Playing video at position: " + position);
        }
        
        // 3. 预加载相邻页面的播放器
        if (position > 0) {
            getOrCreatePlayer(position - 1);
        }
        if (position < mVideoDataList.size() - 1) {
            getOrCreatePlayer(position + 1);
        }
        
        // 4. 释放远离的播放器以节省内存
        releaseDistantPlayers(position);
        
        mCurrentPlayingIndex = position;
    }

    /**
     * 获取或创建指定位置的播放器
     */
    @OptIn(markerClass = UnstableApi.class)
    private ExoPlayer getOrCreatePlayer(int position) {
        if (position < 0 || position >= mVideoDataList.size()) {
            return null;
        }
        
        ExoPlayer player = mPlayerMap.get(position);
        if (player != null) {
            return player;
        }
        
        // 创建新的播放器
        View pageView = mPageViewMap.get(position);
        if (pageView == null) {
            Log.w(TAG, "Page view not found for position: " + position);
            return null;
        }
        
        PlayerView playerView = pageView.findViewById(R.id.player_view);
        ProgressBar loadingIndicator = pageView.findViewById(R.id.loading_indicator);
        
        if (playerView == null) {
            Log.w(TAG, "PlayerView not found for position: " + position);
            return null;
        }
        
        // 创建ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        
        // 设置媒体项
        VideoData data = mVideoDataList.get(position);
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(data.videoUri));
        player.setMediaItem(mediaItem);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        
        // 监听播放状态
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (loadingIndicator != null) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        loadingIndicator.setVisibility(View.VISIBLE);
                    } else {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                }
            }
        });
        
        player.prepare();
        
        // 保存到map
        mPlayerMap.put(position, player);
        Log.d(TAG, "Created player for position: " + position + ", total players: " + mPlayerMap.size());
        
        return player;
    }

    /**
     * 释放远离当前页面的播放器
     */
    @OptIn(markerClass = UnstableApi.class)
    private void releaseDistantPlayers(int currentPosition) {
        // 释放距离当前页面超过1的播放器
        List<Integer> keysToRemove = new ArrayList<>();
        
        for (int i = 0; i < mPlayerMap.size(); i++) {
            int key = mPlayerMap.keyAt(i);
            if (Math.abs(key - currentPosition) > 1) {
                keysToRemove.add(key);
            }
        }
        
        for (int key : keysToRemove) {
            ExoPlayer player = mPlayerMap.get(key);
            if (player != null) {
                // 清除PlayerView的关联
                View pageView = mPageViewMap.get(key);
                if (pageView != null) {
                    PlayerView playerView = pageView.findViewById(R.id.player_view);
                    if (playerView != null) {
                        playerView.setPlayer(null);
                    }
                }
                
                player.stop();
                player.release();
                mPlayerMap.remove(key);
                Log.d(TAG, "Released player for position: " + key + ", remaining players: " + mPlayerMap.size());
            }
        }
    }

    /**
     * 格式化数字显示
     */
    private String formatCount(int count) {
        if (count >= 10000) {
            return String.format("%.1fw", count / 10000.0);
        }
        return String.valueOf(count);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停当前播放的视频
        if (mCurrentPlayingIndex >= 0) {
            ExoPlayer player = mPlayerMap.get(mCurrentPlayingIndex);
            if (player != null) {
                player.pause();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 恢复当前视频播放
        if (mCurrentPlayingIndex >= 0) {
            ExoPlayer player = mPlayerMap.get(mCurrentPlayingIndex);
            if (player != null) {
                player.play();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放所有播放器
        for (int i = 0; i < mPlayerMap.size(); i++) {
            ExoPlayer player = mPlayerMap.valueAt(i);
            if (player != null) {
                player.stop();
                player.release();
            }
        }
        mPlayerMap.clear();
        mPageViewMap.clear();
        Log.d(TAG, "All players released");
    }

    /**
     * 视频数据类
     */
    private static class VideoData {
        String videoUri;
        String author;
        String description;
        int likeCount;
        int commentCount;
        int favoriteCount;
        int shareCount;

        VideoData(String videoUri, String author, String description,
                  int likeCount, int commentCount, int favoriteCount, int shareCount) {
            this.videoUri = videoUri;
            this.author = author;
            this.description = description;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
            this.favoriteCount = favoriteCount;
            this.shareCount = shareCount;
        }
    }
}
