# Architecture Review: HighPerformanceFriendsCircle

**Reviewer**: arch-reviewer
**Date**: 2026-02-07
**Scope**: Multi-module architecture, shared code, naming, dependencies, duplication, patterns

---

## Executive Summary

HighPerformanceFriendsCircle is an Android performance testing platform with **31 modules** (1 app, 3 libraries, 27 test APKs) organized around three test categories: scrolling (20 modules), launch (5+common), and switch (3+common). The project uses a well-designed centralized load-configuration library (`load-config`) and has clear separation between test scenarios.

**Key strengths:**
- Clear intent-driven module separation -- each APK tests a specific rendering/perf scenario
- Well-designed `load-config` library with deterministic, reproducible load simulation
- Consistent `*-common` pattern for launch and switch module families

**Critical issues:**
- Massive code duplication: bean classes (FriendCircleBean, CommentBean, UserBean, etc.) are copy-pasted across 9+ modules
- Widget duplication: NineGridView and NineImageAdapter duplicated in 9 modules
- Inconsistent Java compatibility (VERSION_11 vs VERSION_17) and minSdk (21 vs 24)
- Parallel/divergent LoadSimulator implementations (Java in `load-config` vs Kotlin in `launch-common`)
- No shared build convention plugin -- signing config, SDK versions, and dependency versions are repeated 27+ times

---

## Detailed Findings

### 1. [Critical] Bean & Widget Code Duplication Across scrolling-aosp-* Modules

**Severity: Critical**

The following classes are copy-pasted with minor variations across 9 modules (app + 8 scrolling-aosp-* modules):

| Class | Copies |
|-------|--------|
| `FriendCircleBean.java` | 10 (incl. Compose Kotlin variant) |
| `CommentBean.java` | 10 |
| `UserBean.java` | 10 |
| `PraiseBean.java` | 9 |
| `OtherInfoBean.java` | 9 |
| `NineGridView.java` | 9 |
| `NineImageAdapter.java` | 9 |

Each copy lives in a different package (e.g., `com.example.wechatfriendforperformance.beans` vs `com.android.wechatfriendforpower.beans`), making them drift independently. The `scrolling-aosp-power` variant of `FriendCircleBean` adds `TranslationState`, `isExpanded`, `showComment/showPraise/showCheckAll` fields that other variants lack.

**Files:**
- `scrolling-aosp-performance/src/main/java/.../beans/FriendCircleBean.java`
- `scrolling-aosp-power/src/main/java/.../beans/FriendCircleBean.java`
- (and 7 more copies)

**Recommendation:** Extract beans, widgets, and adapters into a new `scrolling-common` library module. Modules that need extra fields can extend the base classes.

---

### 2. [Critical] Parallel LoadSimulator Implementations (Java vs Kotlin)

**Severity: Critical**

Two completely separate `LoadSimulator` implementations exist:

1. **`load-config/LoadSimulator.java`** (Java, 944 lines) -- Used by scrolling modules. Focuses on frame-based load with math, Bitmap, sorting algorithms. Uses `LoadConfig` constants and `LoadType` annotations.

2. **`launch-common/LoadSimulator.kt`** (Kotlin, 556 lines) -- Used by launch modules. Focuses on app-startup simulation with CPU, IO, Binder, Crypto, SQLite loads. Uses its own internal `LoadType` enum (LIGHT/MEDIUM/HEAVY).

These share the same class name but fundamentally different concerns:
- Java version: **frame rendering load** (in-frame, between-frame, mixed)
- Kotlin version: **app lifecycle load** (Application init, Activity init, async network)

**Additionally**, `switch-common` has its own load system (`SwitchLoadManager`, `RealLoadExecutor`, `SwitchLoadType`) that is completely independent.

**Files:**
- `load-config/src/main/java/com/example/loadconfig/LoadSimulator.java`
- `launch-common/src/main/java/com/example/launch/common/LoadSimulator.kt`
- `switch-common/src/main/java/com/example/switch_common/SwitchLoadManager.kt`

**Recommendation:** The separate implementations are partially justified by different use cases. However:
- Rename to avoid confusion: `FrameLoadSimulator` (load-config), `LaunchLoadSimulator` (launch-common), keep `SwitchLoadManager`
- Extract shared primitives (CPU load, IO load, Binder load, memory allocation) into a `load-primitives` library that all three can depend on. The launch-common `runCpuLoad()`, `runIoLoad()`, `runBinderLoad()`, `runMemoryLoad()` helpers are general-purpose and reusable.

---

