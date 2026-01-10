package com.example.agribotz.app.viewmodels.home

import android.util.Log
import androidx.lifecycle.*
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.domain.GPS
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager
import kotlinx.coroutines.launch

class SetGadgetLocationViewModel(
    private val repository: Repository,
    private val prefManager: PreferencesManager,
    private val gadgetId: String
) : ViewModel() {

    private val _selectedGps = MutableLiveData<GPS?>()
    val selectedGps: LiveData<GPS?> = _selectedGps

    private val _status = MutableLiveData<ApiStatus>()
    val status: LiveData<ApiStatus> = _status

    private val _eventTransError = MutableLiveData<Int?>()
    val eventTransError: LiveData<Int?> = _eventTransError

    private val _errorServerMessage = MutableLiveData<String?>()
    val errorServerMessage: LiveData<String?> = _errorServerMessage

    private val _errorServerMessageRes = MutableLiveData<Int?>()
    val errorServerMessageRes: LiveData<Int?> = _errorServerMessageRes

    private val _done = MutableLiveData<Boolean?>()
    val done: LiveData<Boolean?> = _done

    private val _showSuccess = MutableLiveData<Boolean>()
    val showSuccess: LiveData<Boolean> = _showSuccess

    private val token: String? = prefManager.getAccessToken()

    fun onLocationSelected(lat: Double, lng: Double) {
        _selectedGps.value = GPS(lat = lat, long = lng)
    }

    fun onConfirmLocation() {
        val gps = _selectedGps.value ?: return
        val authToken = token ?: return

        viewModelScope.launch {
            try {
                _status.value = ApiStatus.LOADING

                when (
                    val result = repository.updateGadgetGps(
                        authToken,
                        gadgetId,
                        gps
                    )
                ) {
                    is ApiResult.Success -> {
                        _status.value = ApiStatus.DONE
                        _showSuccess.value = true
                        _done.value = true
                    }

                    is ApiResult.Error -> {
                        Log.e(
                            "SetGadgetLocationVM",
                            "Update GPS failed: ${result.devMessage}"
                        )

                        _status.value =
                            if (result.userMessageKey == R.string.Error_Internet_Connection) {
                                ApiStatus.ERROR
                            } else {
                                ApiStatus.DONE
                            }

                        if (!result.userMessageString.isNullOrBlank()) {
                            _errorServerMessage.value = result.userMessageString
                            _errorServerMessageRes.value = null
                        } else {
                            _errorServerMessageRes.value = result.userMessageKey
                            _errorServerMessage.value = null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "SetGadgetLocationVM",
                    "Unexpected exception",
                    e
                )

                _status.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
            }
        }
    }

    fun onNavigatedBack() {
        _done.value = null
    }

    fun onTransErrorCompleted() {
        _eventTransError.value = null
    }
}
