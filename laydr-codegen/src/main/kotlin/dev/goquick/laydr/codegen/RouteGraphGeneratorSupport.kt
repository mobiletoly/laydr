// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

internal val laydrRouteClass = ClassName("dev.goquick.laydr.core", "LaydrRoute")
internal val laydrAppGraphClass = ClassName("dev.goquick.laydr.core", "LaydrAppGraph")
internal val laydrRouteKeyClass = ClassName("dev.goquick.laydr.core", "LaydrRouteKey")
internal val laydrRouteMapClass = ClassName("dev.goquick.laydr.core", "LaydrRouteMap")
internal val laydrRouteMetadataClass = ClassName("dev.goquick.laydr.core", "LaydrRouteMetadata")
internal val laydrRouteMatchClass = ClassName("dev.goquick.laydr.core", "LaydrRouteMatch")
internal val laydrRouteSegmentClass = ClassName("dev.goquick.laydr.core", "LaydrRouteSegment")
internal val laydrScreenDestinationClass = ClassName("dev.goquick.laydr.core", "LaydrScreenDestination")
internal val laydrParameterlessScreenRouteRefClass =
    ClassName("dev.goquick.laydr.core", "LaydrParameterlessScreenRouteRef")
internal val laydrScreenRouteRefClass = ClassName("dev.goquick.laydr.core", "LaydrScreenRouteRef")
internal val laydrLayoutRouteRefClass = ClassName("dev.goquick.laydr.core", "LaydrLayoutRouteRef")
internal val laydrLayoutContextClass = ClassName("dev.goquick.laydr.compose", "LaydrLayoutContext")
internal val laydrScreenContentClass = ClassName("dev.goquick.laydr.compose", "LaydrScreenContent")
internal val laydrComposeRouteDefinitionsClass =
    ClassName("dev.goquick.laydr.compose", "LaydrComposeRouteDefinitions")
internal val laydrComposeScreenRouteDefinitionClass =
    ClassName("dev.goquick.laydr.compose", "LaydrComposeScreenRouteDefinition")
internal val laydrComposeLayoutRouteDefinitionClass =
    ClassName("dev.goquick.laydr.compose", "LaydrComposeLayoutRouteDefinition")
internal val laydrComposeScreenAndLayoutRouteDefinitionClass =
    ClassName("dev.goquick.laydr.compose", "LaydrComposeScreenAndLayoutRouteDefinition")
private val navBackStackClass = ClassName("androidx.navigation3.runtime", "NavBackStack")
internal val navKeyClass = ClassName("androidx.navigation3.runtime", "NavKey")
internal val savedStateConfigurationClass =
    ClassName("androidx.savedstate.serialization", "SavedStateConfiguration")
internal val jvmInlineClass = ClassName("kotlin.jvm", "JvmInline")
internal val composableAnnotation = AnnotationSpec.builder(
    ClassName("androidx.compose.runtime", "Composable"),
).build()
internal val unitClass = ClassName("kotlin", "Unit")
internal val anyClass = ClassName("kotlin", "Any")
internal val stringClass = ClassName("kotlin", "String")
internal val mapOfStringStringClass = ClassName("kotlin.collections", "Map")
    .parameterizedBy(stringClass, stringClass)
internal val mapOfStringAnyClass = ClassName("kotlin.collections", "Map")
    .parameterizedBy(stringClass, anyClass)
internal val listOfRoutesClass = ClassName("kotlin.collections", "List").parameterizedBy(laydrRouteClass)
private val listOfMember = MemberName("kotlin.collections", "listOf")
private val mapOfMember = MemberName("kotlin.collections", "mapOf")
internal val emptyMapMember = MemberName("kotlin.collections", "emptyMap")
private val kmpNav3HelperSymbols = nav3HelperSymbols(
    displayName = "Nav3 KMP",
    packageName = "dev.goquick.laydr.nav3.kmp",
    savedStateConfigurationMember = MemberName(
        "dev.goquick.laydr.nav3.kmp",
        "laydrNavSavedStateConfiguration",
    ),
)
private val androidxNav3HelperSymbols = nav3HelperSymbols(
    displayName = "AndroidX Nav3",
    packageName = "dev.goquick.laydr.nav3.androidx",
    savedStateConfigurationMember = null,
)

