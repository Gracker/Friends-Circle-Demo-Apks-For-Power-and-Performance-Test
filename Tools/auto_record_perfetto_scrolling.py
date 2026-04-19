#!/usr/bin/env python3
"""Auto-record Perfetto traces for scrolling debug APKs.

Workflow:
1. Install all `apk-debug/scrolling-*-debug-*.apk`.
2. For each APK and each load type (minimal/heavy/heavy_between_frames/heavy_mixed):
   - launch app
   - enter load page
   - start Perfetto recording
   - swipe up 2 times
   - stop recording and save:
     `device-apk-load-YYYYmmdd-HHMMSS.ptrace`
"""

from __future__ import annotations

import argparse
import hashlib
import re
import shlex
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple


ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
DEFAULT_LOADS = ["minimal", "heavy", "heavy_between_frames", "heavy_mixed"]
DEFAULT_EXCLUDED_MODULES = {
    "scrolling-aosp-douyin",
    "scrolling-aosp-ebook",
    "scrolling-aosp-picasso",
    "scrolling-aosp-power",
    "scrolling-aosp-purerenderthread",
    "scrolling-gl-map",
    "scrolling-surface-map",
    "scrolling-webview-imagereader",
}

LOAD_BUTTON_IDS: Dict[str, List[str]] = {
    "minimal": ["btn_minimal_load"],
    "light": ["btn_light_load"],
    "heavy": ["btn_heavy_load"],
    "heavy_between_frames": ["btn_heavy_between_frames", "btn_heavy_load_between_frames"],
    "medium_between_frames": ["btn_medium_load_between_frames", "btn_medium_between_frames"],
    "heavy_mixed": ["btn_heavy_mixed_load", "btn_heavy_mixed"],
}

@dataclass
class ModuleMeta:
    module_name: str
    apk_path: Path
    module_dir: Path
    package_name: str
    launcher_activity: str
    manifest_activities: List[str]
    supports_activity_type: bool
    # Explicitly declared supported loads for non-repo modules (e.g. binary Flutter builds).
    supported_loads: Optional[Sequence[str]] = None
    # Extra string key used with `am start` to enter a load for modules without discoverable code paths.
    load_entry_key: Optional[str] = None


@dataclass
class CaptureResult:
    module_name: str
    package_name: str
    load: str
    status: str
    reason: str
    trace_path: Optional[Path]
    entry_method: Optional[str]


def now_ts() -> str:
    return datetime.now().strftime("%H:%M:%S")


def log(level: str, msg: str) -> None:
    print(f"[{now_ts()}] [{level}] {msg}")


def sanitize_for_filename(value: str) -> str:
    value = value.strip().replace(" ", "_")
    value = re.sub(r"[^A-Za-z0-9._-]+", "-", value)
    value = re.sub(r"-{2,}", "-", value)
    return value.strip("-_.") or "unknown"


def combined_output(proc: subprocess.CompletedProcess[str]) -> str:
    return ((proc.stdout or "") + (proc.stderr or "")).strip()


def parse_bounds(bounds: str) -> Optional[Tuple[int, int]]:
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not match:
        return None
    x1, y1, x2, y2 = map(int, match.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def parse_bounds_rect(bounds: str) -> Optional[Tuple[int, int, int, int]]:
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not match:
        return None
    x1 = int(match.group(1))
    y1 = int(match.group(2))
    x2 = int(match.group(3))
    y2 = int(match.group(4))
    return x1, y1, x2, y2


class ADB:
    def __init__(self, serial: Optional[str], dry_run: bool = False):
        self.serial = serial
        self.dry_run = dry_run

    def _base(self) -> List[str]:
        cmd = ["adb"]
        if self.serial:
            cmd += ["-s", self.serial]
        return cmd

    def run(
        self, args: Sequence[str], timeout: int = 60
    ) -> subprocess.CompletedProcess[str]:
        cmd = self._base() + list(args)
        if self.dry_run:
            log("DRY", " ".join(shlex.quote(x) for x in cmd))
            return subprocess.CompletedProcess(cmd, 0, "", "")
        return subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout,
        )

    def shell(self, args: Sequence[str], timeout: int = 60) -> subprocess.CompletedProcess[str]:
        return self.run(["shell"] + list(args), timeout=timeout)

    def check_device(self) -> bool:
        proc = self.run(["devices"], timeout=15)
        if proc.returncode != 0:
            return False
        lines = [x.strip() for x in (proc.stdout or "").splitlines()[1:]]
        return any(line.endswith("\tdevice") for line in lines)

    def get_serial(self) -> str:
        proc = self.run(["get-serialno"], timeout=10)
        serial = combined_output(proc)
        return serial if serial else "unknown-serial"

    def get_model(self) -> str:
        proc = self.shell(["getprop", "ro.product.model"], timeout=10)
        model = combined_output(proc)
        return model if model else "unknown-model"

    def force_stop(self, package: str) -> None:
        self.shell(["am", "force-stop", package], timeout=15)

    def press_home(self) -> None:
        self.shell(["input", "keyevent", "KEYCODE_HOME"], timeout=10)

    def swipe(
        self, x1: int, y1: int, x2: int, y2: int, duration_ms: int
    ) -> subprocess.CompletedProcess[str]:
        return self.shell(
            ["input", "swipe", str(x1), str(y1), str(x2), str(y2), str(duration_ms)],
            timeout=20,
        )

    def tap(self, x: int, y: int) -> subprocess.CompletedProcess[str]:
        return self.shell(["input", "tap", str(x), str(y)], timeout=15)

    def get_screen_size(self) -> Tuple[int, int]:
        proc = self.shell(["wm", "size"], timeout=10)
        out = combined_output(proc)
        match = re.search(r"(\d+)x(\d+)", out)
        if match:
            return int(match.group(1)), int(match.group(2))
        return 1080, 2400

    def _am_start_args(
        self,
        package: str,
        activity: str,
        string_extras: Optional[Dict[str, str]] = None,
        int_extras: Optional[Dict[str, int]] = None,
        force_stop: bool = False,
    ) -> List[str]:
        args: List[str] = ["am", "start", "-W"]
        if force_stop:
            args.append("-S")
        args += ["-n", f"{package}/{activity}"]
        if string_extras:
            for key, value in string_extras.items():
                args += ["--es", key, value]
        if int_extras:
            for key, value in int_extras.items():
                args += ["--ei", key, str(value)]
        return args

    def start_activity(
        self,
        package: str,
        activity: str,
        string_extras: Optional[Dict[str, str]] = None,
        int_extras: Optional[Dict[str, int]] = None,
        force_stop: bool = False,
    ) -> Tuple[bool, str]:
        am_args = self._am_start_args(
            package=package,
            activity=activity,
            string_extras=string_extras,
            int_extras=int_extras,
            force_stop=force_stop,
        )
        cmd: List[str] = ["shell"] + am_args
        proc = self.run(cmd, timeout=45)
        out = combined_output(proc)
        return is_am_start_success(proc.returncode, out), out

    def focused_component(self) -> Optional[str]:
        # Prefer activity dump on Android 14/15 where window focus keys may be absent/stale.
        proc2 = self.shell(["dumpsys", "activity", "activities"], timeout=30)
        out2 = combined_output(proc2)
        for line in out2.splitlines():
            if any(
                key in line
                for key in (
                    "topResumedActivity",
                    "mResumedActivity",
                    "ResumedActivity:",
                    "ResumedActivity ",
                )
            ):
                match = re.search(r"([A-Za-z0-9._]+/[A-Za-z0-9.$_]+)", line)
                if match:
                    return match.group(1)

        proc = self.shell(["dumpsys", "window", "windows"], timeout=30)
        out = combined_output(proc)
        for key in ("mCurrentFocus", "mFocusedApp"):
            for line in out.splitlines():
                if key in line:
                    match = re.search(r"([A-Za-z0-9._]+/[A-Za-z0-9.$_]+)", line)
                    if match:
                        return match.group(1)
        return None

    def dump_uixml(self) -> Optional[str]:
        dump_path = "/sdcard/codex_ui_dump.xml"
        dump_proc = self.shell(["uiautomator", "dump", dump_path], timeout=20)
        if dump_proc.returncode != 0:
            return None
        cat_proc = self.shell(["cat", dump_path], timeout=20)
        if cat_proc.returncode != 0:
            return None
        text = cat_proc.stdout or ""
        return text if "<hierarchy" in text else None

    def screenshot_hash(self) -> Optional[str]:
        if self.dry_run:
            return f"dry-{time.time_ns()}"
        cmd = self._base() + ["exec-out", "screencap", "-p"]
        try:
            proc = subprocess.run(cmd, capture_output=True, timeout=30)
        except Exception:
            return None
        if proc.returncode != 0 or not proc.stdout:
            return None
        # adb screencap may contain CRLF; normalize first.
        data = proc.stdout.replace(b"\r\n", b"\n")
        return hashlib.sha1(data).hexdigest()


