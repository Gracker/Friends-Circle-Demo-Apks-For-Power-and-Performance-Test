package com.example.wechatfriendformixedrender;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Trace;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.scrolling.common.beans.FriendCircleBean;
import com.example.scrolling.common.beans.OtherInfoBean;
import com.example.scrolling.common.beans.UserBean;
import com.example.scrolling.common.model.MomentsDataFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard RecyclerView adapter that renders using normal UI + RenderThread pipeline.
 * Combined with PureRenderAnimationView, this creates mixed rendering scenario.
 */
public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private int loadType = LoadType.MINIMAL;
    private static final int ITEM_COUNT = 300;
    private LoadSimulator mLoadSimulator = new LoadSimulator();
    private List<FriendCircleBean> mFriendCircleBeans = new ArrayList<>();
    private boolean loadEnabled = false;

    public ListAdapter() {
        refreshData();
    }

    public void setLoadType(@LoadType.Type int loadType) {
        if (this.loadType != loadType || mFriendCircleBeans.isEmpty()) {
            this.loadType = loadType;
            refreshData();
        }
    }

    public void setLoadEnabled(boolean enabled) {
        loadEnabled = enabled;
    }

    private void refreshData() {
        mFriendCircleBeans = MomentsDataFactory.create(loadType, ITEM_COUNT);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(com.example.scrolling.common.R.layout.item_moments_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trace.beginSection("MixedRender_onBindViewHolder");

        FriendCircleBean bean = mFriendCircleBeans.get(position);
        UserBean userBean = bean.getUserBean();
        OtherInfoBean otherInfoBean = bean.getOtherInfoBean();

        holder.nameText.setText(userBean != null ? userBean.getUserName() : "微信用户");
        holder.contentText.setText(!TextUtils.isEmpty(bean.getContent()) ? bean.getContent() : "");
        holder.timeText.setText(otherInfoBean != null && !TextUtils.isEmpty(otherInfoBean.getTime())
                ? otherInfoBean.getTime() : "刚刚");

        if (otherInfoBean != null && !TextUtils.isEmpty(otherInfoBean.getSource())) {
            holder.sourceText.setVisibility(View.VISIBLE);
            holder.sourceText.setText(otherInfoBean.getSource());
        } else {
            holder.sourceText.setVisibility(View.GONE);
            holder.sourceText.setText("");
        }

        // Set avatar color as a circular placeholder
        String userId = userBean != null ? userBean.getUserId() : String.valueOf(position);
        int hue = Math.abs((userId + position).hashCode()) % 360;
        GradientDrawable avatarDrawable = new GradientDrawable();
        avatarDrawable.setShape(GradientDrawable.OVAL);
        avatarDrawable.setColor(Color.HSVToColor(new float[]{hue, 0.45f, 0.88f}));
        holder.avatarView.setBackground(avatarDrawable);

        boolean hasImage = bean.getImageUrls() != null && !bean.getImageUrls().isEmpty();
        if (hasImage) {
            int imageHue = Math.abs((bean.getContent() + position).hashCode()) % 360;
            GradientDrawable imageDrawable = new GradientDrawable();
            imageDrawable.setCornerRadius(dp(holder.itemView, 8));
            imageDrawable.setColor(Color.HSVToColor(new float[]{imageHue, 0.25f, 0.96f}));
            holder.previewImage.setBackground(imageDrawable);
            holder.previewImage.setVisibility(View.VISIBLE);
        } else {
            holder.previewImage.setVisibility(View.GONE);
        }

        // Execute load
        executeLoad();

        Trace.endSection();
    }

    @Override
    public int getItemCount() {
        return mFriendCircleBeans.size();
    }

    private float dp(View view, int dp) {
        return dp * view.getResources().getDisplayMetrics().density;
    }

    private void executeLoad() {
        // 使用统一的负载中心执行负载
        if (mLoadSimulator != null && loadEnabled) {
            mLoadSimulator.executeInFrameLoad(loadType, "MixedRender_UIThread_doFrameLoad");
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View avatarView;
        TextView nameText;
        TextView contentText;
        View previewImage;
        TextView timeText;
        TextView sourceText;

        ViewHolder(View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(com.example.scrolling.common.R.id.avatar);
            nameText = itemView.findViewById(com.example.scrolling.common.R.id.name);
            contentText = itemView.findViewById(com.example.scrolling.common.R.id.content);
            previewImage = itemView.findViewById(com.example.scrolling.common.R.id.preview_image);
            timeText = itemView.findViewById(com.example.scrolling.common.R.id.time);
            sourceText = itemView.findViewById(com.example.scrolling.common.R.id.source);
        }
    }
}
