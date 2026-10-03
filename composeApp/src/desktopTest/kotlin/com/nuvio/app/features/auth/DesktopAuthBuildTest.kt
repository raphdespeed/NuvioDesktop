package com.nuvio.app.features.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.settings.AppBrandWordmark
import com.nuvio.app.features.settings.MemberBrandWordmark
import com.nuvio.app.core.network.SupabaseConfig
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopAuthBuildTest {
    @OptIn(ExperimentalTestApi::class)
    @Test fun speedyNameIsVisibleInLargeAndMemberWordmarks() = runDesktopComposeUiTest(width = 900, height = 300) {
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface { Column(Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    AppBrandWordmark(modifier = Modifier.height(80.dp), contentDescription = "Nuvio Speedy")
                    MemberBrandWordmark(height = 44.dp, contentDescription = "Nuvio Speedy")
                } }
            }
        }
        onAllNodesWithText("Speedy").assertCountEquals(2)
        onNodeWithText("Supporter").assertDoesNotExist()
        val output = File("build/test-artifacts/desktop-speedy-brand.png")
        output.parentFile.mkdirs()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", output)
    }

    @Test fun packagedPublicBackendAcceptsEmailAuthentication() {
        assumeTrue("Opt-in network check", System.getenv("SPEEDY_VERIFY_AUTH_BACKEND") == "1")
        assertTrue(SupabaseConfig.URL.startsWith("https://"), "The installer needs its account backend URL")
        assertTrue(SupabaseConfig.ANON_KEY.isNotBlank(), "The installer needs its public account key")
        val request = HttpRequest.newBuilder(URI.create(SupabaseConfig.URL.trimEnd('/') + "/auth/v1/settings"))
            .timeout(Duration.ofSeconds(25))
            .header("apikey", SupabaseConfig.ANON_KEY)
            .header("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
            .header("User-Agent", "NuvioSpeedyDesktop/build-verification")
            .GET().build()
        val response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(25)).build()
            .send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode(), "The public account configuration must be accepted")
        val settings = Json.parseToJsonElement(response.body()).jsonObject
        assertTrue(settings["external"]?.jsonObject?.get("email")?.jsonPrimitive?.booleanOrNull == true,
            "Email authentication must be available on the configured backend")
    }
}