def is_am_start_success(returncode: int, output: str) -> bool:
    if returncode != 0:
        return False
    bad_tokens = [
        "Error type",
        "Exception occurred",
        "Permission Denial",
        "does not exist",
        "SecurityException",
    ]
    return not any(token in output for token in bad_tokens)


def summarize_am_start_output(output: str) -> str:
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    if not lines:
        return "empty_output"
    priority_tokens = (
        "Permission Denial",
        "not exported from uid",
        "SecurityException",
        "does not exist",
        "Error type",
        "Error:",
    )
    for token in priority_tokens:
        for line in lines:
            if token in line:
                return line[:220]
    for line in lines:
        if any(
            token in line
            for token in (
                "java.lang",
                "Exception",
            )
        ):
            return line[:220]
    return lines[-1][:220]


def normalize_activity_name(package_name: str, activity_name: str) -> str:
    if activity_name.startswith("."):
        return package_name + activity_name
    if "." in activity_name:
        return activity_name
    return package_name + "." + activity_name


def component_matches_launcher(
    component: str, package_name: str, launcher_activity: str
) -> bool:
    if "/" not in component:
        return False
    pkg, act = component.split("/", 1)
    if pkg != package_name:
        return False
    return normalize_activity_name(pkg, act) == normalize_activity_name(pkg, launcher_activity)


def parse_application_id(module_dir: Path) -> Optional[str]:
    build_file = module_dir / "build.gradle"
    if not build_file.exists():
        return None
    text = build_file.read_text(encoding="utf-8", errors="ignore")
    match = re.search(r'applicationId\s+["\']([^"\']+)["\']', text)
    if match:
        return match.group(1).strip()
    match = re.search(r'namespace\s+["\']([^"\']+)["\']', text)
    if match:
        return match.group(1).strip()
    return None


def parse_launcher_activity(module_dir: Path) -> Optional[str]:
    manifest = module_dir / "src" / "main" / "AndroidManifest.xml"
    if not manifest.exists():
        return None
    try:
        tree = ET.parse(manifest)
    except ET.ParseError:
        return None

    root = tree.getroot()
    app = root.find("application")
    if app is None:
        return None

    for node_name in ("activity", "activity-alias"):
        for activity in app.findall(node_name):
            filters = activity.findall("intent-filter")
            for intent_filter in filters:
                has_main = any(
                    action.get(ANDROID_NS + "name") == "android.intent.action.MAIN"
                    for action in intent_filter.findall("action")
                )
                has_launcher = any(
                    category.get(ANDROID_NS + "name") == "android.intent.category.LAUNCHER"
                    for category in intent_filter.findall("category")
                )
                if has_main and has_launcher:
                    activity_name = activity.get(ANDROID_NS + "name")
                    if activity_name:
                        return activity_name
    return None


def parse_manifest_activities(module_dir: Path) -> List[str]:
    manifest = module_dir / "src" / "main" / "AndroidManifest.xml"
    if not manifest.exists():
        return []
    try:
        tree = ET.parse(manifest)
    except ET.ParseError:
        return []

    root = tree.getroot()
    app = root.find("application")
    if app is None:
        return []

    activities: List[str] = []
    for node_name in ("activity", "activity-alias"):
        for activity in app.findall(node_name):
            name = activity.get(ANDROID_NS + "name")
            if name:
                activities.append(name)
    return activities


def module_supports_activity_type(module_dir: Path) -> bool:
    src_main = module_dir / "src" / "main"
    if not src_main.exists():
        return False
    for ext in ("*.java", "*.kt"):
        for file_path in src_main.rglob(ext):
            try:
                text = file_path.read_text(encoding="utf-8", errors="ignore")
            except OSError:
                continue
            if '"activity_type"' in text or "EXTRA_ACTIVITY_TYPE" in text:
                return True
    return False


