package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


fun JsonObject.toMediaItemsContainer(
    api: DeezerApi = DeezerApi(),
    name: String?
): MediaItemsContainer {
    val itemsArray = jsonObject["items"]!!.jsonArray
    return MediaItemsContainer.Category(
        title = name ?: "Unknown",
        list = itemsArray.mapNotNull { item ->
            item.jsonObject.toEchoMediaItem(api)
        }
    )
}

fun JsonArray.toMediaItemsContainer(
    api: DeezerApi = DeezerApi(),
    name: String?
): MediaItemsContainer {
    val itemsArray = jsonArray
    return MediaItemsContainer.Category(
        title = name ?: "Unknown",
        list = itemsArray.mapNotNull { item ->
            item.jsonObject.toEchoMediaItem(api)
        }
    )
}

fun JsonElement.toEchoMediaItem(
    api: DeezerApi
): EchoMediaItem? {
    val data = jsonObject["data"]?.jsonObject ?: jsonObject
    val type = data["__TYPE__"]!!.jsonPrimitive.content
    return when {
        type.contains("playlist") -> EchoMediaItem.Lists.PlaylistItem(toPlaylist(api))
        type.contains("album") -> EchoMediaItem.Lists.AlbumItem(toAlbum())
        type.contains("song") -> EchoMediaItem.TrackItem(toTrack())
        else -> null
    }
}



fun JsonElement.toAlbum(): Album {
    val data = jsonObject["data"]?.jsonObject ?: jsonObject["DATA"]?.jsonObject ?: jsonObject
    return Album(
        id = data["ALB_ID"]?.jsonPrimitive?.content ?: "",
        title = data["ALB_TITLE"]?.jsonPrimitive?.content ?: "",
        cover = getCover(jsonObject),
        description = jsonObject["description"]?.jsonPrimitive?.content ?: "",
        subtitle = jsonObject["subtitle"]?.jsonPrimitive?.content ?: "",
    )
}

fun JsonElement.toTrack(): Track {
    val data = jsonObject["data"]?.jsonObject ?: jsonObject
    return Track(
        id = data["SNG_ID"]!!.jsonPrimitive.content,
        title = data["SNG_TITLE"]!!.jsonPrimitive.content,
        cover = getCover(jsonObject),
        extras = mapOf(
            "TRACK_TOKEN" to (data["TRACK_TOKEN"]?.jsonPrimitive?.content ?: ""),
            "FILESIZE_MP3_MISC" to (data["FILESIZE_MP3_MISC"]?.jsonPrimitive?.content ?: "")
        )
    )
}

fun JsonElement.toPlaylist(api: DeezerApi): Playlist {
    val data = jsonObject["data"]?.jsonObject ?: jsonObject["DATA"]?.jsonObject ?: jsonObject
    val type = jsonObject["PICTURE_TYPE"]?.jsonPrimitive?.content
    return Playlist(
        id = data["PLAYLIST_ID"]?.jsonPrimitive?.content ?: "",
        title = data["TITLE"]?.jsonPrimitive?.content ?: "",
        cover = getCover(jsonObject, type),
        description = data["DESCRIPTION"]?.jsonPrimitive?.content ?: "",
        subtitle = jsonObject["subtitle"]?.jsonPrimitive?.content ?: "",
        isEditable = data["PARENT_USER_ID"]!!.jsonPrimitive.content == api.userId,
        tracks = data["NB_SONG"]?.jsonPrimitive?.int ?: 0,
    )
}

fun getCover(jsonObject: JsonObject, type: String? = null): ImageHolder {
    if(type != null) {
        val md5 = jsonObject["PLAYLIST_PICTURE"]!!.jsonPrimitive.content
        val url = "https://e-cdns-images.dzcdn.net/images/$type/$md5/264x264-000000-80-0-0.jpg"
        return url.toImageHolder()
    } else {
        if (jsonObject["pictures"]?.jsonArray != null) {
            val pictureArray = jsonObject["pictures"]!!.jsonArray
            val picObject = pictureArray.first().jsonObject
            val md5 = picObject["md5"]!!.jsonPrimitive.content
            val type = picObject["type"]!!.jsonPrimitive.content
            val url = "https://e-cdns-images.dzcdn.net/images/$type/$md5/264x264-000000-80-0-0.jpg"
            return url.toImageHolder()
        } else if (jsonObject["DATA"]?.jsonObject != null) {
            val dataObject = jsonObject["DATA"]!!.jsonObject
            val md5 = dataObject["PLAYLIST_PICTURE"]?.jsonPrimitive?.content
                ?: dataObject["ALB_PICTURE"]?.jsonPrimitive?.content ?: ""
            val type = dataObject["PICTURE_TYPE"]?.jsonPrimitive?.content ?: "cover"
            val url = "https://e-cdns-images.dzcdn.net/images/$type/$md5/264x264-000000-80-0-0.jpg"
            return url.toImageHolder()
        } else {
            val md5 = jsonObject["ALB_PICTURE"]?.jsonPrimitive?.content ?: ""
            val url = "https://e-cdns-images.dzcdn.net/images/cover/$md5/264x264-000000-80-0-0.jpg"
            return url.toImageHolder()
        }
    }
}