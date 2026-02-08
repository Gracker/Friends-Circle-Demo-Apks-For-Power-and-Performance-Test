# Automation Test Infrastructure Review

## Executive Summary

The automation test infrastructure in `automation-test/` provides a well-structured Python-based framework for measuring Android performance across three dimensions: scrolling FPS, cold launch time, and in-app Activity switch time. The codebase is organized clearly with a config-driven approach, reusable utility library, and multi-format report generation.

**Strengths:**
- Clean separation of concerns: config, scripts, lib, results
- Comprehensive APK registry covering 10+ scrolling apps, 5 launch apps, 3 switch apps
- Multiple test strategies (standard scroll, douyin flip, ebook page, map drag)
- Logcat-based monitoring for accurate timing (launch and switch tests)
- Multi-format report output (Markdown, HTML, JSON)
- Good CLI interface with argparse for all scripts

**Key Issues (by priority):**
1. **P0 - Correctness:** FPS calculation uses `janky_percent` as a proxy for `avg_fps` instead of computing true average FPS from frame durations
2. **P0 - Correctness:** Janky frame count vs `janky_percent` mismatch in `parse_gfxinfo` -- the 120 parsed frames may differ significantly from `total_frames` reported by gfxinfo summary
3. **P1 - Reliability:** `shell=True` in all subprocess calls creates fragility and potential shell injection risks
4. **P1 - Reliability:** LogcatMonitor subprocess leaks -- `proc.terminate()` may not be called if thread is interrupted
5. **P1 - Maintainability:** No `__init__.py` in `lib/` -- modules rely on `sys.path` manipulation
6. **P2 - Robustness:** No retry logic for transient ADB failures
7. **P2 - Results:** All result files dumped flat into `results/` -- 200+ files with no subdirectory organization in practice (despite `run_all_tests.sh` creating timestamped dirs, the individual scripts default to flat `results/`)
8. **P2 - Testing:** Zero unit tests for the framework itself

---

## File-by-File Analysis

### 1. `config/test_config.json`

**Purpose:** Central test configuration with thresholds, device timing, and test matrix.

**Findings:**

- **Thresholds are reasonable for a 120Hz device:**
  - FPS: excellent=115, good=90, acceptable=60, poor=30 -- appropriate for 120Hz target
  - Janky%: excellent=1%, good=5%, acceptable=10%, poor=20% -- aligned with industry standards
  - Launch time: excellent=300ms, good=500ms, acceptable=1000ms, poor=2000ms -- matches Android vitals guidance
  - Switch time: excellent=100ms, good=200ms, acceptable=500ms, poor=1000ms -- reasonable

- **Config structure is clean** with versioning, device timing, per-test sections, and centralized thresholds.

- **Issues:**
  - `fps.excellent=115` threshold exists in config but is **never used** by `FPSAnalyzer.calculate_grade()` which only uses `janky_percent` thresholds. The FPS thresholds are dead config.
  - `output.report_format` lists `["json", "markdown", "html"]` but the `ReportGenerator.generate_all()` always generates all three formats unconditionally, ignoring this config.
  - `fps_sample_window_frames: 120` is defined but never referenced in any script.
  - `device.wait_after_install_ms: 2000` and `device.wait_after_launch_ms: 3000` are defined but never used (scripts use their own section-specific wait values).

### 2. `config/apk_registry.json`

**Purpose:** Maps logical app names to packages, activities, APK filenames, and supported load types.

**Findings:**

- **Comprehensive coverage:** 10 scrolling apps, 5 launch apps (each with 3 flavors = 15 APKs), 3 switch apps (each with 10 load combinations).
- **Well-structured** with clear separation between scrolling/launch/switch test categories.

- **Issues:**
  - `apk_source_dir: "../apk-released"` is a relative path that depends on working directory context. Would be more robust as a config override.
  - `aosp-power` app has `supports_activity_type: false` and no `load_types` field -- inconsistent with other apps that explicitly list types even when not supported.
  - The `test_type` field on `aosp-douyin` ("vertical_swipe") and `aosp-ebook` ("horizontal_swipe") is defined in registry but **never read by any script**. The scripts use `TestStrategy.get_strategy_for_app()` with hardcoded app name checks instead.
  - `launch_tests` apps define `activity` with full class path (e.g., `com.example.launch.aosp.MainActivity`) while `scrolling_tests` and `switch_tests` use shorthand (e.g., `.MainActivity`). Inconsistent but functional since ADB accepts both.

### 3. `lib/utils.py` (590 lines)

**Purpose:** Core utility library providing ADB interaction, config management, FPS analysis, results management, logging, logcat monitoring, and test strategies.

