package com.example.wechatfriendforpurerenderthread;

import com.example.loadconfig.LoadType;

public class HeavyBetweenFramesActivity extends BaseListActivity {
    @Override protected int getLoadType() { return LoadType.HEAVY_BETWEEN_FRAMES; }
}
