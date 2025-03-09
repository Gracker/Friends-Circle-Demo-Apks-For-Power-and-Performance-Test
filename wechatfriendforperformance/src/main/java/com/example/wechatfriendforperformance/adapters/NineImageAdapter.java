package com.example.wechatfriendforperformance.adapters;

import android.content.Context;
import android.content.res.Resources;
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
        } else {
            imageView = (ImageView) convertView;
        }

        String imageUrl = mImageUrls.get(position);
        
        // Process image name, remove possible file extension
        if (imageUrl.contains(".")) {
            imageUrl = imageUrl.substring(0, imageUrl.lastIndexOf("."));
        }

        // Try to load from resource ID
        try {
            int resourceId = mContext.getResources().getIdentifier(
                    imageUrl.toLowerCase(), "drawable", mContext.getPackageName());

            if (resourceId != 0) {
                Glide.with(mContext)
                        .load(resourceId)
                        .apply(new RequestOptions().centerCrop())
                        .into(imageView);
            } else {
                // Load placeholder image
                Glide.with(mContext)
                        .load(R.drawable.img_placeholder)
                        .into(imageView);
            }
        } catch (Resources.NotFoundException e) {
            // If error, use placeholder image
            Glide.with(mContext)
                    .load(R.drawable.img_placeholder)
                    .into(imageView);
        }

        return imageView;
    }
}