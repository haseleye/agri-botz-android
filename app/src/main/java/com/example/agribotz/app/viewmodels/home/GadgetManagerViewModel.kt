package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.domain.Gadget
import com.example.agribotz.app.domain.ScheduleValue
import com.example.agribotz.app.domain.Variable
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.ui.home.GadgetCardUi
import com.example.agribotz.app.ui.home.ScheduleUi
import com.example.agribotz.app.ui.home.mapScheduleToUi
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.util.asValveKey
import kotlinx.coroutines.launch

class GadgetManagerViewModel(
    private val repository: Repository,
    private val prefManager: PreferencesManager,
    private val gadgetId: String
) : ViewModel() {

    /* ===============================
     * COMMON STATE
     * =============================== */

    private val _apiStatus = MutableLiveData<ApiStatus>()
    val apiStatus: LiveData<ApiStatus> = _apiStatus

    private val _errorServerMessageRes = MutableLiveData<Int?>()
    val errorServerMessageRes: LiveData<Int?> = _errorServerMessageRes

    private val _errorServerMessage = MutableLiveData<String?>()
    val errorServerMessage: LiveData<String?> = _errorServerMessage

    private val _eventTransError = MutableLiveData<Int?>()
    val eventTransError: LiveData<Int?> = _eventTransError

    /* ===============================
     * GADGET HEADER
     * =============================== */

    private val _gadgetHeader = MutableLiveData<GadgetCardUi>()
    val gadgetHeader: LiveData<GadgetCardUi> = _gadgetHeader

    val isOnline: LiveData<Boolean> = _gadgetHeader.map { it?.isOnline == true }

    private val _showStatusDetails = MutableLiveData<Pair<Int, String>?>()
    val showStatusDetails: LiveData<Pair<Int, String>?> = _showStatusDetails

    /* ===============================
     * VALVE STATE
     * =============================== */

    private var valveStateVar: Variable.BooleanVar? = null
    private var manualModeVar: Variable.BooleanVar? = null
    private val scheduleVars = arrayOfNulls<Variable.ScheduleVar>(5)

    private val _isValveOpen = MutableLiveData<Boolean>(false)
    val isValveOpen: LiveData<Boolean> = _isValveOpen

    private val _isManualMode = MutableLiveData<Boolean>(false)
    val isManualMode: LiveData<Boolean> = _isManualMode

    val canToggleValve: LiveData<Boolean> = _isManualMode.map { it == true }

    val valveStateText: LiveData<String> = _isValveOpen.map {
        if (it == true) "Open" else "Closed"
    }

    val manualModeText: LiveData<String> = _isManualMode.map {
        if (it == true) "On" else "Off"
    }

    /* ===============================
     * SCHEDULES
     * =============================== */

    private val _schedules = MutableLiveData<List<ScheduleUi>>(emptyList())
    val schedules: LiveData<List<ScheduleUi>> = _schedules

    /* ===============================
     * SETTINGS
     * =============================== */

    private var refreshRateVar: Variable.FloatVar? = null
    private var gmtVar: Variable.StringVar? = null
    private var deepSleepVar: Variable.BooleanVar? = null

    private val _isDeepSleepOn = MutableLiveData<Boolean>(false)
    val isDeepSleepOn: LiveData<Boolean> = _isDeepSleepOn

    val refreshRateText: LiveData<String> = _isDeepSleepOn.map {
        refreshRateVar?.value?.let { hours -> "$hours hours" } ?: "Not set"
    }

    val gmtText: LiveData<String> = _isDeepSleepOn.map {
        gmtVar?.value ?: "GMT+00:00"
    }

    /* ===============================
     * INIT
     * =============================== */

    private var _token: String? = null

    init {
        _token = prefManager.getAccessToken()
        //loadGadget()
    }

    fun onLoad() {
        loadGadget()
    }

    private fun loadGadget() {
        viewModelScope.launch {
            _apiStatus.value = ApiStatus.LOADING

            val token = _token
            if (token.isNullOrBlank()) {
                _apiStatus.value = ApiStatus.ERROR
                _errorServerMessage.value = "Missing access token"
                return@launch
            }

            when (val result = repository.gadgetInfo(token, gadgetId)) {
                is ApiResult.Success -> {
                    parseGadget(result.data.message.gadgetInfo)
                    _apiStatus.value = ApiStatus.DONE
                }

                is ApiResult.Error -> {
                    handleError(result)
                }
            }
        }
    }

    /* ===============================
     * PARSING
     * =============================== */

    private fun parseGadget(gadget: Gadget) {
        parseHeader(gadget)
        parseVariables(gadget.variables)
    }

    private fun parseHeader(gadget: Gadget) {
        _gadgetHeader.value = GadgetCardUi(
            id = gadget.id,
            name = gadget.name,
            hasGps = gadget.gps != null,
            gps = gadget.gps,

            isOnline = findBool(gadget, "isOnline"),
            onlineAt = null,
            offlineAt = null,
            onlineTimeAgo = findTimeAgo(gadget, "isOnline"),
            offlineTimeAgo = findTimeAgo(gadget, "isOnline"),

            isActive = findBool(gadget, "isActive"),
            activatedAt = null,
            deactivatedAt = null,
            activeTimeAgo = findTimeAgo(gadget, "isActive"),
            inactiveTimeAgo = findTimeAgo(gadget, "isActive"),

            isTerminated = findBool(gadget, "isTerminated"),
            terminatedAt = null,
            terminatedTimeAgo = findTimeAgo(gadget, "isTerminated"),

            numberOfValves = gadget.numberOfValves ?: 0,
            numberOfSensors = gadget.numberOfSensors ?: 0
        )
    }

    private fun parseVariables(vars: List<Variable>) {
        // reset parsed holders before re-parse
        valveStateVar = null
        manualModeVar = null
        refreshRateVar = null
        gmtVar = null
        deepSleepVar = null
        for (i in scheduleVars.indices) scheduleVars[i] = null

        vars.forEach { variable ->
            when (variable) {
                is Variable.BooleanVar -> handleBoolean(variable)
                is Variable.ScheduleVar -> handleSchedule(variable)
                is Variable.FloatVar -> handleFloat(variable)
                is Variable.StringVar -> handleString(variable)
                else -> Unit
            }
        }

        _schedules.value = (0 until 5).map { index ->
            scheduleVars[index]?.let { mapScheduleToUi(it, index + 1) }
                ?: ScheduleUi(
                    index = index + 1,
                    isSet = false,
                    startEpochSec = 0L,
                    startDateText = "Not Set",
                    startTimeText = "Not Set",
                    durationSec = 0,
                    durationText = "0m",
                    repeatLabel = "Does not repeat",
                    daysMask = 0,
                    selectedDaysShort = emptyList(),
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
    }

    private fun handleBoolean(v: Variable.BooleanVar) {
        when (v.name) {
            "solenoid1State" -> {
                valveStateVar = v
                _isValveOpen.value = v.value
            }
            "solenoid1Manual" -> {
                manualModeVar = v
                _isManualMode.value = v.value
            }
            "deepSleep" -> {
                deepSleepVar = v
                _isDeepSleepOn.value = v.value
            }
        }
    }

    private fun handleSchedule(v: Variable.ScheduleVar) {
        val key = v.name.asValveKey() ?: return
        val slot = key.slot ?: return

        if (key.index == 1 && slot in 1..5) {
            scheduleVars[slot - 1] = v
        }
    }

    private fun handleFloat(v: Variable.FloatVar) {
        if (v.name == "dailyOnlineRefreshes") {
            refreshRateVar = v
        }
    }

    private fun handleString(v: Variable.StringVar) {
        if (v.name == "gmtZone") {
            gmtVar = v
        }
    }

    /* ===============================
     * ACTIONS
     * =============================== */

    fun onValveToggle(isChecked: Boolean) {
        valveStateVar?.let {
            updateBoolean(it, isChecked)
            _isValveOpen.value = isChecked
        }
    }

    fun onManualModeChanged(isChecked: Boolean) {
        manualModeVar?.let {
            updateBoolean(it, isChecked)
            _isManualMode.value = isChecked
        }
    }

    fun onEditSchedule(index: Int) {
        // TODO: open schedule editor dialog
    }

    fun onDeleteSchedule(index: Int) {
        scheduleVars.getOrNull(index)?.let { updateSchedule(it, null) }
    }

    fun onDeepSleepChanged(isChecked: Boolean) {
        deepSleepVar?.let {
            updateBoolean(it, isChecked)
            _isDeepSleepOn.value = isChecked
        }
    }

    fun onEditRefreshRate() {
        // TODO: open refresh rate picker dialog
    }

    fun onEditGmt() {
        // TODO: open GMT picker dialog
    }

    fun onRestartClicked() {
        // TODO: restart gadget command
    }

    fun onRenameGadget() {
        // TODO: open rename dialog
    }

    fun onGpsClicked() {
        // TODO: open map/navigation
    }

    fun onGpsLongPressed(): Boolean {
        // TODO: show gps coordinates tooltip/dialog
        return true
    }

    fun onStatusIconClicked() {
        val gadget = _gadgetHeader.value ?: return

        val statusResId = gadget.statusResId
        val statusDate = gadget.statusDate

        if (statusResId != null && !statusDate.isNullOrBlank()) {
            _showStatusDetails.value = Pair(statusResId, statusDate)
        }
    }

    fun onStatusDetailsShown() {
        _showStatusDetails.value = null
    }

    fun onTransErrorCompleted() {
        _eventTransError.value = null
    }

    /* ===============================
     * UPDATE HELPERS
     * =============================== */

    private fun updateBoolean(v: Variable.BooleanVar, value: Boolean) {
        val token = _token ?: return
        viewModelScope.launch {
            repository.updateVariable(
                token,
                v._id,
                v.copy(value = value)
            )
        }
    }

    private fun updateSchedule(v: Variable.ScheduleVar, value: ScheduleValue?) {
        val token = _token ?: return
        viewModelScope.launch {
            repository.updateVariable(
                token,
                v._id,
                v.copy(value = value ?: ScheduleValue(frm = 0L, to = 0L, len = 0, msk = 0))
            )
        }
    }

    /* ===============================
     * ERROR HANDLING
     * =============================== */

    private fun handleError(error: ApiResult.Error) {
        _apiStatus.value = ApiStatus.ERROR
        _errorServerMessage.value = error.userMessageString
        _errorServerMessageRes.value = error.userMessageKey
    }

    /* ===============================
     * SMALL HELPERS
     * =============================== */

    private fun variableName(v: Variable): String = when (v) {
        is Variable.BooleanVar -> v.name
        is Variable.IntegerVar -> v.name
        is Variable.FloatVar -> v.name
        is Variable.StringVar -> v.name
        is Variable.ScheduleVar -> v.name
    }

    private fun variableTimeAgo(v: Variable): String? = when (v) {
        is Variable.BooleanVar -> v.timeAgo
        is Variable.IntegerVar -> v.timeAgo
        is Variable.FloatVar -> v.timeAgo
        is Variable.StringVar -> v.timeAgo
        is Variable.ScheduleVar -> v.timeAgo
    }

    private fun findBool(gadget: Gadget, name: String): Boolean =
        gadget.variables
            .filterIsInstance<Variable.BooleanVar>()
            .firstOrNull { it.name == name }
            ?.value ?: false

    private fun findTimeAgo(gadget: Gadget, name: String): String? =
        gadget.variables
            .firstOrNull { variableName(it) == name }
            ?.let { variableTimeAgo(it) }
}
