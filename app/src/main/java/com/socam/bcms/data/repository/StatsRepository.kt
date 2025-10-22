package com.socam.bcms.data.repository

import com.socam.bcms.data.database.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository for dashboard statistics
 * Provides real-time data from RfidModule table
 * 
 * Features:
 * - Total tags count from RfidModule table
 * - Active tags count (IsActivated = 1)
 * - Pending sync count (sync_status = 'PENDING')
 * - Real-time updates using Flow
 */
class StatsRepository(
    private val databaseManager: DatabaseManager
) {
    
    /**
     * Get dashboard statistics as Flow for real-time updates
     */
    fun getDashboardStats(): Flow<DashboardStats> = flow {
        while (true) {
            try {
                val stats = loadDashboardStats()
                emit(stats)
                kotlinx.coroutines.delay(2000) // Update every 2 seconds for real-time feel
            } catch (e: Exception) {
                // Emit safe default values on error
                emit(DashboardStats(
                    totalTags = 0,
                    activeTags = 0,
                    pendingSync = 0
                ))
                kotlinx.coroutines.delay(5000) // Longer delay on error
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get one-time snapshot of dashboard statistics
     */
    suspend fun getDashboardStatsSnapshot(): DashboardStats = withContext(Dispatchers.IO) {
        return@withContext loadDashboardStats()
    }
    
    /**
     * Load statistics from RfidModule table
     */
    private suspend fun loadDashboardStats(): DashboardStats = withContext(Dispatchers.IO) {
        try {
            // Get total count of all RFID modules
            val totalTags = databaseManager.database.rfidModuleQueries
                .countAllModules()
                .executeAsOneOrNull() ?: 0L
            
            // Get count of activated modules (IsActivated = 1)
            val activeTags = databaseManager.database.rfidModuleQueries
                .countActivatedModules()
                .executeAsOneOrNull() ?: 0L
            
            // Get count of modules pending sync (sync_status = 'PENDING')
            val pendingSync = databaseManager.database.rfidModuleQueries
                .countModulesForSync()
                .executeAsOneOrNull() ?: 0L
            
            return@withContext DashboardStats(
                totalTags = totalTags.toInt(),
                activeTags = activeTags.toInt(),
                pendingSync = pendingSync.toInt()
            )
        } catch (e: Exception) {
            println("StatsRepository: Error loading dashboard stats: ${e.message}")
            // Return safe default values
            return@withContext DashboardStats(
                totalTags = 0,
                activeTags = 0,
                pendingSync = 0
            )
        }
    }
    
    /**
     * Get statistics by BC Type (MIC, ALW, TID)
     */
    suspend fun getStatsByBcType(bcType: String): BcTypeStats = withContext(Dispatchers.IO) {
        try {
            val totalByType = databaseManager.database.rfidModuleQueries
                .countByBCType(bcType)
                .executeAsOneOrNull() ?: 0L
            
            val pendingByType = databaseManager.database.rfidModuleQueries
                .countPendingByBCType(bcType)
                .executeAsOneOrNull() ?: 0L
            
            return@withContext BcTypeStats(
                bcType = bcType,
                totalCount = totalByType.toInt(),
                pendingSync = pendingByType.toInt()
            )
        } catch (e: Exception) {
            println("StatsRepository: Error loading stats for BC type $bcType: ${e.message}")
            return@withContext BcTypeStats(
                bcType = bcType,
                totalCount = 0,
                pendingSync = 0
            )
        }
    }
    
    /**
     * Get comprehensive statistics including breakdown by BC type
     */
    suspend fun getComprehensiveStats(): ComprehensiveStats = withContext(Dispatchers.IO) {
        try {
            val dashboardStats = loadDashboardStats()
            
            // Get breakdown by BC type
            val micStats = getStatsByBcType("MIC")
            val alwStats = getStatsByBcType("ALW") 
            val tidStats = getStatsByBcType("TID")
            
            return@withContext ComprehensiveStats(
                dashboard = dashboardStats,
                micStats = micStats,
                alwStats = alwStats,
                tidStats = tidStats
            )
        } catch (e: Exception) {
            println("StatsRepository: Error loading comprehensive stats: ${e.message}")
            return@withContext ComprehensiveStats(
                dashboard = DashboardStats(0, 0, 0),
                micStats = BcTypeStats("MIC", 0, 0),
                alwStats = BcTypeStats("ALW", 0, 0),
                tidStats = BcTypeStats("TID", 0, 0)
            )
        }
    }
}

/**
 * Data classes for statistics
 */
data class DashboardStats(
    val totalTags: Int,
    val activeTags: Int,
    val pendingSync: Int
)

data class BcTypeStats(
    val bcType: String,
    val totalCount: Int,
    val pendingSync: Int
)

data class ComprehensiveStats(
    val dashboard: DashboardStats,
    val micStats: BcTypeStats,
    val alwStats: BcTypeStats,
    val tidStats: BcTypeStats
)
