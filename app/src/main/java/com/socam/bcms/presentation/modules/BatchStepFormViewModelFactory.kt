package com.socam.bcms.presentation.modules

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager

/**
 * Factory for creating BatchStepFormViewModel with required dependencies
 */
class BatchStepFormViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BatchStepFormViewModel::class.java)) {
            val databaseManager = DatabaseManager.getInstance(context)
            val authManager = AuthManager.getInstance(context)
            return BatchStepFormViewModel(context, databaseManager, authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
