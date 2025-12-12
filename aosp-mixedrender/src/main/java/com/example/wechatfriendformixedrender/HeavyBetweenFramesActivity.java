package com.example.wechatfriendformixedrender;

import com.example.loadconfig.LoadType;

public class HeavyBetweenFramesActivity extends BaseMixedRenderActivity {
    @Override
    protected int getLoadType() {
        return LoadType.HEAVY_BETWEEN_FRAMES;
    }
}
