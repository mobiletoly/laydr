plugins {
    id("laydr.kotlin-jvm")
    id("laydr.publishing")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":laydr-codegen"))
    compileOnly(libs.kotlin.gradle.plugin)
    testRuntimeOnly(libs.kotlin.gradle.plugin)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("laydr") {
            id = "dev.goquick.laydr"
            displayName = "Laydr Gradle Plugin"
            description = "Gradle plugin for Laydr route generation and route validation."
            implementationClass = "dev.goquick.laydr.gradle.LaydrPlugin"
        }
    }
}

val functionalTestSourceSet = sourceSets.create("functionalTest")
val functionalTestKotlinPlugin by configurations.creating
dependencies {
    add(functionalTestKotlinPlugin.name, libs.kotlin.gradle.plugin.compatibility)
}
// TestKit injects an isolated plugin classloader. Supply its host API for the
// generator tests; published-artifact compatibility is checked separately.
tasks.named<org.gradle.plugin.devel.tasks.PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(functionalTestKotlinPlugin)
}

configurations[functionalTestSourceSet.implementationConfigurationName].extendsFrom(
    configurations.testImplementation.get(),
)
configurations[functionalTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(
    configurations.testRuntimeOnly.get(),
)

val functionalTest by tasks.registering(Test::class) {
    description = "Runs functional tests for the Laydr Gradle plugin."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    systemProperty("laydr.test.kotlinVersion", libs.versions.compatibilityKotlin.get())
    systemProperty("laydr.test.agpVersion", libs.versions.functionalTestAgp.get())
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

tasks.check {
    dependsOn(functionalTest)
}
