package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader

@Composable
fun SupportersContributorsSettingsScreen(onBack: () -> Unit) {
    NuvioScreen(modifier = Modifier.fillMaxSize()) {
        stickyHeader { NuvioScreenHeader(title = "Supporters et contributeurs", onBack = onBack) }
        supportersContributorsContent(isTablet = false)
    }
}

internal fun LazyListScope.supportersContributorsContent(isTablet: Boolean) {
    item { SpeedyCommunityCredits() }
}

@Composable
internal fun SpeedyCommunityCredits() {
    var supporters by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(!supporters, { supporters = false }, label = { Text("Contributeurs") })
            FilterChip(supporters, { supporters = true }, label = { Text("Supporters") })
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("R", Modifier.padding(20.dp), style = MaterialTheme.typography.headlineLarge)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("raph de speed", style = MaterialTheme.typography.headlineSmall)
                    Text("raphdespeed", style = MaterialTheme.typography.titleMedium)
                    Text(if (supporters) "Soutien du projet Nuvio Speedy" else "Développement et adaptations de Nuvio Speedy")
                }
            }
        }
        Text("fait par raph de speed", style = MaterialTheme.typography.bodyMedium)
    }
}
