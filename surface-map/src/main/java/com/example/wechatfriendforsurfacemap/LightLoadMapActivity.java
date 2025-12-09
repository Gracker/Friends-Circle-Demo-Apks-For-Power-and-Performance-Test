package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

public class LightLoadMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.LIGHT;
    }
}
