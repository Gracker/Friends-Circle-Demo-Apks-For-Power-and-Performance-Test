# Test Module Consistency Review

## Executive Summary

This project contains **31 modules** (settings.gradle) organized into three test families -- **Scrolling** (19 modules), **Launch** (6 modules), and **Switch** (4 modules) -- plus shared libraries (`load-config`, `launch-common`, `switch-common`) and the shell `app` module. The modules simulate various Android rendering pipelines (AOSP View, Compose, WebView, GeckoView, GLSurfaceView, SurfaceView, Flutter-style Canvas) under controlled load conditions for performance benchmarking.

**Key Findings:**

1. **Massive bean/model duplication**: `FriendCircleBean`, `CommentBean`, `UserBean`, `PraiseBean`, `OtherInfoBean` are copy-pasted across 8+ scrolling-aosp modules with slightly different fields. This is the single largest source of code duplication.
2. **Inconsistent Constants**: `PerformanceConstants`, `PowerConstants`, `WebViewConstants`, `ComposeConstants`, `DualWindowConstants` all contain overlapping USER_NAMES, CONTENTS, TIMES, SOURCES, LOCATIONS arrays with varying lengths and content.
3. **Inconsistent DataCenter patterns**: Singleton `getInstance()` (performance, webview) vs. static methods (power) vs. Kotlin `object` (compose) -- no shared base.
4. **Adapter duplication**: `PerformanceFriendCircleAdapter`, `PowerFriendCircleAdapter`, `DualWindowFriendCircleAdapter` share ~70% identical logic (header handling, ViewHolder binding, Glide loading) with divergent patterns.
5. **Activity explosion in scrolling modules**: Each load type requires a separate Activity class (11 per module), leading to 100+ near-identical Activity files. Only the load type constant differs.
6. **Build config inconsistency**: Signing config uses two different approaches (`System.getenv` with hardcoded defaults vs. `keystore.properties` file). Java compatibility varies (VERSION_11 vs. VERSION_17).
7. **Launch modules are well-designed**: Flavor-based variants with `launch-common` shared library demonstrate the ideal pattern. Switch modules also share `switch-common` effectively.
8. **Scrolling modules lack a shared library**: Unlike launch/switch, scrolling modules have no `scrolling-common` module.

---

## Module-by-Module Analysis

### 1. Scrolling Modules (19 modules)

#### 1.1 scrolling-aosp-performance

| Aspect | Details |
|---|---|
| **Language** | Java |
| **Activity Pattern** | 11 separate Activity classes (Minimal, Light, Medium, Heavy, LightBetweenFrames, ..., LongFrame) |
| **Adapter** | `PerformanceFriendCircleAdapter` (560 lines), supports header, uses `LoadSimulator` |
| **DataCenter** | Singleton `PerformanceDataCenter.getInstance()`, caches by load type, uses `LoadConfig` seeds |
| **Beans** | 5 classes: FriendCircleBean, CommentBean, UserBean, PraiseBean, OtherInfoBean |
| **Constants** | `PerformanceConstants` (279 lines), USER_NAMES/CONTENTS/TIMES/SOURCES/LOCATIONS/COMMENT_CONTENTS |
| **MainActivty** | Button-per-load-type with `View.OnClickListener`, `checkForDirectActivityLaunch()` for ADB |
| **Load Execution** | `Choreographer.FrameCallback` + `LoadSimulator.executeInFrameLoad()` |
| **Widgets** | `NineGridView`, `NineImageAdapter` |
| **Manifest** | Custom `Application`, 12 activities, `ImageViewerActivity` |

**Issues:**
- `PerformanceMainActivity.onClick()` has 11 if-else branches with duplicated intent-creation logic (lines 245-339)
- `checkForDirectActivityLaunch()` duplicates the same routing as `onClick()` (lines 108-156)
- Every load Activity (Light, Medium, Heavy, etc.) is nearly identical -- only `mLoadType` default and layout differ
- `LightLoadActivity` vs `HeavyLoadActivity`: 97% identical code (diff only in TAG, default loadType, and Choreographer cleanup in onPause)

