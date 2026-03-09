package com.example.agribotz.app.domain

import com.example.agribotz.app.ui.home.ScheduleUi
import com.google.gson.annotations.SerializedName

enum class UserRole() {
    @SerializedName("ADMIN") ADMIN,
    @SerializedName("USER") USER,
    @SerializedName("AGENT") AGENT
}

enum class VarCategory() {
    @SerializedName("SENSOR") SENSOR,
    @SerializedName("IRRIGATION") IRRIGATION,
    @SerializedName("INDICATOR") INDICATOR,
    @SerializedName("COMMAND") COMMAND,
    @SerializedName("SYSTEM") SYSTEM,
    @SerializedName("SETTING") SETTING
}

enum class ApiStatus {
    LOADING,
    ERROR,
    DONE
}

enum class ScheduleRepeatMode(val label: String) {
    NONE("Does not repeat"),
    HOUR("Hour"),
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year");

    companion object {
        fun fromScheduleUi(schedule: ScheduleUi?): ScheduleRepeatMode {
            if (schedule == null || !schedule.isSet || schedule.repeatLabel == "Does not repeat") {
                return NONE
            }

            return when (schedule.repeatLabel) {
                "Every hour" -> HOUR
                "Every day" -> DAY
                "Every week" -> WEEK
                "Every month" -> MONTH
                "Every year" -> YEAR
                else -> NONE
            }
        }
    }
}


