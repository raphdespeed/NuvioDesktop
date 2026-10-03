package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watched.WatchedClock
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.CurrentDateProvider

@Composable
internal fun DesktopProfileStatsPanel(onSwitchProfile: (() -> Unit)? = null) {
    val profiles by ProfileRepository.state.collectAsStateWithLifecycle()
    val library by remember { LibraryRepository.ensureLoaded(); LibraryRepository.uiState }.collectAsStateWithLifecycle()
    val watched by remember { WatchedRepository.ensureLoaded(); WatchedRepository.uiState }.collectAsStateWithLifecycle()
    val progress by remember { WatchProgressRepository.ensureLoaded(); WatchProgressRepository.uiState }.collectAsStateWithLifecycle()
    val fullyWatched by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val stats = remember(profiles.activeProfile?.profileIndex, library, watched, progress, fullyWatched) {
        buildDesktopProfileStats(progress.entries, watched.items, fullyWatched, library.items,
            CurrentDateProvider.todayIsoDate(), WatchedClock.nowEpochMs())
    }
    DesktopProfileStatsContent(profiles.activeProfile?.name?.takeIf(String::isNotBlank) ?: "Mon profil", stats, onSwitchProfile)
}

@Composable
internal fun DesktopProfileStatsContent(profileName: String, stats: DesktopProfileStats, onSwitchProfile: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mon profil", style = MaterialTheme.typography.headlineMedium)
            if (onSwitchProfile != null) OutlinedButton(onClick = onSwitchProfile) { Text("Changer de profil") }
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Le Nuvio de $profileName", style = MaterialTheme.typography.headlineSmall)
                Text("L’historique, la bibliothèque et les sorties sont analysés pour ce profil.")
                Text("${stats.continueCount} en cours   ·   ${stats.libraryCount} dans la bibliothèque   ·   ${stats.upcomingCount} à venir")
            }
        }
        Text("VUE D’ENSEMBLE", style = MaterialTheme.typography.titleSmall)
        val minutes = stats.trackedDurationMs.coerceAtLeast(0) / 60_000
        val duration = if (minutes >= 60) "${minutes / 60} h" else "$minutes min"
        val metrics = listOf(
            Triple(stats.continueCount.toString(), "En cours", "Prêts à reprendre"),
            Triple(stats.completedCount.toString(), "Terminés", "Marqués comme vus"),
            Triple(stats.libraryCount.toString(), "Bibliothèque", "Titres enregistrés"),
            Triple(duration, "Durée suivie", "Selon la progression de lecture"),
            Triple(stats.recentActivityCount.toString(), "Cette semaine", "Activité récente · 7 derniers jours"),
            Triple(stats.upcomingCount.toString(), "À venir", "Dans votre bibliothèque"),
        )
        BoxWithConstraints {
            val columns = if (maxWidth >= 1000.dp) 3 else if (maxWidth >= 560.dp) 2 else 1
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                metrics.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { (value, title, caption) ->
                            Surface(Modifier.weight(1f), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(value, style = MaterialTheme.typography.headlineMedium)
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Text("VOS GOÛTS", style = MaterialTheme.typography.titleSmall)
        Text(if (stats.topGenres.isEmpty()) "Vos goûts apparaîtront au fil des titres ajoutés." else stats.topGenres.joinToString(" · "))
    }
}
