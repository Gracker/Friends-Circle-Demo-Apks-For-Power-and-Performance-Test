package com.example.wechatfriendforcustomscroller.ui.timeline;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Trace;

import com.example.wechatfriendforcustomscroller.LoadProfile;

import java.util.Random;

/**
 * 复用 wechatfriendforperformance 模块的负载配置，保证轻/中/重三档体验一致。
 */
final class LoadStressSimulator {

    private static final Random RANDOM = new Random(0);
    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static Bitmap sBitmap;
    private static Canvas sCanvas;

    private LoadStressSimulator() {
    }

    private static void ensureCanvas() {
        if (sBitmap == null || sCanvas == null) {
            sBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            sCanvas = new Canvas(sBitmap);
        }
    }

    public static void runAdapterLoad(@LoadProfile.LoadType int loadType) {
        ensureCanvas();
        int iterations;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_MEDIUM:
                iterations = 800;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY:
                iterations = 2000;
                break;
            case LoadProfile.LOAD_TYPE_LIGHT:
            default:
                iterations = 5;
                break;
        }
        Trace.beginSection("FriendCircleAdapter_simulateComputationalLoad");
        performIterations(iterations, loadType >= LoadProfile.LOAD_TYPE_MEDIUM);
        Trace.endSection();
    }

    public static void runContinuousLoad(@LoadProfile.LoadType int loadType) {
        int iterations;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_MEDIUM:
                iterations = 200;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY:
                iterations = 500;
                break;
            default:
                iterations = 0;
                break;
        }
        if (iterations <= 0) {
            return;
        }
        ensureCanvas();
        Trace.beginSection("FriendCircleAdapter_continuousLoad");
        performIterations(iterations, true);
        Trace.endSection();
    }

    private static void performIterations(int iterations, boolean includeExtraMath) {
        for (int i = 0; i < iterations; i++) {
            float x = RANDOM.nextFloat() * 100;
            float y = RANDOM.nextFloat() * 100;

            PAINT.setColor(Color.argb(
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256)
            ));
            sCanvas.drawCircle(x, y, 10, PAINT);

            if (includeExtraMath) {
                double sinValue = Math.sin(x) * Math.cos(y);
                double tanValue = Math.tan(x * 0.1);
                if (sinValue > 0.999 && tanValue > 100) {
                    PAINT.setStrokeWidth((float) (sinValue + tanValue));
                }
            }
        }
    }
}


