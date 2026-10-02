package com.nuvio.app.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.beans.PropertyChangeListener

internal actual object AppForegroundMonitor {
    actual fun events(): Flow<Unit> = callbackFlow {
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val listener = PropertyChangeListener { event ->
            if (event.newValue is Window) trySend(Unit)
        }

        if (focusManager.activeWindow != null) trySend(Unit)
        focusManager.addPropertyChangeListener("activeWindow", listener)
        awaitClose {
            focusManager.removePropertyChangeListener("activeWindow", listener)
        }
    }
}
