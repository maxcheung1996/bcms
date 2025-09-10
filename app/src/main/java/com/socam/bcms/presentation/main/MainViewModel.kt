package com.socam.bcms.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainViewModel - SIMPLIFIED for maximum stability
 * 
 * PRIORITY: Stability first, no complex operations
 * - No COUNT database queries
 * - No memory monitoring
 * - No heavy operations
 * - Static values until RFID scanning is implemented
 */
class MainViewModel(
    private val authManager: AuthManager,
    private val databaseManager: DatabaseManager
) : ViewModel() {

    // User information
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    // Statistics information - simplified 
    private val _statsInfo = MutableLiveData<StatsInfo>()
    val statsInfo: LiveData<StatsInfo> = _statsInfo

    // Sync state - simplified
    private val _syncState = MutableLiveData<SyncState>()
    val syncState: LiveData<SyncState> = _syncState

    init {
        println("MainViewModel: Initialized with simplified stable configuration")
    }

    /**
     * Load current user information - ENHANCED with debugging
     */
    fun loadUserInfo(): Unit {
        viewModelScope.launch {
            try {
                println("MainViewModel: Loading user info...")
                val currentUser = authManager.getCurrentUser()
                println("MainViewModel: Current user: ${currentUser?.username ?: "null"}")
                
                if (currentUser != null) {
                    val userInfo = UserInfo(
                        fullName = currentUser.full_name ?: "Demo User",
                        username = currentUser.username ?: "demo",
                        role = currentUser.role ?: "USER",
                        department = currentUser.department ?: "Operations"
                    )
                    println("MainViewModel: Setting user info: ${userInfo.fullName}")
                    _userInfo.value = userInfo
                } else {
                    // If no current user, set a reasonable default
                    println("MainViewModel: No current user found, using default")
                    _userInfo.value = UserInfo("Demo User", "demo", "USER", "Operations")
                }
            } catch (e: Exception) {
                println("MainViewModel: Error loading user info: ${e.message}")
                // Handle error with better default
                _userInfo.value = UserInfo("Demo User", "demo", "USER", "Operations")
            }
        }
    }

    /**
     * Load stats - SIMPLIFIED for maximum stability
     * No database COUNT queries - just return static values
     */
    fun loadStats(): Unit {
        viewModelScope.launch {
            try {
                // Return static values for stability - focus on RFID scanning
                _statsInfo.value = StatsInfo(
                    totalTags = 0,      // Will be updated when RFID scanning is implemented
                    activeTags = 0,     // Will be updated when RFID scanning is implemented
                    pendingSync = 0     // Will be updated when sync is implemented
                )
                println("MainViewModel: Stats loaded with static values for stability")
            } catch (e: Exception) {
                println("MainViewModel: Error loading stats: ${e.message}")
                _statsInfo.value = StatsInfo(0, 0, 0)
            }
        }
    }

    /**
     * Load synchronization status - SIMPLIFIED for stability
     * No database queries - just return static ready state
     */
    fun loadSyncStatus(): Unit {
        viewModelScope.launch {
            try {
                // Set static ready state for stability
                _syncState.value = SyncState.Success(
                    message = "System ready for RFID scanning",
                    timestamp = formatTimestamp(System.currentTimeMillis())
                )
                println("MainViewModel: Sync status loaded with static ready state")
            } catch (e: Exception) {
                println("MainViewModel: Error loading sync status: ${e.message}")
                _syncState.value = SyncState.Idle
            }
        }
    }

    /**
     * Logout current user
     */
    fun logout(): Unit {
        viewModelScope.launch {
            try {
                authManager.logout()
                println("MainViewModel: User logged out successfully")
            } catch (e: Exception) {
                println("MainViewModel: Error during logout: ${e.message}")
            }
        }
    }

    /**
     * Format timestamp to readable string
     */
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "00:00:00"
        }
    }
}

/**
 * Data classes for UI state
 */
data class UserInfo(
    val fullName: String,
    val username: String,
    val role: String,
    val department: String
)

data class StatsInfo(
    val totalTags: Int,
    val activeTags: Int,
    val pendingSync: Int
)

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String, val timestamp: String) : SyncState()
    data class Error(val error: String) : SyncState()
}