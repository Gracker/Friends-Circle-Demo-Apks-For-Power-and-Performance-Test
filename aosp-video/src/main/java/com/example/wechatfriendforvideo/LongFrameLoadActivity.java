package com.example.wechatfriendforvideo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loadconfig.LoadConfig;
import com.example.wechatfriendforvideo.adapters.VideoFriendCircleAdapter;

import java.util.Random;

/**
 * Long Frame Load Activity - 超长帧负载测试
 * 每次滑动 2 秒内随机触发 2-3 次超长帧（HEAVY 负载的 10 倍）
 */
public class LongFrameLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "LongFrameLoadActivity";
    private RecyclerView recyclerView;
    private VideoFriendCircleAdapter adapter;
    private int mLoadType = com.example.loadconfig.LoadType.HEAVY;
    
    private Choreographer mChoreographer;
    private Random mRandom = new Random(LoadConfig.COMPUTATION_SEED);
    private Paint mPaint = new Paint();
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private boolean mIsEnabled = true;
    private boolean mIsScrolling = false;
    
    // Long frame state
    private long mScrollStartTime = 0;
    private int mLongFrameTriggerCount = 0;
    private int mCurrentLongFrameIndex = 0;
    private long[] mLongFrameTriggerTimes;
    private long mLastLongFrameTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_load);
        
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        initHeavyFrameComponents();
        
        mChoreographer = Choreographer.getInstance();
        initScrollListener();
    }
    
    private void initScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                    resetLongFrameState();
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    startLongFrameCycle();
                    mChoreographer.postFrameCallback(LongFrameLoadActivity.this);
                }
            }
        });
    }
    
    private void initHeavyFrameComponents() {
        mBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPaint.setAntiAlias(true);
    }
    
    private void startLongFrameCycle() {
        mScrollStartTime = System.currentTimeMillis();
        mLongFrameTriggerCount = LoadConfig.getLongFrameTriggerCount();
        mLongFrameTriggerTimes = LoadConfig.getLongFrameTriggerTimes(mLongFrameTriggerCount);
        mCurrentLongFrameIndex = 0;
        mLastLongFrameTime = 0;
        Log.d(TAG, "startLongFrameCycle: 计划触发" + mLongFrameTriggerCount + "次超长帧");
    }
    
    private void resetLongFrameState() {
        mScrollStartTime = 0;
        mCurrentLongFrameIndex = 0;
        mLongFrameTriggerTimes = null;
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsEnabled && mIsScrolling) {
            checkAndExecuteLongFrame();
            mChoreographer.postFrameCallback(this);
        }
    }
    
    private void checkAndExecuteLongFrame() {
        if (mCurrentLongFrameIndex >= mLongFrameTriggerCount || mLongFrameTriggerTimes == null) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - mScrollStartTime;
        
        if (elapsedTime >= mLongFrameTriggerTimes[mCurrentLongFrameIndex]) {
            if (currentTime - mLastLongFrameTime >= LoadConfig.LONG_FRAME_MIN_INTERVAL_MS) {
                Log.d(TAG, "触发超长帧 #" + (mCurrentLongFrameIndex + 1) + "/" + mLongFrameTriggerCount);
                
                Trace.beginSection("LongFrameLoad_execute_" + (mCurrentLongFrameIndex + 1));
                executeLongFrameLoad();
                Trace.endSection();
                
                mLastLongFrameTime = currentTime;
                mCurrentLongFrameIndex++;
            }
        }
        
        if (elapsedTime >= LoadConfig.LONG_FRAME_SCROLL_PERIOD_MS) {
            startLongFrameCycle();
        }
    }
    
    private void executeLongFrameLoad() {
        int intensity = LoadConfig.LONG_FRAME_INTENSITY;
        
        for (int i = 0; i < intensity; i++) {
            float x = mRandom.nextFloat() * 500;
            float y = mRandom.nextFloat() * 500;
            
            mPaint.setColor(Color.argb(
                    mRandom.nextInt(256),
                    mRandom.nextInt(256),
                    mRandom.nextInt(256),
                    mRandom.nextInt(256)
            ));
            
            mCanvas.drawCircle(x, y, 10 + mRandom.nextFloat() * 10, mPaint);
            
            double sinValue = Math.sin(x) * Math.cos(y);
            double sqrtValue = Math.sqrt(x * x + y * y);
            double logValue = Math.log(i + 1) + Math.log10(i + 1);
            
            if (sqrtValue > 400 && logValue > 5) {
                mPaint.setARGB((int) sqrtValue % 256, (int) logValue * 25 % 256, 
                               (int) (sinValue * 100) % 256, 255);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }
        if (!mIsEnabled) {
            mIsEnabled = true;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mIsEnabled = false;
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        if (adapter == null) {
            adapter = new VideoFriendCircleAdapter(this, recyclerView, mLoadType);
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        } else {
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.stopContinuousLoadSimulation();
        }
        recyclerView.setAdapter(null);
        adapter = null;
        
        mIsEnabled = false;
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mCanvas = null;
    }
}