def collect_module_text(module_dir: Path) -> str:
    src_main = module_dir / "src" / "main"
    chunks: List[str] = []
    if not src_main.exists():
        return ""
    for ext in ("*.java", "*.kt", "*.xml"):
        for file_path in src_main.rglob(ext):
            try:
                chunks.append(file_path.read_text(encoding="utf-8", errors="ignore"))
            except OSError:
                continue
    return "\n".join(chunks)


def detect_manual_permission_reasons(module_dir: Path) -> List[str]:
    reasons: List[str] = []

    manifest = module_dir / "src" / "main" / "AndroidManifest.xml"
    if manifest.exists():
        manifest_text = manifest.read_text(encoding="utf-8", errors="ignore")
        if "android.permission.SYSTEM_ALERT_WINDOW" in manifest_text:
            reasons.append("SYSTEM_ALERT_WINDOW")

    src_main = module_dir / "src" / "main"
    if src_main.exists():
        patterns = {
            "ACTION_MANAGE_OVERLAY_PERMISSION": "ACTION_MANAGE_OVERLAY_PERMISSION",
            "canDrawOverlays(": "Settings.canDrawOverlays",
            "requestPermissions(": "requestPermissions",
            "ActivityCompat.requestPermissions(": "ActivityCompat.requestPermissions",
        }
        seen = set()
        for ext in ("*.java", "*.kt"):
            for file_path in src_main.rglob(ext):
                try:
                    text = file_path.read_text(encoding="utf-8", errors="ignore")
                except OSError:
                    continue
                for raw, label in patterns.items():
                    if raw in text and label not in seen:
                        reasons.append(label)
                        seen.add(label)

    return reasons


def module_supports_load(meta: ModuleMeta, load: str, module_text_cache: Dict[str, str]) -> bool:
    if meta.supported_loads is not None:
        return load in set(meta.supported_loads)

    text = module_text_cache.get(meta.module_name)
    if text is None:
        text = collect_module_text(meta.module_dir)
        module_text_cache[meta.module_name] = text

    if meta.supports_activity_type:
        # Check activity_type token exists in code path.
        if f'"{load}"' in text:
            return True
        # Some modules (e.g. compose) may map via route helpers but still include the token.
        return False

    # Fallback UI route: requires one of the expected button ids in layout/resources.
    for rid in LOAD_BUTTON_IDS.get(load, []):
        if f"@+id/{rid}" in text or f":id/{rid}" in text:
            return True
    return False


def discover_modules(
    repo_root: Path,
    apk_dir: Path,
    only_tokens: Sequence[str],
    include_manual_permission_modules: bool,
) -> List[ModuleMeta]:
    apks = sorted(apk_dir.glob("scrolling-*-debug-*.apk"))
    modules: List[ModuleMeta] = []
    only = [x.strip() for x in only_tokens if x.strip()]

    for apk_path in apks:
        module_name = apk_path.name.split("-debug-")[0]
        if module_name in DEFAULT_EXCLUDED_MODULES:
            log("WARN", f"Skip {apk_path.name}: excluded by default policy ({module_name})")
            continue

        if only and not any(
            token == module_name
            or token == apk_path.name
            or token in module_name
            or token in apk_path.name
            for token in only
        ):
            continue

        module_dir = repo_root / module_name
        if not module_dir.exists():
            log("WARN", f"Skip {apk_path.name}: module dir not found")
            continue

        manual_permission_reasons = detect_manual_permission_reasons(module_dir)
        if manual_permission_reasons and not include_manual_permission_modules:
            reason_text = ", ".join(manual_permission_reasons)
            log(
                "WARN",
                f"Skip {apk_path.name}: requires manual permission confirmation ({reason_text})",
            )
            continue

        package_name = parse_application_id(module_dir)
        launcher_activity = parse_launcher_activity(module_dir)
        if not package_name or not launcher_activity:
            log("WARN", f"Skip {apk_path.name}: cannot parse package/activity")
            continue

        manifest_activities = parse_manifest_activities(module_dir)
        modules.append(
            ModuleMeta(
                module_name=module_name,
                apk_path=apk_path,
                module_dir=module_dir,
                package_name=package_name,
                launcher_activity=launcher_activity,
                manifest_activities=manifest_activities,
                supports_activity_type=module_supports_activity_type(module_dir),
            )
        )
    return modules


def install_apk(adb: ADB, meta: ModuleMeta) -> bool:
    if adb.dry_run:
        adb.run(["install", "-r", str(meta.apk_path)], timeout=240)
        return True

    proc = adb.run(["install", "-r", str(meta.apk_path)], timeout=240)
    out = combined_output(proc)
    if proc.returncode == 0 and ("Success" in out or out == ""):
        return True

    # 覆盖安装失败时，先卸载旧包再强制覆盖安装重试
    log("WARN", f"Install retry after uninstall: {meta.package_name}")
    if out:
        log("WARN", f"Install error (first try): {out}")
    adb.run(["uninstall", meta.package_name], timeout=60)
    proc2 = adb.run(["install", "-r", str(meta.apk_path)], timeout=240)
    out2 = combined_output(proc2)
    if proc2.returncode != 0 and out2:
        log("ERROR", f"Install error (second try): {out2}")
    return proc2.returncode == 0 and ("Success" in out2 or out2 == "")


def find_tap_point_from_uixml(
    xml_text: str, package_name: str, load: str
) -> Optional[Tuple[int, int]]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return None

    ids = LOAD_BUTTON_IDS.get(load, [])
    for node in root.iter("node"):
        rid = node.attrib.get("resource-id", "")
        bounds = node.attrib.get("bounds", "")
        if not bounds:
            continue
        for short_id in ids:
            if rid == f"{package_name}:id/{short_id}" or rid.endswith(f"/{short_id}"):
                pt = parse_bounds(bounds)
                if pt:
                    return pt

    return None


