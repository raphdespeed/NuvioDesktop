package com.nuvio.app.features.player

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.net.NetworkInterface

@Composable
internal actual fun rememberPlayerDeviceStatus(): PlayerDeviceStatus {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { delay(1000);now=System.currentTimeMillis() } }
    val connected=remember(now/30000) { runCatching {
        NetworkInterface.getNetworkInterfaces().toList().any { it.isUp && !it.isLoopback }
    }.getOrDefault(false) }
    return PlayerDeviceStatus(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),now,null,false,
        if(connected) PlayerDeviceNetworkType.Unknown else PlayerDeviceNetworkType.Offline)
}
