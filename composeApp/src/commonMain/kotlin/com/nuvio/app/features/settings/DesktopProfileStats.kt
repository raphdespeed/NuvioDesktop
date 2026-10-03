package com.nuvio.app.features.settings

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watched.watchedItemKeys
import com.nuvio.app.features.watchprogress.WatchProgressEntry

internal data class DesktopProfileStats(
    val continueCount: Int = 0,
    val completedCount: Int = 0,
    val libraryCount: Int = 0,
    val trackedDurationMs: Long = 0,
    val recentActivityCount: Int = 0,
    val upcomingCount: Int = 0,
    val topGenres: List<String> = emptyList(),
)

// Same profile activity identities, duration rules and release-date interpretation as Mobile.
internal fun buildDesktopProfileStats(
    progress: List<WatchProgressEntry>, watched: List<WatchedItem>,
    fullyWatchedSeriesKeys: Set<String>, library: List<LibraryItem>,
    todayIsoDate: String, nowEpochMs: Long,
): DesktopProfileStats {
    val entries = progress.filter { it.parentMetaType.profileCompletedContentKind() != null &&
        !it.parentMetaId.isLikelyProfileLiveTvValue() && !it.contentType.equals("livetv", ignoreCase = true) }
        .map(WatchProgressEntry::normalizedCompletion)
        .distinctBy { listOf(it.parentMetaType, it.parentMetaId, it.videoId, it.seasonNumber, it.episodeNumber) }
    val libraryItems = library.filter(LibraryItem::isProfileInsightContent)
    val watchedItems = watched.filter(WatchedItem::isProfileInsightContent)
    val continueCount = entries.filter { it.isResumable && it.progressFraction >= 0.02f }
        .distinctBy { entry -> if (entry.isEpisode || entry.parentMetaType.profileNormalizedType() == "series") {
            "series:${entry.parentMetaId}"
        } else "movie:${entry.videoId.ifBlank { entry.parentMetaId }}" }.size
    val completedMovies = watchedItems.filter { it.type.profileCompletedContentKind() == "movie" }.distinctBy { it.id }.size
    val completedSeries = watchedItems.filter { it.type.profileCompletedContentKind() == "series" }.groupBy { it.id }.count { (_, group) ->
        group.any { item -> (item.season == null && item.episode == null && !item.type.equals("tv", true)) ||
            watchedItemKeys(item.type, item.id).any(fullyWatchedSeriesKeys::contains) }
    }
    return DesktopProfileStats(
        continueCount = continueCount,
        completedCount = completedMovies + completedSeries,
        libraryCount = libraryItems.size,
        trackedDurationMs = profileTrackedDurationMs(watchedItems, entries),
        recentActivityCount = profileRecentActivityCount(watchedItems, entries, nowEpochMs - ProfileInsightsRecentWindowMs),
        upcomingCount = libraryItems.count { it.profileReleaseIsoDate()?.let { date -> date >= todayIsoDate } == true },
        topGenres = libraryItems.flatMap { it.genres }.map(String::trim).filter(String::isNotBlank)
            .groupingBy { it }.eachCount().entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(3).map { it.key },
    )
}

private fun WatchProgressEntry.profileTrackedDurationMs(): Long {
    if (durationMs <= 0L) return lastPositionMs.coerceAtLeast(0L)
    if (isEffectivelyCompleted) return durationMs
    if (lastPositionMs > 0L) return lastPositionMs.coerceIn(0L, durationMs)
    val explicitPercent = normalizedProgressPercent ?: return 0L
    return (durationMs * (explicitPercent / 100f)).toLong().coerceIn(0L, durationMs)
}

private fun profileTrackedDurationMs(
    watchedItems: List<WatchedItem>,
    progressEntries: List<WatchProgressEntry>,
): Long {
    val progressDurationByKey = progressEntries
        .asSequence()
        .mapNotNull { entry ->
            entry.profileTrackableActivityKey()?.let { key -> key to entry.profileTrackedDurationMs() }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, durations) -> durations.maxOrNull() ?: 0L }

    val watchedDurationByKey = watchedItems
        .asSequence()
        .mapNotNull { item ->
            item.profileTrackableActivityKey()?.let { key -> key to item.profileCachedWatchedDurationMs() }
        }
        .filter { (key, duration) -> duration > 0L && key !in progressDurationByKey }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, durations) -> durations.maxOrNull() ?: 0L }

    return progressDurationByKey.values.sum() + watchedDurationByKey.values.sum()
}

