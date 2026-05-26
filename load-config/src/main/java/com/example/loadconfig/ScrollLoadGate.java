package com.example.loadconfig;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Shared scroll-load boundary for demos that inject synthetic load.
 *
 * User drag (SCROLL_STATE_DRAGGING) must stay clean. Synthetic load is only
 * allowed after release, while RecyclerView is settling from inertia.
 */
public final class ScrollLoadGate {

    private ScrollLoadGate() {
    }

    public static boolean isInertiaState(int scrollState) {
        return scrollState == RecyclerView.SCROLL_STATE_SETTLING;
    }

    public static boolean shouldRunForRecyclerView(@Nullable RecyclerView recyclerView) {
        return recyclerView != null && isInertiaState(recyclerView.getScrollState());
    }
}
