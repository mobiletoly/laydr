// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun RouteGraphGenerator.generateComposeRouteDefinitions(
    routeTree: RouteDirectoryTree,
    packageName: String,
): String {
    val routes = routeTree.routes.flattenDeclaredRoutes()
    val screenRoutes = routes.filter { route -> route.isScreenRoute }
    val layoutRoutes = routes.filter { route -> route.isLayoutRoute }
    val composeRootObject = TypeSpec.objectBuilder(composeRootObjectName)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("Generated Compose route definitions for the Laydr route tree.\n")
        .addProperty(
            PropertySpec.builder(
                definitionsPropertyName,
                laydrComposeRouteDefinitionsClass,
            )
                .addModifiers(KModifier.PUBLIC)
                .addKdoc("Route-owned Compose definitions consumed by Laydr runtime hosts.\n")
                .initializer(
                    CodeBlock.builder()
                        .add("%T(\n", laydrComposeRouteDefinitionsClass)
                        .indent()
                        .add("routeMap = %L.%L,\n", rootObjectName, routeMapPropertyName)
                        .add("screenDefinitions = %L,\n", screenDefinitionsInitializer(screenRoutes))
                        .add("layoutDefinitions = %L,\n", layoutDefinitionsInitializer(layoutRoutes))
                        .unindent()
                        .add(")")
                        .build(),
                )
                .build(),
        )
        .build()

    return FileSpec.builder(packageName, composeRootObjectName)
        .addType(composeRootObject)
        .build()
        .toString()
}

private fun screenDefinitionsInitializer(routes: List<RouteDirectoryRoute>): CodeBlock =
    listOfCode(
        routes.map { route ->
            when {
                route.isScreenRoute && route.isLayoutRoute ->
                    CodeBlock.of("%L.screenDefinition", route.routeValueReference())
                else -> CodeBlock.of("%L", route.routeValueReference())
            }
        },
    )

private fun layoutDefinitionsInitializer(routes: List<RouteDirectoryRoute>): CodeBlock =
    listOfCode(
        routes.map { route ->
            when {
                route.isScreenRoute && route.isLayoutRoute ->
                    CodeBlock.of("%L.layoutDefinition", route.routeValueReference())
                else -> CodeBlock.of("%L", route.routeValueReference())
            }
        },
    )

internal fun RouteGraphGenerator.generateRouteLocalHelper(
    route: RouteDirectoryRoute,
    packageName: String,
): String {
    val helper = TypeSpec.objectBuilder(routeLocalHelperObjectName)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("Route-local Compose declaration helpers for this Laydr route.\n")

    if (route.isScreenRoute) {
        helper.addFunction(route.localScreenFunction(packageName))
        helper.addFunction(route.localScreenWithLayoutValuesFunction(packageName))
    }
    if (route.isLayoutRoute) {
        helper.addFunction(route.localLayoutFunction(packageName))
    }
    if (route.isScreenRoute && route.isLayoutRoute) {
        helper.addType(route.localScreenAndLayoutBuilder(packageName))
        helper.addFunction(localScreenAndLayoutFunction())
    }
    if (route.isScreenRoute || route.isLayoutRoute) {
        helper.addFunction(route.localValidateMetadataFunction(packageName))
    }

    return FileSpec.builder(route.requiredRoutePackageName(), routeLocalHelperObjectName)
        .addType(helper.build())
        .build()
        .toString()
}

