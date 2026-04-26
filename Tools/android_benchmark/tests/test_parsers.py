"""Unit tests for android_benchmark parsers + report math.

These run off fixture strings and dicts so they never need a device. Run with:
    python3 -m unittest android_benchmark.tests.test_parsers
"""
from __future__ import annotations

import unittest

from ..collectors import (
    parse_animation_scales,
    parse_battery_dump,
    parse_data_filesystem,
    parse_foreground_app,
    parse_hwui_pipeline,
    parse_vendor_power_mode,
    parse_wifi_link,
    parse_zram,
)
from ..reports import (
    SCORE_WEIGHTS,
    _safe_delta_pct,
    compare_devices,
    compute_category_aggregates,
    compute_config_diffs,
    compute_scores,
    flatten_device_metrics,
    format_ascii_bar,
    metric_row,
    pct_diff,
)
from ..stages_ai import parse_benchmark_model
from ..stages_gpu import parse_glmark2_output


class TestParsers(unittest.TestCase):
    def test_animation_scales(self):
        text = (
            "window_animation_scale=0.5\n"
            "transition_animation_scale=1.0\n"
            "animator_duration_scale=0.25\n"
            "unrelated=x\n"
        )
        got = parse_animation_scales(text)
        self.assertEqual(got["window_animation_scale"], 0.5)
        self.assertEqual(got["transition_animation_scale"], 1.0)
        self.assertEqual(got["animator_duration_scale"], 0.25)

    def test_foreground_app(self):
        sample = (
            "  mResumedActivity: ActivityRecord{1234 u0 com.example.app/.MainActivity t100}\n"
            "  mFocusedApp=ActivityRecord{abcd u0 com.example.app/.MainActivity t100}\n"
        )
        self.assertEqual(parse_foreground_app(sample), "com.example.app")
        self.assertIsNone(parse_foreground_app(""))

    def test_data_filesystem_proc_mounts_format(self):
        mount_text = "/dev/block/dm-4 /data f2fs rw,noatime,discard,errors=remount-ro 0 0"
        out = parse_data_filesystem(mount_text)
        self.assertEqual(out["fstype"], "f2fs")
        self.assertTrue(out["noatime"])
        self.assertTrue(out["discard"])

    def test_data_filesystem_mount8_format(self):
        # Real Xiaomi/MTK Android 15 output uses mount(8) format
        mount_text = (
            "/dev/block/dm-51 on /data type f2fs "
            "(rw,lazytime,seclabel,nosuid,nodev,noatime,discard,inline_dentry,fsync_mode=nobarrier)"
        )
        out = parse_data_filesystem(mount_text)
        self.assertEqual(out["fstype"], "f2fs")
        self.assertTrue(out["noatime"])
        self.assertTrue(out["discard"])

    def test_zram(self):
        zram = (
            "===/sys/block/zram0===\n"
            "comp_algorithm=lz4 lzo-rle\n"
            "disksize=2147483648\n"
            "mm_stat=123 456 789 0 0 0 0 0\n"
        )
        swaps = "Filename\tType\tSize\tUsed\tPriority\n/dev/block/zram0\tpartition\t2097152\t0\t-2\n"
        out = parse_zram(zram, swaps)
        self.assertTrue(out["enabled"])
        self.assertEqual(out["comp_algorithm"], "lz4")  # only first algo token
        self.assertEqual(out["disksize_bytes"], 2147483648)
        self.assertEqual(out["orig_data_bytes"], 123)
        self.assertEqual(out["compr_data_bytes"], 456)
        self.assertEqual(out["mem_used_bytes"], 789)

    def test_wifi_link(self):
        text = "    mWifiInfo SSID: \"ExampleAP\", RSSI: -55, Link speed: 433, Frequency: 5180"
        out = parse_wifi_link(text)
        self.assertEqual(out["rssi_dbm"], -55)
        self.assertEqual(out["link_mbps"], 433)
        self.assertEqual(out["frequency_mhz"], 5180)
        self.assertEqual(out["ssid_sanitized"], "***present***")

    def test_battery_dump(self):
        text = (
            "Current Battery Service state:\n"
            "  AC powered: false\n"
            "  USB powered: true\n"
            "  Wireless powered: false\n"
            "  level: 67\n"
            "  scale: 100\n"
            "  temperature: 314\n"
            "  voltage: 4200\n"
            "  health: 2\n"
        )
        out = parse_battery_dump(text)
        self.assertEqual(out["level_pct"], 67)
        self.assertEqual(out["temperature_c"], 31.4)
        self.assertFalse(out["ac_powered"])
        self.assertTrue(out["usb_powered"])

    def test_hwui_vendor_mode(self):
        props = {
            "debug.hwui.renderer": "skiavk",
            "debug.hwui.use_vulkan": "true",
            "persist.sys.miui_performance": "1",
            "ro.config.low_ram": "false",
        }
        hwui = parse_hwui_pipeline(props)
        self.assertEqual(hwui["hwui_renderer"], "skiavk")
        self.assertEqual(hwui["hwui_use_vulkan"], "true")
        vendor = parse_vendor_power_mode(props)
        self.assertIn("persist.sys.miui_performance", vendor)


