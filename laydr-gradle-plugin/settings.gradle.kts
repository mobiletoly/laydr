pluginManagement {
    includeBuild("../build-logic/conventions")

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
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "laydr-product-plugin"

include(":laydr-codegen")
project(":laydr-codegen").projectDir = file("../laydr-codegen")
