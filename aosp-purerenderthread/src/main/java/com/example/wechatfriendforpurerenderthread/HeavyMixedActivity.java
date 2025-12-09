package com.example.wechatfriendforpurerenderthread;

import com.example.loadconfig.LoadType;

public class HeavyMixedActivity extends BaseListActivity {
    @Override protected int getLoadType() { return LoadType.HEAVY_MIXED; }
}
