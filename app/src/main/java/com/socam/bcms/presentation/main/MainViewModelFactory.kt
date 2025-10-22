package com.socam.bcms.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.presentation.sync.SyncViewModel
import com.socam.bcms.presentation.sync.SyncViewModelFactory

/**
 * Factory for creating MainViewModel with required dependencies
 * SIMPLIFIED for stability
 */
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val databaseManager = DatabaseManager.getInstance(context)
            val authManager = AuthManager.getInstance(context)
            
            // Create SyncViewModel for notification count
            val syncViewModelFactory = SyncViewModelFactory(context)
            val syncViewModel = syncViewModelFactory.create(SyncViewModel::class.java)
            
            return MainViewModel(authManager, databaseManager, syncViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
