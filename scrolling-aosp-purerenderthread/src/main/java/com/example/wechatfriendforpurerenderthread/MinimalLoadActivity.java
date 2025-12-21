package com.example.wechatfriendforpurerenderthread;

import com.example.loadconfig.LoadType;

public class MinimalLoadActivity extends BaseListActivity {
    @Override protected int getLoadType() { return LoadType.MINIMAL; }
}
