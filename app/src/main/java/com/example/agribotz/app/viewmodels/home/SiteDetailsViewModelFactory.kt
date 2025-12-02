package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager

class SiteDetailsViewModelFactory(private val repository: Repository, private val prefManager: PreferencesManager, private val siteId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SiteDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SiteDetailsViewModel(repository, prefManager, siteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
