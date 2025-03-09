package com.example.wechatfriendforperformance.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.wechatfriendforperformance.R;
import com.example.wechatfriendforperformance.widgets.NineGridView;

import java.util.List;

/**
 * NineImageAdapter for displaying images in a grid
 */
public class NineImageAdapter implements NineGridView.NineGridAdapter<String> {

    private Context mContext;
    private List<String> mImageUrls;
    private int mImageSize;
    private RequestOptions mRequestOptions;
    private DrawableTransitionOptions mDrawableTransitionOptions;

    /**
     * Constructor for the adapter
     * @param context Context
     * @param imageUrls List of image URLs
     */
    public NineImageAdapter(Context context, List<String> imageUrls) {
        this.mContext = context;
        this.mImageUrls = imageUrls;
        
        // 计算图片大小 - 屏幕宽度减去边距，然后除以3
        int screenWidth = getScreenWidth(context);
        // 54dp是左侧头像的宽度和边距，每张图片间隔4dp
        mImageSize = (screenWidth - dpToPx(context, 54) - dpToPx(context, 4) * 2) / 3;
        
        // 设置图片加载选项
        mRequestOptions = new RequestOptions()
                .centerCrop()
                .override(mImageSize, mImageSize);
        
        // 设置过渡动画
        mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
    }
    
    /**
     * 获取图片数据
     * @return 图片URL列表
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
        return mImageUrls == null ? null : 
               position < mImageUrls.size() ? mImageUrls.get(position) : null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.background_gray));
            imageView.setLayoutParams(new ViewGroup.LayoutParams(mImageSize, mImageSize));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }
        
        // 直接加载测试图片
        try {
            String url = mImageUrls.get(position);
            
            // 简化处理：直接尝试使用test_img_x作为资源名称
            int testImgIndex = (position % 12) + 1; // 使用1-12的图片资源
            String resourceName = "test_img_" + testImgIndex;
            
            int resourceId = mContext.getResources().getIdentifier(
                    resourceName, "drawable", mContext.getPackageName());
            
            if (resourceId != 0) {
                // 发现资源，加载它
                Glide.with(mContext)
                    .load(resourceId)
                    .apply(mRequestOptions)
                    .transition(mDrawableTransitionOptions)
                    .into(imageView);
            } else {
                // 尝试直接使用原始URL作为资源名称
                resourceId = mContext.getResources().getIdentifier(
                    url, "drawable", mContext.getPackageName());
                
                if (resourceId != 0) {
                    Glide.with(mContext)
                        .load(resourceId)
                        .apply(mRequestOptions)
                        .transition(mDrawableTransitionOptions)
                        .into(imageView);
                } else {
                    // 设置默认背景色
                    imageView.setBackgroundColor(Color.LTGRAY);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setBackgroundColor(Color.LTGRAY);
        }
        
        return imageView;
    }
    
    /**
     * 获取屏幕宽度
     */
    private int getScreenWidth(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        return displayMetrics.widthPixels;
    }
    
    /**
     * dp转px
     */
    private int dpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}