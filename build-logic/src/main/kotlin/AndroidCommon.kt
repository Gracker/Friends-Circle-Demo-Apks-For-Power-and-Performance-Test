import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Shared Android configuration applied by both app and library convention plugins.
 *
 * Centralizes compileSdk, Java compatibility, and Kotlin JVM target settings
 * so they remain consistent across all modules.
 */
internal fun Project.configureAndroidCommon(
    extension: CommonExtension<*, *, *, *, *, *>,
) {
    extension.apply {
        compileSdk = 35

        defaultConfig {
            minSdk = 24
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    configureKotlin()
}

/**
 * Configures Kotlin JVM target when the Kotlin Android plugin is present.
 *
 * Uses [pluginManager.withPlugin] to defer configuration until the Kotlin plugin
 * is actually applied, making this safe to call unconditionally.
 */
private fun Project.configureKotlin() {
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        extensions.configure<KotlinAndroidProjectExtension>("kotlin") {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }
}