#### 3.1 `ADBHelper` (lines 18-198)

**Positives:**
- Clean static method API with consistent `(bool, str)` return tuple pattern
- Proper timeout handling with `subprocess.TimeoutExpired` catch
- Good extraction of `TotalTime`/`WaitTime` from `am start -W` output

**Issues:**
- **`shell=True` in `subprocess.run()`** (line 37): Every ADB command goes through shell expansion. This is both a security risk (if any parameter comes from untrusted input) and a reliability issue (shell metacharacters in package names could cause unexpected behavior). Should use `shlex.split()` or pass a list.
- **No multi-device support:** All commands go to the default device. If multiple devices are connected, tests will fail unpredictably. Should support `-s <serial>` option.
- **`get_screen_size()` fallback** (line 186): Returns hardcoded `(1080, 2400)` on failure with no warning log. This could cause tests to silently use wrong coordinates.
- **No retry logic:** ADB is notoriously flaky. A single timeout or connection hiccup fails the entire test. At minimum, `run_command` should support configurable retries.
- **`start_activity` extras handling** (line 99): Only supports `--es` (string extras). The `activity_type` extra used by scrolling tests is always passed as string, which works, but the method signature suggests generality it doesn't deliver.

#### 3.2 `ConfigManager` (lines 201-241)

**Positives:**
- Lazy-loaded properties with caching (`_test_config`, `_apk_registry`)
- Clean getter methods for each test category

**Issues:**
- **No config validation:** If `test_config.json` is malformed or missing required keys, errors will be cryptic `KeyError` exceptions deep in test execution rather than early, clear validation errors.
- **`get_threshold()` returns 0 as default** (line 241): This means any typo in threshold category/level silently returns 0, which would make every test pass as "excellent".

#### 3.3 `FPSAnalyzer` (lines 244-385)

**Positives:**
- Handles both framestats and legacy gfxinfo summary formats
- Smart heuristic to detect column layout shift (Frame ID vs timestamp)
- Filters outliers > 500ms (likely app paused)

**Issues:**
- **P0 - Incorrect `avg_fps` calculation** (lines 363-365):
  ```python
  non_janky_ratio = 1 - (result["janky_percent"] / 100)
  result["avg_fps"] = target_fps * non_janky_ratio
  ```
  This is mathematically wrong. A frame that takes 9ms (janky at 120Hz = 8.33ms) is counted as janky but would produce ~111 FPS, not 0 FPS. The correct approach is `1000 / mean(frame_times)` or `len(frame_times) / (sum(frame_times) / 1000)`.

- **P0 - `janky_percent` denominator inconsistency** (lines 352-354): `janky_percent` is calculated from `len(frame_durations)` (parsed framestats) but `total_frames` comes from the gfxinfo summary "Total frames rendered" line, which includes frames outside the profiling window. These can diverge significantly. The report displays `total_frames` alongside the `janky_percent` from parsed frames, which is misleading.

- **Percentile calculation off-by-one** (lines 358-360): `sorted_times[int(len(sorted_times) * 0.99)]` can exceed array bounds for small arrays (e.g., 1 frame: `int(1 * 0.99) = 0`, OK. But semantically, using `int()` truncation instead of proper percentile interpolation is imprecise).

- **`frame_times` included in result dict** (line 349): For heavy tests with 3000+ frames, this embeds thousands of floats in every JSON result file. The raw gfxinfo output is already saved separately; storing parsed frame times doubles storage.

#### 3.4 `LogcatMonitor` (lines 438-556)

**Positives:**
- Threading-based design allows async monitoring while test continues
- Proper use of `stop_event` for clean shutdown
- Separate patterns for launch (`[Duration: xxxms]`) and switch (`Switch complete in xxxms`)

**Issues:**
- **Subprocess leak** (lines 480-496, 526-546): `proc.terminate()` is in the `try` block but if the thread is killed (e.g., daemon thread on main thread exit), the subprocess may not be terminated. Should use a `finally` block or context manager.
- **`import threading` inside methods** (lines 472, 519): Imports should be at module level.
- **Race condition potential**: Between `LogcatMonitor.clear_logcat()` and starting the monitor, log messages could be missed if the app starts very quickly. The monitor should be started *before* the action that triggers the log.
- **No log level flexibility**: Launch uses `:I`, Switch uses `:D`. These are hardcoded. If the app changes log levels, the monitor silently fails.

#### 3.5 `ResultsManager` (lines 388-406)

