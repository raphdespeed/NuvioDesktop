package com.nuvio.app.features.player
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import com.nuvio.app.core.storage.DesktopStorage
internal actual object SubtitleFontFileBridge {
 actual fun importFont(onResult: (Result<SubtitleFontImportResult>)->Unit) { SwingUtilities.invokeLater {
  val chooser=JFileChooser();chooser.fileFilter=FileNameExtensionFilter("Polices","ttf","otf")
  if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) onResult(runCatching {
   val source=chooser.selectedFile;require(source.extension.lowercase() in setOf("ttf","otf"))
   val target=DesktopStorage.rootDir.resolve("fonts").resolve(source.name).toFile();target.parentFile.mkdirs();source.copyTo(target,true)
   SubtitleFontImportResult(source.nameWithoutExtension,target.absolutePath)
  }) else onResult(Result.failure(IllegalStateException("Import annulé")))
 } }
}
