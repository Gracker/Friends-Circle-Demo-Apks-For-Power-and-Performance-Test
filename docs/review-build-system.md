# Build System & Dependency Review

## Executive Summary

The HighPerformanceFriendsCircle project uses a traditional Groovy-based Gradle build system with **32 modules** (29 application modules + 3 library modules). The build system is functional but suffers from **significant version fragmentation**, **no centralized dependency management**, **inconsistent signing configurations**, and **massive CI/CD boilerplate duplication**. The project uses AGP 8.2.2 with Gradle 8.11.1 -- the Gradle wrapper is reasonably current, but the AGP version is behind (latest stable is 8.7.x as of early 2025). The most impactful modernization step would be adopting a Gradle Version Catalog (`libs.versions.toml`) to centralize the 40+ distinct dependency declarations scattered across modules.

---

## 1. Gradle Configuration Analysis

### 1.1 Root `build.gradle`

**File**: `build.gradle`

| Item | Value | Notes |
|------|-------|-------|
| AGP | 8.2.2 | Behind latest (8.7.x) |
| Kotlin Plugin | 1.9.22 | Behind latest (2.1.x) |
| Hilt Plugin | 2.51.1 | Current |
| Compose Compiler Plugin | 2.0.0 | Mismatch with Kotlin 1.9.22 (requires Kotlin 2.0+) |

**Findings**:

- **[HIGH] Kotlin/Compose Compiler Version Mismatch**: The root `build.gradle` declares `org.jetbrains.kotlin.plugin.compose` version `2.0.0`, but the Kotlin plugin is `1.9.22`. The Kotlin Compose compiler plugin 2.0.0 requires Kotlin 2.0.0+. This likely works only because the Compose plugin is `apply false` and only used in 2 modules. However, this is a latent breakage risk.
- **[MEDIUM] Legacy `buildscript` Block**: The project uses the legacy `buildscript { dependencies { classpath ... } }` pattern alongside the `plugins {}` block. This is redundant and should be migrated fully to the `plugins {}` DSL.
- **[LOW] Deprecated `rootProject.buildDir`**: The `clean` task uses `rootProject.buildDir` which is deprecated in Gradle 8.x. Should use `rootProject.layout.buildDirectory`.

### 1.2 `settings.gradle`

**File**: `settings.gradle`

- Plain module includes, no `pluginManagement` or `dependencyResolutionManagement` blocks.
- No version catalog declaration.
- Repositories are defined in `allprojects {}` in root `build.gradle` instead of the modern `dependencyResolutionManagement` in `settings.gradle`.

### 1.3 `gradle.properties`

**File**: `gradle.properties`

| Property | Value | Assessment |
|----------|-------|------------|
| `org.gradle.jvmargs` | `-Xmx2048m` | Adequate for this project size |
| `android.useAndroidX` | `true` | Correct |
| `android.enableJetifier` | `true` | **Should be removed** -- all deps are AndroidX now |
| `org.gradle.parallel` | `true` | Good |
| `org.gradle.caching` | `true` | Good |
| `org.gradle.workers.max` | `8` | Hardcoded -- may not suit all machines |

**Findings**:

- **[MEDIUM] Jetifier Still Enabled**: `android.enableJetifier=true` adds build overhead. All dependencies in the project are already AndroidX. Jetifier should be removed.
- **[LOW] Parallel Build Defined Twice**: The commented `org.gradle.parallel=true` on line 13 and the active one on line 23 are redundant.

### 1.4 `gradle-wrapper.properties`

**File**: `gradle/wrapper/gradle-wrapper.properties`

- Gradle 8.11.1 -- this is a recent version, well within compatibility range.
- Uses `-bin` distribution (no sources). This is fine for CI but `-all` can aid IDE experience.

---

## 2. Dependency Management

### 2.1 No Centralized Version Management

**Severity: HIGH**

There is no `gradle/libs.versions.toml` (Version Catalog), no `ext {}` block with shared versions, and no `versions.gradle` file. Every module independently declares its own dependency versions. This leads to the version inconsistencies documented below.

### 2.2 Version Inconsistencies Across Modules

The following table documents version drift found across modules:

