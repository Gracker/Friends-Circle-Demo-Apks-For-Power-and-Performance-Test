from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional

from .adb import ADB
from .collectors import ANIMATION_SCALE_KEYS, parse_battery_dump
from .utils import log, parse_float


ANIMATION_KEYS = ANIMATION_SCALE_KEYS


@dataclass
class EnvSnapshot:
    stayon: Optional[str] = None
    screen_off_timeout: Optional[str] = None
    screen_brightness_mode: Optional[str] = None
    screen_brightness: Optional[str] = None
    animation_scales: Dict[str, Optional[str]] = field(default_factory=dict)
    zen_mode: Optional[str] = None
    applied: Dict[str, Any] = field(default_factory=dict)
    notes: List[str] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "stayon": self.stayon,
            "screen_off_timeout": self.screen_off_timeout,
            "screen_brightness_mode": self.screen_brightness_mode,
            "screen_brightness": self.screen_brightness,
            "animation_scales": self.animation_scales,
            "zen_mode": self.zen_mode,
            "applied": self.applied,
            "notes": self.notes,
        }


def _get_setting(adb: ADB, namespace: str, key: str) -> Optional[str]:
    out = adb.shell_out(f"settings get {namespace} {key} 2>/dev/null", timeout=10).strip()
    if not out or out.lower() == "null":
        return None
    return out


def _put_setting(adb: ADB, namespace: str, key: str, value: str) -> None:
    adb.shell(f"settings put {namespace} {key} {value}", timeout=10)


def _delete_setting(adb: ADB, namespace: str, key: str) -> None:
    adb.shell(f"settings delete {namespace} {key}", timeout=10)


def _restore_setting(adb: ADB, namespace: str, key: str, original: Optional[str]) -> None:
    if original is None:
        _delete_setting(adb, namespace, key)
    else:
        _put_setting(adb, namespace, key, original)


def read_battery_gate(adb: ADB) -> Dict[str, Any]:
    sample = parse_battery_dump(adb.shell_out("dumpsys battery 2>/dev/null", timeout=15))
    ac = bool(sample.get("ac_powered"))
    usb = bool(sample.get("usb_powered"))
    wireless = bool(sample.get("wireless_powered"))
    return {
        "charging": ac or usb or wireless,
        "ac": ac,
        "usb": usb,
        "wireless": wireless,
        "level_pct": sample.get("level_pct"),
    }


def prepare_environment(adb: ADB, args: Any) -> EnvSnapshot:
    snap = EnvSnapshot()
    if adb.dry_run:
        snap.notes.append("dry-run: environment not actually touched")
        return snap

    animation_scale = parse_float(getattr(args, "animation_scale", None))
    if getattr(args, "animations_off", False):
        animation_scale = 0.0
    if animation_scale is None:
        animation_scale = 1.0
    animation_scale = max(0.0, animation_scale)

    brightness = getattr(args, "screen_brightness", 128)
    if not getattr(args, "no_env_stabilize", False):
        snap.stayon = _get_setting(adb, "global", "stay_on_while_plugged_in")
        snap.screen_off_timeout = _get_setting(adb, "system", "screen_off_timeout")
        snap.screen_brightness_mode = _get_setting(adb, "system", "screen_brightness_mode")
        snap.screen_brightness = _get_setting(adb, "system", "screen_brightness")
        snap.zen_mode = _get_setting(adb, "global", "zen_mode")
        for key in ANIMATION_KEYS:
            snap.animation_scales[key] = _get_setting(adb, "global", key)

        adb.shell("svc power stayon usb", timeout=10)
        _put_setting(adb, "global", "stay_on_while_plugged_in", "3")
        _put_setting(adb, "system", "screen_off_timeout", "1800000")
        adb.shell("input keyevent KEYCODE_WAKEUP", timeout=10)
        adb.shell("wm dismiss-keyguard", timeout=10)
        _put_setting(adb, "system", "screen_brightness_mode", "0")
        _put_setting(adb, "system", "screen_brightness", str(int(brightness)))
        for key in ANIMATION_KEYS:
            _put_setting(adb, "global", key, f"{animation_scale:.2f}")
        if getattr(args, "dnd_on", False):
            _put_setting(adb, "global", "zen_mode", "1")
        adb.shell("logcat -c", timeout=10)

        snap.applied = {
            "stay_on_while_plugged_in": "3",
            "screen_off_timeout_ms": 1800000,
            "screen_brightness_mode": 0,
            "screen_brightness": int(brightness),
            "animation_scale": animation_scale,
            "zen_mode": "1" if getattr(args, "dnd_on", False) else "unchanged",
        }
    else:
        snap.notes.append("env stabilization disabled via --no-env-stabilize")

    return snap


def restore_environment(adb: ADB, snapshot: EnvSnapshot) -> None:
    if adb.dry_run:
        return
    if not snapshot.applied:
        return
    _restore_setting(adb, "global", "stay_on_while_plugged_in", snapshot.stayon)
    _restore_setting(adb, "system", "screen_off_timeout", snapshot.screen_off_timeout)
    _restore_setting(adb, "system", "screen_brightness_mode", snapshot.screen_brightness_mode)
    _restore_setting(adb, "system", "screen_brightness", snapshot.screen_brightness)
    for key in ANIMATION_KEYS:
        _restore_setting(adb, "global", key, snapshot.animation_scales.get(key))
    if snapshot.applied.get("zen_mode") == "1":
        _restore_setting(adb, "global", "zen_mode", snapshot.zen_mode)


