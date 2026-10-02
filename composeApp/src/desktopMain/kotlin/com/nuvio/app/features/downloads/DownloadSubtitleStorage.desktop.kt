package com.nuvio.app.features.downloads

import java.io.File
import java.net.URI

internal class DownloadSubtitleStorage constructor(localVideoUri: String) {
    private val directory = File(File(URI(localVideoUri)).path + ".subtitles")

    fun read(fileName: String): String? =
        runCatching { file(fileName).takeIf(File::isFile)?.readText(Charsets.UTF_8) }.getOrNull()

    fun write(fileName: String, text: String) {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create subtitle directory" }
        file(fileName).writeText(text, Charsets.UTF_8)
    }

    fun localFileUri(fileName: String): String? =
        file(fileName).takeIf(File::isFile)?.toURI()?.toString()

    fun remove() {
        directory.deleteRecursively()
    }

    private fun file(fileName: String): File {
        require(fileName.isNotBlank() && File(fileName).name == fileName && fileName != "." && fileName != "..")
        return File(directory, fileName)
    }
}