| Dependency | Versions Found | Modules |
|-----------|---------------|---------|
| `appcompat` | **1.6.1** vs **1.7.1** | app, launch-*, switch-* use 1.6.1; scrolling-aosp-performance, scrolling-aosp-power, scrolling-webview, scrolling-aosp-douyin, scrolling-aosp-renderstress use 1.7.1 |
| `material` | **1.11.0** vs **1.12.0** | launch-*, switch-*, scrolling-gl-map use 1.11.0; scrolling-aosp-performance, scrolling-aosp-power, scrolling-webview, scrolling-aosp-douyin, scrolling-aosp-renderstress use 1.12.0 |
| `constraintlayout` | **2.1.4** vs **2.2.1** | app, launch-*, switch-*, scrolling-gl-map use 2.1.4; scrolling-aosp-performance, scrolling-aosp-power, scrolling-webview, scrolling-aosp-douyin, scrolling-aosp-renderstress use 2.2.1 |
| `webkit` | **1.9.0** vs **1.14.0** | launch-webview, switch-webview use 1.9.0; scrolling-webview uses 1.14.0 |
| `kotlinx-coroutines` | **1.7.1** vs **1.7.3** | launch-common uses 1.7.1; switch-common uses 1.7.3 |
| `compose-bom` | **2023.08.00** vs **2024.02.00** | launch-compose uses 2023.08.00; scrolling-compose uses 2024.02.00 |
| `espresso-core` | **3.5.1** vs **3.6.1** | app, scrolling-compose, scrolling-surface-map use 3.5.1; newer modules use 3.6.1 |
| `test.ext:junit` | **1.1.5** vs **1.2.1** | scrolling-compose, scrolling-surface-map use 1.1.5; newer modules use 1.2.1 |
| `kotlin-stdlib (forced)` | **1.8.22** | app module forces 1.8.22 but Kotlin plugin is 1.9.22 |

### 2.3 Kotlin Stdlib Resolution Conflict

**Severity: HIGH**

The `app/build.gradle` contains a `resolutionStrategy` that forces `kotlin-stdlib` to version `1.8.22`:

```groovy
force 'org.jetbrains.kotlin:kotlin-stdlib:1.8.22'
force 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.22'
force 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22'
```

The Kotlin plugin version is `1.9.22`. Forcing an older stdlib can cause subtle runtime issues where APIs available in 1.9.x are not present. This force should be removed in favor of letting the Kotlin plugin manage its own stdlib version.

### 2.4 Outdated / Legacy Dependencies

| Dependency | Current | Latest (approx.) | Severity |
|-----------|---------|----------|----------|
| RxJava 2 (`2.2.21`) | End-of-life | RxJava 3 or Kotlin Coroutines/Flow | LOW (app-only) |
| PhotoView (`2.3.0`) | Unmaintained | Consider alternatives | LOW |
| Glide `annotationProcessor` | Java annotation processor | Should use `kapt` or `ksp` in Kotlin modules | MEDIUM |
| Hilt `annotationProcessor` | Java annotation processor | Should use `kapt` or `ksp` | MEDIUM |
| Room `annotationProcessor` | Java annotation processor | Should use `ksp` | MEDIUM |

---

## 3. Build Variants & Flavors

### 3.1 Product Flavor Configuration

Product flavors are used in launch modules (`launch-aosp`, `launch-compose`, `launch-webview`, `launch-gl`, `launch-game`) and the `launch-common` library module.

| Flavor | Dimension | Purpose |
|--------|-----------|---------|
| `light` | `load` | Light workload simulation |
| `medium` | `load` | Medium workload simulation |
| `heavy` | `load` | Heavy workload simulation |

The flavor configuration is **consistent** across launch modules, with proper `applicationIdSuffix` and `resValue` for display names. `launch-common` correctly uses `buildConfigField` to expose the `LOAD_TYPE` string. `launch-game` adds a `LOAD_DURATION_MS` field.

Switch modules (`switch-aosp`, `switch-flutter`, `switch-webview`) do **not** use product flavors, which is appropriate as they handle load variation internally.

### 3.2 Signing Configurations

**Severity: HIGH -- Inconsistency**

There are **three different signing configuration patterns** across the project:

**Pattern A** (majority of modules -- 25+ modules):
```groovy
storeFile file(System.getenv("KEYSTORE_FILE_PATH") ?: "$System.env.HOME/Code/APK-Key/Chris/keystore.jks")
storePassword System.getenv("KEYSTORE_PASSWORD") ?: "123456"
keyAlias System.getenv("KEY_ALIAS") ?: "key0"
keyPassword System.getenv("KEY_PASSWORD") ?: "123456"
```

**Pattern B** (`scrolling-gl-map`, `scrolling-surface-map`):
```groovy
def keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    // reads from properties file
} else {
    storeFile file(System.getenv('KEYSTORE_FILE_PATH') ?: '../release.keystore')
    storePassword System.getenv('STORE_PASSWORD')
    keyAlias System.getenv('KEY_ALIAS')
    keyPassword System.getenv('KEY_PASSWORD')
}
```

**Pattern C** (`load-config` library module): No signing config (correct for library).

