package com.example.agribotz.app.ui.home

import com.example.agribotz.R
import com.example.agribotz.app.domain.GPS
import com.example.agribotz.app.domain.Site
import com.example.agribotz.app.util.toDisplayDate
import com.example.agribotz.app.util.parseScheduleMask
import com.example.agribotz.app.domain.ScheduleResult
import com.example.agribotz.app.domain.Variable
import java.util.Calendar
import java.util.Locale

data class SiteUi(
    val id: String,
    val name: String,
    val createdAt: String?,
    val createdAgo: String?,
    val isActive: Boolean,
    val activatedAt: String?,
    val deactivatedAt: String?,
    val activatedAgo: String?,
    val deactivatedAgo: String?,
    val isTerminated: Boolean,
    val terminatedAt: String?,
    val terminatedAgo: String?,
    val numberOfGadgets: Int
) {
    val createdAtFormatted: String?
        get() = createdAt?.toDisplayDate()

    val activatedAtFormatted: String?
        get() = activatedAt?.toDisplayDate()

    val deactivatedAtFormatted: String?
        get() = deactivatedAt?.toDisplayDate()

    val terminatedAtFormatted: String?
        get() = terminatedAt?.toDisplayDate()

    val createdResId = R.string.Created_Since

    val statusResId: Int
        get() = when {
            isTerminated -> R.string.Terminated_Since
            isActive -> R.string.Activated_Since
            else -> R.string.Deactivated_Since
        }

    val statusDate: String?
        get() = when {
            isTerminated -> terminatedAtFormatted
            isActive -> activatedAtFormatted
            else -> deactivatedAtFormatted
        }
}

fun SiteUi.toDomain(): Site {
    return Site(
        id = id,
        name = name,
        createdAt = createdAt,
        isActive = isActive,
        activatedAt = activatedAt,
        deactivatedAt = deactivatedAt,
        isTerminated = isTerminated,
        terminatedAt = terminatedAt,
        numberOfGadgets = numberOfGadgets
    )
}

data class GadgetCardUi(
    val id: String,
    val name: String,
    val hasGps: Boolean,
    val gps: GPS?,
    val isOnline: Boolean,
    val onlineAt: String?,
    val offlineAt: String?,
    val onlineTimeAgo: String?,
    val offlineTimeAgo: String?,
    val isActive: Boolean,
    val activatedAt: String?,
    val deactivatedAt: String?,
    val activeTimeAgo: String?,
    val inactiveTimeAgo: String?,
    val isTerminated: Boolean,
    val terminatedAt: String?,
    val terminatedTimeAgo: String?,
    val numberOfValves: Int,
    val numberOfSensors: Int
) {

    val canOpenMap: Boolean
        get() = hasGps && gps != null

    val onlineAtFormatted: String?
        get() = onlineAt?.toDisplayDate()

    val offlineAtFormatted: String?
        get() = offlineAt?.toDisplayDate()

    val activatedAtFormatted: String?
        get() = activatedAt?.toDisplayDate()

    val deactivatedAtFormatted: String?
        get() = deactivatedAt?.toDisplayDate()

    val terminatedAtFormatted: String?
        get() = terminatedAt?.toDisplayDate()

    val showOnlineState: Boolean
        get() = !isTerminated && isActive

    val onlineStateTimeAgo: String?
        get() = if (isOnline) onlineTimeAgo else offlineTimeAgo

    val statusResId: Int?
        get() = if (isTerminated) {
            R.string.Terminated_Since
        } else if (isActive) {
            R.string.Activated_Since
        } else {
            R.string.Deactivated_Since
        }

    val statusDate: String?
        get() = if (isTerminated) {
            terminatedAtFormatted
        } else if (isActive) {
            activatedAtFormatted
        } else {
            deactivatedAtFormatted
        }

    val connectResId: Int?
        get() = if (isOnline) {
            R.string.Online_Since
        } else {
            R.string.Offline_Since
        }

    val connectDate: String?
        get() = if (isOnline) {
            onlineAtFormatted
        } else {
            offlineAtFormatted
        }
}