#### 1.2 scrolling-aosp-power

| Aspect | Details |
|---|---|
| **Language** | Java |
| **Package** | `com.android.wechatfriendforpower` (note: `com.android` prefix, inconsistent with others) |
| **Activity Pattern** | Single `PowerMainActivity` + `PowerActivity` (no load variants) |
| **Adapter** | `PowerFriendCircleAdapter` (367 lines) |
| **DataCenter** | Static methods `PowerDataCenter.makeFriendCircleBeans()` (no caching, no load types) |
| **Beans** | 5 classes, **divergent**: FriendCircleBean has extra fields (isExpanded, showComment, showPraise, showCheckAll, TranslationState, contentSpan) |
| **Constants** | `PowerConstants` (234 lines), different set of content |
| **Interfaces** | `OnItemClickPopupMenuListener` has 4 methods (copy, collection, translation, hideTranslation) vs 1 method in performance |
| **Tests** | Has unit tests (PowerActivityTest, BeanTest, PowerDataGeneratorTest, etc.) -- unique among scrolling modules |

**Issues:**
- Package uses `com.android` instead of `com.example` -- inconsistent
- `FriendCircleBean` has significantly different fields than the performance version
- No load type support -- simulates a single "steady state" scenario
- `NineImageAdapter` constructor takes different parameters than performance version

#### 1.3 scrolling-compose

| Aspect | Details |
|---|---|
| **Language** | Kotlin |
| **Activity Pattern** | Single `MainActivity` with navigation-based screen selection |
| **Data** | Kotlin data classes: `FriendCircleBean`, `UserBean`, `CommentBean`, `PraiseBean`, `OtherInfoBean` -- all in `Models.kt` |
| **DataCenter** | `ComposeDataCenter` (Kotlin object), caches by base load type |
| **Constants** | `ComposeConstants` (111 lines) |
| **Load Type** | Own `LoadType` enum with `BaseLoadType` mapping, separate from `com.example.loadconfig.LoadType` |
| **UI** | Compose `LazyColumn` with `FriendCircleScreen`, `NineGridImages` |

**Issues:**
- Defines its own `LoadType`/`BaseLoadType` enum instead of using the shared `load-config` module's `LoadType`
- `ComposeConstants` has overlapping but not identical content with `PerformanceConstants`
- No `LoadSimulator` integration -- load simulation is handled differently in Compose vs AOSP

#### 1.4 scrolling-webview (+ surface, texture, imagereader variants)

| Aspect | Details |
|---|---|
| **Language** | Java |
| **Activity Pattern** | `BaseFriendCircleWebViewActivity` base class + 10 concrete load activities |
| **Data** | `WebViewDataCenter` singleton, generates JSON for JS consumption |
| **Constants** | `WebViewConstants` (245 lines) |
| **Load Execution** | JavaScript injection via `evaluateJavascript()` for in-WebView load |
| **Variants** | scrolling-webview-surface/texture/imagereader use GeckoView with identical pattern but different rendering backends |

**Issues:**
- `LightLoadWebViewActivity` contains ~250 lines of inline JavaScript string concatenation (enormous, hard to maintain)
- All GeckoView variants (surface, texture, imagereader) have near-identical code with package name changes
- `BaseFriendCircleWebViewActivity` has `Thread.sleep()` calls in touch listeners (anti-pattern, blocks main thread)
- webview Manifest exports all sub-activities (security: `exported="true"`) while performance module does not

#### 1.5 scrolling-gl-map / scrolling-surface-map

| Aspect | Details |
|---|---|
| **Language** | Java |
| **Activity Pattern** | `BaseGLMapActivity`/`BaseMapActivity` base class + 11 concrete activities |
| **Rendering** | `GLMapView` (GLSurfaceView + custom renderer) / `MapSurfaceView` (SurfaceView + thread) |
| **Signing** | Uses `keystore.properties` file approach (different from most other modules) |

**Issues:**
- Different signing config pattern than most modules
- `BaseGLMapActivity` is clean and well-designed (69 lines) -- good template for others

