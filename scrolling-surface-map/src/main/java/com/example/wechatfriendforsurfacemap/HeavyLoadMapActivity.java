package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

public class HeavyLoadMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.HEAVY;
    }
}
