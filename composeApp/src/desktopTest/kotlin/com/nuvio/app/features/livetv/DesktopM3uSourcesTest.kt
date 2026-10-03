package com.nuvio.app.features.livetv

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import java.io.File
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import kotlin.test.*

class DesktopM3uSourcesTest {
    private val france = DesktopM3uSource("fr", "France", "https://example.test/france.m3u")
    private val sport = DesktopM3uSource("sport", "Sport", "https://example.test/sport.m3u")

    @Test fun savedSourcesRemainSeparateAcrossEditsDeletesAndRestart() {
        val storage = mutableMapOf<Int, String>()
        val store = DesktopM3uSourceStore({ storage[it] }, { profile, data -> storage[profile] = data })
        store.load(1, LiveTvSourceType.M3u, "https://example.test/old.m3u", "")
        assertEquals("https://example.test/old.m3u", store.state.value.active?.url)
        store.save(france); store.save(sport)
        assertEquals(3, store.state.value.sources.size)
        assertEquals(sport, store.state.value.active)
        store.select(france.id)
        store.save(france.copy(name = "Chaînes françaises"))
        assertEquals(3, store.state.value.sources.size)
        assertEquals(sport, store.state.value.sources.last())
        val restarted = DesktopM3uSourceStore({ storage[it] }, { profile, data -> storage[profile] = data })
        restarted.load(1, LiveTvSourceType.M3u, sport.url, "")
        assertEquals("Chaînes françaises", restarted.state.value.active?.name)
        restarted.remove(france.id)
        assertEquals(2, restarted.state.value.sources.size)
        assertEquals("legacy-m3u", restarted.state.value.activeId)
        restarted.load(2, LiveTvSourceType.M3u, "", "")
        assertTrue(restarted.state.value.sources.isEmpty())
        restarted.load(1, LiveTvSourceType.M3u, "", "")
        assertEquals(2, restarted.state.value.sources.size)
    }

    @Test fun localPlaylistIsMigratedAndProvidersAreNotMistakenForM3u() {
        val storage = mutableMapOf<Int, String>()
        val store = DesktopM3uSourceStore({ storage[it] }, { profile, data -> storage[profile] = data })
        val data = "#EXTM3U\n#EXTINF:-1,Chaîne\nhttps://example.test/live.m3u8"
        store.load(1, LiveTvSourceType.M3u, "playlist.m3u", data)
        assertEquals(data, store.state.value.active?.playlistData)
        assertEquals("playlist.m3u", store.state.value.active?.fileName)
        store.remove("legacy-m3u")
        store.load(1, LiveTvSourceType.M3u, "playlist.m3u", data)
        assertTrue(store.state.value.sources.isEmpty())
        store.load(2, LiveTvSourceType.Xtream, "https://provider.test", "")
        assertTrue(store.state.value.sources.isEmpty())
        assertFailsWith<IllegalArgumentException> { store.save(france.copy(name = "")) }
        assertFailsWith<IllegalArgumentException> { store.save(france.copy(url = "ftp://invalid")) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun selectingAnotherListReplacesChannelsAndGuideAndResetsSearch() = runDesktopComposeUiTest(width = 1280, height = 720) {
        var active by mutableStateOf(france)
        var played: LiveTvChannel? = null
        val channelFr = LiveTvChannel("same-provider-id", "France 2", "https://example.test/fr.m3u8", tvgId = "same.xml", group = "Actualités")
        val channelSport = LiveTvChannel("same-provider-id", "Sport PC", "https://example.test/sport.m3u8", tvgId = "same.xml", group = "Sports")
        val now = 1_000_000L
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface {
            val channel = if (active == france) channelFr else channelSport
            val programme = LiveTvProgramme(if (active == france) "Journal France" else "Match Sport", now - 1000, now + 60000, "12:00")
            DesktopLiveTvContent(LiveTvUiState(channels = listOf(channel), programmesByChannel = mapOf("same.xml" to listOf(programme))),
                1, now, { played = it }, {}, {}, {}, Modifier.size(1280.dp, 720.dp),
                sourceLists = listOf(france, sport), activeSourceId = active.id, onSelectSource = { active = it })
        } } }
        onAllNodesWithText("Actualités")[0].performClick()
        onNodeWithText("Rechercher une chaîne").performTextInput("France")
        onNodeWithText("Sport").performClick()
        onAllNodesWithText("Sport PC").assertCountEquals(2)
        onAllNodesWithText("Journal France").assertCountEquals(0)
        onAllNodesWithText("Match Sport").assertCountEquals(2)
        onNodeWithText("Regarder dans le lecteur PC").performClick()
        runOnIdle { assertEquals(channelSport, played) }
        val output = File("build/test-artifacts/desktop-m3u-sources.png")
        output.parentFile.mkdirs()
        output.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        onNodeWithText("France").performClick()
        onAllNodesWithText("France 2").assertCountEquals(2)
        onAllNodesWithText("Match Sport").assertCountEquals(0)
    }
}
