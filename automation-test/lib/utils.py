#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
通用工具库
提供 ADB 操作、配置读取、数据处理等通用功能
"""

import shlex
import subprocess
import json
import os
import re
import time
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, List, Any, Tuple


class ADBHelper:
    """ADB 命令封装"""
    
    @staticmethod
    def run_command(command: str, timeout: int = 30) -> Tuple[bool, str]:
        """
        执行 ADB 命令
        
        Args:
            command: ADB 命令（不包含 'adb' 前缀）
            timeout: 超时时间（秒）
        
        Returns:
            (成功标志, 输出内容)
        """
        try:
            args = ["adb"] + shlex.split(command)
            result = subprocess.run(
                args,
                capture_output=True,
                text=True,
                timeout=timeout
            )
            output = result.stdout.strip()
            if result.returncode == 0:
                return True, output
            else:
                return False, result.stderr.strip()
        except subprocess.TimeoutExpired:
            return False, "Command timeout"
        except Exception as e:
            return False, str(e)
    
    @staticmethod
    def is_device_connected() -> bool:
        """检查是否有设备连接"""
        success, output = ADBHelper.run_command("devices")
        if not success:
            return False
        lines = output.strip().split('\n')
        # 跳过第一行 "List of devices attached"
        for line in lines[1:]:
            if '\tdevice' in line:
                return True
        return False
    
    @staticmethod
    def get_device_info() -> Dict[str, str]:
        """获取设备信息"""
        info = {}
        props = [
            ("brand", "ro.product.brand"),
            ("model", "ro.product.model"),
            ("device", "ro.product.device"),
            ("android_version", "ro.build.version.release"),
            ("sdk_version", "ro.build.version.sdk"),
            ("build_id", "ro.build.id"),
        ]
        for key, prop in props:
            success, value = ADBHelper.run_command(f"shell getprop {prop}")
            info[key] = value if success else "unknown"
        return info
    
    @staticmethod
    def start_activity(package: str, activity: str, extras: Optional[Dict[str, str]] = None) -> bool:
        """
        启动 Activity
        
        Args:
            package: 包名
            activity: Activity 名称
            extras: Intent extras (key-value 字符串)
        
        Returns:
            是否成功
        """
        cmd = f"shell am start -n {package}/{activity}"
        if extras:
            for key, value in extras.items():
                cmd += f" --es {key} {value}"
        success, _ = ADBHelper.run_command(cmd)
        return success
    
    @staticmethod
    def start_activity_with_timing(package: str, activity: str) -> Tuple[bool, int, int]:
        """
        启动 Activity 并获取启动耗时
        
        Returns:
            (成功标志, TotalTime, WaitTime)
        """
        cmd = f"shell am start -W -n {package}/{activity}"
        success, output = ADBHelper.run_command(cmd, timeout=60)
        
        if not success:
            return False, 0, 0
        
        total_time = 0
        wait_time = 0
        
        # 解析输出中的 TotalTime 和 WaitTime
        for line in output.split('\n'):
            if 'TotalTime:' in line:
                match = re.search(r'TotalTime:\s*(\d+)', line)
                if match:
                    total_time = int(match.group(1))
            elif 'WaitTime:' in line:
                match = re.search(r'WaitTime:\s*(\d+)', line)
                if match:
                    wait_time = int(match.group(1))
        
        return True, total_time, wait_time
    
    @staticmethod
    def force_stop(package: str) -> bool:
        """强制停止应用"""
        success, _ = ADBHelper.run_command(f"shell am force-stop {package}")
        return success
    
    @staticmethod
    def clear_app_data(package: str) -> bool:
        """清除应用数据"""
        success, _ = ADBHelper.run_command(f"shell pm clear {package}")
        return success
    
    @staticmethod
    def swipe(x1: int, y1: int, x2: int, y2: int, duration_ms: int = 300) -> bool:
        """
        执行滑动操作
        
        Args:
            x1, y1: 起始坐标
            x2, y2: 结束坐标
            duration_ms: 滑动持续时间
        """
        cmd = f"shell input swipe {x1} {y1} {x2} {y2} {duration_ms}"
        success, _ = ADBHelper.run_command(cmd)
        return success
    
    @staticmethod
    def tap(x: int, y: int) -> bool:
        """点击屏幕"""
        cmd = f"shell input tap {x} {y}"
        success, _ = ADBHelper.run_command(cmd)
        return success
    
    @staticmethod
    def press_back() -> bool:
        """按返回键"""
        success, _ = ADBHelper.run_command("shell input keyevent KEYCODE_BACK")
        return success
    
    @staticmethod
    def press_home() -> bool:
        """按 Home 键"""
        success, _ = ADBHelper.run_command("shell input keyevent KEYCODE_HOME")
        return success
    
    @staticmethod
    def get_screen_size() -> Tuple[int, int]:
        """获取屏幕尺寸"""
        success, output = ADBHelper.run_command("shell wm size")
        if success:
            match = re.search(r'(\d+)x(\d+)', output)
            if match:
                return int(match.group(1)), int(match.group(2))
        return 1080, 2400  # 默认尺寸
    
    @staticmethod
    def get_gfxinfo(package: str) -> str:
        """获取 gfxinfo 数据 (包含 framestats)"""
        success, output = ADBHelper.run_command(f"shell dumpsys gfxinfo {package} framestats")
        return output if success else ""
    
    @staticmethod
    def reset_gfxinfo(package: str) -> bool:
        """重置 gfxinfo 统计"""
        success, _ = ADBHelper.run_command(f"shell dumpsys gfxinfo {package} reset")
        return success


class ConfigManager:
    """配置管理器"""
    
    def __init__(self, config_dir: str):
        self.config_dir = Path(config_dir)
        self._test_config: Optional[Dict] = None
        self._apk_registry: Optional[Dict] = None
    
    @property
    def test_config(self) -> Dict:
        """获取测试配置"""
        if self._test_config is None:
            config_path = self.config_dir / "test_config.json"
            with open(config_path, 'r', encoding='utf-8') as f:
                self._test_config = json.load(f)
        return self._test_config
    
    @property
    def apk_registry(self) -> Dict:
        """获取 APK 注册表"""
        if self._apk_registry is None:
            registry_path = self.config_dir / "apk_registry.json"
            with open(registry_path, 'r', encoding='utf-8') as f:
                self._apk_registry = json.load(f)
        return self._apk_registry
    
    def get_scrolling_apps(self) -> List[Dict]:
        """获取滑动测试应用列表"""
        return self.apk_registry.get("scrolling_tests", {}).get("apps", [])
    
    def get_launch_apps(self) -> List[Dict]:
        """获取启动测试应用列表"""
        return self.apk_registry.get("launch_tests", {}).get("apps", [])
    
    def get_switch_apps(self) -> List[Dict]:
        """获取 Switch 测试应用列表"""
        return self.apk_registry.get("switch_tests", {}).get("apps", [])
    
    def get_threshold(self, category: str, level: str) -> float:
        """获取阈值配置"""
        return self.test_config.get("thresholds", {}).get(category, {}).get(level, 0)


class FPSAnalyzer:
    """FPS 数据分析器"""
    
    @staticmethod
    def parse_gfxinfo(gfxinfo_output: str, target_fps: int = 120) -> Dict[str, Any]:
        """
        解析 gfxinfo 输出
        
        Args:
            gfxinfo_output: dumpsys gfxinfo 输出
            target_fps: 目标帧率 (默认 120)
            
        Returns:
            包含帧数据的字典
        """
        result = {
            "total_frames": 0,
            "janky_frames": 0,
            "janky_percent": 0.0,
            "percentile_90": 0,
            "percentile_95": 0,
            "percentile_99": 0,
            "avg_fps": 0.0,
            "frame_times": []
        }
        
        target_frame_time_ms = 1000.0 / target_fps
        target_frame_time_ns = target_frame_time_ms * 1_000_000
        
        lines = gfxinfo_output.split('\n')
        in_frame_stats = False
        frame_durations = []
        
        for line in lines:
            line = line.strip()
            
            # 解析总帧数
            if 'Total frames rendered:' in line:
                match = re.search(r'Total frames rendered:\s*(\d+)', line)
                if match:
                    result["total_frames"] = int(match.group(1))
            
            # 兼容旧逻辑：如果 framestats 为空，尝试解析 summary
            elif 'Janky frames:' in line and result["total_frames"] > 0 and not frame_durations:
                match = re.search(r'Janky frames:\s*(\d+)\s*\(([0-9.]+)%\)', line)
                if match:
                    # 暂时存储，如果后面解析到了 framestats 则覆盖
                    result["janky_frames"] = int(match.group(1))
                    result["janky_percent"] = float(match.group(2))
            
            # 解析帧时间数据
            elif line.startswith('---PROFILEDATA---'):
                in_frame_stats = True
                continue
            elif in_frame_stats and line.startswith('---PROFILEDATA---'):
                in_frame_stats = False
                continue
                
            if in_frame_stats and ',' in line:
                parts = line.split(',')
                # remove empty strings
                parts = [p for p in parts if p.strip()]
                
                if len(parts) < 13:
                    continue
                    
                try:
                    # Detect layout based on values
                    # If index 1 is small (Frame ID), shift indices by 1
                    # Timestamp should be ~uptime (huge number)
                    
                    idx_intended_vsync = 1
                    idx_frame_completed = 13
                    
                    # Heuristic: Check if part[1] is a timestamp (very large) or ID (small)
                    val_1 = int(parts[1])
                    if val_1 < 1_000_000_000: # Likely Frame ID/PID
                        idx_intended_vsync = 2
                        idx_frame_completed = 14
                    
                    if len(parts) <= idx_frame_completed:
                         continue
                         
                    intended_vsync = int(parts[idx_intended_vsync])
                    frame_completed = int(parts[idx_frame_completed])
                    
                    # Ignore invalid frames
                    if intended_vsync == 0 or frame_completed == 0:
                        continue
                        
                    duration_ns = frame_completed - intended_vsync
                    duration_ms = duration_ns / 1_000_000.0
                    
                    # Filter crazy outliers (e.g. > 1000ms often paused app)
                    if duration_ms < 500: 
                        frame_durations.append(duration_ms)
                        
                except (ValueError, IndexError):
                    continue

        if frame_durations:
            # Overwrite total frames with actual counted frames if different?
            # Usually keep Total frames rendered from summary as canonical total
            # But let's use what we parsed for stats
            
            result["frame_times"] = frame_durations
            
            # Manual Janky Calculation
            janky_count = sum(1 for d in frame_durations if d > target_frame_time_ms)
            result["janky_frames"] = janky_count
            result["janky_percent"] = (janky_count / len(frame_durations)) * 100
            
            # Percentiles
            sorted_times = sorted(frame_durations)
            result["percentile_90"] = sorted_times[int(len(sorted_times) * 0.90)]
            result["percentile_95"] = sorted_times[int(len(sorted_times) * 0.95)]
            result["percentile_99"] = sorted_times[int(len(sorted_times) * 0.99)]
            
            # Avg FPS: compute from actual frame durations
            avg_frame_time_ms = sum(frame_durations) / len(frame_durations)
            result["avg_fps"] = round(1000.0 / avg_frame_time_ms, 1) if avg_frame_time_ms > 0 else 0
        elif result["total_frames"] > 0 and result["avg_fps"] == 0:
             # Fallback: no framestats available, cannot compute real FPS
             result["avg_fps"] = 0
        
        return result
    
    @staticmethod
    def calculate_grade(janky_percent: float, thresholds: Dict[str, float]) -> str:
        """根据 Janky 比例计算评分等级"""
        if janky_percent <= thresholds.get("excellent", 1):
            return "A+"
        elif janky_percent <= thresholds.get("good", 5):
            return "A"
        elif janky_percent <= thresholds.get("acceptable", 10):
            return "B"
        elif janky_percent <= thresholds.get("poor", 20):
            return "C"
        else:
            return "D"


class ResultsManager:
    """测试结果管理器"""
    
    def __init__(self, results_dir: str):
        self.results_dir = Path(results_dir)
        self.results_dir.mkdir(parents=True, exist_ok=True)
        self.timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    def save_raw_data(self, test_type: str, name: str, data: Dict) -> str:
        """保存原始数据到 JSON"""
        filename = f"{test_type}_{name}_{self.timestamp}.json"
        filepath = self.results_dir / filename
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        return str(filepath)
    
    def get_result_file_path(self, filename: str) -> str:
        """获取结果文件路径"""
        return str(self.results_dir / f"{filename}_{self.timestamp}")


def wait_ms(ms: int):
    """等待指定毫秒数"""
    time.sleep(ms / 1000.0)


def log_info(message: str):
    """输出信息日志"""
    timestamp = datetime.now().strftime("%H:%M:%S")
    print(f"[{timestamp}] [INFO] {message}")


def log_success(message: str):
    """输出成功日志"""
    timestamp = datetime.now().strftime("%H:%M:%S")
    print(f"\033[92m[{timestamp}] [SUCCESS] {message}\033[0m")


def log_error(message: str):
    """输出错误日志"""
    timestamp = datetime.now().strftime("%H:%M:%S")
    print(f"\033[91m[{timestamp}] [ERROR] {message}\033[0m")


def log_warning(message: str):
    """输出警告日志"""
    timestamp = datetime.now().strftime("%H:%M:%S")
    print(f"\033[93m[{timestamp}] [WARNING] {message}\033[0m")


class LogcatMonitor:
    """
    Logcat 监听器
    
    用于监听 Launch/Switch 完成日志，获取真实完成耗时
    
    Launch 完成标记:
        TAG: LaunchPerformance
        格式: [Duration: xxxms]
    
    Switch 完成标记:
        TAG: RealLoadExecutor
        格式: SWITCH_ALL_TASKS_COMPLETE.*finished in xxxms
    """
    
    LAUNCH_TAG = "LaunchPerformance"
    SWITCH_TAG = "SwitchPerf"
    
    @staticmethod
    def clear_logcat():
        """清空 logcat 缓冲区"""
        ADBHelper.run_command("logcat -c")
    
    @staticmethod
    def wait_for_launch_complete(timeout: int = 30) -> Tuple[bool, int]:
        """
        等待 Launch 完成日志
        
        Args:
            timeout: 超时时间（秒）
        
        Returns:
            (是否成功, Duration毫秒)
        """
        import threading
        
        result = {"found": False, "duration": 0}
        stop_event = threading.Event()
        
        def monitor():
            args = ["adb", "logcat", "-v", "time", f"{LogcatMonitor.LAUNCH_TAG}:I", "*:S"]
            proc = subprocess.Popen(
                args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
            )
            try:
                while not stop_event.is_set():
                    line = proc.stdout.readline()
                    if not line:
                        break

                    match = re.search(r'\[Duration:\s*(\d+)ms\]', line)
                    if match:
                        result["found"] = True
                        result["duration"] = int(match.group(1))
                        stop_event.set()
                        break
            except Exception as e:
                log_error(f"Logcat monitor error: {e}")
            finally:
                proc.terminate()
                proc.wait()

        thread = threading.Thread(target=monitor)
        thread.daemon = True
        thread.start()
        thread.join(timeout=timeout)
        stop_event.set()

        return result["found"], result["duration"]
    
    @staticmethod
    def wait_for_switch_complete(timeout: int = 30) -> Tuple[bool, int]:
        """
        等待 Switch 完成日志
        
        Args:
            timeout: 超时时间（秒）
        
        Returns:
            (是否成功, Duration毫秒)
        """
        import threading
        
        result = {"found": False, "duration": 0}
        stop_event = threading.Event()
        
        def monitor():
            # [Fix] SwitchPerf uses Log.d, so we must use :D level or lower
            args = ["adb", "logcat", "-v", "time", f"{LogcatMonitor.SWITCH_TAG}:D", "*:S"]
            proc = subprocess.Popen(
                args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
            )
            try:
                while not stop_event.is_set():
                    line = proc.stdout.readline()
                    if not line:
                        break

                    # 匹配 "Switch complete in xxxms" (忽略大小写和前缀)
                    # 兼容 "WebView switch complete..." 和 "Switch complete..."
                    match = re.search(r'[Ss]witch complete in (\d+)ms', line)
                    if match:
                        result["found"] = True
                        result["duration"] = int(match.group(1))
                        stop_event.set()
                        break
            except Exception as e:
                log_error(f"Logcat monitor error: {e}")
            finally:
                proc.terminate()
                proc.wait()

        thread = threading.Thread(target=monitor)
        thread.daemon = True
        thread.start()
        thread.join(timeout=timeout)
        stop_event.set()

        return result["found"], result["duration"]


class TestStrategy:
    """测试策略枚举和工具"""
    
    STANDARD_SCROLL = "standard_scroll"
    DOUYIN_FLIP = "douyin_flip"
    EBOOK_PAGE = "ebook_page"
    MAP_DRAG = "map_drag"
    
    @staticmethod
    def get_strategy_for_app(app_name: str, test_type: str = None) -> str:
        """根据 App 名称或 registry test_type 返回测试策略"""
        if test_type:
            type_mapping = {
                "vertical_swipe": TestStrategy.DOUYIN_FLIP,
                "horizontal_swipe": TestStrategy.EBOOK_PAGE,
                "map_drag": TestStrategy.MAP_DRAG,
            }
            if test_type in type_mapping:
                return type_mapping[test_type]
        # Fallback to name-based matching
        if app_name == "aosp-douyin":
            return TestStrategy.DOUYIN_FLIP
        elif app_name == "aosp-ebook":
            return TestStrategy.EBOOK_PAGE
        elif app_name in ["gl-map", "surface-map"]:
            return TestStrategy.MAP_DRAG
        else:
            return TestStrategy.STANDARD_SCROLL
    
    @staticmethod
    def get_strategy_description(strategy: str) -> str:
        """获取策略描述"""
        descriptions = {
            TestStrategy.STANDARD_SCROLL: "标准滑动测试：上下交替滑动",
            TestStrategy.DOUYIN_FLIP: "抖音翻页测试：全屏向上滑动切换视频",
            TestStrategy.EBOOK_PAGE: "电子书翻页测试：点击右侧区域翻页",
            TestStrategy.MAP_DRAG: "地图拖拽测试：随机方向拖拽"
        }
        return descriptions.get(strategy, "未知策略")

