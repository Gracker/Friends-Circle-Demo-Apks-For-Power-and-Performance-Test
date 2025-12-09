package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

public class HeavyBetweenFramesMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.HEAVY_BETWEEN_FRAMES;
    }
}
