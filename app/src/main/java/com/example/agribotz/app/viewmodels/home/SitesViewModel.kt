package com.example.agribotz.app.viewmodels.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.domain.Site
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.ui.home.SiteUi
import com.example.agribotz.app.util.PreferencesManager
import kotlinx.coroutines.launch

class SitesViewModel(private val repository: Repository, private val prefManager: PreferencesManager) : ViewModel() {
    private val _sites = MutableLiveData<List<SiteUi>>(emptyList())
    val sites: LiveData<List<SiteUi>> = _sites

    private val _navigateToSite = MutableLiveData<String?>()
    val navigateToSite: LiveData<String?> = _navigateToSite

    private var _token: String? = ""

    private val _eventTransError = MutableLiveData<Int?>()
    val eventTransError: LiveData<Int?> = _eventTransError

    private var _status = MutableLiveData<ApiStatus>()
    val status: LiveData<ApiStatus> =_status

    private val _errorServerMessage = MutableLiveData<String?>()
    val errorServerMessage: LiveData<String?> = _errorServerMessage

    // For messages defined in strings.xml (fixed resource IDs)
    private val _errorServerMessageRes = MutableLiveData<Int?>()
    val errorServerMessageRes: LiveData<Int?> = _errorServerMessageRes

    init {
        _sites.value = prefManager.getSites()?.map {
            SiteUi(
                id = it.id,
                name = it.name,
                createdAt = it.createdAt,
                isActive = it.isActive,
                activatedAt = it.createdAt,
                deactivatedAt = it.deactivatedAt,
                isTerminated = it.isTerminated,
                terminatedAt = it.terminatedAt,
                numberOfGadgets = it.numberOfGadgets
            )
        }

        _token = prefManager.getAccessToken()
    }

    fun onLoad() {
        viewModelScope.launch {
            try {
                _status.value = ApiStatus.LOADING

                when (val result = repository.getSites(_token!!)) {
                    is ApiResult.Success -> {
                        _sites.value = result.data.message.sites.map {
                            SiteUi(
                                id = it.id,
                                name = it.name,
                                createdAt = it.createdAt,
                                isActive = it.isActive,
                                activatedAt = it.createdAt,
                                deactivatedAt = it.deactivatedAt,
                                isTerminated = it.isTerminated,
                                terminatedAt = it.terminatedAt,
                                numberOfGadgets = it.numberOfGadgets
                            )
                        }
                        _status.value = ApiStatus.DONE
                    }

                    is ApiResult.Error -> {
                        Log.e("SitesViewModel", "Loading failed: ${result.devMessage}")

                        if (result.userMessageKey == R.string.Error_Internet_Connection) {
                            _status.value = ApiStatus.ERROR
                        }
                        else {
                            _status.value = ApiStatus.DONE
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
                Log.e("SitesViewModel", "Loading failed with exception", e)
            }
        }
    }

    fun onSiteClicked(site: SiteUi) {
        if (!site.isTerminated) _navigateToSite.value = site.id
    }

    fun onNavigated() { _navigateToSite.value = null }

    fun onAddSite(name: String) {
        viewModelScope.launch {
            try {
                _status.value = ApiStatus.LOADING
                when (val result = repository.addSite(_token!!, name)) {
                    is ApiResult.Success -> { onLoad() }

                    is ApiResult.Error -> {
                        Log.e("SitesViewModel", "Adding new site failed: ${result.devMessage}")

                        // Show the "no connection" icon only for connectivity problems
                        if (result.userMessageKey == R.string.Error_Internet_Connection) {
                            _status.value = ApiStatus.ERROR
                        }
                        else {
                            _status.value = ApiStatus.DONE
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
                Log.e("SitesViewModel", "Adding new site failed with exception", e)
            }
        }
    }

    fun onRenameSite(siteId: String, newName: String) {
        viewModelScope.launch {
            try {
                when (val result = repository.renameSite(_token!!, siteId, newName)) {
                    is ApiResult.Success -> { onLoad() }

                    is ApiResult.Error -> {
                        Log.e("SitesViewModel", "Renaming site failed: ${result.devMessage}")

                        // Show the "no connection" icon only for connectivity problems
                        if (result.userMessageKey == R.string.Error_Internet_Connection) {
                            _status.value = ApiStatus.ERROR
                        }
                        else {
                            _status.value = ApiStatus.DONE
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
                Log.e("SitesViewModel", "Renaming site failed with exception", e)
            }
        }
    }

    fun onDeleteSiteClicked(site: Site) {
        if (site.numberOfGadgets > 0) return
        viewModelScope.launch {
            try {
                _status.value = ApiStatus.LOADING
                when (val result = repository.deleteSite(_token!!, site.id)) {
                    is ApiResult.Success -> { onLoad() }

                    is ApiResult.Error -> {
                        Log.e("SitesViewModel", "Deleting site failed: ${result.devMessage}")

                        // Show the "no connection" icon only for connectivity problems
                        if (result.userMessageKey == R.string.Error_Internet_Connection) {
                            _status.value = ApiStatus.ERROR
                        }
                        else {
                            _status.value = ApiStatus.DONE
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
                Log.e("SitesViewModel", "Deleting site failed with exception", e)
            }
        }
    }

    fun onTransErrorCompleted() {
        _eventTransError.value = null
    }
}