**Issues:**
- **Timestamp set at construction time** (line 394): If a test run takes 30+ minutes, all results get the same timestamp, which is confusing when looking at file modification times.
- **No file locking**: Concurrent test runs could overwrite each other's results.

#### 3.6 Logging Functions (lines 409-435)

- ANSI colors are always emitted, even when output is redirected to a file. Should use `sys.stdout.isatty()` check.

#### 3.7 `TestStrategy` (lines 559-588)

- **Hardcoded app-to-strategy mapping** (lines 568-577): `get_strategy_for_app()` uses `if/elif` on app names. This should read from `apk_registry.json`'s `test_type` field, which already exists but is unused.

### 4. `lib/report_generator.py` (322 lines)

**Purpose:** Loads latest test results and generates Markdown, HTML, and JSON reports.

**Positives:**
- Clean separation of format generation methods
- Overall grade calculation from individual test grades
- HTML report with reasonable CSS styling

**Issues:**
- **`load_latest_results()` fragility** (lines 41-42): Uses `sorted(glob(...), reverse=True)` to find the latest summary file. This sorts by filename string, which works because filenames contain timestamps in `YYYYMMDD_HHMMSS` format. But it will break if the format changes. Should parse actual timestamps.
- **HTML relies on external CDN** (line 231): `<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js">` -- the HTML report requires internet access to render. Should either bundle the library or use server-side Markdown rendering.
- **No chart/graph generation**: For a performance test suite, visual charts (FPS over time, launch time distribution) would be very valuable.
- **The `--format` CLI argument** (line 289) is parsed but never used -- `generate_all()` is always called regardless of the format selection.
- **Report path in `run_all_tests.sh`** (line 135): `python3 "$PROJECT_ROOT/lib/report_generator.py" --results-dir "$RESULTS_DIR"` -- the report generator is in `lib/` but treated as a script. Should either move to `scripts/` or be imported as a library.

### 5. `scripts/run_launch_test.py` (483 lines)

**Purpose:** Cold launch performance testing with logcat-based timing.

**Positives:**
- Dual measurement approach: `am start -W` for basic timing, logcat monitoring for accurate app-reported timing
- Multiple iterations with statistical aggregation (avg, min, max)
- Graceful degradation: falls back to elapsed time if logcat monitoring times out
- Clean separation of single launch, iterations, app test, and full test levels

**Issues:**
- **Double force-stop in `run_iterations()`** (lines 190-191, 197): Calls `force_stop()` at the beginning of the loop AND inside `run_single_launch_monitored()` (line 107). Redundant.
- **Double `wait_ms(1000 + ...)` wait** (lines 194, 110): The iteration loop adds a post-force-stop wait, then `run_single_launch_monitored` adds another. Total wait between iterations is ~4 seconds of pure waiting, which may be excessive.
- **`run_single_launch()` is unused** (lines 33-82): The `run_iterations` method always calls `run_single_launch_monitored()`. The non-monitored version is dead code.
- **Debug print left in** (line 123): `print(f"[DEBUG] run_single_launch_monitored calling start_activity...")` should use `log_info` or be removed.
- **`--package` mode hardcodes `.MainActivity`** (line 462): `runner.run_iterations(args.package, ".MainActivity", iterations)` -- assumes all apps use `.MainActivity`, which is not always true per the registry.
- **Grading logic duplicated** (lines 253-262): Same threshold comparison pattern is copy-pasted across all three test scripts. Should be a shared utility function.

### 6. `scripts/run_scrolling_test.py` (437 lines)

**Purpose:** Scrolling performance testing with gfxinfo-based FPS measurement.

**Positives:**
- Multiple test strategies: standard scroll, douyin flip, ebook page, map drag
- Block-based scroll pattern (5 up, 5 down) simulates realistic reading behavior
- Proper gfxinfo reset before measurement window

**Issues:**
- **No warm-up run**: Scrolling tests go straight into measurement. The first few frames after launch are typically slower. Should add a discarded warm-up pass.
- **`random` import at module level but only used by map drag** (line 25): Minor but `random.choice` in `_execute_map_drag` means map drag results are non-deterministic. Should use a fixed seed for reproducibility.
- **Single measurement per load type**: Unlike launch tests (5 iterations), scrolling tests run once per config. This provides no statistical confidence. Should support multiple runs with aggregation.
- **Missing `launch-game` in scrolling tests**: The `launch-game` app category has no corresponding scrolling test app, suggesting game rendering performance during interaction is untested.

### 7. `scripts/run_switch_test.py` (520 lines)

**Purpose:** In-app Activity switch timing using logcat monitoring.

