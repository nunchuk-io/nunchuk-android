import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** META-INF excludes duplicated in every legacy module's packagingOptions block. */
internal val PACKAGING_EXCLUDES = setOf(
    "META-INF/DEPENDENCIES",
    "META-INF/LICENSE",
    "META-INF/LICENSE.txt",
    "META-INF/license.txt",
    "META-INF/NOTICE",
    "META-INF/NOTICE.txt",
    "META-INF/notice.txt",
    "META-INF/ASL2.0",
    "META-INF/versions/9/module-info.class",
)

/**
 * The set of dependencies previously injected into every library module via
 * `configs/submodule-config.gradle`. Bundles are expanded to individual libraries
 * because version-catalog bundle providers are awkward to add from plugin code.
 */
internal fun Project.addCommonModuleDependencies() {
    val catalog = libs
    fun lib(alias: String) = catalog.findLibrary(alias).get()
    dependencies {
        add("ksp", lib("hilt.compiler"))
        add("implementation", lib("hilt.android"))
        add("implementation", lib("hilt.navigation.compose"))

        // coroutines bundle
        add("implementation", lib("kotlinx.coroutines.core"))
        add("implementation", lib("kotlinx.coroutines.android"))

        add("implementation", lib("androidx.navigation.ui.ktx"))
        add("implementation", lib("androidx.navigation.fragment.ktx"))
        add("implementation", lib("timber"))
        add("implementation", lib("androidx.lifecycle.runtime.ktx"))

        add("implementation", lib("glide"))
        add("implementation", lib("glide.compose"))
        add("ksp", lib("glide.ksp"))

        add("implementation", platform(lib("androidx.compose.bom")))
        // compose bundle
        add("implementation", lib("compose.ui"))
        add("implementation", lib("compose.ui.tooling"))
        add("implementation", lib("compose.foundation"))
        add("implementation", lib("compose.material3"))
        add("implementation", lib("compose.activity"))
        add("implementation", lib("compose.fragment"))
        add("implementation", lib("compose.viewmodel"))
        add("implementation", lib("accompanist.systemuicontroller"))
        add("implementation", lib("accompanist.insets"))
        add("implementation", lib("compose.lifecycle.runtime"))
        add("implementation", lib("compose.constraintlayout"))
        add("implementation", lib("navigation.compose"))
        add("implementation", lib("compose.ui.viewbinding"))

        add("implementation", lib("kotlinx.serialization.json"))
    }
}
