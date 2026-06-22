// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.codegen

import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

/**
 * Public entry point for Laydr source generation.
 */
public object LaydrCodegen {
    /**
     * Scans [routesDirectory] and returns Kotlin source for the generated route graph.
     *
     * The returned source uses [generatedPackage] as its package and does not write
     * files. Callers own output paths and source-set integration.
     */
    public fun generateRouteGraph(
        routesDirectory: Path,
        generatedPackage: String,
    ): String =
        RouteGraphGenerator().generateRouteGraph(
            routeTree = RouteDirectoryScanner().scan(
                routesRoot = routesDirectory,
                requirePackageNames = false,
            ),
            packageName = generatedPackage,
        )

    /**
     * Scans [routesDirectory] and returns all generated Laydr Kotlin sources.
     *
     * The returned sources always include the core route graph. When
     * [generateCompose] is true, the returned sources also include generated
     * Compose route definitions and route-local helper files. When
     * [generateNav3Kmp] is true, the returned sources also include generated
     * Nav3 KMP adapter helpers and require [generateCompose] to be true.
     * When [generateNav3Androidx] is true, the returned sources instead include
     * AndroidX Nav3 adapter helpers and require [generateCompose] to be true.
     * Only one Nav3 helper target may be enabled for a route tree.
     * Callers own output paths and source-set integration.
     */
    public fun generateSources(
        routesDirectory: Path,
        generatedPackage: String,
        generateCompose: Boolean = false,
        generateNav3Kmp: Boolean = false,
        generateNav3Androidx: Boolean = false,
    ): List<LaydrGeneratedSource> {
        val nav3HelperTarget = nav3HelperTarget(
            generateCompose = generateCompose,
            generateNav3Kmp = generateNav3Kmp,
            generateNav3Androidx = generateNav3Androidx,
        )
        return RouteGraphGenerator().generateSources(
            routeTree = RouteDirectoryScanner().scan(
                routesRoot = routesDirectory,
                requirePackageNames = generateCompose,
                generateCompose = generateCompose,
            ),
            packageName = generatedPackage,
            generateCompose = generateCompose,
            nav3HelperTarget = nav3HelperTarget,
        )
    }

    private fun nav3HelperTarget(
        generateCompose: Boolean,
        generateNav3Kmp: Boolean,
        generateNav3Androidx: Boolean,
    ): LaydrNav3HelperTarget? {
        if (generateNav3Kmp && generateNav3Androidx) {
            throw RouteGraphGeneratorException(
                "Only one Nav3 helper target can be enabled: nav3Kmp or nav3Androidx.",
            )
        }
        if (generateNav3Kmp && !generateCompose) {
            throw RouteGraphGeneratorException("Nav3 KMP helper generation requires generateCompose=true.")
        }
        if (generateNav3Androidx && !generateCompose) {
            throw RouteGraphGeneratorException("AndroidX Nav3 helper generation requires generateCompose=true.")
        }
        return when {
            generateNav3Kmp -> LaydrNav3HelperTarget.Kmp
            generateNav3Androidx -> LaydrNav3HelperTarget.Androidx
            else -> null
        }
    }
}

internal enum class LaydrNav3HelperTarget {
    Kmp,
    Androidx,
}

/**
 * One generated Kotlin source file.
 */
public data class LaydrGeneratedSource public constructor(
    /**
     * Slash-separated source path relative to the generated source root.
     */
    public val relativePath: String,
    /**
     * Generated Kotlin source text.
     */
    public val content: String,
)

