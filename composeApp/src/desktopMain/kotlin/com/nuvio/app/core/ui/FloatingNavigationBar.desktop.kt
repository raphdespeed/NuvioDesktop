package com.nuvio.app.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import org.jetbrains.compose.resources.painterResource
import com.nuvio.app.features.settings.NavBarStyle
import dev.chrisbanes.haze.HazeState

internal actual val floatingNavigationGlowSupported: Boolean = false

@Composable
internal actual fun FloatingNavigationBar(
    items: List<FloatingNavigationItem>,
    modifier: Modifier,
    scrollState: NuvioNavBarScrollState?,
    hazeState: HazeState?,
    contentPadding: PaddingValues,
    compactSize: Boolean,
    glowEnabled: Boolean,
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = {
                    when {
                        item.content != null -> item.content.invoke(item.onClick)
                        item.icon != null -> Icon(item.icon, item.label)
                        item.drawable != null -> Icon(painterResource(item.drawable), item.label)
                        else -> Text(item.label.take(1))
                    }
                },
                label = if (compactSize) null else { { Text(item.label) } },
            )
        }
    }
}
