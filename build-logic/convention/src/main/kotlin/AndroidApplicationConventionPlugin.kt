import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Common configuration for the application module. App-specific configuration
 * (signing, flavors, build types, Firebase) stays in `nunchuk-app/build.gradle.kts`.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("com.google.devtools.ksp")
            apply("org.jetbrains.kotlin.plugin.serialization")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        extensions.configure<ApplicationExtension> {
            compileSdk = 36
            defaultConfig {
                minSdk = 24
                targetSdk = 36
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            packaging {
                resources {
                    excludes += PACKAGING_EXCLUDES
                }
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain(21)
        }
    }
}
