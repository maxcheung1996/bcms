package com.socam.bcms.data.repository

import android.content.Context
import com.socam.bcms.data.api.ApiClient
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.database.TagBatch
import com.socam.bcms.database.TagBatchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Repository for batch processing operations
 * Manages batch creation, item tracking, and processing status
 */
class BatchRepository(private val context: Context) {
    
    private val databaseManager = DatabaseManager.getInstance(context)
    private val apiClient = ApiClient.getInstance(context)
    
    /**
     * Get all batches
     */
    fun getAllBatches(): Flow<List<TagBatch>> = flow {
        val batches = databaseManager.database.tagBatchQueries.selectAllBatches().executeAsList()
        emit(batches.map { batch ->
            TagBatch(
                id = batch.id,
                batch_name = batch.batch_name,
                description = batch.description,
                status = batch.status,
                created_by = batch.created_by,
                created_at = batch.created_at,
                updated_at = batch.updated_at
            )
        })
    }
    
    /**
     * Get batch by ID
     */
    suspend fun getBatchById(batchId: Long): TagBatch? = withContext(Dispatchers.IO) {
        databaseManager.database.tagBatchQueries.selectBatchById(batchId).executeAsOneOrNull()
    }
    
    /**
     * Create new batch
     */
    suspend fun createBatch(
        batchName: String,
        description: String?,
        createdBy: Long
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            databaseManager.database.tagBatchQueries.insertBatch(
                batch_name = batchName,
                description = description,
                created_by = createdBy
            )
            
            // Get the inserted batch ID
            val batch = databaseManager.database.tagBatchQueries
                .selectAllBatches()
                .executeAsList()
                .maxByOrNull { it.created_at }
            
            if (batch != null) {
                Result.success(batch.id)
            } else {
                Result.failure(Exception("Failed to create batch"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add tag to batch
     */
    suspend fun addTagToBatch(batchId: Long, tagId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            databaseManager.database.tagBatchItemQueries.insertBatchItem(
                batch_id = batchId,
                tag_id = tagId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get batch items
     */
    suspend fun getBatchItems(batchId: Long): List<TagBatchItem> = withContext(Dispatchers.IO) {
        databaseManager.database.tagBatchItemQueries.selectBatchItems(batchId).executeAsList()
            .map { item ->
                TagBatchItem(
                    id = item.id,
                    batch_id = item.batch_id,
                    tag_id = item.tag_id,
                    processing_status = item.processing_status,
                    processed_at = item.processed_at,
                    notes = item.notes
                )
            }
    }
    
    /**
     * Update batch item status
     */
    suspend fun updateBatchItemStatus(
        batchId: Long,
        tagId: Long,
        status: String,
        notes: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            databaseManager.database.tagBatchItemQueries.updateBatchItemStatus(
                processing_status = status,
                notes = notes,
                batch_id = batchId,
                tag_id = tagId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update batch status
     */
    suspend fun updateBatchStatus(batchId: Long, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            databaseManager.database.tagBatchQueries.updateBatchStatus(
                status = status,
                id = batchId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process batch - mark all items as processed
     */
    suspend fun processBatch(batchId: Long): Result<BatchProcessResult> = withContext(Dispatchers.IO) {
        try {
            val batchItems = getBatchItems(batchId)
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()
            
            for (item in batchItems) {
                try {
                    // Process individual item (could include API calls, tag operations, etc.)
                    updateBatchItemStatus(batchId, item.tag_id, "PROCESSED", "Automatically processed")
                    successCount++
                } catch (e: Exception) {
                    updateBatchItemStatus(batchId, item.tag_id, "FAILED", "Error: ${e.message}")
                    failureCount++
                    errors.add("Tag ID ${item.tag_id}: ${e.message}")
                }
            }
            
            // Update batch status based on results
            val batchStatus = if (failureCount == 0) "COMPLETED" else "COMPLETED_WITH_ERRORS"
            updateBatchStatus(batchId, batchStatus)
            
            Result.success(BatchProcessResult(successCount, failureCount, errors))
        } catch (e: Exception) {
            updateBatchStatus(batchId, "FAILED")
            Result.failure(e)
        }
    }
    
    /**
     * Sync batch with API
     */
    suspend fun syncBatch(batchId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = getBatchById(batchId)
            if (batch == null) {
                return@withContext Result.failure(Exception("Batch not found"))
            }
            
            val batchItems = getBatchItems(batchId)
            
                val batchData: Map<String, Any> = mapOf(
                "id" to batch.id,
                "batchName" to batch.batch_name,
                "description" to (batch.description ?: ""),
                "status" to batch.status,
                "items" to batchItems.map { item ->
                    mapOf(
                        "tagId" to item.tag_id,
                        "status" to item.processing_status,
                        "notes" to (item.notes ?: "")
                    )
                }
            )
            
            val response = apiClient.getApiService().processBatch("", batchData)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class BatchProcessResult(
        val successCount: Int,
        val failureCount: Int,
        val errors: List<String>
    )
}
