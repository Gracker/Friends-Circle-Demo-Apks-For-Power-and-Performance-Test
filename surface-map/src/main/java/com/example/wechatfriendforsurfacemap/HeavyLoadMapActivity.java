package com.example.wechatfriendforsurfacemap;

public class HeavyLoadMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadProfile.LOAD_TYPE_HEAVY;
    }
}