internal class RouteDirectoryScanner {
    fun scan(
        routesRoot: Path,
        requirePackageNames: Boolean = false,
        generateCompose: Boolean = false,
    ): RouteDirectoryTree {
        val normalizedRoot = routesRoot.toAbsolutePath().normalize()

        if (!Files.exists(normalizedRoot)) {
            throw RouteDirectoryScannerException("Routes root does not exist: $normalizedRoot")
        }
        if (!Files.isDirectory(normalizedRoot)) {
            throw RouteDirectoryScannerException("Routes root is not a directory: $normalizedRoot")
        }

        val routeNames = mutableMapOf<String, Path>()
        val routes = scanChildren(
            parentDirectory = normalizedRoot,
            parentSegments = emptyList(),
            routeNames = routeNames,
            requirePackageNames = requirePackageNames,
            generateCompose = generateCompose,
        )
        return RouteDirectoryTree(
            routesRoot = normalizedRoot,
            routes = routes,
        )
    }

    private fun scanChildren(
        parentDirectory: Path,
        parentSegments: List<RouteDirectorySegment>,
        routeNames: MutableMap<String, Path>,
        requirePackageNames: Boolean,
        generateCompose: Boolean,
    ): List<RouteDirectoryRoute> {
        val stream = Files.list(parentDirectory)
        return try {
            stream
                .asSequence()
                .filter { Files.isDirectory(it) }
                .sortedBy { it.fileName.toString() }
                .mapNotNull {
                    scanRouteDirectory(
                        directory = it,
                        parentSegments = parentSegments,
                        routeNames = routeNames,
                        requirePackageNames = requirePackageNames,
                        generateCompose = generateCompose,
                    )
                }
                .toList()
        } finally {
            stream.close()
        }
    }

    private fun scanRouteDirectory(
        directory: Path,
        parentSegments: List<RouteDirectorySegment>,
        routeNames: MutableMap<String, Path>,
        requirePackageNames: Boolean,
        generateCompose: Boolean,
    ): RouteDirectoryRoute? {
        val routeFile = directory.resolve(routeFileName)
        val hasRouteFile = Files.isRegularFile(routeFile)

        if (!hasRouteFile && !hasRouteFileDescendantOrValidateIgnoredDirectories(directory)) {
            return null
        }

        val segment = parseSegment(directory)
        val segments = parentSegments + segment
        val children = scanChildren(
            parentDirectory = directory,
            parentSegments = segments,
            routeNames = routeNames,
            requirePackageNames = requirePackageNames,
            generateCompose = generateCompose,
        )

        if (!hasRouteFile) {
            return RouteDirectoryRoute(
                directory = directory,
                routeFile = null,
                routePackageName = null,
                routeKind = null,
                metadataName = null,
                metadataLabels = emptyMap(),
                routePath = routePathFor(segments),
                routeName = routeNameFor(segments),
                segments = segments,
                children = children,
            )
        }

        val routeDeclaration = parseRouteDeclaration(routeFile, generateCompose)
        val routePackageName = if (requirePackageNames) parsePackageName(routeFile) else null

        val routeName = routeNameFor(segments)
        val duplicate = routeNames.putIfAbsent(routeName, directory)
        if (duplicate != null) {
            throw RouteDirectoryScannerException(
                "Duplicate generated route name $routeName for $duplicate and $directory",
            )
        }

        if (routeDeclaration.kind == RouteDirectoryRouteKind.LAYOUT && !children.hasDeclaredRoutes()) {
            throw RouteDirectoryScannerException(
                "Layout-only route directory must contain child routes: $directory",
            )
        }

        return RouteDirectoryRoute(
            directory = directory,
            routeFile = routeFile,
            routePackageName = routePackageName,
            routeKind = routeDeclaration.kind,
            metadataName = routeDeclaration.metadataName,
            metadataLabels = routeDeclaration.metadataLabels,
            routePath = routePathFor(segments),
            routeName = routeName,
            segments = segments,
            children = children,
        )
    }

    private fun hasRouteFileDescendantOrValidateIgnoredDirectories(directory: Path): Boolean {
        validateNamespaceOnlySegmentDirectory(directory)
        val stream = Files.list(directory)
        return try {
            stream
                .asSequence()
                .filter { path -> Files.isDirectory(path) }
                .any { child ->
                    Files.isRegularFile(child.resolve(routeFileName)) ||
                        hasRouteFileDescendantOrValidateIgnoredDirectories(child)
                }
        } finally {
            stream.close()
        }
    }

