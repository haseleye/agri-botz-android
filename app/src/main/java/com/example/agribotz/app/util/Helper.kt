package com.example.agribotz.app.util

import com.example.agribotz.app.domain.ScheduleResult
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.text.DateFormatSymbols
import android.content.Context
import android.content.res.Configuration
import com.example.agribotz.R

data class ValveKey(val index: Int, val slot: Int?) // slot null for state/manual

fun String.asValveKey(): ValveKey? {
    // "solenoid1Scheduler3" -> index=1, slot=3; "manualSwitch2" -> index=2, slot=null
    val sch = Regex("""solenoid(\d)Scheduler(\d)""").matchEntire(this)
    if (sch != null) return ValveKey(sch.groupValues[1].toInt(), sch.groupValues[2].toInt())

    val st = Regex("""solenoid(\d)State""").matchEntire(this)
    if (st != null) return ValveKey(st.groupValues[1].toInt(), null)

    val man = Regex("""manualSwitch(\d)""").matchEntire(this)
    if (man != null) return ValveKey(man.groupValues[1].toInt(), null)

    return null
}

fun String.asSensorScheduleSlot(): Int? {
    // "sensorScheduler3" -> slot=3
    val sch = Regex("""sensorScheduler(\d)""").matchEntire(this)
    if (sch != null) return sch.groupValues[1].toInt()

    return null
}

fun String.toDisplayDate(): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(this)
        val formatter = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        formatter.format(date!!)
    } catch (e: Exception) {
        this // fallback to raw string if parsing fails
    }
}

fun parseScheduleMask(mask: Long): ScheduleResult {
    // Extract base fields using unsigned shifts
    val scheduleUnit = ((mask shr 30) and 0x3).toInt()
    val scheduleType = ((mask shr 26) and 0xF).toInt()

    val daysOfWeek = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")
    val localizedMonths = DateFormatSymbols(Locale.getDefault()).shortMonths

    return when (scheduleType) {
        0 -> ScheduleResult("does not repeat")

        1 -> { // FixedDelta
            val unitString = if (scheduleUnit == 2) "hour" else "day"
            ScheduleResult(unitString)
        }

        2 -> { // Weekly
            val weekMask = (mask and 0xFF).toInt()
            val selectedDays = mutableListOf<String>()
            for (i in 0..6) {
                if ((weekMask and (1 shl i)) != 0) {
                    selectedDays.add(daysOfWeek[i])
                }
            }
            ScheduleResult("week", selectedDays = selectedDays)
        }

        3 -> { // Monthly
            val dayOfMonth = (mask and 0xFF).toInt()
            ScheduleResult("month", dayOfMonth = dayOfMonth)
        }

        4 -> { // Yearly
            val dayOfMonth = (mask and 0xFF).toInt()
            val monthIndex = ((mask shr 8) and 0xFF).toInt()

            val monthString = if (monthIndex in 0..11) {
                localizedMonths[monthIndex]
                    ?.replace(".", "")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            } else {
                null
            }

            ScheduleResult("year", dayOfMonth = dayOfMonth, month = monthString)
        }

        else -> ScheduleResult("unknown")
    }
}

object GmtZoneLocalizer {

    @JvmStatic
    fun localize(context: Context, backendValue: String?): String {
        val cleanBackendValue = backendValue?.trim().orEmpty()

        if (cleanBackendValue.isBlank()) {
            return ""
        }

        val displayTimeZones = context.resources
            .getStringArray(R.array.gmt_cities_list)
            .toList()

        val englishConfiguration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        }

        val englishResources = context
            .createConfigurationContext(englishConfiguration)
            .resources

        val backendTimeZones = englishResources
            .getStringArray(R.array.gmt_cities_list)
            .toList()

        val index = backendTimeZones.indexOf(cleanBackendValue)

        return if (index in displayTimeZones.indices) {
            displayTimeZones[index]
        } else {
            cleanBackendValue
        }
    }
}

object TimeAgoLocalizer {

    private const val MINUTE_MS = 60 * 1000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS
    private const val WEEK_MS = 7 * DAY_MS
    private const val MONTH_MS = 30 * DAY_MS
    private const val QUARTER_MS = 90 * DAY_MS
    private const val YEAR_MS = 365 * DAY_MS

    // "now" only forces the binding to re-evaluate when the ViewModel ticks, the value itself is not used
    @JvmStatic
    fun localize(
        context: Context,
        label: String?,
        updatedAt: String?,
        fallback: String?,
        now: Long?
    ): String {
        val updatedAtMs = parseUpdatedAt(updatedAt) ?: return fallback.orEmpty()

        val elapsed = (System.currentTimeMillis() - updatedAtMs).coerceAtLeast(0L)
        val resources = context.resources

        // Same thresholds as the backend timeAgo(), so the shown text matches what the API returns
        val relative = when {
            elapsed < HOUR_MS -> {
                val minutes = (elapsed / MINUTE_MS).toInt()
                resources.getQuantityString(R.plurals.Time_Ago_Minutes, minutes, minutes)
            }

            elapsed < DAY_MS -> {
                val hours = (elapsed / HOUR_MS).toInt()
                resources.getQuantityString(R.plurals.Time_Ago_Hours, hours, hours)
            }

            elapsed < MONTH_MS -> {
                val days = (elapsed / DAY_MS).toInt()
                resources.getQuantityString(R.plurals.Time_Ago_Days, days, days)
            }

            elapsed < QUARTER_MS -> {
                val weeks = (elapsed / WEEK_MS).toInt()
                resources.getQuantityString(R.plurals.Time_Ago_Weeks, weeks, weeks)
            }

            elapsed < YEAR_MS -> {
                val months = (elapsed / MONTH_MS).toInt()
                resources.getQuantityString(R.plurals.Time_Ago_Months, months, months)
            }

            else -> {
                val years = (elapsed / YEAR_MS).toInt()
                resources.getQuantityString(R.plurals.Time_Ago_Years, years, years)
            }
        }

        return if (label.isNullOrBlank()) relative else "$label\n$relative"
    }

    private fun parseUpdatedAt(updatedAt: String?): Long? {
        if (updatedAt.isNullOrBlank()) return null

        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            parser.parse(updatedAt)?.time
        } catch (e: Exception) {
            null
        }
    }
}

object LastUpdateLocalizer {

    @JvmStatic
    fun localize(context: Context, updatedAt: String?): String? {
        if (updatedAt.isNullOrBlank()) return null

        return context.getString(R.string.Last_Update_At, updatedAt.toDisplayDate())
    }
}
