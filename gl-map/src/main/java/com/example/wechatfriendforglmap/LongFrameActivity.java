package com.example.wechatfriendforglmap;

import com.example.loadconfig.LoadType;

/**
 * Activity for long frame load (10x HEAVY intensity).
 */
public class LongFrameActivity extends BaseGLMapActivity {
    @Override
    protected int getLoadType() {
        return LoadType.LONG_FRAME;
    }
}
