package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import okhttp3.OkHttpClient
import okhttp3.Request

// For more information on which clients to use
// visit https://brahmkshatriya.github.io/echo/common/dev.brahmkshatriya.echo.common/
class TestExtension : ExtensionClient {

    // Hover the function to see their documentation, you can click on highlighted class names
    // to see their documentation as well.
    override suspend fun getSettingItems(): List<Setting> {
        return emptyList()
    }

    // Every extension has its own settings instance
    private lateinit var setting: Settings
    override fun setSettings(settings: Settings) {
        setting = settings
    }

    // Simple HTTP client usage example
    private val httpClient = OkHttpClient()
    override suspend fun onInitialize() {
        val request = Request.Builder().url("https://example.com").build()
        val response = httpClient.newCall(request).await()
        println("Response ${response.code}: ${response.body}")
    }
}