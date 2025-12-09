package com.example.wechatfriendfordualwindow;

import com.example.loadconfig.LoadType;

public class HeavyBetweenFramesActivity extends BaseDualWindowActivity {
    @Override protected int getLoadType() { return LoadType.HEAVY_BETWEEN_FRAMES; }
}