### 3. [High] No Shared Build Convention Plugin -- Massive build.gradle Repetition

**Severity: High**

Every module repeats the same boilerplate:
- `compileSdk 34` (27+ times)
- `targetSdk 34` (27+ times)
- `versionCode 3` / `versionName "1.2.0"` (27+ times)
- Signing config with env-var fallback (27+ times)
- `compileOptions { JavaVersion.VERSION_11 }` (27+ times)
- Common dependencies (appcompat, material, constraintlayout) repeated with version drift

**Files:** Every `*/build.gradle` in the project.

**Recommendation:** Create a Gradle convention plugin or `buildSrc` shared configuration:
```groovy
// buildSrc/src/main/groovy/perf-test-app.gradle
// Centralizes compileSdk, targetSdk, versionCode, signing, compileOptions
```

---

### 4. [High] Inconsistent Java/Kotlin Compatibility Targets

**Severity: High**

| Module | Java Target | Kotlin JVM Target |
|--------|------------|------------------|
| `app` | VERSION_11 | N/A (Java only) |
| `load-config` | VERSION_11 | N/A |
| `launch-common` | VERSION_11 | 11 |
| `switch-common` | **VERSION_17** | **17** |
| All scrolling-aosp-* | VERSION_11 | N/A |
| `scrolling-aosp-customscroller` | VERSION_11 | N/A |
| `scrolling-aosp-renderstress` | VERSION_11 | N/A |

`switch-common` uses Java 17, while all other modules use Java 11. This inconsistency can cause runtime issues if switch-common code is invoked by modules compiled with Java 11.

**Files:**
- `switch-common/build.gradle:27` -- `sourceCompatibility JavaVersion.VERSION_17`
- `launch-common/build.gradle:38` -- `sourceCompatibility JavaVersion.VERSION_11`

**Recommendation:** Standardize on Java 17 across all modules (or Java 11 if backward compat is needed). Centralize in convention plugin.

---

### 5. [High] Inconsistent minSdk Values

**Severity: High**

| minSdk | Modules |
|--------|---------|
| **21** | `app`, `load-config` |
| **24** | All other modules (27 modules) |

The `app` module (minSdk 21) depends on `load-config` (minSdk 21), but no scrolling/launch/switch module uses minSdk 21. The `load-config` library exposes `api` dependencies (RecyclerView, Lifecycle) which all consumers at minSdk 24 would inherit.

**Files:**
- `app/build.gradle:8` -- `minSdk 21`
- `load-config/build.gradle:10` -- `minSdk 21`

**Recommendation:** Unify minSdk to 24 across all modules unless there's a specific need for API 21 support.

---

### 6. [High] Package Naming Inconsistency

**Severity: High**

Package names follow no consistent convention:

| Module | Package |
|--------|---------|
| `app` | `com.kcrason.highperformancefriendscircle` |
| `scrolling-aosp-performance` | `com.example.wechatfriendforperformance` |
| `scrolling-aosp-power` | **`com.android.wechatfriendforpower`** |
| `scrolling-aosp-douyin` | `com.example.wechatfriendfordouyin` |
| `load-config` | `com.example.loadconfig` |
| `launch-common` | `com.example.launch.common` |
| `switch-common` | `com.example.switch_common` |
| `switch-aosp` | `com.example.switch_aosp` |

Issues:
1. `com.android.*` prefix in `scrolling-aosp-power` collides with AOSP framework namespace
2. `app` uses `com.kcrason.*` (original author prefix) while everything else uses `com.example.*`
3. `switch_common` uses underscores while `launch.common` uses dots
4. `com.example.*` is a placeholder -- not suitable for production

**Recommendation:** Adopt a consistent scheme like `com.example.perftest.scrolling.power`, `com.example.perftest.launch.aosp`, etc. Or use a real domain-based prefix.

---

### 7. [Medium] Dependency Version Drift Across Modules

**Severity: Medium**

| Dependency | Versions Found |
|-----------|---------------|
| `appcompat` | `1.6.1` (app, launch-*, switch-*) vs `1.7.1` (scrolling-*) |
| `material` | `1.11.0` (launch-*, switch-*) vs `1.12.0` (scrolling-*) |
| `constraintlayout` | `2.1.4` (app, launch-*, switch-*) vs `2.2.1` (scrolling-*) |
| `junit` | `4.13.2` (all -- consistent) |
| `test.ext:junit` | `1.1.5` (scrolling-compose) vs `1.2.1` (others) |
| `coroutines` | `1.7.1` (launch-common) vs `1.7.3` (switch-common) |
| `kotlin-stdlib` force | `1.8.22` in app only |