def read_max_thermal_temp(adb: ADB) -> Optional[float]:
    from .collectors import collect_thermal_zones
    try:
        zones = collect_thermal_zones(adb)
    except Exception:
        return None
    temps = [v for v in zones.values() if v is not None]
    return max(temps) if temps else None


COOLDOWN_STATUS_DISABLED = "disabled"
COOLDOWN_STATUS_REACHED = "reached"
COOLDOWN_STATUS_TIMEOUT = "timeout"
COOLDOWN_STATUS_UNAVAILABLE = "unavailable"
COOLDOWN_STATUS_ERROR = "error"
COOLDOWN_STATUS_SKIPPED_NO_HEAT = "skipped_no_heat"

# Skip cooldown when the previous stage produced no heat. These match the
# literal status strings the rest of the pipeline emits for non-running stages.
_COOLDOWN_SKIP_PREV = {"skipped", "dry_run", "failed"}


def _cool_to_ceiling(
    adb: ADB,
    target_ceiling_c: float,
    max_wait_sec: int,
    dry_run: bool,
    label: str,
) -> Dict[str, Any]:
    if dry_run or max_wait_sec <= 0:
        return {
            "label": label,
            "waited_sec": 0.0,
            "start_max_c": None,
            "end_max_c": None,
            "status": COOLDOWN_STATUS_DISABLED,
        }
    start = time.monotonic()
    start_temp: Optional[float] = None
    try:
        while True:
            end_temp = read_max_thermal_temp(adb)
            if start_temp is None:
                start_temp = end_temp
            elapsed = time.monotonic() - start
            if end_temp is None:
                return {
                    "label": label,
                    "waited_sec": round(elapsed, 2),
                    "start_max_c": start_temp,
                    "end_max_c": None,
                    "status": COOLDOWN_STATUS_UNAVAILABLE,
                }
            if end_temp <= target_ceiling_c:
                return {
                    "label": label,
                    "waited_sec": round(elapsed, 2),
                    "start_max_c": start_temp,
                    "end_max_c": end_temp,
                    "status": COOLDOWN_STATUS_REACHED,
                }
            if elapsed >= max_wait_sec:
                log("WARN",
                    f"cooldown[{label}] did not reach {target_ceiling_c}C "
                    f"after {int(elapsed)}s (cur={end_temp}C)")
                return {
                    "label": label,
                    "waited_sec": round(elapsed, 2),
                    "start_max_c": start_temp,
                    "end_max_c": end_temp,
                    "status": COOLDOWN_STATUS_TIMEOUT,
                }
            time.sleep(3.0)
    except Exception as exc:
        log("WARN", f"cooldown[{label}] errored: {exc}")
        return {
            "label": label,
            "waited_sec": round(time.monotonic() - start, 2),
            "start_max_c": start_temp,
            "end_max_c": None,
            "status": COOLDOWN_STATUS_ERROR,
        }


def thermal_precool(adb: ADB, target_ceiling_c: float, max_wait_sec: int, dry_run: bool) -> Dict[str, Any]:
    rec = _cool_to_ceiling(adb, target_ceiling_c, max_wait_sec, dry_run, label="precool")
    return {
        "waited_sec": rec["waited_sec"],
        "start_max_c": rec["start_max_c"],
        "end_max_c": rec["end_max_c"],
        "reached_ceiling": rec["status"] == COOLDOWN_STATUS_REACHED,
    }


def inter_stage_cooldown(
    adb: ADB,
    label: str,
    *,
    max_wait_sec: int,
    ceiling_c: float,
    min_wait_sec: int,
    dry_run: bool,
    prev_status: Optional[str] = None,
) -> Dict[str, Any]:
    if dry_run or max_wait_sec <= 0:
        return {"label": label, "status": COOLDOWN_STATUS_DISABLED, "waited_sec": 0.0,
                "start_max_c": None, "end_max_c": None}
    if prev_status in _COOLDOWN_SKIP_PREV:
        return {"label": label, "status": COOLDOWN_STATUS_SKIPPED_NO_HEAT, "waited_sec": 0.0,
                "start_max_c": None, "end_max_c": None,
                "prev_status": prev_status}

    min_wait = max(0, min_wait_sec)
    if min_wait > 0:
        time.sleep(min_wait)

    rec = _cool_to_ceiling(adb, ceiling_c, max_wait_sec, dry_run, label=label)
    rec["waited_sec"] = round(rec.get("waited_sec", 0.0) + min_wait, 2)
    rec["min_wait_sec"] = min_wait
    log("INFO",
        f"cooldown[{label}] status={rec['status']} waited={rec['waited_sec']}s "
        f"start={rec.get('start_max_c')}C end={rec.get('end_max_c')}C")
    return rec
