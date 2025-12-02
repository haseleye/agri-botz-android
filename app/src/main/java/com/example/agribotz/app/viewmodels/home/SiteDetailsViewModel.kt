package com.example.agribotz.app.viewmodels.home

import android.util.Log
import androidx.lifecycle.*
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.ui.home.GadgetCardUi
import com.example.agribotz.app.util.PreferencesManager
import kotlinx.coroutines.launch

class SiteDetailsViewModel(
    private val repository: Repository,
    private val prefManager: PreferencesManager,
    private val siteId: String
) : ViewModel() {

    private val _siteName = MutableLiveData<String?>()
    val siteName: LiveData<String?> = _siteName

    private val _gadgets = MutableLiveData<List<GadgetCardUi>>(emptyList())
    val gadgets: LiveData<List<GadgetCardUi>> = _gadgets

    private val _navigateToGadget = MutableLiveData<String?>()
    val navigateToGadget: LiveData<String?> = _navigateToGadget

    private var _token: String? = null

    private val _status = MutableLiveData<ApiStatus>()
    val status: LiveData<ApiStatus> = _status

    private val _eventTransError = MutableLiveData<Int?>()
    val eventTransError: LiveData<Int?> = _eventTransError

    private val _errorServerMessage = MutableLiveData<String?>()
    val errorServerMessage: LiveData<String?> = _errorServerMessage

    private val _errorServerMessageRes = MutableLiveData<Int?>()
    val errorServerMessageRes: LiveData<Int?> = _errorServerMessageRes

    init {
        _token = prefManager.getAccessToken()
    }

    /**
     * Load site info (site meta + gadgets)
     */
    fun onLoad() {
        viewModelScope.launch {
            try {
                _status.value = ApiStatus.LOADING

                when (val result = repository.siteInfo(_token!!, siteId)) {
                    is ApiResult.Success -> {
                        val msg = result.data.message
                        _siteName.value = msg.siteInfo.name

                        _gadgets.value = msg.gadgets.map { g ->
                            GadgetCardUi(
                                id = g.id,
                                name = g.name,
                                isActive = true,             // backend doesn't provide explicit flags here
                                isOnline = false,            // no online flag in returned model; keep default
                                isTerminated = false,        // default
                                numberOfValves = g.numberOfValves ?: 0,
                                numberOfSensors = g.numberOfSensors ?: 0,
                                statusLine = ""              // could be populated if backend returns status
                            )
                        }
                        _status.value = ApiStatus.DONE
                    }
                    is ApiResult.Error -> {
                        Log.e("SiteDetailsViewModel", "Loading failed: ${result.devMessage}")

                        _status.value = if (result.userMessageKey == R.string.Error_Internet_Connection) {
                            ApiStatus.ERROR
                        }
                        else {
                            ApiStatus.DONE
                        }

                        if (!result.userMessageString.isNullOrBlank()) {
                            _errorServerMessage.value = result.userMessageString
                            _errorServerMessageRes.value = null
                        }
                        else {
                            _errorServerMessageRes.value = result.userMessageKey
                            _errorServerMessage.value = null
                        }
                    }
                }
            }
            catch (e: Exception) {
                _status.value = ApiStatus.DONE
                _eventTransError.value = R.string.Error_Transaction_Failed
                Log.e("SiteDetailsViewModel", "Loading failed with exception", e)
            }
        }
    }

    fun onGadgetClicked(gadgetId: String) {
        _navigateToGadget.value = gadgetId
    }

    fun onNavigatedToGadget() {
        _navigateToGadget.value = null
    }

    fun onTransErrorCompleted() {
        _eventTransError.value = null
    }
}
