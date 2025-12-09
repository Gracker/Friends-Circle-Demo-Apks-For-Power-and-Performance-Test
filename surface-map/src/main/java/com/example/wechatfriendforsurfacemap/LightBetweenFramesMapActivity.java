package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

public class LightBetweenFramesMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.LIGHT_BETWEEN_FRAMES;
    }
}
