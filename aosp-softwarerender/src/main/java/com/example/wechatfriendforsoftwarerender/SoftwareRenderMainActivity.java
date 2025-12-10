package com.example.wechatfriendforsoftwarerender;

import android.content.Intent;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wechatfriendforsoftwarerender.adapters.SoftwareRenderFriendCircleAdapter;
import com.example.wechatfriendforsoftwarerender.model.AppInfo;
import com.example.loadconfig.LoadConfig;

/**
 * Performance test main Activity, used to select different load levels for testing
 */
public class SoftwareRenderMainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnMinimalLoad, btnLightLoad, btnMediumLoad, btnHeavyLoad;
    private Button btnLightLoadBetweenFrames, btnMediumLoadBetweenFrames, btnHeavyLoadBetweenFrames;
    private Button btnLightMixedLoad, btnMediumMixedLoad, btnHeavyMixedLoad;
    private Button btnLongFrameLoad;
    
    // 定义常量，用于Intent传递负载类型
    public static final String EXTRA_LOAD_TYPE = "load_type";
    // 定义常量，用于命令行直接启动不同Activity
    public static final String EXTRA_ACTIVITY_TYPE = "activity_type";
    
    // Activity类型常量
    public static final String ACTIVITY_TYPE_MINIMAL = "minimal";
    public static final String ACTIVITY_TYPE_LIGHT = "light";
    public static final String ACTIVITY_TYPE_MEDIUM = "medium";
    public static final String ACTIVITY_TYPE_HEAVY = "heavy";
    public static final String ACTIVITY_TYPE_LIGHT_BETWEEN_FRAMES = "light_between_frames";
    public static final String ACTIVITY_TYPE_MEDIUM_BETWEEN_FRAMES = "medium_between_frames";
    public static final String ACTIVITY_TYPE_HEAVY_BETWEEN_FRAMES = "heavy_between_frames";
    public static final String ACTIVITY_TYPE_LIGHT_MIXED = "light_mixed";
    public static final String ACTIVITY_TYPE_MEDIUM_MIXED = "medium_mixed";
    public static final String ACTIVITY_TYPE_HEAVY_MIXED = "heavy_mixed";
    public static final String ACTIVITY_TYPE_LONG_FRAME = "long_frame";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("SoftwareRenderMainActivity_onCreate");
        super.onCreate(savedInstanceState);
        // Apply theme without ActionBar
        setTheme(R.style.Theme_HighPerformanceFriendsCircle_NoActionBar);
        setContentView(R.layout.activity_performance_main);

        // 设置应用信息
        setupAppInfo();

        // 验证负载配置的科学性
        validateLoadConfiguration();
        
        // Initialize buttons
        btnMinimalLoad = findViewById(R.id.btn_minimal_load);
        btnLightLoad = findViewById(R.id.btn_light_load);
        btnMediumLoad = findViewById(R.id.btn_medium_load);
        btnHeavyLoad = findViewById(R.id.btn_heavy_load);
        btnLightLoadBetweenFrames = findViewById(R.id.btn_light_load_between_frames);
        btnMediumLoadBetweenFrames = findViewById(R.id.btn_medium_load_between_frames);
        btnHeavyLoadBetweenFrames = findViewById(R.id.btn_heavy_load_between_frames);
        btnLightMixedLoad = findViewById(R.id.btn_light_mixed_load);
        btnMediumMixedLoad = findViewById(R.id.btn_medium_mixed_load);
        btnHeavyMixedLoad = findViewById(R.id.btn_heavy_mixed_load);
        btnLongFrameLoad = findViewById(R.id.btn_long_frame_load);
        
        // Set click listeners
        btnMinimalLoad.setOnClickListener(this);
        btnLightLoad.setOnClickListener(this);
        btnMediumLoad.setOnClickListener(this);
        btnHeavyLoad.setOnClickListener(this);
        btnLightLoadBetweenFrames.setOnClickListener(this);
        btnMediumLoadBetweenFrames.setOnClickListener(this);
        btnHeavyLoadBetweenFrames.setOnClickListener(this);
        btnLightMixedLoad.setOnClickListener(this);
        btnMediumMixedLoad.setOnClickListener(this);
        btnHeavyMixedLoad.setOnClickListener(this);
        btnLongFrameLoad.setOnClickListener(this);
        
        // 检查是否通过命令行直接启动特定Activity
        checkForDirectActivityLaunch();
        
        Trace.endSection();
    }

    /**
     * 检查是否通过命令行直接启动特定Activity
     * 支持通过adb命令直接启动不同负载的Activity，方便CI测试
     * 
     * 使用方法：
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type minimal
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type light
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type medium
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type heavy
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type light_between_frames
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type medium_between_frames
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type heavy_between_frames
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type light_mixed
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type medium_mixed
     * adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type heavy_mixed
     */
    private void checkForDirectActivityLaunch() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ACTIVITY_TYPE)) {
            String activityType = intent.getStringExtra(EXTRA_ACTIVITY_TYPE);
            if (activityType != null) {
                // 清除缓存数据
                SoftwareRenderDataCenter.getInstance().clearCachedData();
                
                switch (activityType) {
                    case ACTIVITY_TYPE_MINIMAL:
                        startMinimalLoadActivity();
                        break;
                    case ACTIVITY_TYPE_LIGHT:
                        startLightLoadActivity();
                        break;
                    case ACTIVITY_TYPE_MEDIUM:
                        startMediumLoadActivity();
                        break;
                    case ACTIVITY_TYPE_HEAVY:
                        startHeavyLoadActivity();
                        break;
                    case ACTIVITY_TYPE_LIGHT_BETWEEN_FRAMES:
                        startLightLoadBetweenFramesActivity();
                        break;
                    case ACTIVITY_TYPE_MEDIUM_BETWEEN_FRAMES:
                        startMediumLoadBetweenFramesActivity();
                        break;
                    case ACTIVITY_TYPE_HEAVY_BETWEEN_FRAMES:
                        startHeavyLoadBetweenFramesActivity();
                        break;
                    case ACTIVITY_TYPE_LIGHT_MIXED:
                        startLightMixedLoadActivity();
                        break;
                    case ACTIVITY_TYPE_MEDIUM_MIXED:
                        startMediumMixedLoadActivity();
                        break;
                    case ACTIVITY_TYPE_HEAVY_MIXED:
                        startHeavyMixedLoadActivity();
                        break;
                    case ACTIVITY_TYPE_LONG_FRAME:
                        startLongFrameLoadActivity();
                        break;
                    default:
                        Log.w("SoftwareRenderMainActivity", "Unknown activity_type: " + activityType);
                        break;
                }
            }
        }
    }
    
    private void startMinimalLoadActivity() {
        Intent intent = new Intent(this, MinimalLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MINIMAL);
        startActivity(intent);
        finish(); // 关闭主Activity，避免返回时显示
    }
    
    private void startLightLoadActivity() {
        Intent intent = new Intent(this, LightLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT);
        startActivity(intent);
        finish();
    }
    
    private void startMediumLoadActivity() {
        Intent intent = new Intent(this, MediumLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MEDIUM);
        startActivity(intent);
        finish();
    }
    
    private void startHeavyLoadActivity() {
        Intent intent = new Intent(this, HeavyLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.HEAVY);
        startActivity(intent);
        finish();
    }
    
    private void startLightLoadBetweenFramesActivity() {
        Intent intent = new Intent(this, LightLoadBetweenFramesActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT_BETWEEN_FRAMES);
        startActivity(intent);
        finish();
    }
    
    private void startMediumLoadBetweenFramesActivity() {
        Intent intent = new Intent(this, MediumLoadBetweenFramesActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MEDIUM_BETWEEN_FRAMES);
        startActivity(intent);
        finish();
    }
    
    private void startHeavyLoadBetweenFramesActivity() {
        Intent intent = new Intent(this, HeavyLoadBetweenFramesActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.HEAVY_BETWEEN_FRAMES);
        startActivity(intent);
        finish();
    }
    
    private void startLightMixedLoadActivity() {
        Intent intent = new Intent(this, LightMixedLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT_MIXED);
        startActivity(intent);
        finish();
    }
    
    private void startMediumMixedLoadActivity() {
        Intent intent = new Intent(this, MediumMixedLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MEDIUM_MIXED);
        startActivity(intent);
        finish();
    }
    
    private void startHeavyMixedLoadActivity() {
        Intent intent = new Intent(this, HeavyMixedLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.HEAVY_MIXED);
        startActivity(intent);
        finish();
    }
    
    private void startLongFrameLoadActivity() {
        Intent intent = new Intent(this, LongFrameLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LONG_FRAME);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        Trace.beginSection("SoftwareRenderMainActivity_onResume");
        super.onResume();
        // 确保数据中心的缓存被清空
        SoftwareRenderDataCenter.getInstance().clearCachedData();
        Trace.endSection();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        // 先清除所有缓存的数据，确保每次都重新生成
        SoftwareRenderDataCenter.getInstance().clearCachedData();
        
        if (id == R.id.btn_minimal_load) {
            Trace.beginSection("SoftwareRenderMainActivity_startMinimalLoad");
            // Start MinimalLoad Activity
            Intent intent = new Intent(this, MinimalLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MINIMAL);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_light_load) {
            Trace.beginSection("SoftwareRenderMainActivity_startLightLoad");
            // Start LightLoad Activity
            Intent intent = new Intent(this, LightLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_medium_load) {
            Trace.beginSection("SoftwareRenderMainActivity_startMediumLoad");
            // Start MediumLoad Activity
            Intent intent = new Intent(this, MediumLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MEDIUM);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_heavy_load) {
            Trace.beginSection("SoftwareRenderMainActivity_startHeavyLoad");
            // Start HeavyLoad Activity
            Intent intent = new Intent(this, HeavyLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.HEAVY);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_light_load_between_frames) {
            Trace.beginSection("SoftwareRenderMainActivity_startLightLoadBetweenFrames");
            // Start LightLoadBetweenFrames Activity
            Intent intent = new Intent(this, LightLoadBetweenFramesActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT_BETWEEN_FRAMES);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_medium_load_between_frames) {
            Trace.beginSection("SoftwareRenderMainActivity_startMediumLoadBetweenFrames");
            // Start MediumLoadBetweenFrames Activity
            Intent intent = new Intent(this, MediumLoadBetweenFramesActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MEDIUM_BETWEEN_FRAMES);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_heavy_load_between_frames) {
            Trace.beginSection("SoftwareRenderMainActivity_startHeavyLoadBetweenFrames");
            // Start HeavyLoadBetweenFrames Activity
            Intent intent = new Intent(this, HeavyLoadBetweenFramesActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.HEAVY_BETWEEN_FRAMES);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_light_mixed_load) {
            Trace.beginSection("SoftwareRenderMainActivity_startLightMixedLoad");
            // Start Light MixedLoad Activity
            Intent intent = new Intent(this, LightMixedLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT_MIXED);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_medium_mixed_load) {
            Trace.beginSection("SoftwareRenderMainActivity_startMediumMixedLoad");
            // Start Medium MixedLoad Activity
            Intent intent = new Intent(this, MediumMixedLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MEDIUM_MIXED);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_heavy_mixed_load) {
            Trace.beginSection("SoftwareRenderMainActivity_startHeavyMixedLoad");
            // Start Heavy MixedLoad Activity
            Intent intent = new Intent(this, HeavyMixedLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.HEAVY_MIXED);
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_long_frame_load) {
            Trace.beginSection("SoftwareRenderMainActivity_startLongFrameLoad");
            // Start Long Frame Load Activity
            Intent intent = new Intent(this, LongFrameLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LONG_FRAME);
            startActivity(intent);
            Trace.endSection();
        }
    }
    
    /**
     * 验证负载配置的科学性和一致性
     */
    private void validateLoadConfiguration() {
        Log.d("LoadConfig", "开始验证负载配置...");
        
        boolean isValid = LoadConfig.validateConfig();
        if (isValid) {
            Log.d("LoadConfig", "✅ 负载配置验证通过");
            
            // 输出配置详情
            Log.d("LoadConfig", "📊 混合负载配置详情:");
            Log.d("LoadConfig", "  " + LoadConfig.getDescription(com.example.loadconfig.LoadType.LIGHT_MIXED));
            Log.d("LoadConfig", "  " + LoadConfig.getDescription(com.example.loadconfig.LoadType.MEDIUM_MIXED));
            Log.d("LoadConfig", "  " + LoadConfig.getDescription(com.example.loadconfig.LoadType.HEAVY_MIXED));
            
            // 输出任务间隔配置
            Log.d("LoadConfig", "📋 任务调度配置:");
            Log.d("LoadConfig", "  任务间隔: " + LoadConfig.MIN_TASK_INTERVAL_MS + "-" + 
                  LoadConfig.MAX_TASK_INTERVAL_MS + "ms");
            Log.d("LoadConfig", "  随机种子: Task=" + LoadConfig.TASK_INTERVAL_SEED + 
                  ", DoFrame=" + LoadConfig.DOFRAME_INTERVAL_SEED + 
                  ", Computation=" + LoadConfig.COMPUTATION_SEED);
                  
        } else {
            Log.e("LoadConfig", "❌ 负载配置验证失败！请检查LoadConfig中的数值设置");
        }
    }

    private void setupAppInfo() {
        // 创建应用信息
        AppInfo appInfo = new AppInfo(
            "朋友圈软件渲染版本",
            "软件渲染版本（禁用硬件加速）",
            getPackageName()
        );

        // 设置应用信息到UI
        TextView appNameView = findViewById(R.id.tv_app_name);
        TextView appFeatureView = findViewById(R.id.tv_app_feature);
        TextView packageNameView = findViewById(R.id.tv_package_name);

        if (appNameView != null) {
            appNameView.setText(appInfo.getAppName());
        }
        if (appFeatureView != null) {
            appFeatureView.setText(appInfo.getAppFeature());
        }
        if (packageNameView != null) {
            packageNameView.setText(appInfo.getPackageName());
        }
    }
} 