class TestReportMath(unittest.TestCase):
    def test_safe_delta_pct_zero_worst(self):
        self.assertIsNone(_safe_delta_pct(0.0, 0.0, "lower"))
        # When worst is 0 and best is nonzero, fall back to |best| denom.
        self.assertEqual(_safe_delta_pct(5.0, 0.0, "higher"), 100.0)

    def test_safe_delta_pct_higher_direction(self):
        self.assertAlmostEqual(_safe_delta_pct(150.0, 100.0, "higher"), 50.0, places=3)

    def test_safe_delta_pct_lower_direction(self):
        self.assertAlmostEqual(_safe_delta_pct(50.0, 200.0, "lower"), 75.0, places=3)

    def test_metric_row_sets_status_from_advisory(self):
        row = metric_row(
            "storage", "Random read", "storage.random_read_iops", "IOPS", "higher", 120.0,
            stage_status="success", advisory="shell-fork bound",
        )
        self.assertEqual(row["status"], "advisory")
        self.assertFalse(row["valid_for_score"])
        self.assertIn("shell-fork", row["advisory_reason"])

    def test_metric_row_active_when_stage_success_and_no_advisory(self):
        row = metric_row("cpu", "Native int", "native.cpu_int_best_mops", "Mops/s", "higher", 1234.0, stage_status="success")
        self.assertEqual(row["status"], "active")
        self.assertTrue(row["valid_for_score"])

    def test_compute_scores_single_device_returns_none_total(self):
        device = {
            "device_label": "phoneA",
            "metrics": [
                metric_row("cpu", "Native int", "native.cpu_int_best_mops", "Mops/s", "higher", 1000.0, stage_status="success"),
            ],
        }
        scores = compute_scores([device])
        self.assertIsNone(scores["phoneA"]["total"])
        self.assertFalse(scores["phoneA"]["absolute_mode"])

    def test_compute_scores_two_devices_normalize(self):
        make_dev = lambda label, value: {
            "device_label": label,
            "metrics": [
                metric_row("cpu", "Native int", "native.cpu_int_best_mops", "Mops/s", "higher", value, stage_status="success"),
                metric_row("scroll", "Scroll FPS", "scroll.avg_fps", "fps", "higher", value / 10, stage_status="success"),
            ],
        }
        scores = compute_scores([make_dev("A", 1000.0), make_dev("B", 500.0)])
        self.assertEqual(scores["A"]["categories"]["cpu"], 100.0)
        self.assertEqual(scores["B"]["categories"]["cpu"], 50.0)
        self.assertIsNotNone(scores["A"]["total"])
        self.assertGreater(scores["A"]["total"], scores["B"]["total"])

    def test_compute_scores_excludes_advisory_metrics(self):
        make_dev = lambda label, crypto, cpu: {
            "device_label": label,
            "metrics": [
                # crypto is advisory category; must NOT influence score even though valid
                metric_row("crypto", "Hash", "crypto.hash_single_mib_s", "MiB/s", "higher", crypto,
                           stage_status="advisory"),
                metric_row("cpu", "Native int", "native.cpu_int_best_mops", "Mops/s", "higher", cpu,
                           stage_status="success"),
            ],
        }
        scores = compute_scores([make_dev("A", 100000.0, 100.0), make_dev("B", 1.0, 1000.0)])
        # B should win despite A's huge crypto number, because crypto is excluded from scoring.
        self.assertGreater(scores["B"]["total"], scores["A"]["total"])

    def test_compare_devices_handles_zero_worst(self):
        dev_a = {
            "device_label": "A",
            "metrics": [
                metric_row("network", "Ping jitter", "network.ping_jitter_ms", "ms", "lower", 0.0,
                           stage_status="success"),
            ],
        }
        dev_b = {
            "device_label": "B",
            "metrics": [
                metric_row("network", "Ping jitter", "network.ping_jitter_ms", "ms", "lower", 5.0,
                           stage_status="success"),
            ],
        }
        comps = compare_devices([dev_a, dev_b])
        self.assertEqual(len(comps), 1)
        self.assertEqual(comps[0]["winner"], "A")
        # should not raise ZeroDivisionError; delta_pct may be 100.0 via fallback
        self.assertIsInstance(comps[0]["winner_delta_pct"], (int, float))

    def test_flatten_renames_old_keys(self):
        device_result = {
            "benchmarks": {
                "cpu": {"status": "advisory", "crypto_single_mib_s": 1500.0, "crypto_best_multi_mib_s": 6000.0},
                "memory": {"status": "advisory", "kernel_loop_mib_s": 4000.0},
                "storage": {
                    "status": "success",
                    "sequential_write_mib_s": 500.0,
                    "buffered_read_mib_s": 1200.0,
                    "random_read_iops_advisory": 400.0,
                    "random_write_iops_advisory": 300.0,
                },
                "battery": {"status": "success", "drain_pct": 0.5, "avg_power_mw_estimate": 850.0},
                "native": {
                    "status": "success",
                    "summary": {
                        "cpu_int_best_mops": 8000.0,
                        "cpu_int_single_mops": 2000.0,
                        "cpu_fp_best_mflops": 12000.0,
                        "memory_triad_mib_s": 14000.0,
                    },
                },
            },
        }
        rows = flatten_device_metrics(device_result)
        keys = {r["key"]: r for r in rows}
        self.assertIn("crypto.hash_single_mib_s", keys)
        self.assertIn("baseline.kernel_loop_mib_s", keys)
        self.assertIn("storage.buffered_read_mib_s", keys)
        self.assertIn("battery.drain_pct", keys)
        self.assertEqual(keys["crypto.hash_single_mib_s"]["status"], "advisory")
        self.assertEqual(keys["storage.buffered_read_mib_s"]["status"], "advisory")
        self.assertEqual(keys["native.cpu_int_best_mops"]["status"], "active")
        self.assertTrue(keys["native.cpu_int_best_mops"]["valid_for_score"])


