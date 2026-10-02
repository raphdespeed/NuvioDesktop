package com.nuvio.app.features.settings
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import java.io.File
internal actual object NuvioSpeedyBackupFileBridge {
 actual fun exportBackup(fileName: String,payload: String,onResult: (Result<String>)->Unit) { SwingUtilities.invokeLater {
  val chooser=JFileChooser(); chooser.selectedFile=File(fileName)
  if(chooser.showSaveDialog(null)==JFileChooser.APPROVE_OPTION) onResult(runCatching { chooser.selectedFile.writeText(payload);chooser.selectedFile.absolutePath })
  else onResult(Result.failure(IllegalStateException("Export annulé")))
 } }
 actual fun importBackup(onResult: (Result<String>)->Unit) { SwingUtilities.invokeLater {
  val chooser=JFileChooser()
  if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) onResult(runCatching { chooser.selectedFile.readText() })
  else onResult(Result.failure(IllegalStateException("Import annulé")))
 } }
}
