package dev.brahmkshatriya.echo.extension

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer

class MediaItemsContainerComparator : DiffUtil.ItemCallback<MediaItemsContainer>() {

    override fun areItemsTheSame(
        oldItem: MediaItemsContainer,
        newItem: MediaItemsContainer
    ): Boolean {
        return when (oldItem) {
            is MediaItemsContainer.Category -> {
                val newCategory = newItem as? MediaItemsContainer.Category
                oldItem.title == newCategory?.title
            }

            is MediaItemsContainer.TrackItem -> {
                val newTrack = newItem as? MediaItemsContainer.TrackItem
                oldItem.track.uri == newTrack?.track?.uri
            }

            is MediaItemsContainer.AlbumItem -> {
                val newAlbum = newItem as? MediaItemsContainer.AlbumItem
                oldItem.album.uri == newAlbum?.album?.uri
            }
            is MediaItemsContainer.ArtistItem -> {
                val newArtist = newItem as? MediaItemsContainer.ArtistItem
                oldItem.artist.uri == newArtist?.artist?.uri
            }
            is MediaItemsContainer.PlaylistItem -> {
                val newPlaylist = newItem as? MediaItemsContainer.PlaylistItem
                oldItem.playlist.uri == newPlaylist?.playlist?.uri
            }
        }
    }

    override fun areContentsTheSame(
        oldItem: MediaItemsContainer,
        newItem: MediaItemsContainer
    ): Boolean {
        when (oldItem) {
            is MediaItemsContainer.Category -> {
                val newCategory = newItem as? MediaItemsContainer.Category
                newCategory ?: return true
                oldItem.list.forEachIndexed { index, mediaItem ->
                    if (newCategory.list.getOrNull(index) != mediaItem) return false
                }
            }

            is MediaItemsContainer.TrackItem -> {
                val newTrack = newItem as? MediaItemsContainer.TrackItem
                return oldItem.track == newTrack?.track
            }

            is MediaItemsContainer.AlbumItem -> {
                val newAlbum = newItem as? MediaItemsContainer.AlbumItem
                return oldItem.album == newAlbum?.album
            }
            is MediaItemsContainer.ArtistItem -> {
                val newArtist = newItem as? MediaItemsContainer.ArtistItem
                return oldItem.artist == newArtist?.artist
            }
            is MediaItemsContainer.PlaylistItem -> {
                val newPlaylist = newItem as? MediaItemsContainer.PlaylistItem
                return oldItem.playlist == newPlaylist?.playlist
            }
        }
        return true
    }
}

class ListCallback : ListUpdateCallback {
    override fun onChanged(position: Int, count: Int, payload: Any?) {}
    override fun onMoved(fromPosition: Int, toPosition: Int) {}
    override fun onInserted(position: Int, count: Int) {}
    override fun onRemoved(position: Int, count: Int) {}
}