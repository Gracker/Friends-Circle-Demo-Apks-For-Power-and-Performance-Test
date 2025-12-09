package com.example.wechatfriendforpurerenderthread;

import com.example.loadconfig.LoadType;

public class HeavyLoadActivity extends BaseListActivity {
    @Override protected int getLoadType() { return LoadType.HEAVY; }
}
