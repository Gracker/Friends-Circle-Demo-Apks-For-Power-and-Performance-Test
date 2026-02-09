import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            configureTraceFix()

            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
            val versionCode = libs.findVersion("app-version-code").get().requiredVersion.toInt()
            val versionName = libs.findVersion("app-version-name").get().requiredVersion

            extensions.configure<ApplicationExtension>("android") {
                configureAndroidCommon(this)

                defaultConfig {
                    this.versionCode = versionCode
                    this.versionName = versionName
                    targetSdk = 35
                }

                signingConfigs {
                    create("release") {
                        storeFile = file(
                            System.getenv("KEYSTORE_FILE_PATH")
                                ?: "${System.getenv("HOME")}/Code/APK-Key/Chris/keystore.jks"
                        )
                        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "123456"
                        keyAlias = System.getenv("KEY_ALIAS") ?: "key0"
                        keyPassword = System.getenv("KEY_PASSWORD") ?: "123456"
                    }
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                        signingConfig = signingConfigs.getByName("release")
                    }
                }
            }
        }
    }
}
