package com.kcrason.highperformancefriendscircle.adapters;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.kcrason.highperformancefriendscircle.widgets.NineGridView;
import com.kcrason.highperformancefriendscircle.R;
import com.kcrason.highperformancefriendscircle.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KCrason
 * @date 2018/4/27
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

    private List<String> mImageBeans;

    private Context mContext;

    private RequestOptions mRequestOptions;

    private DrawableTransitionOptions mDrawableTransitionOptions;


    public NineImageAdapter(Context context, RequestOptions requestOptions, DrawableTransitionOptions drawableTransitionOptions, List<String> imageBeans) {
        this.mContext = context;
        this.mDrawableTransitionOptions = drawableTransitionOptions;
        this.mImageBeans = imageBeans;
        int itemSize = (Utils.getScreenWidth() - 2 * Utils.dp2px(4) - Utils.dp2px(54)) / 3;
        this.mRequestOptions = requestOptions.override(itemSize, itemSize);
    }

    @Override
    public int getCount() {
        return mImageBeans == null ? 0 : mImageBeans.size();
    }

    @Override
    public String getItem(int position) {
        return mImageBeans == null ? null :
                position < mImageBeans.size() ? mImageBeans.get(position) : null;
    }

    @Override
    public View getView(int position, View itemView) {
        ImageView imageView;
        if (itemView == null) {
            imageView = new ImageView(mContext);
            imageView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.base_F2F2F2));
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            imageView = (ImageView) itemView;
        }
        String url = mImageBeans.get(position);
        int resourceId = getDrawableId(mContext, url);
        if (resourceId != 0) {
            Glide.with(mContext).load(resourceId).apply(mRequestOptions).transition(mDrawableTransitionOptions).into(imageView);
        }
        return imageView;
    }
}
