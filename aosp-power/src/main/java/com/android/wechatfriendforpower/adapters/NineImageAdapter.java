package com.android.wechatfriendforpower.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.android.wechatfriendforpower.PowerUtils;
import com.android.wechatfriendforpower.R;
import com.android.wechatfriendforpower.widgets.NineGridView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

/**
 * 九宫格图片适配器
 */
public class NineImageAdapter implements NineGridView.NineGridAdapter<String> {

    private List<String> mImageUrls;
    private Context mContext;
    private RequestOptions mRequestOptions;
    private DrawableTransitionOptions mDrawableTransitionOptions;

    public NineImageAdapter(Context context, RequestOptions requestOptions, 
                           DrawableTransitionOptions drawableTransitionOptions, 
                           List<String> imageUrls) {
        this.mContext = context;
        this.mDrawableTransitionOptions = drawableTransitionOptions;
        this.mImageUrls = imageUrls;
        int itemSize = (PowerUtils.getScreenWidth() - 2 * PowerUtils.dp2px(4) - PowerUtils.dp2px(54)) / 3;
        this.mRequestOptions = requestOptions.override(itemSize, itemSize);
    }

    @Override
    public int getCount() {
        return mImageUrls == null ? 0 : mImageUrls.size();
    }

    @Override
    public String getItem(int position) {
        return mImageUrls == null ? null :
                position < mImageUrls.size() ? mImageUrls.get(position) : null;
    }

    @Override
    public View getView(int position, View itemView) {
        ImageView imageView;
        if (itemView == null) {
            imageView = new ImageView(mContext);
            imageView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white));
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) itemView;
        }
        
        String url = mImageUrls.get(position);
        int resourceId = mContext.getResources().getIdentifier(
            url, "drawable", mContext.getPackageName());
        if (resourceId != 0) {
            Glide.with(mContext)
                .load(resourceId)
                .apply(mRequestOptions)
                .transition(mDrawableTransitionOptions)
                .into(imageView);
        }
        
        return imageView;
    }
} 