**Issues**:
1. **Hardcoded default password "123456"** in Pattern A. While overridden by env vars in CI, these defaults are visible in the source.
2. **Different env var names**: Pattern A uses `KEYSTORE_PASSWORD`, Pattern B uses `STORE_PASSWORD`. The build script and CI workflows export both, but this is fragile.
3. **Different fallback keystore paths**: Pattern A falls back to `$HOME/Code/APK-Key/Chris/keystore.jks`, Pattern B falls back to `../release.keystore`.
4. **Not centralized**: Signing config is copy-pasted into each module.

### 3.3 Java Compatibility Versions

**Severity: MEDIUM -- Inconsistency**

| Java Version | Modules |
|-------------|---------|
| `VERSION_11` | Majority (25+ modules) |
| `VERSION_17` | `switch-common` only |
| `VERSION_1_8` | `scrolling-gl-map`, `scrolling-surface-map`, `launch-game` |

All modules should align on a single Java version. Given the CI uses JDK 17, `VERSION_17` is the recommended target, or at minimum `VERSION_11` across the board.

### 3.4 minSdk Inconsistency

| minSdk | Modules |
|--------|---------|
| `21` | `app`, `load-config` |
| `24` | All other modules |

The `app` module and `load-config` library use minSdk 21, but all other modules require 24. Since `app` depends on `load-config` and no other module depends on `app`, this is not a build error, but it is inconsistent.

---

## 4. Build Scripts

### 4.1 `build_release.sh`

**File**: `build_release.sh`

A well-structured shell script that:
1. Reads keystore config from `keystore.properties` or environment variables
2. Runs `./gradlew assembleRelease --parallel`
3. Copies all APKs to `apk-released/` with version suffixes
4. Generates `install_all_apks.sh` dynamically

**Issues**:
- **[LOW] Hardcoded module list**: The `MODULE_CONFIG` array must be manually updated when modules are added/removed. Could be auto-discovered from `settings.gradle`.
- **[LOW] Missing `launch-game` in CI**: The `launch-game` module is listed in `build_release.sh` but **not** in the `android.yml` CI workflow's assembleRelease command.

### 4.2 `install_all_apks.sh`

**File**: `install_all_apks.sh`

Auto-generated by `build_release.sh`. Functions correctly. Uses `adb install -r` for replacement installs.

### 4.3 CI/CD Workflows

#### `android.yml` (Auto CI/CD)

- Triggered on push/PR to `master`
- Uses JDK 17 (Temurin), Gradle caching, wrapper validation
- **[HIGH] Massive APK copy boilerplate**: 60+ lines of individual `if [ -f ... ]; then cp ...` statements. The launch module loop is good, but all scrolling modules are handled one by one. This should use a `find` or loop pattern.
- **[MEDIUM] Missing modules**: `launch-game` is not built in CI. `switch-*` modules are built but `switch-flutter` and `switch-webview` APK copy steps are present while the build command does list them.

#### `release.yml` (Manual Release)

- Triggered via `workflow_dispatch` with version input
- Uses `find` command for APK collection (better than `android.yml`)
- **[MEDIUM] Missing modules**: Same as `android.yml` -- `launch-game` is not included in the build step.
- **[LOW] Version default mismatch**: Default version input is `v1.1.0` but current project version is `1.2.0`.

---

## 5. Detailed Findings Summary

### Critical (Should Fix)

| # | Finding | Location | Impact |
|---|---------|----------|--------|
| C1 | No centralized dependency management (Version Catalog) | All 32 build.gradle files | Version drift, maintenance burden |
| C2 | Kotlin 1.9.22 / Compose Compiler Plugin 2.0.0 mismatch | Root build.gradle | Potential build failures |
| C3 | Forced kotlin-stdlib 1.8.22 with Kotlin plugin 1.9.22 | app/build.gradle | Runtime issues |

### High Severity

| # | Finding | Location | Impact |
|---|---------|----------|--------|
| H1 | Three different signing config patterns | Module build.gradle files | Maintenance burden, CI fragility |
| H2 | Hardcoded default keystore password "123456" in source | 25+ module build.gradle files | Security (minor, overridden in CI) |
| H3 | appcompat version split (1.6.1 vs 1.7.1) | Multiple modules | Potential behavior differences |
| H4 | material version split (1.11.0 vs 1.12.0) | Multiple modules | Potential theming differences |
| H5 | Compose BOM version split (2023.08 vs 2024.02) | launch-compose vs scrolling-compose | API differences |

### Medium Severity

