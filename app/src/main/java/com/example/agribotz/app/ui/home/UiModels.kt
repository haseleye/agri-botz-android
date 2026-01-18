package com.example.agribotz.app.ui.home

import com.example.agribotz.R
import com.example.agribotz.app.domain.GPS
import com.example.agribotz.app.domain.Site
import com.example.agribotz.app.util.toDisplayDate

data class SiteUi(
    val id: String,
    val name: String,

    // Creation state
    val createdAt: String?,
    val createdAgo: String?,

    // Activation state
    val isActive: Boolean,
    val activatedAt: String?,
    val deactivatedAt: String?,
    val activatedAgo: String?,
    val deactivatedAgo: String?,

    // Termination state
    val isTerminated: Boolean,
    val terminatedAt: String?,
    val terminatedAgo: String?,

    // Hardware info
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

    // Online state
    val isOnline: Boolean,
    val onlineAt: String?,
    val offlineAt: String?,
    val onlineTimeAgo: String?,
    val offlineTimeAgo: String?,

    // Activation state
    val isActive: Boolean,
    val activatedAt: String?,
    val deactivatedAt: String?,
    val activeTimeAgo: String?,
    val inactiveTimeAgo: String?,

    // Termination state
    val isTerminated: Boolean,
    val terminatedAt: String?,
    val terminatedTimeAgo: String?,

    // Hardware info
    val numberOfValves: Int,
    val numberOfSensors: Int
) {

    val canOpenMap: Boolean
        get() = hasGps && gps != null

    /* ==============================
     * Formatted dates (for drill-down)
     * ============================== */

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

    /* ==============================
     * ONLINE / OFFLINE (SECONDARY)
     * ============================== */

    /**
     * Online/Offline is shown ONLY if:
     * - Not terminated
     * - Active
     */
    val showOnlineState: Boolean
        get() = !isTerminated && isActive

    val onlineStateTimeAgo: String?
        get() = if (isOnline) onlineTimeAgo else offlineTimeAgo

    val statusResId: Int?
        get() = when {
            isTerminated -> R.string.Terminated_Since
            isActive -> R.string.Activated_Since
            !isActive -> R.string.Deactivated_Since
            isOnline -> R.string.Online_Since
            else -> R.string.Offline_Since
        }

    val statusDate: String?
        get() = when {
            isTerminated -> terminatedAtFormatted
            isActive -> activatedAtFormatted
            !isActive -> deactivatedAtFormatted
            isOnline -> onlineAtFormatted
            else -> offlineAtFormatted
        }
}

data class ScheduleUi(
    val index: Int,
    val startTime: String,
    val durationMin: Int,
    val daysMask: Int,
    val enabled: Boolean
)

data class ValveUi(
    val id: String,
    val label: String,
    val isOpen: Boolean,
    val lastChangeAgo: String,
    val manualMode: Boolean,
    val schedules: List<ScheduleUi>
)