    private fun validateNamespaceOnlySegmentDirectory(directory: Path) {
        val kotlinFiles = directKotlinFiles(directory)
        val kotlinFile = kotlinFiles.firstOrNull()
        if (kotlinFile != null) {
            throw RouteDirectoryScannerException(
                "Kotlin files under route segment directories require $routeFileName: $kotlinFile",
            )
        }
    }

    private fun directKotlinFiles(directory: Path): List<Path> {
        val stream = Files.list(directory)
        return try {
            stream
                .asSequence()
                .filter { path -> Files.isRegularFile(path) }
                .filter { path -> path.fileName.toString().endsWith(".kt") }
                .sortedBy { path -> path.fileName.toString() }
                .toList()
        } finally {
            stream.close()
        }
    }

    private fun parseRouteDeclaration(
        routeFile: Path,
        generateCompose: Boolean,
    ): RouteDirectoryRouteDeclaration {
        val originalSource = Files.readString(routeFile)
        val source = stripKotlinCommentsAndStrings(originalSource)
        val bindingMatches = routeBindingRegex.findAll(source)
            .filter { match -> source.isTopLevelAt(match.range.first) }
            .toList()
        val extraRouteKindCall = routeKindCallRegex.findAll(source)
            .firstOrNull { call ->
                bindingMatches.none { binding -> call.range.first in binding.range }
            }
        if (extraRouteKindCall != null) {
            throw RouteDirectoryScannerException(
                "$routeFileName route kind declarations must be assigned to the top-level Route binding: " +
                    "$routeFile",
            )
        }

        val matches = bindingMatches
            .map { match ->
                val visibility = match.groupValues[1]
                val receiver = match.groupValues[2]
                val function = match.groupValues[3]
                val openIndex = match.groups[4]?.range?.first
                    ?: error("Route binding regex must capture the route declaration opening token.")
                if (visibility == "private") {
                    throw RouteDirectoryScannerException(
                        "$routeFileName Route binding must be package-visible, not private: $routeFile",
                    )
                }
                if (generateCompose && receiver != composeRouteDeclarationReceiver) {
                    throw RouteDirectoryScannerException(
                        "$routeFileName must bind Route with LaydrRouteDef.* when Compose generation is enabled: " +
                            "$routeFile",
                    )
                }
                if (!generateCompose && receiver != coreRouteDeclarationReceiver) {
                    throw RouteDirectoryScannerException(
                        "$routeFileName must bind Route with LaydrRouteDeclaration.* for core-only generation. " +
                            "Enable compose.set(true) to use LaydrRouteDef.*: $routeFile",
                    )
                }
                val metadata = parseRouteMetadata(
                    routeFile = routeFile,
                    source = originalSource,
                    openIndex = openIndex,
                )
                RouteDirectoryRouteDeclaration(
                    kind = routeKind(function),
                    metadataName = metadata.name,
                    metadataLabels = metadata.labels,
                )
            }
            .toList()

        if (matches.isEmpty()) {
            throw RouteDirectoryScannerException(
                "$routeFileName must bind exactly one package-visible top-level Route value with " +
                    supportedRouteBindings(generateCompose) +
                    ": $routeFile",
            )
        }
        if (matches.size > 1) {
            throw RouteDirectoryScannerException(
                "$routeFileName has multiple Route bindings: $routeFile",
            )
        }

        return matches.single()
    }

    private fun String.isTopLevelAt(index: Int): Boolean {
        var depth = 0
        var cursor = 0
        while (cursor < index) {
            when (this[cursor]) {
                '{', '(', '[' -> depth += 1
                '}', ')', ']' -> depth -= 1
            }
            cursor += 1
        }
        return depth == 0
    }

