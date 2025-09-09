package com.socam.bcms.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Tag API operations
 * Represents tag data as exchanged with the backend API
 */
data class TagDto(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("epc")
    val epc: String,
    
    @SerializedName("tid")
    val tid: String? = null,
    
    @SerializedName("userData")
    val userData: String? = null,
    
    @SerializedName("rssi")
    val rssi: Int? = null,
    
    @SerializedName("antennaId")
    val antennaId: Int? = null,
    
    @SerializedName("readCount")
    val readCount: Int = 1,
    
    @SerializedName("batteryLevel")
    val batteryLevel: Int? = null,
    
    @SerializedName("temperature")
    val temperature: Double? = null,
    
    @SerializedName("isActivated")
    val isActivated: Boolean = false,
    
    @SerializedName("activationDate")
    val activationDate: String? = null,
    
    @SerializedName("lastScanned")
    val lastScanned: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String,
    
    @SerializedName("metadata")
    val metadata: Map<String, Any>? = null
)

/**
 * Data Transfer Object for Batch operations
 */
data class BatchDto(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("batchName")
    val batchName: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("createdBy")
    val createdBy: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String,
    
    @SerializedName("items")
    val items: List<BatchItemDto>? = null,
    
    @SerializedName("totalItems")
    val totalItems: Int = 0,
    
    @SerializedName("processedItems")
    val processedItems: Int = 0,
    
    @SerializedName("failedItems")
    val failedItems: Int = 0
)

data class BatchItemDto(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("tagEpc")
    val tagEpc: String,
    
    @SerializedName("processingStatus")
    val processingStatus: String,
    
    @SerializedName("processedAt")
    val processedAt: String? = null,
    
    @SerializedName("notes")
    val notes: String? = null,
    
    @SerializedName("errorMessage")
    val errorMessage: String? = null
)
