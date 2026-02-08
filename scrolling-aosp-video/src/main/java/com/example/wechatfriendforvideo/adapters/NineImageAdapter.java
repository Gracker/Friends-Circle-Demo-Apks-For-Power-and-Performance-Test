package com.example.wechatfriendforvideo.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.scrolling.common.widgets.NineGridView;
import com.example.wechatfriendforvideo.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 九宫格图片适配器
 */
public class NineImageAdapter implements NineGridView.NineGridAdapter<String> {

    private static Map<String, Integer> sResourceMap;

    private static int getDrawableId(Context context, String name) {
        if (sResourceMap == null) {
            sResourceMap = new HashMap<>();
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
    private List<String> mImageUrls;

    public NineImageAdapter(Context context, List<String> imageUrls) {
        this.mContext = context;
        this.mImageUrls = imageUrls;
    }

    @Override
    public int getCount() {
        return mImageUrls != null ? Math.min(mImageUrls.size(), 9) : 0;
    }

    @Override
    public String getItem(int position) {
        return mImageUrls != null && position < mImageUrls.size() ? mImageUrls.get(position) : null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null || !(convertView instanceof ImageView)) {
            imageView = new ImageView(mContext);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

        String imageUrl = mImageUrls.get(position);

        // 尝试加载本地资源
        int resourceId = getDrawableId(mContext, imageUrl);

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
            int localId = getDrawableId(mContext, localName);

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

