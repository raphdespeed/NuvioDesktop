package com.nuvio.app.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import com.nuvio.app.core.ui.appTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun AppBrandWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    icon: AppIconOption? = null,
) {
    val state by remember {
        AppIconRepository.ensureLoaded()
        AppIconRepository.state
    }.collectAsStateWithLifecycle()
    val painter = painterResource(
        icon?.wordmarkResource ?: MaterialTheme.appTheme.wordmarkResource(state.selected),
    )
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val height = if (constraints.hasBoundedHeight) maxHeight else 44.dp
        val ratio = (painter.intrinsicSize.width / painter.intrinsicSize.height).takeIf { it.isFinite() && it > 0f } ?: 3.2f
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()) {
            Image(painter = painter, contentDescription = contentDescription,
                modifier = Modifier.height(height).width(height * ratio), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(height * 0.18f))
            Text("Speedy", color = MaterialTheme.colorScheme.onBackground,
                fontSize = (height.value * 0.42f).sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, softWrap = false)
        }
    }
}
