package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun formatDonationDate(rawDate: String): String {
    val datePart = rawDate.substringBefore('T')
    val parts = datePart.split('-')
    if (parts.size != 3) return rawDate
    val year = parts[0]
    val month = parts[1].toIntOrNull()?.let { monthIndex ->
        listOf(
            stringResource(Res.string.community_month_jan),
            stringResource(Res.string.community_month_feb),
            stringResource(Res.string.community_month_mar),
            stringResource(Res.string.community_month_apr),
            stringResource(Res.string.community_month_may),
            stringResource(Res.string.community_month_jun),
            stringResource(Res.string.community_month_jul),
            stringResource(Res.string.community_month_aug),
            stringResource(Res.string.community_month_sep),
            stringResource(Res.string.community_month_oct),
            stringResource(Res.string.community_month_nov),
            stringResource(Res.string.community_month_dec),
        ).getOrNull(monthIndex - 1)
    } ?: return rawDate
    val day = parts[2].toIntOrNull()?.toString() ?: return rawDate
    return stringResource(Res.string.community_date_format, month, day, year)
}