class TestNewParsers(unittest.TestCase):
    def test_glmark2_output(self):
        sample = """
=======================================================
    glmark2 2021.02
=======================================================
    [build] use-vbo=false: FPS: 1234 FrameTime: 0.810 ms
    [build] use-vbo=true: FPS: 1456 FrameTime: 0.687 ms
    [texture] texture-filter=nearest: FPS: 2000 FrameTime: 0.5 ms
=======================================================
                                  glmark2 Score: 1500
=======================================================
"""
        out = parse_glmark2_output(sample)
        self.assertEqual(out["glmark2_score"], 1500)
        self.assertEqual(len(out["scenes"]), 3)
        self.assertEqual(out["scenes"][0]["scene"], "build")
        self.assertEqual(out["scenes"][0]["fps"], 1234)

    def test_benchmark_model_parse(self):
        sample = """
INFO: Loaded model /data/local/tmp/mobilenet_v2.tflite
INFO: Initialized session in 12.345 ms.
INFO: Running benchmark for 50 iterations
INFO: Inference timings in us: Init: 12345, First inference: 9876, Warmup (avg): 8000, Inference (avg): 7654.32
"""
        parsed = parse_benchmark_model(sample)
        self.assertAlmostEqual(parsed["inference_avg_us"], 7654.32, places=2)
        self.assertAlmostEqual(parsed["init_ms"], 12.345, places=3)


