package dev.brahmkshatriya.echo.extension

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer

class MediaItemsContainerComparator : DiffUtil.ItemCallback<MediaItemsContainer>() {
    override fun areItemsTheSame(
        oldItem: MediaItemsContainer,
        newItem: MediaItemsContainer
    ) = oldItem.sameAs(newItem)

    override fun areContentsTheSame(
        oldItem: MediaItemsContainer,
        newItem: MediaItemsContainer
    ) = oldItem == newItem

}

class ListCallback : ListUpdateCallback {
    override fun onChanged(position: Int, count: Int, payload: Any?) {}
    override fun onMoved(fromPosition: Int, toPosition: Int) {}
    override fun onInserted(position: Int, count: Int) {}
    override fun onRemoved(position: Int, count: Int) {}
}