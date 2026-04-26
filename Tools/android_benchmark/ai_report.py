"""LLM-friendly comparison report.

Produces `ai_report.md` — a token-efficient, semantically self-explanatory
summary of one or more device runs. Designed to paste directly into a chat
context for analysis, with:
  - device profiles (one block each)
  - configuration diffs (only fields that vary across devices)
  - per-category active-metric comparison + percentage delta
  - per-category ASCII bar charts
  - notable findings (auto-generated heuristics)
  - skipped/advisory inventory

Single-device runs still produce a useful report — diffs and comparisons
sections are short-circuited with a note.
"""
from __future__ import annotations

import math
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence

from .analysis import interpret_findings, metric_dependency_hint
from .reports import (
    SCORE_WEIGHTS,
    compute_category_aggregates,
    compute_config_diffs,
    format_ascii_bar,
    pct_diff,
)
from .utils import parse_float


_SEVERITY_TAG = {3: "🔴 严重", 2: "🟠 关注", 1: "🟡 提示", 0: "ℹ️  信息"}


def _device_profile_block(device_result: Dict[str, Any]) -> str:
    info = device_result.get("device_info", {}) or {}
    env = (device_result.get("env", {}) or {}).get("applied") or {}
    battery_gate = device_result.get("battery_gate", {}) or {}
    hwui = info.get("hwui") or {}
    data_fs = (info.get("storage") or {}).get("data_fs") or {}
    fs_opts = []
    if data_fs.get("noatime"): fs_opts.append("noatime")
    if data_fs.get("discard"): fs_opts.append("discard")
    if "fsync_mode=nobarrier" in (data_fs.get("options") or ""):
        fs_opts.append("nobarrier")
    fs_summary = data_fs.get("fstype") or "?"
    if fs_opts:
        fs_summary += f" ({', '.join(fs_opts)})"
    hwui_pipe = hwui.get("hwui_renderer") or hwui.get("renderengine_backend") or "?"
    if hwui.get("hwui_use_vulkan") in ("1", "true", "True"):
        hwui_pipe += " (vulkan)"
    charging_state = "unplugged"
    if battery_gate.get("ac"): charging_state = "AC"
    elif battery_gate.get("usb"): charging_state = "USB"
    elif battery_gate.get("wireless"): charging_state = "Wireless"

    lines = [f"### {device_result['device_label']}"]
    lines.append(f"- **soc**: {info.get('soc_model')} / cores={info.get('cpu_count')} / "
                 f"ram={info.get('memory_total_mb')} MiB")
    lines.append(f"- **android**: {info.get('android_release')} (sdk {info.get('sdk')}); "
                 f"selinux={info.get('selinux_mode')}")
    lines.append(f"- **graphics**: {hwui_pipe}; refresh peak={info.get('peak_refresh_rate')} "
                 f"min={info.get('min_refresh_rate')}")
    lines.append(f"- **storage**: {fs_summary}")
    lines.append(f"- **memory**: zram_enabled={(info.get('mm') or {}).get('zram', {}).get('enabled')}; "
                 f"mglru={(info.get('mm') or {}).get('mglru_enabled')}")
    lines.append(f"- **env applied**: stayon={env.get('stay_on_while_plugged_in')}, "
                 f"brightness={env.get('screen_brightness')}, anim={env.get('animation_scale')}, "
                 f"dnd={env.get('zen_mode')}")
    lines.append(f"- **battery at start**: {battery_gate.get('level_pct')}% ({charging_state})")
    lines.append(f"- **foreground at start**: {info.get('foreground_package')}")
    return "\n".join(lines)


def _category_comparison_block(category: str, agg: Dict[str, Any], device_results: Sequence[Dict[str, Any]]) -> str:
    lines = [f"### {category} (weight={SCORE_WEIGHTS.get(category, '-')})"]
    devices = agg.get("devices") or {}
    if not devices:
        lines.append("_no active metrics in this category._")
        return "\n".join(lines)
    norm_values = [(label, info.get("normalized")) for label, info in devices.items()]
    norm_values_filtered = [(l, v) for l, v in norm_values if v is not None]
    if not norm_values_filtered:
        lines.append("_no comparable metrics across devices._")
        return "\n".join(lines)
    peak = max(v for _, v in norm_values_filtered)
    lines.append("")
    lines.append("```")
    for label, info in devices.items():
        v = info.get("normalized")
        bar = format_ascii_bar(v, peak, width=24) if v is not None else " " * 24
        v_str = f"{v:6.2f}" if v is not None else "  -   "
        lines.append(f"  {label:<32s} {bar} {v_str}  (n={info.get('metrics_counted')})")
    lines.append("```")
    metric_keys = agg.get("metrics") or []
    if metric_keys:
        labels = [d["device_label"] for d in device_results]
        rows: List[List[str]] = []
        for key in metric_keys:
            for d in device_results:
                row_meta = next((m for m in d.get("metrics", []) if m["key"] == key), None)
                if not row_meta or not row_meta.get("valid_for_score", False):
                    continue
                values = {dd["device_label"]: parse_float(next(
                    (m["value"] for m in dd.get("metrics", []) if m["key"] == key), None
                )) for dd in device_results}
                direction = row_meta["direction"]
                if direction == "lower":
                    best = min((v for v in values.values() if v is not None), default=None)
                else:
                    best = max((v for v in values.values() if v is not None), default=None)
                row = [key, row_meta.get("unit") or "", direction]
                for label in labels:
                    val = values.get(label)
                    if val is None:
                        row.append("-")
                    else:
                        delta = pct_diff(val, best, direction) if best is not None else None
                        if delta is None or delta == 0:
                            row.append(f"{val:.3g}")
                        else:
                            row.append(f"{val:.3g} ({delta:+.1f}%)")
                rows.append(row)
                break
        if rows:
            header = ["metric", "unit", "dir"] + labels
            lines.append("")
            lines.append("| " + " | ".join(header) + " |")
            lines.append("| " + " | ".join("---" for _ in header) + " |")
            for row in rows:
                lines.append("| " + " | ".join(row) + " |")
    return "\n".join(lines)


