package com.android.wechatfriendforpower.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.wechatfriendforpower.PowerConstants;
import com.android.wechatfriendforpower.PowerUtils;
import com.android.wechatfriendforpower.R;
import com.android.wechatfriendforpower.beans.CommentBean;
import com.android.wechatfriendforpower.beans.FriendCircleBean;
import com.android.wechatfriendforpower.beans.OtherInfoBean;
import com.android.wechatfriendforpower.beans.UserBean;
import com.android.wechatfriendforpower.interfaces.OnItemClickPopupMenuListener;
import com.android.wechatfriendforpower.interfaces.OnPraiseOrCommentClickListener;
import com.android.wechatfriendforpower.widgets.NineGridView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
// 移除StfalconImageViewer相关imports

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.util.Log;

/**
 * Power测试专用的朋友圈适配器
 */
public class PowerFriendCircleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> 
        implements OnItemClickPopupMenuListener {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_NORMAL = 1;
    
    private static final String TAG = "PowerFriendCircleAdapter";
    
    private Context mContext;
    private List<FriendCircleBean> mFriendCircleBeans;
    private RequestOptions mRequestOptions;
    private int mAvatarSize;
    private DrawableTransitionOptions mDrawableTransitionOptions;
    private OnPraiseOrCommentClickListener mOnPraiseOrCommentClickListener;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private View mHeaderView;
    private Random mRandom = new Random(0); // 使用固定种子，确保每次运行结果一样

    public PowerFriendCircleAdapter(Context context, RecyclerView recyclerView) {
        this.mContext = context;
        this.mRecyclerView = recyclerView;
        this.mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        this.mAvatarSize = PowerUtils.dp2px(44f);
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mRequestOptions = new RequestOptions().centerCrop();
        this.mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
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
        if (mHeaderView != null) {
            // 设置固定的背景图片 main_bg.jpg
            ImageView imgCover = mHeaderView.findViewById(R.id.img_cover);
            if (imgCover != null) {
                int coverResourceId = mContext.getResources().getIdentifier(
                    "main_bg", "drawable", mContext.getPackageName());
                if (coverResourceId != 0) {
                    Glide.with(mContext)
                         .load(coverResourceId)
                         .centerCrop()
                         .into(imgCover);
                }
            }
            
            // 设置固定的头像 main_avatar.jpg，使用大圆角变换
            ImageView imgUserAvatar = mHeaderView.findViewById(R.id.img_user_avatar);
            if (imgUserAvatar != null) {
                int avatarResourceId = mContext.getResources().getIdentifier(
                    "main_avatar", "drawable", mContext.getPackageName());
                if (avatarResourceId != 0) {
                    RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(24)); // 更大的圆角
                    
                    Glide.with(mContext)
                         .load(avatarResourceId)
                         .apply(options)
                         .into(imgUserAvatar);
                }
            }
            
            // 设置用户昵称固定为Gracker
            TextView txtUserNickname = mHeaderView.findViewById(R.id.txt_user_nickname);
            if (txtUserNickname != null) {
                txtUserNickname.setText("Gracker");
            }
            
            // 设置返回按钮点击事件
            ImageView imgBack = mHeaderView.findViewById(R.id.img_back);
            if (imgBack != null && mContext instanceof android.app.Activity) {
                imgBack.setOnClickListener(v -> ((android.app.Activity) mContext).finish());
            }
        }
    }

    public void setFriendCircleBeans(List<FriendCircleBean> friendCircleBeans) {
        this.mFriendCircleBeans = friendCircleBeans;
        notifyDataSetChanged();
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
        View view = mLayoutInflater.inflate(R.layout.item_friend_circle, parent, false);
        return new FriendCircleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            // 头部视图不需要额外绑定数据
            return;
        }
        
        if (holder instanceof FriendCircleViewHolder && mFriendCircleBeans != null) {
            int dataPosition = mHeaderView != null ? position - 1 : position;
            if (dataPosition < mFriendCircleBeans.size()) {
                FriendCircleViewHolder circleHolder = (FriendCircleViewHolder) holder;
                
                // 为第一个正常的列表项添加额外的顶部边距
                if (position == 1 && mHeaderView != null) {
                    circleHolder.itemView.setPadding(
                        circleHolder.itemView.getPaddingLeft(),
                        32, // 32dp的顶部间距
                        circleHolder.itemView.getPaddingRight(),
                        circleHolder.itemView.getPaddingBottom()
                    );
                } else {
                    circleHolder.itemView.setPadding(
                        circleHolder.itemView.getPaddingLeft(),
                        16, // 恢复默认16dp的顶部间距
                        circleHolder.itemView.getPaddingRight(),
                        circleHolder.itemView.getPaddingBottom()
                    );
                }
                
                FriendCircleBean friendCircleBean = mFriendCircleBeans.get(dataPosition);
                
                // 设置内容
                circleHolder.txtContent.setText(friendCircleBean.getContent());
                
                // 设置用户信息
                UserBean userBean = friendCircleBean.getUserBean();
                if (userBean != null) {
                    circleHolder.txtUserName.setText(userBean.getUserName());
                    String avatarUrl = userBean.getUserAvatarUrl();
                    int resourceId = mContext.getResources().getIdentifier(
                        avatarUrl, "drawable", mContext.getPackageName());
                    if (resourceId != 0) {
                        // 添加圆角变换
                        RequestOptions avatarOptions = new RequestOptions()
                            .override(mAvatarSize, mAvatarSize)
                            .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(12));
                            
                        Glide.with(mContext)
                            .load(resourceId)
                            .apply(avatarOptions)
                            .transition(mDrawableTransitionOptions)
                            .into(circleHolder.imgAvatar);
                    }
                }
                
                // 设置其他信息
                OtherInfoBean otherInfoBean = friendCircleBean.getOtherInfoBean();
                if (otherInfoBean != null) {
                    circleHolder.txtTime.setText(otherInfoBean.getTime());
                    circleHolder.txtSource.setText(otherInfoBean.getSource());
                }
                
                // 设置位置信息 - 根据位置循环显示
                int locationIndex = dataPosition % PowerConstants.LOCATIONS.length;
                circleHolder.txtLocation.setText(PowerConstants.LOCATIONS[locationIndex]);
                
                // 设置九宫格图片
                List<String> imageUrls = friendCircleBean.getImageUrls();
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    circleHolder.nineGridView.setVisibility(View.VISIBLE);
                    circleHolder.nineGridView.setAdapter(new NineImageAdapter(mContext, mRequestOptions, 
                            mDrawableTransitionOptions, imageUrls));
                    
                    // 移除图片点击事件
                    circleHolder.nineGridView.setOnItemClickListener(null);
                } else {
                    circleHolder.nineGridView.setVisibility(View.GONE);
                }
                
                // 设置点赞和评论区域
                if (friendCircleBean.getPraiseBeans() != null && !friendCircleBean.getPraiseBeans().isEmpty() || 
                    friendCircleBean.getCommentBeans() != null && !friendCircleBean.getCommentBeans().isEmpty()) {
                    circleHolder.layoutPraiseComment.setVisibility(View.VISIBLE);
                    
                    // 设置点赞内容
                    if (friendCircleBean.getPraiseBeans() != null && !friendCircleBean.getPraiseBeans().isEmpty()) {
                        circleHolder.txtPraise.setVisibility(View.VISIBLE);
                        circleHolder.txtPraise.setText(friendCircleBean.getPraiseSpan());
                        
                        // 如果有评论，显示分割线
                        if (friendCircleBean.getCommentBeans() != null && !friendCircleBean.getCommentBeans().isEmpty()) {
                            circleHolder.viewLine.setVisibility(View.VISIBLE);
                        } else {
                            circleHolder.viewLine.setVisibility(View.GONE);
                        }
                    } else {
                        circleHolder.txtPraise.setVisibility(View.GONE);
                        circleHolder.viewLine.setVisibility(View.GONE);
                    }
                    
                    // 设置评论内容
                    if (friendCircleBean.getCommentBeans() != null && !friendCircleBean.getCommentBeans().isEmpty()) {
                        circleHolder.commentLayout.setVisibility(View.VISIBLE);
                        circleHolder.commentLayout.removeAllViews();
                        
                        // 显示所有评论
                        for (CommentBean commentBean : friendCircleBean.getCommentBeans()) {
                            TextView textView = new TextView(mContext);
                            textView.setTextSize(16);
                            textView.setPadding(0, 4, 0, 4);
                            textView.setText(commentBean.getCommentContentSpan());
                            circleHolder.commentLayout.addView(textView);
                        }
                    } else {
                        circleHolder.commentLayout.setVisibility(View.GONE);
                    }
                } else {
                    circleHolder.layoutPraiseComment.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = mFriendCircleBeans == null ? 0 : mFriendCircleBeans.size();
        return mHeaderView != null ? count + 1 : count;
    }

    @Override
    public void onItemClickCopy(int position) {
        Log.d(TAG, "复制成功");
    }

    @Override
    public void onItemClickCollection(int position) {
        Log.d(TAG, "收藏成功");
    }

    @Override
    public void onItemClickTranslation(int position) {
        // 简化处理，不实现翻译功能
    }

    @Override
    public void onItemClickHideTranslation(int position) {
        // 简化处理，不实现翻译功能
    }
    
    /**
     * 头部ViewHolder
     */
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * 朋友圈ViewHolder
     */
    public static class FriendCircleViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserName;
        TextView txtTime;
        TextView txtSource;
        TextView txtContent;
        TextView txtLocation;
        ImageView imgAvatar;
        NineGridView nineGridView;
        ImageView imgPraiseComment;
        ImageView imgComment;
        TextView txtPraise;
        View layoutPraiseComment;
        View viewLine;
        ViewGroup commentLayout;

        public FriendCircleViewHolder(View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.txt_user_name);
            txtTime = itemView.findViewById(R.id.txt_time);
            txtSource = itemView.findViewById(R.id.txt_source);
            txtContent = itemView.findViewById(R.id.txt_content);
            txtLocation = itemView.findViewById(R.id.txt_location);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            nineGridView = itemView.findViewById(R.id.nine_grid_view);
            imgPraiseComment = itemView.findViewById(R.id.img_praise_comment);
            imgComment = itemView.findViewById(R.id.img_comment);
            txtPraise = itemView.findViewById(R.id.txt_praise);
            layoutPraiseComment = itemView.findViewById(R.id.layout_praise_comment);
            viewLine = itemView.findViewById(R.id.view_line);
            commentLayout = itemView.findViewById(R.id.comment_layout);
        }
    }

    /**
     * 设置点赞或评论点击监听器
     * @param listener 监听器
     */
    public void setOnPraiseOrCommentClickListener(OnPraiseOrCommentClickListener listener) {
        this.mOnPraiseOrCommentClickListener = listener;
    }

    /**
     * 点赞或评论点击监听器
     */
    public interface OnPraiseOrCommentClickListener {
        /**
         * 点赞点击事件
         * @param friendCircleBean 朋友圈数据
         * @param position 位置
         */
        void onPraiseClick(FriendCircleBean friendCircleBean, int position);
        
        /**
         * 评论点击事件
         * @param friendCircleBean 朋友圈数据
         * @param position 位置
         */
        void onCommentClick(FriendCircleBean friendCircleBean, int position);
    }
} 