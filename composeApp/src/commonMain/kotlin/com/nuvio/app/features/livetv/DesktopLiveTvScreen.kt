package com.nuvio.app.features.livetv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
    val profiles by ProfileRepository.state.collectAsState()
    val profileId = remember(profiles) { resolveLiveTvStorageProfileId() }
    val sources by desktopM3uSourceStore.state.collectAsState()
    var loadedProfile by remember { mutableStateOf<Int?>(null) }
    var editSource by remember(profileId) { mutableStateOf(false) }
    var editingSource by remember(profileId) { mutableStateOf<DesktopM3uSource?>(null) }
    var deletingSource by remember(profileId) { mutableStateOf<DesktopM3uSource?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var now by remember { mutableStateOf(LiveTvClock.nowEpochMs()) }
    LaunchedEffect(profileId) {
        loadedProfile = null
        LiveTvRepository.onProfileChanged()
        val stored = LiveTvRepository.uiState.value
        desktopM3uSourceStore.load(profileId, stored.sourceType, stored.sourceUrl, LiveTvStorage.loadLocalPlaylistData().orEmpty())
        loadedProfile = profileId
    }
    LaunchedEffect(profileId, loadedProfile, sources.active, reload) {
        if (loadedProfile != profileId) return@LaunchedEffect
        LiveTvRepository.onProfileChanged()
        val source = sources.active
        if (source != null) {
            if (source.playlistData.isNotBlank()) LiveTvRepository.loadLocalPlaylist(source.fileName.ifBlank { source.name }, source.playlistData)
            else LiveTvRepository.load(source.url)
        } else {
            val stored = LiveTvRepository.uiState.value
            when (stored.sourceType) {
                LiveTvSourceType.M3u -> Unit
                LiveTvSourceType.Xtream -> if (stored.xtreamSettings.isConfigured) LiveTvRepository.loadXtream(stored.xtreamSettings)
                LiveTvSourceType.Stalker -> if (stored.stalkerSettings.isConfigured) LiveTvRepository.loadStalker(stored.stalkerSettings)
            }
        }
    }
    DisposableEffect(Unit) {
        LiveTvRepository.setEpgLoadingActive(true)
        onDispose { LiveTvRepository.setEpgLoadingActive(false) }
    }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LiveTvClock.nowEpochMs() } }
    DesktopLiveTvContent(state, profileId, now, onPlayChannel,
        onToggleFavorite = LiveTvRepository::toggleFavorite,
        onConfigureSource = { editingSource = null; editSource = true },
        onRefresh = { reload++ }, modifier = modifier,
        sourceLists = sources.sources, activeSourceId = sources.activeId,
        onSelectSource = { desktopM3uSourceStore.select(it.id) },
        onEditSource = { editingSource = it; editSource = true },
        onDeleteSource = { deletingSource = it },
    )
    if (editSource) TvSourceDialog(state, editingSource, onDismiss = { editSource = false },
        onSaveM3u = { source -> desktopM3uSourceStore.save(source); editSource = false },
        onProviderSaved = { desktopM3uSourceStore.select(null); reload++; editSource = false })
    deletingSource?.let { source ->
        AlertDialog(onDismissRequest = { deletingSource = null }, title = { Text("Supprimer la liste M3U ?") },
            text = { Text("La liste « ${source.name} » sera retirée. Les autres listes seront conservées.") },
            confirmButton = { TextButton(onClick = {
                if (sources.activeId == source.id && sources.sources.size == 1) LiveTvRepository.disconnect()
                desktopM3uSourceStore.remove(source.id)
                deletingSource = null
            }) { Text("Supprimer") } }, dismissButton = { TextButton(onClick = { deletingSource = null }) { Text("Annuler") } })
    }

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
    sourceLists: List<DesktopM3uSource> = emptyList(),
    activeSourceId: String? = null,
    onSelectSource: (DesktopM3uSource) -> Unit = {},
    onEditSource: (DesktopM3uSource) -> Unit = {},
    onDeleteSource: (DesktopM3uSource) -> Unit = {},
) {
    var query by rememberSaveable(profileId, activeSourceId) { mutableStateOf("") }
    var category by rememberSaveable(profileId, activeSourceId) { mutableStateOf("") }
    var favoritesOnly by rememberSaveable(profileId, activeSourceId) { mutableStateOf(false) }
    var selectedId by rememberSaveable(profileId, activeSourceId) { mutableStateOf<String?>(null) }
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
            Button(onClick = onConfigureSource) { Icon(Icons.Rounded.Settings, null); Spacer(Modifier.width(8.dp)); Text("Ajouter une source") }
        }
        if (sourceLists.isNotEmpty()) {
            DesktopM3uSourceSelector(sourceLists, activeSourceId, onSelectSource, onEditSource, onDeleteSource)
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
internal fun DesktopM3uSourceSelector(
    sources: List<DesktopM3uSource>, activeId: String?,
    onSelect: (DesktopM3uSource) -> Unit, onEdit: (DesktopM3uSource) -> Unit,
    onDelete: (DesktopM3uSource) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("MES LISTES M3U", style = MaterialTheme.typography.labelLarge)
        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sources, key = { it.id }) { source ->
                FilterChip(source.id == activeId, { onSelect(source) }, label = { Text(source.name) })
            }
        }
        sources.firstOrNull { it.id == activeId }?.let { source ->
            OutlinedButton(onClick = { onEdit(source) }) { Text("Modifier") }
            OutlinedButton(onClick = { onDelete(source) }) { Text("Supprimer") }
        }
    }
}