class TestNewScoreBuckets(unittest.TestCase):
    def test_score_weights_complete(self):
        for cat in ("cpu", "gpu", "memory", "memory_pressure", "storage", "ux_data",
                    "ux_image", "scroll", "launch", "interaction", "sustained", "ai"):
            self.assertIn(cat, SCORE_WEIGHTS, f"missing scoring category: {cat}")
        self.assertNotIn("crypto", SCORE_WEIGHTS)
        self.assertNotIn("baseline", SCORE_WEIGHTS)
        self.assertNotIn("video", SCORE_WEIGHTS)

    def test_flatten_includes_new_categories(self):
        device_result = {
            "benchmarks": {
                "geekbench_mix": {"status": "success",
                                   "summary": {"aes_round_mib_s": 500, "sha256_mib_s": 300,
                                               "fft_mflops": 4000, "matmul_mflops": 8000,
                                               "nbody_mflops": 2000, "sort_mops": 50}},
                "ux_data": {"status": "success",
                             "summary": {"regex_mib_per_s": 1500, "json_tokenize_mib_per_s": 800,
                                         "data_sort_mops": 40}},
                "ux_image": {"status": "success",
                              "summary": {"image_mpix_out_per_s": 120}},
                "storage_random_native": {"status": "success",
                                           "summary": {"random_read_iops": 30000, "random_write_iops": 20000}},
                "sustained": {"status": "success",
                               "summary": {"stability_ratio": 0.92, "drift_pct": 6.5}},
                "memory_pressure": {"status": "success", "max_resident_mb": 1024, "lmkd_kills_during": 2},
                "gpu": {"status": "success", "glmark2_score": 1500},
                "ai": {"status": "success",
                        "summary": {"best_inference_avg_us": 5000, "accel_speedup": 4.2}},
                "video": {"status": "advisory",
                           "screenrecord": {"estimated_mb_per_sec": 5.0}},
            },
        }
        rows = flatten_device_metrics(device_result)
        keys = {r["key"]: r for r in rows}
        # Geekbench-style
        for k in ("gb.aes_round_mib_s", "gb.sha256_mib_s", "gb.fft_mflops",
                  "gb.matmul_mflops", "gb.nbody_mflops", "gb.sort_mops"):
            self.assertIn(k, keys)
            self.assertEqual(keys[k]["status"], "active")
        # UX
        self.assertIn("ux.regex_mib_per_s", keys)
        self.assertIn("ux.image_mpix_out_per_s", keys)
        # Native random IOPS now active
        self.assertIn("storage.random_read_iops_native", keys)
        self.assertEqual(keys["storage.random_read_iops_native"]["status"], "active")
        # Sustained
        self.assertIn("sustained.stability_ratio", keys)
        # Memory pressure
        self.assertIn("memory_pressure.max_resident_mb", keys)
        # GPU + AI
        self.assertIn("gpu.glmark2_score", keys)
        self.assertIn("ai.best_inference_avg_us", keys)
        self.assertIn("ai.accel_speedup", keys)
        # Video should be advisory
        self.assertEqual(keys["video.encoder_mb_per_s"]["status"], "advisory")
        self.assertFalse(keys["video.encoder_mb_per_s"]["valid_for_score"])

    def test_score_two_devices_with_new_categories(self):
        def make(label, multiplier):
            return {
                "device_label": label,
                "metrics": [
                    metric_row("cpu", "AES", "gb.aes_round_mib_s", "MiB/s", "higher",
                               1000 * multiplier, stage_status="success"),
                    metric_row("gpu", "GPU", "gpu.glmark2_score", "score", "higher",
                               2000 * multiplier, stage_status="success"),
                    metric_row("ai", "AI", "ai.best_inference_avg_us", "us", "lower",
                               5000 / multiplier, stage_status="success"),
                    metric_row("scroll", "FPS", "scroll.avg_fps", "fps", "higher",
                               90 * multiplier, stage_status="success"),
                ],
            }
        scores = compute_scores([make("A", 1.0), make("B", 0.5)])
        self.assertGreater(scores["A"]["total"], scores["B"]["total"])
        self.assertEqual(scores["A"]["categories"]["cpu"], 100.0)
        self.assertEqual(scores["A"]["categories"]["gpu"], 100.0)
        self.assertEqual(scores["A"]["categories"]["ai"], 100.0)