**Positives:**
- Logcat-based monitoring for accurate switch completion detection
- Multiple load combinations (pure, self-load, background-load, combined)
- Statistical aggregation over iterations

**Issues:**
- **Hardcoded button coordinates** (lines 28-39, 53-68): `BUTTON_POSITIONS` maps load combinations to grid positions, then `_calculate_button_coords()` converts to pixel coordinates assuming a fixed 5x2 grid layout starting at 30% screen height. This is extremely fragile:
  - If the UI layout changes, all tests silently click wrong buttons
  - No verification that the correct button was actually clicked
  - No accessibility/UI Automator-based approach for reliable element targeting
- **`run_single_switch()` is unused** (lines 70-150): Similar to launch test, the non-monitored version exists but `run_iterations()` always calls `run_single_switch_monitored()`. Dead code.
- **No validation of click target**: After tapping the button, there's no check that the expected Activity actually launched (e.g., via `dumpsys activity top`).

### 8. `scripts/install_apks.sh` (261 lines)

**Purpose:** Install test APKs to device.

**Positives:**
- Categorized installation (scrolling, launch, switch, quick)
- Success/failure counting
- Device connectivity check

**Issues:**
- **APK filenames hardcoded** (lines 106-117, 136-152, 171-175): The APK list is duplicated between `install_apks.sh` and `apk_registry.json`. If a new APK is added to the registry, `install_apks.sh` must be manually updated.
- **No version management**: All APKs are `v1.0.0`. No mechanism to install different versions for A/B comparison.
- **`adb install -r -d`** (line 53): The `-d` flag allows downgrading, which could mask version mismatch issues.
- **Error swallowed** (line 53): `> /dev/null 2>&1` suppresses all ADB output, making it impossible to diagnose install failures.

### 9. `scripts/run_all_tests.sh` (259 lines)

**Purpose:** Orchestrates the full test pipeline.

**Positives:**
- Timestamped result directories for historical tracking
- Environment checks (Python, device) before starting
- Total execution time tracking
- Clean CLI interface with multiple modes

**Issues:**
- **`set -e` with no error handling** (line 8): If any Python script fails (non-zero exit), the entire pipeline aborts. Should trap errors and continue with remaining tests, reporting failures at the end.
- **Hardcoded step numbering** ("Step 1/4", "Step 2/4"...): When running single tests (e.g., `--scrolling`), the step numbers are still "2/4", which is confusing.
- **Box-drawing characters in output** (lines 195-202): May render incorrectly on some terminals.
- **Result directory mismatch**: `run_all_tests.sh` creates `results/$TIMESTAMP/` subdirectory, but individual Python scripts default to `results/` (no timestamp subdirectory). When running scripts individually vs. via the orchestrator, results land in different places.

### 10. `debug_launch.py`

**Purpose:** Quick debug script for testing ADB connectivity.

**Issues:**
- Uses `sys.path.append` hack to import from `lib/`.
- Hardcoded package/activity. Useful only as a developer scratch file, not part of the test infrastructure.

### 11. `results/` Directory

**Observed data:** ~200 flat JSON files, 2 markdown reports, 2 JSON reports. All from a single test date (2026-01-18).

**Issues:**
- **Flat file explosion**: 200+ files in a single directory with no subdirectories. The naming convention (`{test_type}_{app}_{load}_{timestamp}.json`) helps but the directory is difficult to navigate.
- **No result cleanup mechanism**: Old results accumulate indefinitely.
- **No `.gitignore`**: Result files (especially raw data with frame times) could accidentally be committed.
- **Summary files mix with individual results**: `launch_summary_all_*.json` sits alongside `launch_launch-aosp_heavy_*.json`. No visual/structural separation.

---

## Improvement Recommendations

### Priority 0 - Correctness (Fix Immediately)

1. **Fix `avg_fps` calculation in `FPSAnalyzer.parse_gfxinfo()`**: Replace the proxy calculation with actual computation from frame durations:
   ```python
   avg_frame_time_ms = sum(frame_durations) / len(frame_durations)
   result["avg_fps"] = 1000.0 / avg_frame_time_ms
   ```

2. **Fix `janky_percent` / `total_frames` inconsistency**: Either use `len(frame_durations)` as the displayed total, or clearly label the two different values (summary total vs. profiled frames).

### Priority 1 - Reliability

3. **Replace `shell=True` with argument lists in `ADBHelper.run_command()`**: Use `subprocess.run(["adb"] + shlex.split(command), ...)` to avoid shell injection and metacharacter issues.

4. **Fix LogcatMonitor subprocess leak**: Add `finally` block to ensure `proc.terminate()` and `proc.wait()` are always called. Move the logcat process start *before* the triggering action to avoid missing early logs.

