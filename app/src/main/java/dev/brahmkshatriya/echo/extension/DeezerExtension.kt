package dev.brahmkshatriya.echo.extension

import api.deezer.DeezerApi
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongFeedEndpoint
import dev.brahmkshatriya.echo.extension.network.GET
import dev.brahmkshatriya.echo.extension.network.LoginInterceptor
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response

class DeezerExtension : ExtensionClient, HomeFeedClient, LoginClient.WebView {

    override val settingItems: List<Setting> = listOf()

    private lateinit var settings: Settings

    private val client: OkHttpClient = OkHttpClient()

    private val loginInterceptor = client.newBuilder().addInterceptor(LoginInterceptor(client)).build()

    override fun setSettings(settings: Settings) {
        this.settings = settings
    }

    override suspend fun onExtensionSelected() {}

    private val api = DeezerApi()

    private val songFeedEndPoint = EchoSongFeedEndpoint(api)

    override suspend fun getHomeTabs(): List<Tab> {
        /*val result = api.genre().all.executeAsync()
        val tabs = result.join().data.map { genre ->
            Tab(genre.id.toString(), genre.name)
        }
        return listOf(tabs).flatten()*/
        return emptyList()
    }

    override fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> = PagedData.Single {
        /*val params = tab?.id?.takeIf { id -> id != "null" }
        val name = tab?.name
        val result = songFeedEndPoint.getSongFeed(name = name, params = params).getOrThrow()

        val data = result.data.map { playlist ->
        playlist.toMediaItemsContainer(api, params, name)
        }
        data*/
        emptyList()
    }

    override val loginWebViewInitialUrl = "https://www.deezer.com/login?redirect_type=page&redirect_link=%2Faccount%2F"
        .toRequest(mapOf(Pair("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")))
        //GET("https://www.deezer.com/login?redirect_type=page&redirect_link=%2Faccount%2F",Headers.headersOf("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"))

        //"https://www.deezer.com/login?redirect_type=page&redirect_link=%2Faccount%2F".toRequest(Headers.headersOf("test","test").toMap())

    override val loginWebViewStopUrlRegex = "https://www\\.deezer\\.com/account/.*".toRegex()

    override suspend fun onLoginWebviewStop(url: String, cookie: String): List<User> {
        if (url.contains("deezer")) throw Exception(url)
        return emptyList()
    }

    override suspend fun onSetLoginUser(user: User?) {
    }
}