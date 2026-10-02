@file:Suppress("DEPRECATION") // LocalClipboardManager is the clipboard API exposed by this Compose dependency.

package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.build.TrailerPlaybackMode
import com.nuvio.app.core.diagnostics.CrashDiagnostics
import com.nuvio.app.core.network.DnsOverHttpsProvider
import com.nuvio.app.core.network.DnsOverHttpsSettingsRepository
import com.nuvio.app.core.sync.ProfileSettingsSync
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioKeyboardFocusIndicator
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.details.MetaEpisodeCardStyle
import com.nuvio.app.features.downloads.DownloadsExternalFolderPlatform
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.player.AndroidPlaybackEngine
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.isIos
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.*
import nuvio.composeapp.generated.resources.settings_advanced_doh_description
import nuvio.composeapp.generated.resources.settings_advanced_doh_selected
import nuvio.composeapp.generated.resources.settings_advanced_hero_auto_scroll
import nuvio.composeapp.generated.resources.settings_advanced_hero_auto_scroll_description
import nuvio.composeapp.generated.resources.settings_advanced_hero_motion_preview
import nuvio.composeapp.generated.resources.settings_advanced_hero_motion_preview_description
import nuvio.composeapp.generated.resources.settings_meta_show_episode_ratings
import nuvio.composeapp.generated.resources.settings_meta_show_episode_ratings_description
import nuvio.composeapp.generated.resources.settings_nuvio_speedy_title
import org.jetbrains.compose.resources.stringResource

private const val NuvioSpeedyGithubUrl = "https://github.com/raphdespeed/NuvioMobile-speedy"

private enum class SpeedySettingsCategory {
    New,
    All,
    Core,
    Home,
    Player,
    System,
}

internal fun LazyListScope.nuvioSpeedySettingsContent(
    isTablet: Boolean,
) {
    item {
        NuvioSpeedySettingsPageContent(isTablet = isTablet)
    }
}

