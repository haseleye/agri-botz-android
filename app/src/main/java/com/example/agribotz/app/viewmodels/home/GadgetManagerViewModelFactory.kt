package com.example.agribotz.app.viewmodels.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager

class GadgetManagerViewModelFactory(
    private val app: Application,
    private val prefManager: PreferencesManager,
    private val gadgetId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GadgetManagerViewModel::class.java)) {
            return GadgetManagerViewModel(
                repository = Repository(),
                prefManager = prefManager,
                gadgetId = gadgetId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