#### 1.6 Specialized scrolling modules (douyin, ebook, video, customscroller, renderstress, mixedrender, picasso, purerenderthread, softwarerender)

These are unique single-purpose modules that each demonstrate a specific rendering scenario:

| Module | Unique Feature |
|---|---|
| `scrolling-aosp-douyin` | Vertical video scroller (TikTok-style) |
| `scrolling-aosp-ebook` | EPUB reader with page splitting |
| `scrolling-aosp-video` | Video feed scrolling |
| `scrolling-aosp-customscroller` | Custom OverScroller implementation |
| `scrolling-aosp-renderstress` | Room DB + custom Canvas rendering + MVVM |
| `scrolling-aosp-mixedrender` | Mixed rendering pipeline |
| `scrolling-aosp-picasso` | Picasso image loading (vs Glide) |
| `scrolling-aosp-purerenderthread` | Pure RenderThread workload |
| `scrolling-aosp-softwarerender` | Software rendering (no hardware acceleration) |

**Issues:**
- `scrolling-aosp-customscroller` and `scrolling-aosp-renderstress` duplicate beans/interfaces/widgets despite similar architectures
- `AppInfo` model class is duplicated across douyin, ebook, renderstress, performance modules

### 2. Launch Modules (6 modules)

#### 2.1 launch-common (shared library)

Well-designed shared library containing:
- `LoadSimulator` -- comprehensive load simulation (CPU, IO, Binder, Crypto, SQLite, Memory)
- `LifecycleLoadSimulator` -- lifecycle-aware load injection
- `LaunchConfig` -- configuration
- `PerformanceLogger` -- timing measurement
- `SuccessDialogFragment` -- completion UI

Uses **product flavors** (`light`/`medium`/`heavy`) with `BuildConfig.LOAD_TYPE` to control load at build time.

**Strength**: This is the best-designed module family in the project.

#### 2.2 launch-aosp / launch-compose / launch-webview / launch-gl

| Aspect | Details |
|---|---|
| **Language** | Kotlin |
| **Pattern** | Thin Activity that delegates to `launch-common` `LoadSimulator` |
| **Flavor** | 3 flavors per module (light/medium/heavy), each produces a separate APK |
| **Code** | launch-aosp: 150 lines, launch-compose/webview/gl: similarly thin |

**Consistency**: All four follow the same pattern consistently.

#### 2.3 launch-game

