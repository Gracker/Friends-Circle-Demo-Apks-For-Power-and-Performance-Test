package com.example.wechatfriendforperformance.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Trace;
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
import com.example.wechatfriendforperformance.widgets.NineGridView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
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

    public PerformanceFriendCircleAdapter(Context context, RecyclerView recyclerView, int loadType) {
        this.mContext = context;
        this.mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        this.mRecyclerView = recyclerView;
        this.mLayoutInflater = LayoutInflater.from(context);
        
        // Use rounded rectangle instead of circular crop, with 8dp corner radius to match the original project
        this.mRequestOptions = new RequestOptions().transform(new CircleCrop());
        
        this.mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
        this.mLoadType = loadType;
        
        // Set load type string
        switch (loadType) {
            case LOAD_TYPE_LIGHT:
                mLoadTypeString = "Light Load";
                break;
            case LOAD_TYPE_MEDIUM:
                mLoadTypeString = "Medium Load";
                break;
            case LOAD_TYPE_HEAVY:
                mLoadTypeString = "Heavy Load";
                break;
            default:
                mLoadTypeString = "Unknown Load";
                break;
        }
        
        // Initialize objects for simulating computational load
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        mBitmapList.add(bitmap);
        
        if (context instanceof OnPraiseOrCommentClickListener) {
            mOnPraiseOrCommentClickListener = (OnPraiseOrCommentClickListener) context;
        }
    }

    public PerformanceFriendCircleAdapter(Context context, RecyclerView recyclerView, 
                                     ImageLoader<String> imageLoader, int loadType) {
        this(context, recyclerView, loadType);
        this.mImageLoader = imageLoader;
    }

    private LayoutInflater mLayoutInflater;

    public void setHeaderView(View headerView) {
        mHeaderView = headerView;
        setupHeaderView();
        notifyItemInserted(0);
    }
    
    private void setupHeaderView() {
        if (mHeaderView == null) {
            return;
        }
        
        // 加载背景图
        ImageView imgCover = mHeaderView.findViewById(R.id.img_cover);
        if (imgCover != null) {
            try {
                int coverResourceId = mContext.getResources().getIdentifier(
                    "main_bg", "drawable", mContext.getPackageName());
                if (coverResourceId != 0) {
                    Glide.with(mContext)
                        .load(coverResourceId)
                        .into(imgCover);
                } else {
                    imgCover.setImageResource(R.drawable.default_background);
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
                int avatarResourceId = mContext.getResources().getIdentifier(
                    "main_avatar", "drawable", mContext.getPackageName());
                if (avatarResourceId != 0) {
                    Glide.with(mContext)
                        .load(avatarResourceId)
                        .apply(mRequestOptions)
                        .into(imgUserAvatar);
                } else {
                    Glide.with(mContext)
                        .load(R.drawable.default_avatar)
                        .apply(mRequestOptions)
                        .into(imgUserAvatar);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Glide.with(mContext)
                    .load(R.drawable.default_avatar)
                    .apply(mRequestOptions)
                    .into(imgUserAvatar);
            }
        }
        
        // 设置用户名 - 显示当前负载类型
        TextView tvUserName = mHeaderView.findViewById(R.id.tv_user_name);
        if (tvUserName != null) {
            tvUserName.setText(mLoadTypeString);
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
        Trace.beginSection("FriendCircleAdapter_onBindViewHolder");
        if (getItemViewType(position) == TYPE_HEADER) {
            // header view not processed
            Trace.endSection();
            return;
        }
        
        int dataPosition = mHeaderView != null ? position - 1 : position;
        FriendCircleViewHolder viewHolder = (FriendCircleViewHolder) holder;
        FriendCircleBean friendCircleBean = mFriendCircleBeans.get(dataPosition);
        
        // Execute different computation based on load type
        simulateComputationalLoad(dataPosition);
        
        // Set user information
        UserBean userBean = friendCircleBean.getUserBean();
        if (userBean != null) {
            viewHolder.txtUserName.setText(userBean.getUserName());
            
            // Load avatar - use predefined RequestOptions for rounded effect
            try {
                String avatarUrl = userBean.getUserAvatarUrl();
                // Handle file extension
                if (avatarUrl.contains(".")) {
                    avatarUrl = avatarUrl.substring(0, avatarUrl.lastIndexOf("."));
                }
                
                int avatarResourceId = mContext.getResources().getIdentifier(
                    avatarUrl.toLowerCase(), "drawable", mContext.getPackageName());
                
                if (avatarResourceId != 0) {
                    Glide.with(mContext)
                        .load(avatarResourceId)
                        .apply(mRequestOptions)
                        .transition(mDrawableTransitionOptions)
                        .into(viewHolder.imgAvatar);
                } else {
                    Glide.with(mContext)
                        .load(R.drawable.default_avatar)
                        .apply(mRequestOptions)
                        .into(viewHolder.imgAvatar);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Glide.with(mContext)
                    .load(R.drawable.default_avatar)
                    .apply(mRequestOptions)
                    .into(viewHolder.imgAvatar);
            }
        }
        
        // Set content
        viewHolder.txtContent.setText(friendCircleBean.getContent());
        
        // Set images
        List<String> imageUrls = friendCircleBean.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            viewHolder.nineGridView.setVisibility(View.VISIBLE);
            // Use new NineImageAdapter
            NineImageAdapter adapter = new NineImageAdapter(mContext, imageUrls);
            viewHolder.nineGridView.setAdapter(adapter);
        } else {
            viewHolder.nineGridView.setVisibility(View.GONE);
        }
        
        // Set other information
        OtherInfoBean otherInfoBean = friendCircleBean.getOtherInfoBean();
        if (otherInfoBean != null) {
            // Set time
            String time = otherInfoBean.getTime();
            viewHolder.txtTime.setText(time);
            
            // Set source
            String source = otherInfoBean.getSource();
            if (source != null && !source.isEmpty()) {
                viewHolder.txtSource.setVisibility(View.VISIBLE);
                viewHolder.txtSource.setText(source);
            } else {
                viewHolder.txtSource.setVisibility(View.GONE);
            }
            
            // Set location - according to the implementation of the original project
            String location = otherInfoBean.getLocation();
            if (location != null && !location.isEmpty()) {
                viewHolder.txtLocation.setVisibility(View.VISIBLE);
                viewHolder.txtLocation.setText(location);
            } else {
                viewHolder.txtLocation.setVisibility(View.GONE);
            }
        }
        
        // Set likes and comments
        boolean hasPraise = friendCircleBean.getPraiseBeans() != null && !friendCircleBean.getPraiseBeans().isEmpty();
        boolean hasComment = friendCircleBean.getCommentBeans() != null && !friendCircleBean.getCommentBeans().isEmpty();
        
        if (hasPraise || hasComment) {
            viewHolder.layoutPraiseComment.setVisibility(View.VISIBLE);
            
            // Set likes
            if (hasPraise) {
                viewHolder.layoutPraise.setVisibility(View.VISIBLE);
                viewHolder.txtPraise.setText(friendCircleBean.getPraiseSpan());
            } else {
                viewHolder.layoutPraise.setVisibility(View.GONE);
            }
            
            // Set comments
            if (hasComment) {
                viewHolder.recyclerViewComment.setVisibility(View.VISIBLE);
                CommentAdapter commentAdapter = new CommentAdapter(mContext, friendCircleBean.getCommentBeans());
                viewHolder.recyclerViewComment.setAdapter(commentAdapter);
            } else {
                viewHolder.recyclerViewComment.setVisibility(View.GONE);
            }
            
            // Set divider
            viewHolder.viewLine.setVisibility(hasPraise && hasComment ? View.VISIBLE : View.GONE);
        } else {
            viewHolder.layoutPraiseComment.setVisibility(View.GONE);
        }
        
        // Set comment icon click event
        final int finalDataPosition = dataPosition;
        viewHolder.imgComment.setOnClickListener(v -> {
            if (mOnPraiseOrCommentClickListener != null) {
                mOnPraiseOrCommentClickListener.onCommentClick(finalDataPosition);
            }
        });
        
        Trace.endSection();
    }
    
    /**
     * Execute different computation based on load type
     * @param position Data position
     */
    private void simulateComputationalLoad(int position) {
        Trace.beginSection("FriendCircleAdapter_simulateComputationalLoad");
        int iterations;
        switch (mLoadType) {
            case LOAD_TYPE_LIGHT:
                iterations = 5; // Light load: only do a small amount of work per frame
                break;
            case LOAD_TYPE_MEDIUM:
                iterations = 300; // Medium load: medium number of calculations per frame
                break;
            case LOAD_TYPE_HEAVY:
                iterations = 1000; // Heavy load: large amount of calculation per frame
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
        }
        Trace.endSection();
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
        RecyclerView recyclerViewComment;
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
            recyclerViewComment = itemView.findViewById(R.id.recycler_view_comment);
            viewLine = itemView.findViewById(R.id.view_line);
            imgComment = itemView.findViewById(R.id.img_comment);
            
            // Set comment list
            recyclerViewComment.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
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