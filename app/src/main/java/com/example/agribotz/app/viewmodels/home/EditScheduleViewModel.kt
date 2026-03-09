package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.agribotz.app.domain.ScheduleRepeatMode
import com.example.agribotz.app.ui.home.ScheduleUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditScheduleViewModel(
    private val schedule: ScheduleUi?
) : ViewModel() {

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


    private val _timeZone = MutableLiveData("Cairo")
    val timeZone: LiveData<String> = _timeZone

    private val _durationHours = MutableLiveData(defaultHours())
    val durationHours: LiveData<String> = _durationHours

    private val _durationMinutes = MutableLiveData(defaultMinutes())
    val durationMinutes: LiveData<String> = _durationMinutes

    private val _durationSeconds = MutableLiveData(defaultSeconds())
    val durationSeconds: LiveData<String> = _durationSeconds

    private val _repeatMode = MutableLiveData(ScheduleRepeatMode.fromScheduleUi(schedule))

    val repeatModeBackend: LiveData<String> = _repeatMode.map { it.label }

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

    val showEndRecurrenceSection: LiveData<Boolean> = _repeatMode.map {
        it != ScheduleRepeatMode.NONE
    }

    val showEndDateTime: LiveData<Boolean> = _endRecurrenceOn.map { it == true }

    val showWeekSelector: LiveData<Boolean> = _repeatMode.map {
        it == ScheduleRepeatMode.WEEK
    }

    val showMonthSelector: LiveData<Boolean> = _repeatMode.map {
        it == ScheduleRepeatMode.MONTH
    }

    val showYearSelector: LiveData<Boolean> = _repeatMode.map {
        it == ScheduleRepeatMode.YEAR
    }

    fun setStartDate(value: String) {
        _startDate.value = value
    }

    fun setStartTime(value: String) {
        _startTime.value = value
    }

    fun setDurationHours(value: String) {
        _durationHours.value = value
    }

    fun setDurationMinutes(value: String) {
        _durationMinutes.value = value
    }

    fun setDurationSeconds(value: String) {
        _durationSeconds.value = value
    }

    fun setRepeatMode(mode: ScheduleRepeatMode, localizedLabel: String) {
        _repeatMode.value = mode
        _repeatModeLabel.value = localizedLabel
        if (mode == ScheduleRepeatMode.NONE) {
            _endRecurrenceOn.value = false
        }
    }

    fun setEndRecurrenceOn(enabled: Boolean) {
        _endRecurrenceOn.value = enabled
    }

    fun setEndDate(value: String) {
        _endDate.value = value
    }

    fun setEndTime(value: String) {
        _endTime.value = value
    }

    fun setMonthDay(value: String) {
        _monthDay.value = value
    }

    fun setYearDay(value: String) {
        _yearDay.value = value
    }

    fun setYearMonth(value: String, localizedValue: String) {
        _yearMonthBackend.value = value
        _yearMonth.value = localizedValue
    }

    fun toggleWeekDay(key: String) {
        val current = _selectedWeekDays.value?.toMutableSet() ?: mutableSetOf()

        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
        }

        _selectedWeekDays.value = current
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
}
