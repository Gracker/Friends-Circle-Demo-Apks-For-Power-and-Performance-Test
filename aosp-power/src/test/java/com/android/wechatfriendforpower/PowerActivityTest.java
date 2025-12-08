package com.android.wechatfriendforpower;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.wechatfriendforpower.adapters.PowerFriendCircleAdapter;
import com.android.wechatfriendforpower.beans.FriendCircleBean;
import com.android.wechatfriendforpower.utils.PowerDataGenerator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;

/**
 * PowerActivity的单元测试
 */
@RunWith(RobolectricTestRunner.class)
public class PowerActivityTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private RecyclerView mockRecyclerView;
    
    @Mock
    private SwipeRefreshLayout mockSwipeRefreshLayout;
    
    @Mock
    private PowerFriendCircleAdapter mockAdapter;
    
    @Mock
    private LinearLayoutManager mockLayoutManager;
    
    @Mock
    private TextView mockBatteryTextView;
    
    @Mock
    private ImageView mockBatteryImageView;
    
    @Mock
    private Handler mockHandler;
    
    private PowerActivity activitySpy;
    private ActivityController<PowerActivity> activityController;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置控制器和模拟Activity
        activityController = Robolectric.buildActivity(PowerActivity.class);
        PowerActivity activity = activityController.get();
        activitySpy = spy(activity);
        
        // 模拟视图
        when(activitySpy.findViewById(R.id.recycler_view)).thenReturn(mockRecyclerView);
        when(activitySpy.findViewById(R.id.swipe_refresh_layout)).thenReturn(mockSwipeRefreshLayout);
        when(activitySpy.findViewById(R.id.tv_battery)).thenReturn(mockBatteryTextView);
        when(activitySpy.findViewById(R.id.iv_battery)).thenReturn(mockBatteryImageView);
        
        // 设置适配器和布局管理器
        when(mockRecyclerView.getAdapter()).thenReturn(mockAdapter);
        when(mockRecyclerView.getLayoutManager()).thenReturn(mockLayoutManager);
        
        // 模拟Handler的行为
        doNothing().when(mockHandler).postDelayed(any(Runnable.class), anyInt());
    }
    
    @Test
    public void testOnCreate() {
        // 执行onCreate
        activityController.create();
        
        // 验证初始化视图
        verify(activitySpy, times(1)).setContentView(anyInt());
        
        // 验证初始化RecyclerView
        verify(mockRecyclerView, times(1)).setLayoutManager(any(LinearLayoutManager.class));
        verify(mockRecyclerView, times(1)).setAdapter(any(PowerFriendCircleAdapter.class));
        
        // 验证设置下拉刷新监听器
        verify(mockSwipeRefreshLayout, times(1)).setOnRefreshListener(any());
    }
    
    @Test
    public void testLoadData() {
        // 准备模拟数据
        List<FriendCircleBean> testData = PowerTestUtil.createTestFriendCircleBeans(10);
        
        // 模拟PowerDataGenerator生成数据
        PowerDataGenerator mockGenerator = mock(PowerDataGenerator.class);
        when(mockGenerator.generateFakeFriendCircleData(anyInt())).thenReturn(testData);
        
        // 调用loadData方法
        activitySpy.loadData(10);
        
        // 验证设置适配器数据
        verify(mockAdapter, times(1)).setFriendCircleBeans(any());
    }
    
    @Test
    public void testRefreshData() {
        // 模拟刷新操作
        activitySpy.refreshData();
        
        // 验证SwipeRefreshLayout的行为
        verify(mockSwipeRefreshLayout, times(1)).setRefreshing(true);
        
        // 验证数据加载
        verify(mockAdapter, times(1)).setFriendCircleBeans(any());
        
        // 验证结束刷新
        verify(mockSwipeRefreshLayout, times(1)).setRefreshing(false);
    }
    
    @Test
    public void testOnPraiseClick() {
        // 创建测试数据
        FriendCircleBean testBean = PowerTestUtil.createTestFriendCircleBean();
        int position = 1;
        
        // 调用点赞方法
        activitySpy.onPraiseClick(testBean, position);
        
        // 验证更新适配器
        verify(mockAdapter, times(1)).notifyItemChanged(position);
    }
    
    @Test
    public void testOnCommentClick() {
        // 创建测试数据
        FriendCircleBean testBean = PowerTestUtil.createTestFriendCircleBean();
        int position = 1;
        
        // 调用评论方法
        activitySpy.onCommentClick(testBean, position);
        
        // 验证显示评论UI(软键盘等)
        verify(activitySpy, times(1)).showCommentBox(eq(testBean), eq(position));
    }
    
    @Test
    public void testUpdateBatteryInfo() {
        // 创建模拟电池信息
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.getIntExtra(eq(BatteryManager.EXTRA_LEVEL), anyInt())).thenReturn(75);
        when(mockIntent.getIntExtra(eq(BatteryManager.EXTRA_SCALE), anyInt())).thenReturn(100);
        
        // 调用更新电池信息方法
        activitySpy.updateBatteryInfo(mockIntent);
        
        // 验证更新UI
        verify(mockBatteryTextView, times(1)).setText(eq("电量: 75%"));
        verify(mockBatteryImageView, times(1)).setImageLevel(eq(75));
    }
    
    @Test
    public void testStartBatteryMonitoring() {
        // 调用开始电池监控方法
        activitySpy.startBatteryMonitoring();
        
        // 验证注册广播接收器
        verify(activitySpy, times(1)).registerReceiver(any(), any());
    }
    
    @Test
    public void testStopBatteryMonitoring() {
        // 调用停止电池监控方法
        activitySpy.stopBatteryMonitoring();
        
        // 验证取消注册广播接收器
        verify(activitySpy, times(1)).unregisterReceiver(any());
    }
    
    @Test
    public void testOnDestroy() {
        // 执行onDestroy
        activityController.destroy();
        
        // 验证停止电池监控
        verify(activitySpy, times(1)).stopBatteryMonitoring();
    }
} 