def find_scrollable_swipe_route(
    xml_text: str, package_name: str
) -> Optional[Tuple[int, int, int, str]]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return None

    candidates: List[Tuple[int, int, int, int, int, int, str]] = []
    for node in root.iter("node"):
        bounds = node.attrib.get("bounds", "")
        rect = parse_bounds_rect(bounds)
        if not rect:
            continue
        x1, y1, x2, y2 = rect
        width = x2 - x1
        height = y2 - y1
        if width < 120 or height < 180:
            continue

        class_name = node.attrib.get("class", "")
        node_package = node.attrib.get("package", "")
        rid = node.attrib.get("resource-id", "")
        is_scrollable = node.attrib.get("scrollable") == "true"
        clickable = node.attrib.get("clickable") == "true"

        priority = 0
        if is_scrollable:
            priority += 220
        if node_package == package_name:
            priority += 20
        if any(
            key in class_name
            for key in (
                "RecyclerView",
                "ListView",
                "ScrollView",
                "NestedScrollView",
                "ViewPager",
                "WebView",
                "MapView",
                "GLSurfaceView",
                "SurfaceView",
                "TextureView",
            )
        ):
            priority += 120
        rid_lower = rid.lower()
        if any(
            key in rid_lower
            for key in ("recycler", "list", "feed", "timeline", "scroll", "map", "webview", "content")
        ):
            priority += 70
        if clickable:
            # Avoid tapping controls: swipes should start from content, not buttons.
            priority -= 30
        if any(
            class_name.endswith(suffix)
            for suffix in (
                "LinearLayout",
                "FrameLayout",
                "RelativeLayout",
                "ConstraintLayout",
            )
        ) and not is_scrollable and not rid:
            # Generic containers (especially root) are unsafe on map pages with bottom toolbars.
            priority -= 120

        # Ignore generic non-scroll views with no evidence.
        if priority < 90:
            continue

        area = width * height
        candidates.append((priority, area, x1, y1, x2, y2, class_name))

    if not candidates:
        return None

    candidates.sort(key=lambda x: (x[0], x[1]), reverse=True)
    _, _, x1, y1, x2, y2, class_name = candidates[0]

    x = (x1 + x2) // 2
    height = y2 - y1

    # Use longer-distance + shorter-duration flick to better match manual inertia.
    top_guard = y1 + int(height * 0.14)
    bottom_guard = y2 - int(height * 0.06)
    y_start = bottom_guard
    travel = min(980, max(520, int(height * 0.40)))
    y_end = y_start - travel
    if y_end < top_guard:
        y_end = top_guard

    if y_start - y_end < 90:
        return None
    return x, y_start, y_end, class_name


def find_package_viewport(
    xml_text: str, package_name: str
) -> Optional[Tuple[int, int, int, int]]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return None

    best: Optional[Tuple[int, int, int, int, int]] = None
    for node in root.iter("node"):
        bounds = node.attrib.get("bounds", "")
        rect = parse_bounds_rect(bounds)
        if not rect:
            continue
        x1, y1, x2, y2 = rect
        width = x2 - x1
        height = y2 - y1
        if width < 200 or height < 300:
            continue
        node_package = node.attrib.get("package", "")
        if node_package != package_name:
            continue
        area = width * height
        if best is None or area > best[0]:
            best = (area, x1, y1, x2, y2)

    if best is None:
        return None
    _, x1, y1, x2, y2 = best
    return x1, y1, x2, y2


def viewport_fallback_swipe_route(
    viewport: Tuple[int, int, int, int],
    x_ratio: float,
    start_y_ratio: float,
    end_y_ratio: float,
) -> Tuple[int, int, int]:
    x1, y1, x2, y2 = viewport
    width = x2 - x1
    height = y2 - y1
    x = x1 + int(width * x_ratio)
    y_start = y1 + int(height * start_y_ratio)
    y_end = y1 + int(height * end_y_ratio)
    # Keep fallback swipe as a long and forceful fling.
    min_distance = max(360, int(height * 0.34))
    if y_start - y_end < min_distance:
        y_end = max(y1 + int(height * 0.10), y_start - min_distance)
    return x, y_start, y_end


def wait_for_entered_target(
    adb: ADB,
    meta: ModuleMeta,
    timeout_sec: float,
) -> bool:
    if adb.dry_run:
        return True

    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        focused = adb.focused_component()
        if focused and "/" in focused:
            pkg, _ = focused.split("/", 1)
            if pkg != meta.package_name:
                time.sleep(0.25)
                continue

            # Compose keeps one activity and changes route internally.
            # Flutter binaries pass load via launch extras while keeping launcher Activity.
            if meta.module_name == "scrolling-compose" or meta.load_entry_key:
                return True

            if not component_matches_launcher(
                focused, meta.package_name, meta.launcher_activity
            ):
                return True
        time.sleep(0.25)
    return False


def click_load_button(
    adb: ADB,
    meta: ModuleMeta,
    package_name: str,
    load: str,
    wait_after_tap_sec: float,
) -> bool:
    if adb.dry_run:
        adb.tap(540, 1200)
        time.sleep(wait_after_tap_sec)
        return True

    for _ in range(4):
        xml_text = adb.dump_uixml()
        if xml_text:
            point = find_tap_point_from_uixml(xml_text, package_name, load)
            if point:
                adb.tap(point[0], point[1])
                time.sleep(wait_after_tap_sec)
                if wait_for_entered_target(adb, meta, timeout_sec=2.5):
                    return True
        time.sleep(0.5)
    return False


def load_to_int(load: str) -> int:
    # Mirrors load-config LoadType constants.
    mapping = {
        "minimal": 0,
        "light": 1,
        "heavy": 3,
        "heavy_between_frames": 6,
        "medium_between_frames": 5,
        "heavy_mixed": 9,
    }
    return mapping[load]


def fallback_swipe_route(
    screen_size: Tuple[int, int],
    x_ratio: float,
    start_y_ratio: float,
    end_y_ratio: float,
) -> Tuple[int, int, int]:
    width, height = screen_size
    x = int(width * x_ratio)
    y_start = int(height * start_y_ratio)
    y_end = int(height * end_y_ratio)
    return x, y_start, y_end


def detect_motion_after_swipe(adb: ADB, before_hash: Optional[str]) -> Tuple[bool, Optional[str]]:
    after_hash = adb.screenshot_hash()
    if before_hash and after_hash and before_hash != after_hash:
        return True, after_hash
    return False, after_hash or before_hash


