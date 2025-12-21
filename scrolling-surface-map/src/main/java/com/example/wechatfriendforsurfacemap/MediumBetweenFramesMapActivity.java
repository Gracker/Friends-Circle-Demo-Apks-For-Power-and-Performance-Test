package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

public class MediumBetweenFramesMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.MEDIUM_BETWEEN_FRAMES;
    }
}