def _findings(payload: Dict[str, Any], category_aggs: Dict[str, Any], config_diffs: List[Dict[str, Any]]) -> List[str]:
    """Heuristic findings — short bulleted list for the LLM to start from."""
    findings: List[str] = []
    devices = payload.get("devices", []) or []
    labels = [d["device_label"] for d in devices]
    if len(labels) < 2:
        findings.append("- Single-device run: no cross-device deltas computed; metric absolutes only.")
    # Charging warnings
    for d in devices:
        if (d.get("battery_gate") or {}).get("charging"):
            findings.append(f"- `{d['device_label']}` was **charging** during the run; battery stage is advisory.")
    # Config-driven hints
    diff_fields = {row["field"] for row in config_diffs}
    if "hwui.renderengine_backend" in diff_fields:
        findings.append("- Devices use **different graphics pipelines** (skia-gl vs skia-vulkan); "
                        "scroll/launch deltas can be partly attributed to that.")
    if any(f.startswith("env.animation_scale") for f in diff_fields):
        findings.append("- **Animation scales differ** across devices; UI timing comparisons are not apples-to-apples.")
    if "data_fs.fstype" in diff_fields or any(f == "data_fs.fstype" for f in diff_fields):
        findings.append("- /data filesystem type differs across devices; storage numbers reflect different fs implementations.")
    # Performance findings
    for cat, agg in category_aggs.items():
        devs = agg.get("devices") or {}
        normalized = [(l, info.get("normalized")) for l, info in devs.items() if info.get("normalized") is not None]
        if len(normalized) >= 2:
            normalized.sort(key=lambda x: -x[1])
            top = normalized[0]
            bottom = normalized[-1]
            if top[1] - bottom[1] >= 25:
                findings.append(
                    f"- **{cat}**: `{top[0]}` leads `{bottom[0]}` by "
                    f"{top[1] - bottom[1]:.0f} pts (norm {top[1]:.0f} vs {bottom[1]:.0f})."
                )
    # Skipped / failed
    skipped_pairs: List[str] = []
    failed_pairs: List[str] = []
    for d in devices:
        for stage, info in (d.get("benchmarks") or {}).items():
            status = (info or {}).get("status")
            if status == "skipped":
                skipped_pairs.append(f"{d['device_label']}/{stage}")
            elif status == "failed":
                failed_pairs.append(f"{d['device_label']}/{stage}")
    if failed_pairs:
        findings.append(f"- **Failures**: {', '.join(failed_pairs[:8])}")
    if skipped_pairs:
        findings.append(f"- Skipped stages: {', '.join(skipped_pairs[:8])}")
    return findings


def _advisory_inventory(payload: Dict[str, Any]) -> List[str]:
    out: List[str] = []
    for d in payload.get("devices", []) or []:
        for m in d.get("metrics", []):
            if m.get("status") == "advisory":
                reason = m.get("advisory_reason") or "n/a"
                out.append(f"- `{d['device_label']}` `{m['key']}` = {m['value']} {m['unit']} — {reason}")
    return out


