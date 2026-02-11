#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
启动测试脚本
测试不同技术栈和负载等级下的冷启动耗时
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


class LaunchTestRunner:
    """启动测试执行器"""
    
    def __init__(self, config_dir: str, results_dir: str):
        self.config = ConfigManager(config_dir)
        self.results = ResultsManager(results_dir)
        
    def run_single_launch_monitored(
        self,
        package: str,
        activity: str,
        clear_cache: bool = False,
        timeout: int = 30
    ) -> Dict[str, Any]:
        """
        执行单次冷启动测试，使用 logcat 监听真正的完成时间
        
        这种方式比 am start -W 更准确，因为它等待异步加载完成后
        应用输出的 LaunchPerformance 日志
        
        Args:
            package: 应用包名
            activity: Activity 名称
            clear_cache: 是否清除缓存
            timeout: 等待超时时间
        
        Returns:
            单次测试结果
        """
        # 1. 强制停止应用
        ADBHelper.force_stop(package)
        
        # [优化] 退出后等待
        wait_ms(1000 + self.config.test_config["launch_test"]["wait_after_force_stop_ms"])
        
        # 2. 可选：清除缓存
        if clear_cache:
            ADBHelper.clear_app_data(package)
            wait_ms(500)
        
        # 3. 清空 logcat
        LogcatMonitor.clear_logcat()
        wait_ms(100)
        
        # 4. 启动应用（不使用 -W）
        start_time = time.time()
        success = ADBHelper.start_activity(package, activity)
        
        if not success:
            return {
                "status": "failed",
                "total_time": 0,
                "logcat_duration": 0
            }
        
        # [优化] 进入页面后等待 (start_activity 内部已经完成了启动动作)
        
        # 5. 等待 logcat 中的完成日志
        found, logcat_duration = LogcatMonitor.wait_for_launch_complete(timeout)
        
        if found:
            log_info(f"    Logcat Duration: {logcat_duration}ms")
            
            # [优化] 页面加载完成后等待
            # wait_ms(1500) # 已经在 wait_for_launch_complete 后隐式等待了，这里不需要额外太久，但为了符合用户要求
            
            return {
                "status": "success",
                "total_time": logcat_duration,  # 使用应用报告的时间
                "logcat_duration": logcat_duration,
                "wait_time": 0
            }
        else:
            # 回退到时间戳计算
            elapsed = int((time.time() - start_time) * 1000)
            log_warning(f"    Logcat timeout, using elapsed time: {elapsed}ms")
            
            # [优化] 即使超时也等待一下
            wait_ms(1000)
            
            return {
                "status": "timeout",
                "total_time": elapsed,
                "logcat_duration": 0
            }
    
    def run_iterations(
        self,
        package: str,
        activity: str,
        iterations: int = 5,
        clear_cache: bool = False
    ) -> Dict[str, Any]:
        """
        执行多次启动测试并统计结果
        
        Args:
            package: 应用包名
            activity: Activity 名称
            iterations: 测试迭代次数
            clear_cache: 是否清除缓存
        
        Returns:
            统计结果
        """
        results = []
        successful = 0
        
        for i in range(iterations):
            log_info(f"  第 {i + 1}/{iterations} 次启动...")
            
            # 1. 强制停止应用
            ADBHelper.force_stop(package)
            
            # [优化] 退出后等待
            # log_info("应用已停止，等待 1s...") # reduce log noise
            wait_ms(1000 + self.config.test_config["launch_test"]["wait_after_force_stop_ms"])
            
            # 使用 Logcat 监控方式，更准确且能确认应用真正启动完成
            result = self.run_single_launch_monitored(package, activity, clear_cache)
            
            # [优化] 测试完成后清理
            log_info(f"    清理环境...")
            ADBHelper.force_stop(package)
            ADBHelper.press_home()
            wait_ms(1500)
            
            results.append(result)
            
            if result["status"] == "success":
                successful += 1
                log_info(f"    TotalTime: {result['total_time']}ms (Logcat Verified)")
            elif result["status"] == "timeout":
                # 超时也算作一次有效数据（虽然是超时），或者可以根据策略丢弃
                # 这里为了严谨，如果没抓到日志，可能意味着 Crash 或者卡死
                log_error(f"    测试超时或失败: {result.get('total_time', 0)}ms")
            else:
                log_error(f"    启动失败")
        
        # 计算统计数据
        successful_results = [r for r in results if r["status"] == "success"]
        
        if not successful_results:
            return {
                "status": "all_failed",
                "iterations": iterations,
                "successful": 0
            }
        
        total_times = [r["total_time"] for r in successful_results]
        wait_times = [r["wait_time"] for r in successful_results]
        
        # 计算平均值、最小值、最大值
        stats = {
            "status": "completed",
            "iterations": iterations,
            "successful": successful,
            "total_time": {
                "avg": sum(total_times) / len(total_times),
                "min": min(total_times),
                "max": max(total_times),
                "values": total_times
            },
            "wait_time": {
                "avg": sum(wait_times) / len(wait_times),
                "min": min(wait_times),
                "max": max(wait_times),
                "values": wait_times
            }
        }
        
        # 计算评分
        avg_time = stats["total_time"]["avg"]
        thresholds = self.config.test_config["thresholds"]["launch_time_ms"]
        
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
        flavors: Optional[List[str]] = None,
        iterations: int = 5
    ) -> List[Dict[str, Any]]:
        """
        测试单个应用的多个 flavor
        
        Args:
            app_config: 应用配置
            flavors: 要测试的 flavor 列表
            iterations: 迭代次数
        
        Returns:
            测试结果列表
        """
        results = []
        app_name = app_config["name"]
        activity = app_config["activity"]
        
        log_info(f"\n{'='*60}")
        log_info(f"开始启动测试: {app_name}")
        log_info(f"{'='*60}")
        
        # 确定要测试的 flavor
        all_flavors = app_config.get("flavors", [])
        
        if flavors:
            flavors_to_test = [f for f in all_flavors if f["name"] in flavors]
        else:
            flavors_to_test = all_flavors
        
        clear_cache = self.config.test_config["launch_test"]["clear_cache_before_test"]
        
        for flavor in flavors_to_test:
            flavor_name = flavor["name"]
            package = flavor["package"]
            
            log_info(f"\n--- 测试 flavor: {flavor_name} ---")
            log_info(f"    包名: {package}")
            
            stats = self.run_iterations(package, activity, iterations, clear_cache)
            
            stats["app_name"] = app_name
            stats["flavor"] = flavor_name
            stats["package"] = package
            
            if stats["status"] == "completed":
                log_success(
                    f"  完成 - 平均启动时间: {stats['total_time']['avg']:.0f}ms, "
                    f"评分: {stats['grade']}"
                )
            else:
                log_error(f"  测试失败")
            
            results.append(stats)
            
            # 保存数据
            self.results.save_raw_data("launch", f"{app_name}_{flavor_name}", stats)
        
        return results
    
    def run_full_test(
        self,
        app_names: Optional[List[str]] = None,
        flavors: Optional[List[str]] = None,
        iterations: int = 0
    ) -> Dict[str, Any]:
        """
        执行完整的启动测试
        
        Args:
            app_names: 要测试的应用名称列表
            flavors: 要测试的 flavor 列表
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
        all_apps = self.config.get_launch_apps()
        
        if app_names:
            apps_to_test = [app for app in all_apps if app["name"] in app_names]
        else:
            test_app_names = self.config.test_config["launch_test"]["test_apps"]
            apps_to_test = [app for app in all_apps if app["name"] in test_app_names]
        
        # 使用配置中的 flavor（如果未指定）
        if flavors is None:
            flavors = self.config.test_config["launch_test"]["test_flavors"]
        
        # 使用配置中的迭代次数（如果未指定）
        if iterations == 0:
            iterations = self.config.test_config["launch_test"]["iterations"]
        
        log_info(f"准备测试 {len(apps_to_test)} 个应用, flavors: {flavors}, 迭代: {iterations}次")
        
        all_results = []
        
        for app_config in apps_to_test:
            app_results = self.run_app_test(app_config, flavors, iterations)
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
        summary_path = self.results.save_raw_data("launch_summary", "all", summary)
        log_success(f"\n测试完成！结果保存至: {summary_path}")
        
        # 打印汇总表格
        print("\n" + "="*70)
        print("启动测试结果汇总")
        print("="*70)
        print(f"{'应用':<20} {'Flavor':<10} {'平均(ms)':<12} {'最小':<10} {'最大':<10} {'评分':<6}")
        print("-"*70)
        
        for r in all_results:
            if r["status"] == "completed":
                print(
                    f"{r['app_name']:<20} "
                    f"{r['flavor']:<10} "
                    f"{r['total_time']['avg']:<12.0f} "
                    f"{r['total_time']['min']:<10} "
                    f"{r['total_time']['max']:<10} "
                    f"{r['grade']:<6}"
                )
        
        print("="*70)
        
        return summary


def main():
    parser = argparse.ArgumentParser(description="启动性能测试脚本")
    parser.add_argument(
        "--package",
        help="指定测试的应用包名（测试单个应用）"
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
        "--flavors",
        nargs="+",
        help="指定要测试的 flavor 列表 (light/medium/heavy)"
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="测试所有应用和 flavor"
    )
    parser.add_argument("--results-dir", help="Results directory", default=None)
    
    args = parser.parse_args()
    
    # 设置路径
    LIB_DIR = Path(__file__).parent.absolute()
    config_dir = str(LIB_DIR.parent / "config")
    results_dir = args.results_dir or str(LIB_DIR.parent / "results")
    
    # 创建测试执行器
    runner = LaunchTestRunner(config_dir, results_dir)
    
    if args.package:
        # 测试单个包
        log_info(f"测试单个应用: {args.package}")
        iterations = args.iterations or 5
        stats = runner.run_iterations(
            args.package,
            ".MainActivity",
            iterations
        )
        print(json.dumps(stats, indent=2))
        if stats.get("status") != "completed":
            sys.exit(1)
    else:
        # 批量测试
        app_names = args.apps
        flavors = args.flavors
        
        if args.all:
            app_names = None
            flavors = None
        
        summary = runner.run_full_test(
            app_names=app_names,
            flavors=flavors,
            iterations=args.iterations
        )
        if summary.get("status") != "completed":
            sys.exit(1)
        if summary.get("total_tests", 0) == 0:
            sys.exit(1)
        if summary.get("successful_tests", 0) != summary.get("total_tests", 0):
            sys.exit(1)


if __name__ == "__main__":
    main()
