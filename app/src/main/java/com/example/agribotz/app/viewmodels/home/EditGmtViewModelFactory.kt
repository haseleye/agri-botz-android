package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager

class EditGmtViewModelFactory(
    private val prefManager: PreferencesManager,
    private val currentGmt: String?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditGmtViewModel::class.java)) {
            return EditGmtViewModel(
                repository = Repository(),
                prefManager = prefManager,
                currentGmt = currentGmt
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}