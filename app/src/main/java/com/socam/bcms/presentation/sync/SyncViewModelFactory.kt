package com.socam.bcms.presentation.sync

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager

/**
 * Factory for creating SyncViewModel with required dependencies
 * SIMPLIFIED for stability
 */
class SyncViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SyncViewModel::class.java)) {
            val databaseManager = DatabaseManager.getInstance(context)
            val authManager = AuthManager.getInstance(context)
            return SyncViewModel(databaseManager, authManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