def direct_activity_candidates(meta: ModuleMeta, load: str) -> List[str]:
    def is_match(simple: str) -> bool:
        if simple.endswith("MainActivity"):
            return False
        if load == "minimal":
            return "Minimal" in simple
        if load == "light":
            return (
                "Light" in simple
                and "BetweenFrames" not in simple
                and "Mixed" not in simple
                and "LongFrame" not in simple
            )
        if load == "heavy":
            return (
                "Heavy" in simple
                and "BetweenFrames" not in simple
                and "Mixed" not in simple
                and "LongFrame" not in simple
            )
        if load == "medium_between_frames":
            return "Medium" in simple and "BetweenFrames" in simple
        if load == "heavy_between_frames":
            return "Heavy" in simple and "BetweenFrames" in simple
        if load == "heavy_mixed":
            return "Heavy" in simple and "Mixed" in simple
        return False

    scored: List[Tuple[int, str]] = []
    for act in meta.manifest_activities:
        fq = normalize_activity_name(meta.package_name, act)
        simple = fq.split(".")[-1]
        if not is_match(simple):
            continue

        score = 0
        if "Load" in simple:
            score += 20
        if any(k in simple for k in ("WebView", "GeckoView", "Map")):
            score += 10
        scored.append((score, act))

    scored.sort(key=lambda x: x[0], reverse=True)
    return [act for _, act in scored]


def enter_load_page(
    adb: ADB,
    meta: ModuleMeta,
    load: str,
    wait_after_entry_sec: float,
    allow_main_entry: bool,
) -> Tuple[bool, str]:
    log("INFO", f"  Enter load `{load}` for {meta.module_name}")

    # Flutter/binary APKs: entry is usually via launcher extras.
    if meta.load_entry_key:
        log("INFO", f"    Try method: activity_extra:{meta.load_entry_key}")
        ok, out = adb.start_activity(
            package=meta.package_name,
            activity=meta.launcher_activity,
            string_extras={meta.load_entry_key: load},
            force_stop=False,
        )
        if ok:
            time.sleep(wait_after_entry_sec)
            if wait_for_entered_target(adb, meta, timeout_sec=2.5):
                return True, f"{meta.load_entry_key}_extra"
        if out:
            short = summarize_am_start_output(out)
            log("WARN", f"      activity_extra failed: {short}")

    # CustomScroll modules use one feed activity + int load extra.
    for maybe_feed in (".CustomScrollFeedActivity",):
        if maybe_feed in meta.manifest_activities:
            log("INFO", "    Try method: direct_feed_extra")
            ok, out = adb.start_activity(
                package=meta.package_name,
                activity=maybe_feed,
                int_extras={"extra_load_type": load_to_int(load)},
                force_stop=False,
            )
            if not ok and out:
                short = summarize_am_start_output(out)
                log("WARN", f"      direct_feed_extra failed: {short}")
            if ok:
                time.sleep(wait_after_entry_sec)
                if wait_for_entered_target(adb, meta, timeout_sec=2.5):
                    return True, "direct_feed_extra"

    # Try direct target activity (without main screen).
    direct_candidates = direct_activity_candidates(meta, load)
    if direct_candidates:
        log("INFO", f"    Try method: direct_activity ({len(direct_candidates)} candidates)")
    for activity_name in direct_candidates:
        ok, out = adb.start_activity(
            package=meta.package_name,
            activity=activity_name,
            force_stop=False,
        )
        entry_method = "direct_activity"
        if not ok:
            if out:
                short = summarize_am_start_output(out)
                log("WARN", f"      direct_activity failed: {activity_name} -> {short}")
            continue
        if ok:
            time.sleep(wait_after_entry_sec)
            focused = adb.focused_component()
            if focused:
                log("INFO", f"      focused: {focused}")
            if wait_for_entered_target(adb, meta, timeout_sec=2.5):
                return True, entry_method

    # ADB-only redirect via launcher extras (no UI interaction/tap).
    if meta.supports_activity_type:
        log("INFO", "    Try method: activity_type_redirect")
        ok, _ = adb.start_activity(
            package=meta.package_name,
            activity=meta.launcher_activity,
            string_extras={"activity_type": load},
            force_stop=False,
        )
        if ok:
            time.sleep(wait_after_entry_sec)
            if wait_for_entered_target(adb, meta, timeout_sec=2.5):
                return True, "activity_type"

    if allow_main_entry:
        # Legacy fallback: open launcher and click button.
        log("INFO", "    Fallback method: open launcher + click button")
        ok_launch, _ = adb.start_activity(
            package=meta.package_name,
            activity=meta.launcher_activity,
            force_stop=False,
        )
        if not ok_launch:
            return False, "launcher_open_failed"
        time.sleep(0.6)

        if click_load_button(adb, meta, meta.package_name, load, wait_after_entry_sec):
            return True, "ui_button"
        return False, "unsupported_load"

    return False, "direct_entry_failed"


class TraceRecorder:
    def __init__(
        self,
        perfetto_bin: Path,
        perfetto_config: Path,
        serial: Optional[str],
        output_path: Path,
        dry_run: bool = False,
    ):
        self.perfetto_bin = perfetto_bin
        self.perfetto_config = perfetto_config
        self.serial = serial
        self.output_path = output_path
        self.dry_run = dry_run
        self.proc: Optional[subprocess.Popen[str]] = None

    def start(self) -> Tuple[bool, str]:
        cmd = [
            str(self.perfetto_bin),
            "-n",
            "--no-open-browser",
            "-c",
            str(self.perfetto_config),
            "-o",
            str(self.output_path),
        ]
        if self.serial:
            cmd += ["-s", self.serial]
        if self.dry_run:
            log("DRY", " ".join(shlex.quote(x) for x in cmd))
            return True, ""
        try:
            self.proc = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            return True, ""
        except OSError as exc:
            return False, str(exc)

    def stop(self, timeout_sec: int) -> Tuple[bool, str]:
        if self.dry_run:
            return True, ""
        if not self.proc:
            return False, "trace process not started"

        try:
            if self.proc.poll() is None:
                self.proc.send_signal(signal.SIGINT)
            stdout, stderr = self.proc.communicate(timeout=timeout_sec)
            out = ((stdout or "") + (stderr or "")).strip()
            return self.proc.returncode == 0, out
        except subprocess.TimeoutExpired:
            self.proc.kill()
            stdout, stderr = self.proc.communicate()
            out = ((stdout or "") + (stderr or "")).strip()
            return False, f"trace stop timeout; output={out}"


