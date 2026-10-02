package com.nuvio.app.features.player
import java.nio.file.Files
internal actual fun writeTemporaryHlsPlaylist(playlistText: String): String? = runCatching {
 val path=Files.createTempFile("nuvio-speedy-", ".m3u8");path.toFile().deleteOnExit();Files.writeString(path,playlistText);path.toUri().toString()
}.getOrNull()
