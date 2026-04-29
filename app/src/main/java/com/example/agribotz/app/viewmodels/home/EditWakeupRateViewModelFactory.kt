package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager

class EditWakeupRateViewModelFactory(
    private val prefManager: PreferencesManager,
    private val currentWakeupRate: Float?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditWakeupRateViewModel::class.java)) {
            return EditWakeupRateViewModel(
                repository = Repository(),
                prefManager = prefManager,
                currentWakeupRate = currentWakeupRate
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