def run_single_capture(
    adb: ADB,
    meta: ModuleMeta,
    load: str,
    trace_dir: Path,
    perfetto_bin: Path,
    perfetto_config: Path,
    device_tag: str,
    swipe_count: int,
    swipe_duration_ms: int,
    swipe_interval_sec: float,
    post_swipe_wait_sec: float,
    swipe_x_ratio: float,
    swipe_start_y_ratio: float,
    swipe_end_y_ratio: float,
    motion_check_enabled: bool,
    allow_main_entry: bool,
    trace_start_wait_sec: float,
    wait_after_launch_sec: float,
    wait_after_entry_sec: float,
    trace_stop_timeout_sec: int,
    dry_run: bool,
) -> CaptureResult:
    trace_path: Optional[Path] = None
    recorder: Optional[TraceRecorder] = None
    entry_method: Optional[str] = None

    try:
        # Requirement: force-stop before each test.
        log("INFO", f"[1/8] Pre-clean: force-stop {meta.package_name}")
        adb.force_stop(meta.package_name)

        log("INFO", f"[2/8] Enter target page: {meta.module_name}/{load}")
        screen_size = adb.get_screen_size()
        entered, entry_method = enter_load_page(
            adb=adb,
            meta=meta,
            load=load,
            wait_after_entry_sec=wait_after_entry_sec,
            allow_main_entry=allow_main_entry,
        )
        if not entered:
            log("WARN", f"  Enter target page failed: {meta.module_name}/{load}")
            return CaptureResult(
                module_name=meta.module_name,
                package_name=meta.package_name,
                load=load,
                status="failed",
                reason="direct_entry_failed",
                trace_path=None,
                entry_method=None,
            )

        # Requirement: wait for page stabilization before tracing.
        if wait_after_launch_sec > 0:
            log("INFO", f"[3/8] Stabilize page: wait {wait_after_launch_sec:.1f}s")
            time.sleep(wait_after_launch_sec)

        # Requirement: enter load activity first, then start tracing.
        timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        trace_name = f"{device_tag}-{meta.module_name}-{load}-{timestamp}.ptrace"
        trace_path = trace_dir / trace_name
        log("INFO", f"[4/8] Trace output path: {trace_path}")
        recorder = TraceRecorder(
            perfetto_bin=perfetto_bin,
            perfetto_config=perfetto_config,
            serial=adb.serial,
            output_path=trace_path,
            dry_run=dry_run,
        )

        log("INFO", f"[5/8] Start trace (entry method: {entry_method})")
        ok_start, start_err = recorder.start()
        if not ok_start:
            log("ERROR", f"  Trace start failed: {start_err}")
            return CaptureResult(
                module_name=meta.module_name,
                package_name=meta.package_name,
                load=load,
                status="failed",
                reason=f"trace_start_failed: {start_err}",
                trace_path=None,
                entry_method=entry_method,
            )

        log("INFO", f"[6/8] Wait {trace_start_wait_sec:.1f}s then swipe x{swipe_count}")
        time.sleep(trace_start_wait_sec)

        # Prefer real scrollable container bounds. Fallback to ratio route.
        primary_route: Tuple[int, int, int]
        route_desc = "ratio_fallback"
        xml_text = None if adb.dry_run else adb.dump_uixml()
        if xml_text:
            found = find_scrollable_swipe_route(xml_text, meta.package_name)
            if found:
                x, y_start, y_end, klass = found
                primary_route = (x, y_start, y_end)
                route_desc = f"scrollable:{klass}"
            else:
                viewport = find_package_viewport(xml_text, meta.package_name)
                if viewport:
                    primary_route = viewport_fallback_swipe_route(
                        viewport, swipe_x_ratio, swipe_start_y_ratio, swipe_end_y_ratio
                    )
                    route_desc = "package_viewport_fallback"
                else:
                    primary_route = fallback_swipe_route(
                        screen_size, swipe_x_ratio, swipe_start_y_ratio, swipe_end_y_ratio
                    )
        else:
            primary_route = fallback_swipe_route(
                screen_size, swipe_x_ratio, swipe_start_y_ratio, swipe_end_y_ratio
            )

        x, y_start, y_end = primary_route
        distance = y_start - y_end
        log(
            "INFO",
            f"    Swipe route ({route_desc}): x={x}, y:{y_start}->{y_end}, "
            f"distance={distance}px, duration={swipe_duration_ms}ms",
        )

        prev_hash = adb.screenshot_hash() if motion_check_enabled else None
        motion_detected = False
        for i in range(swipe_count):
            sx, sy_start, sy_end = x, y_start, y_end
            log("INFO", f"    Swipe {i + 1}/{swipe_count}")
            swipe_proc = adb.swipe(sx, sy_start, sx, sy_end, swipe_duration_ms)
            if swipe_proc.returncode != 0:
                log("WARN", f"      swipe command failed: {summarize_am_start_output(combined_output(swipe_proc))}")
            if motion_check_enabled:
                time.sleep(0.8)
                moved, prev_hash = detect_motion_after_swipe(adb, prev_hash)
                if moved:
                    motion_detected = True
            if i < swipe_count - 1 and swipe_interval_sec > 0:
                log("INFO", f"      Wait {swipe_interval_sec:.1f}s before next swipe")
                time.sleep(swipe_interval_sec)

        # Recovery: if no visual change detected, try alternative lower routes.
        if motion_check_enabled and not motion_detected:
            log("WARN", "    No visual motion detected, trying fallback swipe routes")
            fallback_routes = [
                fallback_swipe_route(screen_size, 0.50, 0.82, 0.72),
                fallback_swipe_route(screen_size, 0.35, 0.80, 0.70),
                fallback_swipe_route(screen_size, 0.65, 0.80, 0.70),
            ]
            for idx, (fx, fy_start, fy_end) in enumerate(fallback_routes, start=1):
                log("INFO", f"    Fallback route {idx}: x={fx}, y:{fy_start}->{fy_end}")
                adb.swipe(fx, fy_start, fx, fy_end, max(80, int(swipe_duration_ms * 0.85)))
                time.sleep(0.9)
                moved, prev_hash = detect_motion_after_swipe(adb, prev_hash)
                if moved:
                    motion_detected = True
                    log("INFO", f"    Motion detected on fallback route {idx}")
                    break

        if post_swipe_wait_sec > 0:
            log("INFO", f"[7/8] Wait {post_swipe_wait_sec:.1f}s after last swipe")
            time.sleep(post_swipe_wait_sec)

        log("INFO", "[8/8] Stop trace")
        ok_stop, stop_out = recorder.stop(trace_stop_timeout_sec)

        if not dry_run:
            trace_ok = bool(trace_path and trace_path.exists() and trace_path.stat().st_size > 0)
        else:
            trace_ok = True

        if motion_check_enabled and not motion_detected:
            return CaptureResult(
                module_name=meta.module_name,
                package_name=meta.package_name,
                load=load,
                status="failed",
                reason="no_motion_detected_after_swipes",
                trace_path=trace_path if trace_path and trace_path.exists() else None,
                entry_method=entry_method,
            )

        if ok_stop and trace_ok:
            if trace_path and trace_path.exists():
                size_mb = trace_path.stat().st_size / (1024 * 1024)
                log("INFO", f"  Trace ready: {trace_path} ({size_mb:.2f} MB)")
            else:
                log("INFO", f"  Trace ready: {trace_path}")
            return CaptureResult(
                module_name=meta.module_name,
                package_name=meta.package_name,
                load=load,
                status="success",
                reason="ok",
                trace_path=trace_path,
                entry_method=entry_method,
            )

        if stop_out:
            log("ERROR", f"  Trace stop output: {stop_out}")
        return CaptureResult(
            module_name=meta.module_name,
            package_name=meta.package_name,
            load=load,
            status="failed",
            reason=f"trace_stop_or_file_failed: {stop_out}",
            trace_path=trace_path if trace_path and trace_path.exists() else None,
            entry_method=entry_method,
        )
    finally:
        # Requirement: force-stop after each test.
        if recorder and recorder.proc and recorder.proc.poll() is None:
            recorder.stop(trace_stop_timeout_sec)
        log("INFO", f"  Cleanup: force-stop {meta.package_name} + HOME")
        adb.force_stop(meta.package_name)
        adb.press_home()


