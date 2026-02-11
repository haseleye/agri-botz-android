package com.example.agribotz.app.util

import com.example.agribotz.app.domain.ScheduleResult
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
    val monthsOfYear = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

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
            val monthString = if (monthIndex in 0..11) monthsOfYear[monthIndex] else null
            ScheduleResult("year", dayOfMonth = dayOfMonth, month = monthString)
        }

        else -> ScheduleResult("unknown")
    }
}