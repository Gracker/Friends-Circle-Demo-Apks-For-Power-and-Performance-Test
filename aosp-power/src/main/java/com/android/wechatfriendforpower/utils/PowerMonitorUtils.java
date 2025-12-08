package com.android.wechatfriendforpower.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 电量监控工具类
 */
public class PowerMonitorUtils {
    
    private static final String TAG = "PowerMonitor";
    
    /**
     * 获取电池百分比
     * @param batteryIntent 电池状态Intent
     * @return 电池百分比
     */
    public static int getBatteryPercentage(Intent batteryIntent) {
        if (batteryIntent == null) {
            return 0;
        }
        
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return (int) ((level / (float) scale) * 100);
    }
    
    /**
     * 获取电池温度
     * @param batteryIntent 电池状态Intent
     * @return 电池温度（摄氏度）
     */
    public static float getBatteryTemperature(Intent batteryIntent) {
        if (batteryIntent == null) {
            return 0;
        }
        
        int temp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        return temp / 10.0f; // 转换为摄氏度
    }
    
    /**
     * 获取电池电压
     * @param batteryIntent 电池状态Intent
     * @return 电池电压（伏特）
     */
    public static float getBatteryVoltage(Intent batteryIntent) {
        if (batteryIntent == null) {
            return 0;
        }
        
        int voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        return voltage / 1000.0f; // 转换为伏特
    }
    
    /**
     * 判断电池是否正在充电
     * @param batteryIntent 电池状态Intent
     * @return 是否正在充电
     */
    public static boolean isBatteryCharging(Intent batteryIntent) {
        if (batteryIntent == null) {
            return false;
        }
        
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING || 
               status == BatteryManager.BATTERY_STATUS_FULL;
    }
    
    /**
     * 获取电池健康状态
     * @param batteryIntent 电池状态Intent
     * @return 电池健康状态描述
     */
    public static String getBatteryHealthStatus(Intent batteryIntent) {
        if (batteryIntent == null) {
            return "Unknown";
        }
        
        int health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Overheated";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "Unspecified Failure";
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Cold";
            default:
                return "Unknown";
        }
    }
    
    /**
     * 注册电池监控
     * @param context 上下文
     * @return 电池状态Intent
     */
    public static Intent registerBatteryMonitor(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        return context.registerReceiver(null, filter);
    }
    
    /**
     * 收集电池统计信息
     * @param batteryIntent 电池状态Intent
     * @return 电池统计信息Map
     */
    public static Map<String, Object> collectBatteryStatistics(Intent batteryIntent) {
        Map<String, Object> stats = new HashMap<>();
        
        if (batteryIntent == null) {
            return stats;
        }
        
        stats.put("level", getBatteryPercentage(batteryIntent));
        stats.put("temperature", getBatteryTemperature(batteryIntent));
        stats.put("voltage", getBatteryVoltage(batteryIntent));
        stats.put("isCharging", isBatteryCharging(batteryIntent));
        stats.put("health", getBatteryHealthStatus(batteryIntent));
        stats.put("timestamp", System.currentTimeMillis());
        
        return stats;
    }
    
    /**
     * 保存电池统计信息到日志
     * @param stats 电池统计信息Map
     * @return 是否保存成功
     */
    public static boolean saveBatteryStatsToLog(Map<String, Object> stats) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Battery Stats: ");
            
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
            }
            
            Log.i(TAG, sb.toString());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving battery stats", e);
            return false;
        }
    }
    
    /**
     * 计算功耗
     * @param startLevel 开始电量百分比
     * @param endLevel 结束电量百分比
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 每小时功耗百分比
     */
    public static float calculatePowerConsumption(int startLevel, int endLevel, long startTime, long endTime) {
        int consumedPercentage = startLevel - endLevel;
        long durationMs = endTime - startTime;
        
        if (durationMs <= 0 || consumedPercentage < 0) {
            return 0;
        }
        
        // 转换为每小时消耗的百分比
        float hours = durationMs / (1000f * 60 * 60);
        return consumedPercentage / hours;
    }
} 