def build_arg_parser(repo_root: Path) -> argparse.ArgumentParser:
    default_apk_dir = repo_root / "apk-debug"
    default_tools = repo_root / "Tools"
    default_trace_dir = default_tools / "trace"
    parser = argparse.ArgumentParser(description="Auto record Perfetto for scrolling APKs")
    parser.add_argument("--serial", help="ADB device serial", default=None)
    parser.add_argument("--apk-dir", type=Path, default=default_apk_dir)
    parser.add_argument("--trace-dir", type=Path, default=default_trace_dir)
    parser.add_argument(
        "--perfetto-bin",
        type=Path,
        default=default_tools / "record_android_trace",
    )
    parser.add_argument(
        "--perfetto-config",
        type=Path,
        default=default_tools / "perfetto.config",
    )
    parser.add_argument(
        "--loads",
        nargs="+",
        default=DEFAULT_LOADS,
        choices=["minimal", "heavy", "heavy_between_frames", "heavy_mixed"],
        help="Loads to run for each apk",
    )
    parser.add_argument(
        "--only-apk",
        nargs="+",
        default=[],
        help="Filter apk/module by name token (e.g. scrolling-aosp-performance)",
    )
    parser.add_argument("--swipe-count", type=int, default=2, help="Swipe count per trace")
    parser.add_argument("--swipe-duration-ms", type=int, default=75)
    parser.add_argument(
        "--swipe-interval-ms",
        type=int,
        default=3000,
        help="Interval between swipes",
    )
    parser.add_argument(
        "--post-swipe-wait-ms",
        type=int,
        default=5000,
        help="Wait time after last swipe before stopping trace",
    )
    parser.add_argument(
        "--swipe-x-ratio",
        type=float,
        default=0.50,
        help="Swipe X position ratio relative to screen width",
    )
    parser.add_argument(
        "--swipe-start-y-ratio",
        type=float,
        default=0.92,
        help="Swipe start Y ratio relative to screen height (lower is closer to top)",
    )
    parser.add_argument(
        "--swipe-end-y-ratio",
        type=float,
        default=0.54,
        help="Swipe end Y ratio relative to screen height",
    )
    parser.set_defaults(motion_check_enabled=False)
    parser.add_argument(
        "--motion-check",
        dest="motion_check_enabled",
        action="store_true",
        help="Enable screenshot-based motion verification after swipes (disabled by default)",
    )
    parser.add_argument(
        "--no-motion-check",
        dest="motion_check_enabled",
        action="store_false",
        help=argparse.SUPPRESS,
    )
    parser.add_argument(
        "--trace-start-wait-ms",
        type=int,
        default=0,
        help="Delay after trace starts before first swipe",
    )
    parser.add_argument(
        "--case-interval-ms",
        type=int,
        default=1000,
        help="Cooldown interval between test cases (minimum 1000ms)",
    )
    parser.add_argument(
        "--allow-main-entry",
        action="store_true",
        help="Allow fallback via launcher main page when direct entry fails",
    )
    parser.add_argument("--wait-after-launch-ms", type=int, default=4000)
    parser.add_argument("--wait-after-entry-ms", type=int, default=800)
    parser.add_argument("--trace-stop-timeout-sec", type=int, default=45)
    parser.add_argument("--skip-install", action="store_true", help="Skip APK install step")
    parser.add_argument(
        "--include-manual-permission-modules",
        action="store_true",
        help="Include modules that may require manual runtime permission confirmation",
    )
    parser.add_argument(
        "--strict-loads",
        action="store_true",
        help="Treat skipped loads as failure",
    )
    parser.add_argument("--dry-run", action="store_true")
    return parser


