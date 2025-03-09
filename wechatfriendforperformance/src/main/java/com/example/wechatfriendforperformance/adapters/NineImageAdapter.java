package com.example.wechatfriendforperformance.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.example.wechatfriendforperformance.R;
import com.example.wechatfriendforperformance.widgets.NineGridView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

/**
 * 九宫格图片适配器，与原项目保持一致
 */
public class NineImageAdapter implements NineGridView.NineGridAdapter<String> {

    private List<String> mImageUrls;
    private Context mContext;
    private RequestOptions mRequestOptions;
    private DrawableTransitionOptions mDrawableTransitionOptions;

    public NineImageAdapter(Context context, List<String> imageUrls) {
        this.mContext = context;
        this.mImageUrls = imageUrls;
        this.mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
        
        // 屏幕宽度的三分之一作为图片大小
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int itemSize = (screenWidth - 2 * dpToPx(4) - dpToPx(54)) / 3;
        this.mRequestOptions = new RequestOptions()
            .centerCrop()
            .override(itemSize, itemSize);
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
        try {
            // 处理图片名称，去掉可能的文件扩展名
            String imageName = url;
            if (imageName.contains(".")) {
                imageName = imageName.substring(0, imageName.lastIndexOf("."));
            }
            
            // 尝试从资源ID加载
            int resourceId = mContext.getResources().getIdentifier(
                imageName.toLowerCase(), "drawable", mContext.getPackageName());
            
            if (resourceId != 0) {
                Glide.with(mContext)
                    .load(resourceId)
                    .apply(mRequestOptions)
                    .transition(mDrawableTransitionOptions)
                    .into(imageView);
            } else {
                // 加载占位图
                imageView.setImageResource(R.drawable.img_placeholder);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 如果出错，使用占位图
            imageView.setImageResource(R.drawable.img_placeholder);
        }
        
        return imageView;
    }
    
    /**
     * dp转px
     */
    private int dpToPx(float dp) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}