package com.nuvio.app.features.details.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nuvio.app.features.player.PlatformPlayerSurface
import com.nuvio.app.features.player.PlayerEngineController

@Composable
actual fun HeroTrailerPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    playWhenReady: Boolean,
    muted: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    seekRequest: HeroTrailerSeekRequest?,
    onPlaybackStateChanged: (HeroTrailerPlaybackSnapshot) -> Unit,
) {
    var controller by remember(sourceUrl) { mutableStateOf<PlayerEngineController?>(null) }
    var ready by remember(sourceUrl) { mutableStateOf(false) }
    LaunchedEffect(controller, muted) { controller?.setMuted(muted) }
    LaunchedEffect(controller, seekRequest?.id) { seekRequest?.let { controller?.seekTo(it.positionMs) } }
    PlatformPlayerSurface(
        sourceUrl = sourceUrl,
        sourceAudioUrl = sourceAudioUrl,
        playWhenReady = playWhenReady,
        modifier = modifier,
        playerControlsState = com.nuvio.app.features.player.PlayerControlsState(controlsVisible = false),
        onControllerReady = { controller = it; it.setMuted(muted) },
        onSnapshot = { snapshot ->
            if (!ready && !snapshot.isLoading) { ready = true; onReady() }
            if (snapshot.isEnded) onEnded()
            onPlaybackStateChanged(HeroTrailerPlaybackSnapshot(snapshot.positionMs,snapshot.durationMs,snapshot.isPlaying))
        },
        onError = { if (it != null) onError() },
    )
}
