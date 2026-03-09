package com.example.agribotz.app.viewmodels.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.app.ui.home.ScheduleUi

class EditScheduleViewModelFactory(
    private val schedule: ScheduleUi?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditScheduleViewModel::class.java)) {
            return EditScheduleViewModel(schedule) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
