package com.socam.bcms.presentation.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.socam.bcms.data.repository.SyncRepository
import com.socam.bcms.data.repository.TagRepository
import com.socam.bcms.domain.AuthManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for main screen
 * Manages user info, statistics, and synchronization state
 */
class MainViewModel(private val context: Context) : ViewModel() {
    
    private val authManager = AuthManager(context)
    private val tagRepository = TagRepository(context)
    private val syncRepository = SyncRepository(context)
    
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo
    
    private val _statsInfo = MutableLiveData<StatsInfo>()
    val statsInfo: LiveData<StatsInfo> = _statsInfo
    
    private val _syncState = MutableLiveData<SyncState>(SyncState.Idle)
    val syncState: LiveData<SyncState> = _syncState
    
    /**
     * Load current user information
     */
    suspend fun loadUserInfo(): Unit {
        viewModelScope.launch {
            try {
                val currentUser = authManager.getCurrentUser()
                if (currentUser != null) {
                    _userInfo.value = UserInfo(
                        fullName = currentUser.full_name,
                        username = currentUser.username,
                        role = currentUser.role,
                        department = currentUser.department ?: "Unknown"
                    )
                }
            } catch (e: Exception) {
                // Handle error silently or show default user info
                _userInfo.value = UserInfo("Unknown User", "unknown", "USER", "Unknown")
            }
        }
    }
    
    /**
     * Load statistics information
     */
    suspend fun loadStats(): Unit {
        viewModelScope.launch {
            try {
                // Get all tags
                val allTags = tagRepository.getAllTags().firstOrNull() ?: emptyList()
                val totalTags = allTags.size
                val activeTags = allTags.count { it.is_activated > 0 }
                
                // Get pending sync count
                val pendingSyncItems = syncRepository.getPendingSyncItems().firstOrNull() ?: emptyList()
                val pendingSync = pendingSyncItems.size
                
                _statsInfo.value = StatsInfo(
                    totalTags = totalTags,
                    activeTags = activeTags,
                    pendingSync = pendingSync
                )
            } catch (e: Exception) {
                // Handle error with default values
                _statsInfo.value = StatsInfo(0, 0, 0)
            }
        }
    }
    
    /**
     * Load synchronization status
     */
    suspend fun loadSyncStatus(): Unit {
        viewModelScope.launch {
            try {
                val recentSyncLogs = syncRepository.getRecentSyncLogs(1)
                if (recentSyncLogs.isNotEmpty()) {
                    val lastSync = recentSyncLogs.first()
                    val timestamp = formatTimestamp(lastSync.completed_at ?: lastSync.started_at)
                    
                    when (lastSync.status) {
                        "COMPLETED" -> {
                            _syncState.value = SyncState.Success(
                                message = "Last sync completed successfully",
                                timestamp = timestamp
                            )
                        }
                        "COMPLETED_WITH_ERRORS" -> {
                            _syncState.value = SyncState.Success(
                                message = "Last sync completed with some errors",
                                timestamp = timestamp
                            )
                        }
                        "FAILED" -> {
                            _syncState.value = SyncState.Error(
                                message = lastSync.error_summary ?: "Sync failed"
                            )
                        }
                        else -> {
                            _syncState.value = SyncState.Idle
                        }
                    }
                } else {
                    _syncState.value = SyncState.Idle
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Idle
            }
        }
    }
    
    /**
     * Perform data synchronization
     */
    suspend fun performSync(): Unit {
        _syncState.value = SyncState.Loading
        
        viewModelScope.launch {
            try {
                val result = syncRepository.performManualSync()
                
                if (result.isSuccess) {
                    val syncResult = result.getOrNull()!!
                    val timestamp = formatTimestamp(System.currentTimeMillis())
                    
                    if (syncResult.failureCount == 0) {
                        _syncState.value = SyncState.Success(
                            message = "Sync completed successfully (${syncResult.successCount} items)",
                            timestamp = timestamp
                        )
                    } else {
                        _syncState.value = SyncState.Success(
                            message = "Sync completed with ${syncResult.failureCount} errors",
                            timestamp = timestamp
                        )
                    }
                } else {
                    _syncState.value = SyncState.Error(
                        message = result.exceptionOrNull()?.message ?: "Sync failed"
                    )
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(
                    message = e.message ?: "Sync failed"
                )
            }
        }
    }
    
    /**
     * Logout current user
     */
    suspend fun logout(): Unit {
        viewModelScope.launch {
            authManager.logout()
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp * 1000))
    }
}

/**
 * User information data class
 */
data class UserInfo(
    val fullName: String,
    val username: String,
    val role: String,
    val department: String
)

/**
 * Statistics information data class
 */
data class StatsInfo(
    val totalTags: Int,
    val activeTags: Int,
    val pendingSync: Int
)

/**
 * Synchronization states
 */
sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String, val timestamp: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * ViewModel factory for dependency injection
 */
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
