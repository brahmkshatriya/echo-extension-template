package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import org.junit.Test

import org.junit.Assert.*

class ExtensionUnitTest {
    private val extension: Any = TestExtension()

    @Test
    fun testMetadata() {
        println("\n-- Testing Extension Metadata --")
        if (extension is ExtensionClient) {
            val metadata = extension.getMetadata()
            println(metadata)
        } else {
            fail("ExtensionClient not implemented")
        }
        println("\n")
    }
}