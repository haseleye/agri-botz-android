package com.example.agribotz.app.viewmodels.home

import android.util.Log
import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.domain.Gadget
import com.example.agribotz.app.domain.ScheduleValue
import com.example.agribotz.app.domain.SetLocationNav
import com.example.agribotz.app.domain.Variable
import com.example.agribotz.app.domain.VariableValue
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

    private val _isValveOpen = MutableLiveData(false)
    val isValveOpen: LiveData<Boolean> = _isValveOpen

    private val _isManualMode = MutableLiveData(false)
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

    private val _isDeepSleepOn = MutableLiveData(false)
    val isDeepSleepOn: LiveData<Boolean> = _isDeepSleepOn

    val sleepModeText: LiveData<String> = _isDeepSleepOn.map {
        if (it == true) "On" else "Off"
    }

    val refreshRateText: LiveData<String> = _isDeepSleepOn.map {
        refreshRateVar?.value?.let { hours -> "Each $hours hours" } ?: "Each 0.0 hours"
    }

    val gmtText: LiveData<String> = _isDeepSleepOn.map {
        gmtVar?.value ?: "GMT+00:00"
    }

    private val _openRenameDialog = MutableLiveData<GadgetCardUi?>()
    val openRenameDialog: LiveData<GadgetCardUi?> = _openRenameDialog

    private val _navigateToMap = MutableLiveData<SetLocationNav?>()
    val navigateToMap: LiveData<SetLocationNav?> = _navigateToMap

    private val _navigateToSetLocation = MutableLiveData<SetLocationNav?>()
    val navigateToSetLocation: LiveData<SetLocationNav?> = _navigateToSetLocation


    /* ===============================
     * INIT
     * =============================== */

    private var _token: String? = null

    init {
        _token = prefManager.getAccessToken()
    }

    fun onLoad() {
        loadGadget()
    }

    private fun loadGadget() {
        viewModelScope.launch {
            try {
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
                        handleError(result, "Loading failed")
                    }
                }
            } catch (e: Exception) {
                _apiStatus.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("GadgetManagerViewModel", "Loading failed with exception", e)
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
        val isOnlineVar = gadget.variables
            .firstOrNull { it is Variable.BooleanVar && it.name == "isOnline" } as Variable.BooleanVar?

        val isActiveVar = gadget.variables
            .firstOrNull { it is Variable.BooleanVar && it.name == "isActive" } as Variable.BooleanVar?

        val isTerminatedVar = gadget.variables
            .firstOrNull { it is Variable.BooleanVar && it.name == "isTerminated" } as Variable.BooleanVar?

        val isOnline = isOnlineVar?.value ?: false
        val isActive = isActiveVar?.value ?: false
        val isTerminated = isTerminatedVar?.value ?: false

        _gadgetHeader.value = GadgetCardUi(
            id = gadget.id,
            name = gadget.name,
            hasGps = gadget.gps != null,
            gps = gadget.gps,

            // Online state
            isOnline = isOnline,
            onlineAt = if (isOnline) isOnlineVar.updatedAt else null,
            offlineAt = if (!isOnline) isOnlineVar?.updatedAt else null,
            onlineTimeAgo = if (isOnline) isOnlineVar.timeAgo else null,
            offlineTimeAgo = if (!isOnline) isOnlineVar?.timeAgo else null,

            // Activation state
            isActive = isActive,
            activatedAt = if (isActive) isActiveVar.updatedAt else null,
            deactivatedAt = if (!isActive) isActiveVar?.updatedAt else null,
            activeTimeAgo = if (isActive) isActiveVar.timeAgo else null,
            inactiveTimeAgo = if (!isActive) isActiveVar?.timeAgo else null,

            // Termination state
            isTerminated = isTerminated,
            terminatedAt = if (isTerminated) isTerminatedVar.updatedAt else null,
            terminatedTimeAgo = if (isTerminated) isTerminatedVar.timeAgo else null,

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
            "deepSleepMode" -> {
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

    fun onValveToggleChanged(buttonView: CompoundButton, isChecked: Boolean) {
        onValveToggle(isChecked)
    }

    fun onValveToggle(isChecked: Boolean) {
        valveStateVar?.let {
            updateBoolean(it, isChecked)
            _isValveOpen.value = isChecked
        }
    }

    fun onManualModeChangedListener(buttonView: CompoundButton, isChecked: Boolean) {
        onManualModeChanged(isChecked)
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

    fun onDeepSleepChangedListener(buttonView: CompoundButton, isChecked: Boolean) {
        onDeepSleepChanged(isChecked)
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
        _gadgetHeader.value?.let { gadget ->
            _openRenameDialog.value = gadget
        }
    }

    fun onRenameDialogConsumed() {
        _openRenameDialog.value = null
    }

    fun renameGadget(newName: String) {
        val gadget = _gadgetHeader.value ?: return
        val token = _token
        if (token.isNullOrBlank()) {
            _apiStatus.value = ApiStatus.ERROR
            _errorServerMessage.value = "Missing access token"
            return
        }

        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == gadget.name) return

        viewModelScope.launch {
            try {
                when (val result = repository.renameGadget(token, gadget.id, trimmed)) {
                    is ApiResult.Success -> {
                        // Re-load to reflect new name and keep behavior consistent
                        loadGadget()
                    }

                    is ApiResult.Error -> {
                        handleError(result, "Renaming gadget failed")
                    }
                }
            } catch (e: Exception) {
                _apiStatus.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("GadgetManagerViewModel", "Renaming gadget failed with exception", e)
            }
        }
    }

    fun onGpsClicked(gadget: GadgetCardUi) {
        _apiStatus.value = ApiStatus.LOADING

        if (gadget.canOpenMap) {
            _navigateToMap.value = SetLocationNav(
                gadgetId = gadget.id,
                gadgetName = gadget.name,
                gps = gadget.gps
            )
        }
        else {
            _navigateToSetLocation.value =
                SetLocationNav(
                    gadgetId = gadget.id,
                    gadgetName = gadget.name,
                    gps = null
                )
        }
    }

    fun onGpsLongPressed(gadget: GadgetCardUi): Boolean {
        _apiStatus.value = ApiStatus.LOADING

        _navigateToSetLocation.value =
            SetLocationNav(
                gadgetId = gadget.id,
                gadgetName = gadget.name,
                gps = gadget.gps
            )
        return true
    }

    fun onStatusIconClicked(@StringRes resId: Int?, date: String?) {
        if (resId != null && !date.isNullOrBlank()) {
            _showStatusDetails.value = Pair(resId, date)
        }
    }

    fun onStatusDetailsShown() {
        _showStatusDetails.value = null
    }

    fun onTransErrorCompleted() {
        _eventTransError.value = null
    }

    fun onMapNavigated() {
        _navigateToMap.value = null
    }

    fun onSetGpsNavigated() {
        _navigateToSetLocation.value = null
    }

    /* ===============================
     * UPDATE HELPERS
     * =============================== */

    private fun updateBoolean(v: Variable.BooleanVar, value: Boolean) {
        val token = _token ?: return
        viewModelScope.launch {
            try {
                repository.updateVariable(
                    token,
                    v._id,
                    VariableValue.Bool(value)
                )
            } catch (e: Exception) {
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("GadgetManagerViewModel", "updateBoolean failed", e)
            }
        }
    }

    private fun updateSchedule(v: Variable.ScheduleVar, value: ScheduleValue?) {
        val token = _token ?: return
        viewModelScope.launch {
            try {
                repository.updateVariable(
                    token,
                    v._id,
                    VariableValue.ScheduleVal(value ?: ScheduleValue(frm = 0L, to = 0L, len = 0, msk = 0))
                )
            } catch (e: Exception) {
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("GadgetManagerViewModel", "updateSchedule failed", e)
            }
        }
    }


    /* ===============================
     * ERROR HANDLING
     * =============================== */

    private fun handleError(error: ApiResult.Error, event: String) {
        Log.e("GadgetManagerViewModel", "$event: ${error.devMessage}")

        _apiStatus.value =
            if (error.userMessageKey == R.string.Error_Internet_Connection) {
                ApiStatus.ERROR
            } else {
                ApiStatus.DONE
            }

        if (!error.userMessageString.isNullOrBlank()) {
            _errorServerMessage.value = error.userMessageString
            _errorServerMessageRes.value = null
        } else {
            _errorServerMessageRes.value = error.userMessageKey
            _errorServerMessage.value = null
        }
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
