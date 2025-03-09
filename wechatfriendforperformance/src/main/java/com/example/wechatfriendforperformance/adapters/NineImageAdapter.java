package com.example.wechatfriendforperformance.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.wechatfriendforperformance.R;
import com.example.wechatfriendforperformance.widgets.NineGridView;

import java.util.List;

/**
 * Adapter for NineGridView to display images in a grid
 */
public class NineImageAdapter implements NineGridView.NineGridAdapter<String> {

    private Context mContext;
    private List<String> mImageUrls;
    private int mImageSize;

    /**
     * Constructor
     * 
     * @param context Context
     * @param imageUrls List of image URLs to display
     */
    public NineImageAdapter(Context context, List<String> imageUrls) {
        this.mContext = context;
        this.mImageUrls = imageUrls;
        
        // One third of screen width as image size
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        mImageSize = (screenWidth - 2 * 6) / 3;
    }

    /**
     * Get the data list
     * 
     * @return List of image URLs
     */
    public List<String> getImageData() {
        return mImageUrls;
    }

    @Override
    public int getCount() {
        return mImageUrls == null ? 0 : mImageUrls.size();
    }

    @Override
    public String getItem(int position) {
        return mImageUrls == null ? null : mImageUrls.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            // 设置固定尺寸和边距
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    mImageSize, mImageSize));
        } else {
            imageView = (ImageView) convertView;
        }

        String imageUrl = mImageUrls.get(position);
        
        // 尝试加载测试图片
        try {
            // 首先尝试直接加载原始路径
            int resourceId = mContext.getResources().getIdentifier(
                    imageUrl.toLowerCase(), "drawable", mContext.getPackageName());

            if (resourceId != 0) {
                Glide.with(mContext)
                        .load(resourceId)
                        .apply(new RequestOptions().centerCrop())
                        .into(imageView);
            } else {
                // 尝试加载test_img_x格式的图片
                int fallbackId = mContext.getResources().getIdentifier(
                        "test_img_" + ((position % 10) + 1),
                        "drawable", mContext.getPackageName());
                
                if (fallbackId != 0) {
                    Glide.with(mContext)
                            .load(fallbackId)
                            .apply(new RequestOptions().centerCrop())
                            .into(imageView);
                } else {
                    // 最后使用默认图片
                    imageView.setBackgroundColor(Color.LTGRAY);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setBackgroundColor(Color.LTGRAY);
        }

        return imageView;
    }
}