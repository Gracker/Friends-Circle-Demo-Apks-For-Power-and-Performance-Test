package com.android.wechatfriendforpower.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

/**
 * PowerMonitorUtils工具类的单元测试
 */
public class PowerMonitorUtilsTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private Intent mockBatteryIntent;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 模拟电池信息Intent
        when(mockBatteryIntent.getIntExtra(eq(BatteryManager.EXTRA_LEVEL), anyInt())).thenReturn(75);
        when(mockBatteryIntent.getIntExtra(eq(BatteryManager.EXTRA_SCALE), anyInt())).thenReturn(100);
        when(mockBatteryIntent.getIntExtra(eq(BatteryManager.EXTRA_TEMPERATURE), anyInt())).thenReturn(320); // 32.0°C
        when(mockBatteryIntent.getIntExtra(eq(BatteryManager.EXTRA_VOLTAGE), anyInt())).thenReturn(4200); // 4.2V
        when(mockBatteryIntent.getIntExtra(eq(BatteryManager.EXTRA_HEALTH), anyInt())).thenReturn(BatteryManager.BATTERY_HEALTH_GOOD);
        when(mockBatteryIntent.getIntExtra(eq(BatteryManager.EXTRA_STATUS), anyInt())).thenReturn(BatteryManager.BATTERY_STATUS_CHARGING);
        
        // 模拟Context行为
        when(mockContext.registerReceiver(any(), any(IntentFilter.class))).thenReturn(mockBatteryIntent);
    }
    
    @Test
    public void testGetBatteryPercentage() {
        // 测试通过Intent获取电池百分比
        int percentage = PowerMonitorUtils.getBatteryPercentage(mockBatteryIntent);
        
        // 验证结果
        assertEquals(75, percentage);
    }
    
    @Test
    public void testGetBatteryPercentageWithNullIntent() {
        // 测试传入null的情况
        int percentage = PowerMonitorUtils.getBatteryPercentage(null);
        
        // 验证结果
        assertEquals(0, percentage);
    }
    
    @Test
    public void testGetBatteryTemperature() {
        // 测试获取电池温度
        float temperature = PowerMonitorUtils.getBatteryTemperature(mockBatteryIntent);
        
        // 验证结果
        assertEquals(32.0f, temperature, 0.01f);
    }
    
    @Test
    public void testGetBatteryVoltage() {
        // 测试获取电池电压
        float voltage = PowerMonitorUtils.getBatteryVoltage(mockBatteryIntent);
        
        // 验证结果
        assertEquals(4.2f, voltage, 0.01f);
    }
    
    @Test
    public void testIsBatteryCharging() {
        // 测试判断电池是否充电中
        boolean isCharging = PowerMonitorUtils.isBatteryCharging(mockBatteryIntent);
        
        // 验证结果
        assertTrue(isCharging);
    }
    
    @Test
    public void testGetBatteryHealthStatus() {
        // 测试获取电池健康状态
        String healthStatus = PowerMonitorUtils.getBatteryHealthStatus(mockBatteryIntent);
        
        // 验证结果
        assertEquals("Good", healthStatus);
    }
    
    @Test
    public void testGetBatteryHealthStatusWithUnknownValue() {
        // 修改模拟返回一个未知的健康状态
        when(mockBatteryIntent.getIntExtra(eq(BatteryManager.EXTRA_HEALTH), anyInt())).thenReturn(100);
        
        // 测试获取电池健康状态
        String healthStatus = PowerMonitorUtils.getBatteryHealthStatus(mockBatteryIntent);
        
        // 验证结果
        assertEquals("Unknown", healthStatus);
    }
    
    @Test
    public void testRegisterBatteryMonitor() {
        // 测试注册电池监控
        Intent resultIntent = PowerMonitorUtils.registerBatteryMonitor(mockContext);
        
        // 验证结果
        assertNotNull(resultIntent);
        verify(mockContext).registerReceiver(any(), any(IntentFilter.class));
    }
    
    @Test
    public void testCollectBatteryStatistics() {
        // 测试收集电池统计信息
        Map<String, Object> stats = PowerMonitorUtils.collectBatteryStatistics(mockBatteryIntent);
        
        // 验证结果
        assertNotNull(stats);
        assertEquals(75, stats.get("level"));
        assertEquals(32.0f, (float)stats.get("temperature"), 0.01f);
        assertEquals(4.2f, (float)stats.get("voltage"), 0.01f);
        assertEquals(true, stats.get("isCharging"));
        assertEquals("Good", stats.get("health"));
    }
    
    @Test
    public void testSaveBatteryStatsToLog() {
        // 准备测试数据
        Map<String, Object> stats = new HashMap<>();
        stats.put("level", 75);
        stats.put("temperature", 32.0f);
        stats.put("voltage", 4.2f);
        stats.put("isCharging", true);
        stats.put("health", "Good");
        stats.put("timestamp", System.currentTimeMillis());
        
        // 测试保存电池统计信息到日志
        boolean result = PowerMonitorUtils.saveBatteryStatsToLog(stats);
        
        // 验证结果 - 注意这里只能测试方法不抛异常，实际写入需要集成测试
        assertTrue(result);
    }
    
    @Test
    public void testCalculatePowerConsumption() {
        // 准备测试数据
        int startLevel = 100;
        int endLevel = 90;
        long startTime = System.currentTimeMillis() - 3600000; // 1小时前
        long endTime = System.currentTimeMillis();
        
        // 测试计算功耗
        float consumption = PowerMonitorUtils.calculatePowerConsumption(startLevel, endLevel, startTime, endTime);
        
        // 验证结果 - 1小时消耗10%电量，功耗率为10%/小时
        assertEquals(10.0f, consumption, 0.1f);
    }
} 