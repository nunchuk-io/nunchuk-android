import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Adds the Nunchuk native SDK: the debug build from the catalog, and the release
 * prebuild AAR. The prebuild uses the `@aar` artifact suffix (artifact-only,
 * non-transitive), which a version catalog cannot express — so its version is
 * centralized in the catalog (`prebuildNativeSdk`) and the coordinate is built here.
 *
 * Applied (via `id("nunchuk.android.nativesdk")`) only by modules that need the
 * native SDK; applied alongside the library/application plugin, so the debug/release
 * configurations already exist.
 */
class NativeSdkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val prebuildVersion = libs.findVersion("prebuildNativeSdk").get().requiredVersion
        dependencies {
            add("debugImplementation", libs.findLibrary("nunchuk-nativesdk").get())
            add(
                "releaseImplementation",
                "com.github.nunchuk-io:nunchuk-android-nativesdk-prebuild:$prebuildVersion@aar",
            )
        }
    }
}
