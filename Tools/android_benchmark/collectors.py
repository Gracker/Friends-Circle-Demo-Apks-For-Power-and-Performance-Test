from __future__ import annotations

import re
import shlex
from pathlib import Path
from typing import Any, Dict, List, Optional

from .adb import ADB
from .utils import count_lines, parse_float, parse_int, write_csv

def normalize_thermal_temp(raw: Any) -> Optional[float]:
    value = parse_float(raw)
    if value is None:
        return None
    value = abs(value)
    if value > 1000:
        return value / 1000.0
    if value > 150:
        return value / 10.0
    return value


def parse_getprop(text: str) -> Dict[str, str]:
    props: Dict[str, str] = {}
    for line in text.splitlines():
        match = re.match(r"\[(.+?)\]:\s*\[(.*)\]", line.strip())
        if match:
            props[match.group(1)] = match.group(2)
    return props


def parse_meminfo(text: str) -> Dict[str, int]:
    result: Dict[str, int] = {}
    for line in text.splitlines():
        match = re.match(r"^(\w+):\s+(\d+)\s+kB", line)
        if match:
            result[match.group(1)] = int(match.group(2))
    return result


def parse_wm_size(text: str) -> Optional[str]:
    match = re.search(r"Physical size:\s*([0-9]+x[0-9]+)", text)
    return match.group(1) if match else None


def parse_wm_density(text: str) -> Optional[int]:
    match = re.search(r"Physical density:\s*(\d+)", text)
    return int(match.group(1)) if match else None


ANIMATION_SCALE_KEYS = (
    "window_animation_scale",
    "transition_animation_scale",
    "animator_duration_scale",
)


def parse_animation_scales(settings_global_text: str) -> Dict[str, Optional[float]]:
    scales: Dict[str, Optional[float]] = {key: None for key in ANIMATION_SCALE_KEYS}
    for line in settings_global_text.splitlines():
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        if key in scales:
            scales[key] = parse_float(value.strip())
    return scales


def parse_foreground_app(activity_text: str) -> Optional[str]:
    for pattern in (
        r"mResumedActivity:\s+ActivityRecord\{[^ ]+\s+[^ ]+\s+([^\s/]+)/",
        r"mFocusedApp=.*?ActivityRecord\{[^ ]+\s+[^ ]+\s+([^\s/]+)/",
        r"topResumedActivity=ActivityRecord\{[^ ]+\s+[^ ]+\s+([^\s/]+)/",
    ):
        match = re.search(pattern, activity_text)
        if match:
            return match.group(1)
    return None


def parse_data_filesystem(mount_text: str) -> Dict[str, Optional[str]]:
    """Handle two formats:
      - /proc/mounts:  <dev> <target> <fstype> <opts>
      - mount(8):      <dev> on <target> type <fstype> (<opts>)
    Android busybox `mount` emits the latter.
    """
    for line in mount_text.splitlines():
        parts = line.split()
        if len(parts) < 4:
            continue
        if "on" in parts and "type" in parts:
            try:
                on_idx = parts.index("on")
                type_idx = parts.index("type", on_idx + 1)
            except ValueError:
                continue
            target = parts[on_idx + 1] if on_idx + 1 < len(parts) else ""
            fstype = parts[type_idx + 1] if type_idx + 1 < len(parts) else ""
            options = parts[-1] if parts[-1].startswith("(") else " ".join(parts[type_idx + 2:])
        else:
            target = parts[1]
            fstype = parts[2]
            options = parts[3] if len(parts) > 3 else ""
        if target != "/data":
            continue
        noatime = "noatime" in options
        discard = "discard" in options
        return {
            "fstype": fstype,
            "options": options,
            "noatime": noatime,
            "discard": discard,
        }
    return {"fstype": None, "options": None, "noatime": None, "discard": None}


