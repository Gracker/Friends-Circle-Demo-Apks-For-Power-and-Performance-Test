#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
滑动测试脚本
测试不同负载下的滑动性能，采集 FPS 数据
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
    ADBHelper, ConfigManager, FPSAnalyzer, ResultsManager,
    log_info, log_success, log_error, log_warning, wait_ms, TestStrategy
)

import random


class ScrollingTestRunner:
    """滑动测试执行器"""
    
    def __init__(self, config_dir: str, results_dir: str):
        self.config = ConfigManager(config_dir)
        self.results = ResultsManager(results_dir)
        self.screen_width, self.screen_height = ADBHelper.get_screen_size()
        
    def run_single_test(
        self,
        package: str,
        activity: str,
        app_name: str = "",
        load_type: Optional[str] = None,
        swipe_count: int = 20,
        swipe_duration_ms: int = 300,
        test_type: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        执行单次滑动测试

        Args:
            package: 应用包名
            activity: Activity 名称
            app_name: 应用名称（用于确定测试策略）
            load_type: 负载类型（如 minimal, medium, heavy 等）
            swipe_count: 滑动次数
            swipe_duration_ms: 单次滑动持续时间
            test_type: 注册表中的测试类型（如 vertical_swipe, horizontal_swipe）

        Returns:
            测试结果字典
        """
        # 确定测试策略
        strategy = TestStrategy.get_strategy_for_app(app_name, test_type)
        strategy_desc = TestStrategy.get_strategy_description(strategy)
        
        result = {
            "package": package,
            "app_name": app_name,
            "load_type": load_type or "default",
            "strategy": strategy,
            "swipe_count": swipe_count,
            "status": "pending",
            "fps_data": {}
        }
        
        # 1. 强制停止应用
        log_info(f"停止应用: {package}")
        ADBHelper.force_stop(package)
        wait_ms(1000)
        
        # 2. 启动应用
        log_info(f"启动应用: {package}/{activity}")
        log_info(f"测试策略: {strategy_desc}")
        extras = {"activity_type": load_type} if load_type else None
        if not ADBHelper.start_activity(package, activity, extras):
            log_error(f"启动失败: {package}")
            result["status"] = "launch_failed"
            return result
            
        # [优化] 进入页面后等待
        # wait_ms(1500) # Removed to capture early performance as per user feedback
        
        # 3. 等待页面稳定
        settle_time = self.config.test_config["scrolling_test"]["settle_time_before_test_ms"]
        log_info(f"等待页面稳定 {settle_time}ms...")
        wait_ms(settle_time)
        
        # 4. 重置 gfxinfo 统计
        log_info("重置 gfxinfo 统计...")
        ADBHelper.reset_gfxinfo(package)
        wait_ms(300)
        
        # 5. 根据策略执行测试
        if strategy == TestStrategy.DOUYIN_FLIP:
            self._execute_douyin_flip(swipe_count)
        elif strategy == TestStrategy.EBOOK_PAGE:
            self._execute_ebook_page(swipe_count)
        elif strategy == TestStrategy.MAP_DRAG:
            self._execute_map_drag(swipe_count)
        else:
            self._execute_standard_scroll(swipe_count, swipe_duration_ms)
        
        # 6. 等待渲染完成
        settle_after = self.config.test_config["scrolling_test"]["settle_time_after_test_ms"]
        log_info(f"等待渲染完成 {settle_after}ms...")
        wait_ms(settle_after)
        
        # 7. 采集 gfxinfo 数据
        log_info("采集 gfxinfo 数据...")
        gfxinfo_output = ADBHelper.get_gfxinfo(package)
        
        # 8. 解析数据
        fps_data = FPSAnalyzer.parse_gfxinfo(gfxinfo_output)
        
        # 9. 计算评分
        janky_thresholds = self.config.test_config["thresholds"]["janky_percent"]
        grade = FPSAnalyzer.calculate_grade(fps_data["janky_percent"], janky_thresholds)
        fps_data["grade"] = grade
        
        result["fps_data"] = fps_data
        result["status"] = "success"
        result["raw_gfxinfo"] = gfxinfo_output
        
        log_success(
            f"测试完成 - 帧数:{fps_data['total_frames']}, "
            f"Janky:{fps_data['janky_percent']:.1f}%, "
            f"评分:{grade}"
        )
        
        # 10. 测试结束清理
        log_info("测试完成，停止应用...")
        ADBHelper.force_stop(package)
        
        log_info("返回桌面...")
        ADBHelper.press_home()
        
        log_info("等待 1s...")
        wait_ms(1000)
        
        return result
    
    def _execute_standard_scroll(self, swipe_count: int, swipe_duration_ms: int):
        """标准滑动测试：上下交替滑动"""
        log_info(f"执行标准滑动测试 ({swipe_count} 次)...")
        
        start_y_ratio = self.config.test_config["scrolling_test"]["swipe_start_y_ratio"]
        end_y_ratio = self.config.test_config["scrolling_test"]["swipe_end_y_ratio"]
        
        center_x = self.screen_width // 2
        start_y = int(self.screen_height * start_y_ratio)
        end_y = int(self.screen_height * end_y_ratio)
        wait_after_swipe = self.config.test_config["device"]["wait_after_swipe_ms"]
        
        # 修改为连续上滑后连续下滑，模拟真实阅读习惯
        # 默认每组 5 次，或者总次数的一半
        block_size = 5
        
        for i in range(swipe_count):
            # 决定滑动方向：前 block_size 次上滑，后 block_size 次下滑，循环
            cycle_index = i % (block_size * 2)
            is_up_swipe = cycle_index < block_size

            if is_up_swipe:
                # 上滑 (从下往上)
                ADBHelper.swipe(center_x, start_y, center_x, end_y, swipe_duration_ms)
            else:
                # 下滑 (从上往下)
                ADBHelper.swipe(center_x, end_y, center_x, start_y, swipe_duration_ms)
            
            wait_ms(wait_after_swipe)
            if (i + 1) % 5 == 0:
                direction = "上滑" if is_up_swipe else "下滑"
                log_info(f"  滑动进度: {i + 1}/{swipe_count} ({direction})")
    
    def _execute_douyin_flip(self, page_count: int):
        """抖音翻页测试：全屏向上滑动切换视频"""
        log_info(f"执行抖音翻页测试 ({page_count} 次)...")
        
        center_x = self.screen_width // 2
        start_y = int(self.screen_height * 0.8)  # 从底部 20% 开始
        end_y = int(self.screen_height * 0.2)    # 滑到顶部 20%
        swipe_duration = 400  # 快速翻页
        
        for i in range(page_count):
            # 向上滑动翻页
            ADBHelper.swipe(center_x, start_y, center_x, end_y, swipe_duration)
            # 等待视频开始播放
            wait_ms(1000)
            if (i + 1) % 3 == 0:
                log_info(f"  翻页进度: {i + 1}/{page_count}")
    
    def _execute_ebook_page(self, page_count: int):
        """电子书翻页测试：点击右侧区域翻页"""
        log_info(f"执行电子书翻页测试 ({page_count} 次)...")
        
        # 点击右侧 1/3 区域翻页
        tap_x = int(self.screen_width * 0.85)
        tap_y = self.screen_height // 2
        
        for i in range(page_count):
            ADBHelper.tap(tap_x, tap_y)
            # 等待翻页动画
            wait_ms(350)
            if (i + 1) % 5 == 0:
                log_info(f"  翻页进度: {i + 1}/{page_count}")
    
    def _execute_map_drag(self, drag_count: int):
        """地图拖拽测试：随机方向拖拽"""
        log_info(f"执行地图拖拽测试 ({drag_count} 次)...")
        
        center_x = self.screen_width // 2
        center_y = self.screen_height // 2
        drag_distance = int(self.screen_width * 0.3)
        
        directions = [(0, -1), (0, 1), (-1, 0), (1, 0), (-1, -1), (1, 1)]  # 上下左右斜向
        
        for i in range(drag_count):
            dx, dy = random.choice(directions)
            end_x = center_x + dx * drag_distance
            end_y = center_y + dy * drag_distance
            ADBHelper.swipe(center_x, center_y, end_x, end_y, 500)
            wait_ms(300)
            if (i + 1) % 3 == 0:
                log_info(f"  拖拽进度: {i + 1}/{drag_count}")
    
    def run_iterations(
        self,
        package: str,
        activity: str,
        app_name: str = "",
        load_type: Optional[str] = None,
        swipe_count: int = 20,
        swipe_duration_ms: int = 300,
        warmup_runs: int = 1,
        iterations: int = 3,
        test_type: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        执行多次滑动测试（含预热）并返回聚合统计结果

        Args:
            package: 应用包名
            activity: Activity 名称
            app_name: 应用名称
            load_type: 负载类型
            swipe_count: 滑动次数
            swipe_duration_ms: 单次滑动持续时间
            warmup_runs: 预热次数（结果丢弃）
            iterations: 正式测试迭代次数
            test_type: 注册表中的测试类型

        Returns:
            聚合统计结果
        """
        # 预热阶段
        for i in range(warmup_runs):
            log_info(f"  预热 {i + 1}/{warmup_runs}...")
            self.run_single_test(
                package, activity, app_name, load_type,
                swipe_count, swipe_duration_ms, test_type=test_type
            )

        # 正式测试
        results = []
        for i in range(iterations):
            log_info(f"  迭代 {i + 1}/{iterations}...")
            result = self.run_single_test(
                package, activity, app_name, load_type,
                swipe_count, swipe_duration_ms, test_type=test_type
            )
            if result["status"] == "success":
                results.append(result)

        if not results:
            return {
                "status": "all_failed",
                "package": package,
                "app_name": app_name,
                "load_type": load_type or "default",
                "iterations": iterations,
                "successful": 0
            }

        # 聚合 FPS 统计
        janky_percents = [r["fps_data"]["janky_percent"] for r in results]
        p90s = [r["fps_data"]["percentile_90"] for r in results]
        p95s = [r["fps_data"]["percentile_95"] for r in results]
        p99s = [r["fps_data"]["percentile_99"] for r in results]
        avg_fps_list = [r["fps_data"]["avg_fps"] for r in results]

        stats = {
            "status": "completed",
            "package": package,
            "app_name": app_name,
            "load_type": load_type or "default",
            "warmup_runs": warmup_runs,
            "iterations": iterations,
            "successful": len(results),
            "janky_percent": {
                "avg": sum(janky_percents) / len(janky_percents),
                "min": min(janky_percents),
                "max": max(janky_percents),
                "values": janky_percents
            },
            "percentile_90": {
                "avg": sum(p90s) / len(p90s),
                "min": min(p90s),
                "max": max(p90s)
            },
            "percentile_95": {
                "avg": sum(p95s) / len(p95s),
                "min": min(p95s),
                "max": max(p95s)
            },
            "percentile_99": {
                "avg": sum(p99s) / len(p99s),
                "min": min(p99s),
                "max": max(p99s)
            },
            "avg_fps": {
                "avg": sum(avg_fps_list) / len(avg_fps_list),
                "min": min(avg_fps_list),
                "max": max(avg_fps_list)
            }
        }

        # 根据平均 janky_percent 计算评分
        janky_thresholds = self.config.test_config["thresholds"]["janky_percent"]
        stats["grade"] = FPSAnalyzer.calculate_grade(
            stats["janky_percent"]["avg"], janky_thresholds
        )

        return stats

    def run_app_test(
        self,
        app_config: Dict,
        load_types: Optional[List[str]] = None,
        swipe_count: int = 20
    ) -> List[Dict[str, Any]]:
        """
        测试单个应用的多种负载

        Args:
            app_config: 应用配置
            load_types: 要测试的负载类型列表
            swipe_count: 滑动次数

        Returns:
            测试结果列表
        """
        results = []
        package = app_config["package"]
        activity = app_config["activity"]
        app_name = app_config["name"]
        test_type = app_config.get("test_type")

        # 确定要测试的负载类型
        if app_config.get("supports_activity_type", False):
            available_types = app_config.get("load_types", [])
            if load_types:
                # 过滤出可用的类型
                types_to_test = [t for t in load_types if t in available_types]
            else:
                types_to_test = available_types
        else:
            types_to_test = [None]  # 不支持负载类型的应用只测试默认状态

        warmup_runs = self.config.test_config["scrolling_test"].get("warmup_runs", 1)
        iterations = self.config.test_config["scrolling_test"].get("iterations", 3)

        log_info(f"\n{'='*60}")
        log_info(f"开始测试应用: {app_name}")
        log_info(f"负载类型: {types_to_test}")
        log_info(f"预热: {warmup_runs}次, 迭代: {iterations}次")
        log_info(f"{'='*60}")

        for load_type in types_to_test:
            log_info(f"\n--- 测试负载: {load_type or 'default'} ---")
            stats = self.run_iterations(
                package=package,
                activity=activity,
                app_name=app_name,
                load_type=load_type,
                swipe_count=swipe_count,
                warmup_runs=warmup_runs,
                iterations=iterations,
                test_type=test_type
            )
            results.append(stats)

            # 保存原始数据
            self.results.save_raw_data(
                "scrolling",
                f"{app_name}_{load_type or 'default'}",
                stats
            )

        return results
    
    def run_full_test(
        self,
        app_names: Optional[List[str]] = None,
        load_types: Optional[List[str]] = None,
        swipe_count: int = 20
    ) -> Dict[str, Any]:
        """
        执行完整的滑动测试
        
        Args:
            app_names: 要测试的应用名称列表（None 表示全部）
            load_types: 要测试的负载类型列表（None 表示配置中的默认值）
            swipe_count: 滑动次数
        
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
        all_apps = self.config.get_scrolling_apps()
        
        if app_names:
            apps_to_test = [app for app in all_apps if app["name"] in app_names]
        else:
            # 使用配置中的测试应用列表
            test_app_names = self.config.test_config["scrolling_test"]["test_apps"]
            apps_to_test = [app for app in all_apps if app["name"] in test_app_names]
        
        # 使用配置中的负载类型（如果未指定）
        if load_types is None:
            load_types = self.config.test_config["scrolling_test"]["test_load_types"]
        
        if swipe_count == 0:
            swipe_count = self.config.test_config["scrolling_test"]["swipe_count"]
        
        log_info(f"准备测试 {len(apps_to_test)} 个应用, 负载类型: {load_types}")
        
        all_results = []

        for app_config in apps_to_test:
            app_results = self.run_app_test(app_config, load_types, swipe_count)
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
        summary_path = self.results.save_raw_data("scrolling_summary", "all", summary)
        log_success(f"\n测试完成！结果保存至: {summary_path}")

        # 打印汇总表格
        print("\n" + "=" * 80)
        print("滑动测试结果汇总")
        print("=" * 80)
        print(f"{'应用':<20} {'负载':<15} {'Janky%':<12} {'P95(ms)':<12} {'FPS':<10} {'评分':<6}")
        print("-" * 80)

        for r in all_results:
            if r["status"] == "completed":
                print(
                    f"{r['app_name']:<20} "
                    f"{r['load_type']:<15} "
                    f"{r['janky_percent']['avg']:<12.1f} "
                    f"{r['percentile_95']['avg']:<12.1f} "
                    f"{r['avg_fps']['avg']:<10.1f} "
                    f"{r['grade']:<6}"
                )

        print("=" * 80)

        return summary


def main():
    parser = argparse.ArgumentParser(description="滑动性能测试脚本")
    parser.add_argument(
        "--package",
        help="指定测试的应用包名（测试单个应用）"
    )
    parser.add_argument(
        "--load-type",
        help="指定负载类型 (minimal/light/medium/heavy/...)"
    )
    parser.add_argument(
        "--swipe-count",
        type=int,
        default=0,
        help="滑动次数（默认使用配置文件值）"
    )
    parser.add_argument(
        "--apps",
        nargs="+",
        help="指定要测试的应用名称列表"
    )
    parser.add_argument(
        "--load-types",
        nargs="+",
        help="指定要测试的负载类型列表"
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="测试所有应用和负载类型"
    )
    parser.add_argument("--results-dir", help="Results directory", default=None)
    
    args = parser.parse_args()
    
    # 设置路径
    LIB_DIR = Path(__file__).parent.absolute()
    config_dir = str(LIB_DIR.parent / "config")
    results_dir = args.results_dir or str(LIB_DIR.parent / "results")
    
    # 创建测试执行器
    runner = ScrollingTestRunner(config_dir, results_dir)
    
    if args.package:
        # 测试单个应用
        log_info(f"测试单个应用: {args.package}")
        
        # 查找应用配置
        all_apps = runner.config.get_scrolling_apps()
        app_config = None
        for app in all_apps:
            if app["package"] == args.package:
                app_config = app
                break
        
        if not app_config:
            log_error(f"未在注册表中找到应用: {args.package}")
            sys.exit(1)
        
        load_types = [args.load_type] if args.load_type else None
        results = runner.run_app_test(app_config, load_types, args.swipe_count or 20)
        if not results or any(r.get("status") != "completed" for r in results):
            sys.exit(1)
    else:
        # 批量测试
        app_names = args.apps
        load_types = args.load_types
        
        if args.all:
            app_names = None  # 测试全部
            load_types = None
        
        summary = runner.run_full_test(
            app_names=app_names,
            load_types=load_types,
            swipe_count=args.swipe_count
        )
        if summary.get("status") != "completed":
            sys.exit(1)
        if summary.get("total_tests", 0) == 0:
            sys.exit(1)
        if summary.get("successful_tests", 0) != summary.get("total_tests", 0):
            sys.exit(1)


if __name__ == "__main__":
    main()
