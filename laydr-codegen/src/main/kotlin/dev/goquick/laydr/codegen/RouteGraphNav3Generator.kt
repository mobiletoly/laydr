// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName

internal fun RouteGraphGenerator.generateNavRoutes(
    routeTree: RouteDirectoryTree,
    packageName: String,
    target: LaydrNav3HelperTarget,
): String {
    val symbols = target.symbols
    val navRootObject = TypeSpec.objectBuilder(navRootObjectName)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("Generated %L helpers for the Laydr route tree.\n", target.displayName)
        .addFunction(navRememberSectionsFunction(symbols))
        .addFunction(navRememberStackInitialDestinationFunction(symbols))
        .addFunction(navRememberStackBackStackFunction(symbols))

    navRootObject.addNavRouteObjects(
        routes = routeTree.routes,
        packageName = packageName,
        target = target,
        symbols = symbols,
    )

    val file = FileSpec.builder(packageName, navRootObjectName)
        .addType(navRootObject.build())
    return file.build().toString()
}

private fun navRememberSectionsFunction(symbols: Nav3HelperSymbols): FunSpec {
    val dataType = TypeVariableName("Data", anyClass)
    val sectionSpecType = symbols.laydrNavSectionSpecClass.parameterizedBy(dataType)
    val sectionType = symbols.laydrNavSectionClass.parameterizedBy(dataType)
    val entryMetadataType = LambdaTypeName.get(
        parameters = listOf(
            ParameterSpec.builder("context", symbols.laydrNavSectionEntryContextClass.parameterizedBy(dataType))
                .build(),
        ),
        returnType = mapOfStringAnyClass,
    )
    val notFoundContentType = LambdaTypeName.get(
        parameters = listOf(
            ParameterSpec.builder("notFound", symbols.laydrNavNotFoundClass).build(),
        ),
        returnType = unitClass,
    ).copy(annotations = listOf(composableAnnotation))

    val function = FunSpec.builder("rememberSections")
        .addModifiers(KModifier.PUBLIC)
        .addAnnotation(composableAnnotation)
        .addTypeVariable(dataType)
        .addKdoc("Remembers %L sections for this generated route graph.\n", symbols.displayName)
        .addParameter("sectionSpecs", listOfSectionSpecsClass(sectionSpecType))
        .addParameter(
            ParameterSpec.builder("sceneSupport", symbols.laydrNavSceneSupportClass)
                .defaultValue("%T.%L", symbols.laydrNavSceneSupportClass, "None")
                .build(),
        )
        .addParameter(
            ParameterSpec.builder("entryMetadata", entryMetadataType)
                .defaultValue("{ %M() }", emptyMapMember)
                .build(),
        )
        .addParameter(
            ParameterSpec.builder("initialSection", sectionType.copy(nullable = true))
                .defaultValue("null")
                .build(),
        )
        .addParameter("notFoundContent", notFoundContentType)
        .returns(symbols.laydrNavSectionsClass.parameterizedBy(dataType))

    if (symbols.savedStateConfigurationMember != null) {
        function.addParameter(
            ParameterSpec.builder("savedStateConfiguration", savedStateConfigurationClass)
                .defaultValue("%M()", symbols.savedStateConfigurationMember)
                .build(),
        )
    }

    val savedStateArgument = if (symbols.savedStateConfigurationMember == null) {
        ""
    } else {
        "savedStateConfiguration = savedStateConfiguration, "
    }

    return function
        .addStatement(
            "return %M(" +
                "routeDefinitions = %L.%L, " +
                "sectionSpecs = sectionSpecs, " +
                "sceneSupport = sceneSupport, " +
                "entryMetadata = entryMetadata, " +
                "initialSection = initialSection, " +
                savedStateArgument +
                "notFoundContent = notFoundContent" +
                ")",
            symbols.rememberLaydrNavSectionsMember,
            composeRootObjectName,
            definitionsPropertyName,
        )
        .build()
}

private fun navRememberStackInitialDestinationFunction(symbols: Nav3HelperSymbols): FunSpec {
    val notFoundContentType = LambdaTypeName.get(
        parameters = listOf(
            ParameterSpec.builder("notFound", symbols.laydrNavNotFoundClass).build(),
        ),
        returnType = unitClass,
    ).copy(annotations = listOf(composableAnnotation))

    val function = FunSpec.builder("rememberStack")
        .addModifiers(KModifier.PUBLIC)
        .addAnnotation(composableAnnotation)
        .addKdoc("Remembers a Laydr-managed %L stack for generated routes.\n", symbols.displayName)
        .addParameter("initialDestination", laydrScreenDestinationClass)
        .addParameter(
            ParameterSpec.builder("sceneSupport", symbols.laydrNavSceneSupportClass)
                .defaultValue("%T.%L", symbols.laydrNavSceneSupportClass, "None")
                .build(),
        )
        .addParameter(
            ParameterSpec.builder("entryMetadata", symbols.laydrNavEntryMetadataProviderClass)
                .defaultValue(
                    "%T { %T.%L }",
                    symbols.laydrNavEntryMetadataProviderClass,
                    symbols.laydrNavEntryMetadataClass,
                    "Empty",
                )
                .build(),
        )
        .addParameter("notFoundContent", notFoundContentType)
        .returns(symbols.laydrNavStackClass)

    if (symbols.savedStateConfigurationMember != null) {
        function.addParameter(
            ParameterSpec.builder("savedStateConfiguration", savedStateConfigurationClass)
                .defaultValue("%M()", symbols.savedStateConfigurationMember)
                .build(),
        )
    }

    val savedStateArgument = if (symbols.savedStateConfigurationMember == null) {
        ""
    } else {
        "savedStateConfiguration = savedStateConfiguration, "
    }

    return function
        .addStatement(
            "return %M(" +
                "routeDefinitions = %L.%L, " +
                "initialDestination = initialDestination, " +
                "sceneSupport = sceneSupport, " +
                "entryMetadata = entryMetadata, " +
                savedStateArgument +
                "notFoundContent = notFoundContent" +
                ")",
            symbols.rememberLaydrNavStackMember,
            composeRootObjectName,
            definitionsPropertyName,
        )
        .build()
}

