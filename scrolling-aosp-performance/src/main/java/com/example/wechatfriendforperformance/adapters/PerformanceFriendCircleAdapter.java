package com.example.wechatfriendforperformance.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
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
import com.example.scrolling.common.beans.CommentBean;
import com.example.scrolling.common.beans.FriendCircleBean;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.loadconfig.ScrollLoadGate;

/**
 * 性能测试专用的朋友圈适配器，支持不同负载级别
 * 使用统一的 LoadConfig 和 LoadType 进行负载配置
 */
public class PerformanceFriendCircleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements OnItemClickPopupMenuListener {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_NORMAL = 1;

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
    private List<FriendCircleBean> mFriendCircleBeans;
    private RequestOptions mRequestOptions;
    private int mAvatarSize;
    private DrawableTransitionOptions mDrawableTransitionOptions;
    private OnPraiseOrCommentClickListener mOnPraiseOrCommentClickListener;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    // 移除ImageLoader成员变量
    private View mHeaderView;
    private Random mRandom = new Random(LoadConfig.DATA_GENERATION_SEED); // Using fixed seed to ensure consistent
                                                                          // results for each run
    private int mLoadType; // Load type

    // 统一负载模拟器
    private LoadSimulator mLoadSimulator;

    // String to identify current load type
    private String mLoadTypeString;

    // Variables for continuous frame load simulation - REMOVED

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

        // 使用统一的 LoadType.toLabel() 获取负载类型字符串
        mLoadTypeString = LoadType.toLabel(loadType);

        // 初始化负载模拟器
        mLoadSimulator = new LoadSimulator();
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
        boolean hasComment = friendCircleBean.getCommentBeans() != null
                && !friendCircleBean.getCommentBeans().isEmpty();

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
            mLoadSimulator.executeInFrameLoad(mLoadType, "Adapter_bindLoad");
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        // 释放负载模拟器资源
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }

    @Override
    public int getItemCount() {
        int count = mFriendCircleBeans == null ? 0 : mFriendCircleBeans.size();
        return mHeaderView == null ? count : count + 1;
    }

    public void setFriendCircleBeans(List<FriendCircleBean> newBeans) {
        final List<FriendCircleBean> oldBeans = this.mFriendCircleBeans;
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

    /**
     * 停止持续负载模拟，释放资源
     * 在 Activity onDestroy 时调用
     */
    public void stopContinuousLoadSimulation() {
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
        }
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