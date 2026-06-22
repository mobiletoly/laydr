package dev.goquick.laydr.nav3.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LaydrNavEntryMetadataKeyTest {
    @Test
    fun blankNamesFail() {
        val error = assertFailsWith<IllegalArgumentException> {
            laydrNavEntryMetadataKey<TestPresentation>(" ")
        }

        assertEquals(
            "Laydr Nav entry metadata key name must not be blank.",
            error.message,
        )
    }

    @Test
    fun typedKeyWritesRawStringMetadataEntry() {
        val key = laydrNavEntryMetadataKey<TestPresentation>("test:presentation")
        val metadata = LaydrNavEntryMetadata(
            key to TestPresentation.Overlay,
        )

        assertEquals(
            mapOf("test:presentation" to TestPresentation.Overlay),
            metadata.values,
        )
    }

    @Test
    fun typedKeyReadsMatchingValue() {
        val key = laydrNavEntryMetadataKey<TestPresentation>("test:presentation")
        val metadata = mapOf<String, Any>(
            "test:presentation" to TestPresentation.Overlay,
        )

        assertEquals(TestPresentation.Overlay, metadata[key])
    }

    @Test
    fun typedKeyReadReturnsNullForAbsentMetadata() {
        val key = laydrNavEntryMetadataKey<TestPresentation>("test:presentation")

        assertNull(emptyMap<String, Any>()[key])
    }

    @Test
    fun typedKeyReadReturnsNullForWrongType() {
        val key = laydrNavEntryMetadataKey<TestPresentation>("test:presentation")
        val metadata = mapOf<String, Any>(
            "test:presentation" to "overlay",
        )

        assertNull(metadata[key])
    }

    private enum class TestPresentation {
        Overlay,
    }
}
