package com.example.wechatfriendforsurfacemap;

public class LightBetweenFramesMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadProfile.LOAD_TYPE_LIGHT_BETWEEN_FRAMES;
    }
}