@Composable
private fun NuvioSpeedySettingsPageContent(
    isTablet: Boolean,
) {
    val settings by remember {
        NuvioSpeedySettingsRepository.ensureLoaded()
        NuvioSpeedySettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val homeSettings by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val dnsOverHttpsSettings by remember {
        DnsOverHttpsSettingsRepository.ensureLoaded()
        DnsOverHttpsSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val detailSettings by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val externalFolderState by DownloadsExternalFolderPlatform.state.collectAsStateWithLifecycle()
    val lastCrashReport by remember {
        CrashDiagnostics.lastReport
    }.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var backupPayload by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importPayload by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    val hasNewFeatures = settings.isNew(NuvioSpeedyFeature.SubtitleSyncMenu) ||
        settings.isNew(NuvioSpeedyFeature.SubtitleSelectorStyle) ||
        settings.isNew(NuvioSpeedyFeature.AudioSelectorStyle) ||
        settings.isNew(NuvioSpeedyFeature.NextEpisodeButton) ||
        settings.isNew(NuvioSpeedyFeature.PlayerTimeOverlay) ||
        settings.isNew(NuvioSpeedyFeature.PersistentEpisodeShuffle) ||
        settings.isNew(NuvioSpeedyFeature.HeroControlsV2) ||
        settings.isNew(NuvioSpeedyFeature.DetailPresentationControlsV2) ||
        settings.isNew(NuvioSpeedyFeature.NuvioRead) ||
        settings.isNew(NuvioSpeedyFeature.CinematicDetailHeader)
    var selectedCategory by rememberSaveable {
        mutableStateOf(
            if (hasNewFeatures) {
                SpeedySettingsCategory.New
            } else {
                SpeedySettingsCategory.Core
            },
        )
    }
    val backupImportedMessage = stringResource(Res.string.nuvio_speedy_toast_backup_imported)
    val invalidBackupPayloadMessage = stringResource(Res.string.nuvio_speedy_toast_invalid_backup)
    val backupFileReadyMessage = stringResource(Res.string.nuvio_speedy_toast_backup_ready)
    val backupExportFailedMessage = stringResource(Res.string.nuvio_speedy_toast_backup_export_failed)
    val backupImportFailedMessage = stringResource(Res.string.nuvio_speedy_toast_backup_import_failed)
    val backupCopiedMessage = stringResource(Res.string.nuvio_speedy_toast_backup_copied)
    val crashCopiedMessage = stringResource(Res.string.nuvio_speedy_toast_crash_copied)
    val diagnosticsCopiedMessage = stringResource(Res.string.nuvio_speedy_toast_diagnostics_copied)
    val externalFolderFailedMessage = stringResource(Res.string.nuvio_speedy_external_folder_failed)
    val homeHeroVideoPreviewSupported = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP
    val heroVisualControlsEnabled = !settings.originalNuvioHeroBannerEnabled
    val detailHeroTrailerPlaybackSupported = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP

    LaunchedEffect(hasNewFeatures, selectedCategory) {
        if (!hasNewFeatures && selectedCategory == SpeedySettingsCategory.New) {
            selectedCategory = SpeedySettingsCategory.Player
        }
    }

    fun isNew(feature: NuvioSpeedyFeature): Boolean = settings.isNew(feature)
    fun markSeen(feature: NuvioSpeedyFeature) {
        NuvioSpeedySettingsRepository.markFeatureSeen(feature)
    }
    fun importBackupPayload(payload: String) {
        ProfileSettingsSync.importBackupJson(payload)
            .onSuccess {
                NuvioToastController.show(backupImportedMessage)
                showImportDialog = false
                importPayload = ""
                importError = null
            }
            .onFailure { error ->
                importError = error.message ?: invalidBackupPayloadMessage
                NuvioToastController.show(importError ?: invalidBackupPayloadMessage)
            }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nuvio.spacing.listGap),
    ) {
        SettingsSection(
            title = stringResource(Res.string.settings_nuvio_speedy_title),
            isTablet = isTablet,
        ) {
            SpeedyIntroCard(
                isTablet = isTablet,
                hasNewFeatures = settings.hasNewFeatures,
                onMarkAllSeen = NuvioSpeedySettingsRepository::markAllFeaturesSeen,
            )
        }

        SpeedySettingsCategoryBar(
            selected = selectedCategory,
            hasNewFeatures = hasNewFeatures,
            onSelected = { selectedCategory = it },
        )

        if (selectedCategory == SpeedySettingsCategory.All ||
            selectedCategory == SpeedySettingsCategory.Core
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_nuvio_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_home_features_title),
                    description = stringResource(Res.string.nuvio_speedy_home_features_desc),
                    checked = settings.speedyHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HomeExperienceControls)
                        NuvioSpeedySettingsRepository.setSpeedyHomeFeaturesEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_concierge_title),
                    description = stringResource(Res.string.nuvio_speedy_concierge_desc),
                    checked = settings.nuvioConciergeEnabled,
                    enabled = settings.speedyHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HomeExperienceControls)
                        NuvioSpeedySettingsRepository.setNuvioConciergeEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_smart_resume_title),
                    description = stringResource(Res.string.nuvio_speedy_smart_resume_desc),
                    checked = settings.smartResumeEnabled,
                    enabled = settings.speedyHomeFeaturesEnabled && settings.nuvioConciergeEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.SmartResume2),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.SmartResume2)
                        NuvioSpeedySettingsRepository.setSmartResumeEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_continue_watching_ready_badge_title),
                    description = stringResource(Res.string.settings_continue_watching_ready_badge_description),
                    checked = settings.showContinueWatchingReadyBadge,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HomeExperienceControls)
                        NuvioSpeedySettingsRepository.setShowContinueWatchingReadyBadge(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_release_signals_title),
                    description = stringResource(Res.string.nuvio_speedy_release_signals_desc),
                    checked = settings.releaseRadarHomeSignalsEnabled,
                    enabled = settings.speedyHomeFeaturesEnabled && settings.nuvioConciergeEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HomeExperienceControls)
                        NuvioSpeedySettingsRepository.setReleaseRadarHomeSignalsEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_profile_stats_title),
                    description = stringResource(Res.string.nuvio_speedy_profile_stats_desc),
                    checked = settings.profileStatsEnabled,
                    enabled = settings.speedyHomeFeaturesEnabled && settings.nuvioConciergeEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HomeExperienceControls)
                        NuvioSpeedySettingsRepository.setProfileStatsEnabled(it)
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_premium_labs),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_smart_shelf_title),
                    description = stringResource(Res.string.nuvio_speedy_smart_shelf_desc),
                    checked = settings.smartShelvesEnabled,
                    enabled = settings.speedyHomeFeaturesEnabled && !settings.quietHomeModeEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.SmartShelfComposer),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.SmartShelfComposer)
                        NuvioSpeedySettingsRepository.setSmartShelvesEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_release_digest_title),
                    description = stringResource(Res.string.nuvio_speedy_release_digest_desc),
                    checked = settings.releaseRadarDigestEnabled,
                    enabled = settings.speedyHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.ReleaseRadarDigest),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.ReleaseRadarDigest)
                        NuvioSpeedySettingsRepository.setReleaseRadarDigestEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_quiet_home_title),
                    description = stringResource(Res.string.nuvio_speedy_quiet_home_desc),
                    checked = settings.quietHomeModeEnabled,
                    enabled = settings.speedyHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.QuietHomeMode),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.QuietHomeMode)
                        NuvioSpeedySettingsRepository.setQuietHomeModeEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_library_health_title),
                    description = stringResource(Res.string.nuvio_speedy_library_health_desc),
                    checked = settings.libraryHealthEnabled,
                    enabled = settings.speedyHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.LibraryHealth),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.LibraryHealth)
                        NuvioSpeedySettingsRepository.setLibraryHealthEnabled(it)
                    },
                )
            }
        }

        }

        if (selectedCategory == SpeedySettingsCategory.All ||
            selectedCategory == SpeedySettingsCategory.System
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_app_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_status_bar_title),
                    description = stringResource(Res.string.nuvio_speedy_status_bar_desc),
                    checked = settings.statusBarVisible,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.StatusBarVisibility),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.StatusBarVisibility)
                        NuvioSpeedySettingsRepository.setStatusBarVisible(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_live_tv_title),
                    description = stringResource(Res.string.nuvio_speedy_live_tv_desc),
                    checked = settings.liveTvEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.LiveTvControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.LiveTvControls)
                        NuvioSpeedySettingsRepository.setLiveTvEnabled(it)
                    },
                )
            }
        }
        }

        if (selectedCategory == SpeedySettingsCategory.New ||
            selectedCategory == SpeedySettingsCategory.All ||
            selectedCategory == SpeedySettingsCategory.Player
        ) {
            SettingsSection(
                title = stringResource(Res.string.nuvio_speedy_section_player_tools),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    if (selectedCategory != SpeedySettingsCategory.New) {
                        SettingsSwitchRow(
                            title = stringResource(Res.string.settings_playback_parental_guide),
                            description = stringResource(Res.string.settings_playback_parental_guide_description),
                            checked = playerSettings.showParentalGuide,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioSpeedyFeature.ContentWarnings),
                            onCheckedChange = {
                                markSeen(NuvioSpeedyFeature.ContentWarnings)
                                PlayerSettingsRepository.setShowParentalGuide(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_speedy_source_pinning_title),
                            description = stringResource(Res.string.nuvio_speedy_source_pinning_desc),
                            checked = settings.streamSourcePinningEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioSpeedyFeature.StreamSourcePinning),
                            onCheckedChange = {
                                markSeen(NuvioSpeedyFeature.StreamSourcePinning)
                                NuvioSpeedySettingsRepository.setStreamSourcePinningEnabled(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_speedy_background_stream_prefetch_title),
                            description = stringResource(Res.string.nuvio_speedy_background_stream_prefetch_desc),
                            checked = settings.backgroundStreamPrefetchEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioSpeedyFeature.BackgroundStreamPrefetch),
                            onCheckedChange = {
                                markSeen(NuvioSpeedyFeature.BackgroundStreamPrefetch)
                                NuvioSpeedySettingsRepository.setBackgroundStreamPrefetchEnabled(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_speedy_player_status_overlay_title),
                            description = stringResource(Res.string.nuvio_speedy_player_status_overlay_desc),
                            checked = settings.playerStatusOverlayEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioSpeedyFeature.PlayerStatusOverlay),
                            onCheckedChange = {
                                markSeen(NuvioSpeedyFeature.PlayerStatusOverlay)
                                NuvioSpeedySettingsRepository.setPlayerStatusOverlayEnabled(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.SubtitleSyncMenu)) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_speedy_subtitle_sync_title),
                        description = stringResource(Res.string.nuvio_speedy_subtitle_sync_desc),
                        checked = playerSettings.subtitleSyncMenuEnabled,
                        enabled = settings.speedyHomeFeaturesEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.SubtitleSyncMenu),
                        onCheckedChange = {
                            markSeen(NuvioSpeedyFeature.SubtitleSyncMenu)
                            PlayerSettingsRepository.setSubtitleSyncMenuEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.SubtitleSelectorStyle)) {
                    SpeedyChoiceRow(
                        title = stringResource(Res.string.nuvio_speedy_subtitle_selector_title),
                        description = stringResource(Res.string.nuvio_speedy_subtitle_selector_desc),
                        selected = settings.subtitleSelectorStyle,
                        options = listOf(
                            SpeedyChoiceOption(
                                NuvioSubtitleSelectorStyle.Speedy,
                                stringResource(Res.string.nuvio_speedy_subtitle_selector_speedy),
                            ),
                            SpeedyChoiceOption(
                                NuvioSubtitleSelectorStyle.Nuvio,
                                stringResource(Res.string.nuvio_speedy_subtitle_selector_nuvio),
                            ),
                        ),
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.SubtitleSelectorStyle),
                        onSelected = {
                            markSeen(NuvioSpeedyFeature.SubtitleSelectorStyle)
                            NuvioSpeedySettingsRepository.setSubtitleSelectorStyle(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.AudioSelectorStyle)) {
                    SpeedyChoiceRow(
                        title = stringResource(Res.string.nuvio_speedy_audio_selector_title),
                        description = stringResource(Res.string.nuvio_speedy_audio_selector_desc),
                        selected = settings.audioSelectorStyle,
                        options = listOf(
                            SpeedyChoiceOption(
                                NuvioAudioSelectorStyle.Speedy,
                                stringResource(Res.string.nuvio_speedy_subtitle_selector_speedy),
                            ),
                            SpeedyChoiceOption(
                                NuvioAudioSelectorStyle.Nuvio,
                                stringResource(Res.string.nuvio_speedy_subtitle_selector_nuvio),
                            ),
                        ),
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.AudioSelectorStyle),
                        onSelected = {
                            markSeen(NuvioSpeedyFeature.AudioSelectorStyle)
                            NuvioSpeedySettingsRepository.setAudioSelectorStyle(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.PlayerTimeOverlay)) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_speedy_player_clock_title),
                        description = stringResource(Res.string.nuvio_speedy_player_clock_desc),
                        checked = playerSettings.playerClockEndTimeEnabled,
                        enabled = settings.speedyHomeFeaturesEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.PlayerTimeOverlay),
                        onCheckedChange = {
                            markSeen(NuvioSpeedyFeature.PlayerTimeOverlay)
                            PlayerSettingsRepository.setPlayerClockEndTimeEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.PersistentEpisodeShuffle)) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_playback_random_next_episode),
                        description = stringResource(Res.string.settings_playback_random_next_episode_description),
                        checked = playerSettings.randomNextEpisodeEnabled,
                        enabled = settings.speedyHomeFeaturesEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.PersistentEpisodeShuffle),
                        onCheckedChange = {
                            markSeen(NuvioSpeedyFeature.PersistentEpisodeShuffle)
                            PlayerSettingsRepository.setRandomNextEpisodeEnabled(it)
                        },
                    )
                    }
                    if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.NextEpisodeButton)) {
                        if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.PersistentEpisodeShuffle)) {
                            SettingsGroupDivider(isTablet = isTablet)
                        }
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_speedy_next_episode_button_title),
                            description = stringResource(Res.string.nuvio_speedy_next_episode_button_desc),
                            checked = settings.nextEpisodeButtonEnabled,
                            enabled = settings.speedyHomeFeaturesEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioSpeedyFeature.NextEpisodeButton),
                            onCheckedChange = {
                                markSeen(NuvioSpeedyFeature.NextEpisodeButton)
                                NuvioSpeedySettingsRepository.setNextEpisodeButtonEnabled(it)
                            },
                        )
                    }
                    if (selectedCategory != SpeedySettingsCategory.New && !isIos) {
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.settings_playback_android_memory_safe_buffer),
                            description = stringResource(Res.string.settings_playback_android_memory_safe_buffer_description),
                            checked = playerSettings.androidMemorySafeBufferEnabled,
                            enabled = !playerSettings.externalPlayerEnabled &&
                                playerSettings.androidPlaybackEngine != AndroidPlaybackEngine.Libmpv,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioSpeedyFeature.PlayerStatusOverlay),
                            onCheckedChange = PlayerSettingsRepository::setAndroidMemorySafeBufferEnabled,
                        )
                    }
                }
            }
        }

        if (selectedCategory == SpeedySettingsCategory.New ||
            selectedCategory == SpeedySettingsCategory.All ||
            selectedCategory == SpeedySettingsCategory.Home ||
            selectedCategory == SpeedySettingsCategory.Core
        ) {
        if (selectedCategory != SpeedySettingsCategory.Core) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_hero_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_original_hero_title),
                    description = stringResource(Res.string.nuvio_speedy_original_hero_desc),
                    checked = settings.originalNuvioHeroBannerEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroControlsV2),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroControlsV2)
                        NuvioSpeedySettingsRepository.setOriginalNuvioHeroBannerEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_hero_details_button_title),
                    description = stringResource(Res.string.nuvio_speedy_hero_details_button_desc),
                    checked = settings.showHeroDetailsButton,
                    enabled = heroVisualControlsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroControlsV2),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroControlsV2)
                        NuvioSpeedySettingsRepository.setShowHeroDetailsButton(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SpeedyChoiceRow(
                    title = stringResource(Res.string.nuvio_speedy_hero_display_title),
                    description = stringResource(Res.string.nuvio_speedy_hero_display_desc),
                    selected = settings.heroDisplayMode,
                    options = listOf(
                        SpeedyChoiceOption(
                            NuvioHeroDisplayMode.Cinematic,
                            stringResource(Res.string.nuvio_speedy_hero_mode_cinematic),
                        ),
                        SpeedyChoiceOption(
                            NuvioHeroDisplayMode.Balanced,
                            stringResource(Res.string.nuvio_speedy_hero_mode_balanced),
                        ),
                        SpeedyChoiceOption(
                            NuvioHeroDisplayMode.InfoRich,
                            stringResource(Res.string.nuvio_speedy_hero_mode_info_rich),
                        ),
                    ),
                    isTablet = isTablet,
                    enabled = heroVisualControlsEnabled,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onSelected = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setHeroDisplayMode(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SpeedyChoiceRow(
                    title = stringResource(Res.string.nuvio_speedy_hero_artwork_title),
                    description = stringResource(Res.string.nuvio_speedy_hero_artwork_desc),
                    selected = settings.heroArtworkSource,
                    options = listOf(
                        SpeedyChoiceOption(
                            NuvioHeroArtworkSource.Backdrop,
                            stringResource(Res.string.nuvio_speedy_hero_artwork_backdrop),
                        ),
                        SpeedyChoiceOption(
                            NuvioHeroArtworkSource.Poster,
                            stringResource(Res.string.nuvio_speedy_hero_artwork_poster),
                        ),
                    ),
                    isTablet = isTablet,
                    enabled = heroVisualControlsEnabled,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onSelected = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setHeroArtworkSource(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_poster_hero_title),
                    description = stringResource(Res.string.nuvio_speedy_poster_hero_desc),
                    checked = settings.posterArtHeroEnabled,
                    enabled = heroVisualControlsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setPosterArtHeroEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_showcase_hero_title),
                    description = stringResource(Res.string.nuvio_speedy_showcase_hero_desc),
                    checked = settings.streamingShowcaseHeroEnabled,
                    enabled = heroVisualControlsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setStreamingShowcaseHeroEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_showcase_video_preview_title),
                    description = stringResource(Res.string.nuvio_speedy_showcase_video_preview_desc),
                    checked = settings.streamingShowcaseVideoPreviewEnabled,
                    enabled = heroVisualControlsEnabled &&
                        settings.streamingShowcaseHeroEnabled && homeHeroVideoPreviewSupported,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setStreamingShowcaseVideoPreviewEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_showcase_video_preview_sound_title),
                    description = stringResource(Res.string.nuvio_speedy_showcase_video_preview_sound_desc),
                    checked = settings.streamingShowcaseVideoPreviewSoundEnabled,
                    enabled = heroVisualControlsEnabled &&
                        settings.streamingShowcaseHeroEnabled &&
                        settings.streamingShowcaseVideoPreviewEnabled &&
                        homeHeroVideoPreviewSupported,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setStreamingShowcaseVideoPreviewSoundEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_compact_hero_title),
                    description = stringResource(Res.string.nuvio_speedy_compact_hero_desc),
                    checked = settings.compactHeroMetadata,
                    enabled = heroVisualControlsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setCompactHeroMetadata(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_hero_ratings_title),
                    description = stringResource(Res.string.nuvio_speedy_hero_ratings_desc),
                    checked = settings.showHeroRatings,
                    enabled = heroVisualControlsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setShowHeroRatings(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_ratings_above_metadata_title),
                    description = stringResource(Res.string.nuvio_speedy_ratings_above_metadata_desc),
                    checked = settings.ratingsAboveMetadata,
                    enabled = heroVisualControlsEnabled && settings.showHeroRatings,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroControlsV2),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroControlsV2)
                        NuvioSpeedySettingsRepository.setRatingsAboveMetadata(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_hero_overview_title),
                    description = stringResource(Res.string.nuvio_speedy_hero_overview_desc),
                    checked = settings.showHeroOverview,
                    enabled = heroVisualControlsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setShowHeroOverview(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_hero_refresh_haptics_title),
                    description = stringResource(Res.string.nuvio_speedy_hero_refresh_haptics_desc),
                    checked = settings.heroRefreshHapticsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        NuvioSpeedySettingsRepository.setHeroRefreshHapticsEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_advanced_hero_auto_scroll),
                    description = stringResource(Res.string.settings_advanced_hero_auto_scroll_description),
                    checked = homeSettings.heroAutoScrollEnabled,
                    enabled = heroVisualControlsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        HomeCatalogSettingsRepository.setHeroAutoScrollEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_advanced_hero_motion_preview),
                    description = stringResource(Res.string.settings_advanced_hero_motion_preview_description),
                    checked = homeSettings.heroMotionPreviewEnabled,
                    enabled = heroVisualControlsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.HeroExperienceControls)
                        HomeCatalogSettingsRepository.setHeroMotionPreviewEnabled(it)
                    },
                )
            }
        }
        }

        if (selectedCategory == SpeedySettingsCategory.New ||
            selectedCategory == SpeedySettingsCategory.All ||
            selectedCategory == SpeedySettingsCategory.Core
        ) {
            SettingsSection(
                title = stringResource(Res.string.nuvio_speedy_detail_presentation_title),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.CinematicDetailHeader)) {
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_speedy_cinematic_detail_header_title),
                            description = stringResource(Res.string.nuvio_speedy_cinematic_detail_header_desc),
                            checked = settings.cinematicDetailHeaderEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioSpeedyFeature.CinematicDetailHeader),
                            onCheckedChange = {
                                markSeen(NuvioSpeedyFeature.CinematicDetailHeader)
                                NuvioSpeedySettingsRepository.setCinematicDetailHeaderEnabled(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                        SpeedyChoiceRow(
                            title = stringResource(Res.string.nuvio_speedy_cinematic_header_content_title),
                            description = stringResource(Res.string.nuvio_speedy_cinematic_header_content_desc),
                            selected = settings.cinematicHeaderContentMode,
                            options = listOf(
                                SpeedyChoiceOption(
                                    CinematicHeaderContentMode.Productions,
                                    stringResource(Res.string.nuvio_speedy_cinematic_header_content_productions),
                                ),
                                SpeedyChoiceOption(
                                    CinematicHeaderContentMode.WhereToWatch,
                                    stringResource(Res.string.nuvio_speedy_cinematic_header_content_where_to_watch),
                                ),
                                SpeedyChoiceOption(
                                    CinematicHeaderContentMode.Hidden,
                                    stringResource(Res.string.nuvio_speedy_cinematic_header_content_hidden),
                                ),
                            ),
                            isTablet = isTablet,
                            enabled = settings.cinematicDetailHeaderEnabled,
                            highlighted = false,
                            onSelected = NuvioSpeedySettingsRepository::setCinematicHeaderContentMode,
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != SpeedySettingsCategory.New || isNew(NuvioSpeedyFeature.NuvioRead)) {
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_speedy_nuvio_read_title),
                            description = stringResource(Res.string.nuvio_speedy_nuvio_read_desc),
                            checked = settings.nuvioReadEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioSpeedyFeature.NuvioRead),
                            onCheckedChange = {
                                markSeen(NuvioSpeedyFeature.NuvioRead)
                                NuvioSpeedySettingsRepository.setNuvioReadEnabled(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    SpeedyChoiceRow(
                        title = stringResource(Res.string.nuvio_speedy_episode_cards_layout_title),
                        description = stringResource(Res.string.nuvio_speedy_episode_cards_layout_desc),
                        selected = detailSettings.episodeCardStyle,
                        options = listOf(
                            SpeedyChoiceOption(
                                MetaEpisodeCardStyle.Horizontal,
                                stringResource(Res.string.settings_meta_episode_style_horizontal),
                            ),
                            SpeedyChoiceOption(
                                MetaEpisodeCardStyle.List,
                                stringResource(Res.string.nuvio_speedy_episode_cards_layout_compact_list),
                            ),
                            SpeedyChoiceOption(
                                MetaEpisodeCardStyle.VerticalHorizontal,
                                stringResource(Res.string.nuvio_speedy_episode_cards_layout_vertical),
                            ),
                        ),
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.DetailPresentationControlsV2),
                        onSelected = {
                            markSeen(NuvioSpeedyFeature.DetailPresentationControlsV2)
                            MetaScreenSettingsRepository.setEpisodeCardStyle(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_speedy_show_download_button_title),
                        description = stringResource(Res.string.nuvio_speedy_show_download_button_desc),
                        checked = detailSettings.showDownloadAction,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.DetailPresentationControlsV2),
                        onCheckedChange = {
                            markSeen(NuvioSpeedyFeature.DetailPresentationControlsV2)
                            MetaScreenSettingsRepository.setShowDownloadAction(it)
                        },
                    )
                }
            }
        }

        if (selectedCategory == SpeedySettingsCategory.New ||
            selectedCategory == SpeedySettingsCategory.All ||
            selectedCategory == SpeedySettingsCategory.Home
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_release_radar),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_library_only_radar_title),
                    description = stringResource(Res.string.nuvio_speedy_library_only_radar_desc),
                    checked = settings.releaseRadarLibraryOnly,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.ReleaseRadarFilters),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.ReleaseRadarFilters)
                        NuvioSpeedySettingsRepository.setReleaseRadarLibraryOnly(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SpeedyChoiceRow(
                    title = stringResource(Res.string.nuvio_speedy_radar_window_title),
                    description = stringResource(Res.string.nuvio_speedy_radar_window_desc),
                    selected = settings.releaseRadarWindowDays,
                    options = listOf(
                        SpeedyChoiceOption(7, stringResource(Res.string.nuvio_speedy_radar_window_7_days)),
                        SpeedyChoiceOption(30, stringResource(Res.string.nuvio_speedy_radar_window_30_days)),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.ReleaseRadarFilters),
                    onSelected = {
                        markSeen(NuvioSpeedyFeature.ReleaseRadarFilters)
                        NuvioSpeedySettingsRepository.setReleaseRadarWindowDays(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SpeedyChoiceRow(
                    title = stringResource(Res.string.nuvio_speedy_radar_content_title),
                    description = stringResource(Res.string.nuvio_speedy_radar_content_desc),
                    selected = settings.releaseRadarContentFilter,
                    options = listOf(
                        SpeedyChoiceOption(
                            NuvioReleaseRadarContentFilter.All,
                            stringResource(Res.string.nuvio_speedy_filter_all),
                        ),
                        SpeedyChoiceOption(
                            NuvioReleaseRadarContentFilter.Episodes,
                            stringResource(Res.string.nuvio_speedy_filter_episodes),
                        ),
                        SpeedyChoiceOption(
                            NuvioReleaseRadarContentFilter.Movies,
                            stringResource(Res.string.nuvio_speedy_filter_movies),
                        ),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.ReleaseRadarFilters),
                    onSelected = {
                        markSeen(NuvioSpeedyFeature.ReleaseRadarFilters)
                        NuvioSpeedySettingsRepository.setReleaseRadarContentFilter(it)
                    },
                )
            }
        }
        }
        }

        if (selectedCategory == SpeedySettingsCategory.All ||
            selectedCategory == SpeedySettingsCategory.Home
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_details_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                if (detailHeroTrailerPlaybackSupported) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_meta_hero_trailer_playback),
                        description = stringResource(Res.string.settings_meta_hero_trailer_playback_description),
                        checked = detailSettings.heroTrailerPlayback,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.DetailExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioSpeedyFeature.DetailExperienceControls)
                            MetaScreenSettingsRepository.setHeroTrailerPlayback(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_meta_hero_trailer_sound),
                        description = stringResource(Res.string.settings_meta_hero_trailer_sound_description),
                        checked = detailSettings.heroTrailerSoundEnabled,
                        enabled = detailSettings.heroTrailerPlayback,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.DetailExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioSpeedyFeature.DetailExperienceControls)
                            MetaScreenSettingsRepository.setHeroTrailerSoundEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                }
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_meta_show_episode_ratings),
                    description = stringResource(Res.string.settings_meta_show_episode_ratings_description),
                    checked = detailSettings.showEpisodeRatings,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.DetailExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.DetailExperienceControls)
                        MetaScreenSettingsRepository.setShowEpisodeRatings(it)
                    },
                )
            }
        }
        }

        if (selectedCategory == SpeedySettingsCategory.All ||
            selectedCategory == SpeedySettingsCategory.System
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_network),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                DnsOverHttpsProvider.entries.forEachIndexed { index, provider ->
                    if (index > 0) {
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    SpeedyDnsProviderRow(
                        provider = provider,
                        selected = provider == dnsOverHttpsSettings.provider,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioSpeedyFeature.NetworkControls),
                        onClick = {
                            markSeen(NuvioSpeedyFeature.NetworkControls)
                            DnsOverHttpsSettingsRepository.setProvider(provider)
                        },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_backup_import),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_speedy_backup_download_title),
                    description = stringResource(Res.string.nuvio_speedy_backup_download_desc),
                    icon = Icons.Rounded.Backup,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioSpeedyFeature.BackupImport)
                        val payload = ProfileSettingsSync.exportBackupJson()
                        NuvioSpeedyBackupFileBridge.exportBackup(
                            fileName = "nuvio-backup.json",
                            payload = payload,
                        ) { result ->
                            result
                                .onSuccess { NuvioToastController.show(backupFileReadyMessage) }
                                .onFailure { error -> NuvioToastController.show(error.message ?: backupExportFailedMessage) }
                        }
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_speedy_backup_import_file_title),
                    description = stringResource(Res.string.nuvio_speedy_backup_import_file_desc),
                    icon = Icons.Rounded.Restore,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioSpeedyFeature.BackupImport)
                        importError = null
                        NuvioSpeedyBackupFileBridge.importBackup { result ->
                            result
                                .onSuccess(::importBackupPayload)
                                .onFailure { error -> NuvioToastController.show(error.message ?: backupImportFailedMessage) }
                        }
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_speedy_backup_copy_title),
                    description = stringResource(Res.string.nuvio_speedy_backup_copy_desc),
                    icon = Icons.Rounded.ContentCopy,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioSpeedyFeature.BackupImport)
                        val payload = ProfileSettingsSync.exportBackupJson()
                        backupPayload = payload
                        clipboardManager.setText(AnnotatedString(payload))
                        NuvioToastController.show(backupCopiedMessage)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_speedy_backup_paste_title),
                    description = stringResource(Res.string.nuvio_speedy_backup_paste_desc),
                    icon = Icons.Rounded.Download,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioSpeedyFeature.BackupImport)
                        importError = null
                        showImportDialog = true
                    },
                )
            }
        }

        if (!isIos) {
            SettingsSection(
                title = stringResource(Res.string.nuvio_speedy_section_downloads),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(
                            if (externalFolderState.uri.isNullOrBlank()) {
                                Res.string.nuvio_speedy_external_folder_choose_title
                            } else {
                                Res.string.nuvio_speedy_external_folder_change_title
                            },
                        ),
                        description = when {
                            externalFolderState.unavailable -> stringResource(
                                Res.string.nuvio_speedy_external_folder_unavailable_description,
                            )
                            !externalFolderState.displayName.isNullOrBlank() -> externalFolderState.displayName.orEmpty()
                            else -> stringResource(Res.string.nuvio_speedy_external_folder_description)
                        },
                        icon = Icons.Rounded.Folder,
                        isTablet = isTablet,
                        onClick = {
                            DownloadsExternalFolderPlatform.chooseFolder { result ->
                                result.onFailure { error ->
                                    NuvioToastController.show(error.message ?: externalFolderFailedMessage)
                                }
                            }
                        },
                    )
                    if (!externalFolderState.uri.isNullOrBlank()) {
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsNavigationRow(
                            title = stringResource(Res.string.nuvio_speedy_external_folder_remove_title),
                            description = stringResource(Res.string.nuvio_speedy_external_folder_remove_description),
                            icon = Icons.Rounded.Delete,
                            isTablet = isTablet,
                            onClick = DownloadsExternalFolderPlatform::clearFolder,
                        )
                    }
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_speedy_section_discovery),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_speedy_highlight_title),
                    description = stringResource(Res.string.nuvio_speedy_highlight_desc),
                    checked = settings.featureHighlightsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioSpeedyFeature.FeatureHighlights),
                    onCheckedChange = {
                        markSeen(NuvioSpeedyFeature.FeatureHighlights)
                        NuvioSpeedySettingsRepository.setFeatureHighlightsEnabled(it)
                    },
                )
            }
        }

        SpeedyCommunityFooter(
            isTablet = isTablet,
            onGithubClick = {
                markSeen(NuvioSpeedyFeature.CommunityLinks)
                uriHandler.openUri(NuvioSpeedyGithubUrl)
            },
        )

        if (CrashDiagnostics.reportsSupported) {
            SettingsSection(
                title = stringResource(Res.string.nuvio_speedy_section_diagnostics),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.nuvio_speedy_copy_diagnostics_title),
                        description = stringResource(Res.string.nuvio_speedy_copy_diagnostics_desc),
                        icon = Icons.Rounded.ContentCopy,
                        isTablet = isTablet,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(CrashDiagnostics.currentReport()))
                            NuvioToastController.show(diagnosticsCopiedMessage)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.nuvio_speedy_copy_last_crash_title),
                        description = lastCrashReport?.let { report ->
                            listOf(report.contextSummary, report.summary)
                                .filter(String::isNotBlank)
                                .joinToString("\n")
                        } ?: stringResource(Res.string.nuvio_speedy_copy_last_crash_empty_desc),
                        icon = Icons.Rounded.ContentCopy,
                        enabled = lastCrashReport != null,
                        isTablet = isTablet,
                        onClick = {
                            val report = lastCrashReport ?: return@SettingsNavigationRow
                            clipboardManager.setText(AnnotatedString(report.details))
                            NuvioToastController.show(crashCopiedMessage)
                        },
                    )
                }
            }
        }

        }
    }

    backupPayload?.let { payload ->
        BackupPayloadDialog(
            payload = payload,
            onDismiss = { backupPayload = null },
            onCopy = {
                clipboardManager.setText(AnnotatedString(payload))
                NuvioToastController.show(backupCopiedMessage)
            },
        )
    }

    if (showImportDialog) {
        ImportBackupDialog(
            payload = importPayload,
            error = importError,
            onPayloadChange = {
                importPayload = it
                importError = null
            },
            onDismiss = {
                showImportDialog = false
                importPayload = ""
                importError = null
            },
            onImport = {
                importBackupPayload(importPayload)
            },
        )
    }
}

