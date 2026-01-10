package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager

class SetGadgetLocationViewModelFactory(
    private val repository: Repository,
    private val prefManager: PreferencesManager,
    private val gadgetId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetGadgetLocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetGadgetLocationViewModel(repository, prefManager, gadgetId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
