package com.nuvio.app.features.downloads

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import java.io.File

internal actual object DownloadsExternalFolderPlatform {
    private val store=DesktopStorage.store("download_folders")
    private val mutableState=MutableStateFlow(DownloadExternalFolderState())
    actual val state: StateFlow<DownloadExternalFolderState> = mutableState
    init { onProfileChanged() }
    actual fun selectedFolderUri(): String? = store.getString(ProfileScopedKey.of("folder"))
    actual fun onProfileChanged() {
        val uri=selectedFolderUri()
        val file=uri?.let { runCatching { File(java.net.URI(it)) }.getOrNull() }
        mutableState.value=DownloadExternalFolderState(uri,file?.absolutePath,uri!=null && (file==null || !file.isDirectory || !file.canWrite()))
    }
    actual fun chooseFolder(onResult: (Result<String?>)->Unit) { SwingUtilities.invokeLater {
        val chooser=JFileChooser();chooser.fileSelectionMode=JFileChooser.DIRECTORIES_ONLY
        if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) onResult(runCatching {
            val folder=chooser.selectedFile;require(folder.isDirectory && folder.canWrite())
            val uri=folder.toURI().toString();store.putString(ProfileScopedKey.of("folder"),uri);onProfileChanged();uri
        }) else onResult(Result.success(null))
    } }
    actual fun clearFolder() { store.remove(ProfileScopedKey.of("folder"));onProfileChanged() }
    actual fun markUnavailable() { mutableState.value=mutableState.value.copy(unavailable=true) }
}

internal actual object PendingDownloadSourceSearchPlatform {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val jobs=java.util.concurrent.ConcurrentHashMap<String,Job>()
    actual fun schedule(search: PendingEpisodeDownload) {
        cancel(search.id)
        jobs[search.id]=scope.launch {
            delay((search.nextAttemptAtEpochMs-System.currentTimeMillis()).coerceAtLeast(0))
            EpisodeDownloadCoordinator.retryPending(search.id)
        }
    }
    actual fun cancel(searchId: String) { jobs.remove(searchId)?.cancel() }
    actual fun notifyManualChoice(searches: List<PendingEpisodeDownload>) {
        // The persisted pending choices are presented by the shared Downloads UI.
        if(searches.isNotEmpty()) java.awt.Toolkit.getDefaultToolkit().beep()
    }
}
