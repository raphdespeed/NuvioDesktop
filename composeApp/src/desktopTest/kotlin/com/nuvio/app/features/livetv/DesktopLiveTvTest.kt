package com.nuvio.app.features.livetv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import androidx.compose.ui.graphics.asSkiaBitmap
import java.io.File

class DesktopLiveTvTest {
    private val first = LiveTvChannel("stream-id", "France 2", "https://example.test/france2.m3u8", tvgId = "france2.fr", group = "Actualités")
    private val second = LiveTvChannel("sport", "Sport PC", "https://example.test/sport.m3u8", group = "Sport")
    private val now = java.time.Instant.parse("2026-10-03T10:15:00Z").toEpochMilli()
    private val programme = LiveTvProgramme("Journal de démonstration", now - 15 * 60_000, now + 45 * 60_000, "12:00", description = "Le guide de la chaîne sélectionnée.")

    @OptIn(ExperimentalTestApi::class)
    @Test fun guideUsesXmlTvIdAndPlayUsesSelectedChannel() = runDesktopComposeUiTest(width = 1280, height = 720) {
        val compose = this
        var played: LiveTvChannel? = null
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface { Box(Modifier.size(1280.dp, 720.dp)) {
                    DesktopLiveTvContent(
                        LiveTvUiState(channels = listOf(first, second), programmesByChannel = mapOf("france2.fr" to listOf(programme))),
                        profileId = 1, now = now, onPlayChannel = { played = it },
                        onToggleFavorite = {}, onConfigureSource = {}, onRefresh = {})
                } }
            }
        }
        compose.onAllNodesWithText("Journal de démonstration").assertCountEquals(2)
        compose.onNodeWithText("GUIDE DES PROGRAMMES").assertIsDisplayed()
        compose.onNodeWithText("Regarder dans le lecteur PC").performClick()
        compose.runOnIdle { assertEquals(first, played) }
        val bitmap = compose.onRoot().captureToImage().asSkiaBitmap()
        val output = File("build/test-artifacts/desktop-live-tv.png")
        output.parentFile.mkdirs()
        output.writeBytes(Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        compose.onNodeWithText("Rechercher une chaîne").performTextInput("Sport PC")
        compose.onNodeWithText("Regarder dans le lecteur PC").performClick()
        compose.runOnIdle { assertEquals(second, played) }
    }

    @Test fun m3uPreservesProviderHeadersAndGuideIdentifiers() {
        val playlist = parseM3uPlaylistData("""
            #EXTM3U url-tvg="https://example.test/guide.xml"
            #EXTINF:-1 tvg-id="france2.fr" group-title="Actualités",France 2
            #EXTVLCOPT:http-user-agent=SpeedyTest
            https://example.test/france2.m3u8
        """.trimIndent())
        assertEquals(listOf("https://example.test/guide.xml"), playlist.epgUrls)
        assertEquals("france2.fr", playlist.channels.single().tvgId)
        assertEquals("SpeedyTest", playlist.channels.single().headers["User-Agent"])
        assertEquals("Actualités", playlist.channels.single().group)
    }

    @Test fun providerAliasesResolveWithoutReplacingPlaybackUrl() {
        val channels = resolveXmlTvChannelIds(listOf(first.copy(tvgId = null)), extractXmlTvChannelAliases("""
            <tv><channel id="france2.fr"><display-name>France 2</display-name></channel></tv>
        """.trimIndent()))
        assertEquals("france2.fr", channels.single().tvgId)
        assertEquals(first.streamUrl, channels.single().streamUrl)
        assertTrue(channels.single().id == first.id)
    }
    @Test fun compressedGuidesAreDecodedAndBounded() {
        val xml = "<tv><channel id=\"france2.fr\"/></tv>"
        val payload = ByteArrayOutputStream().apply {
            GZIPOutputStream(this).use { it.write(xml.toByteArray(Charsets.UTF_8)) }
        }.toByteArray()
        assertEquals(xml, decodeLiveTvEpgPayload("https://example.test/guide.xml.gz", payload))
        assertFailsWith<IllegalArgumentException> {
            decompressLiveTvEpgPayload(LiveTvEpgCompression.Gzip, payload, 8) {}
        }
    }

}