data class WeekDayChipUi(
    val key: String,      // "sun", "mon", ...
    val label: String,    // "S", "M", ...
    val isSelected: Boolean
)

data class ScheduleUi(
    val index: Int,
    val isSet: Boolean,

    // Start
    val startEpochSec: Long,
    val startDateText: String,   // e.g. 26/05/2025
    val startTimeText: String,   // e.g. 12:00

    // Duration
    val durationSec: Int,
    val durationText: String,    // e.g. 2m 30s

    // Recurrence (raw / logic)
    val repeatLabel: String,     // e.g. Does not repeat / Every day / Every week ...
    val daysMask: Long,
    val selectedDaysShort: List<String>,
    val selectedDayKeys: Set<String>,
    val dayOfMonth: Int?,
    val monthShort: String?,

    // End recurrence
    val endEpochSec: Long,
    val hasEndRecurrence: Boolean,
    val endDateText: String?,
    val endTimeText: String?,

    // Row state
    val enabled: Boolean,

    // Backward-compatible summary
    val summaryLine: String
) {
    // ---------- display helpers for the UI ----------
    val titleText: String
        get() = when (index) {
            1 -> "First Schedule"
            2 -> "Second Schedule"
            3 -> "Third Schedule"
            4 -> "Fourth Schedule"
            5 -> "Fifth Schedule"
            else -> "Schedule $index"
        }

    val showNotSetOnly: Boolean
        get() = !isSet

    val startingOnValueText: String
        get() = if (!isSet) "Not Set" else "$startDateText at $startTimeText"

    val showRepeatEveryRow: Boolean
        get() = isSet && repeatLabel != "Does not repeat"

    val repeatEveryValueText: String
        get() = when {
            !isSet -> ""

            repeatLabel == "Every week" -> "Week on"

            repeatLabel == "Every month" -> {
                val day = dayOfMonth
                if (day != null && day > 0) {
                    "${day.toOrdinal()} of the month"
                } else {
                    "Month"
                }
            }

            repeatLabel == "Every year" -> {
                val day = dayOfMonth
                val month = monthShort
                if (day != null && day > 0 && !month.isNullOrBlank()) {
                    "${day.toOrdinal()} of $month"
                } else {
                    "Year"
                }
            }

            repeatLabel == "Every day" -> "Day"
            repeatLabel == "Every hour" -> "Hour"

            else -> repeatLabel.removePrefix("Every ").replaceFirstChar { it.uppercaseChar() }
        }

    fun Int.toOrdinal(): String {
        if (this % 100 in 11..13) return "${this}th"

        return when (this % 10) {
            1 -> "${this}st"
            2 -> "${this}nd"
            3 -> "${this}rd"
            else -> "${this}th"
        }
    }

    val showWeeklyChips: Boolean
        get() = isSet && repeatLabel == "Every week"

    val weeklyChips: List<WeekDayChipUi>
        get() {
            val ordered = listOf(
                "sun" to "S",
                "mon" to "M",
                "tue" to "T",
                "wed" to "W",
                "thu" to "T",
                "fri" to "F",
                "sat" to "S"
            )
            return ordered.map { (key, label) ->
                WeekDayChipUi(
                    key = key,
                    label = label,
                    isSelected = selectedDayKeys.contains(key)
                )
            }
        }

    val showEndRecurrenceRow: Boolean
        get() = isSet && hasEndRecurrence && !endDateText.isNullOrBlank() && !endTimeText.isNullOrBlank()

    val endRecurrenceValueText: String
        get() = if (showEndRecurrenceRow) "$endDateText at $endTimeText" else ""
}