private fun profileMetaLookupCandidates(type: String?, id: String?): List<Pair<String, String>> {
    val cleanId = id?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
    val cleanType = type?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
    val normalizedKind = cleanType.profileCompletedContentKind()

    val typeCandidates = buildList {
        add(cleanType)
        normalizedKind?.let(::add)
        when (normalizedKind) {
            "movie" -> add("film")
            "series" -> {
                add("tv")
                add("show")
                add("tvshow")
            }
        }
    }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

    return typeCandidates.map { candidateType -> candidateType to cleanId }
}

private fun profileRecentActivityCount(
    watchedItems: List<WatchedItem>,
    progressEntries: List<WatchProgressEntry>,
    recentCutoff: Long,
): Int = buildSet {
    watchedItems
        .asSequence()
        .filter { item -> item.markedAtEpochMs >= recentCutoff }
        .mapNotNull(WatchedItem::profileActivityKey)
        .forEach(::add)
    progressEntries
        .asSequence()
        .filter { entry -> entry.lastUpdatedEpochMs >= recentCutoff }
        .mapNotNull(WatchProgressEntry::profileActivityKey)
        .forEach(::add)
}.size

private fun WatchedItem.profileActivityKey(): String? {
    if (!isProfileTrackableActivity()) return null
    val kind = type.profileCompletedContentKind() ?: return null
    val contentId = id.trim().takeIf { it.isNotBlank() } ?: return null
    return "$kind:$contentId:${season ?: -1}:${episode ?: -1}"
}

private fun WatchProgressEntry.profileActivityKey(): String? {
    if (!isProfileTrackableActivity()) return null
    val kind = parentMetaType.profileCompletedContentKind() ?: return null
    val contentId = parentMetaId.trim().takeIf { it.isNotBlank() } ?: return null
    return "$kind:$contentId:${seasonNumber ?: -1}:${episodeNumber ?: -1}"
}

private fun WatchedItem.profileTrackableActivityKey(): String? = profileActivityKey()

private fun WatchProgressEntry.profileTrackableActivityKey(): String? = profileActivityKey()

private fun WatchedItem.isProfileTrackableActivity(): Boolean {
    val kind = type.profileCompletedContentKind() ?: return false
    return kind == "movie" || (kind == "series" && season != null && episode != null)
}

private fun WatchProgressEntry.isProfileTrackableActivity(): Boolean {
    val kind = parentMetaType.profileCompletedContentKind() ?: return false
    return kind == "movie" || (kind == "series" && seasonNumber != null && episodeNumber != null)
}

private fun WatchedItem.profileCachedWatchedDurationMs(): Long {
    val kind = type.profileCompletedContentKind() ?: return 0L
    val meta = profileCachedMeta(type, id) ?: return 0L
    val minutes = when {
        kind == "movie" && season == null && episode == null -> profileParseRuntimeMinutes(meta.runtime)
        kind == "series" && season != null && episode != null -> meta.videos
            .firstOrNull { video -> video.season == season && video.episode == episode }
            ?.runtime
            ?.takeIf { runtime -> runtime > 0 }
        else -> null
    } ?: return 0L
    return minutes.toLong() * ProfileInsightsMinuteMs
}

private fun profileCachedMeta(type: String?, id: String?): MetaDetails? {
    for ((lookupType, lookupId) in profileMetaLookupCandidates(type, id)) {
        MetaDetailsRepository.peek(type = lookupType, id = lookupId)?.let { return it }
    }
    return null
}

private fun profileParseRuntimeMinutes(value: String?): Int? {
    val runtime = value?.trim()?.takeIf { it.isNotBlank() } ?: return null

    profileHourMinuteColonRegex.matchEntire(runtime)?.let { match ->
        val hours = match.groupValues[1].toIntOrNull() ?: return null
        val minutes = match.groupValues[2].toIntOrNull() ?: return null
        return ((hours * 60) + minutes).coerceAtLeast(0)
    }

    val hoursToken = profileHourTokenRegex.find(runtime)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val minutesToken = profileMinuteTokenRegex.find(runtime)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (hoursToken != null || minutesToken != null) {
        return (((hoursToken ?: 0).coerceAtLeast(0) * 60) + (minutesToken ?: 0).coerceAtLeast(0))
    }

    return profileDigitsOnlyRegex.matchEntire(runtime)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.coerceAtLeast(0)
}

private fun LibraryItem.profileReleaseIsoDate(): String? =
    releaseInfo.profileExtractIsoDate()