internal const val rootObjectName = "LaydrRoutes"
internal const val composeRootObjectName = "LaydrComposeRoutes"
internal const val navRootObjectName = "LaydrNavRoutes"
internal const val rootRoutesPropertyName = "routes"
internal const val screenRoutesPropertyName = "screenRoutes"
internal const val layoutRoutesPropertyName = "layoutRoutes"
internal const val routeMapPropertyName = "routeMap"
internal const val appGraphPropertyName = "appGraph"
internal const val definitionsPropertyName = "definitions"
internal const val routePropertyName = "route"
internal const val pathFunctionName = "path"
internal const val destinationFunctionName = "destination"
internal const val defaultDestinationPropertyName = "defaultDestination"
internal const val keyFunctionName = "key"
internal const val requireDestinationFunctionName = "requireDestination"
private const val defaultDestinationTypeName = "Destination"
private const val fallbackDestinationTypeName = "RouteDestination"
internal const val routeLocalHelperObjectName = "LaydrRouteDef"
internal const val screenAndLayoutBuilderTypeName = "LaydrScreenAndLayoutRouteDefBuilder"
internal const val screenFunctionName = "screen"
internal const val screenWithLayoutValuesFunctionName = "screenWithLayoutValues"
internal const val layoutFunctionName = "layout"
internal const val screenAndLayoutFunctionName = "screenAndLayout"
internal const val buildFunctionName = "build"
internal const val contentParameterName = "content"
internal const val blockParameterName = "block"
internal const val metadataNameParameterName = "name"
internal const val metadataLabelsParameterName = "labels"
internal const val validateMetadataFunctionName = "validateMetadata"
internal const val screenDefinitionPropertyName = "screenDefinition"
internal const val layoutDefinitionPropertyName = "layoutDefinition"
internal const val routeMatchPropertyName = "routeMatch"
internal const val routeKeyPropertyName = "routeKey"
internal const val pathPropertyName = "path"
internal const val buildPathFunctionName = "buildPath"
internal val lowercasePackageNameRegex = Regex("[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)*")

internal data class Nav3HelperSymbols(
    val displayName: String,
    val laydrNavSectionClass: ClassName,
    val laydrNavSectionsClass: ClassName,
    val laydrNavSectionSpecClass: ClassName,
    val laydrNavStackClass: ClassName,
    val laydrNavEntryMetadataClass: ClassName,
    val laydrNavEntryMetadataProviderClass: ClassName,
    val laydrNavSceneSupportClass: ClassName,
    val laydrNavNotFoundClass: ClassName,
    val laydrNavSectionEntryContextClass: ClassName,
    val backStackClass: ClassName,
    val laydrNavSectionMember: MemberName,
    val rememberLaydrNavSectionsMember: MemberName,
    val rememberLaydrNavStackMember: MemberName,
    val savedStateConfigurationMember: MemberName?,
)

internal val LaydrNav3HelperTarget.displayName: String
    get() = when (this) {
        LaydrNav3HelperTarget.Kmp -> "Nav3 KMP"
        LaydrNav3HelperTarget.Androidx -> "AndroidX Nav3"
    }

internal val LaydrNav3HelperTarget.symbols: Nav3HelperSymbols
    get() = when (this) {
        LaydrNav3HelperTarget.Kmp -> kmpNav3HelperSymbols
        LaydrNav3HelperTarget.Androidx -> androidxNav3HelperSymbols
    }

private fun nav3HelperSymbols(
    displayName: String,
    packageName: String,
    savedStateConfigurationMember: MemberName?,
): Nav3HelperSymbols =
    Nav3HelperSymbols(
        displayName = displayName,
        laydrNavSectionClass = ClassName(packageName, "LaydrNavSection"),
        laydrNavSectionsClass = ClassName(packageName, "LaydrNavSections"),
        laydrNavSectionSpecClass = ClassName(packageName, "LaydrNavSectionSpec"),
        laydrNavStackClass = ClassName(packageName, "LaydrNavStack"),
        laydrNavEntryMetadataClass = ClassName(packageName, "LaydrNavEntryMetadata"),
        laydrNavEntryMetadataProviderClass = ClassName(packageName, "LaydrNavEntryMetadataProvider"),
        laydrNavSceneSupportClass = ClassName(packageName, "LaydrNavSceneSupport"),
        laydrNavNotFoundClass = ClassName(packageName, "LaydrNavNotFound"),
        laydrNavSectionEntryContextClass = ClassName(packageName, "LaydrNavSectionEntryContext"),
        backStackClass = navBackStackClass,
        laydrNavSectionMember = MemberName(packageName, "laydrNavSection"),
        rememberLaydrNavSectionsMember = MemberName(packageName, "rememberLaydrNavSections"),
        rememberLaydrNavStackMember = MemberName(packageName, "rememberLaydrNavStack"),
        savedStateConfigurationMember = savedStateConfigurationMember,
    )

