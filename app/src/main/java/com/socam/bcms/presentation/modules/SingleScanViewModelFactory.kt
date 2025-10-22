package com.socam.bcms.presentation.modules

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager

/**
 * Factory for creating SingleScanViewModel with required dependencies
 */
class SingleScanViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SingleScanViewModel::class.java)) {
            val databaseManager = DatabaseManager.getInstance(context)
            val authManager = AuthManager.getInstance(context)
            return SingleScanViewModel(databaseManager, authManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
