package com.android.wechatfriendforpower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.wechatfriendforpower.adapters.PowerFriendCircleAdapter;
import com.android.wechatfriendforpower.beans.FriendCircleBean;
import com.android.wechatfriendforpower.utils.PowerDataGenerator;
import com.android.wechatfriendforpower.utils.PowerMonitorUtils;

import java.util.List;

/**
 * 朋友圈功耗测试主活动
 */
public class PowerActivity extends AppCompatActivity implements PowerFriendCircleAdapter.OnPraiseOrCommentClickListener {
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PowerFriendCircleAdapter adapter;
    private TextView tvBattery;
    private ImageView ivBattery;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerDataGenerator dataGenerator;
    
    // 电池状态接收器
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                updateBatteryInfo(intent);
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power);
        
        initViews();
        initData();
        
        // 开始电池监控
        startBatteryMonitoring();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvBattery = findViewById(R.id.tv_battery);
        ivBattery = findViewById(R.id.iv_battery);
        
        // 设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        
        // 设置适配器
        adapter = new PowerFriendCircleAdapter(this, recyclerView);
        adapter.setOnPraiseOrCommentClickListener(this);
        recyclerView.setAdapter(adapter);
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }
    
    private void initData() {
        dataGenerator = new PowerDataGenerator(this);
        loadData(10); // 默认加载10条数据
    }
    
    /**
     * 加载数据
     * @param count 数据条数
     */
    public void loadData(int count) {
        List<FriendCircleBean> data = dataGenerator.generateFakeFriendCircleData(count);
        adapter.setFriendCircleBeans(data);
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        swipeRefreshLayout.setRefreshing(true);
        
        // 延时模拟网络请求
        handler.postDelayed(() -> {
            loadData(10);
            swipeRefreshLayout.setRefreshing(false);
        }, 1000);
    }
    
    /**
     * 开始电池监控
     */
    public void startBatteryMonitoring() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }
    
    /**
     * 停止电池监控
     */
    public void stopBatteryMonitoring() {
        unregisterReceiver(batteryReceiver);
    }
    
    /**
     * 更新电池信息
     * @param intent 电池状态Intent
     */
    public void updateBatteryInfo(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int percentage = (int) ((level / (float) scale) * 100);
        
        // 更新UI
        tvBattery.setText("电量: " + percentage + "%");
        ivBattery.setImageLevel(percentage);
        
        // 收集并保存电池统计信息
        PowerMonitorUtils.saveBatteryStatsToLog(
                PowerMonitorUtils.collectBatteryStatistics(intent));
    }
    
    @Override
    public void onPraiseClick(FriendCircleBean friendCircleBean, int position) {
        // 处理点赞事件
        if (friendCircleBean != null) {
            // 实际应用中这里应该调用API请求
            // 这里简单模拟处理
            adapter.notifyItemChanged(position);
        }
    }
    
    @Override
    public void onCommentClick(FriendCircleBean friendCircleBean, int position) {
        // 处理评论事件
        if (friendCircleBean != null) {
            // 显示评论输入框
            showCommentBox(friendCircleBean, position);
        }
    }
    
    /**
     * 显示评论输入框
     * @param friendCircleBean 朋友圈数据
     * @param position 位置
     */
    public void showCommentBox(FriendCircleBean friendCircleBean, int position) {
        // 实际应用中这里应该弹出软键盘和评论输入框
        // 这里简单模拟
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBatteryMonitoring();
    }
} 