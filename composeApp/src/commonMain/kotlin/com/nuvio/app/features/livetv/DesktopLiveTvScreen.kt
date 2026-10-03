package com.nuvio.app.features.livetv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Desktop-only presentation: the existing Nuvio shell stays in charge of navigation. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopLiveTvScreen(
    onPlayChannel: (LiveTvChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by LiveTvRepository.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val profileId = ProfileRepository.activeProfileId
    var editSource by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LiveTvClock.nowEpochMs()) }
    LaunchedEffect(profileId) {
        LiveTvRepository.onProfileChanged()
        val stored = LiveTvRepository.uiState.value
        when (stored.sourceType) {
            LiveTvSourceType.M3u -> if (stored.sourceUrl.isNotBlank()) {
                if (stored.sourceUrl.startsWith("http")) LiveTvRepository.load(stored.sourceUrl)
                else LiveTvRepository.loadStoredLocalPlaylist()
            }
            LiveTvSourceType.Xtream -> if (stored.xtreamSettings.isConfigured) LiveTvRepository.loadXtream(stored.xtreamSettings)
            LiveTvSourceType.Stalker -> if (stored.stalkerSettings.isConfigured) LiveTvRepository.loadStalker(stored.stalkerSettings)
        }
    }
    DisposableEffect(Unit) {
        LiveTvRepository.setEpgLoadingActive(true)
        onDispose { LiveTvRepository.setEpgLoadingActive(false) }
    }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LiveTvClock.nowEpochMs() } }
    DesktopLiveTvContent(state, profileId, now, onPlayChannel,
        onToggleFavorite = LiveTvRepository::toggleFavorite,
        onConfigureSource = { editSource = true },
        onRefresh = { scope.launch {
            when (state.sourceType) {
                LiveTvSourceType.M3u -> if (state.sourceUrl.startsWith("http")) LiveTvRepository.load(state.sourceUrl) else LiveTvRepository.loadStoredLocalPlaylist()
                LiveTvSourceType.Xtream -> LiveTvRepository.loadXtream(state.xtreamSettings)
                LiveTvSourceType.Stalker -> LiveTvRepository.loadStalker(state.stalkerSettings)
            }
        } }, modifier = modifier)
    if (editSource) TvSourceDialog(state, onDismiss = { editSource = false })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopLiveTvContent(
    state: LiveTvUiState,
    profileId: Int,
    now: Long,
    onPlayChannel: (LiveTvChannel) -> Unit,
    onToggleFavorite: (LiveTvChannel) -> Unit,
    onConfigureSource: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable(profileId) { mutableStateOf("") }
    var category by rememberSaveable(profileId) { mutableStateOf("") }
    var favoritesOnly by rememberSaveable(profileId) { mutableStateOf(false) }
    var selectedId by rememberSaveable(profileId) { mutableStateOf<String?>(null) }
    val categories = remember(state.channels) { state.channels.map { it.group }.filter { it.isNotBlank() }.distinct().sorted() }
    val filtered = remember(state.channels, state.favoriteUrls, category, favoritesOnly, query) {
        state.channels.filter { channel ->
            (category.isEmpty() || channel.group == category) &&
                (!favoritesOnly || channel.streamUrl in state.favoriteUrls) &&
                (query.isBlank() || channel.name.contains(query, true) || channel.group.contains(query, true))
        }
    }
    val selected = filtered.firstOrNull { it.id == selectedId }
        ?: filtered.firstOrNull { it.streamUrl == state.recentChannel?.streamUrl }
        ?: filtered.firstOrNull()
    val programmes = selected?.let { state.programmesByChannel[it.tvgId] }.orEmpty()
        .filter { it.stopEpochMs > now }.sortedBy { it.startEpochMs }
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("TV en direct", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Vos chaînes, favoris et programmes", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.isLoading || state.isEpgLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            OutlinedButton(onClick = onRefresh, enabled = !state.isLoading && state.channels.isNotEmpty()) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Actualiser") }
            Button(onClick = onConfigureSource) { Icon(Icons.Rounded.Settings, null); Spacer(Modifier.width(8.dp)); Text("Configurer la source") }
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.channels.isEmpty()) {
            Surface(Modifier.fillMaxSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Tv, null, Modifier.size(64.dp))
                    Spacer(Modifier.height(18.dp))
                    Text("Ajoutez vos chaînes", style = MaterialTheme.typography.headlineSmall)
                    Text("Importez une liste M3U ou connectez votre fournisseur Xtream ou Stalker.")
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onConfigureSource) { Text("Ajouter une source TV") }
                }
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(Modifier.width(190.dp).fillMaxHeight(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item { Text("CATÉGORIES", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp)) }
                        item { TvCategory("Toutes les chaînes", !favoritesOnly && category.isEmpty()) { category = ""; favoritesOnly = false } }
                        item { TvCategory("Favoris", favoritesOnly) { category = ""; favoritesOnly = true } }
                        items(categories, key = { it }) { group -> TvCategory(group, !favoritesOnly && category == group) { category = group; favoritesOnly = false } }
                    }
                }
                Column(Modifier.width(310.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Rechercher une chaîne") }, leadingIcon = { Icon(Icons.Rounded.Search, null) })
                    Text("${filtered.size} chaînes", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filtered, key = { it.id }) { channel ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    onClick = { selectedId = channel.id }, onDoubleClick = { onPlayChannel(channel) }),
                                shape = MaterialTheme.shapes.medium,
                                color = if (channel.id == selected?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                        val current = state.programmesByChannel[channel.tvgId]?.firstOrNull { now in it.startEpochMs until it.stopEpochMs }
                                        Text(current?.title ?: channel.group, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onToggleFavorite(channel) }) {
                                        Icon(if (channel.streamUrl in state.favoriteUrls) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                            contentDescription = "Ajouter ou retirer des favoris", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        if (filtered.isEmpty()) item { Text("Aucune chaîne ne correspond à votre recherche.", Modifier.padding(12.dp)) }
                    }
                }
                Surface(Modifier.weight(1f).fillMaxHeight(), shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (selected != null) {
                            Text(selected.name, style = MaterialTheme.typography.headlineSmall)
                            Text(selected.group, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { onPlayChannel(selected) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Regarder dans le lecteur PC")
                            }
                            HorizontalDivider()
                            Text("GUIDE DES PROGRAMMES", style = MaterialTheme.typography.labelLarge)
                            if (programmes.isEmpty()) Text(if (state.isEpgLoading) "Chargement du guide…" else "Aucun programme disponible pour cette chaîne.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(programmes.take(60), key = { "${it.startEpochMs}:${it.title}" }) { programme ->
                                    val current = now in programme.startEpochMs until programme.stopEpochMs
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${LiveTvClock.formatLocalTime(programme.startEpochMs)} – ${LiveTvClock.formatLocalTime(programme.stopEpochMs)}${if (current) "  •  EN DIRECT" else ""}",
                                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(programme.title, fontWeight = FontWeight.SemiBold)
                                        programme.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 5, overflow = TextOverflow.Ellipsis) }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        } else Text("Sélectionnez une chaîne pour afficher ses programmes.")
                    }
                }
            }
        }
    }
}

