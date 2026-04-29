package com.example.agribotz.app.viewmodels.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.domain.VariableValue
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager
import kotlinx.coroutines.launch

class EditWakeupRateViewModel(
    private val repository: Repository,
    private val prefManager: PreferencesManager,
    private val currentWakeupRate: Float?
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

    private val _wakeupRateText = MutableLiveData("")
    val wakeupRateText: LiveData<String> = _wakeupRateText

    private val _wakeupRateValue = MutableLiveData(currentWakeupRate ?: 0f)

    fun setWakeupRate(displayValue: String, backendValue: Float) {
        _wakeupRateText.value = displayValue
        _wakeupRateValue.value = backendValue
    }

    fun saveWakeupRate(backendValue: Float) {
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

        if (backendValue < 1f || backendValue > 24f) {
            _apiStatus.value = ApiStatus.DONE
            _eventTransError.value = R.string.Error_Transaction_Failed
            return
        }

        _wakeupRateValue.value = backendValue

        viewModelScope.launch {
            try {
                _apiStatus.value = ApiStatus.LOADING

                when (val result = repository.updateVariable(token, variableId, VariableValue.FloatVal(backendValue))) {
                    is ApiResult.Success -> {
                        _apiStatus.value = ApiStatus.DONE
                        _dismissDialog.value = true
                    }

                    is ApiResult.Error -> {
                        handleError(result, "Saving wakeup rate failed")
                    }
                }
            } catch (e: Exception) {
                _apiStatus.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("EditWakeupRateViewModel", "saveWakeupRate failed", e)
            }
        }
    }

    /* =========================
     * ERROR HANDLING
     * ========================= */

    private fun handleError(error: ApiResult.Error, event: String) {
        Log.e("EditWakeupRateViewModel", "$event: ${error.devMessage}")

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

    fun onTransErrorCompleted() {
        _eventTransError.value = null
    }
}