internal data class GeneratedDynamicParameter(
    val segment: RouteDirectorySegment.Dynamic,
    val typeName: String,
) {
    val kotlinParameterName: String = segment.kotlinParameterName
}

internal fun RouteDirectoryRoute.dynamicSegmentsForGeneratedFunctions(): List<RouteDirectorySegment.Dynamic> {
    val dynamicSegments = segments.filterIsInstance<RouteDirectorySegment.Dynamic>()
    val duplicateParameterName = dynamicSegments
        .groupingBy { it.parameterName }
        .eachCount()
        .firstNotNullOfOrNull { (parameterName, count) ->
            parameterName.takeIf { count > 1 }
        }
    if (duplicateParameterName != null) {
        throw RouteGraphGeneratorException(
            "Duplicate generated route path parameter $duplicateParameterName in $routePath",
        )
    }
    val duplicateKotlinParameterName = dynamicSegments
        .groupingBy { it.kotlinParameterName }
        .eachCount()
        .firstNotNullOfOrNull { (parameterName, count) ->
            parameterName.takeIf { count > 1 }
        }
    if (duplicateKotlinParameterName != null) {
        throw RouteGraphGeneratorException(
            "Duplicate generated Kotlin route parameter $duplicateKotlinParameterName in $routePath",
        )
    }

    return dynamicSegments
}

internal fun RouteDirectoryRoute.dynamicParametersForGeneratedFunctions(
    destinationTypeName: String,
): List<GeneratedDynamicParameter> {
    val unavailableTypeNames = children.map { child -> child.objectName }.toSet() + destinationTypeName
    val usedTypeNames = mutableSetOf<String>()
    return dynamicSegmentsForGeneratedFunctions().map { segment ->
        val baseTypeName = "${pascalCase(segment.parameterName)}Param"
        var typeName = baseTypeName
        var suffix = 1
        while (typeName in unavailableTypeNames || typeName in usedTypeNames) {
            typeName = "${baseTypeName}Value$suffix"
            suffix += 1
        }
        usedTypeNames += typeName
        GeneratedDynamicParameter(segment = segment, typeName = typeName)
    }
}

internal val RouteDirectoryRoute.objectName: String
    get() = pascalCase(segments.last().sourceName)

internal fun RouteDirectoryRoute.objectPath(parentObjectPath: String?): String =
    if (parentObjectPath == null) {
        objectName
    } else {
        "$parentObjectPath.$objectName"
    }

internal fun List<RouteDirectoryRoute>.flattenRoutes(): List<RouteDirectoryRoute> =
    flatMap { route ->
        listOf(route) + route.children.flattenRoutes()
    }

internal fun List<RouteDirectoryRoute>.flattenDeclaredRoutes(): List<RouteDirectoryRoute> =
    flattenRoutes().filter { route -> route.isDeclaredRoute }

internal fun List<RouteDirectoryRoute>.nearestDeclaredRoutes(): List<RouteDirectoryRoute> =
    flatMap { route ->
        if (route.isDeclaredRoute) {
            listOf(route)
        } else {
            route.children.nearestDeclaredRoutes()
        }
    }

internal fun RouteDirectoryRoute.objectReference(): String =
    segments.joinToString(separator = ".") { segment ->
        pascalCase(segment.sourceName)
    }

internal fun RouteDirectoryRoute.objectReferenceFrom(parent: RouteDirectoryRoute): String =
    segments
        .drop(parent.segments.size)
        .joinToString(separator = ".") { segment ->
            pascalCase(segment.sourceName)
        }