@Composable
private fun SpeedyDnsProviderRow(
    provider: DnsOverHttpsProvider,
    selected: Boolean,
    isTablet: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val iconSize = if (isTablet) 42.dp else 36.dp
    val rowShape = RoundedCornerShape(if (isTablet) NuvioTokens.Radius.lg else NuvioTokens.Radius.md)
    val rowColor = when {
        selected -> tokens.colors.accent.copy(alpha = 0.13f)
        highlighted -> tokens.colors.accent.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val borderColor = when {
        selected -> tokens.colors.accent.copy(alpha = 0.86f)
        highlighted -> tokens.colors.accent.copy(alpha = 0.72f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor, rowShape)
            .border(tokens.borders.hairline, borderColor, rowShape)
            .nuvioKeyboardFocusIndicator(rowShape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(iconSize),
            color = if (selected) {
                tokens.colors.accent.copy(alpha = 0.22f)
            } else {
                tokens.colors.accent.copy(alpha = tokens.opacity.pressed)
            },
            shape = tokens.shapes.compactCard,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = provider.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (highlighted) {
                    SpeedyNewBadge()
                }
            }
            Text(
                text = if (selected) {
                    stringResource(Res.string.settings_advanced_doh_selected)
                } else {
                    stringResource(Res.string.settings_advanced_doh_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = stringResource(Res.string.settings_advanced_doh_selected),
                tint = tokens.colors.accent,
            )
        }
    }
}

private data class SpeedyChoiceOption<T>(
    val value: T,
    val label: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SpeedyChoiceRow(
    title: String,
    description: String,
    selected: T,
    options: List<SpeedyChoiceOption<T>>,
    isTablet: Boolean,
    enabled: Boolean = true,
    highlighted: Boolean,
    onSelected: (T) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val highlightShape = RoundedCornerShape(if (isTablet) NuvioTokens.Radius.lg else NuvioTokens.Radius.md)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier
                        .background(tokens.colors.accent.copy(alpha = 0.08f), highlightShape)
                        .border(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.72f), highlightShape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) tokens.colors.textPrimary else tokens.colors.textMuted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (highlighted) {
                    SpeedyNewBadge()
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted.copy(alpha = if (enabled) 1f else 0.6f),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option.value == selected
                Surface(
                    modifier = Modifier
                        .nuvioKeyboardFocusIndicator(RoundedCornerShape(999.dp), enabled)
                        .clickable(enabled = enabled) { onSelected(option.value) },
                    color = if (isSelected) {
                        tokens.colors.accent.copy(alpha = if (enabled) 1f else 0.45f)
                    } else {
                        tokens.colors.surfaceCard.copy(alpha = if (enabled) 0.72f else 0.42f)
                    },
                    contentColor = if (isSelected) {
                        tokens.colors.onAccent
                    } else {
                        tokens.colors.textPrimary
                    },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        tokens.borders.hairline,
                        if (isSelected) {
                            tokens.colors.accent.copy(alpha = 0.86f)
                        } else {
                            tokens.colors.borderSubtle
                        },
                    ),
                ) {
                    Text(
                        text = option.label,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) tokens.colors.onAccent else tokens.colors.textPrimary.copy(alpha = if (enabled) 1f else 0.52f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedyNewBadge() {
    val tokens = MaterialTheme.nuvio
    Surface(
        color = tokens.colors.accent.copy(alpha = 0.16f),
        contentColor = tokens.colors.accent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.42f)),
    ) {
        Text(
            text = stringResource(Res.string.settings_new_feature_badge),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun SpeedyCommunityFooter(
    isTablet: Boolean,
    onGithubClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface.copy(alpha = 0.72f),
        shape = if (isTablet) RoundedCornerShape(NuvioTokens.Radius.xl) else tokens.shapes.compactCard,
        border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.nuvio_speedy_footer_title),
                style = MaterialTheme.typography.titleMedium,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.nuvio_speedy_footer_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpeedyFooterLink(
                    title = stringResource(Res.string.nuvio_speedy_footer_github),
                    subtitle = stringResource(Res.string.nuvio_speedy_footer_releases),
                    icon = appIconPainter(AppIconResource.GithubMark),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGithubClick,
                )
            }
        }
    }
}

@Composable
private fun SpeedyFooterLink(
    title: String,
    subtitle: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
        color = tokens.colors.accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(NuvioTokens.Radius.lg),
        border = BorderStroke(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                color = tokens.colors.accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(NuvioTokens.Radius.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = title,
                        tint = tokens.colors.accent,
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SpeedySettingsCategoryBar(
    selected: SpeedySettingsCategory,
    hasNewFeatures: Boolean,
    onSelected: (SpeedySettingsCategory) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val categories = buildList {
        if (hasNewFeatures) {
            add(SpeedySettingsCategory.New to stringResource(Res.string.nuvio_speedy_category_new))
        }
        add(SpeedySettingsCategory.All to stringResource(Res.string.nuvio_speedy_category_all))
        add(SpeedySettingsCategory.Core to stringResource(Res.string.nuvio_speedy_category_core))
        add(SpeedySettingsCategory.Home to stringResource(Res.string.nuvio_speedy_category_home))
        add(SpeedySettingsCategory.Player to stringResource(Res.string.nuvio_speedy_category_player))
        add(SpeedySettingsCategory.System to stringResource(Res.string.nuvio_speedy_category_system))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        shape = tokens.shapes.compactCard,
        border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            categories.forEach { (category, label) ->
                val isSelected = selected == category
                Surface(
                    modifier = Modifier.clickable { onSelected(category) },
                    color = if (isSelected) {
                        tokens.colors.accent.copy(alpha = 0.16f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        tokens.borders.hairline,
                        if (isSelected) tokens.colors.accent else tokens.colors.borderSubtle,
                    ),
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) tokens.colors.accent else tokens.colors.textMuted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedyIntroCard(
    isTablet: Boolean,
    hasNewFeatures: Boolean,
    onMarkAllSeen: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        shape = if (isTablet) RoundedCornerShape(NuvioTokens.Radius.xl) else tokens.shapes.compactCard,
        border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.nuvio_speedy_intro_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.nuvio_speedy_intro_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                }
            }
            if (hasNewFeatures) {
                OutlinedButton(onClick = onMarkAllSeen) {
                    Text(stringResource(Res.string.nuvio_speedy_mark_all_seen))
                }
            }
        }
    }
}

@Composable
private fun BackupPayloadDialog(
    payload: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nuvio_speedy_backup_ready_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(Res.string.nuvio_speedy_backup_ready_desc))
                OutlinedTextField(
                    value = payload,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    maxLines = 8,
                )
            }
        },
        confirmButton = {
            Button(onClick = onCopy) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(stringResource(Res.string.nuvio_speedy_backup_copy_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.nuvio_speedy_close))
            }
        },
    )
}

@Composable
private fun ImportBackupDialog(
    payload: String,
    error: String?,
    onPayloadChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nuvio_speedy_import_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(Res.string.nuvio_speedy_import_desc))
                OutlinedTextField(
                    value = payload,
                    onValueChange = onPayloadChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    placeholder = { Text(stringResource(Res.string.nuvio_speedy_import_placeholder)) },
                    maxLines = 10,
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = payload.isNotBlank(),
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(stringResource(Res.string.nuvio_speedy_import_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.nuvio_speedy_cancel))
            }
        },
    )
}
