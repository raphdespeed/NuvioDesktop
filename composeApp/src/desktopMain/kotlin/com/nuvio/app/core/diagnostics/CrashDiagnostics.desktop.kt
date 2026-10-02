package com.nuvio.app.core.diagnostics

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual object CrashDiagnostics {
    private val store=DesktopStorage.store("crash_diagnostics")
    private val pending=MutableStateFlow<LocalCrashReport?>(null)
    private val last=MutableStateFlow<LocalCrashReport?>(null)
    private var initialized=false
    actual val reportsSupported=true
    actual val pendingReport: StateFlow<LocalCrashReport?> = pending
    actual val lastReport: StateFlow<LocalCrashReport?> = last
    @Synchronized actual fun initialize(context: Any?) {
        if(initialized)return
        initialized=true
        store.getString("id")?.let { id ->
            val report=LocalCrashReport(id,store.getString("summary").orEmpty(),store.getString("details").orEmpty(),store.getString("context").orEmpty())
            last.value=report
            if(store.getString("dismissed")!=id)pending.value=report
        }
        val previous=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread,error ->
            runCatching {
                val report=LocalCrashReport(System.currentTimeMillis().toString(),error.message ?: error.javaClass.simpleName,error.stackTraceToString(),RuntimeDiagnostics.snapshotText())
                store.putString("id",report.id);store.putString("summary",report.summary)
                store.putString("details",report.details);store.putString("context",report.contextSummary)
                last.value=report;pending.value=report
            }
            if(previous!=null) previous.uncaughtException(thread,error) else error.printStackTrace()
        }
    }
    actual fun dismiss(reportId: String) { store.putString("dismissed",reportId);if(pending.value?.id==reportId)pending.value=null }
    actual fun currentReport(): String = buildString {
        appendLine("Nuvio Speedy — Windows");appendLine(RuntimeDiagnostics.snapshotText())
        last.value?.let { appendLine(it.summary);appendLine(it.details) }
    }
}
