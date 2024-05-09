package dev.brahmkshatriya.echo.extension.endpoints

import api.deezer.DeezerApi
import api.deezer.objects.data.PlaylistData

open class EchoSongFeedEndpoint(private val api: DeezerApi) {

    fun getSongFeed(
        name: String? = "",
        params: String? = null,
    ) = runCatching {

        fun performRequest(): PlaylistData {
            val response = api.chart().topPlaylists.addParam(name, params).executeAsync()
            return response.join()
        }

        return@runCatching performRequest()
    }
}