def parse_zram(zram_text: str, swaps_text: str) -> Dict[str, Any]:
    result: Dict[str, Any] = {"enabled": False}
    comp = re.search(r"comp_algorithm=([^\s]+)", zram_text)
    if comp:
        result["comp_algorithm"] = comp.group(1).strip()
    disksize = re.search(r"disksize=(\d+)", zram_text)
    if disksize:
        result["disksize_bytes"] = int(disksize.group(1))
    mm_stat = re.search(r"mm_stat=([^\n]+)", zram_text)
    if mm_stat:
        parts = mm_stat.group(1).split()
        if len(parts) >= 3:
            result["orig_data_bytes"] = parse_int(parts[0])
            result["compr_data_bytes"] = parse_int(parts[1])
            result["mem_used_bytes"] = parse_int(parts[2])
    for line in swaps_text.splitlines():
        if "zram" in line.lower():
            result["enabled"] = True
            break
    return result


def parse_wifi_link(wifi_text: str) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    rssi = re.search(r"RSSI:\s*(-?\d+)", wifi_text)
    if rssi:
        result["rssi_dbm"] = int(rssi.group(1))
    link = re.search(r"(?:Tx )?Link speed:\s*(\d+)", wifi_text)
    if link:
        result["link_mbps"] = int(link.group(1))
    freq = re.search(r"Frequency:\s*(\d+)", wifi_text)
    if freq:
        result["frequency_mhz"] = int(freq.group(1))
    ssid = re.search(r"mWifiInfo\s*SSID:\s*\"([^\"]+)\"", wifi_text)
    if ssid:
        result["ssid_sanitized"] = "***present***"
    return result


def parse_gpu_freq(text: str) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    for line in text.splitlines():
        if "cur_freq=" in line and "gpu_cur_freq_raw" not in result:
            value = line.split("cur_freq=", 1)[1].strip()
            parsed = parse_int(value)
            if parsed is not None:
                result["gpu_cur_freq_raw"] = parsed
                result["gpu_freq_source_line"] = line.strip()[:160]
        if "governor=" in line and "gpu_governor" not in result:
            result["gpu_governor"] = line.split("governor=", 1)[1].strip()
    return result


def parse_vendor_power_mode(getprop_props: Dict[str, str]) -> Dict[str, Any]:
    keys_of_interest = [
        "persist.sys.miui_performance",
        "persist.sys.miui_extreme_power",
        "persist.sys.oplus.game_mode",
        "persist.sys.oppo.performance",
        "persist.sys.vivo.gamemode",
        "persist.vendor.gamemode",
        "persist.vendor.performance.mode",
        "sys.perf.mode",
        "sys.powerctl",
        "ro.config.low_ram",
        "ro.config.per_app_memcg",
    ]
    result: Dict[str, Any] = {}
    for key in keys_of_interest:
        if key in getprop_props:
            result[key] = getprop_props[key]
    return result


def parse_hwui_pipeline(getprop_props: Dict[str, str]) -> Dict[str, Any]:
    def _nonempty(*keys: str) -> Optional[str]:
        for key in keys:
            value = (getprop_props.get(key) or "").strip()
            if value:
                return value
        return None
    return {
        "hwui_renderer": _nonempty("debug.hwui.renderer", "ro.hwui.renderer"),
        "hwui_use_vulkan": _nonempty("debug.hwui.use_vulkan", "ro.hwui.use_vulkan"),
        "skia_pipeline": _nonempty("debug.hwui.skia_pipeline"),
        "renderengine_backend": _nonempty("debug.renderengine.backend"),
    }


def parse_dalvik_vm(getprop_props: Dict[str, str]) -> Dict[str, Any]:
    return {
        "heapgrowthlimit": getprop_props.get("dalvik.vm.heapgrowthlimit"),
        "heapmaxfree": getprop_props.get("dalvik.vm.heapmaxfree"),
        "heapsize": getprop_props.get("dalvik.vm.heapsize"),
        "image_dex2oat_filter": getprop_props.get("dalvik.vm.image-dex2oat-filter"),
        "dex2oat_filter": getprop_props.get("dalvik.vm.dex2oat-filter"),
    }


