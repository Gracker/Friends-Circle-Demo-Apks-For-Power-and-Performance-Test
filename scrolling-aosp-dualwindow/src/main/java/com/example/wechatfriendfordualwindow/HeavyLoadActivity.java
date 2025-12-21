package com.example.wechatfriendfordualwindow;

import com.example.loadconfig.LoadType;

public class HeavyLoadActivity extends BaseDualWindowActivity {
    @Override protected int getLoadType() { return LoadType.HEAVY; }
}
