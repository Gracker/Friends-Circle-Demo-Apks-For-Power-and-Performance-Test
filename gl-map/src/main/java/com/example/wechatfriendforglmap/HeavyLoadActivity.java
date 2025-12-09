package com.example.wechatfriendforglmap;

import com.example.loadconfig.LoadType;

public class HeavyLoadActivity extends BaseGLMapActivity {
    @Override protected int getLoadType() { return LoadType.HEAVY; }
}