internal fun RouteDirectoryRoute.destinationTypeName(): String {
    val childObjectNames = children.map { child -> child.objectName }.toSet()
    if (defaultDestinationTypeName !in childObjectNames) {
        return defaultDestinationTypeName
    }

    var suffix = 1
    var candidate = fallbackDestinationTypeName
    while (candidate in childObjectNames) {
        suffix += 1
        candidate = "$fallbackDestinationTypeName$suffix"
    }
    return candidate
}

internal fun RouteDirectoryRoute.routeValueReference(): String =
    "${requiredRoutePackageName()}.Route"

internal fun RouteDirectoryRoute.requiredRoutePackageName(): String =
    routePackageName ?: throw RouteGraphGeneratorException(
        "Route.kt package declaration is required for Compose generation: $routeFile",
    )

internal fun RouteDirectoryRoute.routeDescriptorReference(packageName: String): CodeBlock =
    CodeBlock.of("%T.%L", routeObjectClass(packageName), routePropertyName)

internal fun RouteDirectoryRoute.routeObjectClass(packageName: String): ClassName =
    ClassName(packageName, rootObjectName, *objectReference().split(".").toTypedArray())

internal fun RouteDirectoryRoute.destinationClass(packageName: String): ClassName =
    ClassName(packageName, rootObjectName, *objectReference().split(".").toTypedArray(), destinationTypeName())

internal fun screenBodyLambdaType(destinationClass: ClassName): TypeName =
    LambdaTypeName.get(
        parameters = listOf(
            ParameterSpec.builder("route", destinationClass).build(),
        ),
        returnType = unitClass,
    ).copy(annotations = listOf(composableAnnotation))

internal fun screenContentLambdaType(destinationClass: ClassName): TypeName =
    LambdaTypeName.get(
        parameters = listOf(
            ParameterSpec.builder("route", destinationClass).build(),
        ),
        returnType = laydrScreenContentClass,
    ).copy(annotations = listOf(composableAnnotation))

internal fun layoutContentLambdaType(): TypeName =
    LambdaTypeName.get(
        parameters = listOf(
            ParameterSpec.builder("context", laydrLayoutContextClass).build(),
            ParameterSpec.builder(
                "content",
                LambdaTypeName.get(returnType = unitClass)
                    .copy(annotations = listOf(composableAnnotation)),
            ).build(),
        ),
        returnType = unitClass,
    ).copy(annotations = listOf(composableAnnotation))

internal fun screenAndLayoutBuilderLambdaType(): LambdaTypeName =
    LambdaTypeName.get(
        receiver = ClassName("", screenAndLayoutBuilderTypeName),
        returnType = unitClass,
    )

internal fun listOfSectionSpecsClass(sectionSpecType: TypeName): TypeName =
    ClassName("kotlin.collections", "List").parameterizedBy(sectionSpecType)

internal fun generatedSourcePath(packageName: String, fileName: String): String =
    packageName.replace(".", "/") + "/$fileName.kt"

internal fun pascalCase(sourceName: String): String =
    sourceName
        .split("_")
        .joinToString(separator = "") { word ->
            word.replaceFirstChar { char -> char.uppercaseChar() }
        }

internal fun validatePackageName(packageName: String) {
    if (!lowercasePackageNameRegex.matches(packageName)) {
        throw RouteGraphGeneratorException(
            "Generated package name must be dot-separated lowercase Kotlin identifiers: $packageName",
        )
    }
}

internal fun listOfCode(items: List<CodeBlock>): CodeBlock =
    collectionCode(listOfMember, items)

internal fun mapOfCode(items: List<CodeBlock>): CodeBlock =
    collectionCode(mapOfMember, items)

internal fun namedArgumentsCode(items: List<CodeBlock>): CodeBlock {
    val builder = CodeBlock.builder()
    items.forEachIndexed { index, item ->
        if (index > 0) {
            builder.add(", ")
        }
        builder.add("%L", item)
    }
    return builder.build()
}

private fun collectionCode(
    memberName: MemberName,
    items: List<CodeBlock>,
): CodeBlock {
    if (items.isEmpty()) {
        return CodeBlock.of("%M()", memberName)
    }

    val builder = CodeBlock.builder()
    builder.add("%M(", memberName)
    items.forEachIndexed { index, item ->
        if (index > 0) {
            builder.add(", ")
        }
        builder.add("%L", item)
    }
    builder.add(")")
    return builder.build()
}