private fun String?.profileExtractIsoDate(): String? {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return null

    if (value.length >= 10) {
        for (start in 0..(value.length - 10)) {
            val candidate = value.substring(start, start + 10)
            if (candidate.isIsoDateCandidate()) return candidate
        }
    }

    val normalized = value
        .replace(',', ' ')
        .replace('.', ' ')
        .replace('/', ' ')
        .replace('-', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
    val tokens = normalized.split(' ').filter(String::isNotBlank)
    val yearIndex = tokens.indexOfFirst { token ->
        token.length == 4 && token.all(Char::isDigit) && token.toIntOrNull() in 1000..9999
    }
    if (yearIndex < 0) return null

    val year = tokens[yearIndex].toInt()
    val monthBefore = tokens.getOrNull(yearIndex - 1)?.profileMonthNumber()
    val monthAfter = tokens.getOrNull(yearIndex + 1)?.profileMonthNumber()
    val month = monthBefore ?: monthAfter ?: 12
    val dayBefore = tokens.getOrNull(yearIndex - 1)?.toIntOrNull()?.takeIf { it in 1..31 }
    val dayAfterOne = tokens.getOrNull(yearIndex + 1)?.toIntOrNull()?.takeIf { it in 1..31 }
    val dayAfterTwo = tokens.getOrNull(yearIndex + 2)?.toIntOrNull()?.takeIf { it in 1..31 }
    val day = when {
        monthBefore != null -> dayBefore ?: 1
        monthAfter != null -> dayAfterTwo ?: 1
        else -> dayAfterOne ?: 31
    }.coerceAtMost(profileDaysInMonth(year, month))

    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun String.isIsoDateCandidate(): Boolean =
    length == 10 &&
        this[4] == '-' &&
        this[7] == '-' &&
        take(4).all(Char::isDigit) &&
        substring(5, 7).all(Char::isDigit) &&
        substring(8, 10).all(Char::isDigit)

private fun String.profileMonthNumber(): Int? =
    when (trim().lowercase().take(3)) {
        "jan", "oca" -> 1
        "feb", "şub", "sub" -> 2
        "mar" -> 3
        "apr", "nis" -> 4
        "may", "mai" -> 5
        "jun", "haz" -> 6
        "jul", "tem" -> 7
        "aug", "ağu", "agu" -> 8
        "sep", "eyl" -> 9
        "oct", "eki" -> 10
        "nov", "kas" -> 11
        "dec", "ara" -> 12
        else -> null
    }

private fun profileDaysInMonth(year: Int, month: Int): Int =
    when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
        else -> 31
    }

private fun String.profileNormalizedType(): String? =
    when (trim().lowercase()) {
        "movie", "film" -> "movie"
        "series", "show", "tv", "tvshow", "anime" -> "series"
        "" -> null
        else -> trim().lowercase()
    }

private fun String.profileCompletedContentKind(): String? =
    when (trim().lowercase()) {
        "live-tv", "livetv", "live_tv", "channel", "tv-channel", "tv_channel", "iptv", "m3u", "stalker" -> null
        "movie", "film" -> "movie"
        "series", "show", "tv", "tvshow", "anime" -> "series"
        else -> null
    }

private fun LibraryItem.isProfileInsightContent(): Boolean =
    type.profileCompletedContentKind() != null &&
        !id.isLikelyProfileLiveTvValue() &&
        !name.isLikelyProfileLiveTvValue()

private fun WatchedItem.isProfileInsightContent(): Boolean =
    type.profileCompletedContentKind() != null &&
        !id.isLikelyProfileLiveTvValue() &&
        !name.isLikelyProfileLiveTvValue()

private fun String.isLikelyProfileLiveTvValue(): Boolean {
    val value = trim().lowercase()
    return value.startsWith("http://") ||
        value.startsWith("https://") ||
        value.startsWith("rtmp://") ||
        value.startsWith("rtsp://") ||
        value.endsWith(".m3u") ||
        value.endsWith(".m3u8")
}


private const val ProfileInsightsRecentWindowMs = 7L * 24L * 60L * 60L * 1000L
private const val ProfileInsightsMinuteMs = 60_000L
private val profileHourTokenRegex = Regex("""(?i)(\d+)\s*h(?:ours?)?""")
private val profileMinuteTokenRegex = Regex("""(?i)(\d+)\s*m(?:in(?:ute)?s?)?""")
private val profileHourMinuteColonRegex = Regex("""^\s*(\d+)\s*:\s*(\d{1,2})\s*$""")
private val profileDigitsOnlyRegex = Regex("""^\s*(\d+)\s*$""")
