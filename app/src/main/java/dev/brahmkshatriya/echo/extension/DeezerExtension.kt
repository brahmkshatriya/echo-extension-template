package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.exceptions.LoginRequiredException
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.apache.http.conn.ConnectTimeoutException
import java.util.Locale

class DeezerExtension : ExtensionClient, HomeFeedClient, TrackClient, SearchClient, AlbumClient,
    PlaylistClient, LoginClient.WebView, LibraryClient {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    override val settingItems: List<Setting> = listOf()

    private lateinit var settings: Settings
    override fun setSettings(settings: Settings) {
        this.settings = settings
    }

    private var arl: String?
        get() = settings.getString("arl")
        set(value) = settings.putString("arl", value)

    private var sid: String?
        get() = settings.getString("sid")
        set(value) = settings.putString("sid", value)

    private var userId: String?
        get() = settings.getString("user_id")
        set(value) = settings.putString("user_id", value)

    private var token: String?
        get() = settings.getString("token")
        set(value) = settings.putString("token", value)

    private var licenseToken: String?
        get() = settings.getString("license_token")
        set(value) = settings.putString("license_token", value)

    override suspend fun onExtensionSelected() {}

    //<============= HomeTab =============>

    override suspend fun getHomeTabs(): List<Tab> {
        if (arl == null) return  emptyList()
        val resultObject = DeezerApi(arl!!, sid!!, token!!, userId!!).homePage()
        val id = resultObject["page_id"]!!.jsonPrimitive.content
        val sections = resultObject["sections"]!!.jsonArray
        val tab = Tab(
            id = id,
            name = "Home",
            extras = mapOf(
                "sections" to sections.toString()
            )
        )
        return listOf(tab)
    }

    override fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> = PagedData.Single {
        if(arl == null) throw loginRequiredException
        val dataList = mutableListOf<MediaItemsContainer>()
        val jsonData = json.decodeFromString<JsonArray>(tab?.extras!!["sections"].toString())
        jsonData.map { section ->
            val name = section.jsonObject["title"]!!.jsonPrimitive.content
            // Just for the time being until everything is implemented
            if (name == "Continue streaming" || name == "Mixes inspired by..." || name == "Playlists you'll love") {
                val data = section.jsonObject.toMediaItemsContainer(name = name)
                dataList.add(data)
            }
        }
        dataList
    }

    //<============= Library =============>

    private var allTabs: Pair<String, List<MediaItemsContainer>>? = null

    override suspend fun getLibraryTabs(): List<Tab> {
        if (arl == null) return emptyList()
        val tabs = listOf(Tab("playlists", "Playlists"), Tab("albums", "Albums"), Tab("tracks", "Tracks"))
        allTabs = "all" to tabs.mapNotNull { tab ->
            when(tab.id) {
                "playlists" -> {
                    val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).getPlaylists()
                    val resultObject = jsonObject["results"]!!.jsonObject
                    val tabObject = resultObject["TAB"]!!.jsonObject
                    val playlistObject = tabObject["playlists"]!!.jsonObject
                    val dataArray = playlistObject["data"]!!.jsonArray
                    dataArray.toMediaItemsContainer(DeezerApi(arl!!, sid!!, token!!, userId!!), tab.name)
                }
                "albums" -> {
                    val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).getAlbums()
                    val resultObject = jsonObject["results"]!!.jsonObject
                    val tabObject = resultObject["TAB"]!!.jsonObject
                    val playlistObject = tabObject["albums"]!!.jsonObject
                    val dataArray = playlistObject["data"]!!.jsonArray
                    dataArray.toMediaItemsContainer(DeezerApi(arl!!, sid!!, token!!, userId!!), tab.name)
                }
                "tracks" -> {
                    val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).getTracks()
                    val resultObject = jsonObject["results"]!!.jsonObject
                    val dataArray = resultObject["data"]!!.jsonArray
                    dataArray.toMediaItemsContainer(DeezerApi(arl!!, sid!!, token!!, userId!!), tab.name)
                }
                else -> {
                    null
                }
            }
        }
        return listOf(Tab("all", "All")) + tabs
    }

    override fun getLibraryFeed(tab: Tab?) = PagedData.Single {
        if(arl == null) throw loginRequiredException
        val tabId = tab?.id ?: "all"
        var list = listOf<MediaItemsContainer>()
        when (tabId) {
            "all" -> {
                val all = allTabs?.second
                if (all != null) return@Single all
            }
            "playlists" -> {
                val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).getPlaylists()
                val resultObject = jsonObject["results"]!!.jsonObject
                val tabObject = resultObject["TAB"]!!.jsonObject
                val playlistObject = tabObject["playlists"]!!.jsonObject
                val dataArray = playlistObject["data"]!!.jsonArray

                val itemArray = dataArray.mapNotNull { item ->
                    item.toEchoMediaItem(DeezerApi(arl!!, sid!!, token!!, userId!!))?.toMediaItemsContainer()
                }
                list = itemArray
            }

            "albums" -> {
                val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).getAlbums()
                val resultObject = jsonObject["results"]!!.jsonObject
                val tabObject = resultObject["TAB"]!!.jsonObject
                val playlistObject = tabObject["albums"]!!.jsonObject
                val dataArray = playlistObject["data"]!!.jsonArray

                val itemArray = dataArray.mapNotNull { item ->
                    item.toEchoMediaItem(DeezerApi(arl!!, sid!!, token!!, userId!!))?.toMediaItemsContainer()
                }
                list = itemArray
            }

            "tracks" -> {
                val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).getTracks()
                val resultObject = jsonObject["results"]!!.jsonObject
                val dataArray = resultObject["data"]!!.jsonArray
                val itemArray = dataArray.mapNotNull { item ->
                    item.toEchoMediaItem(DeezerApi(arl!!, sid!!, token!!, userId!!))?.toMediaItemsContainer()
                }
                list = itemArray
            }
        }
        list
    }

    override suspend fun addTracksToPlaylist(playlist: Playlist, tracks: List<Track>, index: Int, new: List<Track>) {
        TODO("Not yet implemented")
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        TODO("Not yet implemented")
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        TODO("Not yet implemented")
    }

    override suspend fun editPlaylistMetadata(playlist: Playlist, title: String, description: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun likeTrack(track: Track, liked: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun listEditablePlaylists(): List<Playlist> {
        TODO("Not yet implemented")
    }

    override suspend fun moveTrackInPlaylist(playlist: Playlist, tracks: List<Track>, fromIndex: Int, toIndex: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun removeTracksFromPlaylist(playlist: Playlist, tracks: List<Track>, indexes: List<Int>) {
        TODO("Not yet implemented")
    }

    //<============= Search =============>

    override suspend fun quickSearch(query: String?) = query?.run {
        try {
            val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).searchSuggestions(query)
            val resultObject = jsonObject["results"]!!.jsonObject
            val suggestionArray = resultObject["SUGGESTION"]!!.jsonArray
            suggestionArray.map { item ->
                val queryItem = item.jsonObject["QUERY"]!!.jsonPrimitive.content
                QuickSearchItem.SearchQueryItem(queryItem, false)
            }
        } catch (e: NullPointerException) {
            null
        } catch (e: ConnectTimeoutException) {
            null
        }
    } ?: listOf()

    private var oldSearch: Pair<String, List<MediaItemsContainer>>? = null
    override fun searchFeed(query: String?, tab: Tab?) = PagedData.Single {
        if (arl == null) throw loginRequiredException
        query ?: return@Single emptyList()
        val old = oldSearch?.takeIf {
            it.first == query && (tab == null || tab.id == "All")
        }?.second
        if (old != null) return@Single old

        var list = listOf<MediaItemsContainer>()
        if(tab?.id != "TOP_RESULT") {
            val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).search(query)
            val resultObject = jsonObject["results"]!!.jsonObject
            val tabObject = resultObject[tab?.id]!!.jsonObject
            val dataArray = tabObject["data"]!!.jsonArray

            val itemArray =  dataArray.mapNotNull { item ->
                item.toEchoMediaItem(DeezerApi(arl!!, sid!!, token!!, userId!!))?.toMediaItemsContainer()
            }
            list = itemArray
        }
        list
    }

    override suspend fun searchTabs(query: String?): List<Tab> {
        if (arl == null) return emptyList()
        query ?: return emptyList()
        val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).search(query)
        val resultObject = jsonObject["results"]!!.jsonObject
        val orderObject = resultObject["ORDER"]!!.jsonArray

        val tabs = orderObject.mapNotNull {
            val tab = it.jsonPrimitive.content
            Tab(
                id = tab,
                name = tab.lowercase().capitalize(Locale.ROOT)
            )
        }.filter {
            it.id != "TOP_RESULT" &&
            it.id != "FLOW_CONFIG"
        }

        oldSearch = query to tabs.map { tab ->
            val name = tab.id
            val tabObject = resultObject[name]!!.jsonObject
            val dataArray = tabObject["data"]!!.jsonArray
            dataArray.toMediaItemsContainer(DeezerApi(arl!!, sid!!, token!!, userId!!), name.lowercase().capitalize(
                Locale.ROOT))
        }
        return listOf(Tab("All", "All")) + tabs
    }

    //<============= Play =============>

    private val client = OkHttpClient()

    override suspend fun getStreamableAudio(streamable: Streamable) = getByteStreamAudio(streamable, client)

    override suspend fun getStreamableVideo(streamable: Streamable) = throw Exception("not Used")

    override suspend fun loadTrack(track: Track) = coroutineScope {
        val jsonObject = if (track.extras["FILESIZE_MP3_MISC"] != "") {
            DeezerApi(arl!!, sid!!, token!!, userId!!, licenseToken!!).getMP3MediaUrl(track)
        } else {
            DeezerApi(arl!!, sid!!, token!!, userId!!, licenseToken!!).getMediaUrl(track)
        }
        val dataObject = jsonObject["data"]!!.jsonArray.first().jsonObject
        val mediaObject = dataObject["media"]!!.jsonArray.first().jsonObject
        val sourcesObject = mediaObject["sources"]!!.jsonArray[0]
        val url = sourcesObject.jsonObject["url"]!!.jsonPrimitive.content
        val key = Utils.createBlowfishKey(trackId = track.id)

        Track(
            id = track.id,
            title = track.title,
            cover = track.cover,
            audioStreamables = listOf(
                Streamable(
                    id = url,
                    quality = 0,
                    extra = mapOf(
                        "key" to key
                    )
                )
            )
        )
    }

    override fun getMediaItems(track: Track): PagedData<MediaItemsContainer> = PagedData.Single {
        coroutineScope {
            val album = track.album?.let {
                async { listOf(loadAlbum(it).toMediaItem().toMediaItemsContainer()) }
            } ?: async { listOf() }
            album.await()
        }
    }

    //<============= Album =============>

    override fun getMediaItems(album: Album) = PagedData.Single {
        val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).album(album)
        val resultsObject = jsonObject["results"]!!.jsonObject
        val songsObject = resultsObject["SONGS"]!!.jsonObject
        val dataArray = songsObject["data"]!!.jsonArray
        val data = dataArray.map { song ->
            song.jsonObject.toMediaItemsContainer(name = "")
        }
        data
    }

    override suspend fun loadAlbum(album: Album): Album {
        val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).album(album)
        val resultsObject = jsonObject["results"]!!.jsonObject
        return resultsObject.toAlbum()
    }

    override fun loadTracks(album: Album): PagedData<Track> = PagedData.Single {
        val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).album(album)
        val resultsObject = jsonObject["results"]!!.jsonObject
        val songsObject = resultsObject["SONGS"]!!.jsonObject
        val dataArray = songsObject["data"]!!.jsonArray
        val data = dataArray.map { song ->
            song.jsonObject.toTrack()
        }
        data
    }

    //<============= Playlist =============>
    override fun getMediaItems(playlist: Playlist) = PagedData.Single {
        val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).playlist(playlist)
        val resultsObject = jsonObject["results"]!!.jsonObject
        val songsObject = resultsObject["SONGS"]!!.jsonObject
        val dataArray = songsObject["data"]!!.jsonArray
        val data = dataArray.map { song ->
            song.jsonObject.toMediaItemsContainer(name = "")
        }
        data
    }

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).playlist(playlist)
        val resultsObject = jsonObject["results"]!!.jsonObject
        return resultsObject.toPlaylist(DeezerApi(userId = userId!!))
    }

    override fun loadTracks(playlist: Playlist): PagedData<Track> = PagedData.Single {
        val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!).playlist(playlist)
        val resultsObject = jsonObject["results"]!!.jsonObject
        val songsObject = resultsObject["SONGS"]!!.jsonObject
        val dataArray = songsObject["data"]!!.jsonArray
        val data = dataArray.map { song ->
            song.jsonObject.toTrack()
        }
        data
    }

    //<============= Login =============>

    private val loginRequiredException = LoginRequiredException("deezer", "Deezer")

    override val loginWebViewInitialUrl = "https://www.deezer.com/login?redirect_type=page&redirect_link=%2Faccount%2F"
        .toRequest(mapOf(Pair("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")))

    override val loginWebViewStopUrlRegex = "https://www\\.deezer\\.com/account/.*".toRegex()

    override suspend fun onLoginWebviewStop(url: String, cookie: String): List<User> {
        val userList = mutableListOf<User>()
        if (cookie.contains("arl=")) {
            val arl = cookie.substringAfter("arl=").substringBefore(";")
            val sid = cookie.substringAfter("sid=").substringBefore(";")
            val user = DeezerApi(arl, sid).makeUser()
            userList.add(user)
            return userList
        } else {
            return emptyList()
        }
    }

    override suspend fun onSetLoginUser(user: User?) {
        if (user != null) {
            arl = user.extras["arl"]
            userId = user.extras["user_id"]
            sid = user.extras["sid"]
            token = user.extras["token"]
            licenseToken = user.extras["license_token"]
        }
    }
}