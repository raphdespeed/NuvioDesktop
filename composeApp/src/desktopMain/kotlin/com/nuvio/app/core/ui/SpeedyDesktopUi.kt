package com.nuvio.app.core.ui
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
internal actual object AppSystemUiController { actual fun setStatusBarVisible(visible: Boolean) {} }
internal actual fun nuvioMediaActionOverlayDialogProperties(): DialogProperties = DialogProperties(usePlatformDefaultWidth=false)
@Composable internal actual fun isKeyboardNavigationAvailable(): Boolean = true
@Composable actual fun isTvLayoutProfileEnabled(): Boolean = false
