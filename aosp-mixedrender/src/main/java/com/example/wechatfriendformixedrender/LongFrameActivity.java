package com.example.wechatfriendformixedrender;

import com.example.loadconfig.LoadType;

public class LongFrameActivity extends BaseMixedRenderActivity {
    @Override
    protected int getLoadType() {
        return LoadType.LONG_FRAME;
    }
}
