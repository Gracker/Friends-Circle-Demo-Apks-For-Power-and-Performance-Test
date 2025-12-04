package com.example.wechatfriendforvideo.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.wechatfriendforvideo.R;

import java.util.List;

/**
 * 九宫格图片适配器
 */
public class NineImageAdapter {
    
    private Context mContext;
    private List<String> mImageUrls;
    
    public NineImageAdapter(Context context, List<String> imageUrls) {
        this.mContext = context;
        this.mImageUrls = imageUrls;
    }
    
    public int getCount() {
        return mImageUrls != null ? Math.min(mImageUrls.size(), 9) : 0;
    }
    
    public ImageView getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null || !(convertView instanceof ImageView)) {
            imageView = new ImageView(mContext);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }
        
        String imageUrl = mImageUrls.get(position);
        
        // 尝试加载本地资源
        int resourceId = mContext.getResources().getIdentifier(
                imageUrl, "drawable", mContext.getPackageName());
        
        if (resourceId != 0) {
            Glide.with(mContext)
                    .load(resourceId)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.img_placeholder)
                    .into(imageView);
        } else {
            // 尝试加载local系列图片
            int localIndex = (position % 11) + 1;
            String localName = "local" + localIndex;
            int localId = mContext.getResources().getIdentifier(
                    localName, "drawable", mContext.getPackageName());
            
            if (localId != 0) {
                Glide.with(mContext)
                        .load(localId)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.img_placeholder)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.img_placeholder);
            }
        }
        
        return imageView;
    }
}

