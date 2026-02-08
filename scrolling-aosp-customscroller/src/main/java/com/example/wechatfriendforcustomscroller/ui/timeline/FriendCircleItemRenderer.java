package com.example.wechatfriendforcustomscroller.ui.timeline;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.loadconfig.LoadType;
import com.example.wechatfriendforcustomscroller.R;
import com.example.wechatfriendforcustomscroller.adapters.NineImageAdapter;
import com.example.scrolling.common.beans.CommentBean;
import com.example.scrolling.common.beans.FriendCircleBean;
import com.example.scrolling.common.beans.OtherInfoBean;
import com.example.scrolling.common.beans.PraiseBean;
import com.example.scrolling.common.beans.UserBean;
import com.example.scrolling.common.utils.SpanUtils;
import com.example.scrolling.common.widgets.NineGridView;

import java.util.List;

/**
 * 负责创建并绑定朋友圈Item View，供自定义Timeline使用。
 */
public class FriendCircleItemRenderer {

    private final Context context;
    private final LayoutInflater layoutInflater;
    private final RequestOptions avatarOptions;
    private final DrawableTransitionOptions transitionOptions;

    public FriendCircleItemRenderer(Context context) {
        this.context = context.getApplicationContext();
        this.layoutInflater = LayoutInflater.from(context);
        this.avatarOptions = new RequestOptions().transform(new RoundedCorners(30));
        this.transitionOptions = DrawableTransitionOptions.withCrossFade();
    }

    public View createItemView(ViewGroup parent) {
        View view = layoutInflater.inflate(R.layout.item_friend_circle, parent, false);
        view.setTag(new FriendCircleViewHolder(view));
        return view;
    }

    public void bind(@NonNull View view,
                     @NonNull FriendCircleBean friendCircleBean,
                     int position,
                     @LoadType.Type int loadType) {
        FriendCircleViewHolder holder = (FriendCircleViewHolder) view.getTag();
        bindUserSection(holder, friendCircleBean, position);
        bindContentSection(holder, friendCircleBean);
        bindOtherInfo(holder, friendCircleBean);
        bindSocialSection(holder, friendCircleBean);
        LoadStressSimulator.runAdapterLoad(loadType);
    }

    public void bindHeaderView(@NonNull View headerView,
                               @LoadType.Type int loadType,
                               @NonNull Runnable backAction) {
        ImageView cover = headerView.findViewById(R.id.img_cover);
        ImageView avatar = headerView.findViewById(R.id.img_user_avatar);
        TextView name = headerView.findViewById(R.id.tv_user_name);
        ImageView back = headerView.findViewById(R.id.img_back);

        loadDrawableByName(cover, "main_bg", R.drawable.default_background, new RequestOptions().centerCrop());
        loadDrawableByName(avatar, "main_avatar", R.drawable.default_avatar, avatarOptions);
        name.setText(LoadType.toLabel(loadType));
        back.setOnClickListener(v -> backAction.run());
    }

