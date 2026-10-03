package com.nuvio.app.features.livetv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual suspend fun pickDesktopLiveTvPlaylist(): Pair<String, String>? {
    val file = suspendCancellableCoroutine<java.io.File?> { continuation ->
        SwingUtilities.invokeLater {
            if (continuation.isActive) {
                try {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Importer une liste M3U"
                        fileFilter = FileNameExtensionFilter("Listes M3U", "m3u", "m3u8")
                    }
                    val selected = if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
                    if (continuation.isActive) continuation.resume(selected)
                } catch (error: Exception) { if (continuation.isActive) continuation.resumeWithException(error) }
            }
        }
    } ?: return null
    return withContext(Dispatchers.IO) {
        require(file.length() <= 32L * 1024 * 1024) { "La liste M3U dépasse 32 Mo." }
        file.name to file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
    }
}

private val epgHttpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS).build()

internal actual suspend fun fetchLiveTvEpgBytes(url: String, headers: Map<String, String>, maxResponseBodyBytes: Int): ByteArray = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url).apply { headers.forEach { (name, value) -> header(name, value) } }.build()
    epgHttpClient.newCall(request).execute().use { response ->
        check(response.isSuccessful) { "Chargement du guide impossible (HTTP ${response.code})." }
        val body = response.body ?: error("Le guide reçu est vide.")
        body.byteStream().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                check(output.size().toLong() + count <= maxResponseBodyBytes.toLong()) { "Le guide dépasse la taille autorisée." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }
}