| Aspect | Details |
|---|---|
| **Language** | Java (inconsistent with other launch modules which use Kotlin) |
| **Pattern** | Uses own `LoadSimulator.java` instead of `launch-common`'s |
| **Unique** | `GameGLSurfaceView`, `GameEngine` -- game-specific rendering |
| **Flavor Config** | Uses `LOAD_DURATION_MS` buildConfigField (different from other launch modules' `LOAD_TYPE`) |

**Issues:**
- Has its own `LoadSimulator.java` instead of using `launch-common`'s version
- Written in Java while all other launch modules use Kotlin
- Uses a different flavor config approach (`LOAD_DURATION_MS` vs `LOAD_TYPE`)

### 3. Switch Modules (4 modules)

#### 3.1 switch-common (shared library)

Well-designed shared library containing:
- `SwitchLoadManager` -- orchestrates self + background loads
- `SwitchLoadType` -- enum with 10 combinations of self/background load levels
- `RealLoadExecutor` -- real inflate/View/Binder/IO load execution
- `BackgroundLoadService` -- background thread load service
- `MainThreadLoadRunner` -- main thread load scheduling
- Custom views: `HeavyCustomView`, `MediumCustomView`, `LightCustomView`, `ComplexContainerView`, `DataBindingView`

**Issue:** Uses `JavaVersion.VERSION_17` while all other modules use `VERSION_11`.

#### 3.2 switch-aosp / switch-webview / switch-flutter

| Aspect | switch-aosp | switch-webview | switch-flutter |
|---|---|---|---|
| **MainActivity** | Button-per-load-type, launches target Activity | Identical pattern | Identical pattern |
| **BaseTargetActivity** | Delegates to `SwitchLoadManager`, measures timing | Adds WebView loading | Adds FlutterStyleView |
| **TargetActivities** | 10 one-liner classes extending BaseTargetActivity | Same | Same |
| **Completion** | `RealLoadExecutor.setCompletionCallback` | Same | Same |

**Consistency**: All three switch modules follow the same pattern excellently. `BaseTargetActivity` in each module is ~100 lines with platform-specific additions.

**Issues:**
- `scheduleNotifySwitchComplete()` is copy-pasted across all three BaseTargetActivity files (lines 72-83 in switch-aosp, 80-91 in switch-webview, 72-83 in switch-flutter) -- could be extracted to switch-common.

---

## Pattern Comparison Tables

### Bean/Model Classes Across Scrolling Modules

| Field | performance | power | compose | dualwindow | renderstress | webview |
|---|---|---|---|---|---|---|
| viewType | int | int | N/A (data class has id) | int | int | JSON |
| content | String | String + contentSpan | String | String | String | JSON |
| userBean | UserBean | UserBean | UserBean (data class) | UserBean | UserBean | JSON |
| otherInfoBean | OtherInfoBean | OtherInfoBean | OtherInfoBean (data class) | OtherInfoBean | OtherInfoBean | JSON |
| imageUrls | List<String> | List<String> | List<String> | List<String> | List<String> | JSON |
| commentBeans | List<CommentBean> | List<CommentBean> | List<CommentBean> (data class) | List<CommentBean> | List<CommentBean> | JSON |
| praiseBeans | List<PraiseBean> | List<PraiseBean> | List<PraiseBean> (data class) | List<PraiseBean> | List<PraiseBean> | JSON |
| praiseSpan | SpannableStringBuilder | SpannableStringBuilder | N/A | SpannableStringBuilder | SpannableStringBuilder | N/A |
| **Extra fields** | -- | isExpanded, showComment, showPraise, showCheckAll, translationState | id | -- | -- | -- |

**Bean code is duplicated 8+ times** across: performance, power, customscroller, renderstress, dualwindow, mixedrender, picasso (each with their own beans/ package).

### DataCenter Pattern Comparison

| Module | Pattern | Caching | Load Type Support | Context Dependency |
|---|---|---|---|---|
| performance | Singleton `.getInstance()` | By load type (3 caches) | Yes (via LoadConfig) | `setContext()` method |
| power | Static methods | None | No (single mode) | Context parameter |
| compose | Kotlin `object` | By base load type (3 caches) | Yes (own LoadType) | No |
| webview | Singleton `.getInstance()` | By load type | Yes (generates JSON) | Context in method |
| dualwindow | Singleton `.getInstance()` | By load type | Yes (via LoadConfig) | `setContext()` method |

### MainActivty Routing Pattern Comparison

| Module | Routing Pattern | Direct Launch Support | Lines of Routing Code |
|---|---|---|---|
| performance | if-else chain in `onClick()` + switch in `checkForDirectActivityLaunch()` | Yes (via `activity_type` extra) | ~200 lines |
| webview | if-else chain in `onClick()` + switch in `checkForDirectActivityLaunch()` | Yes (via `activity_type` extra) | ~200 lines |
| gl-map | Same pattern | Yes | ~150 lines |
| surface-map | Same pattern | Yes | ~150 lines |
| dualwindow | Same pattern | Yes | ~150 lines |
| compose | Navigation-based | Yes (via intent extra) | ~50 lines |
| switch-aosp | Kotlin `when` expression with lambda buttons | No | ~50 lines |

### Build Configuration Comparison

| Aspect | Most Modules | gl-map / surface-map | switch-common |
|---|---|---|---|
| **Signing** | `System.getenv` with hardcoded defaults | `keystore.properties` file | No signing config |
| **Java Compat** | VERSION_11 | VERSION_11 | VERSION_17 |
| **compileSdk** | 34 | 34 | 34 |
| **minSdk** | 24 | 24 | 24 |
| **versionCode** | 3 | 3 | N/A (library) |
| **versionName** | "1.2.0" | "1.2.0" | N/A |

### Exported Activity Security

| Module | Sub-Activities exported? |
|---|---|
| scrolling-aosp-performance | No (only launcher) |
| scrolling-webview | **Yes** (all 10 sub-activities) |
| scrolling-gl-map | **Yes** (all 11 sub-activities) |
| scrolling-surface-map | **Yes** (all sub-activities) |
| scrolling-webview-surface/texture/imagereader | **Yes** (all sub-activities) |

---

## Specific Refactoring Proposals

### Proposal 1: Create `scrolling-common` Shared Library

**Impact**: Eliminates ~5000+ lines of duplicated code across 8+ modules.

Create a new `scrolling-common` module containing:

```
scrolling-common/
  src/main/java/com/example/scrolling/common/
    beans/
      FriendCircleBean.java      // Unified bean
      CommentBean.java
      UserBean.java
      PraiseBean.java
      OtherInfoBean.java
    data/
      ScrollingConstants.java     // Unified constants (USER_NAMES, CONTENTS, etc.)
      BaseDataCenter.java         // Abstract DataCenter with caching + load type support
    adapters/
      BaseFriendCircleAdapter.java  // Base adapter with header, ViewHolder, Glide
      NineImageAdapter.java         // Shared nine-grid adapter
    widgets/
      NineGridView.java            // Shared widget
    interfaces/
      OnPraiseOrCommentClickListener.java
    utils/
      SpanUtils.java               // Shared span utilities
    model/
      AppInfo.java                 // Shared app info model
```

Each scrolling module would then only contain:
- Its unique Activity/rendering logic
- Module-specific extensions to shared beans (if needed)
- Module-specific layouts/resources

### Proposal 2: Consolidate Load Activities with Generic Activity

**Impact**: Reduces 11 Activity classes per scrolling module to 1 generic Activity + 1 config mapping.

Instead of `LightLoadActivity`, `MediumLoadActivity`, `HeavyLoadActivity`, etc., use a single parameterized Activity:

```java
// Before: 11 near-identical Activity files per module
public class LightLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback { ... }
public class MediumLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback { ... }
public class HeavyLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback { ... }
// ... 8 more

// After: 1 generic Activity
public class ScrollingLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoadType = getIntent().getIntExtra(EXTRA_LOAD_TYPE, LoadType.LIGHT);
        // All setup driven by mLoadType
    }
}
```

**Note**: This is already partially done in the webview family via `BaseFriendCircleWebViewActivity`, and in the GL-map family via `BaseGLMapActivity`. The proposal is to complete it for all families.

### Proposal 3: Unify MainActivty Routing with Data-Driven Approach

**Impact**: Reduces ~200 lines of routing code per module to ~30 lines.

```java
// Before: 200+ lines of if-else chains
@Override
public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.btn_minimal_load) {
        Intent intent = new Intent(this, MinimalLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, LoadType.MINIMAL);
        startActivity(intent);
    } else if (id == R.id.btn_light_load) {
        // ... repeated 10 more times
    }
}

// After: Data-driven routing (combined with Proposal 2)
private static final Map<Integer, Integer> BUTTON_TO_LOAD_TYPE = Map.of(
    R.id.btn_minimal_load, LoadType.MINIMAL,
    R.id.btn_light_load, LoadType.LIGHT,
    R.id.btn_medium_load, LoadType.MEDIUM,
    // ...
);

@Override
public void onClick(View v) {
    Integer loadType = BUTTON_TO_LOAD_TYPE.get(v.getId());
    if (loadType != null) {
        DataCenter.getInstance().clearCachedData();
        Intent intent = new Intent(this, ScrollingLoadActivity.class);
        intent.putExtra(EXTRA_LOAD_TYPE, loadType);
        startActivity(intent);
    }
}
```

### Proposal 4: Extract `scheduleNotifySwitchComplete()` to switch-common

**Impact**: Small but eliminates triple duplication across switch modules.

```kotlin
// In switch-common, add to BaseTargetActivityHelper or SwitchLoadManager:
object SwitchCompletionHelper {
    fun scheduleNotifySwitchComplete(
        activity: AppCompatActivity,
        loadType: SwitchLoadType,
        startTime: Long,
        tag: String = "SwitchPerf"
    ) {
        var notified = false
        RealLoadExecutor.setCompletionCallback {
            if (!notified) {
                notified = true
                val duration = System.currentTimeMillis() - startTime
                Trace.beginSection("NotifySwitchComplete")
                Log.d(tag, "$tag switch complete in ${duration}ms")
                SwitchLoadManager.notifySwitchComplete(activity)
                Trace.endSection()
            }
        }
    }
}
```

### Proposal 5: Unify Signing Configuration

**Impact**: Single source of truth for signing across all modules.

Move the signing config to the root `build.gradle`:

```groovy
// root build.gradle
subprojects {
    afterEvaluate {
        if (plugins.hasPlugin('com.android.application')) {
            android {
                signingConfigs {
                    release {
                        def keystorePropertiesFile = rootProject.file("keystore.properties")
                        if (keystorePropertiesFile.exists()) {
                            def keystoreProperties = new Properties()
                            keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
                            storeFile file(keystoreProperties['storeFile'])
                            storePassword keystoreProperties['storePassword']
                            keyAlias keystoreProperties['keyAlias']
                            keyPassword keystoreProperties['keyPassword']
                        }
                    }
                }
            }
        }
    }
}
```

### Proposal 6: Unify Java/Kotlin Compatibility

**Impact**: Prevents build issues and ensures consistent behavior.

Standardize on `JavaVersion.VERSION_17` across all modules (currently `switch-common` uses 17, all others use 11). Set in root `build.gradle`:

```groovy
subprojects {
    afterEvaluate {
        if (plugins.hasPlugin('com.android.application') || plugins.hasPlugin('com.android.library')) {
            android {
                compileOptions {
                    sourceCompatibility JavaVersion.VERSION_17
                    targetCompatibility JavaVersion.VERSION_17
                }
                kotlinOptions {
                    jvmTarget = '17'
                }
            }
        }
    }
}
```

### Proposal 7: Fix Exported Activities

**Impact**: Security improvement.

Sub-activities in scrolling-webview, scrolling-gl-map, scrolling-surface-map, and GeckoView modules are unnecessarily `exported="true"`. These should only be exported if they need to be launched by external apps (e.g., automation tests via ADB). For ADB testing, only the main launcher Activity needs to be exported, combined with the existing `activity_type` intent extra routing.

```xml
<!-- Change from -->
<activity android:name=".LightLoadWebViewActivity" android:exported="true" ... />

<!-- To -->
<activity android:name=".LightLoadWebViewActivity" android:exported="false" ... />
```

### Proposal 8: Compose Module Should Use Shared LoadType

**Impact**: Eliminates the separate `LoadType`/`BaseLoadType` enum in scrolling-compose.

Currently `scrolling-compose` defines its own `LoadType` enum:
```kotlin
enum class LoadType { MINIMAL, LIGHT, MEDIUM, HEAVY, LIGHT_BETWEEN_FRAMES, ... }
enum class BaseLoadType { MINIMAL, LIGHT, MEDIUM, HEAVY }
```

This should use `com.example.loadconfig.LoadType` from the shared `load-config` module, consistent with all other scrolling modules.

---

## Priority Ranking

| Priority | Proposal | Effort | Impact |
|---|---|---|---|
| **P0** | #7 Fix exported activities | Low | Security |
| **P1** | #1 Create scrolling-common | High | ~5000 LOC reduction |
| **P2** | #2 Consolidate load Activities | Medium | ~3000 LOC reduction |
| **P3** | #3 Unify MainActivty routing | Low | ~1500 LOC reduction |
| **P3** | #5 Unify signing config | Low | Consistency |
| **P3** | #6 Unify Java compatibility | Low | Consistency |
| **P4** | #8 Compose use shared LoadType | Low | Consistency |
| **P4** | #4 Extract switch completion helper | Low | ~30 LOC reduction |