def main() -> int:
    script_path = Path(__file__).resolve()
    repo_root = script_path.parent.parent
    parser = build_arg_parser(repo_root)
    args = parser.parse_args()

    for required in (args.perfetto_bin, args.perfetto_config):
        if not required.exists():
            log("ERROR", f"Missing file: {required}")
            return 1

    if not args.apk_dir.exists():
        log("ERROR", f"APK dir not found: {args.apk_dir}")
        return 1

    if args.case_interval_ms < 1000:
        log(
            "WARN",
            f"--case-interval-ms={args.case_interval_ms} is too small; clamp to 1000ms",
        )
        args.case_interval_ms = 1000
    if args.swipe_interval_ms < 0:
        log(
            "WARN",
            f"--swipe-interval-ms={args.swipe_interval_ms} is invalid; clamp to 0ms",
        )
        args.swipe_interval_ms = 0
    if args.post_swipe_wait_ms < 0:
        log(
            "WARN",
            f"--post-swipe-wait-ms={args.post_swipe_wait_ms} is invalid; clamp to 0ms",
        )
        args.post_swipe_wait_ms = 0

    args.trace_dir.mkdir(parents=True, exist_ok=True)
    log("INFO", f"Trace dir: {args.trace_dir}")
    log("INFO", f"Loads: {', '.join(args.loads)}")
    log("INFO", f"Motion check: {'on' if args.motion_check_enabled else 'off'}")
    log("INFO", f"Case interval: {args.case_interval_ms}ms")
    log("INFO", f"Launch stabilize: {args.wait_after_launch_ms}ms")
    log("INFO", f"Swipe interval: {args.swipe_interval_ms}ms")
    log("INFO", f"Post-swipe wait: {args.post_swipe_wait_ms}ms")
    log("INFO", f"Main-entry fallback: {'on' if args.allow_main_entry else 'off'}")
    log("INFO", f"Excluded modules: {', '.join(sorted(DEFAULT_EXCLUDED_MODULES))}")

    adb = ADB(serial=args.serial, dry_run=args.dry_run)
    if not args.dry_run and not adb.check_device():
        log("ERROR", "No connected adb device")
        return 1

    serial = adb.serial or adb.get_serial()
    model = adb.get_model()
    device_tag = sanitize_for_filename(f"{model}-{serial}")
    log("INFO", f"Device: model={model}, serial={serial}")

    modules = discover_modules(
        repo_root=repo_root,
        apk_dir=args.apk_dir,
        only_tokens=args.only_apk,
        include_manual_permission_modules=args.include_manual_permission_modules,
    )
    if not modules:
        log("ERROR", "No matching scrolling debug APKs found")
        return 1

    log("INFO", f"Discovered {len(modules)} scrolling APK(s)")
    for meta in modules:
        log(
            "INFO",
            f"  - {meta.module_name} ({meta.package_name}) "
            f"launcher={meta.launcher_activity} "
            f"activity_type={'yes' if meta.supports_activity_type else 'no'}",
        )

    install_ok_map: Dict[str, bool] = {}
    if not args.skip_install:
        log("INFO", "Installing scrolling APKs ...")
        for meta in modules:
            ok = install_apk(adb, meta)
            install_ok_map[meta.module_name] = ok
            if ok:
                log("SUCCESS", f"Installed: {meta.apk_path.name}")
            else:
                log("ERROR", f"Install failed: {meta.apk_path.name}")
    else:
        for meta in modules:
            install_ok_map[meta.module_name] = True

    results: List[CaptureResult] = []
    module_text_cache: Dict[str, str] = {}
    for meta in modules:
        if not install_ok_map.get(meta.module_name, False):
            for load in args.loads:
                results.append(
                    CaptureResult(
                        module_name=meta.module_name,
                        package_name=meta.package_name,
                        load=load,
                        status="failed",
                        reason="install_failed",
                        trace_path=None,
                        entry_method=None,
                    )
                )
            continue

        for load in args.loads:
            if not module_supports_load(meta, load, module_text_cache):
                results.append(
                    CaptureResult(
                        module_name=meta.module_name,
                        package_name=meta.package_name,
                        load=load,
                        status="skipped",
                        reason="module_does_not_support_load",
                        trace_path=None,
                        entry_method=None,
                    )
                )
                log("WARN", f"Skipped: {meta.module_name}/{load} (module_does_not_support_load)")
                log("INFO", f"Case gap: sleep {args.case_interval_ms}ms")
                time.sleep(args.case_interval_ms / 1000.0)
                continue

            log("INFO", f"Capture: {meta.module_name} / {load}")
            result = run_single_capture(
                adb=adb,
                meta=meta,
                load=load,
                trace_dir=args.trace_dir,
                perfetto_bin=args.perfetto_bin,
                perfetto_config=args.perfetto_config,
                device_tag=device_tag,
                swipe_count=args.swipe_count,
                swipe_duration_ms=args.swipe_duration_ms,
                swipe_interval_sec=args.swipe_interval_ms / 1000.0,
                post_swipe_wait_sec=args.post_swipe_wait_ms / 1000.0,
                swipe_x_ratio=args.swipe_x_ratio,
                swipe_start_y_ratio=args.swipe_start_y_ratio,
                swipe_end_y_ratio=args.swipe_end_y_ratio,
                motion_check_enabled=args.motion_check_enabled,
                allow_main_entry=args.allow_main_entry,
                trace_start_wait_sec=args.trace_start_wait_ms / 1000.0,
                wait_after_launch_sec=args.wait_after_launch_ms / 1000.0,
                wait_after_entry_sec=args.wait_after_entry_ms / 1000.0,
                trace_stop_timeout_sec=args.trace_stop_timeout_sec,
                dry_run=args.dry_run,
            )
            results.append(result)

            if result.status == "success":
                log(
                    "SUCCESS",
                    f"Saved: {result.trace_path} (entry={result.entry_method})",
                )
            elif result.status == "skipped":
                log("WARN", f"Skipped: {meta.module_name}/{load} ({result.reason})")
            else:
                log("ERROR", f"Failed: {meta.module_name}/{load} ({result.reason})")
            log("INFO", f"Case gap: sleep {args.case_interval_ms}ms")
            time.sleep(args.case_interval_ms / 1000.0)

    success = sum(1 for r in results if r.status == "success")
    skipped = sum(1 for r in results if r.status == "skipped")
    failed = sum(1 for r in results if r.status == "failed")
    total = len(results)

    print()
    log("INFO", "=" * 72)
    log("INFO", "Summary")
    log("INFO", "=" * 72)
    log("INFO", f"Total: {total}, Success: {success}, Skipped: {skipped}, Failed: {failed}")
    for r in results:
        trace_str = str(r.trace_path) if r.trace_path else "-"
        log(
            "INFO",
            f"{r.status.upper():7} {r.module_name:34} {r.load:10} {trace_str}",
        )

    if failed > 0:
        return 1
    if args.strict_loads and skipped > 0:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
