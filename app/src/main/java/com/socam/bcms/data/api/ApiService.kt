package com.socam.bcms.data.api

import com.socam.bcms.data.dto.TagDto
import com.socam.bcms.data.dto.ApiResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * API service interface for BCMS backend communication
 * Defines all API endpoints for tag management and synchronization
 */
interface ApiService {
    
    @GET("Tag/TagsList")
    suspend fun getTagsList(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<TagDto>>>
    
    @POST("Tag/Create")
    suspend fun createTag(
        @Header("Authorization") token: String,
        @Body tag: TagDto
    ): Response<ApiResponse<TagDto>>
    
    @PUT("Tag/Update/{id}")
    suspend fun updateTag(
        @Header("Authorization") token: String,
        @Path("id") tagId: Long,
        @Body tag: TagDto
    ): Response<ApiResponse<TagDto>>
    
    @DELETE("Tag/Delete/{id}")
    suspend fun deleteTag(
        @Header("Authorization") token: String,
        @Path("id") tagId: Long
    ): Response<ApiResponse<Unit>>
    
    @POST("Tag/Activate/{id}")
    suspend fun activateTag(
        @Header("Authorization") token: String,
        @Path("id") tagId: Long
    ): Response<ApiResponse<TagDto>>
    
    @POST("Tag/Batch/Process")
    suspend fun processBatch(
        @Header("Authorization") token: String,
        @Body batchData: Map<String, Any>
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("Tag/Batch/{id}")
    suspend fun getBatchStatus(
        @Header("Authorization") token: String,
        @Path("id") batchId: Long
    ): Response<ApiResponse<Map<String, Any>>>
    
    @POST("Auth/ValidateToken")
    suspend fun validateToken(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("System/Health")
    suspend fun healthCheck(): Response<ApiResponse<Map<String, String>>>
}
