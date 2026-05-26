package com.example.wechatfriendforvideo.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

import com.example.wechatfriendforvideo.VideoConstants;
import com.example.wechatfriendforvideo.R;
import com.example.scrolling.common.beans.CommentBean;
import com.example.scrolling.common.beans.FriendCircleBean;
import com.example.wechatfriendforvideo.beans.VideoFriendCircleBean;
import com.example.scrolling.common.beans.OtherInfoBean;
import com.example.scrolling.common.beans.UserBean;
import com.example.scrolling.common.interfaces.OnItemClickPopupMenuListener;
import com.example.scrolling.common.interfaces.OnPraiseOrCommentClickListener;
import com.example.scrolling.common.utils.SpanUtils;
import com.example.scrolling.common.widgets.NineGridView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
// 移除StfalconImageViewer相关imports

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.loadconfig.ScrollLoadGate;

/**
 * 视频朋友圈适配器 - 基于原PerformanceFriendCircleAdapter
 * 添加了视频类型item的支持
 * 使用统一的 LoadConfig 和 LoadType 进行负载配置
 */
@OptIn(markerClass = UnstableApi.class)
public class VideoFriendCircleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements OnItemClickPopupMenuListener {

    private static final String TAG = "VideoFriendCircleAdapter";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_VIDEO = 2;  // 新增：视频类型

    private static Map<String, Integer> sResourceMap;

    private static int getDrawableId(Context context, String name) {
        if (sResourceMap == null) {
            sResourceMap = new HashMap<>();
            String[] extraNames = {"main_bg", "main_avatar"};
            for (String resName : extraNames) {
                int id = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
                if (id != 0) sResourceMap.put(resName, id);
            }
            for (int i = 1; i <= 11; i++) {
                String resName = "local" + i;
                int id = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
                if (id != 0) sResourceMap.put(resName, id);
            }
            for (int i = 1; i <= 20; i++) {
                String resName = "avatar" + i;
                int id = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
                if (id != 0) sResourceMap.put(resName, id);
            }
        }
        Integer id = sResourceMap.get(name);
        return id != null ? id : 0;
    }

    private Context mContext;
    private List<VideoFriendCircleBean> mFriendCircleBeans;
    private RequestOptions mRequestOptions;
    private int mAvatarSize;
    private DrawableTransitionOptions mDrawableTransitionOptions;
    private OnPraiseOrCommentClickListener mOnPraiseOrCommentClickListener;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    // 移除ImageLoader成员变量
    private View mHeaderView;
    private Random mRandom = new Random(LoadConfig.DATA_GENERATION_SEED); // Using fixed seed to ensure consistent results for each run
    private int mLoadType; // Load type

    // 统一负载模拟器
    private LoadSimulator mLoadSimulator;

    // String to identify current load type
    private String mLoadTypeString;

    // Variables for continuous frame load simulation
    private boolean mIsScrolling = false;
    private Runnable mFrameLoadRunnable;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private LayoutInflater mLayoutInflater;

    // 新增：视频播放器管理
    private Map<Integer, ExoPlayer> mPlayerMap = new HashMap<>();
    private int mCurrentPlayingPosition = -1;

