package com.socam.bcms.data.repository

import android.content.Context
import com.socam.bcms.data.api.ApiClient
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.data.dto.TagDto
import com.socam.bcms.database.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Repository for tag data management
 * Implements offline-first approach with API synchronization
 */
class TagRepository(private val context: Context) {
    
    private val databaseManager = DatabaseManager.getInstance(context)
    private val apiClient = ApiClient.getInstance(context)
    
    /**
     * Get all tags from local database
     */
    fun getAllTags(): Flow<List<Tag>> = flow {
        val tags = databaseManager.database.tagQueries.selectAllTags().executeAsList()
        emit(tags)
    }
    
    /**
     * Get tag by EPC
     */
    suspend fun getTagByEpc(epc: String): Tag? = withContext(Dispatchers.IO) {
        databaseManager.database.tagQueries.selectTagByEpc(epc).executeAsOneOrNull()
    }
    
    /**
     * Insert or update tag in local database
     */
    suspend fun insertOrUpdateTag(
        epc: String,
        tid: String? = null,
        userData: String? = null,
        rssi: Int? = null,
        antennaId: Int? = null,
        batteryLevel: Int? = null,
        temperature: Double? = null
    ): Result<Tag> = withContext(Dispatchers.IO) {
        try {
            val existingTag = getTagByEpc(epc)
            
            if (existingTag != null) {
                // Update existing tag
                databaseManager.database.tagQueries.updateTag(
                    tid = tid,
                    user_data = userData,
                    rssi = rssi?.toLong(),
                    antenna_id = antennaId?.toLong(),
                    battery_level = batteryLevel?.toLong(),
                    temperature = temperature,
                    epc = epc
                )
            } else {
                // Insert new tag
                databaseManager.database.tagQueries.insertTag(
                    epc = epc,
                    tid = tid,
                    user_data = userData,
                    rssi = rssi?.toLong(),
                    antenna_id = antennaId?.toLong(),
                    battery_level = batteryLevel?.toLong(),
                    temperature = temperature
                )
                
                // Mark for sync
                markTagForSync(epc, "CREATE")
            }
            
            val updatedTag = getTagByEpc(epc)
            if (updatedTag != null) {
                Result.success(updatedTag)
            } else {
                Result.failure(Exception("Failed to retrieve updated tag"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Activate tag
     */
    suspend fun activateTag(tagId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            databaseManager.database.tagQueries.activateTag(tagId)
            markTagForSyncById(tagId, "UPDATE")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get recent tags scanned within specified time range
     */
    suspend fun getRecentTags(sinceTimestamp: Long, limit: Long): List<Tag> = withContext(Dispatchers.IO) {
        databaseManager.database.tagQueries.selectRecentTags(sinceTimestamp, limit).executeAsList()
    }
    
    /**
     * Delete tag
     */
    suspend fun deleteTag(tagId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            markTagForSyncById(tagId, "DELETE")
            databaseManager.database.tagQueries.deleteTag(tagId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync tags with API
     */
    suspend fun syncTags(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val unsyncedTags = databaseManager.database.tagQueries.selectTagsNotSynced().executeAsList()
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()
            
            for (tag in unsyncedTags) {
                try {
                    when (tag.sync_status) {
                        "PENDING" -> {
                            syncTagToApi(tag)
                            successCount++
                        }
                        else -> {
                            // Skip already processed tags
                        }
                    }
                } catch (e: Exception) {
                    failureCount++
                    errors.add("Tag ${tag.epc}: ${e.message}")
                }
            }
            
            Result.success(SyncResult(successCount, failureCount, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncTagToApi(tag: Tag): Unit {
        val tagDto = TagDto(
            id = tag.id,
            epc = tag.epc,
            tid = tag.tid,
            userData = tag.user_data,
            rssi = tag.rssi?.toInt(),
            antennaId = tag.antenna_id?.toInt(),
            readCount = tag.read_count.toInt(),
            batteryLevel = tag.battery_level?.toInt(),
            temperature = tag.temperature,
            isActivated = tag.is_activated > 0,
            lastScanned = tag.last_scanned.toString(),
            createdAt = tag.created_at.toString(),
            updatedAt = tag.updated_at.toString()
        )
        
        val response = if (tag.id != null) {
            apiClient.getApiService().updateTag("", tag.id, tagDto)
        } else {
            apiClient.getApiService().createTag("", tagDto)
        }
        
        if (response.isSuccessful) {
            databaseManager.database.tagQueries.updateSyncStatus("SYNCED", tag.id)
        } else {
            throw Exception("API call failed: ${response.code()}")
        }
    }
    
    private suspend fun markTagForSync(epc: String, operation: String): Unit {
        val tag = getTagByEpc(epc)
        if (tag != null) {
            databaseManager.database.syncStatusQueries.insertSyncStatus(
                table_name = "Tag",
                record_id = tag.id,
                operation = operation,
                payload = ""  // Could include JSON payload here
            )
        }
    }
    
    private suspend fun markTagForSyncById(tagId: Long, operation: String): Unit {
        databaseManager.database.syncStatusQueries.insertSyncStatus(
            table_name = "Tag",
            record_id = tagId,
            operation = operation,
            payload = ""
        )
    }
    
    data class SyncResult(
        val successCount: Int,
        val failureCount: Int,
        val errors: List<String>
    )
}
