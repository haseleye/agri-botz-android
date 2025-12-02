package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager

class GadgetDetailsViewModel(private val repo: Repository, private val gadgetId: String, private val prefManager: PreferencesManager) : ViewModel() {
    // Header section
    private val _gadgetName = MutableLiveData<String>("")
    val gadgetName: LiveData<String> = _gadgetName

    private val _isActive = MutableLiveData<Boolean>(true)
    val isActive: LiveData<Boolean> = _isActive

    private val _isOnline = MutableLiveData<Boolean>(true)
    val isOnline: LiveData<Boolean> = _isOnline

    private val _isTerminated = MutableLiveData<Boolean>(false)
    val isTerminated: LiveData<Boolean> = _isTerminated

    private val _activeStatusText = MutableLiveData<String>("")
    val activeStatusText: LiveData<String> = _activeStatusText

    private val _onlineStatusText = MutableLiveData<String>("")
    val onlineStatusText: LiveData<String> = _onlineStatusText

    private val _terminatedStatusText = MutableLiveData<String>("")
    val terminatedStatusText: LiveData<String> = _terminatedStatusText


}