private fun RouteDirectoryRoute.localScreenFunction(packageName: String): FunSpec =
    FunSpec.builder(screenFunctionName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Declares this route as a Compose screen route with content-only screen body.\n")
        .addParameter(metadataNameParameter())
        .addParameter(metadataLabelsParameter())
        .addParameter(
            ParameterSpec.builder(
                name = contentParameterName,
                type = screenBodyLambdaType(destinationClass(packageName)),
            )
                .build(),
        )
        .returns(laydrComposeScreenRouteDefinitionClass)
        .addStatement("%L(%N, %N)", validateMetadataFunctionName, metadataNameParameterName, metadataLabelsParameterName)
        .addStatement(
            "return %T(route = %L) { match ->",
            laydrComposeScreenRouteDefinitionClass,
            routeDescriptorReference(packageName),
        )
        .addStatement(
            "val route = %T.%L(match)",
            routeObjectClass(packageName),
            requireDestinationFunctionName,
        )
        .addStatement(
            "%T { %N(route) }",
            laydrScreenContentClass,
            contentParameterName,
        )
        .addStatement("}")
        .build()

private fun RouteDirectoryRoute.localScreenWithLayoutValuesFunction(packageName: String): FunSpec =
    FunSpec.builder(screenWithLayoutValuesFunctionName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Declares this route as a Compose screen route that supplies layout values.\n")
        .addParameter(metadataNameParameter())
        .addParameter(metadataLabelsParameter())
        .addParameter(
            ParameterSpec.builder(
                name = contentParameterName,
                type = screenContentLambdaType(destinationClass(packageName)),
            )
                .build(),
        )
        .returns(laydrComposeScreenRouteDefinitionClass)
        .addStatement("%L(%N, %N)", validateMetadataFunctionName, metadataNameParameterName, metadataLabelsParameterName)
        .addStatement(
            "return %T(route = %L) { match ->",
            laydrComposeScreenRouteDefinitionClass,
            routeDescriptorReference(packageName),
        )
        .addStatement(
            "%N(%T.%L(match))",
            contentParameterName,
            routeObjectClass(packageName),
            requireDestinationFunctionName,
        )
        .addStatement("}")
        .build()

private fun RouteDirectoryRoute.localLayoutFunction(packageName: String): FunSpec =
    FunSpec.builder(layoutFunctionName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Declares this route as a Compose layout route.\n")
        .addParameter(metadataNameParameter())
        .addParameter(metadataLabelsParameter())
        .addParameter(
            ParameterSpec.builder(
                name = contentParameterName,
                type = layoutContentLambdaType(),
            )
                .build(),
        )
        .returns(laydrComposeLayoutRouteDefinitionClass)
        .addStatement("%L(%N, %N)", validateMetadataFunctionName, metadataNameParameterName, metadataLabelsParameterName)
        .addStatement(
            "return %T(route = %L) { context, content ->",
            laydrComposeLayoutRouteDefinitionClass,
            routeDescriptorReference(packageName),
        )
        .addStatement("%N(context, content)", contentParameterName)
        .addStatement("}")
        .build()

private fun RouteDirectoryRoute.localScreenAndLayoutBuilder(packageName: String): TypeSpec {
    val builder = TypeSpec.classBuilder(screenAndLayoutBuilderTypeName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Builder for a route that renders a screen and wraps descendants as a layout.\n")
        .addProperty(
            PropertySpec.builder(
                screenDefinitionPropertyName,
                laydrComposeScreenRouteDefinitionClass
                    .copy(nullable = true),
            )
                .addModifiers(KModifier.PRIVATE)
                .mutable(true)
                .initializer("null")
                .build(),
        )
        .addProperty(
            PropertySpec.builder(
                layoutDefinitionPropertyName,
                laydrComposeLayoutRouteDefinitionClass
                    .copy(nullable = true),
            )
                .addModifiers(KModifier.PRIVATE)
                .mutable(true)
                .initializer("null")
                .build(),
        )
        .addFunction(localScreenAndLayoutScreenFunction(packageName))
        .addFunction(localScreenAndLayoutScreenWithLayoutValuesFunction(packageName))
        .addFunction(localScreenAndLayoutLayoutFunction(packageName))
        .addFunction(localScreenAndLayoutBuildFunction())

    return builder.build()
}

private fun RouteDirectoryRoute.localScreenAndLayoutScreenFunction(packageName: String): FunSpec =
    FunSpec.builder(screenFunctionName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Registers the content-only screen part of a screen-and-layout route.\n")
        .addParameter(contentParameterName, screenBodyLambdaType(destinationClass(packageName)))
        .addStatement(
            "%L = %T(route = %L) { match ->",
            screenDefinitionPropertyName,
            laydrComposeScreenRouteDefinitionClass,
            routeDescriptorReference(packageName),
        )
        .addStatement(
            "val route = %T.%L(match)",
            routeObjectClass(packageName),
            requireDestinationFunctionName,
        )
        .addStatement(
            "%T { %N(route) }",
            laydrScreenContentClass,
            contentParameterName,
        )
        .addStatement("}")
        .build()

private fun RouteDirectoryRoute.localScreenAndLayoutScreenWithLayoutValuesFunction(packageName: String): FunSpec =
    FunSpec.builder(screenWithLayoutValuesFunctionName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Registers the layout-value screen part of a screen-and-layout route.\n")
        .addParameter(contentParameterName, screenContentLambdaType(destinationClass(packageName)))
        .addStatement(
            "%L = %T(route = %L) { match ->",
            screenDefinitionPropertyName,
            laydrComposeScreenRouteDefinitionClass,
            routeDescriptorReference(packageName),
        )
        .addStatement(
            "%N(%T.%L(match))",
            contentParameterName,
            routeObjectClass(packageName),
            requireDestinationFunctionName,
        )
        .addStatement("}")
        .build()

private fun RouteDirectoryRoute.localScreenAndLayoutLayoutFunction(packageName: String): FunSpec =
    FunSpec.builder(layoutFunctionName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Registers the layout part of a screen-and-layout route.\n")
        .addParameter(contentParameterName, layoutContentLambdaType())
        .addStatement(
            "%L = %T(route = %L) { context, content ->",
            layoutDefinitionPropertyName,
            laydrComposeLayoutRouteDefinitionClass,
            routeDescriptorReference(packageName),
        )
        .addStatement("%N(context, content)", contentParameterName)
        .addStatement("}")
        .build()

private fun localScreenAndLayoutBuildFunction(): FunSpec =
    FunSpec.builder(buildFunctionName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Builds the complete screen-and-layout route definition.\n")
        .returns(laydrComposeScreenAndLayoutRouteDefinitionClass)
        .addStatement(
            "val screen = %L ?: error(%S)",
            screenDefinitionPropertyName,
            "Missing Laydr screen definition in screenAndLayout route",
        )
        .addStatement(
            "val layout = %L ?: error(%S)",
            layoutDefinitionPropertyName,
            "Missing Laydr layout definition in screenAndLayout route",
        )
        .addStatement("return %T(screen, layout)", laydrComposeScreenAndLayoutRouteDefinitionClass)
        .build()

private fun localScreenAndLayoutFunction(): FunSpec =
    FunSpec.builder(screenAndLayoutFunctionName)
        .addModifiers(KModifier.PUBLIC)
        .addKdoc("Declares this route as both a Compose screen and layout route.\n")
        .addParameter(metadataNameParameter())
        .addParameter(metadataLabelsParameter())
        .addParameter(
            ParameterSpec.builder(
                name = blockParameterName,
                type = screenAndLayoutBuilderLambdaType(),
            )
                .build(),
        )
        .returns(laydrComposeScreenAndLayoutRouteDefinitionClass)
        .addStatement("%L(%N, %N)", validateMetadataFunctionName, metadataNameParameterName, metadataLabelsParameterName)
        .addStatement(
            "return %L().apply(%N).%L()",
            screenAndLayoutBuilderTypeName,
            blockParameterName,
            buildFunctionName,
        )
        .build()

private fun RouteDirectoryRoute.localValidateMetadataFunction(packageName: String): FunSpec =
    FunSpec.builder(validateMetadataFunctionName)
        .addModifiers(KModifier.PRIVATE)
        .addParameter(metadataNameParameterName, stringClass.copy(nullable = true))
        .addParameter(metadataLabelsParameterName, mapOfStringStringClass)
        .addStatement("val expected = %L.metadata", routeDescriptorReference(packageName))
        .beginControlFlow("if (%N != null)", metadataNameParameterName)
        .addStatement(
            "require(%N == expected.name) { %S + expected.name + %S + %N }",
            metadataNameParameterName,
            "Laydr route metadata name does not match generated metadata. Expected ",
            " but was ",
            metadataNameParameterName,
        )
        .endControlFlow()
        .addStatement(
            "require(%N == expected.labels) { %S + expected.labels + %S + %N }",
            metadataLabelsParameterName,
            "Laydr route metadata labels do not match generated metadata. Expected ",
            " but was ",
            metadataLabelsParameterName,
        )
        .build()

private fun metadataNameParameter(): ParameterSpec =
    ParameterSpec.builder(metadataNameParameterName, stringClass.copy(nullable = true))
        .defaultValue("null")
        .build()

private fun metadataLabelsParameter(): ParameterSpec =
    ParameterSpec.builder(metadataLabelsParameterName, mapOfStringStringClass)
        .defaultValue("%M()", emptyMapMember)
        .build()