private fun navRememberStackBackStackFunction(symbols: Nav3HelperSymbols): FunSpec {
    val notFoundContentType = LambdaTypeName.get(
        parameters = listOf(
            ParameterSpec.builder("notFound", symbols.laydrNavNotFoundClass).build(),
        ),
        returnType = unitClass,
    ).copy(annotations = listOf(composableAnnotation))
    val backStackType = symbols.backStackClass.parameterizedBy(navKeyClass)

    return FunSpec.builder("rememberStack")
        .addModifiers(KModifier.PUBLIC)
        .addAnnotation(composableAnnotation)
        .addKdoc("Remembers Laydr management for an app-owned %L stack.\n", symbols.displayName)
        .addParameter("backStack", backStackType)
        .addParameter(
            ParameterSpec.builder("sceneSupport", symbols.laydrNavSceneSupportClass)
                .defaultValue("%T.%L", symbols.laydrNavSceneSupportClass, "None")
                .build(),
        )
        .addParameter(
            ParameterSpec.builder("entryMetadata", symbols.laydrNavEntryMetadataProviderClass)
                .defaultValue(
                    "%T { %T.%L }",
                    symbols.laydrNavEntryMetadataProviderClass,
                    symbols.laydrNavEntryMetadataClass,
                    "Empty",
                )
                .build(),
        )
        .addParameter("notFoundContent", notFoundContentType)
        .returns(symbols.laydrNavStackClass)
        .addStatement(
            "return %M(" +
                "routeDefinitions = %L.%L, " +
                "backStack = backStack, " +
                "sceneSupport = sceneSupport, " +
                "entryMetadata = entryMetadata, " +
                "notFoundContent = notFoundContent" +
                ")",
            symbols.rememberLaydrNavStackMember,
            composeRootObjectName,
            definitionsPropertyName,
        )
        .build()
}

private fun TypeSpec.Builder.addNavRouteObjects(
    routes: List<RouteDirectoryRoute>,
    packageName: String,
    target: LaydrNav3HelperTarget,
    symbols: Nav3HelperSymbols,
) {
    routes
        .map { route -> route.toNavRouteObject(packageName, target, symbols) }
        .forEach(::addType)
}

private fun RouteDirectoryRoute.toNavRouteObject(
    packageName: String,
    target: LaydrNav3HelperTarget,
    symbols: Nav3HelperSymbols,
): TypeSpec {
    val navRouteObject = TypeSpec.objectBuilder(objectName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc(
            if (isDeclaredRoute) {
                "Generated %L helpers for `%L`.\n"
            } else {
                "Generated %L namespace for route segment `%L`.\n"
            },
            target.displayName,
            routePath,
        )

    if (isScreenRoute) {
        navRouteObject.addFunction(navSectionFunction(packageName, symbols))
    }

    navRouteObject.addNavRouteObjects(
        routes = children,
        packageName = packageName,
        target = target,
        symbols = symbols,
    )

    return navRouteObject.build()
}

private fun RouteDirectoryRoute.navSectionFunction(
    packageName: String,
    symbols: Nav3HelperSymbols,
): FunSpec {
    val dataType = TypeVariableName("Data", anyClass)
    val dynamicParameters = dynamicParametersForGeneratedFunctions(destinationTypeName())
    val function = FunSpec.builder("section")
        .addModifiers(KModifier.PUBLIC)
        .addTypeVariable(dataType)
        .addKdoc("Creates a %L section spec for this generated route.\n", symbols.displayName)

    if (dynamicParameters.isNotEmpty()) {
        function.addParameter("rootDestination", destinationClass(packageName))
    }
    function.addParameter("sectionData", dataType)
    function.returns(symbols.laydrNavSectionSpecClass.parameterizedBy(dataType))

    if (dynamicParameters.isEmpty()) {
        function.addStatement(
            "return %M(%T, sectionData)",
            symbols.laydrNavSectionMember,
            routeObjectClass(packageName),
        )
    } else {
        function.addStatement(
            "return %M(%T, rootDestination, sectionData)",
            symbols.laydrNavSectionMember,
            routeObjectClass(packageName),
        )
    }

    return function.build()
}
