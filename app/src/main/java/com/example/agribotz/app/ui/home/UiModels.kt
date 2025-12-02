package com.example.agribotz.app.ui.home

import com.example.agribotz.R
import com.example.agribotz.app.util.toDisplayDate

data class SiteUi(
    val id: String,
    val name: String,
    val createdAt: String?,
    val isActive: Boolean,
    val activatedAt: String?,
    val deactivatedAt: String?,
    val isTerminated: Boolean,
    val terminatedAt: String?,
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

    // Choose the correct label
    val statusLineResId: Int
        get() = when {
            isTerminated -> R.string.Terminated_Since
            isActive -> R.string.Activated_Since
            else -> R.string.Deactivated_Since
        }

    // Choose the correct date
    val statusDate: String?
        get() = when {
            isTerminated -> terminatedAtFormatted
            isActive -> activatedAtFormatted
            else -> deactivatedAtFormatted
        }
}

fun SiteUi.toDomain(): com.example.agribotz.app.domain.Site {
    return com.example.agribotz.app.domain.Site(
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
    val isActive: Boolean,
    val isOnline: Boolean,
    val isTerminated: Boolean,
    val numberOfValves: Int,
    val numberOfSensors: Int,
    val statusLine: String? = ""
)

data class ScheduleUi(
    val index: Int,               // 1..5
    val startTime: String,        // "HH:mm"
    val durationMin: Int,
    val daysMask: Int,            // bitmask for days, or list if you prefer
    val enabled: Boolean
)

data class ValveUi(
    val id: String,
    val label: String,            // "Valve 1"
    val isOpen: Boolean,
    val lastChangeAgo: String,    // "13 minutes ago"
    val manualMode: Boolean,
    val schedules: List<ScheduleUi>
)
