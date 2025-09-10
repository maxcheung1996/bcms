package com.socam.bcms.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager

/**
 * Factory for creating MainViewModel with required dependencies
 * SIMPLIFIED for stability
 */
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val databaseManager = DatabaseManager.getInstance(context)
            val authManager = AuthManager(context)
            return MainViewModel(authManager, databaseManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