class TestComparisonHelpers(unittest.TestCase):
    def test_pct_diff_higher_direction(self):
        self.assertAlmostEqual(pct_diff(120, 100, "higher"), 20.0)
        self.assertAlmostEqual(pct_diff(80, 100, "higher"), -20.0)

    def test_pct_diff_lower_direction(self):
        # Lower is better: value 80 vs reference 100 → "+20% better"
        self.assertAlmostEqual(pct_diff(80, 100, "lower"), 20.0)
        self.assertAlmostEqual(pct_diff(120, 100, "lower"), -20.0)

    def test_pct_diff_zero_reference(self):
        self.assertIsNone(pct_diff(5.0, 0.0, "higher"))
        self.assertIsNone(pct_diff(None, 100, "higher"))

    def test_format_ascii_bar(self):
        self.assertEqual(len(format_ascii_bar(50, 100, width=10)), 10)
        self.assertTrue(format_ascii_bar(100, 100, width=10).startswith("█" * 10))
        self.assertEqual(format_ascii_bar(None, 100), " " * 20)

    def test_compute_config_diffs_empty_for_single_device(self):
        device = {"device_label": "A", "device_info": {"model": "X"}, "env": {}, "battery_gate": {}}
        self.assertEqual(compute_config_diffs([device]), [])

    def test_compute_config_diffs_finds_differences(self):
        a = {
            "device_label": "A",
            "device_info": {
                "soc_model": "Tensor G4", "cpu_count": 8,
                "hwui": {"renderengine_backend": "skia-vulkan"},
                "storage": {"data_fs": {"fstype": "f2fs", "noatime": True, "discard": True}},
                "mm": {"zram": {"enabled": True}},
            },
            "env": {"applied": {"animation_scale": 1.0, "screen_brightness": 128}},
            "battery_gate": {"charging": True, "level_pct": 100},
        }
        b = {
            "device_label": "B",
            "device_info": {
                "soc_model": "MT6855", "cpu_count": 8,
                "hwui": {"renderengine_backend": "skia-gl"},
                "storage": {"data_fs": {"fstype": "f2fs", "noatime": True, "discard": True}},
                "mm": {"zram": {"enabled": False}},
            },
            "env": {"applied": {"animation_scale": 1.0, "screen_brightness": 128}},
            "battery_gate": {"charging": False, "level_pct": 60},
        }
        diffs = compute_config_diffs([a, b])
        fields = [r["field"] for r in diffs]
        self.assertIn("device.soc_model", fields)
        self.assertIn("hwui.renderengine_backend", fields)
        self.assertIn("mm.zram_enabled", fields)
        self.assertIn("battery.charging", fields)
        # Same-everywhere fields should NOT appear
        self.assertNotIn("device.cpu_count", fields)
        self.assertNotIn("env.animation_scale", fields)
        self.assertNotIn("data_fs.fstype", fields)

    def test_compute_category_aggregates(self):
        def make(label, cpu_v, scroll_v):
            return {
                "device_label": label,
                "metrics": [
                    metric_row("cpu", "AES", "gb.aes_round_mib_s", "MiB/s", "higher", cpu_v, stage_status="success"),
                    metric_row("cpu", "FFT", "gb.fft_mflops", "MFLOPS", "higher", cpu_v * 2, stage_status="success"),
                    metric_row("scroll", "FPS", "scroll.avg_fps", "fps", "higher", scroll_v, stage_status="success"),
                ],
            }
        aggs = compute_category_aggregates([make("A", 1000, 120), make("B", 500, 90)])
        self.assertIn("cpu", aggs)
        self.assertIn("scroll", aggs)
        self.assertEqual(aggs["cpu"]["devices"]["A"]["normalized"], 100.0)
        self.assertEqual(aggs["cpu"]["devices"]["B"]["normalized"], 50.0)
        self.assertEqual(aggs["scroll"]["devices"]["A"]["normalized"], 100.0)
        self.assertEqual(aggs["scroll"]["devices"]["B"]["normalized"], 75.0)

    def test_compute_category_aggregates_skip_unique_metrics(self):
        # If only one device has the metric, it shouldn't enter the aggregation
        a = {"device_label": "A", "metrics": [
            metric_row("cpu", "AES", "gb.aes_round_mib_s", "MiB/s", "higher", 100.0, stage_status="success"),
        ]}
        b = {"device_label": "B", "metrics": [
            metric_row("cpu", "FFT", "gb.fft_mflops", "MFLOPS", "higher", 200.0, stage_status="success"),
        ]}
        aggs = compute_category_aggregates([a, b])
        self.assertEqual(aggs.get("cpu", {}).get("devices", {}).get("A", {}).get("metrics_counted"), 0)
        self.assertEqual(aggs.get("cpu", {}).get("devices", {}).get("B", {}).get("metrics_counted"), 0)


