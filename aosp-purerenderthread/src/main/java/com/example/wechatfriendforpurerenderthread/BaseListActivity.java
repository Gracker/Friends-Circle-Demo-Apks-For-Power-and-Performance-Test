package com.example.wechatfriendforpurerenderthread;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.loadconfig.LoadType;

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

    protected abstract @LoadType.Type int getLoadType();
}