    private fun supportedRouteBindings(generateCompose: Boolean): String =
        if (generateCompose) {
            "LaydrRouteDef.screen, LaydrRouteDef.screenWithLayoutValues, " +
                "LaydrRouteDef.layout, or LaydrRouteDef.screenAndLayout"
        } else {
            "LaydrRouteDeclaration.screen, LaydrRouteDeclaration.layout, or " +
                "LaydrRouteDeclaration.screenAndLayout"
        }

    private fun routeKind(function: String): RouteDirectoryRouteKind =
        when (function) {
            "screen" -> RouteDirectoryRouteKind.SCREEN
            "screenWithLayoutValues" -> RouteDirectoryRouteKind.SCREEN
            "layout" -> RouteDirectoryRouteKind.LAYOUT
            "screenAndLayout" -> RouteDirectoryRouteKind.SCREEN_AND_LAYOUT
            else -> error("Unsupported route declaration function: $function")
        }

    private fun parseRouteMetadata(
        routeFile: Path,
        source: String,
        openIndex: Int,
    ): RouteDirectoryRouteMetadata {
        if (source.getOrNull(openIndex) != '(') {
            return RouteDirectoryRouteMetadata()
        }

        val closeIndex = findMatchingParen(source = source, openIndex = openIndex)
            ?: throw RouteDirectoryScannerException(
                "Route metadata arguments are malformed in $routeFileName: $routeFile",
            )
        val arguments = source.substring(openIndex + 1, closeIndex)
        if (arguments.isBlank()) {
            return RouteDirectoryRouteMetadata()
        }

        var name: String? = null
        var labels: Map<String, String> = emptyMap()
        var positionalIndex = 0
        splitTopLevelCommas(arguments).forEach { argument ->
            val trimmed = argument.trim()
            if (trimmed.isEmpty()) {
                return@forEach
            }
            val namedArgument = namedArgumentRegex.matchEntire(trimmed)
            if (namedArgument != null) {
                val key = namedArgument.groupValues[1]
                val value = namedArgument.groupValues[2].trim()
                when (key) {
                    "name" -> name = parseMetadataName(routeFile = routeFile, expression = value)
                    "labels" -> labels = parseMetadataLabels(routeFile = routeFile, expression = value)
                    "content", "block" -> Unit
                }
            } else {
                when (positionalIndex) {
                    0 ->
                        name = parseMetadataName(routeFile = routeFile, expression = trimmed)
                    1 ->
                        labels = parseMetadataLabels(routeFile = routeFile, expression = trimmed)
                    else -> Unit
                }
                positionalIndex += 1
            }
        }

        return RouteDirectoryRouteMetadata(name = name, labels = labels)
    }

    private fun parseMetadataName(
        routeFile: Path,
        expression: String,
    ): String =
        parseStringLiteral(expression)
            ?: throw RouteDirectoryScannerException(
                "Route metadata name must be a string literal in $routeFileName: $routeFile",
            )

    private fun parseMetadataLabels(
        routeFile: Path,
        expression: String,
    ): Map<String, String> {
        if (expression == "emptyMap()") {
            return emptyMap()
        }
        if (!expression.startsWith("mapOf(") || !expression.endsWith(")")) {
            throw RouteDirectoryScannerException(
                "Route metadata labels must be mapOf string pairs in $routeFileName: $routeFile",
            )
        }

        val inner = expression.removePrefix("mapOf(").dropLast(1).trim()
        if (inner.isEmpty()) {
            return emptyMap()
        }

        return buildMap {
            splitTopLevelCommas(inner).forEach { pair ->
                val match = metadataLabelPairRegex.matchEntire(pair.trim())
                    ?: throw RouteDirectoryScannerException(
                        "Route metadata labels must be mapOf string pairs in $routeFileName: $routeFile",
                    )
                val key = parseStringLiteral(match.groupValues[1])
                    ?: throw RouteDirectoryScannerException(
                        "Route metadata label keys must be string literals in $routeFileName: $routeFile",
                    )
                val value = parseStringLiteral(match.groupValues[2])
                    ?: throw RouteDirectoryScannerException(
                        "Route metadata label values must be string literals in $routeFileName: $routeFile",
                    )
                put(key, value)
            }
        }
    }

