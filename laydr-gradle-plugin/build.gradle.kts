plugins {
    id("laydr.kotlin-jvm")
    id("laydr.publishing")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":laydr-codegen"))
    implementation(libs.kotlin.gradle.plugin)
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
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

tasks.check {
    dependsOn(functionalTest)
}
