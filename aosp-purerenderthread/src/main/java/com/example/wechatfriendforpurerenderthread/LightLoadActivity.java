package com.example.wechatfriendforpurerenderthread;

import com.example.loadconfig.LoadType;

public class LightLoadActivity extends BaseListActivity {
    @Override protected int getLoadType() { return LoadType.LIGHT; }
}
