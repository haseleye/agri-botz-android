package com.example.agribotz.app.viewmodels.home

import android.util.Log
import androidx.lifecycle.*
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.domain.GPS
import com.example.agribotz.app.domain.SetLocationNav
import com.example.agribotz.app.domain.Variable
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

    private val _showEmptyStateIcon = MutableLiveData<Boolean>(false)
    val showEmptyStateIcon: LiveData<Boolean> = _showEmptyStateIcon

    private val _navigateToGadget = MutableLiveData<String?>()
    val navigateToGadget: LiveData<String?> = _navigateToGadget

    private val _status = MutableLiveData<ApiStatus>()
    val status: LiveData<ApiStatus> = _status

    private val _eventTransError = MutableLiveData<Int?>()
    val eventTransError: LiveData<Int?> = _eventTransError

    private val _errorServerMessage = MutableLiveData<String?>()
    val errorServerMessage: LiveData<String?> = _errorServerMessage

    private val _errorServerMessageRes = MutableLiveData<Int?>()
    val errorServerMessageRes: LiveData<Int?> = _errorServerMessageRes

    private val _showStatusDetails = MutableLiveData<String?>()
    val showStatusDetails: LiveData<String?> = _showStatusDetails

    private val _navigateToMap = MutableLiveData<GPS?>()
    val navigateToMap: LiveData<GPS?> = _navigateToMap

    private val _navigateToSetLocation = MutableLiveData<SetLocationNav?>()
    val navigateToSetLocation: LiveData<SetLocationNav?> = _navigateToSetLocation

    private var _token: String? = null

    init {
        _token = prefManager.getAccessToken()
    }

    fun onLoad() {
        viewModelScope.launch {
            try {
                _status.value = ApiStatus.LOADING

                when (val result = repository.siteInfo(_token!!, siteId)) {
                    is ApiResult.Success -> {
                        val msg = result.data.message
                        _siteName.value = msg.siteInfo.name

                        _gadgets.value = msg.gadgets.map { gadget ->

                            val hasGps = gadget.gps != null

                            val isOnlineVar = gadget.variables
                                .firstOrNull { it is Variable.BooleanVar && it.name == "isOnline" }
                                    as Variable.BooleanVar?

                            val isActiveVar = gadget.variables
                                .firstOrNull { it is Variable.BooleanVar && it.name == "isActive" }
                                    as Variable.BooleanVar?

                            val isTerminatedVar = gadget.variables
                                .firstOrNull { it is Variable.BooleanVar && it.name == "isTerminated" }
                                    as Variable.BooleanVar?

                            val isOnline = isOnlineVar?.value ?: false
                            val isActive = isActiveVar?.value ?: false
                            val isTerminated = isTerminatedVar?.value ?: false

                            GadgetCardUi(
                                id = gadget.id,
                                name = gadget.name,

                                // GPS
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

                                // Hardware info
                                numberOfValves = gadget.numberOfValves ?: 0,
                                numberOfSensors = gadget.numberOfSensors ?: 0
                            )
                        }

                        _status.value = ApiStatus.DONE
                        _showEmptyStateIcon.value = _gadgets.value?.isEmpty()
                    }

                    is ApiResult.Error -> {
                        Log.e("SiteDetailsViewModel", "Loading failed: ${result.devMessage}")

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

    fun onStatusIconClicked(fullText: String?) {
        if (!fullText.isNullOrBlank()) {
            _showStatusDetails.value = fullText
        }
    }

    fun onStatusDetailsShown() {
        _showStatusDetails.value = null
    }

    fun onGpsClicked(gadget: GadgetCardUi) {
        _status.value = ApiStatus.LOADING

        if (gadget.canOpenMap) {
            _navigateToMap.value = gadget.gps
        }
        else {
            _navigateToSetLocation.value =
                SetLocationNav(
                    gadgetId = gadget.id,
                    gps = null
                )
        }
    }

    fun onGpsLongPressed(gadget: GadgetCardUi): Boolean {
        _status.value = ApiStatus.LOADING

        _navigateToSetLocation.value =
            SetLocationNav(
                gadgetId = gadget.id,
                gps = gadget.gps
            )
        return true
    }

    fun onMapNavigated() {
        _navigateToMap.value = null
        _status.value = ApiStatus.DONE
    }

    fun onSetGpsNavigated() {
        _navigateToSetLocation.value = null
    }
}
