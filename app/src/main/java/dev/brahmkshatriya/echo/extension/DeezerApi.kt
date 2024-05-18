package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.util.zip.GZIPInputStream

// Settings placeholder
class Settings {
    val deezerLanguage: String? = "de"
    val deezerCountry: String? = "DE"
}

class DeezerApi(
    private val arl: String = "",
    private val sid: String = "",
    private val token: String = "",
    val userId: String = "",
    private val licenseToken: String = "",
    private var userName: String? = null,
    private var favoritesPlaylistId: String = ""
) {

    private val settings = Settings()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            if (originalResponse.header("Content-Encoding") == "gzip") {
                val gzipSource = GZIPInputStream(originalResponse.body?.byteStream())
                val decompressedBody = gzipSource.readBytes().toResponseBody(originalResponse.body?.contentType())
                originalResponse.newBuilder().body(decompressedBody).build()
            } else {
                originalResponse
            }
        }.build()

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    //Get headers
    private fun getHeaders(): Headers {
        val headersBuilder = Headers.Builder()
        headersBuilder.add("Accept", "*/*")
        headersBuilder.add("Accept-Charset", "utf-8,ISO-8859-1;q=0.7,*;q=0.3")
        headersBuilder.add("Accept-Encoding", "gzip")
        headersBuilder.add("Accept-Language", settings.deezerLanguage ?: "en")
        headersBuilder.add("Cache-Control", "max-age=0")
        headersBuilder.add("Connection", "keep-alive")
        headersBuilder.add("Content-Language", "${settings.deezerLanguage ?: "en"}-${settings.deezerCountry ?: "US"}")
        headersBuilder.add("Content-Type", "application/json; charset=utf-8")
        headersBuilder.add("Cookie", "arl=$arl; sid=$sid")
        headersBuilder.add("Host", "www.deezer.com")
        headersBuilder.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
        return headersBuilder.build()
    }

    private suspend fun callApi(method: String, params: Map<*,*>? = null, gatewayInput: String? = ""): String = withContext(Dispatchers.IO) {
        // Generate URL
        val urlBuilder = HttpUrl.Builder()
            .scheme("https")
            .host("www.deezer.com")
            .addPathSegment("ajax")
            .addPathSegment("gw-light.php")
            .addQueryParameter("method", method)
            .addQueryParameter("input", "3")
            .addQueryParameter("api_version", "1.0")
            .addQueryParameter("api_token", token)

        // Conditionally add gateway_input if it's not empty
        if (!gatewayInput.isNullOrEmpty()) {
            urlBuilder.addQueryParameter("gateway_input", gatewayInput)
        }


        val url = urlBuilder.build()

        // Create request body
        val requestBody = JSONObject(params ?: emptyMap<String, Any>()).toString()
            .toRequestBody()

        // Create request
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .headers(getHeaders())
            .build()

        // Execute request
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        val body = responseBody.toString()

        // Grab SID
        /*if (method == "deezer.getUserData") {
            response.headers("Set-Cookie").forEach { cookie ->
                if (cookie.startsWith("sid=")) {
                    sid = cookie.split("=")[1]
                }
            }
        }*/

        body
    }

    private suspend fun rawAuthorize(): Boolean {
        val jsonData = callApi("deezer.getUserData")
        val jObject = json.decodeFromString<JsonObject>(jsonData)
        val userResults = jObject["results"]
        val user = userResults!!.jsonObject["USER"]
        val userId = user!!.jsonObject["USER_ID"]!!.jsonPrimitive.content
        if (userId.contentEquals("0")) {
           return false
        } else {
            //token = userResults.jsonObject["checkForm"]!!.jsonPrimitive.content
            //userId = user.jsonObject["USER_ID"]!!.jsonPrimitive.int.toString()
            userName = user.jsonObject["BLOG_NAME"]!!.jsonPrimitive.content
            favoritesPlaylistId = user.jsonObject["LOVEDTRACKS_ID"]!!.jsonPrimitive.content
          return  true
        }
    }

    suspend fun makeUser(): User {
        val jsonData = callApi("deezer.getUserData")
        val jObject = json.decodeFromString<JsonObject>(jsonData)
        val userResults = jObject["results"]
        val user = userResults!!.jsonObject["USER"]
        val token = userResults.jsonObject["checkForm"]!!.jsonPrimitive.content
        val userId = user!!.jsonObject["USER_ID"]!!.jsonPrimitive.content
        val licenseToken = user.jsonObject["OPTIONS"]!!.jsonObject["license_token"]!!.jsonPrimitive.content
        val name = user.jsonObject["BLOG_NAME"]!!.jsonPrimitive.content
        val cover = user.jsonObject["USER_PICTURE"]!!.jsonPrimitive.content
        return User(
            id = userId,
            name = name,
            cover = "https://e-cdns-images.dzcdn.net/images/user/$cover/100x100-000000-80-0-0.jpg".toImageHolder(),
            extras = mapOf(
                "arl" to arl,
                "user_id" to userId,
                "sid" to sid,
                "token" to token,
                "license_token" to licenseToken
            )
        )
    }

    fun getArlByEmail(mail: String, password: String): String {
        return ""
    }

    suspend fun getMediaUrl(track: Track): JsonObject = withContext(Dispatchers.IO) {
        val headersBuilder = Headers.Builder()
        headersBuilder.add("Accept-Encoding", "gzip")
        headersBuilder.add("Accept-Language", settings.deezerLanguage ?: "en")
        headersBuilder.add("Cache-Control", "max-age=0")
        headersBuilder.add("Connection", "Keep-alive")
        headersBuilder.add("Content-Type", "application/json; charset=utf-8")
        headersBuilder.add("Cookie", "arl=$arl&sid=$sid")
        //headersBuilder.add("Host", "media.deezer.com")
        headersBuilder.add("Host", "media.deezer.com")
        headersBuilder.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
        val headers = headersBuilder.build()

        val urlBuilder = HttpUrl.Builder()
            .scheme("https")
            .host("media.deezer.com")
            .addPathSegment("v1")
            .addPathSegment("get_url")

        val url = urlBuilder.build()

        // Create request body
        /*val requestBody = JSONObject(mapOf(
            "formats" to arrayOf("FLAC", "MP3_320", "MP3_128", "MP3_64", "MP3_MISC"),
            "ids" to arrayOf(track.id.toInt())
        )).toString().toRequestBody("application/json; charset=utf-8".toMediaType())*/

        val format = if (track.extras["FILESIZE_MP3_MISC"] != "") {
            "MP3_MISC"
        } else {
            "MP3_128"
        }

        val requestBody =
        JSONObject(
        mapOf(
            "license_token" to licenseToken,
            "media" to arrayOf(
                mapOf(
                    "type" to "FULL",
                    "formats" to arrayOf(
                        mapOf(
                            "cipher" to "BF_CBC_STRIPE",
                            "format" to format
                        )
                    )
                )
            ),
            "track_tokens" to arrayOf(track.extras["TRACK_TOKEN"])
        )
        ).toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // Create request
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .headers(headers)
            .build()

        // Execute request
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        val body = responseBody.toString()

        val jObject = json.decodeFromString<JsonObject>(body)

        jObject
    }

    suspend fun track(track: Track): JsonObject {
        val jsonData = callApi(
            method = "song.getListData",
            params = mapOf(
                "sng_ids" to arrayOf(track.id)
            )
        )
        val jObject = json.decodeFromString<JsonObject>(jsonData)
        return jObject
    }

    suspend fun album(album: Album): JsonObject {
        val jsonData = callApi(
            method = "deezer.pageAlbum",
            params = mapOf(
                "alb_id" to album.id,
                "header" to true,
                "lang" to settings.deezerLanguage
            )
        )
        val jObject = json.decodeFromString<JsonObject>(jsonData)
        return jObject
    }

    suspend fun getAlbums(): String {
        val jsonData = callApi(
            method = "deezer.pageProfile",
            params = mapOf(
                "user_id" to userId,
                "tab" to "albums",
                "nb" to 50
            )
        )
        //val jObject = json.decodeFromString<JsonObject>(jsonData)
        return ""
    }

    suspend fun playlist(playlist: Playlist): JsonObject {
        val jsonData = callApi(
            method = "deezer.pagePlaylist",
            params = mapOf(
                "playlist_id" to playlist.id,
                "lang" to settings.deezerLanguage,
                "nb" to playlist.tracks,
                "tags" to true,
                "start" to 0
            )
        )
        val jObject = json.decodeFromString<JsonObject>(jsonData)
        return jObject
    }

    suspend fun getPlaylists(): JsonObject {
        val jsonData = callApi(
            method = "deezer.pageProfile",
            params = mapOf(
                "user_id" to userId,
                "tab" to "playlists",
                "nb" to 100
            )
        )
        val jObject = json.decodeFromString<JsonObject>(jsonData)
        return jObject
    }

    suspend fun homePage(): JsonObject {
        val jsonData = callApi(
            method = "page.get",
            gatewayInput = """
                {"PAGE":"home","VERSION":"2.5","SUPPORT":{"ads":[],"deeplink-list":["deeplink"],"event-card":["live-event"],"grid-preview-one":["album","artist","artistLineUp","channel","livestream","flow","playlist","radio","show","smarttracklist","track","user","video-link","external-link"],"grid-preview-two":["album","artist","artistLineUp","channel","livestream","flow","playlist","radio","show","smarttracklist","track","user","video-link","external-link"],"grid":["album","artist","artistLineUp","channel","livestream","flow","playlist","radio","show","smarttracklist","track","user","video-link","external-link"],"horizontal-grid":["album","artist","artistLineUp","channel","livestream","flow","playlist","radio","show","smarttracklist","track","user","video-link","external-link"],"horizontal-list":["track","song"],"item-highlight":["radio"],"large-card":["album","external-link","playlist","show","video-link"],"list":["episode"],"message":["call_onboarding"],"mini-banner":["external-link"],"slideshow":["album","artist","channel","external-link","flow","livestream","playlist","show","smarttracklist","user","video-link"],"small-horizontal-grid":["flow"],"long-card-horizontal-grid":["album","artist","artistLineUp","channel","livestream","flow","playlist","radio","show","smarttracklist","track","user","video-link","external-link"],"filterable-grid":["flow"]},"LANG":"en","OPTIONS":["deeplink_newsandentertainment","deeplink_subscribeoffer"]}
            """.trimIndent()
        )
        val jObject = json.decodeFromString<JsonObject>(jsonData)
        val result = jObject["results"]!!.jsonObject
        return result
    }
}