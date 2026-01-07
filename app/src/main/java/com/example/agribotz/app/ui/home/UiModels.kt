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

    val statusLineResId: Int
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
     * PRIMARY STATUS (ICON + timeAgo)
     * ============================== */

    /**
     * Priority:
     * 1) Terminated
     * 2) Inactive
     * 3) Active
     */
    val primaryStatusResId: Int
        get() = when {
            isTerminated -> R.string.Terminated
            !isActive -> R.string.Inactive
            else -> R.string.Active
        }

    val primaryStatusTimeAgo: String?
        get() = when {
            isTerminated -> terminatedTimeAgo
            !isActive -> inactiveTimeAgo
            else -> activeTimeAgo
        }

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

    val onlineStateResId: Int
        get() = if (isOnline) R.string.Online else R.string.Offline

    val onlineStateTimeAgo: String?
        get() = if (isOnline) onlineTimeAgo else offlineTimeAgo

    /* ==============================
     * FULL STATUS TEXT (for dialogs)
     * ============================== */

    val terminatedFullText: String?
        get() = terminatedAtFormatted?.let {
            "Terminated since $it"
        }

    val inactiveFullText: String?
        get() = deactivatedAtFormatted?.let {
            "Deactivated since $it"
        }

    val activeFullText: String?
        get() = activatedAtFormatted?.let {
            "Activated since $it"
        }

    val onlineFullText: String?
        get() = onlineAtFormatted?.let {
            "Online since $it"
        }

    val offlineFullText: String?
        get() = offlineAtFormatted?.let {
            "Offline since $it"
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
