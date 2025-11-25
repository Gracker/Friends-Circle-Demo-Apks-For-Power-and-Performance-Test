package com.example.wechatfriendforpurerenderthread;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base Activity for pure RenderThread list demos.
 */
public abstract class BaseListActivity extends AppCompatActivity {
    
    protected PureRenderListView listView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        
        listView = findViewById(R.id.pure_render_list);
        listView.setLoadType(getLoadType());
    }
    
    protected abstract @LoadProfile.LoadType int getLoadType();
}


