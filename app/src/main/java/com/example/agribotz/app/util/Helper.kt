package com.example.agribotz.app.util

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