5. **Add ADB retry logic**: Wrap `ADBHelper.run_command()` with configurable retry count and backoff for transient failures.

6. **Proper Python package structure**: Add `__init__.py` to `lib/` directory and use relative imports instead of `sys.path` manipulation.

### Priority 2 - Robustness & Maintainability

7. **Replace hardcoded button coordinates in switch test**: Use UI Automator (`uiautomator dump`) or accessibility service to find buttons by text/ID rather than screen position.

8. **Unify test strategy dispatch**: Read `test_type` from `apk_registry.json` instead of hardcoding app name checks in `TestStrategy.get_strategy_for_app()`.

9. **De-duplicate APK lists**: Have `install_apks.sh` read from `apk_registry.json` instead of maintaining a parallel hardcoded list.

10. **Add warm-up pass for scrolling tests**: Run one discard pass before resetting gfxinfo and measuring.

11. **Add multiple iterations for scrolling tests**: Like launch/switch tests, run scrolling tests multiple times and report statistics.

12. **Extract grading logic**: Create a shared `GradeCalculator` class in `utils.py` used by all three test scripts, eliminating copy-paste.

13. **Remove dead code**: Delete `run_single_launch()` and `run_single_switch()` methods that are never called.

14. **Remove debug prints**: Clean up `[DEBUG]` print statements in `run_launch_test.py`.

### Priority 3 - Reporting & CI/CD

15. **Add chart generation to reports**: Use `matplotlib` (or embed Chart.js in HTML) to generate FPS distribution histograms, launch time box plots, and trend charts across runs.

16. **Fix HTML report CDN dependency**: Bundle `marked.min.js` or generate HTML server-side with Python `markdown` library.

17. **Implement the `--format` CLI option in report generator**: Currently ignored.

18. **Connect unused config values**: Either use `fps_sample_window_frames`, `device.wait_after_install_ms`, `device.wait_after_launch_ms`, `output.report_format` -- or remove them from config to avoid confusion.

### Priority 4 - Testing & CI/CD Integration

19. **Add unit tests for `FPSAnalyzer.parse_gfxinfo()`**: This is the most critical parsing logic. Create test fixtures from real gfxinfo output and verify frame time extraction, janky calculation, and percentiles.

20. **Add unit tests for `LogcatMonitor` pattern matching**: Verify that the regex patterns correctly match real logcat output.

21. **Add a `requirements.txt` or `pyproject.toml`**: Currently the project has zero Python dependencies (only stdlib), which is great, but making this explicit helps CI/CD.

22. **Add `.gitignore` for `results/`**: Prevent accidental commit of large result JSON files.

23. **CI/CD integration suggestions**:
    - Add a `--ci` mode to `run_all_tests.sh` that outputs JUnit XML for CI test result parsing
    - Add threshold-based pass/fail exit codes so CI can gate on performance regressions
    - Support device farm integration (multiple devices in parallel with `-s serial`)
    - Add result comparison mode: compare current run against a baseline to detect regressions

### Priority 5 - Python Best Practices

24. **Add type hints**: Most methods have type hints (good!), but some internal variables and dict returns could benefit from `TypedDict` or dataclasses.

25. **Use dataclasses for test results**: Replace bare `Dict[str, Any]` return types with structured dataclasses (e.g., `LaunchResult`, `ScrollingResult`, `SwitchResult`) for better IDE support and self-documentation.

26. **Use `logging` module instead of custom `log_*` functions**: Would enable log level control, file output, and structured logging.

27. **Add docstrings to all public methods**: Most methods have docstrings (good!), but `_calculate_button_coords()` and the scroll strategy methods lack them.

---

## Summary Table

| Area | Rating | Key Issue |
|------|--------|-----------|
| Configuration | B+ | Well-structured but has unused/dead config values |
| ADB Interaction | B | Functional but uses shell=True, no retries, no multi-device |
| FPS Analysis | C | Incorrect avg_fps calculation, janky% denominator mismatch |
| Launch Testing | B+ | Good logcat monitoring, but dead code and debug prints |
| Scrolling Testing | B | Works well but lacks warm-up, single iteration only |
| Switch Testing | B- | Fragile hardcoded coordinates, no click validation |
| Report Generation | B | Multi-format output but CDN dependency, no charts |
| Shell Scripts | B | Good orchestration but duplicated APK lists, error handling |
| Result Management | C+ | Flat directory, no cleanup, no gitignore |
| Test Coverage | D | Zero unit tests for the framework itself |
| Overall | B- | Solid foundation with important correctness and reliability issues |
