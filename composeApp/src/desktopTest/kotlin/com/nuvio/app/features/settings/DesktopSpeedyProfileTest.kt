package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.details.mainSeriesStats
import com.nuvio.app.features.details.components.desktopEpisodeCountLabel
import com.nuvio.app.features.details.components.desktopSeasonCountLabel
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watched.watchedItemKeys
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import java.io.File
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import kotlin.test.*

class DesktopSpeedyProfileTest {
    @Test fun seriesCountsExcludeSpecialsAndDuplicateEpisodeIdentities() {
        val episodes = (1..4).flatMap { season -> (1..8).map { episode ->
            MetaVideo("reacher:$season:$episode", "Épisode $episode", season = season, episode = episode)
        } }
        val meta = MetaDetails("reacher", "series", "Reacher", videos = episodes + episodes.first().copy(id = "other-addon") +
            MetaVideo("special", "Spécial", season = 0, episode = 1))
        assertEquals(32, meta.mainSeriesStats()?.episodeCount)
        assertEquals("4 saisons", desktopSeasonCountLabel(meta))
        assertEquals("32 épisodes", desktopEpisodeCountLabel(meta))
        assertNull(desktopEpisodeCountLabel(MetaDetails("film", "movie", "Film")))
    }

    @Test fun profileStatsUseProgressWithoutDoubleCountingAndIgnoreLiveTv() {
        val now = 2_000_000_000L
        val film = WatchProgressEntry("movie", "film", "movie", "film", "Film", lastPositionMs = 600_000, durationMs = 600_000, lastUpdatedEpochMs = now, isCompleted = true)
        val episode = WatchProgressEntry("series", "partial", "series", "partial:1:2", "Série", seasonNumber = 1, episodeNumber = 2,
            lastPositionMs = 300_000, durationMs = 600_000, lastUpdatedEpochMs = now)
        val watched = listOf(
            WatchedItem("film", "movie", "Film", markedAtEpochMs = now),
            WatchedItem("film", "film", "Film", markedAtEpochMs = now),
            WatchedItem("partial", "series", "Épisode", season = 1, episode = 1, markedAtEpochMs = now),
            WatchedItem("complete", "series", "Série terminée", markedAtEpochMs = now),
            WatchedItem("https://example.test/live.m3u8", "livetv", "TV", markedAtEpochMs = now),
        )
        val library = listOf(LibraryItem("film", "movie", "Film", releaseInfo = "2025", savedAtEpochMs = 0),
            LibraryItem("future", "series", "À venir", releaseInfo = "2027-01-01", savedAtEpochMs = 0),
            LibraryItem("https://example.test/live.m3u8", "livetv", "TV", savedAtEpochMs = 0))
        val result = buildDesktopProfileStats(listOf(film, episode, episode,
            film.copy(contentType = "livetv", parentMetaType = "livetv", parentMetaId = "live")), watched,
            watchedItemKeys("series", "complete"), library, "2026-10-04", now)
        assertEquals(1, result.continueCount)
        assertEquals(2, result.completedCount)
        assertEquals(2, result.libraryCount)
        assertEquals(900_000L, result.trackedDurationMs)
        assertEquals(3, result.recentActivityCount)
        assertEquals(1, result.upcomingCount)
        assertEquals(DesktopProfileStats(), buildDesktopProfileStats(emptyList(), emptyList(), emptySet(), emptyList(), "2026-10-04", now))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileCardsUseDesktopWidthAndFrenchLabels() = runDesktopComposeUiTest(width = 1280, height = 720) {
        var switched = false
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface {
            Column(Modifier.size(1280.dp, 720.dp).padding(24.dp)) {
                DesktopProfileStatsContent("Raph", DesktopProfileStats(7, 138, 269, 61 * 3_600_000L, 3752, 30, listOf("Action", "Crime")), { switched = true })
            }
        } } }
        onNodeWithText("Le Nuvio de Raph").assertExists()
        onNodeWithText("61 h").assertExists()
        onNodeWithText("Terminés").assertExists()
        onNodeWithText("Cette semaine").assertExists()
        onNodeWithText("Action · Crime").assertExists()
        onNodeWithText("Changer de profil").performClick()
        runOnIdle { assertTrue(switched) }
        val output = File("build/test-artifacts/desktop-profile-stats.png")
        output.parentFile.mkdirs()
        output.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun bothCommunityTabsOnlyShowRaphAndFooterIsPersonalized() = runDesktopComposeUiTest(width = 1280, height = 720) {
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface {
            Column(Modifier.padding(24.dp)) { SpeedyCommunityCredits(); SettingsAttribution(true) }
        } } }
        onNodeWithText("raphdespeed").assertExists()
        onAllNodesWithText("fait par raph de speed").assertCountEquals(2)
        onNodeWithText("Supporters").performClick()
        onNodeWithText("raphdespeed").assertExists()
        onNodeWithText("Soutien du projet Nuvio Speedy").assertExists()
        onNodeWithText("Nuvio Supporter Membership").assertDoesNotExist()
    }
}
