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