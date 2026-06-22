// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class RouteGraphGenerator {
    fun generateRouteGraph(
        routeTree: RouteDirectoryTree,
        packageName: String,
    ): String {
        validatePackageName(packageName)

        val rootObject = TypeSpec.objectBuilder(rootObjectName)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Generated route graph for the Laydr route tree.\n")
            .addProperty(rootRoutesProperty(routeTree.routes))
            .addProperty(screenRoutesProperty(routeTree.routes))
            .addProperty(layoutRoutesProperty(routeTree.routes))
            .addProperty(rootRouteMapProperty())
            .addProperty(rootAppGraphProperty())

        rootObject.addRouteObjects(
            routes = routeTree.routes,
            parentName = rootObjectName,
            parentObjectPath = null,
        )

        return FileSpec.builder(packageName, rootObjectName)
            .addType(rootObject.build())
            .build()
            .toString()
    }

    fun generateSources(
        routeTree: RouteDirectoryTree,
        packageName: String,
        generateCompose: Boolean = false,
        generateNav3Kmp: Boolean = false,
        nav3HelperTarget: LaydrNav3HelperTarget? = if (generateNav3Kmp) LaydrNav3HelperTarget.Kmp else null,
    ): List<LaydrGeneratedSource> {
        validatePackageName(packageName)
        if (nav3HelperTarget != null && !generateCompose) {
            throw RouteGraphGeneratorException(
                "${nav3HelperTarget.displayName} helper generation requires generateCompose=true.",
            )
        }
        val coreSource = LaydrGeneratedSource(
            relativePath = generatedSourcePath(packageName, rootObjectName),
            content = generateRouteGraph(routeTree = routeTree, packageName = packageName),
        )
        if (!generateCompose) {
            return listOf(coreSource)
        }

        val routes = routeTree.routes.flattenDeclaredRoutes()
        val composeSources = listOf(
            coreSource,
            LaydrGeneratedSource(
                relativePath = generatedSourcePath(packageName, composeRootObjectName),
                content = generateComposeRouteDefinitions(routeTree = routeTree, packageName = packageName),
            ),
        ) + routes.map { route ->
            LaydrGeneratedSource(
                relativePath = generatedSourcePath(route.requiredRoutePackageName(), routeLocalHelperObjectName),
                content = generateRouteLocalHelper(route = route, packageName = packageName),
            )
        }
        if (nav3HelperTarget == null) {
            return composeSources
        }

        return composeSources + LaydrGeneratedSource(
            relativePath = generatedSourcePath(packageName, navRootObjectName),
            content = generateNavRoutes(
                routeTree = routeTree,
                packageName = packageName,
                target = nav3HelperTarget,
            ),
        )
    }

    private fun TypeSpec.Builder.addRouteObjects(
        routes: List<RouteDirectoryRoute>,
        parentName: String,
        parentObjectPath: String?,
    ) {
        val duplicateObjectName = routes
            .groupingBy { it.objectName }
            .eachCount()
            .firstNotNullOfOrNull { (objectName, count) ->
                objectName.takeIf { count > 1 }
            }
        if (duplicateObjectName != null) {
            throw RouteGraphGeneratorException(
                "Duplicate generated route object $duplicateObjectName under $parentName",
            )
        }

        routes
            .map { route ->
                route.toRouteObject(
                    objectPath = route.objectPath(parentObjectPath),
                )
            }
            .forEach(::addType)
    }

    private fun RouteDirectoryRoute.toRouteObject(objectPath: String): TypeSpec {
        val routeObject = TypeSpec.objectBuilder(objectName)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc(
                if (isDeclaredRoute) {
                    "Generated route API for `%L`.\n"
                } else {
                    "Generated namespace for route segment `%L`.\n"
                },
                routePath,
            )
        val destinationName = destinationTypeName()
        val dynamicParameters = dynamicParametersForGeneratedFunctions(destinationName)

        if (isDeclaredRoute) {
            if (isScreenRoute) {
                routeObject.addSuperinterface(
                    if (dynamicParameters.isEmpty()) {
                        laydrParameterlessScreenRouteRefClass
                    } else {
                        laydrScreenRouteRefClass
                    },
                )
            }
            if (isLayoutRoute) {
                routeObject.addSuperinterface(laydrLayoutRouteRefClass)
            }

            routeObject.addProperty(
                PropertySpec.builder(routePropertyName, laydrRouteClass)
                    .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                    .addKdoc("Route descriptor for this generated route.\n")
                    .initializer(routeInitializer(objectPath))
                    .build(),
            )
            routeObject.addFunction(routeMatchesFunction())
            routeObject.addFunction(routeContainsFunction())
        }

        if (isScreenRoute) {
            dynamicParameters.forEach { parameter ->
                routeObject.addType(dynamicParameterType(parameter))
                routeObject.addFunction(dynamicParameterFactory(parameter))
            }
            routeObject.addFunction(pathFunction(dynamicParameters))
            routeObject.addType(destinationType(objectPath, destinationName))
            routeObject.addFunction(destinationFunction(destinationName, dynamicParameters))
            if (dynamicParameters.isEmpty()) {
                routeObject.addProperty(defaultDestinationProperty(destinationName))
            }
            routeObject.addFunction(requireDestinationFunction(objectPath, destinationName))
        }

        routeObject.addRouteObjects(
            routes = children,
            parentName = objectName,
            parentObjectPath = objectPath,
        )

        return routeObject.build()
    }

    private fun rootRoutesProperty(routes: List<RouteDirectoryRoute>): PropertySpec =
        PropertySpec.builder(rootRoutesPropertyName, listOfRoutesClass)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Top-level generated route descriptors.\n")
            .initializer(
                listOfCode(
                    routes.nearestDeclaredRoutes().map { route ->
                        CodeBlock.of("%L.%L", route.objectReference(), routePropertyName)
                    },
                ),
            )
            .build()

    private fun screenRoutesProperty(routes: List<RouteDirectoryRoute>): PropertySpec =
        routeListProperty(
            name = screenRoutesPropertyName,
            routes = routes.flattenDeclaredRoutes().filter { route -> route.isScreenRoute },
        )

    private fun layoutRoutesProperty(routes: List<RouteDirectoryRoute>): PropertySpec =
        routeListProperty(
            name = layoutRoutesPropertyName,
            routes = routes.flattenDeclaredRoutes().filter { route -> route.isLayoutRoute },
        )

    private fun rootRouteMapProperty(): PropertySpec =
        PropertySpec.builder(routeMapPropertyName, laydrRouteMapClass)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Structural route map for generated route lookup.\n")
            .initializer(
                "%T(routes = %L, screenRoutes = %L, layoutRoutes = %L)",
                laydrRouteMapClass,
                rootRoutesPropertyName,
                screenRoutesPropertyName,
                layoutRoutesPropertyName,
            )
            .build()

    private fun rootAppGraphProperty(): PropertySpec =
        PropertySpec.builder(appGraphPropertyName, laydrAppGraphClass)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("App-level route graph facade for runtime adapters.\n")
            .initializer("%T(routeMap = %L)", laydrAppGraphClass, routeMapPropertyName)
            .build()

    private fun routeListProperty(
        name: String,
        routes: List<RouteDirectoryRoute>,
    ): PropertySpec =
        PropertySpec.builder(name, listOfRoutesClass)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Flattened generated route descriptors for `%L`.\n", name)
            .initializer(
                listOfCode(
                    routes.map { route ->
                        CodeBlock.of("%L.%L", route.objectReference(), routePropertyName)
                    },
                ),
            )
            .build()

    private fun RouteDirectoryRoute.routeInitializer(objectPath: String): CodeBlock {
        val builder = CodeBlock.builder()
        builder.add("%T(id = %S, segments = %L", laydrRouteClass, objectPath, segmentsInitializer())
        val declaredChildren = children.nearestDeclaredRoutes()
        if (declaredChildren.isNotEmpty()) {
            builder.add(
                ", children = %L",
                listOfCode(
                    declaredChildren.map { child ->
                        CodeBlock.of("%L.%L", child.objectReferenceFrom(this), routePropertyName)
                    },
                ),
            )
        }
        builder.add(", metadata = %L", metadataInitializer())
        builder.add(")")
        return builder.build()
    }

    private fun routeMatchesFunction(): FunSpec =
        FunSpec.builder("matches")
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Returns true when `key` targets this generated route with valid parameters.\n")
            .addParameter("key", laydrRouteKeyClass)
            .returns(Boolean::class)
            .addStatement(
                "return %L.id == key.routeId && runCatching { %L.%L(key.parameters) }.isSuccess",
                routePropertyName,
                routePropertyName,
                buildPathFunctionName,
            )
            .build()

    private fun routeContainsFunction(): FunSpec =
        FunSpec.builder("contains")
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Returns true when `key` is valid and belongs to this generated route subtree.\n")
            .addParameter("key", laydrRouteKeyClass)
            .returns(Boolean::class)
            .addStatement("return %L.%L.contains(this, key)", rootObjectName, routeMapPropertyName)
            .build()

    private fun RouteDirectoryRoute.segmentsInitializer(): CodeBlock =
        listOfCode(segments.map { segment -> segment.toRouteSegmentCode() })

    private fun RouteDirectoryRoute.metadataName(): String =
        metadataName ?: segments
            .flatMap { segment -> segment.sourceName.split("_") }
            .joinToString(separator = " ") { word ->
                word.replaceFirstChar { char -> char.uppercaseChar() }
            }

    private fun RouteDirectoryRoute.metadataInitializer(): CodeBlock {
        val name = metadataName()
        if (metadataLabels.isEmpty()) {
            return CodeBlock.of("%T(name = %S)", laydrRouteMetadataClass, name)
        }
        return CodeBlock.of(
            "%T(name = %S, labels = %L)",
            laydrRouteMetadataClass,
            name,
            mapOfCode(
                metadataLabels.entries
                    .sortedBy { entry -> entry.key }
                    .map { (key, value) -> CodeBlock.of("%S to %S", key, value) },
            ),
        )
    }

    private fun RouteDirectorySegment.toRouteSegmentCode(): CodeBlock =
        when (this) {
            is RouteDirectorySegment.Static -> CodeBlock.of("%T.Static(%S)", laydrRouteSegmentClass, pathPart)
            is RouteDirectorySegment.Dynamic -> CodeBlock.of("%T.Dynamic(%S)", laydrRouteSegmentClass, parameterName)
        }

    private fun dynamicParameterType(parameter: GeneratedDynamicParameter): TypeSpec =
        TypeSpec.classBuilder(parameter.typeName)
            .addModifiers(KModifier.PUBLIC, KModifier.VALUE)
            .addAnnotation(jvmInlineClass)
            .addKdoc(
                "Typed value for the `%L` dynamic route parameter.\n",
                parameter.segment.parameterName,
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", String::class)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", String::class)
                    .addModifiers(KModifier.PUBLIC)
                    .addKdoc("Decoded parameter value.\n")
                    .initializer("value")
                    .build(),
            )
            .build()

    private fun dynamicParameterFactory(parameter: GeneratedDynamicParameter): FunSpec =
        FunSpec.builder(parameter.kotlinParameterName)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc(
                "Creates a typed value for the `%L` dynamic route parameter.\n",
                parameter.segment.parameterName,
            )
            .addParameter("rawValue", String::class)
            .returns(ClassName("", parameter.typeName))
            .addStatement("return %L(rawValue)", parameter.typeName)
            .build()

    private fun pathFunction(
        dynamicParameters: List<GeneratedDynamicParameter>,
    ): FunSpec {
        val function = FunSpec.builder(pathFunctionName)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Builds the encoded route path for this screen route.\n")
            .returns(String::class)

        dynamicParameters.forEach { parameter ->
            function.addParameter(parameter.kotlinParameterName, ClassName("", parameter.typeName))
        }

        if (dynamicParameters.isEmpty()) {
            function.addStatement("return %L.%L()", routePropertyName, buildPathFunctionName)
        } else {
            function.addStatement(
                "return this.%L.%L(%L)",
                routePropertyName,
                buildPathFunctionName,
                mapOfCode(
                    dynamicParameters.map { parameter ->
                        CodeBlock.of(
                            "%S to %N.value",
                            parameter.segment.parameterName,
                            parameter.kotlinParameterName,
                        )
                    },
                ),
            )
        }

        return function.build()
    }

    private fun RouteDirectoryRoute.destinationType(
        objectPath: String,
        destinationTypeName: String,
    ): TypeSpec {
        val dynamicParameters = dynamicParametersForGeneratedFunctions(destinationTypeName)
        val routeReference = "$rootObjectName.$objectPath.$routePropertyName"

        if (dynamicParameters.isEmpty()) {
            return TypeSpec.objectBuilder(destinationTypeName)
                .addModifiers(KModifier.PUBLIC)
                .addKdoc("Generated destination for `%L`.\n", routePath)
                .addSuperinterface(laydrScreenDestinationClass)
                .addProperty(
                    PropertySpec.builder(routeKeyPropertyName, laydrRouteKeyClass)
                        .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                        .addKdoc("Framework-neutral key for this destination.\n")
                        .initializer("%L.%L()", routeReference, keyFunctionName)
                        .build(),
                )
                .addProperty(
                    PropertySpec.builder(pathPropertyName, String::class)
                        .addModifiers(KModifier.PUBLIC)
                        .addKdoc("Encoded route path for this destination.\n")
                        .initializer("%L.%L()", routeReference, buildPathFunctionName)
                        .build(),
                )
                .build()
        }

        val primaryConstructor = FunSpec.constructorBuilder()
        val destinationType = TypeSpec.classBuilder(destinationTypeName)
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.DATA)
            .addKdoc("Generated destination for `%L`.\n", routePath)
            .addSuperinterface(laydrScreenDestinationClass)

        dynamicParameters.forEach { parameter ->
            primaryConstructor.addParameter(parameter.kotlinParameterName, ClassName("", parameter.typeName))
            destinationType.addProperty(
                PropertySpec.builder(parameter.kotlinParameterName, ClassName("", parameter.typeName))
                    .addModifiers(KModifier.PUBLIC)
                    .addKdoc("Typed `%L` route parameter.\n", parameter.segment.parameterName)
                    .initializer(parameter.kotlinParameterName)
                    .build(),
            )
        }

        return destinationType
            .primaryConstructor(primaryConstructor.build())
            .addProperty(
                PropertySpec.builder(routeKeyPropertyName, laydrRouteKeyClass)
                    .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                    .addKdoc("Framework-neutral key for this destination.\n")
                    .initializer(
                        "%L.%L(%L)",
                        routeReference,
                        keyFunctionName,
                        parametersMapCode(dynamicParameters),
                    )
                    .build(),
            )
            .addProperty(
                PropertySpec.builder(pathPropertyName, String::class)
                    .addModifiers(KModifier.PUBLIC)
                    .addKdoc("Encoded route path for this destination.\n")
                    .initializer(
                        "%L.%L(%L)",
                        routeReference,
                        buildPathFunctionName,
                        parametersMapCode(dynamicParameters),
                    )
                    .build(),
            )
            .build()
    }

    private fun destinationFunction(
        destinationTypeName: String,
        dynamicParameters: List<GeneratedDynamicParameter>,
    ): FunSpec {
        val function = FunSpec.builder(destinationFunctionName)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc("Creates a generated destination for this screen route.\n")
            .returns(ClassName("", destinationTypeName))

        dynamicParameters.forEach { parameter ->
            function.addParameter(parameter.kotlinParameterName, ClassName("", parameter.typeName))
        }

        if (dynamicParameters.isEmpty()) {
            function.addStatement("return %L", destinationTypeName)
        } else {
            function.addStatement(
                "return %L(%L)",
                destinationTypeName,
                destinationConstructorArgumentsCode(dynamicParameters),
            )
        }

        return function.build()
    }

    private fun defaultDestinationProperty(destinationTypeName: String): PropertySpec =
        PropertySpec.builder(defaultDestinationPropertyName, ClassName("", destinationTypeName))
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addKdoc("Default destination for this parameterless screen route.\n")
            .initializer(destinationTypeName)
            .build()

    private fun RouteDirectoryRoute.requireDestinationFunction(
        objectPath: String,
        destinationTypeName: String,
    ): FunSpec {
        val dynamicParameters = dynamicParametersForGeneratedFunctions(destinationTypeName)
        val routeReference = "$rootObjectName.$objectPath.$routePropertyName"

        val function = FunSpec.builder(requireDestinationFunctionName)
            .addModifiers(KModifier.INTERNAL)
            .addParameter(routeMatchPropertyName, laydrRouteMatchClass)
            .returns(ClassName("", destinationTypeName))
            .addStatement(
                "require(%N.%L == %L) { %S + %N.%L.id }",
                routeMatchPropertyName,
                routePropertyName,
                routeReference,
                "Expected route match for $objectPath but was ",
                routeMatchPropertyName,
                routePropertyName,
            )
            .addStatement("%L.%L(%N.parameters)", routeReference, buildPathFunctionName, routeMatchPropertyName)

        if (dynamicParameters.isEmpty()) {
            function.addStatement("return %L", destinationTypeName)
        } else {
            function.addStatement(
                "return %L(%L)",
                destinationTypeName,
                routeMatchConstructorArgumentsCode(dynamicParameters),
            )
        }

        return function.build()
    }

    private fun parametersMapCode(dynamicParameters: List<GeneratedDynamicParameter>): CodeBlock =
        mapOfCode(
            dynamicParameters.map { parameter ->
                CodeBlock.of(
                    "%S to %N.value",
                    parameter.segment.parameterName,
                    parameter.kotlinParameterName,
                )
            },
        )

    private fun destinationConstructorArgumentsCode(
        dynamicParameters: List<GeneratedDynamicParameter>,
    ): CodeBlock =
        namedArgumentsCode(
            dynamicParameters.map { parameter ->
                CodeBlock.of("%N = %N", parameter.kotlinParameterName, parameter.kotlinParameterName)
            },
        )

    private fun routeMatchConstructorArgumentsCode(
        dynamicParameters: List<GeneratedDynamicParameter>,
    ): CodeBlock =
        namedArgumentsCode(
            dynamicParameters.map { parameter ->
                CodeBlock.of(
                    "%N = %L(%N.parameters.getValue(%S))",
                    parameter.kotlinParameterName,
                    parameter.typeName,
                    routeMatchPropertyName,
                    parameter.segment.parameterName,
                )
            },
        )

}

internal class RouteGraphGeneratorException(
    message: String,
) : IllegalArgumentException(message)
