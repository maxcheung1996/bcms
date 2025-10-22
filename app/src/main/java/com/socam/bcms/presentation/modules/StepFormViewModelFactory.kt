package com.socam.bcms.presentation.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.socam.bcms.data.database.DatabaseManager

/**
 * Factory for creating StepFormViewModel instances
 */
class StepFormViewModelFactory(
    private val databaseManager: DatabaseManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepFormViewModel::class.java)) {
            return StepFormViewModel(databaseManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
