package com.example.wechatfriendforperformance.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Trace;
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

    // 负载类型常量
    public static final int LOAD_TYPE_LIGHT = 0;  // 轻负载
    public static final int LOAD_TYPE_MEDIUM = 1; // 中等负载
    public static final int LOAD_TYPE_HEAVY = 2;  // 高负载

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
    private Random mRandom = new Random(0); // 使用固定种子，确保每次运行结果一样
    private int mLoadType; // 负载类型
    
    // 用于模拟计算负载的变量
    private Paint mPaint;
    private Rect mRect;
    private Canvas mCanvas;
    private Bitmap mTempBitmap;
    
    // 标识当前负载类型的字符串
    private String mLoadTypeString;

    public PerformanceFriendCircleAdapter(Context context, RecyclerView recyclerView, 
                                         ImageLoader<String> imageLoader, int loadType) {
        this.mContext = context;
        this.mImageLoader = imageLoader;
        this.mRecyclerView = recyclerView;
        this.mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        this.mAvatarSize = dpToPx(44f);
        this.mLayoutInflater = LayoutInflater.from(context);
        
        // 使用圆角矩形替代圆形裁剪，圆角半径为8dp，与原项目保持一致
        this.mRequestOptions = new RequestOptions().transform(new RoundedCorners(dpToPx(8)));
        
        this.mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
        this.mLoadType = loadType;
        
        // 设置负载类型字符串
        switch (loadType) {
            case LOAD_TYPE_LIGHT:
                mLoadTypeString = "轻负载测试";
                break;
            case LOAD_TYPE_MEDIUM:
                mLoadTypeString = "中等负载测试";
                break;
            case LOAD_TYPE_HEAVY:
                mLoadTypeString = "高负载测试";
                break;
            default:
                mLoadTypeString = "性能测试用户";
                break;
        }
        
        // 初始化用于模拟计算负载的对象
        this.mPaint = new Paint();
        this.mRect = new Rect();
        this.mTempBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        this.mCanvas = new Canvas(mTempBitmap);
        
        if (context instanceof OnPraiseOrCommentClickListener) {
            this.mOnPraiseOrCommentClickListener = (OnPraiseOrCommentClickListener) context;
        }
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
            // header view不处理
            Trace.endSection();
            return;
        }
        
        int dataPosition = mHeaderView != null ? position - 1 : position;
        FriendCircleViewHolder viewHolder = (FriendCircleViewHolder) holder;
        FriendCircleBean friendCircleBean = mFriendCircleBeans.get(dataPosition);
        
        // 根据负载类型执行不同的计算量
        simulateComputationalLoad(dataPosition);
        
        // 设置用户信息
        UserBean userBean = friendCircleBean.getUserBean();
        if (userBean != null) {
            viewHolder.txtUserName.setText(userBean.getUserName());
            
            // 加载头像 - 使用预定义的RequestOptions实现圆角效果
            try {
                String avatarUrl = userBean.getUserAvatarUrl();
                // 处理文件扩展名
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
        
        // 设置内容
        viewHolder.txtContent.setText(friendCircleBean.getContent());
        
        // 设置图片
        List<String> imageUrls = friendCircleBean.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            viewHolder.nineGridView.setVisibility(View.VISIBLE);
            // 使用新的NineImageAdapter
            NineImageAdapter adapter = new NineImageAdapter(mContext, imageUrls);
            viewHolder.nineGridView.setAdapter(adapter);
        } else {
            viewHolder.nineGridView.setVisibility(View.GONE);
        }
        
        // 设置其他信息
        OtherInfoBean otherInfoBean = friendCircleBean.getOtherInfoBean();
        if (otherInfoBean != null) {
            // 设置时间
            String time = otherInfoBean.getTime();
            viewHolder.txtTime.setText(time);
            
            // 设置来源
            String source = otherInfoBean.getSource();
            if (source != null && !source.isEmpty()) {
                viewHolder.txtSource.setVisibility(View.VISIBLE);
                viewHolder.txtSource.setText(source);
            } else {
                viewHolder.txtSource.setVisibility(View.GONE);
            }
            
            // 设置位置 - 根据原项目的实现
            String location = otherInfoBean.getLocation();
            if (location != null && !location.isEmpty()) {
                viewHolder.txtLocation.setVisibility(View.VISIBLE);
                viewHolder.txtLocation.setText(location);
            } else {
                viewHolder.txtLocation.setVisibility(View.GONE);
            }
        }
        
        // 设置点赞和评论
        boolean hasPraise = friendCircleBean.getPraiseBeans() != null && !friendCircleBean.getPraiseBeans().isEmpty();
        boolean hasComment = friendCircleBean.getCommentBeans() != null && !friendCircleBean.getCommentBeans().isEmpty();
        
        if (hasPraise || hasComment) {
            viewHolder.layoutPraiseComment.setVisibility(View.VISIBLE);
            
            // 设置点赞
            if (hasPraise) {
                viewHolder.layoutPraise.setVisibility(View.VISIBLE);
                viewHolder.txtPraise.setText(friendCircleBean.getPraiseSpan());
            } else {
                viewHolder.layoutPraise.setVisibility(View.GONE);
            }
            
            // 设置评论
            if (hasComment) {
                viewHolder.recyclerViewComment.setVisibility(View.VISIBLE);
                CommentAdapter commentAdapter = new CommentAdapter(mContext, friendCircleBean.getCommentBeans());
                viewHolder.recyclerViewComment.setAdapter(commentAdapter);
            } else {
                viewHolder.recyclerViewComment.setVisibility(View.GONE);
            }
            
            // 设置分割线
            viewHolder.viewLine.setVisibility(hasPraise && hasComment ? View.VISIBLE : View.GONE);
        } else {
            viewHolder.layoutPraiseComment.setVisibility(View.GONE);
        }
        
        // 设置评论图标点击事件
        final int finalDataPosition = dataPosition;
        viewHolder.imgComment.setOnClickListener(v -> {
            if (mOnPraiseOrCommentClickListener != null) {
                mOnPraiseOrCommentClickListener.onCommentClick(finalDataPosition);
            }
        });
        
        Trace.endSection();
    }
    
    /**
     * 模拟不同级别的计算负载
     * @param position 数据位置
     */
    private void simulateComputationalLoad(int position) {
        Trace.beginSection("FriendCircleAdapter_simulateComputationalLoad");
        // 根据负载类型执行不同的计算量
        int iterations;
        switch (mLoadType) {
            case LOAD_TYPE_LIGHT:
                iterations = 5; // 轻负载：每帧只做少量工作
                break;
            case LOAD_TYPE_MEDIUM:
                iterations = 300; // 中等负载：每帧中等数量计算
                break;
            case LOAD_TYPE_HEAVY:
                iterations = 1000; // 高负载：每帧大量计算
                break;
            default:
                iterations = 5;
                break;
        }
        
        // 执行一些计算，模拟负载
        for (int i = 0; i < iterations; i++) {
            // 随机生成一些数据，避免编译器优化掉
            float x = mRandom.nextFloat() * 1000;
            float y = mRandom.nextFloat() * 1000;
            
            // 执行一些绘图操作，这会带来CPU和GPU负载
            mRect.set((int)x, (int)y, (int)(x + 100), (int)(y + 100));
            mPaint.setColor(0xFF000000 | mRandom.nextInt(0xFFFFFF));
            mCanvas.drawRect(mRect, mPaint);
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
        // 不需要实现
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
            
            // 设置评论列表
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