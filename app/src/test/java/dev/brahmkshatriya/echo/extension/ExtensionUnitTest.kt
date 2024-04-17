package dev.brahmkshatriya.echo.extension

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.LoadState
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCoroutinesApi
class ExtensionUnitTest {
    private val extension: ExtensionClient = TestExtension()
    private val searchQuery = "Skrillex"

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        extension.setPreferences(MockSharedPrefs())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    private fun testIn(title: String, block: suspend CoroutineScope.() -> Unit) = runBlocking {
        println("\n-- $title --")
        block.invoke(this)
        println("\n")
    }

    @Test
    fun testMetadata() = testIn("Testing Extension Metadata") {
        val metadata = extension.metadata
        println(metadata)
    }

    private val differ = AsyncPagingDataDiffer(
        MediaItemsContainerComparator(),
        ListCallback(),
    )

    private suspend fun <T : Any> Flow<PagingData<T>>.getItems(
        differ: AsyncPagingDataDiffer<T>
    ) = coroutineScope {
        val job = launch { collect { differ.submitData(it) } }
        val refresh = differ.loadStateFlow
            .first { it.refresh !is LoadState.Loading }
            .refresh
        job.cancel()
        if (refresh is LoadState.Error) throw refresh.error
        differ.snapshot().items
    }

    @Test
    fun testHomeFeed() = testIn("Testing Home Feed") {
        if (extension !is HomeFeedClient) error("HomeFeedClient is not implemented")
        val feed = extension.getHomeFeed(null).getItems(differ)
        feed.forEach {
            println(it)
        }
    }

    @Test
    fun testHomeFeedWithGenre() = testIn("Testing Home Feed with Genre") {
        if (extension !is HomeFeedClient) error("HomeFeedClient is not implemented")
        val genre = extension.getHomeGenres().firstOrNull()
        val feed = extension.getHomeFeed(genre).getItems(differ)
        feed.forEach {
            println(it)
        }
    }

    @Test
    fun testNullQuickSearch() = testIn("Testing Null Quick Search") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val search = extension.quickSearch(null)
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testQuickSearch() = testIn("Testing Quick Search") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val search = extension.quickSearch(searchQuery)
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testNullSearch() = testIn("Testing Null Search") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val search = extension.search(null, null).getItems(differ)
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testSearch() = testIn("Testing Search") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        println("Genres")
        extension.searchGenres(searchQuery).forEach {
            println(it.name)
        }
        println("Search Results")
        val search = extension.search(searchQuery, null).getItems(differ)
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testSearchWithGenre() = testIn("Testing Search with Genre") {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val genre = extension.searchGenres(searchQuery).firstOrNull()
        val search = extension.search(searchQuery, genre).getItems(differ)
        search.forEach {
            println(it)
        }
    }


    private suspend fun searchTrack(q: String? = null): Track {
        if (extension !is SearchClient) error("SearchClient is not implemented")
        val query = q ?: searchQuery
        println("Searching  : $query")
        val items = extension.search(query, null).getItems(differ)
        val track = items.firstNotNullOfOrNull {
            val item = when (it) {
                is MediaItemsContainer.Item -> it.media
                is MediaItemsContainer.Category -> it.list.firstOrNull()
                else -> null
            }
            (item as? EchoMediaItem.TrackItem)?.track
        }
        return track ?: error("Track not found, try a different search query")
    }

    @Test
    fun testTrackGet() = testIn("Testing Track Get") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        val search = searchTrack()
        measureTimeMillis {
            val track = extension.loadTrack(search)
            println(track)
        }.also { println("time : $it") }
    }

    @Test
    fun testTrackStream() = testIn("Testing Track Stream") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        val search = searchTrack()
        measureTimeMillis {
            val track = extension.loadTrack(search)
            val streamable = track.audioStreamables.firstOrNull()
                ?: error("Track is not streamable")
            val stream = extension.getStreamableAudio(streamable)
            println(stream)
        }.also { println("time : $it") }
    }

    @Test
    fun testTrackRadio() = testIn("Testing Track Radio") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        if (extension !is RadioClient) error("RadioClient is not implemented")
        val track = extension.loadTrack(searchTrack())
        val radio = extension.radio(track)
        radio.tracks.forEach {
            println(it)
        }
    }

    @Test
    fun testTrackMediaItems() = testIn("Testing Track Media Items") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        val track = searchTrack()
        val mediaItems = extension.getMediaItems(track).getItems(differ)
        mediaItems.forEach {
            println(it)
        }
    }

    @Test
    fun testAlbumGet() = testIn("Testing Album Get") {
        if (extension !is TrackClient) error("TrackClient is not implemented")
        val small = extension.loadTrack(searchTrack()).album ?: error("Track has no album")
        if (extension !is AlbumClient) error("AlbumClient is not implemented")
        val album = extension.loadAlbum(small)
        println(album)
    }

}