def generate_ai_report(run_dir: Path, payload: Dict[str, Any]) -> Path:
    devices = payload.get("devices", []) or []
    config_diffs = compute_config_diffs(devices)
    category_aggs = compute_category_aggregates(devices)
    findings = _findings(payload, category_aggs, config_diffs)

    lines: List[str] = []
    lines.append("# Android Benchmark — AI Comparison Report")
    lines.append("")
    lines.append("> Token-efficient summary intended for LLM analysis. Numbers in `()` are "
                 "percentage deltas vs the best device on that metric (positive = better in the "
                 "metric's direction). Active vs advisory split: `compute_scores` only uses active.")
    lines.append("")
    lines.append(f"- run_dir: `{payload.get('run_dir')}`")
    lines.append(f"- started: `{payload.get('started_at')}`  finished: `{payload.get('finished_at')}`")
    lines.append(f"- schema: `{payload.get('schema_version')}`  devices: `{len(devices)}`")
    lines.append("")

    lines.append("## 1. Device profiles")
    lines.append("")
    for d in devices:
        lines.append(_device_profile_block(d))
        lines.append("")

    lines.append("## 2. Configuration differences")
    lines.append("")
    if not config_diffs:
        lines.append("_All inspected fields agree across devices._" if len(devices) >= 2
                     else "_Single-device run; no diff to compute._")
    else:
        labels = [d["device_label"] for d in devices]
        header = ["field"] + labels
        lines.append("| " + " | ".join(header) + " |")
        lines.append("| " + " | ".join("---" for _ in header) + " |")
        for row in config_diffs:
            cells = [str(row.get(h, "")) for h in header]
            lines.append("| " + " | ".join(cells) + " |")
    lines.append("")

    lines.append("## 3. Score per category (active metrics, geomean-normalized)")
    lines.append("")
    if not category_aggs:
        lines.append("_No active metrics matched a scoring category. Likely a single-device "
                     "run or all stages skipped._")
    else:
        for cat in sorted(category_aggs.keys(), key=lambda k: -SCORE_WEIGHTS.get(k, 0)):
            lines.append(_category_comparison_block(cat, category_aggs[cat], devices))
            lines.append("")

    lines.append("## 4. Total weighted score")
    lines.append("")
    scores = payload.get("scores") or {}
    if scores:
        peak = max((s.get("total") or 0) for s in scores.values()) or 1
        for label, score in scores.items():
            total = score.get("total")
            if total is None:
                lines.append(f"- `{label}`: total=**N/A** ({score.get('note', '')})")
            else:
                bar = format_ascii_bar(total, peak, width=24)
                lines.append(f"- `{label}`: total=**{total}**  {bar}")
    lines.append("")

    lines.append("## 5. Notable findings")
    lines.append("")
    if findings:
        lines.extend(findings)
    else:
        lines.append("_No notable findings auto-detected._")
    lines.append("")

    lines.append("## 5b. 数据解读 / 依赖分析")
    lines.append("")
    lines.append("> 自动按规则匹配的解读条目。每条标出涉及的 metric 与可能影响它的 device_info / env 参数。")
    lines.append("")
    interp = interpret_findings(payload)
    if not interp:
        lines.append("_本次没有触发解读规则。_")
    else:
        for f in interp:
            sev = _SEVERITY_TAG.get(f["severity"], "")
            lines.append(f"### {sev} {f['title']}")
            lines.append("")
            lines.append(f["detail"])
            if f.get("related_metrics"):
                lines.append("")
                lines.append(f"  - **相关 metric**: `{'`, `'.join(f['related_metrics'])}`")
            if f.get("related_params"):
                lines.append(f"  - **相关参数**: `{'`, `'.join(f['related_params'])}`")
            lines.append("")
    lines.append("")

    lines.append("## 6. Advisory metrics inventory")
    lines.append("")
    advisory = _advisory_inventory(payload)
    if advisory:
        lines.extend(advisory[:30])
        if len(advisory) > 30:
            lines.append(f"_(... {len(advisory) - 30} more truncated; see summary.json)_")
    else:
        lines.append("_No advisory metrics this run._")
    lines.append("")

    lines.append("## 7. Failures / skipped stages")
    lines.append("")
    any_issue = False
    for d in devices:
        bm = d.get("benchmarks") or {}
        bad = sorted([k for k, v in bm.items() if (v or {}).get("status") in ("failed", "missing")])
        sk = sorted([k for k, v in bm.items() if (v or {}).get("status") == "skipped"])
        if bad or sk:
            any_issue = True
            lines.append(f"- `{d['device_label']}` failed/missing: `{', '.join(bad) or '-'}` ; "
                         f"skipped: `{', '.join(sk) or '-'}`")
    if not any_issue:
        lines.append("_All stages completed across devices._")
    lines.append("")

    lines.append("## 8. Run flags")
    lines.append("")
    cfg = payload.get("config") or {}
    interesting_keys = (
        "preset", "benchmark_iterations", "thermal_duration_sec", "battery_duration_sec",
        "scroll_trace_seconds", "launch_iterations", "interaction_iterations",
        "sustained_rounds", "memory_pressure_max_mb", "ai_runs", "gpu_duration_sec",
        "animation_scale", "screen_brightness", "thermal_precool_sec",
        "harden_cold_launch",
    )
    flag_lines = [f"- `--{k.replace('_', '-')}={cfg.get(k)}`" for k in interesting_keys if k in cfg]
    lines.extend(flag_lines)
    lines.append("")

    path = run_dir / "ai_report.md"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path