**Recommendation:** Use a `libs.versions.toml` version catalog (Gradle 7.4+) or ext{} block in root build.gradle to centralize dependency versions.

---

### 8. [Medium] launch-common Has Duplicate LoadType Relative to load-config

**Severity: Medium**

`load-config/LoadType.java` defines 11 load types (MINIMAL through LONG_FRAME) with `@IntDef`.
`launch-common/LoadSimulator.kt` defines its own `enum class LoadType { LIGHT, MEDIUM, HEAVY }`.
`switch-common/SwitchLoadType.kt` defines a third independent type system with `SelfLoadLevel` and `BackgroundLoadLevel`.

Three separate type systems for conceptually related "load levels" creates confusion.

**Files:**
- `load-config/src/main/java/com/example/loadconfig/LoadType.java`
- `launch-common/src/main/java/com/example/launch/common/LoadSimulator.kt:38-40`
- `switch-common/src/main/java/com/example/switch_common/SwitchLoadType.kt`

**Recommendation:** Define a shared base `LoadLevel` (LIGHT/MEDIUM/HEAVY) in `load-config` that all modules reference. Module-specific type systems can compose/extend this base.

---

### 9. [Medium] Scrolling WebView Modules Have Near-Identical Code Structure

**Severity: Medium**

The three GeckoView-based modules (`scrolling-webview-surface`, `scrolling-webview-texture`, `scrolling-webview-imagereader`) each have 13+ Java files with the same names:

```
BaseGeckoViewSurfaceActivity.java    (surface)
BaseGeckoViewTextureActivity.java    (texture)
GeckoViewApplication.java           (all three)
GeckoViewConstants.java             (all three)
GeckoViewMainActivity.java          (all three)
GeckoViewDataCenter.java            (all three)
HeavyLoadGeckoViewActivity.java     (all three)
LightLoadGeckoViewActivity.java     (all three)
MediumLoadGeckoViewActivity.java    (all three)
... (10 load-variant Activities each)
```

The only real difference between surface/texture/imagereader is the rendering backend in the base activity.

**Recommendation:** Extract common GeckoView code into a `scrolling-webview-common` module. Each variant module only needs the base activity override.

---

### 10. [Medium] Architecture Pattern Inconsistency: Only 2 of 27 Modules Use MVVM/Hilt

**Severity: Medium**

MVVM + Hilt DI is used **only** in:
- `scrolling-aosp-customscroller` (CustomScrollViewModel, @HiltAndroidApp, @AndroidEntryPoint, AppModule)
- `scrolling-aosp-renderstress` (same structure, nearly identical code)

All other 25+ modules use direct Activity-based architecture with no ViewModel, no DI framework, and no repository pattern. The Hilt gradle plugin is declared in root `build.gradle` but only applied in 2 module build files.

**Files:**
- `scrolling-aosp-customscroller/src/.../CustomScrollViewModel.java`
- `scrolling-aosp-renderstress/src/.../CustomScrollViewModel.java`
- `scrolling-aosp-customscroller/src/.../di/AppModule.java`
- `scrolling-aosp-renderstress/src/.../di/AppModule.java`

**Recommendation:** This is acceptable if MVVM+Hilt is specifically being tested in those modules. Document this as intentional variation rather than a pattern to adopt project-wide. Consider adding a comment in those modules explaining why they differ.

---

### 11. [Medium] app Module Has Legacy Code and Unused RxJava Dependency

**Severity: Medium**

The `app` module:
- Uses `com.kcrason.highperformancefriendscircle` package (original forked project)
- Includes RxJava dependency (used for data loading) while newer modules use coroutines
- Has `Kotlin stdlib force 1.8.22` resolution strategy even though it's a pure Java module
- Contains the most complete Friends Circle implementation (emoji panel, spans, etc.)

This appears to be the original project that the performance testing modules were forked from. Its role as the "baseline" vs "legacy" is unclear.

**Files:**
- `app/build.gradle:42-45` -- Kotlin force resolution in pure Java module
- `app/build.gradle:60-61` -- RxJava2 dependencies

**Recommendation:** Clarify the app module's role. If it's the baseline "no performance test" variant, document that. Remove the unnecessary Kotlin stdlib force resolution.

---

### 12. [Low] Signing Config Hardcodes Fallback Password

**Severity: Low**

All modules use `storePassword System.getenv("KEYSTORE_PASSWORD") ?: "123456"` as fallback. While this is for local development convenience, it appears in every build.gradle.