@Composable
private fun TvSourceDialog(
    state: LiveTvUiState, editing: DesktopM3uSource?, onDismiss: () -> Unit,
    onSaveM3u: (DesktopM3uSource) -> Unit, onProviderSaved: () -> Unit,
) {
    val sourceId = remember { editing?.id ?: newDesktopM3uSourceId() }
    var type by remember { mutableStateOf(LiveTvSourceType.M3u) }
    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var url by remember { mutableStateOf(editing?.url.orEmpty()) }
    var fileName by remember { mutableStateOf(editing?.fileName.orEmpty()) }
    var playlistData by remember { mutableStateOf(editing?.playlistData.orEmpty()) }
    var portal by remember { mutableStateOf(state.stalkerSettings.portalUrl) }
    var mac by remember { mutableStateOf(state.stalkerSettings.macAddress) }
    var server by remember { mutableStateOf(state.xtreamSettings.serverUrl) }
    var username by remember { mutableStateOf(state.xtreamSettings.username) }
    var password by remember { mutableStateOf(state.xtreamSettings.password) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(Modifier.width(680.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (editing == null) "Ajouter une source TV" else "Modifier la liste M3U", style = MaterialTheme.typography.headlineSmall)
                if (editing == null) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nom de la liste M3U") }, singleLine = true)
                        OutlinedTextField(url, { url = it; playlistData = ""; fileName = "" }, Modifier.fillMaxWidth(), label = { Text("Adresse de la liste M3U") }, singleLine = true)
                        if (fileName.isNotBlank()) Text("Fichier : $fileName")
                        OutlinedButton(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    pickDesktopLiveTvPlaylist()?.let { selected ->
                                        fileName = selected.first; playlistData = selected.second; url = ""
                                        if (name.isBlank()) name = fileName
                                    }
                                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                catch (_: Exception) { failure = "Impossible d’ouvrir ce fichier." }
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
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                }
                failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(enabled = !busy, onClick = onDismiss) { Text("Annuler") }
                    Button(enabled = !busy, onClick = {
                        try {
                            when(type) {
                                LiveTvSourceType.M3u -> onSaveM3u(DesktopM3uSource(sourceId, name.trim(), url.trim(), fileName, playlistData))
                                LiveTvSourceType.Xtream -> {
                                    val settings = LiveTvXtreamSettings(server.trim(), username.trim(), password)
                                    require(settings.isConfigured) { "Renseignez le serveur, le nom d’utilisateur et le mot de passe." }
                                    LiveTvStorage.saveXtreamSettings(settings); LiveTvStorage.saveSourceType(type); onProviderSaved()
                                }
                                LiveTvSourceType.Stalker -> {
                                    val settings = LiveTvStalkerSettings(portal.trim(), mac.trim(), username.trim(), password)
                                    require(settings.isConfigured) { "Renseignez le portail et l’adresse MAC." }
                                    LiveTvStorage.saveStalkerSettings(settings); LiveTvStorage.saveSourceType(type); onProviderSaved()
                                }
                            }
                        } catch (error: IllegalArgumentException) { failure = error.message }
                    }) { Text("Enregistrer") }
                }
            }
        }
    }
}

internal expect suspend fun pickDesktopLiveTvPlaylist(): Pair<String, String>?
internal expect suspend fun fetchLiveTvEpgBytes(url: String, headers: Map<String, String>, maxResponseBodyBytes: Int): ByteArray
