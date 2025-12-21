package com.example.wechatfriendforpurerenderthread;

import com.example.loadconfig.LoadType;

public class LongFrameActivity extends BaseListActivity {
    @Override
    protected int getLoadType() {
        return LoadType.LONG_FRAME;
    }
}
