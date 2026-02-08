import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Enables Compose for any Android module (application or library).
 *
 * Applies the Kotlin Compose compiler plugin and sets `buildFeatures.compose = true`.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<CommonExtension<*, *, *, *, *, *>>("android") {
                buildFeatures {
                    compose = true
                }
            }
        }
    }
}
