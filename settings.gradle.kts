pluginManagement {
    includeBuild("build-logic/conventions")
    includeBuild("laydr-gradle-plugin") {
        name = "laydr-product-plugin"
    }

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        ivy {
            name = "Node.js Distributions"
            url = uri("https://nodejs.org/dist")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }
        ivy {
            name = "Yarn Distributions"
            url = uri("https://github.com/yarnpkg/yarn/releases/download")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.yarnpkg", "yarn")
            }
        }
    }
}

rootProject.name = "laydr"

include(
    ":laydr-core",
    ":laydr-compose",
    ":laydr-workflow",
    ":laydr-nav-runtime",
    ":laydr-nav3-kmp",
    ":laydr-nav3-androidx",
    ":laydr-nav3-kmp-adaptive",
    ":laydr-codegen",
    ":laydr-gradle-plugin",
    ":examples:compose-basic",
    ":examples:compose-basic:androidApp",
    ":examples:compose-basic:shared",
    ":examples:compose-basic:desktopApp",
    ":examples:nav3-kmp:androidApp",
    ":examples:nav3-kmp:shared",
    ":examples:nav3-kmp:desktopApp",
    ":examples:nav3-androidx",
    ":examples:nav3-kmp-shopping:androidApp",
    ":examples:nav3-kmp-shopping:shared",
    ":examples:nav3-kmp-shopping:desktopApp",
    ":examples:nav3-kmp-shopping:webApp",
)
