package com.example.wechatfriendforperformance.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechatfriendforperformance.PerformanceConstants;
import com.example.wechatfriendforperformance.R;
import com.example.wechatfriendforperformance.beans.CommentBean;
import com.example.wechatfriendforperformance.beans.FriendCircleBean;
import com.example.wechatfriendforperformance.beans.OtherInfoBean;
import com.example.wechatfriendforperformance.beans.UserBean;
import com.example.wechatfriendforperformance.interfaces.OnItemClickPopupMenuListener;
import com.example.wechatfriendforperformance.interfaces.OnPraiseOrCommentClickListener;
import com.example.wechatfriendforperformance.utils.PerformanceSpanUtils;
import com.example.wechatfriendforperformance.widgets.NineGridView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 性能测试专用的朋友圈适配器，支持不同负载级别
 */
public class PerformanceFriendCircleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> 
        implements OnItemClickPopupMenuListener {

    // Load type constants
    public static final int LOAD_TYPE_LIGHT = 0;  // Light load
    public static final int LOAD_TYPE_MEDIUM = 1; // Medium load
    public static final int LOAD_TYPE_HEAVY = 2;  // Heavy load

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_NORMAL = 1;
    
    private Context mContext;
    private List<FriendCircleBean> mFriendCircleBeans;
    private RequestOptions mRequestOptions;
    private int mAvatarSize;
    private DrawableTransitionOptions mDrawableTransitionOptions;
    private OnPraiseOrCommentClickListener mOnPraiseOrCommentClickListener;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private ImageLoader<String> mImageLoader;
    private View mHeaderView;
    private Random mRandom = new Random(0); // Using fixed seed to ensure consistent results for each run
    private int mLoadType; // Load type
    
    // Variables for simulating computational load
    private ArrayList<Bitmap> mBitmapList = new ArrayList<>();
    private Paint mPaint = new Paint();
    private Canvas mCanvas;
    
    // String to identify current load type
    private String mLoadTypeString;
    
    // Variables for continuous frame load simulation
    private boolean mIsScrolling = false;
    private Runnable mFrameLoadRunnable;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private LayoutInflater mLayoutInflater;

    public PerformanceFriendCircleAdapter(Context context, RecyclerView recyclerView, int loadType) {
        this.mContext = context;
        this.mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        this.mRecyclerView = recyclerView;
        this.mLayoutInflater = LayoutInflater.from(context);
        
        // 使用RoundedCorners替代CircleCrop，增大圆角半径为30dp
        this.mRequestOptions = new RequestOptions().transform(new RoundedCorners(30));
        
        this.mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
        this.mLoadType = loadType;
        
        // 设置负载类型字符串
        switch (loadType) {
            case LOAD_TYPE_LIGHT:
                mLoadTypeString = "轻负载";
                break;
            case LOAD_TYPE_MEDIUM:
                mLoadTypeString = "中负载";
                break;
            case LOAD_TYPE_HEAVY:
                mLoadTypeString = "高负载";
                break;
            default:
                mLoadTypeString = "未知负载";
                break;
        }
        
        // 初始化图片加载器
        mImageLoader = new ImageLoader<String>() {
            @Override
            public void loadImage(ImageView imageView, String image) {
                try {
                    // 尝试加载图片资源
                    int resourceId = mContext.getResources().getIdentifier(
                            image.toLowerCase(), "drawable", mContext.getPackageName());
                    if (resourceId != 0) {
                        Glide.with(mContext)
                                .load(resourceId)
                                .transition(mDrawableTransitionOptions)
                                .into(imageView);
                    } else {
                        // 如果找不到资源，尝试加载测试图片
                        int testImgId = mContext.getResources().getIdentifier(
                                "test_img_" + (Math.abs(image.hashCode()) % 10 + 1), 
                                "drawable", mContext.getPackageName());
                        if (testImgId != 0) {
                            Glide.with(mContext)
                                    .load(testImgId)
                                    .transition(mDrawableTransitionOptions)
                                    .into(imageView);
                        } else {
                            // 如果仍然找不到，使用默认图片
                            Glide.with(mContext)
                                    .load(R.drawable.default_avatar)
                                    .into(imageView);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 使用默认图片
                    Glide.with(mContext)
                            .load(R.drawable.default_avatar)
                            .into(imageView);
                }
            }
        };
        
        // 初始化连续加载模拟
        mFrameLoadRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsScrolling) {
                    simulateComputationalLoad();
                    mHandler.postDelayed(this, 16); // 约60fps
                }
            }
        };
    }

    public PerformanceFriendCircleAdapter(Context context, RecyclerView recyclerView, 
                                     ImageLoader<String> imageLoader, int loadType) {
        this(context, recyclerView, loadType);
        this.mImageLoader = imageLoader;
    }

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
                int coverResourceId = mContext.getResources().getIdentifier(
                    "main_bg", "drawable", mContext.getPackageName());
                
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
                        int localId = mContext.getResources().getIdentifier(
                            localName, "drawable", mContext.getPackageName());
                        
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
                int avatarResourceId = mContext.getResources().getIdentifier(
                    "main_avatar", "drawable", mContext.getPackageName());
                
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
                        int avatarId = mContext.getResources().getIdentifier(
                            avatarName, "drawable", mContext.getPackageName());
                        
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
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER && mHeaderView != null) {
            return new HeaderViewHolder(mHeaderView);
        }
        
        View itemView = mLayoutInflater.inflate(R.layout.item_friend_circle, parent, false);
        return new FriendCircleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            return;
        }
        
        // 实际数据位置需要减去header
        int dataPosition = position - (mHeaderView != null ? 1 : 0);
        if (dataPosition < 0 || dataPosition >= mFriendCircleBeans.size()) {
            return;
        }
        
        FriendCircleBean friendCircleBean = mFriendCircleBeans.get(dataPosition);
        if (friendCircleBean == null) {
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
                int avatarResourceId = mContext.getResources().getIdentifier(
                        avatarUrl, "drawable", mContext.getPackageName());
                
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
                    
                    int avatarSeriesId = mContext.getResources().getIdentifier(
                            avatarResource, "drawable", mContext.getPackageName());
                    
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
                    SpannableStringBuilder praiseSpan = PerformanceSpanUtils.makePraiseSpan(
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
        simulateComputationalLoad();
    }
    
    /**
     * Execute different computation based on load type
     * @param position Data position
     */
    private void simulateComputationalLoad(int position) {
        Trace.beginSection("FriendCircleAdapter_simulateComputationalLoad");
        
        // 确保Canvas已经初始化
        if (mCanvas == null || mBitmapList.isEmpty()) {
            Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(bitmap);
            mBitmapList.add(bitmap);
        }
        
        int iterations;
        switch (mLoadType) {
            case LOAD_TYPE_LIGHT:
                iterations = 5; // Light load: only do a small amount of work per frame
                break;
            case LOAD_TYPE_MEDIUM:
                iterations = 800; // Medium load: increased for more pressure
                break;
            case LOAD_TYPE_HEAVY:
                iterations = 2000; // Heavy load: significantly increased for heavy pressure
                break;
            default:
                iterations = 5;
                break;
        }
        
        // Perform some calculations to simulate load
        for (int i = 0; i < iterations; i++) {
            // Generate some random data to prevent compiler from optimizing away
            float x = mRandom.nextFloat() * 100;
            float y = mRandom.nextFloat() * 100;
            
            // Perform some drawing operations, which will bring CPU and GPU load
            mPaint.setColor(Color.argb(
                    mRandom.nextInt(256),
                    mRandom.nextInt(256),
                    mRandom.nextInt(256),
                    mRandom.nextInt(256)
            ));
            mCanvas.drawCircle(x, y, 10, mPaint);
            
            // Add more complex calculations for medium and heavy loads
            if (mLoadType == LOAD_TYPE_MEDIUM || mLoadType == LOAD_TYPE_HEAVY) {
                double sinValue = Math.sin(x) * Math.cos(y);
                double tanValue = Math.tan(x * 0.1);
                // Prevent compiler optimization
                if (sinValue > 0.999 && tanValue > 100) {
                    mPaint.setStrokeWidth((float) (sinValue + tanValue));
                }
            }
        }
        Trace.endSection();
    }

    /**
     * 模拟计算负载 - 无参数版本，使用默认位置0
     */
    private void simulateComputationalLoad() {
        simulateComputationalLoad(0);
    }

    /**
     * Start a continuous background load to simulate pressure on each frame
     */
    private void startContinuousLoadSimulation() {
        if (mFrameLoadRunnable == null) {
            // 确保Canvas已经初始化
            if (mCanvas == null || mBitmapList.isEmpty()) {
                Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(bitmap);
                mBitmapList.add(bitmap);
            }
            
            mFrameLoadRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
                        // Only add extra load during scrolling
                        int extraLoadIterations;
                        switch (mLoadType) {
                            case LOAD_TYPE_LIGHT:
                                extraLoadIterations = 0; // No extra load for light load
                                break;
                            case LOAD_TYPE_MEDIUM:
                                extraLoadIterations = 200; // Moderate extra load for medium
                                break;
                            case LOAD_TYPE_HEAVY:
                                extraLoadIterations = 500; // Heavy extra load for heavy
                                break;
                            default:
                                extraLoadIterations = 0;
                                break;
                        }
                        
                        if (extraLoadIterations > 0) {
                            Trace.beginSection("FriendCircleAdapter_continuousLoad");
                            for (int i = 0; i < extraLoadIterations; i++) {
                                float x = mRandom.nextFloat() * 100;
                                float y = mRandom.nextFloat() * 100;
                                
                                mPaint.setColor(Color.argb(
                                        mRandom.nextInt(256),
                                        mRandom.nextInt(256),
                                        mRandom.nextInt(256),
                                        mRandom.nextInt(256)
                                ));
                                mCanvas.drawCircle(x, y, 10, mPaint);
                                
                                // 增加额外计算
                                if (mLoadType == LOAD_TYPE_HEAVY) {
                                    double sinValue = Math.sin(x) * Math.cos(y);
                                    double tanValue = Math.tan(x * 0.1);
                                    // 防止编译器优化
                                    if (sinValue > 0.999 && tanValue > 100) {
                                        mPaint.setStrokeWidth((float) (sinValue + tanValue));
                                    }
                                }
                            }
                            Trace.endSection();
                        }
                    }
                    
                    // Schedule next frame computation
                    mHandler.postDelayed(this, 8); // Roughly 60fps
                }
            };
            
            // Start the continuous load
            mHandler.post(mFrameLoadRunnable);
            
            // Set up scroll state change listener to detect scrolling
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    mIsScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
                }
            });
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
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopContinuousLoadSimulation();
        
        // 释放Bitmap资源
        for (Bitmap bitmap : mBitmapList) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        mBitmapList.clear();
        mCanvas = null;
    }

    @Override
    public int getItemCount() {
        int count = mFriendCircleBeans == null ? 0 : mFriendCircleBeans.size();
        return mHeaderView == null ? count : count + 1;
    }

    public void setFriendCircleBeans(List<FriendCircleBean> friendCircleBeans) {
        this.mFriendCircleBeans = friendCircleBeans;
        notifyDataSetChanged();
    }

    @Override
    public void onItemClickPopupMenu(int position, int itemId) {
        // No implementation needed
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