fun mapScheduleToUi(
    variable: Variable.ScheduleVar,
    index: Int
): ScheduleUi {
    val v = variable.value

    // Not set / cleared
    if (v.len <= 0 || v.frm <= 0L) {
        return ScheduleUi(
            index = index,
            isSet = false,
            startEpochSec = 0L,
            startDateText = "Not Set",
            startTimeText = "Not Set",
            durationSec = 0,
            durationText = "0m",
            repeatLabel = "Does not repeat",
            daysMask = 0,
            selectedDaysShort = emptyList(),
            selectedDayKeys = emptySet(),
            dayOfMonth = null,
            monthShort = null,
            endEpochSec = 0L,
            hasEndRecurrence = false,
            endDateText = null,
            endTimeText = null,
            enabled = false,
            summaryLine = "Not Set"
        )
    }

    val startDateText = formatDateDmy(v.frm)
    val startTimeText = formatTimeHm(v.frm)
    val durationText = formatDuration(v.len)

    val parsed = parseScheduleMask(v.msk.toLong())
    val repeatLabel = repeatLabelFromParsed(parsed)

    val selectedDayKeys = parsed.selectedDays
        ?.map { it.lowercase(Locale.US) }
        ?.toSet()
        ?: emptySet()

    val selectedDays = selectedDaysShortFromParsed(parsed)

    val hasEnd = v.to > 0L
    val endDateText = if (hasEnd) formatDateDmy(v.to) else null
    val endTimeText = if (hasEnd) formatTimeHm(v.to) else null

    val summary = buildSummaryLine(
        startTimeText = startTimeText,
        repeatLabel = repeatLabel,
        selectedDaysShort = selectedDays,
        durationText = durationText
    )

    return ScheduleUi(
        index = index,
        isSet = true,
        startEpochSec = v.frm,
        startDateText = startDateText,
        startTimeText = startTimeText,
        durationSec = v.len,
        durationText = durationText,
        repeatLabel = repeatLabel,
        daysMask = v.msk,
        selectedDaysShort = selectedDays,
        selectedDayKeys = selectedDayKeys,
        dayOfMonth = parsed.dayOfMonth,
        monthShort = parsed.month?.replaceFirstChar { it.uppercaseChar() },
        endEpochSec = v.to,
        hasEndRecurrence = hasEnd,
        endDateText = endDateText,
        endTimeText = endTimeText,
        enabled = true,
        summaryLine = summary
    )
}

fun formatDateDmy(epochSec: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochSec * 1000L }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    val year = cal.get(Calendar.YEAR)
    return String.format(Locale.US, "%02d/%02d/%04d", day, month, year)
}

fun formatTimeHm(epochSec: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochSec * 1000L }
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    return String.format(Locale.US, "%02d:%02d", h, m)
}

fun formatDuration(durationSec: Int): String {
    val hours = durationSec / 3600
    val mins = (durationSec % 3600) / 60
    val secs = durationSec % 60

    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 && mins == 0 -> "${hours}h"
        mins > 0 && secs > 0 -> "${mins}m ${secs}s"
        mins > 0 -> "${mins}m"
        else -> "${secs}s"
    }
}

fun repeatLabelFromParsed(parsed: ScheduleResult): String {
    return when (parsed.repeatEvery.lowercase(Locale.US)) {
        "does not repeat" -> "Does not repeat"
        "hour" -> "Every hour"
        "day" -> "Every day"
        "week" -> "Every week"
        "month" -> "Every month"
        "year" -> "Every year"
        else -> "Custom repeat"
    }
}

fun selectedDaysShortFromParsed(parsed: ScheduleResult): List<String> {
    val map = mapOf(
        "sun" to "S",
        "mon" to "M",
        "tue" to "T",
        "wed" to "W",
        "thu" to "T",
        "fri" to "F",
        "sat" to "S"
    )
    return parsed.selectedDays?.mapNotNull { map[it.lowercase(Locale.US)] } ?: emptyList()
}

fun buildSummaryLine(
    startTimeText: String,
    repeatLabel: String,
    selectedDaysShort: List<String>,
    durationText: String
): String {
    val recurrencePart = if (repeatLabel == "Every week" && selectedDaysShort.isNotEmpty()) {
        "Week on ${selectedDaysShort.joinToString(",")}"
    } else {
        repeatLabel
    }

    return "$startTimeText • $recurrencePart • $durationText"
}

data class ValveUi(
    val id: String,
    val label: String,
    val isOpen: Boolean,
    val lastChangeAgo: String,
    val manualMode: Boolean,
    val schedules: List<ScheduleUi>
)
