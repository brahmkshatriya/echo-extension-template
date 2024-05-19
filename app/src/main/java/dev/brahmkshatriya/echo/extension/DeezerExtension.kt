package dev.brahmkshatriya.echo.extension

import android.util.Log
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.exceptions.LoginRequiredException
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableVideo
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DeezerExtension : ExtensionClient, HomeFeedClient, TrackClient, AlbumClient, PlaylistClient, LoginClient.WebView {

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
        if(arl == null) {
            throw LoginRequiredException("", "Deezer")
        } else {
            val resultObject = DeezerApi(arl!!, sid!!, token!!, userId!!).homePage()
            val name = resultObject["title"]!!.jsonPrimitive.content
            val id = resultObject["page_id"]!!.jsonPrimitive.content
            val sections = resultObject["sections"]!!.jsonArray
            val tab = Tab(
                id = id,
                name = name,
                extras = mapOf(
                    "sections" to sections.toString()
                )
            )
            return listOf(tab)
        }
    }

    override fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> = PagedData.Single {
        val dataList = mutableListOf<MediaItemsContainer>()
        val jsonData = json.decodeFromString<JsonArray>(tab?.extras!!["sections"].toString())
        //val data = jsonData[2].jsonObject.toMediaItemsContainer(name = name)
         jsonData.mapIndexed { index, section ->
            if(index == 1 || index == 2 || index == 6) {
                val name = section.jsonObject["title"]!!.jsonPrimitive.content
                val data = section.jsonObject.toMediaItemsContainer(name = name)
                dataList.add(data)
            }
        }
        dataList
    }
    //<============= Play =============>

    private val client = OkHttpClient()

    override suspend fun getStreamableAudio(streamable: Streamable) = getByteStreamAudio(streamable)

    private fun getByteStreamAudio(streamable: Streamable): StreamableAudio {
        val url = streamable.id
        val contentLength = getContentLength(url)
        val key = streamable.extra["key"]!!

        val request = okhttp3.Request.Builder().url(url).build()
        var decChunk = ByteArray(0)
        with(client.newCall(request).execute()) {
            val byteStream = this.body?.byteStream()

            // Read the entire byte stream into memory
            val completeStream = ByteArrayOutputStream()
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (byteStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                completeStream.write(buffer, 0, bytesRead)
            }

            // Decrypt the complete stream
            val completeStreamBytes = completeStream.toByteArray()

            var place = 0

            while (place < completeStreamBytes.size) {
                val remainingBytes = completeStreamBytes.size - place
                val chunkSize = if (remainingBytes > 2048 * 3) 2048 * 3 else remainingBytes
                val decryptingChunk = completeStreamBytes.copyOfRange(place, place + chunkSize)
                place += chunkSize

                var decryptedChunk = ByteArray(0)
                if (decryptingChunk.size > 2048) {
                    decryptedChunk = Utils.decryptBlowfish(decryptingChunk.copyOfRange(0, 2048), key)
                    decryptedChunk += decryptingChunk.copyOfRange(2048, decryptingChunk.size)
                }
                decChunk += decryptedChunk
            }
        }

        return StreamableAudio.ByteStreamAudio(
            stream = decChunk.inputStream(),
            totalBytes = contentLength
        )
    }



    private fun getContentLength(url: String): Long {
        var totalLength = 0L
        val request = okhttp3.Request.Builder().url(url).head().build()
        val response = client.newCall(request).execute()
        totalLength += response.header("Content-Length")?.toLong() ?: 0L
        response.close()
        return totalLength
    }

    override suspend fun getStreamableVideo(streamable: Streamable) =
        StreamableVideo(Request(streamable.id), looping = false, crop = false)

    override suspend fun loadTrack(track: Track) = coroutineScope {
        val jsonObject = DeezerApi(arl!!, sid!!, token!!, userId!!, licenseToken!!).getMediaUrl(track)
        val dataObject = jsonObject["data"]!!.jsonArray.first().jsonObject
        val mediaObject = dataObject["media"]!!.jsonArray.first().jsonObject
        val sourcesObject = mediaObject["sources"]!!.jsonArray[1]
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