| # | Finding | Location | Impact |
|---|---------|----------|--------|
| M1 | Jetifier still enabled | gradle.properties | 10-15% build overhead |
| M2 | Java version inconsistency (1.8 / 11 / 17) | Various modules | Compile behavior differences |
| M3 | CI APK copy boilerplate (60+ lines) | .github/workflows/android.yml | Maintenance burden |
| M4 | `launch-game` missing from CI builds | .github/workflows/*.yml | Module not built/released in CI |
| M5 | AGP 8.2.2 (behind 8.7.x) | Root build.gradle | Missing performance/feature improvements |
| M6 | Annotation processors should use KSP | scrolling-aosp-renderstress, others | Slower Java annotation processing vs KSP |

### Low Severity

| # | Finding | Location | Impact |
|---|---------|----------|--------|
| L1 | Deprecated `rootProject.buildDir` | Root build.gradle | Gradle deprecation warning |
| L2 | `org.gradle.parallel` defined twice | gradle.properties | Cosmetic |
| L3 | Release version default is v1.1.0 | release.yml | Cosmetic |
| L4 | minSdk inconsistency (21 vs 24) | app vs others | API availability differences |
| L5 | Legacy `buildscript` + `plugins` mixed usage | Root build.gradle | Style inconsistency |

---

## 6. Modernization Recommendations

### 6.1 Migrate to Gradle Version Catalog (Priority: Critical)

Create `gradle/libs.versions.toml` to centralize all dependency versions:

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
compose-bom = "2024.12.01"
appcompat = "1.7.1"
material = "1.12.0"
constraintlayout = "2.2.1"
glide = "4.16.0"
coroutines = "1.8.1"
lifecycle = "2.8.7"
# ... etc

[libraries]
appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
material = { module = "com.google.android.material:material", version.ref = "material" }
# ... etc

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**Migration steps**:
1. Create `gradle/libs.versions.toml`
2. Add `dependencyResolutionManagement` to `settings.gradle`
3. Update each module to use `libs.xxx` references
4. Remove duplicated version strings

### 6.2 Centralize Signing Configuration (Priority: High)

Extract signing config to a shared Gradle script:

**Create `gradle/signing.gradle`**:
```groovy
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_FILE_PATH") ?: "$System.env.HOME/Code/APK-Key/Chris/keystore.jks")
            storePassword System.getenv("KEYSTORE_PASSWORD") ?: "123456"
            keyAlias System.getenv("KEY_ALIAS") ?: "key0"
            keyPassword System.getenv("KEY_PASSWORD") ?: "123456"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

Apply in each application module: `apply from: "$rootDir/gradle/signing.gradle"`

### 6.3 Create Convention Plugins (Priority: Medium)

Extract shared build configuration (compileSdk, minSdk, targetSdk, compileOptions, signing) into convention plugins or shared Gradle scripts to eliminate the massive duplication across 29 application modules.

### 6.4 Upgrade AGP and Kotlin (Priority: Medium)

- AGP: `8.2.2` -> `8.7.3` (or latest stable)
- Kotlin: `1.9.22` -> `2.0.21` (aligns with Compose compiler plugin)
- Remove the `kotlin-stdlib` force resolution in `app/build.gradle`
- Remove `android.enableJetifier=true` from `gradle.properties`

### 6.5 Simplify CI APK Collection (Priority: Medium)

Replace the 60+ line APK copy block in `android.yml` with:

```yaml
- name: Collect APK files
  run: |
    mkdir -p release-apks
    find . \( -path "*/build/outputs/apk/release/*.apk" -o -path "*/build/outputs/apk/*/release/*.apk" \) -type f | while read apk; do
      cp "$apk" "release-apks/$(basename "$apk" .apk)-${{ steps.version.outputs.BUILD_TIME }}-${{ steps.version.outputs.SHORT_SHA }}.apk"
    done
```

This is already done in `release.yml` but not in `android.yml`.

### 6.6 Unify Java/JVM Target Version (Priority: Low)

Standardize all modules to `VERSION_11` (minimum) or `VERSION_17` (recommended, matches CI JDK):

```groovy
compileOptions {
    sourceCompatibility JavaVersion.VERSION_17
    targetCompatibility JavaVersion.VERSION_17
}
kotlinOptions {
    jvmTarget = '17'
}
```

---

## 7. Build Performance Notes

The current configuration has good fundamentals:
- Parallel builds enabled (`org.gradle.parallel=true`)
- Build caching enabled (`org.gradle.caching=true`)
- Worker threads configured (`org.gradle.workers.max=8`)
- CI uses Gradle Action with cache cleanup

**Potential improvements**:
- Remove Jetifier (saves 10-15% build time)
- Migrate annotation processors to KSP (2-5x faster for Hilt, Room, Glide)
- Consider enabling configuration cache (`org.gradle.configuration-cache=true`) -- requires testing for compatibility
- Use `-bin` -> `-all` Gradle distribution for better IDE support (not a build speed improvement)
