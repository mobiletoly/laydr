plugins {
    id("laydr.kotlin-multiplatform")
    id("laydr.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":laydr-core"))
        }
    }
}

val checkForbiddenNavRuntimeDependencies = tasks.register("checkForbiddenNavRuntimeDependencies") {
    group = "verification"
    description = "Checks that laydr-nav-runtime stays adapter-neutral."
    notCompatibleWithConfigurationCache("Inspects resolved configurations at execution time.")

    doLast {
        val forbiddenGroups = setOf(
            "androidx.navigation3",
            "org.jetbrains.androidx.navigation3",
            "androidx.compose.material3.adaptive",
            "org.jetbrains.compose.material3.adaptive",
            "com.android.tools.build",
        )
        val forbiddenNames = setOf(
            "laydr-compose",
            "laydr-codegen",
            "laydr-gradle-plugin",
            "laydr-nav3-kmp",
            "laydr-nav3-kmp-adaptive",
        )
        val offenders = configurations
            .filter { configuration -> configuration.isCanBeResolved }
            .flatMap { configuration ->
                configuration.incoming.resolutionResult.allComponents.mapNotNull { component ->
                    val id = component.moduleVersion ?: return@mapNotNull null
                    val forbidden = id.group in forbiddenGroups || id.name in forbiddenNames
                    if (forbidden) {
                        "${configuration.name}: ${id.group}:${id.name}:${id.version}"
                    } else {
                        null
                    }
                }
            }
            .distinct()
            .sorted()
        check(offenders.isEmpty()) {
            "laydr-nav-runtime has forbidden dependencies:\n${offenders.joinToString("\n")}"
        }
    }
}
