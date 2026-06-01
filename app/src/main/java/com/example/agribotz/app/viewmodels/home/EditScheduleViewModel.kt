package com.example.agribotz.app.viewmodels.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.domain.ScheduleRepeatMode
import com.example.agribotz.app.domain.ScheduleValues
import com.example.agribotz.app.domain.VariableValue
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.ui.home.ScheduleUi
import com.example.agribotz.app.util.PreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditScheduleViewModel(
    private val repository: Repository,
    private val prefManager: PreferencesManager,
    private val schedule: ScheduleUi?
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

    private val _dismissDialog = MutableLiveData(false)
    val dismissDialog: LiveData<Boolean> = _dismissDialog

    fun onDismissConsumed() {
        _dismissDialog.value = false
    }

    /* ===============================
     * INPUT STATE
     * =============================== */

    private val _variableId = MutableLiveData<String>()
    fun setVariableId(value: String) {
        _variableId.value = value
    }

    private fun isNotSet(value: String?): Boolean {
        return value.isNullOrBlank() || value.equals("Not Set", ignoreCase = true)
    }

    private val currentDate: String =
        SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
            .format(Calendar.getInstance().time)

    private val _startDate = MutableLiveData(
        if (schedule == null || !schedule.isSet || isNotSet(schedule.startDateText)) currentDate
        else schedule.startDateText
    )
    val startDate: LiveData<String?> = _startDate

    private val _startTime = MutableLiveData(
        if (schedule == null || !schedule.isSet || isNotSet(schedule.startTimeText)) "00 : 00"
        else schedule.startTimeText
    )
    val startTime: LiveData<String> = _startTime

    private val _timeZone = MutableLiveData<String?>()
    val timeZone: LiveData<String?> = _timeZone

    fun setTimeZone(value: String?) {
        _timeZone.value = value
    }

    private val _durationHours = MutableLiveData(defaultHours())
    val durationHours: LiveData<String> = _durationHours

    private val _durationMinutes = MutableLiveData(defaultMinutes())
    val durationMinutes: LiveData<String> = _durationMinutes

    private val _durationSeconds = MutableLiveData(defaultSeconds())
    val durationSeconds: LiveData<String> = _durationSeconds

    private val _isSaveEnabled = MediatorLiveData<Boolean>()
    val isSaveEnabled: LiveData<Boolean> = _isSaveEnabled

    private val _repeatMode = MutableLiveData(ScheduleRepeatMode.fromScheduleUi(schedule))
    // private val _repeatModeBackend: LiveData<String> = _repeatMode.map { it.label }

    private val _repeatModeLabel = MutableLiveData(ScheduleRepeatMode.fromScheduleUi(schedule).label)
    val repeatModeLabel: LiveData<String> = _repeatModeLabel

    private val _endRecurrenceOn = MutableLiveData(schedule?.hasEndRecurrence == true)
    val endRecurrenceOn: LiveData<Boolean> = _endRecurrenceOn

    private val _endDate = MutableLiveData(schedule?.endDateText ?: "31 / 12 / 2030")
    val endDate: LiveData<String> = _endDate

    private val _endTime = MutableLiveData(schedule?.endTimeText ?: "00 : 00")
    val endTime: LiveData<String> = _endTime

    private val _monthDay = MutableLiveData(schedule?.dayOfMonth?.toString()?.padStart(2, '0') ?: "01")
    val monthDay: LiveData<String> = _monthDay

    private val _yearDay = MutableLiveData(schedule?.dayOfMonth?.toString()?.padStart(2, '0') ?: "01")
    val yearDay: LiveData<String> = _yearDay

    private val _yearMonth = MutableLiveData(schedule?.monthShort ?: "Jan")
    val yearMonth: LiveData<String> = _yearMonth

    private val _yearMonthBackend = MutableLiveData(schedule?.monthShort ?: "Jan")
    val yearMonthBackend: LiveData<String> = _yearMonthBackend

    private val _selectedWeekDays = MutableLiveData(
        schedule?.selectedDayKeys ?: emptySet()
    )
    val selectedWeekDays: LiveData<Set<String>> = _selectedWeekDays

    val showEndRecurrenceSection: LiveData<Boolean> = _repeatMode.map { it != ScheduleRepeatMode.NONE }
    val showEndDateTime: LiveData<Boolean> = _endRecurrenceOn.map { it == true }
    val showWeekSelector: LiveData<Boolean> = _repeatMode.map { it == ScheduleRepeatMode.WEEK }
    val showMonthSelector: LiveData<Boolean> = _repeatMode.map { it == ScheduleRepeatMode.MONTH }
    val showYearSelector: LiveData<Boolean> = _repeatMode.map { it == ScheduleRepeatMode.YEAR }

    init {
        _isSaveEnabled.addSource(_apiStatus) { updateSaveEnabled() }
        _isSaveEnabled.addSource(_durationHours) { updateSaveEnabled() }
        _isSaveEnabled.addSource(_durationMinutes) { updateSaveEnabled() }
        _isSaveEnabled.addSource(_durationSeconds) { updateSaveEnabled() }

        updateSaveEnabled()
    }

    fun setStartDate(value: String) { _startDate.value = value }
    fun setStartTime(value: String) { _startTime.value = value }
    fun setDurationHours(value: String) { _durationHours.value = value }
    fun setDurationMinutes(value: String) { _durationMinutes.value = value }
    fun setDurationSeconds(value: String) { _durationSeconds.value = value }

    fun setRepeatMode(mode: ScheduleRepeatMode, localizedLabel: String) {
        _repeatMode.value = mode
        _repeatModeLabel.value = localizedLabel
        if (mode == ScheduleRepeatMode.NONE) {
            _endRecurrenceOn.value = false
        }
    }

    fun setEndRecurrenceOn(enabled: Boolean) { _endRecurrenceOn.value = enabled }
    fun setEndDate(value: String) { _endDate.value = value }
    fun setEndTime(value: String) { _endTime.value = value }
    fun setMonthDay(value: String) { _monthDay.value = value }
    fun setYearDay(value: String) { _yearDay.value = value }

    fun setYearMonth(value: String, localizedValue: String) {
        _yearMonthBackend.value = value
        _yearMonth.value = localizedValue
    }

    private fun updateSaveEnabled() {
        val hours = _durationHours.value?.toIntOrNull() ?: 0
        val minutes = _durationMinutes.value?.toIntOrNull() ?: 0
        val seconds = _durationSeconds.value?.toIntOrNull() ?: 0

        val hasNonZeroDuration = hours != 0 || minutes != 0 || seconds != 0
        val isLoading = _apiStatus.value == ApiStatus.LOADING

        _isSaveEnabled.value = hasNonZeroDuration && !isLoading
    }

    fun toggleWeekDay(key: String) {
        val current = _selectedWeekDays.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(key)) current.remove(key) else current.add(key)
        _selectedWeekDays.value = current
    }

    fun saveSchedule() {
        val token = prefManager.getAccessToken()
        if (token.isNullOrBlank()) {
            _apiStatus.value = ApiStatus.ERROR
            _errorServerMessage.value = "Missing access token"
            _errorServerMessageRes.value = null
            return
        }

        val variableId = _variableId.value
        if (variableId.isNullOrBlank()) {
            _apiStatus.value = ApiStatus.DONE
            _errorServerMessage.value = "Missing variableId"
            _errorServerMessageRes.value = null
            return
        }

        val payload = buildScheduleValueOrNull() ?: run {
            _apiStatus.value = ApiStatus.DONE
            _eventTransError.value = R.string.Error_Transaction_Failed
            return
        }

        viewModelScope.launch {
            try {
                _apiStatus.value = ApiStatus.LOADING

                when (val result = repository.updateVariable(token, variableId, VariableValue.ScheduleVal(payload))) {
                    is ApiResult.Success -> {
                        _apiStatus.value = ApiStatus.DONE
                        _dismissDialog.value = true
                    }
                    is ApiResult.Error -> {
                        handleError(result, "Saving schedule failed")
                    }
                }
            } catch (e: Exception) {
                _apiStatus.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("EditScheduleViewModel", "saveSchedule failed", e)
            }
        }
    }

    private fun buildScheduleValueOrNull(): ScheduleValues? {
        // frm
        val frm = parseToEpochSeconds(
            dateText = _startDate.value,
            timeText = _startTime.value
        ) ?: return null

        // len
        val h = _durationHours.value?.toIntOrNull() ?: 0
        val m = _durationMinutes.value?.toIntOrNull() ?: 0
        val s = _durationSeconds.value?.toIntOrNull() ?: 0
        val len = (h * 3600) + (m * 60) + s

        // repeatEvery
        val repeatEvery = _repeatMode.value?.label ?: ScheduleRepeatMode.NONE.label

        // to
        val to = when {
            repeatEvery.equals("Does not repeat", ignoreCase = true) -> 0L
            _endRecurrenceOn.value != true -> 0L
            else -> parseToEpochSeconds(_endDate.value, _endTime.value) ?: 0L
        }

        // extras
        val selectedDays = if (repeatEvery.equals("Week", ignoreCase = true)) {
            (_selectedWeekDays.value ?: emptySet()).mapNotNull { keyToBackendDay(it) }
        } else null

        val dayOfMonth = when {
            repeatEvery.equals("Month", ignoreCase = true) -> _monthDay.value?.toIntOrNull()
            repeatEvery.equals("Year", ignoreCase = true) -> _yearDay.value?.toIntOrNull()
            else -> null
        }

        val month = if (repeatEvery.equals("Year", ignoreCase = true)) {
            _yearMonthBackend.value
        } else null

        return ScheduleValues(
            frm = frm,
            len = len,
            to = to,
            repeatEvery = repeatEvery,
            selectedDays = selectedDays,
            dayOfMonth = dayOfMonth,
            month = month
        )
    }

    private fun parseToEpochSeconds(dateText: String?, timeText: String?): Long? {
        val d = dateText?.replace(" ", "") ?: return null          // "08/04/2025"
        val t = timeText?.replace(" ", "") ?: return null          // "08:20" or "00:00"

        val dateParts = d.split("/")
        if (dateParts.size != 3) return null

        val day = dateParts[0].toIntOrNull() ?: return null
        val month = dateParts[1].toIntOrNull() ?: return null
        val year = dateParts[2].toIntOrNull() ?: return null

        val timeParts = t.split(":")
        if (timeParts.size != 2) return null

        val hour = timeParts[0].toIntOrNull() ?: return null
        val minute = timeParts[1].toIntOrNull() ?: return null

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar month is 0-based
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return cal.timeInMillis / 1000L
    }

    private fun keyToBackendDay(key: String): String? {
        return when (key.lowercase(Locale.US)) {
            "sun" -> "Sun"
            "mon" -> "Mon"
            "tue" -> "Tue"
            "wed" -> "Wed"
            "thu" -> "Thu"
            "fri" -> "Fri"
            "sat" -> "Sat"
            else -> null
        }
    }

    /* =========================
     * ERROR HANDLING
     * ========================= */

    private fun handleError(error: ApiResult.Error, event: String) {
        Log.e("EditScheduleViewModel", "$event: ${error.devMessage}")

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

    private fun defaultHours(): String {
        val sec = schedule?.durationSec ?: 150
        return (sec / 3600).toString().padStart(2, '0')
    }

    private fun defaultMinutes(): String {
        val sec = schedule?.durationSec ?: 150
        return ((sec % 3600) / 60).toString().padStart(2, '0')
    }

    private fun defaultSeconds(): String {
        val sec = schedule?.durationSec ?: 150
        return (sec % 60).toString().padStart(2, '0')
    }

    fun onTransErrorCompleted() {
        _eventTransError.value = null
    }
}