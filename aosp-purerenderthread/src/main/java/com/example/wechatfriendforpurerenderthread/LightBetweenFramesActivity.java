package com.example.wechatfriendforpurerenderthread;

import com.example.loadconfig.LoadType;

public class LightBetweenFramesActivity extends BaseListActivity {
    @Override protected int getLoadType() { return LoadType.LIGHT_BETWEEN_FRAMES; }
}