    public VideoFriendCircleAdapter(Context context, RecyclerView recyclerView, int loadType) {
        this.mContext = context;
        this.mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        this.mRecyclerView = recyclerView;
        this.mLayoutInflater = LayoutInflater.from(context);

        // 使用RoundedCorners替代CircleCrop，增大圆角半径为30dp
        this.mRequestOptions = new RequestOptions().transform(new RoundedCorners(30));

        this.mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
        this.mLoadType = loadType;

        // 使用统一的 LoadType.toLabel() 获取负载类型字符串
        mLoadTypeString = "视频-" + LoadType.toLabel(loadType);

        // 初始化负载模拟器
        mLoadSimulator = new LoadSimulator();

        // 初始化连续加载模拟
        mFrameLoadRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsScrolling && ScrollLoadGate.shouldRunForRecyclerView(mRecyclerView)) {
                    mLoadSimulator.executeInFrameLoad(mLoadType, "VideoAdapter_continuousLoad");
                    mHandler.postDelayed(this, 16); // 约60fps
                }
            }
        };

        // 新增：设置视频滚动监听器
        setupVideoScrollListener();
    }

    // 新增：视频滚动监听器
    private void setupVideoScrollListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // Playback state follows any list movement, while synthetic load below
                // is additionally gated by ScrollLoadGate and only runs during inertia.
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                    checkAndPlayVisibleVideo();
                } else {
                    mIsScrolling = true;
                    pauseInvisibleVideos();
                }
            }
        });
    }

    // 新增：检查并播放可见的视频
    private void checkAndPlayVisibleVideo() {
        if (mLayoutManager == null || mFriendCircleBeans == null) {
            return;
        }

        int firstVisible = mLayoutManager.findFirstVisibleItemPosition();
        int lastVisible = mLayoutManager.findLastVisibleItemPosition();

        int videoToPlay = -1;
        float maxVisibleRatio = 0;

        for (int i = firstVisible; i <= lastVisible; i++) {
            int dataPosition = getDataPosition(i);
            if (dataPosition < 0 || dataPosition >= mFriendCircleBeans.size()) {
                continue;
            }

            VideoFriendCircleBean bean = mFriendCircleBeans.get(dataPosition);
            if (bean.isVideoType()) {
                View itemView = mLayoutManager.findViewByPosition(i);
                if (itemView != null) {
                    float visibleRatio = getVisibleRatio(itemView);
                    if (visibleRatio > maxVisibleRatio && visibleRatio > 0.3f) {
                        maxVisibleRatio = visibleRatio;
                        videoToPlay = i;
                    }
                }
            }
        }

        if (videoToPlay != -1 && videoToPlay != mCurrentPlayingPosition) {
            if (mCurrentPlayingPosition != -1) {
                pauseVideoAt(mCurrentPlayingPosition);
            }
            mCurrentPlayingPosition = videoToPlay;
            playVideoAt(videoToPlay);
        } else if (videoToPlay == mCurrentPlayingPosition && mCurrentPlayingPosition != -1) {
            ExoPlayer player = mPlayerMap.get(mCurrentPlayingPosition);
            if (player != null && !player.isPlaying() && player.getPlaybackState() == Player.STATE_READY) {
                player.play();
                updatePlayIcon(mCurrentPlayingPosition, false);
            }
        }
    }

    private float getVisibleRatio(View view) {
        Rect rect = new Rect();
        view.getLocalVisibleRect(rect);
        int visibleHeight = rect.height();
        int totalHeight = view.getHeight();
        return totalHeight > 0 ? (float) visibleHeight / totalHeight : 0;
    }

    private void pauseInvisibleVideos() {
        if (mLayoutManager == null) return;

        int firstVisible = mLayoutManager.findFirstVisibleItemPosition();
        int lastVisible = mLayoutManager.findLastVisibleItemPosition();

        for (Map.Entry<Integer, ExoPlayer> entry : mPlayerMap.entrySet()) {
            int position = entry.getKey();
            ExoPlayer player = entry.getValue();

            if (position < firstVisible || position > lastVisible) {
                if (player != null && player.isPlaying()) {
                    player.pause();
                    updatePlayIcon(position, true);
                }
            } else {
                View itemView = mLayoutManager.findViewByPosition(position);
                if (itemView != null) {
                    float visibleRatio = getVisibleRatio(itemView);
                    if (visibleRatio < 0.3f && player != null && player.isPlaying()) {
                        player.pause();
                        updatePlayIcon(position, true);
                    }
                }
            }
        }
    }

    private void playVideoAt(int adapterPosition) {
        View itemView = mLayoutManager.findViewByPosition(adapterPosition);
        if (itemView == null) return;

        PlayerView playerView = itemView.findViewById(R.id.player_view);
        ImageView playIcon = itemView.findViewById(R.id.img_play_icon);
        if (playerView == null) return;

        ExoPlayer player = mPlayerMap.get(adapterPosition);
        if (player != null && player.getPlaybackState() == Player.STATE_READY) {
            player.play();
            if (playIcon != null) {
                playIcon.setVisibility(View.GONE);
            }
        }
    }

    private void pauseVideoAt(int adapterPosition) {
        ExoPlayer player = mPlayerMap.get(adapterPosition);
        if (player != null && player.isPlaying()) {
            player.pause();
            updatePlayIcon(adapterPosition, true);
        }
    }

    private void updatePlayIcon(int adapterPosition, boolean showPlayIcon) {
        View itemView = mLayoutManager.findViewByPosition(adapterPosition);
        if (itemView == null) return;

        ImageView playIcon = itemView.findViewById(R.id.img_play_icon);
        if (playIcon != null) {
            playIcon.setVisibility(showPlayIcon ? View.VISIBLE : View.GONE);
        }
    }

    private int getDataPosition(int adapterPosition) {
        return mHeaderView != null ? adapterPosition - 1 : adapterPosition;
    }

    // 移除带ImageLoader参数的构造函数

    public void setHeaderView(View headerView) {
        mHeaderView = headerView;
        setupHeaderView();
        notifyItemInserted(0);
    }

    private void setupHeaderView() {
        if (mHeaderView == null) {
            return;
        }

        // 设置背景图片
        ImageView imgCover = mHeaderView.findViewById(R.id.img_cover);
        if (imgCover != null) {
            try {
                // 优先尝试加载main_bg
                int coverResourceId = getDrawableId(mContext, "main_bg");

                if (coverResourceId != 0) {
                    Glide.with(mContext)
                        .load(coverResourceId)
                        .transition(mDrawableTransitionOptions)
                        .into(imgCover);
                } else {
                    // 依次尝试加载local系列图片作为背景
                    boolean bgLoaded = false;
                    for (int i = 1; i <= 11 && !bgLoaded; i++) {
                        String localName = "local" + i;
                        int localId = getDrawableId(mContext, localName);

                        if (localId != 0) {
                            Glide.with(mContext)
                                .load(localId)
                                .transition(mDrawableTransitionOptions)
                                .into(imgCover);
                            bgLoaded = true;
                            break;
                        }
                    }

                    // 如果仍然没有加载到图片，使用默认背景
                    if (!bgLoaded) {
                        imgCover.setImageResource(R.drawable.default_background);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                imgCover.setImageResource(R.drawable.default_background);
            }
        }

        // 设置头像 - 使用圆角头像
        ImageView imgUserAvatar = mHeaderView.findViewById(R.id.img_user_avatar);
        if (imgUserAvatar != null) {
            try {
                // 先尝试加载固定的main_avatar头像
                int avatarResourceId = getDrawableId(mContext, "main_avatar");

                if (avatarResourceId != 0) {
                    Glide.with(mContext)
                        .load(avatarResourceId)
                        .apply(mRequestOptions)
                        .transition(mDrawableTransitionOptions)
                        .into(imgUserAvatar);
                } else {
                    // 依次尝试加载avatar1到avatar11中的一个头像
                    boolean avatarLoaded = false;
                    for (int i = 1; i <= 11 && !avatarLoaded; i++) {
                        String avatarName = "avatar" + i;
                        int avatarId = getDrawableId(mContext, avatarName);

                        if (avatarId != 0) {
                            Glide.with(mContext)
                                .load(avatarId)
                                .apply(mRequestOptions)
                                .transition(mDrawableTransitionOptions)
                                .into(imgUserAvatar);
                            avatarLoaded = true;
                        }
                    }

                    // 如果上面都加载失败，则使用默认头像
                    if (!avatarLoaded) {
                        Glide.with(mContext)
                            .load(R.drawable.default_avatar)
                            .apply(mRequestOptions)
                            .transition(mDrawableTransitionOptions)
                            .into(imgUserAvatar);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Glide.with(mContext)
                    .load(R.drawable.default_avatar)
                    .apply(mRequestOptions)
                    .transition(mDrawableTransitionOptions)
                    .into(imgUserAvatar);
            }
        }

        // 设置用户名 - 显示当前负载类型
        TextView tvUserName = mHeaderView.findViewById(R.id.tv_user_name);
        if (tvUserName != null) {
            tvUserName.setText(mLoadTypeString);
        }

        // 设置返回按钮点击事件
        ImageView imgBack = mHeaderView.findViewById(R.id.img_back);
        if (imgBack != null) {
            imgBack.setOnClickListener(v -> {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).onBackPressed();
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mHeaderView != null) {
            return TYPE_HEADER;
        }

        // 判断是否为视频类型
        int dataPosition = getDataPosition(position);
        if (dataPosition >= 0 && mFriendCircleBeans != null && dataPosition < mFriendCircleBeans.size()) {
            VideoFriendCircleBean bean = mFriendCircleBeans.get(dataPosition);
            if (bean.isVideoType()) {
                return TYPE_VIDEO;
            }
        }

        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER && mHeaderView != null) {
            return new HeaderViewHolder(mHeaderView);
        }

        // 新增：视频类型
        if (viewType == TYPE_VIDEO) {
            View itemView = mLayoutInflater.inflate(R.layout.item_friend_circle_video, parent, false);
            return new VideoViewHolder(itemView);
        }

        View itemView = mLayoutInflater.inflate(R.layout.item_friend_circle_image, parent, false);
        return new FriendCircleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            return;
        }

        // 实际数据位置需要减去header
        int dataPosition = getDataPosition(position);
        if (dataPosition < 0 || dataPosition >= mFriendCircleBeans.size()) {
            return;
        }

        VideoFriendCircleBean friendCircleBean = mFriendCircleBeans.get(dataPosition);
        if (friendCircleBean == null) {
            return;
        }

        // 新增：处理视频类型
        if (holder instanceof VideoViewHolder) {
            bindVideoViewHolder((VideoViewHolder) holder, friendCircleBean, position);
            if (ScrollLoadGate.shouldRunForRecyclerView(mRecyclerView)) {
                mLoadSimulator.executeInFrameLoad(mLoadType, "VideoAdapter_bindLoad");
            }
            return;
        }

        FriendCircleViewHolder viewHolder = (FriendCircleViewHolder) holder;

        // 设置用户信息
        if (friendCircleBean.getUserBean() != null) {
            UserBean userBean = friendCircleBean.getUserBean();

            // 设置用户名
            viewHolder.txtUserName.setText(userBean.getUserName());

            // 设置用户头像
            try {
                String avatarUrl = userBean.getUserAvatarUrl();
                // 首先尝试直接加载原始URL
                int avatarResourceId = getDrawableId(mContext, avatarUrl);

                if (avatarResourceId != 0) {
                    Glide.with(mContext)
                            .load(avatarResourceId)
                            .apply(mRequestOptions)
                            .transition(mDrawableTransitionOptions)
                            .into(viewHolder.imgAvatar);
                } else {
                    // 尝试加载avatar系列头像
                    int avatarIndex = dataPosition % 11 + 1; // 使用1-11范围
                    String avatarResource = "avatar" + avatarIndex;

                    int avatarSeriesId = getDrawableId(mContext, avatarResource);

                    if (avatarSeriesId != 0) {
                        Glide.with(mContext)
                                .load(avatarSeriesId)
                                .apply(mRequestOptions)
                                .transition(mDrawableTransitionOptions)
                                .into(viewHolder.imgAvatar);
                    } else {
                        // 如果都失败，使用默认头像
                        Glide.with(mContext)
                                .load(R.drawable.default_avatar)
                                .apply(mRequestOptions)
                                .transition(mDrawableTransitionOptions)
                                .into(viewHolder.imgAvatar);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Glide.with(mContext)
                        .load(R.drawable.default_avatar)
                        .apply(mRequestOptions)
                        .transition(mDrawableTransitionOptions)
                        .into(viewHolder.imgAvatar);
            }
        }

        // 设置内容
        if (!TextUtils.isEmpty(friendCircleBean.getContent())) {
            viewHolder.txtContent.setText(friendCircleBean.getContent());
            viewHolder.txtContent.setVisibility(View.VISIBLE);
        } else {
            viewHolder.txtContent.setVisibility(View.GONE);
        }

        // 设置图片
        if (friendCircleBean.getImageUrls() != null && !friendCircleBean.getImageUrls().isEmpty()) {
            viewHolder.nineGridView.setVisibility(View.VISIBLE);
            viewHolder.nineGridView.setAdapter(new NineImageAdapter(mContext, friendCircleBean.getImageUrls()));
        } else {
            viewHolder.nineGridView.setVisibility(View.GONE);
        }

        // 设置点赞和评论区域的可见性
        boolean hasPraise = friendCircleBean.getPraiseBeans() != null && !friendCircleBean.getPraiseBeans().isEmpty();
        boolean hasComment = friendCircleBean.getCommentBeans() != null && !friendCircleBean.getCommentBeans().isEmpty();

        if (hasPraise || hasComment) {
            viewHolder.layoutPraiseComment.setVisibility(View.VISIBLE);

            // 设置点赞信息
            if (hasPraise) {
                // 如果点赞文本为空，重新生成
                if (friendCircleBean.getPraiseSpan() == null) {
                    SpannableStringBuilder praiseSpan = SpanUtils.makePraiseSpan(
                            mContext, friendCircleBean.getPraiseBeans());
                    friendCircleBean.setPraiseSpan(praiseSpan);
                }

                viewHolder.txtPraise.setText(friendCircleBean.getPraiseSpan());
                viewHolder.layoutPraise.setVisibility(View.VISIBLE);
            } else {
                viewHolder.layoutPraise.setVisibility(View.GONE);
            }

            // 设置评论信息
            if (hasComment) {
                viewHolder.recyclerViewComment.removeAllViews();

                for (CommentBean commentBean : friendCircleBean.getCommentBeans()) {
                    // 如果评论文本为空，重新生成
                    if (commentBean.getCommentContentSpan() == null) {
                        commentBean.build();
                    }

                    TextView textView = new TextView(mContext);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setTextColor(mContext.getResources().getColor(R.color.base_333333));
                    textView.setTextSize(14);
                    textView.setText(commentBean.getCommentContentSpan());
                    textView.setPadding(16, 8, 16, 8);

                    // 直接添加到ViewGroup中
                    viewHolder.recyclerViewComment.addView(textView);
                }

                viewHolder.recyclerViewComment.setVisibility(View.VISIBLE);
            } else {
                viewHolder.recyclerViewComment.setVisibility(View.GONE);
            }

            // 设置分隔线
            viewHolder.viewLine.setVisibility(hasPraise && hasComment ? View.VISIBLE : View.GONE);
        } else {
            viewHolder.layoutPraiseComment.setVisibility(View.GONE);
        }

        // 设置其他信息
        if (friendCircleBean.getOtherInfoBean() != null) {
            OtherInfoBean otherInfoBean = friendCircleBean.getOtherInfoBean();

            // 设置发布时间
            if (!TextUtils.isEmpty(otherInfoBean.getTime())) {
                viewHolder.txtTime.setText(otherInfoBean.getTime());
                viewHolder.txtTime.setVisibility(View.VISIBLE);
            } else {
                viewHolder.txtTime.setVisibility(View.GONE);
            }

            // 设置发布来源
            if (!TextUtils.isEmpty(otherInfoBean.getSource())) {
                viewHolder.txtSource.setText(otherInfoBean.getSource());
                viewHolder.txtSource.setVisibility(View.VISIBLE);
            } else {
                viewHolder.txtSource.setVisibility(View.GONE);
            }

            // 设置位置信息
            if (!TextUtils.isEmpty(otherInfoBean.getLocation())) {
                viewHolder.txtLocation.setText(otherInfoBean.getLocation());
                viewHolder.txtLocation.setVisibility(View.VISIBLE);
            } else {
                viewHolder.txtLocation.setVisibility(View.GONE);
            }
        } else {
            viewHolder.txtTime.setVisibility(View.GONE);
            viewHolder.txtSource.setVisibility(View.GONE);
            viewHolder.txtLocation.setVisibility(View.GONE);
        }

        // 设置操作按钮点击事件
        viewHolder.imgComment.setOnClickListener(v -> {
            if (mOnPraiseOrCommentClickListener != null) {
                mOnPraiseOrCommentClickListener.onCommentClick(v, dataPosition);
            }
        });

        // 模拟计算负载
        if (ScrollLoadGate.shouldRunForRecyclerView(mRecyclerView)) {
            mLoadSimulator.executeInFrameLoad(mLoadType, "VideoAdapter_bindLoad");
        }
    }

    /**
     * 停止持续帧负载模拟，释放资源
     */
    public void stopContinuousLoadSimulation() {
        if (mHandler != null && mFrameLoadRunnable != null) {
            mHandler.removeCallbacks(mFrameLoadRunnable);
            mFrameLoadRunnable = null;
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof VideoViewHolder) {
            int position = holder.getAdapterPosition();
            ExoPlayer player = mPlayerMap.get(position);
            if (player != null) {
                player.release();
                mPlayerMap.remove(position);
            }

            if (position == mCurrentPlayingPosition) {
                mCurrentPlayingPosition = -1;
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopContinuousLoadSimulation();
        releaseAllPlayers();

        // 释放负载模拟器资源
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }

    // 新增：释放所有视频播放器
    public void releaseAllPlayers() {
        for (ExoPlayer player : mPlayerMap.values()) {
            if (player != null) {
                player.release();
            }
        }
        mPlayerMap.clear();
        mCurrentPlayingPosition = -1;
    }

    // 新增：暂停所有视频
    public void pauseAllVideos() {
        for (Map.Entry<Integer, ExoPlayer> entry : mPlayerMap.entrySet()) {
            ExoPlayer player = entry.getValue();
            if (player != null && player.isPlaying()) {
                player.pause();
                updatePlayIcon(entry.getKey(), true);
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = mFriendCircleBeans == null ? 0 : mFriendCircleBeans.size();
        return mHeaderView == null ? count : count + 1;
    }

    public void setFriendCircleBeans(List<VideoFriendCircleBean> newBeans) {
        final List<VideoFriendCircleBean> oldBeans = this.mFriendCircleBeans;
        this.mFriendCircleBeans = newBeans != null ? newBeans : new ArrayList<>();

        if (oldBeans == null || oldBeans.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldBeans.size();
            }

            @Override
            public int getNewListSize() {
                return mFriendCircleBeans.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldBeans.get(oldPos) == mFriendCircleBeans.get(newPos);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return oldBeans.get(oldPos) == mFriendCircleBeans.get(newPos);
            }
        });
        result.dispatchUpdatesTo(this);
    }

    @Override
    public void onItemClickPopupMenu(int position, int itemId) {
        // No implementation needed
    }

    // ========== 新增：视频绑定方法 ==========

    private void bindVideoViewHolder(VideoViewHolder holder, VideoFriendCircleBean bean, int adapterPosition) {
        int dataPosition = getDataPosition(adapterPosition);

        // 设置用户信息
        if (bean.getUserBean() != null) {
            UserBean userBean = bean.getUserBean();
            holder.txtUserName.setText(userBean.getUserName());

            try {
                String avatarUrl = userBean.getUserAvatarUrl();
                int avatarResourceId = getDrawableId(mContext, avatarUrl);

                if (avatarResourceId != 0) {
                    Glide.with(mContext)
                            .load(avatarResourceId)
                            .apply(mRequestOptions)
                            .transition(mDrawableTransitionOptions)
                            .into(holder.imgAvatar);
                } else {
                    int avatarIndex = dataPosition % 11 + 1;
                    String avatarResource = "avatar" + avatarIndex;
                    int avatarSeriesId = getDrawableId(mContext, avatarResource);

                    if (avatarSeriesId != 0) {
                        Glide.with(mContext)
                                .load(avatarSeriesId)
                                .apply(mRequestOptions)
                                .transition(mDrawableTransitionOptions)
                                .into(holder.imgAvatar);
                    } else {
                        Glide.with(mContext)
                                .load(R.drawable.default_avatar)
                                .apply(mRequestOptions)
                                .transition(mDrawableTransitionOptions)
                                .into(holder.imgAvatar);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Glide.with(mContext)
                        .load(R.drawable.default_avatar)
                        .apply(mRequestOptions)
                        .transition(mDrawableTransitionOptions)
                        .into(holder.imgAvatar);
            }
        }

        // 设置内容
        if (!TextUtils.isEmpty(bean.getContent())) {
            holder.txtContent.setText(bean.getContent());
            holder.txtContent.setVisibility(View.VISIBLE);
        } else {
            holder.txtContent.setVisibility(View.GONE);
        }

        // 设置视频播放器
        setupVideoPlayer(holder, bean, adapterPosition);

        // 设置点赞和评论
        boolean hasPraise = bean.getPraiseBeans() != null && !bean.getPraiseBeans().isEmpty();
        boolean hasComment = bean.getCommentBeans() != null && !bean.getCommentBeans().isEmpty();

        if (hasPraise || hasComment) {
            holder.layoutPraiseComment.setVisibility(View.VISIBLE);

            if (hasPraise) {
                if (bean.getPraiseSpan() == null) {
                    SpannableStringBuilder praiseSpan = SpanUtils.makePraiseSpan(
                            mContext, bean.getPraiseBeans());
                    bean.setPraiseSpan(praiseSpan);
                }
                holder.txtPraise.setText(bean.getPraiseSpan());
                holder.layoutPraise.setVisibility(View.VISIBLE);
            } else {
                holder.layoutPraise.setVisibility(View.GONE);
            }

            if (hasComment) {
                holder.recyclerViewComment.removeAllViews();
                for (CommentBean commentBean : bean.getCommentBeans()) {
                    if (commentBean.getCommentContentSpan() == null) {
                        commentBean.build();
                    }

                    TextView textView = new TextView(mContext);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    textView.setTextColor(mContext.getResources().getColor(R.color.base_333333));
                    textView.setTextSize(14);
                    textView.setText(commentBean.getCommentContentSpan());
                    textView.setPadding(16, 8, 16, 8);
                    holder.recyclerViewComment.addView(textView);
                }
                holder.recyclerViewComment.setVisibility(View.VISIBLE);
            } else {
                holder.recyclerViewComment.setVisibility(View.GONE);
            }

            holder.viewLine.setVisibility(hasPraise && hasComment ? View.VISIBLE : View.GONE);
        } else {
            holder.layoutPraiseComment.setVisibility(View.GONE);
        }

        // 设置其他信息
        if (bean.getOtherInfoBean() != null) {
            OtherInfoBean otherInfoBean = bean.getOtherInfoBean();

            if (!TextUtils.isEmpty(otherInfoBean.getTime())) {
                holder.txtTime.setText(otherInfoBean.getTime());
                holder.txtTime.setVisibility(View.VISIBLE);
            } else {
                holder.txtTime.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(otherInfoBean.getSource())) {
                holder.txtSource.setText(otherInfoBean.getSource());
                holder.txtSource.setVisibility(View.VISIBLE);
            } else {
                holder.txtSource.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(otherInfoBean.getLocation())) {
                holder.txtLocation.setText(otherInfoBean.getLocation());
                holder.txtLocation.setVisibility(View.VISIBLE);
            } else {
                holder.txtLocation.setVisibility(View.GONE);
            }
        } else {
            holder.txtTime.setVisibility(View.GONE);
            holder.txtSource.setVisibility(View.GONE);
            holder.txtLocation.setVisibility(View.GONE);
        }
    }

    private void setupVideoPlayer(VideoViewHolder holder, VideoFriendCircleBean bean, int adapterPosition) {
        // 释放旧的播放器
        ExoPlayer oldPlayer = mPlayerMap.get(adapterPosition);
        if (oldPlayer != null) {
            oldPlayer.release();
            mPlayerMap.remove(adapterPosition);
        }

        // 创建新的播放器
        ExoPlayer player = new ExoPlayer.Builder(mContext).build();
        player.setVolume(0f); // 静音播放
        player.setRepeatMode(Player.REPEAT_MODE_ALL);

        holder.playerView.setPlayer(player);
        holder.playerView.setUseController(false);

        // 准备视频
        int videoResId = bean.getVideoResId();
        if (videoResId != 0) {
            Uri videoUri = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + videoResId);
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            player.setMediaItem(mediaItem);
            player.prepare();
        }

        // 添加播放状态监听
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    if (mCurrentPlayingPosition == adapterPosition && !mIsScrolling) {
                        player.play();
                        holder.imgPlayIcon.setVisibility(View.GONE);
                    }
                }
            }
        });

        mPlayerMap.put(adapterPosition, player);

        // 设置点击事件
        holder.videoContainer.setOnClickListener(v -> {
            ExoPlayer p = mPlayerMap.get(adapterPosition);
            if (p != null) {
                if (p.isPlaying()) {
                    p.pause();
                    holder.imgPlayIcon.setVisibility(View.VISIBLE);
                } else {
                    p.play();
                    holder.imgPlayIcon.setVisibility(View.GONE);
                }
            }
        });

        holder.imgPlayIcon.setVisibility(View.VISIBLE);
    }

    /**
     * dp转px
     */
    private int dpToPx(float dp) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * 头部ViewHolder
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * 朋友圈ViewHolder
     */
    static class FriendCircleViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtUserName;
        TextView txtContent;
        NineGridView nineGridView;
        TextView txtLocation;
        TextView txtTime;
        TextView txtSource;
        LinearLayout layoutPraiseComment;
        LinearLayout layoutPraise;
        TextView txtPraise;
        LinearLayout recyclerViewComment;
        View viewLine;
        ImageView imgComment;

        FriendCircleViewHolder(View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            txtUserName = itemView.findViewById(R.id.txt_user_name);
            txtContent = itemView.findViewById(R.id.txt_content);
            nineGridView = itemView.findViewById(R.id.nine_grid_view);
            txtLocation = itemView.findViewById(R.id.txt_location);
            txtTime = itemView.findViewById(R.id.txt_time);
            txtSource = itemView.findViewById(R.id.txt_source);
            layoutPraiseComment = itemView.findViewById(R.id.layout_praise_comment);
            layoutPraise = itemView.findViewById(R.id.layout_praise);
            txtPraise = itemView.findViewById(R.id.txt_praise);
            recyclerViewComment = itemView.findViewById(R.id.layout_comment);
            viewLine = itemView.findViewById(R.id.view_line);
            imgComment = itemView.findViewById(R.id.img_comment);
        }
    }

    /**
     * 视频ViewHolder - 新增
     */
    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtUserName;
        TextView txtContent;
        FrameLayout videoContainer;
        PlayerView playerView;
        ImageView imgPlayIcon;
        TextView txtLocation;
        TextView txtTime;
        TextView txtSource;
        LinearLayout layoutPraiseComment;
        LinearLayout layoutPraise;
        TextView txtPraise;
        LinearLayout recyclerViewComment;
        View viewLine;
        ImageView imgComment;

        VideoViewHolder(View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            txtUserName = itemView.findViewById(R.id.txt_user_name);
            txtContent = itemView.findViewById(R.id.txt_content);
            videoContainer = itemView.findViewById(R.id.video_container);
            playerView = itemView.findViewById(R.id.player_view);
            imgPlayIcon = itemView.findViewById(R.id.img_play_icon);
            txtLocation = itemView.findViewById(R.id.txt_location);
            txtTime = itemView.findViewById(R.id.txt_time);
            txtSource = itemView.findViewById(R.id.txt_source);
            layoutPraiseComment = itemView.findViewById(R.id.layout_praise_comment);
            layoutPraise = itemView.findViewById(R.id.layout_praise);
            txtPraise = itemView.findViewById(R.id.txt_praise);
            recyclerViewComment = itemView.findViewById(R.id.layout_comment);
            viewLine = itemView.findViewById(R.id.view_line);
            imgComment = itemView.findViewById(R.id.img_comment);
        }
    }

    /**
     * 评论适配器
     */
    private static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

        private Context mContext;
        private List<CommentBean> mCommentBeans;

        CommentAdapter(Context context, List<CommentBean> commentBeans) {
            this.mContext = context;
            this.mCommentBeans = commentBeans != null ? commentBeans : new ArrayList<>();
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            CommentBean commentBean = mCommentBeans.get(position);
            holder.tvComment.setText(commentBean.getCommentContentSpan());
        }

        @Override
        public int getItemCount() {
            return mCommentBeans.size();
        }

        static class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView tvComment;

            CommentViewHolder(View itemView) {
                super(itemView);
                tvComment = itemView.findViewById(R.id.tv_comment);
            }
        }
    }
}
