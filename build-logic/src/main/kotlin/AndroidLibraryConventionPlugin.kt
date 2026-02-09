import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            configureTraceFix()

            extensions.configure<LibraryExtension>("android") {
                configureAndroidCommon(this)
            }
        }
    }
}
