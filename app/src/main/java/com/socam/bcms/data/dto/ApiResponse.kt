package com.socam.bcms.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper for all API calls
 * Provides consistent response structure across all endpoints
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("errorCode")
    val errorCode: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long? = null,
    
    @SerializedName("pagination")
    val pagination: PaginationInfo? = null
)

data class PaginationInfo(
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("pageSize")
    val pageSize: Int,
    
    @SerializedName("totalItems")
    val totalItems: Long,
    
    @SerializedName("totalPages")
    val totalPages: Int,
    
    @SerializedName("hasNext")
    val hasNext: Boolean,
    
    @SerializedName("hasPrevious")
    val hasPrevious: Boolean
)
