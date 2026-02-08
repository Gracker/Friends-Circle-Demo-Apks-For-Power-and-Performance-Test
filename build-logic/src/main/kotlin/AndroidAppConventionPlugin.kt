import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension>("android") {
                configureAndroidCommon(this)

                defaultConfig {
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
