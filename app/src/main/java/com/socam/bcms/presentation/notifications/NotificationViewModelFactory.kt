package com.socam.bcms.presentation.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.presentation.sync.SyncViewModel

/**
 * Factory for creating NotificationViewModel with required dependencies
 */
class NotificationViewModelFactory(
    private val context: Context,
    private val syncViewModel: SyncViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            return NotificationViewModel(context, syncViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
