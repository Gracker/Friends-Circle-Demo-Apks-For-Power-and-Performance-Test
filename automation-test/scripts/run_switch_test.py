#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Switch 跳转测试脚本
测试应用内 Activity 跳转性能
"""

import argparse
import json
import sys
import time
from pathlib import Path
from typing import Dict, List, Any, Optional

# 添加 lib 目录到 path
SCRIPT_DIR = Path(__file__).parent.absolute()
LIB_DIR = SCRIPT_DIR.parent / "lib"
sys.path.insert(0, str(LIB_DIR))

from utils import (
    ADBHelper, ConfigManager, ResultsManager,
    log_info, log_success, log_error, log_warning, wait_ms, LogcatMonitor
)


# Switch 测试按钮的屏幕位置映射（相对于屏幕尺寸的比例）
# 这些值需要根据实际 UI 布局调整
BUTTON_POSITIONS = {
    "pure":         {"row": 0, "col": 0},
    "self_light":   {"row": 0, "col": 1},
    "self_medium":  {"row": 1, "col": 0},
    "self_heavy":   {"row": 1, "col": 1},
    "bg_medium":    {"row": 2, "col": 0},
    "bg_heavy":     {"row": 2, "col": 1},
    "light_med":    {"row": 3, "col": 0},
    "med_med":      {"row": 3, "col": 1},
    "heavy_med":    {"row": 4, "col": 0},
    "heavy_heavy":  {"row": 4, "col": 1},
}


class SwitchTestRunner:
    """Switch 测试执行器"""
    
    def __init__(self, config_dir: str, results_dir: str):
        self.config = ConfigManager(config_dir)
        self.results = ResultsManager(results_dir)
        self.screen_width, self.screen_height = ADBHelper.get_screen_size()
        
        # 根据屏幕尺寸计算按钮位置
        self._calculate_button_coords()
    
    def _calculate_button_coords(self):
        """计算按钮坐标"""
        # 假设按钮区域从屏幕高度的 30% 开始，到 90% 结束
        # 分为 5 行 2 列
        button_area_top = int(self.screen_height * 0.30)
        button_area_bottom = int(self.screen_height * 0.90)
        button_area_height = button_area_bottom - button_area_top
        
        row_height = button_area_height // 5
        col_width = self.screen_width // 2
        
        self.button_coords = {}
        for name, pos in BUTTON_POSITIONS.items():
            x = int(col_width * (pos["col"] + 0.5))
            y = int(button_area_top + row_height * (pos["row"] + 0.5))
            self.button_coords[name] = (x, y)
    
    def run_single_switch(
        self,
        package: str,
        activity: str,
        load_combination: str
    ) -> Dict[str, Any]:
        """
        执行单次跳转测试
        
        使用 am start -W 的时间戳来测量跳转耗时
        
        Args:
            package: 应用包名
            activity: 主 Activity 名称
            load_combination: 负载组合名称
        
        Returns:
            测试结果
        """
        result = {
            "load_combination": load_combination,
            "status": "pending",
            "switch_time": 0
        }
        
        # 获取按钮坐标
        if load_combination not in self.button_coords:
            log_warning(f"未知的负载组合: {load_combination}")
            result["status"] = "unknown_combination"
            return result
        
        button_x, button_y = self.button_coords[load_combination]
        
        # 1. 确保应用已启动并在主界面
        ADBHelper.force_stop(package)
        wait_ms(500)
        
        if not ADBHelper.start_activity(package, activity):
            result["status"] = "launch_failed"
            return result
        
        # [优化] 进入页面后等待
        log_info("页面加载中，等待 1.5s...")
        wait_ms(1500)
        
        wait_ms(self.config.test_config["switch_test"]["wait_after_main_launch_ms"])
        
        # 2. 记录开始时间
        start_time = time.time()
        
        # 3. 点击按钮
        log_info(f"  点击按钮 ({button_x}, {button_y})...")
        ADBHelper.tap(button_x, button_y)
        
        # 4. 等待目标 Activity 加载
        wait_ms(self.config.test_config["switch_test"]["wait_after_switch_ms"]) # Configured wait usually covers transition
        
        # [优化] 页面跳转后等待
        wait_ms(1000)
        
        # 5. 计算耗时
        end_time = time.time()
        switch_time_ms = int((end_time - start_time) * 1000)
        
        # 减去等待时间得到实际感知时间
        # 注意：这是一个简化的测量方法，实际可能需要更精确的方案
        actual_switch_time = switch_time_ms - self.config.test_config["switch_test"]["wait_after_switch_ms"]
        
        result["status"] = "success"
        result["switch_time"] = max(0, actual_switch_time)
        result["raw_time"] = switch_time_ms
        
        # 6. 测试结束清理
        ADBHelper.force_stop(package)
        ADBHelper.press_home()
        
        # [优化] 退出后等待
        log_info("返回桌面，等待 1s...")
        wait_ms(1000)
        
        return result
    
    def run_single_switch_monitored(
        self,
        package: str,
        activity: str,
        load_combination: str,
        timeout: int = 30
    ) -> Dict[str, Any]:
        """
        执行单次跳转测试，使用 logcat 监听真正的完成时间
        
        监听 RealLoadExecutor 输出的 SWITCH_ALL_TASKS_COMPLETE 日志
        这种方式比简单计时更准确，因为它等待所有延迟任务完成
        
        Args:
            package: 应用包名
            activity: 主 Activity 名称
            load_combination: 负载组合名称
            timeout: 等待超时时间
        
        Returns:
            测试结果
        """
        result = {
            "load_combination": load_combination,
            "status": "pending",
            "switch_time": 0
        }
        
        # 获取按钮坐标
        if load_combination not in self.button_coords:
            log_warning(f"未知的负载组合: {load_combination}")
            result["status"] = "unknown_combination"
            return result
        
        button_x, button_y = self.button_coords[load_combination]
        
        # 1. 确保应用已启动并在主界面
        ADBHelper.force_stop(package)
        wait_ms(500)
        
        if not ADBHelper.start_activity(package, activity):
            result["status"] = "launch_failed"
            return result
        
        # [优化] 进入页面后等待
        log_info("页面加载中，等待 1.5s...")
        wait_ms(1500)
        
        wait_ms(self.config.test_config["switch_test"]["wait_after_main_launch_ms"])
        
        # 2. 清空 logcat
        LogcatMonitor.clear_logcat()
        wait_ms(100)
        
        # 3. 点击按钮
        log_info(f"  点击按钮 ({button_x}, {button_y})...")
        ADBHelper.tap(button_x, button_y)
        
        # 4. 等待 logcat 中的完成日志
        found, logcat_duration = LogcatMonitor.wait_for_switch_complete(timeout)
        
        if found:
            log_info(f"    Logcat Duration: {logcat_duration}ms")
            result["status"] = "success"
            result["switch_time"] = logcat_duration
            result["logcat_duration"] = logcat_duration
            
            # [优化] 跳转完成后等待
            wait_ms(1000)
        else:
            log_warning(f"    Logcat timeout")
            result["status"] = "timeout"
            result["switch_time"] = 0
        
        # 5. 测试结束清理
        ADBHelper.force_stop(package)
        ADBHelper.press_home()
        
        # [优化] 退出后等待
        log_info("返回桌面，等待 1s...")
        wait_ms(1000)
        
        return result
    
    def run_iterations(
        self,
        package: str,
        activity: str,
        load_combination: str,
        iterations: int = 3
    ) -> Dict[str, Any]:
        """
        执行多次跳转测试并统计
        
        Args:
            package: 应用包名
            activity: Activity 名称
            load_combination: 负载组合
            iterations: 迭代次数
        
        Returns:
            统计结果
        """
        results = []
        successful = 0
        
        for i in range(iterations):
            log_info(f"  第 {i + 1}/{iterations} 次跳转...")
            # [Fix] 强制使用 Logcat 监控模式，因为用户强调必须等待任务真正完成
            result = self.run_single_switch_monitored(package, activity, load_combination)
            results.append(result)
            
            if result["status"] == "success":
                successful += 1
                log_info(f"    跳转耗时: {result['switch_time']}ms")
        
        # 测试结束后强制停止应用，确保环境清理
        ADBHelper.force_stop(package)
        
        # [优化] 退出后等待
        wait_ms(1000)
        
        # 计算统计数据
        successful_results = [r for r in results if r["status"] == "success"]
        
        if not successful_results:
            return {
                "status": "all_failed",
                "load_combination": load_combination,
                "iterations": iterations,
                "successful": 0
            }
        
        switch_times = [r["switch_time"] for r in successful_results]
        
        stats = {
            "status": "completed",
            "load_combination": load_combination,
            "iterations": iterations,
            "successful": successful,
            "switch_time": {
                "avg": sum(switch_times) / len(switch_times),
                "min": min(switch_times),
                "max": max(switch_times),
                "values": switch_times
            }
        }
        
        # 计算评分
        avg_time = stats["switch_time"]["avg"]
        thresholds = self.config.test_config["thresholds"]["switch_time_ms"]
        
        if avg_time <= thresholds["excellent"]:
            stats["grade"] = "A+"
        elif avg_time <= thresholds["good"]:
            stats["grade"] = "A"
        elif avg_time <= thresholds["acceptable"]:
            stats["grade"] = "B"
        elif avg_time <= thresholds["poor"]:
            stats["grade"] = "C"
        else:
            stats["grade"] = "D"
        
        return stats
    
    def run_app_test(
        self,
        app_config: Dict,
        load_combinations: Optional[List[str]] = None,
        iterations: int = 3
    ) -> List[Dict[str, Any]]:
        """
        测试单个应用的多种负载组合
        
        Args:
            app_config: 应用配置
            load_combinations: 要测试的负载组合列表
            iterations: 迭代次数
        
        Returns:
            测试结果列表
        """
        results = []
        app_name = app_config["name"]
        package = app_config["package"]
        activity = app_config["activity"]
        
        log_info(f"\n{'='*60}")
        log_info(f"开始 Switch 测试: {app_name}")
        log_info(f"{'='*60}")
        
        # 确定要测试的负载组合
        all_combinations = [c["name"] for c in app_config.get("load_combinations", [])]
        
        if load_combinations:
            combinations_to_test = [c for c in load_combinations if c in all_combinations]
        else:
            combinations_to_test = all_combinations
        
        for combination in combinations_to_test:
            log_info(f"\n--- 测试组合: {combination} ---")
            
            stats = self.run_iterations(package, activity, combination, iterations)
            stats["app_name"] = app_name
            stats["package"] = package
            
            if stats["status"] == "completed":
                log_success(
                    f"  完成 - 平均跳转时间: {stats['switch_time']['avg']:.0f}ms, "
                    f"评分: {stats['grade']}"
                )
            else:
                log_error(f"  测试失败")
            
            results.append(stats)
            
            # 保存数据
            self.results.save_raw_data("switch", f"{app_name}_{combination}", stats)
        
        # 停止应用
        ADBHelper.force_stop(package)
        
        return results
    
    def run_full_test(
        self,
        app_names: Optional[List[str]] = None,
        load_combinations: Optional[List[str]] = None,
        iterations: int = 0
    ) -> Dict[str, Any]:
        """
        执行完整的 Switch 测试
        
        Args:
            app_names: 要测试的应用名称列表
            load_combinations: 要测试的负载组合列表
            iterations: 迭代次数（0 表示使用配置值）
        
        Returns:
            完整测试结果
        """
        # 检查设备连接
        if not ADBHelper.is_device_connected():
            log_error("没有检测到已连接的设备！")
            return {"status": "no_device", "results": []}
        
        device_info = ADBHelper.get_device_info()
        log_info(f"设备: {device_info['brand']} {device_info['model']} (Android {device_info['android_version']})")
        
        # 获取要测试的应用列表
        all_apps = self.config.get_switch_apps()
        
        if app_names:
            apps_to_test = [app for app in all_apps if app["name"] in app_names]
        else:
            test_app_names = self.config.test_config["switch_test"]["test_apps"]
            apps_to_test = [app for app in all_apps if app["name"] in test_app_names]
        
        # 使用配置中的组合（如果未指定）
        if load_combinations is None:
            load_combinations = self.config.test_config["switch_test"]["test_load_combinations"]
        
        # 使用配置中的迭代次数
        if iterations == 0:
            iterations = self.config.test_config["switch_test"]["iterations"]
        
        log_info(f"准备测试 {len(apps_to_test)} 个应用, 组合: {load_combinations}, 迭代: {iterations}次")
        
        all_results = []
        
        for app_config in apps_to_test:
            app_results = self.run_app_test(app_config, load_combinations, iterations)
            all_results.extend(app_results)
        
        # 汇总结果
        summary = {
            "status": "completed",
            "device_info": device_info,
            "test_time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "total_tests": len(all_results),
            "successful_tests": sum(1 for r in all_results if r["status"] == "completed"),
            "results": all_results
        }
        
        # 保存汇总结果
        summary_path = self.results.save_raw_data("switch_summary", "all", summary)
        log_success(f"\n测试完成！结果保存至: {summary_path}")
        
        # 打印汇总表格
        print("\n" + "="*70)
        print("Switch 测试结果汇总")
        print("="*70)
        print(f"{'应用':<18} {'组合':<15} {'平均(ms)':<12} {'最小':<10} {'最大':<10} {'评分':<6}")
        print("-"*70)
        
        for r in all_results:
            if r["status"] == "completed":
                print(
                    f"{r['app_name']:<18} "
                    f"{r['load_combination']:<15} "
                    f"{r['switch_time']['avg']:<12.0f} "
                    f"{r['switch_time']['min']:<10} "
                    f"{r['switch_time']['max']:<10} "
                    f"{r['grade']:<6}"
                )
        
        print("="*70)
        
        return summary


def main():
    parser = argparse.ArgumentParser(description="Switch 跳转性能测试脚本")
    parser.add_argument(
        "--package",
        help="指定测试的应用包名"
    )
    parser.add_argument(
        "--iterations",
        type=int,
        default=0,
        help="迭代次数（默认使用配置文件值）"
    )
    parser.add_argument(
        "--apps",
        nargs="+",
        help="指定要测试的应用名称列表"
    )
    parser.add_argument(
        "--combinations",
        nargs="+",
        help="指定要测试的负载组合列表"
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="测试所有应用和组合"
    )
    parser.add_argument("--results-dir", help="Results directory", default=None)
    
    args = parser.parse_args()
    
    # 设置路径
    script_dir = Path(__file__).parent.absolute()
    project_dir = script_dir.parent
    config_dir = str(project_dir / "config")
    results_dir = args.results_dir or str(project_dir / "results")
    
    # 创建测试执行器
    runner = SwitchTestRunner(config_dir, results_dir)
    
    # 批量测试
    app_names = args.apps
    combinations = args.combinations
    
    if args.all:
        app_names = None
        combinations = None
    
    runner.run_full_test(
        app_names=app_names,
        load_combinations=combinations,
        iterations=args.iterations
    )


if __name__ == "__main__":
    main()
