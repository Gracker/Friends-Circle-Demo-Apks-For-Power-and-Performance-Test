package com.kcrason.highperformancefriendscircle.utils;

import android.util.Log;

import com.kcrason.highperformancefriendscircle.interfaces.OnTimerResultListener;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * @author KCrason
 * @date 2018/5/6
 */
public class TimerUtils {
    private static final String TAG = "TimerUtils";

    /**
     * 延迟执行任务
     * @param onTimerResultListener 回调监听器
     * @return Disposable 用于取消订阅，调用者应在Activity/Fragment销毁时调用dispose()
     */
    public static Disposable timerTranslation(OnTimerResultListener onTimerResultListener) {
        return Single.timer(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        aLong -> {
                            if (onTimerResultListener != null) {
                                onTimerResultListener.onTimerResult();
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Timer error", throwable);
                        }
                );
    }
}
