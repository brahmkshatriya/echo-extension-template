package dev.brahmkshatriya.echo.extension

import api.deezer.DeezerApi
import api.deezer.objects.Playlist as DeezerPlaylist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist

fun DeezerPlaylist.toMediaItemsContainer(
    api: DeezerApi,
    params: String?,
    name: String?
): MediaItemsContainer {
    val result = api.chart().topPlaylists.executeAsync()
    return MediaItemsContainer.Category(
        title = name ?: "Unknown",
        list =
        result.join().data.mapNotNull { item ->
                item.toEchoMediaPlaylistItem(api)
        },
    )
}

fun DeezerPlaylist.toEchoMediaPlaylistItem(
    api: DeezerApi
): EchoMediaItem? {
    return when (this) {
        is DeezerPlaylist -> EchoMediaItem.Lists.PlaylistItem(toPlaylist())
        else -> null
    }

}

fun DeezerPlaylist.toPlaylist(): Playlist {
    return Playlist(
        id = id.toString(),
        title = title,
        cover = picture.toImageHolder(),
        isEditable = !public,
        tracks = nbTracks,
    )
}