class TestAIReport(unittest.TestCase):
    def test_generate_ai_report_writes_file(self):
        from ..ai_report import generate_ai_report
        import tempfile
        payload = {
            "schema_version": "1.1",
            "started_at": "t0",
            "finished_at": "t1",
            "run_dir": "/tmp/x",
            "scores": {
                "A": {"total": 80, "categories": {"cpu": 90}, "absolute_mode": False, "note": "ok"},
                "B": {"total": 50, "categories": {"cpu": 50}, "absolute_mode": False, "note": "ok"},
            },
            "devices": [
                {
                    "device_label": "A",
                    "serial": "AAA",
                    "device_info": {
                        "model": "Phone A", "soc_model": "X1", "cpu_count": 8,
                        "memory_total_mb": 8192, "android_release": "15", "sdk": "35",
                        "selinux_mode": "Enforcing",
                        "peak_refresh_rate": "120", "min_refresh_rate": "60",
                        "hwui": {"hwui_renderer": None, "hwui_use_vulkan": "true",
                                  "renderengine_backend": "skiavk"},
                        "storage": {"data_fs": {"fstype": "f2fs", "noatime": True, "discard": True}},
                        "mm": {"zram": {"enabled": True}, "mglru_enabled": "1"},
                        "foreground_package": "com.android.launcher3",
                    },
                    "env": {"applied": {"stay_on_while_plugged_in": "3", "screen_brightness": 128,
                                         "animation_scale": 1.0, "zen_mode": "unchanged"}},
                    "battery_gate": {"charging": True, "ac": True, "usb": False, "wireless": False, "level_pct": 100},
                    "benchmarks": {"native": {"status": "success"}, "scroll": {"status": "success"}},
                    "metrics": [
                        metric_row("cpu", "AES", "gb.aes_round_mib_s", "MiB/s", "higher", 1000, stage_status="success"),
                        metric_row("scroll", "FPS", "scroll.avg_fps", "fps", "higher", 120, stage_status="success"),
                    ],
                },
                {
                    "device_label": "B",
                    "serial": "BBB",
                    "device_info": {
                        "model": "Phone B", "soc_model": "X2", "cpu_count": 8,
                        "memory_total_mb": 6144, "android_release": "15", "sdk": "35",
                        "selinux_mode": "Enforcing",
                        "peak_refresh_rate": "60", "min_refresh_rate": "60",
                        "hwui": {"renderengine_backend": "skiagl"},
                        "storage": {"data_fs": {"fstype": "f2fs", "noatime": True, "discard": True}},
                        "mm": {"zram": {"enabled": False}, "mglru_enabled": None},
                        "foreground_package": "com.android.launcher3",
                    },
                    "env": {"applied": {"stay_on_while_plugged_in": "3", "screen_brightness": 128,
                                         "animation_scale": 1.0, "zen_mode": "unchanged"}},
                    "battery_gate": {"charging": False, "level_pct": 80},
                    "benchmarks": {"native": {"status": "success"}, "scroll": {"status": "success"}},
                    "metrics": [
                        metric_row("cpu", "AES", "gb.aes_round_mib_s", "MiB/s", "higher", 600, stage_status="success"),
                        metric_row("scroll", "FPS", "scroll.avg_fps", "fps", "higher", 60, stage_status="success"),
                    ],
                },
            ],
            "config": {"preset": "standard", "animation_scale": 1.0},
        }
        with tempfile.TemporaryDirectory() as tmp:
            path = generate_ai_report(Path(tmp), payload)
            text = path.read_text()
        # Must contain config diffs for the fields that vary
        self.assertIn("device.soc_model", text)
        self.assertIn("hwui.renderengine_backend", text)
        self.assertIn("battery.charging", text)
        # Must contain category sections
        self.assertIn("cpu", text)
        self.assertIn("scroll", text)
        # Findings should call out the perf gap. Either phrasing is acceptable.
        self.assertTrue(
            "leads" in text.lower() or "Notable findings" in text,
            "expected ai report to mention 'leads' or 'Notable findings'",
        )


import pathlib
Path = pathlib.Path  # used by TestAIReport


if __name__ == "__main__":
    unittest.main()
