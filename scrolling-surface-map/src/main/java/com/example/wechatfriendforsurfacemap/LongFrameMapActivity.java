package com.example.wechatfriendforsurfacemap;

import com.example.loadconfig.LoadType;

/**
 * Activity for long frame load (10x HEAVY intensity).
 */
public class LongFrameMapActivity extends BaseMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.LONG_FRAME;
    }
}