    private fun findMatchingParen(source: String, openIndex: Int): Int? {
        var depth = 0
        var index = openIndex
        while (index < source.length) {
            val current = source[index]
            when {
                current == '"' && source.getOrNull(index + 1) == '"' && source.getOrNull(index + 2) == '"' ->
                    index = skipTripleQuotedString(source, index)
                current == '"' -> index = skipQuotedString(source, index, '"')
                current == '\'' -> index = skipQuotedString(source, index, '\'')
                current == '(' -> {
                    depth += 1
                    index += 1
                }
                current == ')' -> {
                    depth -= 1
                    if (depth == 0) {
                        return index
                    }
                    index += 1
                }
                else -> index += 1
            }
        }
        return null
    }

    private fun splitTopLevelCommas(source: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        var index = 0
        while (index < source.length) {
            val current = source[index]
            when {
                current == '"' && source.getOrNull(index + 1) == '"' && source.getOrNull(index + 2) == '"' ->
                    index = skipTripleQuotedString(source, index)
                current == '"' -> index = skipQuotedString(source, index, '"')
                current == '\'' -> index = skipQuotedString(source, index, '\'')
                current == '(' || current == '[' || current == '{' -> {
                    depth += 1
                    index += 1
                }
                current == ')' || current == ']' || current == '}' -> {
                    depth -= 1
                    index += 1
                }
                current == ',' && depth == 0 -> {
                    parts += source.substring(start, index)
                    start = index + 1
                    index += 1
                }
                else -> index += 1
            }
        }
        parts += source.substring(start)
        return parts
    }

    private fun skipTripleQuotedString(source: String, startIndex: Int): Int {
        var index = startIndex + 3
        while (index < source.length) {
            if (
                source[index] == '"' &&
                source.getOrNull(index + 1) == '"' &&
                source.getOrNull(index + 2) == '"'
            ) {
                return index + 3
            }
            index += 1
        }
        return source.length
    }

    private fun skipQuotedString(
        source: String,
        startIndex: Int,
        quote: Char,
    ): Int {
        var index = startIndex + 1
        var escaped = false
        while (index < source.length) {
            val current = source[index]
            index += 1
            when {
                escaped -> escaped = false
                current == '\\' -> escaped = true
                current == quote -> return index
            }
        }
        return source.length
    }

    private fun parseStringLiteral(expression: String): String? {
        if (expression.length < 2 || expression.first() != '"' || expression.last() != '"') {
            return null
        }
        if (expression.startsWith("\"\"\"")) {
            if (!expression.endsWith("\"\"\"") || expression.length < 6) {
                return null
            }
            val value = expression.removePrefix("\"\"\"").removeSuffix("\"\"\"")
            if ('$' in value) {
                return null
            }
            return value
        }

        val builder = StringBuilder()
        var index = 1
        while (index < expression.lastIndex) {
            val current = expression[index]
            if (current == '\\') {
                val escaped = expression.getOrNull(index + 1) ?: return null
                builder.append(
                    when (escaped) {
                        'b' -> '\b'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        '"' -> '"'
                        '\'' -> '\''
                        '\\' -> '\\'
                        '$' -> '$'
                        else -> escaped
                    },
                )
                index += 2
            } else if (current == '$') {
                return null
            } else {
                builder.append(current)
                index += 1
            }
        }
        return builder.toString()
    }

