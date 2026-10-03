package com.nuvio.app.features.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.watched.watchedItemKeys
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryWatchFilterTest {
    private val seen = MetaPreview("tt-seen", "movie", "Film déjà vu", releaseInfo = "2025")
    private val unseen = MetaPreview("tt-unseen", "movie", "Film à découvrir", releaseInfo = "2026")
    private val finishedSeries = MetaPreview("tt-complete", "series", "Série terminée")
    private val partialSeries = MetaPreview("tt-partial", "series", "Série en cours")

    @Test fun filtersUseMobileAliasesAndWholeSeriesCompletion() {
        val watched = watchedItemKeys("film", seen.id) + watchedItemKeys("series", partialSeries.id, season = 1, episode = 1)
        val complete = watchedItemKeys("tv", finishedSeries.id)
        val projection = buildLibraryWatchProjection(listOf(seen, unseen, finishedSeries, partialSeries), watched, complete)
        assertEquals(listOf(seen, finishedSeries), projection.items(LibraryWatchFilter.Watched))
        assertEquals(listOf(unseen, partialSeries), projection.items(LibraryWatchFilter.Unwatched))
        assertEquals(4, projection.items(LibraryWatchFilter.All).size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun desktopGridFiltersAndCountersReactToWatchedChanges() = runDesktopComposeUiTest(width = 1280, height = 720) {
        var watched by mutableStateOf(watchedItemKeys("movie", seen.id))
        var filter by mutableStateOf(LibraryWatchFilter.All)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { Surface {
                Column(Modifier.size(1280.dp, 720.dp).padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Films · Bibliothèque", style = MaterialTheme.typography.headlineLarge)
                    val projection = buildLibraryWatchProjection(listOf(seen, unseen), watched, emptySet())
                    LibraryWatchFilterBar(filter, projection, { filter = it })
                    LazyVerticalGrid(GridCells.FixedSize(180.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(projection.items(filter), key = { it.id }) { item ->
                            HomePosterCard(item, isWatched = item in projection.watched, modifier = Modifier.testTag(item.id))
                        }
                    }
                }
            } }
        }
        onNodeWithText("Tous (2)").assertExists()
        onNodeWithText("Vus (1)").assertExists()
        onNodeWithText("Non vus (1)").assertExists()
        onNodeWithTag(seen.id).assertExists()
        onNodeWithTag(unseen.id).assertExists()
        val output = File("build/test-artifacts/desktop-library-watch-filters.png")
        output.parentFile.mkdirs()
        output.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        onNodeWithText("Vus (1)").performClick()
        onNodeWithTag(seen.id).assertExists()
        onNodeWithTag(unseen.id).assertDoesNotExist()
        onNodeWithText("Non vus (1)").performClick()
        onNodeWithTag(seen.id).assertDoesNotExist()
        onNodeWithTag(unseen.id).assertExists()
        runOnIdle { watched = watched + watchedItemKeys("movie", unseen.id) }
        onNodeWithText("Vus (2)").assertExists()
        onNodeWithText("Non vus (0)").assertExists()
        onNodeWithTag(unseen.id).assertDoesNotExist()
        onNodeWithText("Tous (2)").performClick()
        onNodeWithTag(seen.id).assertExists()
        onNodeWithTag(unseen.id).assertExists()
    }
}