def parse_battery_dump(text: str) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    key_map = {
        "level": "level_pct",
        "scale": "scale",
        "temperature": "temperature_raw",
        "voltage": "voltage_mv",
        "status": "status",
        "health": "health",
        "AC powered": "ac_powered",
        "USB powered": "usb_powered",
        "Wireless powered": "wireless_powered",
    }
    for line in text.splitlines():
        if ":" not in line:
            continue
        key, value = [x.strip() for x in line.split(":", 1)]
        mapped = key_map.get(key)
        if not mapped:
            continue
        if value.lower() in ("true", "false"):
            result[mapped] = value.lower() == "true"
        else:
            parsed = parse_int(value)
            result[mapped] = parsed if parsed is not None else value
    temp_raw = result.get("temperature_raw")
    if temp_raw is not None:
        result["temperature_c"] = round(float(temp_raw) / 10.0, 1)
    return result


def collect_cpu_topology(adb: ADB) -> List[Dict[str, Any]]:
    script = r"""
for d in /sys/devices/system/cpu/cpu[0-9]*; do
  [ -d "$d" ] || continue
  cpu="${d##*/}"
  online="$(cat "$d/online" 2>/dev/null || echo 1)"
  cur="$(cat "$d/cpufreq/scaling_cur_freq" 2>/dev/null || true)"
  min="$(cat "$d/cpufreq/cpuinfo_min_freq" 2>/dev/null || cat "$d/cpufreq/scaling_min_freq" 2>/dev/null || true)"
  max="$(cat "$d/cpufreq/cpuinfo_max_freq" 2>/dev/null || cat "$d/cpufreq/scaling_max_freq" 2>/dev/null || true)"
  gov="$(cat "$d/cpufreq/scaling_governor" 2>/dev/null || true)"
  echo "$cpu,$online,$cur,$min,$max,$gov"
done
"""
    rows: List[Dict[str, Any]] = []
    out = adb.shell_out(script, timeout=20)
    for line in out.splitlines():
        parts = line.strip().split(",", 5)
        if len(parts) < 6:
            continue
        rows.append(
            {
                "cpu": parts[0],
                "online": parts[1],
                "scaling_cur_khz": parse_int(parts[2]),
                "min_khz": parse_int(parts[3]),
                "max_khz": parse_int(parts[4]),
                "governor": parts[5],
            }
        )
    return rows


def collect_thermal_zones(adb: ADB) -> Dict[str, Optional[float]]:
    script = r"""
for z in /sys/class/thermal/thermal_zone*; do
  [ -d "$z" ] || continue
  name="$(cat "$z/type" 2>/dev/null || echo "${z##*/}")"
  temp="$(cat "$z/temp" 2>/dev/null || true)"
  echo "$name=$temp"
done
"""
    zones: Dict[str, Optional[float]] = {}
    out = adb.shell_out(script, timeout=20)
    seen: Dict[str, int] = {}
    for line in out.splitlines():
        if "=" not in line:
            continue
        name, raw = line.split("=", 1)
        base = name.strip() or "thermal"
        seen[base] = seen.get(base, 0) + 1
        key = base if seen[base] == 1 else f"{base}_{seen[base]}"
        zones[key] = normalize_thermal_temp(raw)
    if not zones:
        zones = collect_thermalservice_temperatures(adb)
    return zones


def collect_thermalservice_temperatures(adb: ADB) -> Dict[str, Optional[float]]:
    out = adb.shell_out("dumpsys thermalservice", timeout=20)
    if "Current temperatures from HAL:" in out:
        out = out.split("Current temperatures from HAL:", 1)[1]
        out = out.split("Current cooling devices from HAL:", 1)[0]
    zones: Dict[str, Optional[float]] = {}
    seen: Dict[str, int] = {}
    for value, name in re.findall(r"Temperature\{mValue=([0-9.+-]+),.*?mName=([^,}]+)", out):
        base = name.strip() or "thermalservice"
        seen[base] = seen.get(base, 0) + 1
        key = base if seen[base] == 1 else f"{base}_{seen[base]}"
        zones[key] = normalize_thermal_temp(value)
    return zones


