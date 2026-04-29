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
import com.example.agribotz.app.domain.SetLocationNav
import com.example.agribotz.app.domain.Variable
import com.example.agribotz.app.domain.VariableValue
import com.example.agribotz.app.network.GadgetVariableEventsClient
import com.example.agribotz.app.network.VariableUpdateEvent
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
    val scheduleVars = arrayOfNulls<Variable.ScheduleVar>(5)
    private var restartVar: Variable.BooleanVar? = null
    private var onlineStateVar: Variable.BooleanVar? = null

    private val variableEventsClient = GadgetVariableEventsClient()
    private var shouldListenToVariableEvents = false
    private var connectedVariableIdsKey: String? = null

    private val _isValveOpen = MutableLiveData(false)
    val isValveOpen: LiveData<Boolean> = _isValveOpen

    private val _isManualMode = MutableLiveData(false)
    val isManualMode: LiveData<Boolean> = _isManualMode

    private val _isManualModeEnabled = MutableLiveData(false)
    val isManualModeEnabled: LiveData<Boolean> = _isManualModeEnabled

    private val _confirmDeleteScheduleDialog = MutableLiveData<Int?>()
    val confirmDeleteScheduleDialog: LiveData<Int?> = _confirmDeleteScheduleDialog

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
    var gmtVar: Variable.StringVar? = null
    private var deepSleepVar: Variable.BooleanVar? = null

    private val _isDeepSleepOn = MutableLiveData(false)
    val isDeepSleepOn: LiveData<Boolean> = _isDeepSleepOn

    val sleepModeText: LiveData<String> = _isDeepSleepOn.map {
        if (it == true) "On" else "Off"
    }

    private val _refreshRateHours = MutableLiveData(0f)
    val refreshRateHours: LiveData<Float> = _refreshRateHours

    private val _gmtText = MutableLiveData("GMT+00:00")
    val gmtText: LiveData<String> = _gmtText

    private val _openRenameDialog = MutableLiveData<GadgetCardUi?>()
    val openRenameDialog: LiveData<GadgetCardUi?> = _openRenameDialog

    private val _navigateToMap = MutableLiveData<SetLocationNav?>()
    val navigateToMap: LiveData<SetLocationNav?> = _navigateToMap

    private val _navigateToSetLocation = MutableLiveData<SetLocationNav?>()
    val navigateToSetLocation: LiveData<SetLocationNav?> = _navigateToSetLocation

    private val _openEditScheduleDialog = MutableLiveData<Int?>()
    val openEditScheduleDialog: LiveData<Int?> = _openEditScheduleDialog

    private val _openEditWakeupRateDialog = MutableLiveData<Pair<String, Float>?>()
    val openEditWakeupRateDialog: LiveData<Pair<String, Float>?> = _openEditWakeupRateDialog

    private val _openEditGmtDialog = MutableLiveData<Pair<String, String>?>()
    val openEditGmtDialog: LiveData<Pair<String, String>?> = _openEditGmtDialog

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
        val token = _token ?: return
        viewModelScope.launch {
            try {
                _apiStatus.value = ApiStatus.LOADING

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
     * SSE VARIABLE EVENTS
     * =============================== */

    fun startVariableEvents() {
        shouldListenToVariableEvents = true
        connectVariableEventsIfPossible()
    }

    fun stopVariableEvents() {
        shouldListenToVariableEvents = false
        connectedVariableIdsKey = null
        variableEventsClient.disconnect()
    }

    private fun connectVariableEventsIfPossible() {
        if (!shouldListenToVariableEvents) return

        val token = _token ?: return

        val variableIds = listOfNotNull(
            onlineStateVar?._id,
            valveStateVar?._id
        )

        if (variableIds.isEmpty()) return

        val variableIdsKey = variableIds.sorted().joinToString(",")

        if (connectedVariableIdsKey == variableIdsKey) return

        variableEventsClient.disconnect()
        connectedVariableIdsKey = variableIdsKey

        variableEventsClient.connect(
            token = token,
            variableIds = variableIds,
            onVariableUpdate = { event ->
                viewModelScope.launch {
                    applyVariableUpdateEvent(event)
                }
            },
            onError = { error ->
                connectedVariableIdsKey = null
                Log.e("GadgetManagerViewModel", "Variable events connection failed", error)
            }
        )
    }

    private fun applyVariableUpdateEvent(event: VariableUpdateEvent) {
        when {
            event.variableId == valveStateVar?._id || event.variableName == "solenoid1State" -> {
                valveStateVar = valveStateVar?.copy(
                    value = event.value,
                    updatedAt = event.updatedAt,
                    timeAgo = null
                )

                _isValveOpen.value = event.value
            }

            event.variableId == onlineStateVar?._id || event.variableName == "isOnline" -> {
                onlineStateVar = onlineStateVar?.copy(
                    value = event.value,
                    updatedAt = event.updatedAt,
                    timeAgo = null
                )

                applyOnlineUpdate(
                    isOnline = event.value,
                    updatedAt = event.updatedAt
                )
            }
        }
    }

    private fun applyOnlineUpdate(isOnline: Boolean, updatedAt: String?) {
        val current = _gadgetHeader.value ?: return

        _gadgetHeader.value = current.copy(
            isOnline = isOnline,
            onlineAt = if (isOnline) updatedAt else null,
            offlineAt = if (!isOnline) updatedAt else null,
            onlineTimeAgo = if (isOnline) "Online now" else null,
            offlineTimeAgo = if (!isOnline) "Offline now" else null
        )
    }

    /* ===============================
     * PARSING
     * =============================== */

    private fun parseGadget(gadget: Gadget) {
        parseHeader(gadget)
        parseVariables(gadget.variables)
        connectVariableEventsIfPossible()
    }

    private fun parseHeader(gadget: Gadget) {
        val isOnlineVar = gadget.variables
            .firstOrNull { it is Variable.BooleanVar && it.name == "isOnline" } as Variable.BooleanVar?

        onlineStateVar = isOnlineVar

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
            serialNumber = gadget.serialNumber,
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
        restartVar = null
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
            val scheduleVar = scheduleVars[index]

            if (scheduleVar?.value != null) {
                mapScheduleToUi(scheduleVar, index + 1)
            } else {
                ScheduleUi(
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
        }

        _isManualModeEnabled.value = scheduleVars.any { it?.value != null }
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
            "espRestart" -> {
                restartVar = v
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
            _refreshRateHours.value = v.value
        }
    }

    private fun handleString(v: Variable.StringVar) {
        if (v.name == "gmtZone") {
            gmtVar = v
            _gmtText.value = v.value
        }
    }

    /* ===============================
     * ACTIONS
     * =============================== */

    fun onValveToggleChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (!buttonView.isPressed) {
            _isValveOpen.value = isChecked
            return
        }

        valveStateVar?.let {
            updateBoolean(
                buttonView = buttonView,
                v = it,
                value = isChecked,
                onSuccess = { _isValveOpen.value = isChecked }
            )
        }
    }

    fun onManualModeChangedListener(buttonView: CompoundButton, isChecked: Boolean) {
        if (!buttonView.isPressed) {
            _isManualMode.value = isChecked
            return
        }

        manualModeVar?.let { manualVar ->
            updateBoolean(
                buttonView = buttonView,
                v = manualVar,
                value = isChecked,
                onSuccess = {
                    _isManualMode.value = isChecked

                    if (!isChecked) {
                        if (_isValveOpen.value == true) {
                            valveStateVar?.let { valveVar ->
                                updateBoolean(
                                    v = valveVar,
                                    value = false,
                                    onSuccess = { _isValveOpen.value = false }
                                )
                            }
                        } else {
                            _isValveOpen.value = false
                        }
                    }
                }
            )
        }
    }

    fun onEditSchedule(index: Int) {
        _openEditScheduleDialog.value = index
    }

    fun onEditScheduleDialogConsumed() {
        _openEditScheduleDialog.value = null
    }

    fun onDeleteSchedule(index: Int) {
        _confirmDeleteScheduleDialog.value = index
    }

    fun onDeleteScheduleDialogConsumed() {
        _confirmDeleteScheduleDialog.value = null
    }

    fun deleteSchedule(index: Int) {
        val token = _token
        if (token.isNullOrBlank()) {
            _apiStatus.value = ApiStatus.ERROR
            _errorServerMessage.value = "Missing access token"
            return
        }

        val scheduleVar = scheduleVars.getOrNull(index) ?: return

        viewModelScope.launch {
            try {
                _apiStatus.value = ApiStatus.LOADING

                when (val result = repository.updateVariable(
                    token,
                    scheduleVar._id,
                    VariableValue.ScheduleVal(null)
                )) {
                    is ApiResult.Success -> {
                        loadGadget()
                    }

                    is ApiResult.Error -> {
                        handleError(result, "Deleting schedule failed")
                    }
                }
            } catch (e: Exception) {
                _apiStatus.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("GadgetManagerViewModel", "deleteSchedule failed", e)
            }
        }
    }

    fun onDeepSleepChangedListener(buttonView: CompoundButton, isChecked: Boolean) {
        if (!buttonView.isPressed) {
            _isDeepSleepOn.value = isChecked
            return
        }

        deepSleepVar?.let {
            updateBoolean(
                buttonView = buttonView,
                v = it,
                value = isChecked,
                onSuccess = { _isDeepSleepOn.value = isChecked }
            )
        }
    }

    fun onEditRefreshRate() {
        refreshRateVar?.let {
            _openEditWakeupRateDialog.value = Pair(it._id, it.value)
        }
    }

    fun onEditWakeupRateDialogConsumed() {
        _openEditWakeupRateDialog.value = null
    }

    fun onEditGmt() {
        gmtVar?.let {
            _openEditGmtDialog.value = Pair(it._id, it.value)
        }
    }

    fun onEditGmtDialogConsumed() {
        _openEditGmtDialog.value = null
    }

    fun onRestartClicked() {
        restartVar?.let {
            updateBoolean(
                v = it,
                value = true,
                onSuccess = { loadGadget() }
            )
        }
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

    private fun updateBoolean(
        buttonView: CompoundButton,
        v: Variable.BooleanVar,
        value: Boolean,
        onSuccess: (() -> Unit)? = null
    ) {
        val token = _token ?: return

        buttonView.isEnabled = false

        viewModelScope.launch {
            try {
                _apiStatus.value = ApiStatus.LOADING
                when (val result = repository.updateVariable(token, v._id, VariableValue.Bool(value))){
                    is ApiResult.Success -> {
                        _apiStatus.value = ApiStatus.DONE
                        onSuccess?.invoke()
                    }

                    is ApiResult.Error -> {
                        handleError(result, "Changing Status failed")
                        buttonView.isChecked = !value
                    }
                }
            } catch (e: Exception) {
                _apiStatus.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("GadgetManagerViewModel", "updateBoolean failed", e)

                buttonView.isChecked = !value
            } finally {
                buttonView.isEnabled = true
            }
        }
    }

    private fun updateBoolean(
        v: Variable.BooleanVar,
        value: Boolean,
        onSuccess: (() -> Unit)? = null
    ) {
        val token = _token ?: return

        viewModelScope.launch {
            try {
                _apiStatus.value = ApiStatus.LOADING
                when (val result = repository.updateVariable(token, v._id, VariableValue.Bool(value))) {
                    is ApiResult.Success -> {
                        _apiStatus.value = ApiStatus.DONE
                        onSuccess?.invoke()
                    }

                    is ApiResult.Error -> {
                        handleError(result, "Changing Status failed")
                    }
                }
            } catch (e: Exception) {
                _apiStatus.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("GadgetManagerViewModel", "updateBoolean failed", e)
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

    override fun onCleared() {
        variableEventsClient.disconnect()
        super.onCleared()
    }
}