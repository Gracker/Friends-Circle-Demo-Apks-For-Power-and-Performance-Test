package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

public class MediumLoadMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.MEDIUM;
    }
}