**Recommendation:** Centralize in convention plugin or `gradle.properties`. Consider using a `keystore.properties` file (which already exists at root) instead of env-var-with-fallback pattern.

---

### 13. [Low] scrolling-aosp-douyin Does Not Depend on load-config

**Severity: Low**

`scrolling-aosp-douyin/build.gradle` does NOT include `implementation project(':load-config')`, unlike most other scrolling modules. This module appears to focus on video-feed scrolling and may not need frame-based load simulation, but this breaks the pattern.

**Files:**
- `scrolling-aosp-douyin/build.gradle:44-62` -- No load-config dependency

**Recommendation:** If intentional (no load injection needed for Douyin-style feed), document why. Otherwise, add the dependency.

---

### 14. [Low] launch-common productFlavors Duplicate What Could Be Runtime Config

**Severity: Low**

`launch-common` defines three product flavors (light/medium/heavy) that set `BuildConfig.LOAD_TYPE`. This triples the build variants. The `LaunchConfig.kt` already supports runtime intent-based load type selection.

**Files:**
- `launch-common/build.gradle:16-29`
- `launch-common/src/.../LaunchConfig.kt`

**Recommendation:** If automation drives these via intent extras, the flavors may be redundant. If APK-level separation is required for device testing, keep them but document the rationale.

---

## Module Dependency Graph

```
app --> load-config

scrolling-aosp-performance --> load-config
scrolling-aosp-power       --> load-config
scrolling-aosp-picasso     --> load-config
scrolling-aosp-video       --> load-config
scrolling-aosp-ebook       --> load-config
scrolling-aosp-softwarerender --> load-config
scrolling-aosp-purerenderthread --> load-config
scrolling-aosp-dualwindow  --> load-config
scrolling-aosp-mixedrender --> load-config
scrolling-compose          --> load-config
scrolling-webview          --> load-config
scrolling-webview-surface  --> load-config (assumed)
scrolling-webview-texture  --> load-config (assumed)
scrolling-webview-imagereader --> load-config (assumed)
scrolling-gl-map           --> (unknown)
scrolling-surface-map      --> (unknown)

scrolling-aosp-douyin      --> (NO load-config)
scrolling-aosp-customscroller --> load-config (+ Hilt)
scrolling-aosp-renderstress   --> load-config (+ Hilt)

launch-aosp    --> launch-common
launch-compose --> launch-common
launch-webview --> launch-common
launch-gl      --> launch-common
launch-game    --> launch-common

switch-aosp    --> switch-common
switch-webview --> switch-common
switch-flutter --> switch-common
```

No circular dependencies detected. The dependency graph is a clean star topology with `load-config` and `*-common` modules at the center.

---

## Refactoring Recommendations (Priority Order)

### Phase 1: Build System Consolidation (High Impact, Low Risk)
1. Create a `buildSrc` or convention plugin for shared `compileSdk`, `targetSdk`, `versionCode`, signing config, and `compileOptions`
2. Create `libs.versions.toml` for centralized dependency management
3. Unify `minSdk` to 24 and Java target to a single version (11 or 17)

### Phase 2: Extract Shared Code (High Impact, Medium Risk)
4. Create `scrolling-common` module containing:
   - `beans/` (FriendCircleBean, CommentBean, UserBean, PraiseBean, OtherInfoBean)
   - `widgets/NineGridView.java`
   - `adapters/NineImageAdapter.java`
   - `interfaces/` (OnItemClickPopupMenuListener, OnPraiseOrCommentClickListener)
5. Create `scrolling-webview-common` for shared GeckoView code
6. Extract `load-primitives` module from `launch-common/LoadSimulator.kt` helper methods

### Phase 3: Naming & Documentation (Medium Impact, Low Risk)
7. Standardize package naming (`com.example.perftest.*`)
8. Rename LoadSimulator classes to avoid same-name confusion
9. Define a shared `LoadLevel` base type in `load-config`
10. Document the intentional architecture differences (MVVM in 2 modules, no-load in douyin)

---

## Statistics

| Metric | Value |
|--------|-------|
| Total modules | 31 |
| Application modules | 28 |
| Library modules | 3 (load-config, launch-common, switch-common) |
| Languages | Java (primary), Kotlin (launch/switch/compose) |
| Estimated duplicated files | ~60 files across scrolling-aosp-* |
| Build variants per launch module | 6 (3 flavors x 2 build types) |
| Hilt modules | 2 of 28 app modules |
| Modules with tests | ~3 (scrolling-aosp-power has Robolectric) |