@Composable
private fun TvCategory(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick, Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
        Text(label, Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TvSourceDialog(state: LiveTvUiState, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(state.sourceType) }
    var url by remember { mutableStateOf(state.sourceUrl.takeIf { it.startsWith("http") }.orEmpty()) }
    var portal by remember { mutableStateOf(state.stalkerSettings.portalUrl) }
    var mac by remember { mutableStateOf(state.stalkerSettings.macAddress) }
    var server by remember { mutableStateOf(state.xtreamSettings.serverUrl) }
    var username by remember { mutableStateOf(if (type == LiveTvSourceType.Stalker) state.stalkerSettings.username else state.xtreamSettings.username) }
    var password by remember { mutableStateOf(if (type == LiveTvSourceType.Stalker) state.stalkerSettings.password else state.xtreamSettings.password) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(Modifier.width(680.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Configurer la TV en direct", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LiveTvSourceType.entries.forEach { value ->
                        FilterChip(selected = type == value, enabled = !busy, onClick = {
                            type = value
                            username = if (value == LiveTvSourceType.Stalker) state.stalkerSettings.username else state.xtreamSettings.username
                            password = if (value == LiveTvSourceType.Stalker) state.stalkerSettings.password else state.xtreamSettings.password
                            failure = null
                        }, label = { Text(when(value) { LiveTvSourceType.M3u -> "M3U"; LiveTvSourceType.Xtream -> "Xtream"; LiveTvSourceType.Stalker -> "Stalker" }) })
                    }
                }
                when(type) {
                    LiveTvSourceType.M3u -> {
                        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Adresse de la liste M3U") }, singleLine = true)
                        OutlinedButton(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val selected = pickDesktopLiveTvPlaylist()
                                    if (selected != null) {
                                        val result = LiveTvRepository.loadLocalPlaylist(selected.first, selected.second)
                                        if (result.isSuccess) onDismiss() else failure = result.exceptionOrNull()?.message
                                    }
                                } catch (error: Exception) { failure = error.message ?: "Impossible d’ouvrir ce fichier." }
                                finally { busy = false }
                            }
                        }) { Text("Importer un fichier M3U depuis le PC") }
                    }
                    LiveTvSourceType.Xtream -> OutlinedTextField(server, { server = it }, Modifier.fillMaxWidth(), label = { Text("Adresse du serveur Xtream") }, singleLine = true)
                    LiveTvSourceType.Stalker -> {
                        OutlinedTextField(portal, { portal = it }, Modifier.fillMaxWidth(), label = { Text("Adresse du portail Stalker") }, singleLine = true)
                        OutlinedTextField(mac, { mac = it }, Modifier.fillMaxWidth(), label = { Text("Adresse MAC") }, singleLine = true)
                    }
                }
                if (type != LiveTvSourceType.M3u) {
                    OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Nom d’utilisateur") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Mot de passe") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation())
                }
                failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                    TextButton(enabled = !busy, onClick = onDismiss) { Text("Annuler") }
                    Spacer(Modifier.width(12.dp))
                    Button(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true
                            val result = when(type) {
                                LiveTvSourceType.M3u -> LiveTvRepository.load(url)
                                LiveTvSourceType.Xtream -> LiveTvRepository.loadXtream(LiveTvXtreamSettings(server, username, password))
                                LiveTvSourceType.Stalker -> LiveTvRepository.loadStalker(LiveTvStalkerSettings(portal, mac, username, password))
                            }
                            busy = false
                            if (result.isSuccess) onDismiss() else failure = result.exceptionOrNull()?.message
                        }
                    }) { Text("Charger les chaînes") }
                }
            }
        }
    }
}

internal expect suspend fun pickDesktopLiveTvPlaylist(): Pair<String, String>?
internal expect suspend fun fetchLiveTvEpgBytes(url: String, headers: Map<String, String>, maxResponseBodyBytes: Int): ByteArray
