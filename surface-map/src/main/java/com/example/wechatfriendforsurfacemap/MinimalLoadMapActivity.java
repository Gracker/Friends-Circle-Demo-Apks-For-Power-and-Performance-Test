package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

public class MinimalLoadMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.MINIMAL;
    }
}
