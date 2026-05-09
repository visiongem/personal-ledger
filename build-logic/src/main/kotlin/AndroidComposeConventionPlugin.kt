import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        // Wait until either application or library plugin is applied, then configure compose
        pluginManager.withPlugin("com.android.application") { configureCompose() }
        pluginManager.withPlugin("com.android.library") { configureCompose() }
    }

    private fun Project.configureCompose() {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        extensions.configure<CommonExtension<*, *, *, *, *, *>> {
            buildFeatures.compose = true
        }

        dependencies {
            val bom = platform(libs.findLibrary("compose.bom").get())
            "implementation"(bom)
            "androidTestImplementation"(bom)
            "implementation"(libs.findLibrary("compose.foundation").get())
            "implementation"(libs.findLibrary("compose.ui").get())
            "implementation"(libs.findLibrary("compose.material3").get())
            "implementation"(libs.findLibrary("compose.material.icons.extended").get())
            "implementation"(libs.findLibrary("compose.ui.tooling.preview").get())
            "debugImplementation"(libs.findLibrary("compose.ui.tooling").get())
        }
    }
}
