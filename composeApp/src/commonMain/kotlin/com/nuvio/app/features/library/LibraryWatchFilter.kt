package com.nuvio.app.features.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.watching.application.WatchingState

internal enum class LibraryWatchFilter(val label: String) {
    All("Tous"), Watched("Vus"), Unwatched("Non vus");
    fun accepts(watched: Boolean): Boolean = when (this) {
        All -> true
        Watched -> watched
        Unwatched -> !watched
    }
}

internal data class LibraryWatchProjection(
    val all: List<MetaPreview>, val watched: List<MetaPreview>, val unwatched: List<MetaPreview>,
) {
    fun items(filter: LibraryWatchFilter): List<MetaPreview> = when (filter) {
        LibraryWatchFilter.All -> all
        LibraryWatchFilter.Watched -> watched
        LibraryWatchFilter.Unwatched -> unwatched
    }
}

internal fun buildLibraryWatchProjection(
    items: List<MetaPreview>, watchedKeys: Set<String>, fullyWatchedSeriesKeys: Set<String>,
): LibraryWatchProjection {
    val (watched, unwatched) = items.partition { item ->
        WatchingState.isPosterWatched(watchedKeys, item, fullyWatchedSeriesKeys)
    }
    return LibraryWatchProjection(items, watched, unwatched)
}

@Composable
internal fun LibraryWatchFilterBar(
    selected: LibraryWatchFilter, projection: LibraryWatchProjection,
    onSelect: (LibraryWatchFilter) -> Unit, modifier: Modifier = Modifier,
) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LibraryWatchFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter, onClick = { onSelect(filter) },
                label = { Text("${filter.label} (${projection.items(filter).size})") },
                modifier = Modifier.testTag("library-watch:${filter.name}"),
            )
        }
    }
}