    private fun stripKotlinCommentsAndStrings(source: String): String {
        val builder = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            val current = source[index]
            val next = source.getOrNull(index + 1)
            when {
                current == '/' && next == '/' -> {
                    builder.append(' ')
                    builder.append(' ')
                    index += 2
                    while (index < source.length && source[index] != '\n') {
                        builder.append(' ')
                        index += 1
                    }
                }
                current == '/' && next == '*' -> {
                    builder.append(' ')
                    builder.append(' ')
                    index += 2
                    while (index < source.length) {
                        val blockCurrent = source[index]
                        val blockNext = source.getOrNull(index + 1)
                        if (blockCurrent == '*' && blockNext == '/') {
                            builder.append(' ')
                            builder.append(' ')
                            index += 2
                            break
                        }
                        builder.append(if (blockCurrent == '\n') '\n' else ' ')
                        index += 1
                    }
                }
                current == '"' && next == '"' && source.getOrNull(index + 2) == '"' -> {
                    builder.append("   ")
                    index += 3
                    while (index < source.length) {
                        if (
                            source[index] == '"' &&
                            source.getOrNull(index + 1) == '"' &&
                            source.getOrNull(index + 2) == '"'
                        ) {
                            builder.append("   ")
                            index += 3
                            break
                        }
                        builder.append(if (source[index] == '\n') '\n' else ' ')
                        index += 1
                    }
                }
                current == '"' -> {
                    builder.append(' ')
                    index += 1
                    var escaped = false
                    while (index < source.length) {
                        val stringCurrent = source[index]
                        builder.append(if (stringCurrent == '\n') '\n' else ' ')
                        index += 1
                        when {
                            escaped -> escaped = false
                            stringCurrent == '\\' -> escaped = true
                            stringCurrent == '"' -> break
                        }
                    }
                }
                current == '\'' -> {
                    builder.append(' ')
                    index += 1
                    var escaped = false
                    while (index < source.length) {
                        val charCurrent = source[index]
                        builder.append(if (charCurrent == '\n') '\n' else ' ')
                        index += 1
                        when {
                            escaped -> escaped = false
                            charCurrent == '\\' -> escaped = true
                            charCurrent == '\'' -> break
                        }
                    }
                }
                else -> {
                    builder.append(current)
                    index += 1
                }
            }
        }
        return builder.toString()
    }

    private fun parseSegment(directory: Path): RouteDirectorySegment {
        val name = directory.fileName.toString()
        if (name.startsWith(dynamicPrefix)) {
            val parameterName = name.removePrefix(dynamicPrefix)
            if (!snakeCaseRegex.matches(parameterName)) {
                throw RouteDirectoryScannerException(
                    "Dynamic route directory must be 'by_' plus a lowercase snake_case parameter: $directory",
                )
            }
            return RouteDirectorySegment.Dynamic(
                sourceName = name,
                parameterName = parameterName,
                kotlinParameterName = lowerCamelCase(parameterName),
            )
        }

        if (!snakeCaseRegex.matches(name)) {
            throw RouteDirectoryScannerException(
                "Route directory name must be lowercase snake_case: $directory",
            )
        }

        return RouteDirectorySegment.Static(sourceName = name)
    }

    private fun routePathFor(segments: List<RouteDirectorySegment>): String =
        segments.joinToString(separator = "/", prefix = "/") { it.pathPart }

    private fun routeNameFor(segments: List<RouteDirectorySegment>): String =
        segments
            .flatMap { it.sourceName.split("_") }
            .joinToString(separator = "") { word ->
                word.replaceFirstChar { char -> char.uppercaseChar() }
            } + "Route"

    private fun lowerCamelCase(sourceName: String): String {
        val words = sourceName.split("_")
        return words.first() + words.drop(1).joinToString(separator = "") { word ->
            word.replaceFirstChar { char -> char.uppercaseChar() }
        }
    }

    private fun parsePackageName(
        routeFile: Path,
        fileName: String = routeFileName,
    ): String {
        val source = Files.readString(routeFile)
        val packageName = packageRegex.find(source)?.groupValues?.get(1)
            ?: throw RouteDirectoryScannerException(
                "$fileName is missing package declaration: $routeFile",
            )
        if (!lowercasePackageNameRegex.matches(packageName)) {
            throw RouteDirectoryScannerException(
                "$fileName package must be dot-separated lowercase Kotlin identifiers: $routeFile",
            )
        }
        return packageName
    }

    private companion object {
        private const val routeFileName = "Route.kt"
        private const val dynamicPrefix = "by_"
        private val snakeCaseRegex = Regex("[a-z][a-z0-9]*(?:_[a-z0-9]+)*")
        private val packageRegex = Regex("""(?m)^\s*package\s+([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*$""")
        private val namedArgumentRegex = Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.+)""", RegexOption.DOT_MATCHES_ALL)
        private val metadataLabelPairRegex = Regex("""("(?:\\.|[^"\\])*")\s+to\s+("(?:\\.|[^"\\])*")""")
        private const val composeRouteDeclarationReceiver = "LaydrRouteDef"
        private const val coreRouteDeclarationReceiver = "LaydrRouteDeclaration"
        private val routeBindingRegex = Regex(
            """(?m)^[ \t]*(?:(public|internal|private)\s+)?val\s+Route\s*=\s*""" +
                """(LaydrRouteDef|LaydrRouteDeclaration)\s*\.\s*""" +
                """(screenAndLayout|screenWithLayoutValues|screen|layout)\b""" +
                """\s*(?:<[^>\n]+>\s*)?([\(\{])""",
        )
        private val routeKindCallRegex =
            Regex(
                """\b(?:LaydrRouteDef|LaydrRouteDeclaration)\s*\.\s*""" +
                    """(?:screenAndLayout|screenWithLayoutValues|screen|layout)\b""",
            )
    }
}

internal data class RouteDirectoryRouteDeclaration(
    val kind: RouteDirectoryRouteKind,
    val metadataName: String? = null,
    val metadataLabels: Map<String, String> = emptyMap(),
)

private data class RouteDirectoryRouteMetadata(
    val name: String? = null,
    val labels: Map<String, String> = emptyMap(),
)

internal data class RouteDirectoryTree(
    val routesRoot: Path,
    val routes: List<RouteDirectoryRoute>,
)

internal data class RouteDirectoryRoute(
    val directory: Path,
    val routeFile: Path?,
    val routePackageName: String?,
    val routeKind: RouteDirectoryRouteKind?,
    val metadataName: String?,
    val metadataLabels: Map<String, String>,
    val routePath: String,
    val routeName: String,
    val segments: List<RouteDirectorySegment>,
    val children: List<RouteDirectoryRoute>,
) {
    val isDeclaredRoute: Boolean
        get() = routeKind != null

    val isScreenRoute: Boolean
        get() = routeKind == RouteDirectoryRouteKind.SCREEN ||
            routeKind == RouteDirectoryRouteKind.SCREEN_AND_LAYOUT

    val isLayoutRoute: Boolean
        get() = routeKind == RouteDirectoryRouteKind.LAYOUT ||
            routeKind == RouteDirectoryRouteKind.SCREEN_AND_LAYOUT
}

private fun List<RouteDirectoryRoute>.hasDeclaredRoutes(): Boolean =
    any { route -> route.isDeclaredRoute || route.children.hasDeclaredRoutes() }

internal enum class RouteDirectoryRouteKind {
    SCREEN,
    LAYOUT,
    SCREEN_AND_LAYOUT,
}

internal sealed interface RouteDirectorySegment {
    val sourceName: String
    val pathPart: String

    data class Static(
        override val sourceName: String,
    ) : RouteDirectorySegment {
        override val pathPart: String = sourceName.replace("_", "-")
    }

    data class Dynamic(
        override val sourceName: String,
        val parameterName: String,
        val kotlinParameterName: String = parameterName,
    ) : RouteDirectorySegment {
        override val pathPart: String = "{$parameterName}"
    }
}

internal class RouteDirectoryScannerException(
    message: String,
) : IllegalArgumentException(message)
