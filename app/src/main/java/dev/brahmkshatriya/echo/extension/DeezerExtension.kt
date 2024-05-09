package dev.brahmkshatriya.echo.extension

import api.deezer.DeezerApi
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongFeedEndpoint

class DeezerExtension : ExtensionClient, HomeFeedClient {

    override val settingItems: List<Setting> = listOf()

    private lateinit var settings: Settings

    override fun setSettings(settings: Settings) {
        this.settings = settings
    }

    override suspend fun onExtensionSelected() {}

    private val api = DeezerApi()

    private val songFeedEndPoint = EchoSongFeedEndpoint(api)

    override suspend fun getHomeTabs(): List<Tab> {
        val result = api.genre().all.executeAsync()
        val tabs = result.join().data.map { genre ->
            Tab(genre.id.toString(), genre.name)
        }
        return listOf(tabs).flatten()
    }

    override fun getHomeFeed(tab: Tab?) = PagedData.Single {
        val params = tab?.id?.takeIf { id -> id != "null" }
        val name = tab?.name
        val result = songFeedEndPoint.getSongFeed(name = name, params = params).getOrThrow()

        val data = result.data.map { playlist ->
            playlist.toMediaItemsContainer(api, params, name)
        }
        data
    }
}