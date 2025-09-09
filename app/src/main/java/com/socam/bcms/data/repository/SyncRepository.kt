package com.socam.bcms.data.repository

import android.content.Context
import com.socam.bcms.data.api.ApiClient
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.database.SyncLog
import com.socam.bcms.database.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Repository for data synchronization operations
 * Manages offline-first data sync with the backend API
 */
class SyncRepository(private val context: Context) {
    
    private val databaseManager = DatabaseManager.getInstance(context)
    private val apiClient = ApiClient.getInstance(context)
    private val tagRepository = TagRepository(context)
    private val batchRepository = BatchRepository(context)
    
    /**
     * Perform manual data synchronization
     */
    suspend fun performManualSync(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val syncLogId = startSyncLog("MANUAL")
            val result = performSync()
            completeSyncLog(syncLogId, result)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform automatic data synchronization
     */
    suspend fun performAutoSync(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            // Check if auto sync is enabled
            val autoSyncEnabled = getSetting("auto_sync_enabled")?.setting_value?.toBoolean() ?: false
            if (!autoSyncEnabled) {
                return@withContext Result.success(SyncResult(0, 0, emptyList(), "Auto sync disabled"))
            }
            
            val syncLogId = startSyncLog("AUTO")
            val result = performSync()
            completeSyncLog(syncLogId, result)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pending sync items
     */
    fun getPendingSyncItems(): Flow<List<SyncStatus>> = flow {
        val pendingItems = databaseManager.database.syncStatusQueries.selectPendingSyncs().executeAsList()
        emit(pendingItems)
    }
    
    /**
     * Get recent sync logs
     */
    suspend fun getRecentSyncLogs(limit: Long = 10): List<SyncLog> = withContext(Dispatchers.IO) {
        databaseManager.database.syncLogQueries.selectRecentSyncLogs(limit).executeAsList()
    }
    
    /**
     * Get sync status for specific table
     */
    suspend fun getSyncStatusForTable(tableName: String): List<SyncStatus> = withContext(Dispatchers.IO) {
        databaseManager.database.syncStatusQueries.selectSyncsByTable(tableName).executeAsList()
    }
    
    /**
     * Force sync specific record
     */
    suspend fun forceSyncRecord(tableName: String, recordId: Long, operation: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Mark record for immediate sync
            databaseManager.database.syncStatusQueries.insertSyncStatus(
                table_name = tableName,
                record_id = recordId,
                operation = operation,
                payload = ""
            )
            
            // Perform sync for this specific record
            val syncItems = databaseManager.database.syncStatusQueries
                .selectSyncByRecord(tableName, recordId, operation)
                .executeAsOneOrNull()
            
            if (syncItems != null) {
                when (tableName) {
                    "Tag" -> {
                        // Sync specific tag
                        tagRepository.syncTags()
                    }
                    "TagBatch" -> {
                        // Sync specific batch
                        batchRepository.syncBatch(recordId)
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check API connectivity
     */
    suspend fun checkApiConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.getApiService().healthCheck()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Update sync settings
     */
    suspend fun updateSyncSettings(autoSyncEnabled: Boolean, intervalMinutes: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            databaseManager.database.appSettingsQueries.updateSetting(
                setting_value = autoSyncEnabled.toString(),
                setting_key = "auto_sync_enabled"
            )
            
            databaseManager.database.appSettingsQueries.updateSetting(
                setting_value = intervalMinutes.toString(),
                setting_key = "sync_interval_minutes"
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun performSync(): SyncResult {
        var totalSuccess = 0
        var totalFailure = 0
        val allErrors = mutableListOf<String>()
        
        // Sync tags
        val tagSyncResult = tagRepository.syncTags()
        if (tagSyncResult.isSuccess) {
            val result = tagSyncResult.getOrNull()!!
            totalSuccess += result.successCount
            totalFailure += result.failureCount
            allErrors.addAll(result.errors)
        } else {
            totalFailure++
            allErrors.add("Tag sync failed: ${tagSyncResult.exceptionOrNull()?.message}")
        }
        
        // Clean up old completed sync records
        cleanupOldSyncRecords()
        
        // Update last sync timestamp
        updateLastSyncTimestamp()
        
        return SyncResult(
            successCount = totalSuccess,
            failureCount = totalFailure,
            errors = allErrors,
            message = if (totalFailure == 0) "Sync completed successfully" else "Sync completed with errors"
        )
    }
    
    private suspend fun startSyncLog(syncType: String): Long {
        databaseManager.database.syncLogQueries.insertSyncLog(
            sync_type = syncType,
            total_records = 0  // Will be updated later
        )
        
        return databaseManager.database.syncLogQueries
            .selectRecentSyncLogs(1)
            .executeAsOne()
            .id
    }
    
    private suspend fun completeSyncLog(syncLogId: Long, result: SyncResult) {
        val status = if (result.failureCount == 0) "COMPLETED" else "COMPLETED_WITH_ERRORS"
        val errorSummary = if (result.errors.isNotEmpty()) {
            result.errors.take(3).joinToString("; ") + if (result.errors.size > 3) "..." else ""
        } else null
        
        databaseManager.database.syncLogQueries.updateSyncLog(
            status = status,
            successful_records = result.successCount.toLong(),
            failed_records = result.failureCount.toLong(),
            error_summary = errorSummary,
            id = syncLogId
        )
    }
    
    private suspend fun cleanupOldSyncRecords() {
        databaseManager.database.syncStatusQueries.cleanupCompletedSyncs()
    }
    
    private suspend fun updateLastSyncTimestamp() {
        val currentTimestamp = System.currentTimeMillis() / 1000
        databaseManager.database.appSettingsQueries.updateSetting(
            setting_value = currentTimestamp.toString(),
            setting_key = "last_sync_timestamp"
        )
    }
    
    private suspend fun getSetting(key: String) = 
        databaseManager.database.appSettingsQueries.selectSettingByKey(key).executeAsOneOrNull()
    
    data class SyncResult(
        val successCount: Int,
        val failureCount: Int,
        val errors: List<String>,
        val message: String
    )
}
