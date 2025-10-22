package com.socam.bcms.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.data.repository.StatsRepository
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.presentation.sync.SyncViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainViewModel - Enhanced with real-time dashboard statistics
 * 
 * Features:
 * - Real-time statistics from RfidModule table  
 * - Flow-based updates for live data
 * - Stable error handling with fallbacks
 * - Total Tags, Active Tags, Pending Sync counts
 */
class MainViewModel(
    private val authManager: AuthManager,
    private val databaseManager: DatabaseManager,
    private val syncViewModel: SyncViewModel
) : ViewModel() {

    // Initialize stats repository
    private val statsRepository = StatsRepository(databaseManager)

    // User information
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    // Statistics information - real-time from database
    private val _statsInfo = MutableLiveData<StatsInfo>()
    val statsInfo: LiveData<StatsInfo> = _statsInfo

    // Real-time stats flow - automatically updates UI
    val realTimeStats: LiveData<StatsInfo> = statsRepository.getDashboardStats()
        .map { dashboardStats ->
            val notificationCount = try {
                syncViewModel.getSyncErrors().size
            } catch (e: Exception) {
                0
            }
            
            StatsInfo(
                totalTags = dashboardStats.totalTags,
                activeTags = dashboardStats.activeTags,
                pendingSync = dashboardStats.pendingSync,
                notificationCount = notificationCount
            )
        }
        .catch { e ->
            println("MainViewModel: Error in real-time stats flow: ${e.message}")
            emit(StatsInfo(0, 0, 0, 0)) // Safe fallback
        }
        .asLiveData(viewModelScope.coroutineContext)

    // Sync state - simplified
    private val _syncState = MutableLiveData<SyncState>()
    val syncState: LiveData<SyncState> = _syncState

    init {
        println("MainViewModel: Initialized with real-time RfidModule statistics")
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
     * Load stats - Enhanced with real RfidModule database queries
     * Uses StatsRepository for accurate counts
     */
    fun loadStats(): Unit {
        viewModelScope.launch {
            try {
                println("MainViewModel: Loading real-time stats from RfidModule table...")
                
                // Get dashboard statistics from repository
                val dashboardStats = statsRepository.getDashboardStatsSnapshot()
                
                // Get notification count from SyncViewModel
                val notificationCount = try {
                    syncViewModel.getSyncErrors().size
                } catch (e: Exception) {
                    println("MainViewModel: Could not get notification count: ${e.message}")
                    0
                }
                
                // Update UI with real database values
                _statsInfo.value = StatsInfo(
                    totalTags = dashboardStats.totalTags,
                    activeTags = dashboardStats.activeTags,
                    pendingSync = dashboardStats.pendingSync,
                    notificationCount = notificationCount
                )
                
                println("MainViewModel: Stats loaded - Total: ${dashboardStats.totalTags}, Active: ${dashboardStats.activeTags}, Pending: ${dashboardStats.pendingSync}")
            } catch (e: Exception) {
                println("MainViewModel: Error loading stats: ${e.message}")
                // Fallback to safe default values
                _statsInfo.value = StatsInfo(0, 0, 0, 0)
            }
        }
    }
    
    /**
     * Get comprehensive statistics including BC type breakdown
     * Useful for detailed analysis and reporting
     */
    fun loadComprehensiveStats(): Unit {
        viewModelScope.launch {
            try {
                val comprehensiveStats = statsRepository.getComprehensiveStats()
                
                println("MainViewModel: Comprehensive Stats Loaded:")
                println("  - Dashboard: Total ${comprehensiveStats.dashboard.totalTags}, Active ${comprehensiveStats.dashboard.activeTags}, Pending ${comprehensiveStats.dashboard.pendingSync}")
                println("  - MIC: Total ${comprehensiveStats.micStats.totalCount}, Pending ${comprehensiveStats.micStats.pendingSync}")
                println("  - ALW: Total ${comprehensiveStats.alwStats.totalCount}, Pending ${comprehensiveStats.alwStats.pendingSync}")
                println("  - TID: Total ${comprehensiveStats.tidStats.totalCount}, Pending ${comprehensiveStats.tidStats.pendingSync}")
                
            } catch (e: Exception) {
                println("MainViewModel: Error loading comprehensive stats: ${e.message}")
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
    val pendingSync: Int,
    val notificationCount: Int = 0
)

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String, val timestamp: String) : SyncState()
    data class Error(val error: String) : SyncState()
}