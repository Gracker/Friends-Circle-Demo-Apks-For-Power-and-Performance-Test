package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

public class HeavyMixedMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.HEAVY_MIXED;
    }
}
