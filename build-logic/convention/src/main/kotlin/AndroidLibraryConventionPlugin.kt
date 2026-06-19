import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Replaces `configs/submodule-config.gradle`: applies the common library plugins,
 * configures the shared Android library options, and adds the common dependencies.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("com.google.devtools.ksp")
            apply("org.jetbrains.kotlin.plugin.serialization")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        extensions.configure<LibraryExtension> {
            compileSdk = 36
            defaultConfig {
                minSdk = 24
                javaCompileOptions {
                    annotationProcessorOptions {
                        arguments["library"] = "true"
                    }
                }
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            buildFeatures {
                viewBinding = true
                compose = true
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

        addCommonModuleDependencies()
    }
}
