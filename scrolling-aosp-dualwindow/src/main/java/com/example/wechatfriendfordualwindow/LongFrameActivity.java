package com.example.wechatfriendfordualwindow;

import com.example.loadconfig.LoadType;

public class LongFrameActivity extends BaseDualWindowActivity {
    @Override
    protected int getLoadType() {
        return LoadType.LONG_FRAME;
    }
}