    private void bindUserSection(FriendCircleViewHolder holder,
                                 FriendCircleBean bean,
                                 int position) {
        UserBean userBean = bean.getUserBean();
        if (userBean != null) {
            holder.txtUserName.setText(userBean.getUserName());
            loadDrawableByName(holder.imgAvatar, userBean.getUserAvatarUrl(), R.drawable.default_avatar, avatarOptions);
        } else {
            holder.txtUserName.setText(R.string.performance_test);
            holder.imgAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    private void bindContentSection(FriendCircleViewHolder holder, FriendCircleBean bean) {
        if (!TextUtils.isEmpty(bean.getContent())) {
            holder.txtContent.setVisibility(View.VISIBLE);
            holder.txtContent.setText(bean.getContent());
        } else {
            holder.txtContent.setVisibility(View.GONE);
        }

        if (bean.getImageUrls() != null && !bean.getImageUrls().isEmpty()) {
            holder.nineGridView.setVisibility(View.VISIBLE);
            holder.nineGridView.setAdapter(new NineImageAdapter(holder.itemView.getContext(), bean.getImageUrls()));
        } else {
            holder.nineGridView.setVisibility(View.GONE);
        }
    }

    private void bindOtherInfo(FriendCircleViewHolder holder, FriendCircleBean bean) {
        OtherInfoBean infoBean = bean.getOtherInfoBean();
        if (infoBean != null) {
            holder.txtTime.setText(infoBean.getTime());
            if (!TextUtils.isEmpty(infoBean.getSource())) {
                holder.txtSource.setVisibility(View.VISIBLE);
                holder.txtSource.setText(infoBean.getSource());
            } else {
                holder.txtSource.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(infoBean.getLocation())) {
                holder.txtLocation.setVisibility(View.VISIBLE);
                holder.txtLocation.setText(infoBean.getLocation());
            } else {
                holder.txtLocation.setVisibility(View.GONE);
            }
        } else {
            holder.txtTime.setText("");
            holder.txtSource.setVisibility(View.GONE);
            holder.txtLocation.setVisibility(View.GONE);
        }
    }

    private void bindSocialSection(FriendCircleViewHolder holder, FriendCircleBean bean) {
        List<PraiseBean> praiseBeans = bean.getPraiseBeans();
        List<CommentBean> commentBeans = bean.getCommentBeans();
        boolean hasPraise = praiseBeans != null && !praiseBeans.isEmpty();
        boolean hasComment = commentBeans != null && !commentBeans.isEmpty();

        if (!hasPraise && !hasComment) {
            holder.layoutPraiseComment.setVisibility(View.GONE);
            return;
        }
        holder.layoutPraiseComment.setVisibility(View.VISIBLE);

        if (hasPraise) {
            SpannableStringBuilder praiseSpan = bean.getPraiseSpan();
            if (praiseSpan == null) {
                praiseSpan = SpanUtils.makePraiseSpan(holder.itemView.getContext(), praiseBeans);
                bean.setPraiseSpan(praiseSpan);
            }
            holder.layoutPraise.setVisibility(View.VISIBLE);
            holder.txtPraise.setText(praiseSpan);
        } else {
            holder.layoutPraise.setVisibility(View.GONE);
        }

        holder.layoutComment.removeAllViews();
        if (hasComment) {
            holder.layoutComment.setVisibility(View.VISIBLE);
            for (CommentBean commentBean : commentBeans) {
                commentBean.build(holder.itemView.getContext());
                TextView textView = buildCommentTextView(holder.itemView.getContext(), commentBean);
                holder.layoutComment.addView(textView);
            }
        } else {
            holder.layoutComment.setVisibility(View.GONE);
        }
    }

    private TextView buildCommentTextView(Context context, CommentBean commentBean) {
        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setTextColor(context.getResources().getColor(R.color.base_333333));
        textView.setTextSize(14);
        textView.setPadding(16, 8, 16, 8);
        textView.setText(commentBean.getCommentContentSpan());
        return textView;
    }

    private void loadDrawableByName(ImageView imageView,
                                    String resourceName,
                                    int fallback,
                                    RequestOptions options) {
        Context ctx = imageView.getContext();
        Drawable placeholder = ctx.getDrawable(fallback);
        if (TextUtils.isEmpty(resourceName)) {
            imageView.setImageDrawable(placeholder);
            return;
        }
        int resourceId = ctx.getResources().getIdentifier(resourceName, "drawable", ctx.getPackageName());
        if (resourceId == 0) {
            resourceId = fallback;
        }
        RequestOptions requestOptions = options != null ? options : new RequestOptions();
        Glide.with(ctx)
                .load(resourceId)
                .apply(requestOptions)
                .transition(transitionOptions)
                .into(imageView);
    }

    private static class FriendCircleViewHolder {
        final View itemView;
        final ImageView imgAvatar;
        final TextView txtUserName;
        final TextView txtContent;
        final NineGridView nineGridView;
        final TextView txtLocation;
        final TextView txtTime;
        final TextView txtSource;
        final LinearLayout layoutPraiseComment;
        final LinearLayout layoutPraise;
        final TextView txtPraise;
        final LinearLayout layoutComment;

        FriendCircleViewHolder(View itemView) {
            this.itemView = itemView;
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
            layoutComment = itemView.findViewById(R.id.layout_comment);
        }
    }
}

