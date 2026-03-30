package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.ui.home.ScheduleUi
import com.example.agribotz.app.util.PreferencesManager

class EditScheduleViewModelFactory(
    private val prefManager: PreferencesManager,
    private val schedule: ScheduleUi?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditScheduleViewModel::class.java)) {
            return EditScheduleViewModel(
                repository = Repository(),
                prefManager = prefManager,
                schedule = schedule
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}