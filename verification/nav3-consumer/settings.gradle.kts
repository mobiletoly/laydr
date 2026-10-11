pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { maven { url = uri(providers.gradleProperty("consumerRepository").get()) } }
            filter { includeGroup("dev.goquick.laydr") }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    versionCatalogs {
        create("libs") {
            from(files("gradle/consumer.versions.toml"))
            version("kotlin", providers.gradleProperty("consumerKotlinVersion").get())
            version("compose", providers.gradleProperty("consumerComposeVersion").get())
            version("nav3", providers.gradleProperty("consumerNav3Version").get())
            version("laydr", providers.gradleProperty("consumerLaydrVersion").get())
            version("agp", providers.gradleProperty("consumerAgpVersion").get())
            version("activity", providers.gradleProperty("consumerActivityVersion").get())
        }
    }
    repositories {
        exclusiveContent {
            forRepository { maven { url = uri(providers.gradleProperty("consumerRepository").get()) } }
            filter { includeGroup("dev.goquick.laydr") }
        }
        google()
        mavenCentral()
        ivy {
            name = "Binaryen Distributions"
            url = uri("https://github.com/WebAssembly/binaryen/releases/download")
            patternLayout { artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
        ivy {
            name = "Node.js Distributions"
            url = uri("https://nodejs.org/dist")
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy {
            name = "Yarn Distributions"
            url = uri("https://github.com/yarnpkg/yarn/releases/download")
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
    }
}

rootProject.name = "laydr-nav3-artifact-consumer"
if (!providers.gradleProperty("consumerAndroidOnly").orElse("false").get().toBoolean()) {
    include(":kmp")
}
include(":androidApp")
