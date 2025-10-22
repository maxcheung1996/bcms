package com.socam.bcms.presentation.notifications

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.data.dto.SyncError
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.presentation.sync.SyncViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * NotificationViewModel - Handles sync error notifications
 * Manages display and clearing of sync errors for user awareness
 */
class NotificationViewModel(
    private val context: Context,
    private val syncViewModel: SyncViewModel
) : ViewModel() {

    private val databaseManager = DatabaseManager.getInstance(context)
    private val authManager = AuthManager.getInstance(context)

    private val _syncErrors = MutableLiveData<List<SyncErrorDisplayItem>>()
    val syncErrors: LiveData<List<SyncErrorDisplayItem>> = _syncErrors

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Load sync errors from SyncViewModel
     */
    fun loadSyncErrors(): Unit {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Get errors from SyncViewModel
                val errors = syncViewModel.getSyncErrors()
                
                // Convert to display items with formatted data
                val displayItems = withContext(Dispatchers.IO) {
                    errors.map { error ->
                        convertToDisplayItem(error)
                    }.sortedByDescending { it.timestamp } // Most recent first
                }
                
                _syncErrors.value = displayItems
                println("NotificationViewModel: Loaded ${displayItems.size} sync errors")
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load sync errors: ${e.message}"
                println("NotificationViewModel: Error loading sync errors: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear all sync errors
     */
    fun clearAllErrors(): Unit {
        viewModelScope.launch {
            try {
                syncViewModel.clearSyncErrors()
                _syncErrors.value = emptyList()
                println("NotificationViewModel: All sync errors cleared")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear sync errors: ${e.message}"
                println("NotificationViewModel: Error clearing sync errors: ${e.message}")
            }
        }
    }

    /**
     * Clear specific error by record ID
     */
    fun clearError(recordId: String): Unit {
        val currentErrors = _syncErrors.value ?: return
        val updatedErrors = currentErrors.filter { it.recordId != recordId }
        _syncErrors.value = updatedErrors
        
        // Also remove from SyncViewModel (we'd need to add this method)
        println("NotificationViewModel: Cleared error for record: $recordId")
    }

    /**
     * Get error count for dashboard display
     */
    fun getErrorCount(): Int {
        return syncViewModel.getSyncErrors().size
    }

    /**
     * Convert SyncError to display item with additional formatting
     */
    private suspend fun convertToDisplayItem(error: SyncError): SyncErrorDisplayItem {
        // Get tag details from database if available
        val tagDetails = try {
            databaseManager.database.rfidModuleQueries
                .selectModulesById(error.recordId)
                .executeAsOneOrNull()
        } catch (e: Exception) {
            null
        }

        return SyncErrorDisplayItem(
            recordId = error.recordId,
            bcType = error.bcType,
            tagId = tagDetails?.TagId ?: "Unknown",
            tagNumber = tagDetails?.RFIDTagNo ?: "N/A",
            category = tagDetails?.Category ?: "Unknown",
            errorMessage = error.errorMessage,
            timestamp = error.timestamp,
            retryCount = error.retryCount,
            formattedTime = formatTimestamp(error.timestamp),
            severityLevel = determineSeverityLevel(error)
        )
    }

    /**
     * Format timestamp for display
     */
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val date = Date(timestamp)
            val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    /**
     * Determine error severity based on retry count and error message
     */
    private fun determineSeverityLevel(error: SyncError): SeverityLevel {
        return when {
            error.retryCount >= 2 -> SeverityLevel.HIGH
            error.errorMessage.contains("HTTP", ignoreCase = true) -> SeverityLevel.MEDIUM
            else -> SeverityLevel.LOW
        }
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage(): Unit {
        _errorMessage.value = null
    }
}

/**
 * Display item for sync errors with formatted data
 */
data class SyncErrorDisplayItem(
    val recordId: String,
    val bcType: String,
    val tagId: String,
    val tagNumber: String,
    val category: String,
    val errorMessage: String,
    val timestamp: Long,
    val retryCount: Int,
    val formattedTime: String,
    val severityLevel: SeverityLevel
)

/**
 * Error severity levels for UI styling
 */
enum class SeverityLevel(val displayName: String, val colorRes: Int) {
    HIGH("Critical", android.R.color.holo_red_dark),
    MEDIUM("Warning", android.R.color.holo_orange_dark),
    LOW("Info", android.R.color.holo_blue_dark)
}
