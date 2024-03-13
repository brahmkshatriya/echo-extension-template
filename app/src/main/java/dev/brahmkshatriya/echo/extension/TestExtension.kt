package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata

class TestExtension : ExtensionClient {
    override val metadata = ExtensionMetadata(
        name = "Test",
        version = "1.0.0",
        description = "Test extension",
        author = "Echo",
        iconUrl = null
    )
}