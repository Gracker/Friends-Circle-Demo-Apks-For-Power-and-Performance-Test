package com.example.wechatfriendforsurfacemap;

public class HeavyBetweenFramesMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES;
    }
}