def collect_current_sysfs(adb: ADB) -> Dict[str, Any]:
    paths = {
        "current_now_ua": "/sys/class/power_supply/battery/current_now",
        "voltage_now_uv": "/sys/class/power_supply/battery/voltage_now",
        "charge_full_uah": "/sys/class/power_supply/battery/charge_full",
        "charge_full_design_uah": "/sys/class/power_supply/battery/charge_full_design",
        "capacity_pct": "/sys/class/power_supply/battery/capacity",
    }
    result: Dict[str, Any] = {}
    for key, path in paths.items():
        out = adb.shell_out(f"cat {shlex.quote(path)} 2>/dev/null", timeout=10)
        value = parse_int(out)
        if value is not None:
            result[key] = value
    return result


def collect_device_info(adb: ADB, device_dir: Path) -> Dict[str, Any]:
    raw_dir = device_dir / "raw"
    raw_dir.mkdir(parents=True, exist_ok=True)
    raw_commands = {
        "getprop.txt": "getprop",
        "cpuinfo.txt": "cat /proc/cpuinfo",
        "meminfo.txt": "cat /proc/meminfo",
        "proc_version.txt": "cat /proc/version",
        "proc_uptime.txt": "cat /proc/uptime",
        "proc_loadavg.txt": "cat /proc/loadavg",
        "proc_stat.txt": "cat /proc/stat",
        "proc_vmstat.txt": "cat /proc/vmstat",
        "proc_diskstats.txt": "cat /proc/diskstats",
        "proc_mounts.txt": "cat /proc/mounts",
        "proc_partitions.txt": "cat /proc/partitions",
        "proc_buddyinfo.txt": "cat /proc/buddyinfo",
        "proc_zoneinfo.txt": "cat /proc/zoneinfo",
        "proc_pressure_cpu.txt": "cat /proc/pressure/cpu 2>/dev/null",
        "proc_pressure_memory.txt": "cat /proc/pressure/memory 2>/dev/null",
        "proc_pressure_io.txt": "cat /proc/pressure/io 2>/dev/null",
        "proc_net_dev.txt": "cat /proc/net/dev",
        "proc_interrupts.txt": "cat /proc/interrupts 2>/dev/null",
        "proc_slabinfo.txt": "cat /proc/slabinfo 2>/dev/null",
        "df_data.txt": "df -k /data",
        "df_all.txt": "df -k",
        "mount.txt": "mount",
        "settings_system.txt": "settings list system",
        "settings_global.txt": "settings list global",
        "settings_secure.txt": "settings list secure",
        "pm_features.txt": "pm list features",
        "pm_packages_f.txt": "pm list packages -f",
        "wm_size.txt": "wm size",
        "wm_density.txt": "wm density",
        "settings_refresh_rate.txt": (
            "echo peak=$(settings get system peak_refresh_rate 2>/dev/null); "
            "echo min=$(settings get system min_refresh_rate 2>/dev/null)"
        ),
        "dumpsys_display.txt": "dumpsys display",
        "dumpsys_battery.txt": "dumpsys battery",
        "dumpsys_batterystats_checkin.txt": "dumpsys batterystats --checkin",
        "dumpsys_batteryproperties.txt": "dumpsys batteryproperties",
        "dumpsys_cpuinfo.txt": "dumpsys cpuinfo",
        "dumpsys_meminfo.txt": "dumpsys meminfo",
        "dumpsys_procstats.txt": "dumpsys procstats --hours 1",
        "dumpsys_activity.txt": "dumpsys activity",
        "dumpsys_window.txt": "dumpsys window",
        "dumpsys_input.txt": "dumpsys input",
        "dumpsys_gfxinfo.txt": "dumpsys gfxinfo",
        "dumpsys_power.txt": "dumpsys power",
        "dumpsys_deviceidle.txt": "dumpsys deviceidle",
        "dumpsys_connectivity.txt": "dumpsys connectivity",
        "dumpsys_wifi.txt": "dumpsys wifi",
        "dumpsys_netstats.txt": "dumpsys netstats",
        "dumpsys_telephony_registry.txt": "dumpsys telephony.registry",
        "dumpsys_usagestats.txt": "dumpsys usagestats",
        "dumpsys_sensorservice.txt": "dumpsys sensorservice",
        "dumpsys_thermalservice.txt": "dumpsys thermalservice",
        "dumpsys_surfaceflinger.txt": "dumpsys SurfaceFlinger",
        "dumpsys_audio_flinger.txt": "dumpsys media.audio_flinger",
        "dumpsys_audio_policy.txt": "dumpsys media.audio_policy",
        "sys_cpu_snapshot.txt": (
            "for d in /sys/devices/system/cpu/cpu[0-9]*; do "
            "echo ===$d===; "
            "cat $d/online 2>/dev/null; "
            "for f in scaling_cur_freq scaling_min_freq scaling_max_freq cpuinfo_min_freq cpuinfo_max_freq scaling_governor related_cpus affected_cpus; do "
            "echo $f=$(cat $d/cpufreq/$f 2>/dev/null); "
            "done; "
            "done"
        ),
        "sys_thermal_snapshot.txt": (
            "for z in /sys/class/thermal/thermal_zone*; do "
            "echo ===$z===; cat $z/type 2>/dev/null; cat $z/temp 2>/dev/null; cat $z/policy 2>/dev/null; "
            "done"
        ),
        "sys_block_snapshot.txt": (
            "for b in /sys/block/*; do "
            "echo ===$b===; "
            "for f in queue/rotational queue/scheduler queue/read_ahead_kb queue/nr_requests size removable; do "
            "echo $f=$(cat $b/$f 2>/dev/null); "
            "done; "
            "done"
        ),
        "sys_power_supply_snapshot.txt": (
            "for p in /sys/class/power_supply/*; do "
            "echo ===$p===; "
            "for f in type status health capacity current_now voltage_now charge_full charge_full_design temp; do "
            "echo $f=$(cat $p/$f 2>/dev/null); "
            "done; "
            "done"
        ),
        "selinux.txt": "getenforce 2>/dev/null || echo unknown",
        "sys_zram_snapshot.txt": (
            "for z in /sys/block/zram*; do "
            "[ -d \"$z\" ] || continue; "
            "echo ===$z===; "
            "for f in comp_algorithm disksize mm_stat initstate compact_count; do "
            "echo $f=$(cat $z/$f 2>/dev/null); "
            "done; "
            "done"
        ),
        "proc_swaps.txt": "cat /proc/swaps 2>/dev/null || true",
        "proc_sys_vm.txt": (
            "for f in swappiness dirty_ratio dirty_background_ratio dirty_expire_centisecs "
            "dirty_writeback_centisecs overcommit_memory min_free_kbytes watermark_scale_factor; do "
            "echo $f=$(cat /proc/sys/vm/$f 2>/dev/null); "
            "done"
        ),
        "mglru.txt": (
            "echo enabled=$(cat /sys/kernel/mm/lru_gen/enabled 2>/dev/null); "
            "echo min_ttl=$(cat /sys/kernel/mm/lru_gen/min_ttl_ms 2>/dev/null)"
        ),
        "proc_sys_kernel_sched.txt": (
            "for f in /proc/sys/kernel/sched_*; do "
            "[ -f \"$f\" ] || continue; "
            "echo $(basename $f)=$(cat $f 2>/dev/null); "
            "done"
        ),
        "cpufreq_schedutil.txt": (
            "for p in /sys/devices/system/cpu/cpufreq/policy*; do "
            "[ -d \"$p/schedutil\" ] || continue; "
            "echo ===$p===; "
            "for f in rate_limit_us up_rate_limit_us down_rate_limit_us hispeed_freq hispeed_load; do "
            "echo $f=$(cat $p/schedutil/$f 2>/dev/null); "
            "done; "
            "done"
        ),
        "gpu_freq.txt": (
            "for d in /sys/class/kgsl/kgsl-3d0 /sys/class/devfreq/*gpu* /sys/class/devfreq/*.mali "
            "/sys/devices/platform/*.mali /sys/kernel/gpu; do "
            "[ -d \"$d\" ] || continue; "
            "echo ===$d===; "
            "for f in cur_freq clock_mhz governor min_freq max_freq available_frequencies devfreq/cur_freq gpuclk busy_percentage; do "
            "echo $f=$(cat $d/$f 2>/dev/null); "
            "done; "
            "done"
        ),
        "cpu_idle_usage.txt": (
            "for d in /sys/devices/system/cpu/cpu[0-9]*; do "
            "[ -d \"$d/cpuidle\" ] || continue; "
            "echo ===$d===; "
            "for s in $d/cpuidle/state*; do "
            "[ -d \"$s\" ] || continue; "
            "echo $(basename $s)_name=$(cat $s/name 2>/dev/null); "
            "echo $(basename $s)_usage=$(cat $s/usage 2>/dev/null); "
            "done; "
            "done"
        ),
        "storage_identity.txt": (
            "for d in /sys/block/sd* /sys/block/mmcblk*; do "
            "[ -d \"$d\" ] || continue; "
            "echo ===$d===; "
            "for f in device/model device/vendor device/rev size removable; do "
            "echo $f=$(cat $d/$f 2>/dev/null); "
            "done; "
            "done; "
            "ls -1 /proc/scsi/ufs/ 2>/dev/null; "
            "cat /proc/scsi/ufs/* 2>/dev/null"
        ),
        "dumpsys_game.txt": "dumpsys game 2>/dev/null || true",
        "dumpsys_jobscheduler.txt": "dumpsys jobscheduler 2>/dev/null | head -200 || true",
        "foreground_state.txt": (
            "dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|mFocusedApp|topResumedActivity' | head -20"
        ),
        "cmd_wifi_status.txt": "cmd wifi status 2>/dev/null || dumpsys wifi 2>/dev/null | head -80",
        "choreographer_log.txt": "logcat -d -s Choreographer:I '*:S' 2>/dev/null | tail -200 || true",
    }
    raw_outputs: Dict[str, str] = {}
    for filename, command in raw_commands.items():
        out = adb.shell_out(command, timeout=45)
        raw_outputs[filename] = out
        (raw_dir / filename).write_text(out + "\n", encoding="utf-8")

    props = parse_getprop(raw_outputs.get("getprop.txt", ""))
    write_csv(
        device_dir / "details" / "getprop.csv",
        [{"key": key, "value": value} for key, value in sorted(props.items())],
        ["key", "value"],
    )
    proc_index = [
        {
            "file": filename,
            "bytes": len(text.encode("utf-8")),
            "lines": count_lines(text),
        }
        for filename, text in sorted(raw_outputs.items())
        if filename.startswith("proc_") or filename in ("cpuinfo.txt", "meminfo.txt")
    ]
    write_csv(device_dir / "details" / "proc_index.csv", proc_index, ["file", "bytes", "lines"])
    meminfo = parse_meminfo(raw_outputs.get("meminfo.txt", ""))
    cpu_topology = collect_cpu_topology(adb)
    thermal_zones = collect_thermal_zones(adb)
    battery = parse_battery_dump(raw_outputs.get("dumpsys_battery.txt", ""))
    battery.update(collect_current_sysfs(adb))

    refresh = {}
    for line in raw_outputs.get("settings_refresh_rate.txt", "").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            refresh[key] = value.strip()

    display_text = raw_outputs.get("dumpsys_display.txt", "")
    surface_text = raw_outputs.get("dumpsys_surfaceflinger.txt", "")
    gpu_renderer = None
    for pattern in (
        r"GLES:\s*([^,\n]+(?:,\s*[^,\n]+){0,2})",
        r"OpenGL ES.*?renderer\s*=\s*([^\n]+)",
    ):
        match = re.search(pattern, surface_text, re.IGNORECASE)
        if match:
            gpu_renderer = match.group(1).strip()
            break

    animation_scales = parse_animation_scales(raw_outputs.get("settings_global.txt", ""))
    foreground_app = parse_foreground_app(raw_outputs.get("foreground_state.txt", "") or raw_outputs.get("dumpsys_activity.txt", ""))
    data_fs = parse_data_filesystem(raw_outputs.get("mount.txt", ""))
    zram = parse_zram(raw_outputs.get("sys_zram_snapshot.txt", ""), raw_outputs.get("proc_swaps.txt", ""))
    wifi_link = parse_wifi_link(raw_outputs.get("cmd_wifi_status.txt", "") or raw_outputs.get("dumpsys_wifi.txt", ""))
    gpu_freq = parse_gpu_freq(raw_outputs.get("gpu_freq.txt", ""))
    vendor_power = parse_vendor_power_mode(props)
    hwui = parse_hwui_pipeline(props)
    dalvik_vm = parse_dalvik_vm(props)
    selinux_mode = (raw_outputs.get("selinux.txt", "").strip().splitlines() or ["unknown"])[0].strip() or "unknown"

    mglru_enabled = None
    for line in raw_outputs.get("mglru.txt", "").splitlines():
        if line.startswith("enabled="):
            mglru_enabled = line.split("=", 1)[1].strip() or None
            break

    vm_tuning: Dict[str, Any] = {}
    for line in raw_outputs.get("proc_sys_vm.txt", "").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            vm_tuning[key.strip()] = parse_int(value) if value.strip().lstrip("-").isdigit() else value.strip()

    device_info = {
        "serial": adb.get_serial(),
        "model": props.get("ro.product.model") or adb.get_model(),
        "manufacturer": props.get("ro.product.manufacturer"),
        "brand": props.get("ro.product.brand"),
        "device": props.get("ro.product.device"),
        "android_release": props.get("ro.build.version.release"),
        "sdk": props.get("ro.build.version.sdk"),
        "build_fingerprint": props.get("ro.build.fingerprint"),
        "soc_model": props.get("ro.soc.model") or props.get("ro.board.platform") or props.get("ro.hardware"),
        "abi": props.get("ro.product.cpu.abi"),
        "cpu_count": len(cpu_topology),
        "memory_total_mb": round(meminfo.get("MemTotal", 0) / 1024, 1) if meminfo.get("MemTotal") else None,
        "memory_available_mb": round(meminfo.get("MemAvailable", 0) / 1024, 1) if meminfo.get("MemAvailable") else None,
        "memory_free_mb": round(meminfo.get("MemFree", 0) / 1024, 1) if meminfo.get("MemFree") else None,
        "screen_size": parse_wm_size(raw_outputs.get("wm_size.txt", "")),
        "density_dpi": parse_wm_density(raw_outputs.get("wm_density.txt", "")),
        "peak_refresh_rate": refresh.get("peak"),
        "min_refresh_rate": refresh.get("min"),
        "gpu_renderer": gpu_renderer,
        "battery": battery,
        "cpu_topology": cpu_topology,
        "thermal_zones": thermal_zones,
        "display_summary": summarize_display(display_text),
        "raw_dir": str(raw_dir),
        "selinux_mode": selinux_mode,
        "hwui": hwui,
        "dalvik_vm": dalvik_vm,
        "animation_scales": animation_scales,
        "foreground_package": foreground_app,
        "storage": {"data_fs": data_fs},
        "mm": {
            "zram": zram,
            "mglru_enabled": mglru_enabled,
            "vm_tuning": vm_tuning,
        },
        "gpu_freq": gpu_freq,
        "wifi_link": wifi_link,
        "vendor_power_mode": vendor_power,
    }
    write_csv(device_dir / "details" / "cpu_topology.csv", cpu_topology)
    thermal_rows = [{"zone": key, "temp_c": value} for key, value in thermal_zones.items()]
    write_csv(device_dir / "details" / "thermal_baseline.csv", thermal_rows, ["zone", "temp_c"])
    return device_info


def summarize_display(text: str) -> Dict[str, Any]:
    summary: Dict[str, Any] = {}
    refresh_rates = sorted({float(x) for x in re.findall(r"fps=([0-9]+(?:\.[0-9]+)?)", text)})
    if refresh_rates:
        summary["reported_refresh_rates"] = refresh_rates
    brightness = re.search(r"brightness\s*=\s*([0-9.]+)", text, re.IGNORECASE)
    if brightness:
        summary["brightness"] = brightness.group(1)
    return summary
