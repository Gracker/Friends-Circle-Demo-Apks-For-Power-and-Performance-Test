package com.example.wechatfriendfordouyin;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.wechatfriendfordouyin.view.VerticalVideoScroller;
import com.example.wechatfriendfordouyin.model.AppInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 抖音风格视频播放主界面
 * 使用 Android 原生 MediaPlayer + TextureView
 * 横屏视频会以宽度为基准，上下显示模糊背景
 */
public class DouyinMainActivity extends AppCompatActivity {
    private static final String TAG = "DouyinMainActivity";

    private VerticalVideoScroller mVideoScroller;
    private final List<VideoData> mVideoDataList = new ArrayList<>();

    // 前景播放器
    private final SparseArray<MediaPlayer> mPlayerMap = new SparseArray<>();
    // 背景播放器（用于模糊效果）
    private final SparseArray<MediaPlayer> mBgPlayerMap = new SparseArray<>();
    // 页面View引用
    private final SparseArray<View> mPageViewMap = new SparseArray<>();
    // TextureView 就绪状态
    private final SparseArray<Boolean> mTextureReadyMap = new SparseArray<>();

    private int mCurrentPlayingIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupFullScreen();
        setContentView(R.layout.activity_douyin_main);
        setupAppInfo();
        initVideoData();
        initViews();
    }

    private void setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }
    }

    private void initVideoData() {
        String[] authors = { "@LS.", "@奶盖", "@骑行车队", "@户外运动" };
        String[] descriptions = {
                "2025格兰芬多·鏖战腾冲 赛事第一视角🏆 #比赛现场 #公路...",
                "暖阳#阳光是最好的滤镜 #晒太阳 #好天气",
                "买了一台汽车送给维塔利每当他在路上看到这台汽车他都会说太美了",
                "骑行在路上，风景在心中 #骑行日记 #户外运动"
        };

        mVideoDataList.add(new VideoData(R.raw.video1, authors[0], descriptions[0], 7361, 363, 189, 705));
        mVideoDataList.add(new VideoData(R.raw.video2, authors[1], descriptions[1], 3521, 287, 156, 312));
        mVideoDataList.add(new VideoData(R.raw.video3, authors[2], descriptions[2], 4892, 421, 203, 389));
        mVideoDataList.add(new VideoData(R.raw.video4, authors[3], descriptions[3], 2156, 198, 87, 245));
    }

    private void initViews() {
        mVideoScroller = findViewById(R.id.video_scroller);

        mVideoScroller.setAdapter(new VerticalVideoScroller.VideoPageAdapter() {
            @Override
            public int getItemCount() {
                return mVideoDataList.size();
            }

            @Override
            public View createView(ViewGroup parent, int position) {
                View view = LayoutInflater.from(DouyinMainActivity.this)
                        .inflate(R.layout.item_video_page, parent, false);
                mPageViewMap.put(position, view);

                TextureView textureView = view.findViewById(R.id.texture_view);
                TextureView textureViewBg = view.findViewById(R.id.texture_view_bg);
                final int pos = position;

                // 前景 TextureView 监听
                if (textureView != null) {
                    textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                        @Override
                        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                            mTextureReadyMap.put(pos, true);
                            if (pos == mCurrentPlayingIndex) {
                                prepareAndPlay(pos);
                            }
                        }

                        @Override
                        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                        }

                        @Override
                        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                            mTextureReadyMap.put(pos, false);
                            return true;
                        }

                        @Override
                        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                        }
                    });
                }

                // 背景 TextureView 不需要特殊监听，跟随前景
                return view;
            }

            @Override
            public void bindView(View view, int position) {
                VideoData data = mVideoDataList.get(position);
                ((TextView) view.findViewById(R.id.tv_author)).setText(data.author);
                ((TextView) view.findViewById(R.id.tv_description)).setText(data.description);
                ((TextView) view.findViewById(R.id.tv_like_count)).setText(formatCount(data.likeCount));
                ((TextView) view.findViewById(R.id.tv_comment_count)).setText(formatCount(data.commentCount));
                ((TextView) view.findViewById(R.id.tv_favorite_count)).setText(formatCount(data.favoriteCount));
                ((TextView) view.findViewById(R.id.tv_share_count)).setText(formatCount(data.shareCount));
            }
        });

        mVideoScroller.setOnPageChangeListener(position -> {
            Log.d(TAG, "Page selected: " + position);
            onPageChanged(position);
        });

        mVideoScroller.setOnScrollListener(scrollY -> {
            int pageHeight = mVideoScroller.getHeight();
            if (pageHeight > 0) {
                int predictedPage = (scrollY + pageHeight / 2) / pageHeight;
                predictedPage = Math.max(0, Math.min(predictedPage, mVideoDataList.size() - 1));
                if (predictedPage != mCurrentPlayingIndex) {
                    preparePlayer(predictedPage);
                }
            }
        });

        mCurrentPlayingIndex = 0;
        mVideoScroller.post(() -> onPageChanged(0));
    }

    private void onPageChanged(int position) {
        // 暂停之前的播放器
        if (mCurrentPlayingIndex >= 0 && mCurrentPlayingIndex != position) {
            pausePlayer(mCurrentPlayingIndex);
        }
        mCurrentPlayingIndex = position;
        prepareAndPlay(position);

        // 预加载相邻页面
        if (position > 0)
            preparePlayer(position - 1);
        if (position < mVideoDataList.size() - 1)
            preparePlayer(position + 1);
    }

    private void pausePlayer(int position) {
        MediaPlayer player = mPlayerMap.get(position);
        MediaPlayer bgPlayer = mBgPlayerMap.get(position);
        try {
            if (player != null && player.isPlaying())
                player.pause();
            if (bgPlayer != null && bgPlayer.isPlaying())
                bgPlayer.pause();
        } catch (Exception ignored) {
        }
    }

    private void prepareAndPlay(int position) {
        if (position < 0 || position >= mVideoDataList.size())
            return;

        View pageView = mPageViewMap.get(position);
        if (pageView == null)
            return;

        TextureView textureView = pageView.findViewById(R.id.texture_view);
        TextureView textureViewBg = pageView.findViewById(R.id.texture_view_bg);
        ProgressBar loading = pageView.findViewById(R.id.loading_indicator);

        if (textureView == null || !textureView.isAvailable()) {
            if (loading != null)
                loading.setVisibility(View.VISIBLE);
            return;
        }

        // 创建或获取播放器
        MediaPlayer player = mPlayerMap.get(position);
        if (player == null) {
            player = createPlayer(position, textureView, textureViewBg, loading);
        }

        // 启动播放
        if (player != null) {
            try {
                if (!player.isPlaying())
                    player.start();
                MediaPlayer bgPlayer = mBgPlayerMap.get(position);
                if (bgPlayer != null && !bgPlayer.isPlaying())
                    bgPlayer.start();
                if (loading != null)
                    loading.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Error starting player: " + e.getMessage());
            }
        }
    }

    private void preparePlayer(int position) {
        if (position < 0 || position >= mVideoDataList.size())
            return;
        if (mPlayerMap.get(position) != null)
            return;

        View pageView = mPageViewMap.get(position);
        if (pageView == null)
            return;

        TextureView textureView = pageView.findViewById(R.id.texture_view);
        TextureView textureViewBg = pageView.findViewById(R.id.texture_view_bg);
        ProgressBar loading = pageView.findViewById(R.id.loading_indicator);

        if (textureView == null || !textureView.isAvailable())
            return;

        createPlayer(position, textureView, textureViewBg, loading);
    }

    private MediaPlayer createPlayer(int position, TextureView fgView, TextureView bgView, ProgressBar loading) {
        VideoData data = mVideoDataList.get(position);
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + data.videoResId);

        try {
            // 前景播放器
            MediaPlayer fgPlayer = new MediaPlayer();
            fgPlayer.setSurface(new Surface(fgView.getSurfaceTexture()));
            fgPlayer.setDataSource(this, uri);
            fgPlayer.setLooping(true);
            fgPlayer.setVolume(1f, 1f);

            // 背景播放器（静音）
            MediaPlayer bgPlayer = null;
            if (bgView != null && bgView.isAvailable()) {
                bgPlayer = new MediaPlayer();
                bgPlayer.setSurface(new Surface(bgView.getSurfaceTexture()));
                bgPlayer.setDataSource(this, uri);
                bgPlayer.setLooping(true);
                bgPlayer.setVolume(0f, 0f);
            }

            final MediaPlayer bg = bgPlayer;
            final int viewWidth = fgView.getWidth();
            final int viewHeight = fgView.getHeight();

            fgPlayer.setOnPreparedListener(mp -> {
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                Log.d(TAG, "Video " + position + " size: " + videoWidth + "x" + videoHeight);

                // 计算前景缩放矩阵（宽度填满，保持比例）
                adjustTextureViewTransform(fgView, videoWidth, videoHeight, false);

                // 计算背景缩放矩阵（放大填满整个屏幕，用于模糊效果）
                if (bgView != null) {
                    adjustTextureViewTransform(bgView, videoWidth, videoHeight, true);
                }

                if (loading != null)
                    runOnUiThread(() -> loading.setVisibility(View.GONE));
                if (position == mCurrentPlayingIndex) {
                    mp.start();
                    if (bg != null) {
                        try {
                            bg.start();
                        } catch (Exception ignored) {
                        }
                    }
                }
            });

            fgPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Player " + position + " error: " + what + "/" + extra);
                return true;
            });

            if (loading != null)
                runOnUiThread(() -> loading.setVisibility(View.VISIBLE));

            fgPlayer.prepareAsync();
            if (bgPlayer != null)
                bgPlayer.prepareAsync();

            mPlayerMap.put(position, fgPlayer);
            if (bgPlayer != null)
                mBgPlayerMap.put(position, bgPlayer);

            return fgPlayer;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create player: " + e.getMessage());
            return null;
        }
    }

    /**
     * 调整 TextureView 的变换矩阵以正确缩放视频
     * 
     * @param isBackground true=放大填满屏幕(center-crop), false=宽度填满保持比例(fit-width)
     */
    private void adjustTextureViewTransform(TextureView textureView, int videoWidth, int videoHeight,
            boolean isBackground) {
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();

        if (viewWidth == 0 || viewHeight == 0 || videoWidth == 0 || videoHeight == 0)
            return;

        float scaleX, scaleY;
        float videoAspect = (float) videoWidth / videoHeight;
        float viewAspect = (float) viewWidth / viewHeight;

        if (isBackground) {
            // 背景：center-crop 模式，放大填满整个屏幕
            if (videoAspect > viewAspect) {
                scaleY = 1f;
                scaleX = (viewHeight * videoAspect) / viewWidth;
            } else {
                scaleX = 1f;
                scaleY = (viewWidth / videoAspect) / viewHeight;
            }
        } else {
            // 前景：fit-width 模式，宽度填满，保持比例
            scaleX = 1f;
            scaleY = (viewWidth / videoAspect) / viewHeight;
        }

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f);
        textureView.setTransform(matrix);
    }

    private String formatCount(int count) {
        return count >= 10000 ? String.format("%.1fw", count / 10000.0) : String.valueOf(count);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pausePlayer(mCurrentPlayingIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCurrentPlayingIndex >= 0) {
            MediaPlayer player = mPlayerMap.get(mCurrentPlayingIndex);
            MediaPlayer bgPlayer = mBgPlayerMap.get(mCurrentPlayingIndex);
            try {
                if (player != null)
                    player.start();
                if (bgPlayer != null)
                    bgPlayer.start();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < mPlayerMap.size(); i++) {
            MediaPlayer p = mPlayerMap.valueAt(i);
            if (p != null)
                p.release();
        }
        for (int i = 0; i < mBgPlayerMap.size(); i++) {
            MediaPlayer p = mBgPlayerMap.valueAt(i);
            if (p != null)
                p.release();
        }
        mPlayerMap.clear();
        mBgPlayerMap.clear();
        mPageViewMap.clear();
        mTextureReadyMap.clear();
    }

    private static class VideoData {
        int videoResId;
        String author, description;
        int likeCount, commentCount, favoriteCount, shareCount;

        VideoData(int resId, String author, String desc, int like, int comment, int fav, int share) {
            this.videoResId = resId;
            this.author = author;
            this.description = desc;
            this.likeCount = like;
            this.commentCount = comment;
            this.favoriteCount = fav;
            this.shareCount = share;
        }
    }

    private void setupAppInfo() {
        AppInfo appInfo = new AppInfo("抖音风格视频滑动", "使用MediaPlayer播放本地视频", getPackageName());
        TextView nameView = findViewById(R.id.tv_app_name);
        TextView featureView = findViewById(R.id.tv_app_feature);
        TextView pkgView = findViewById(R.id.tv_package_name);
        if (nameView != null)
            nameView.setText(appInfo.getAppName());
        if (featureView != null)
            featureView.setText(appInfo.getAppFeature());
        if (pkgView != null)
            pkgView.setText(appInfo.getPackageName());
    }
}
