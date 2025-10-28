package com.socam.bcms.data.api

import com.socam.bcms.BuildConfig
import com.socam.bcms.data.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API Service for synchronization endpoints
 * STABILITY PRIORITY: Simple, focused endpoints
 * 
 * NOTE: PROJECT_ID is now centralized in BuildConfig per device/APK build
 */
interface SyncApiService {
    
    companion object {
        // Use centralized project ID from BuildConfig
        @Deprecated("Use BuildConfig.PROJECT_ID directly instead", ReplaceWith("BuildConfig.PROJECT_ID", "com.socam.bcms.BuildConfig"))
        const val PROJECT_ID = BuildConfig.PROJECT_ID
        const val CATEGORY_PROJECT_ID = "FCC5D974-3513-4F2E-8979-13E2867B42EE"
    }
    
    // RfidModule Data Endpoints (MIC/ALW/TID)
    @POST("Rfids/{projId}/List")
    suspend fun getRfidModules(
        @Path("projId") projId: String,
        @Body request: RfidModuleRequest
    ): Response<List<RfidModuleDto>>
    
    // Batch modification endpoint for pending changes
    @POST("Rfids/ModificationAppv2/Multi")
    suspend fun batchModifyRfidModules(
        @Body request: List<RfidModificationDto>
    ): Response<BatchModificationResponse>
    
    // Master Data Endpoints
    @GET("Masters/{projId}/Locations/Regions")
    suspend fun getMasterRegions(
        @Path("projId") projId: String
    ): Response<List<MasterRegionDto>>
    
    @GET("Masters/{projId}/Locations/Floors")
    suspend fun getMasterFloors(
        @Path("projId") projId: String
    ): Response<List<MasterFloorDto>>
    
    @GET("Masters/{projId}/Concretes/Grades")
    suspend fun getMasterConcreteGrades(
        @Path("projId") projId: String
    ): Response<List<MasterConcreteGradeDto>>
    
    @GET("Masters/{projId}/Locations/List")
    suspend fun getMasterLocations(
        @Path("projId") projId: String
    ): Response<List<MasterLocationDto>>
    
    @GET("Masters/{projId}/Bcs/Categories")
    suspend fun getMasterCategories(
        @Path("projId") projId: String
    ): Response<List<MasterCategoryDto>>
    
    @GET("Masters/{projId}/Companies/List")
    suspend fun getMasterCompanies(
        @Path("projId") projId: String
    ): Response<List<MasterCompanyDto>>
    
    @GET("Masters/{projId}/WorkFlows/Steps/FullList")
    suspend fun getMasterWorkflowSteps(
        @Path("projId") projId: String
    ): Response<List<MasterWorkflowStepDto>>
    
    @GET("Masters/Contracts/List")
    suspend fun getMasterContracts(
        @Query("projid") projId: String
    ): Response<List<MasterContractDto>>
    
    // Device Serial Number Endpoint for tag generation
    // TODO: Update endpoint URL when backend implementation is ready
    @POST("Devices/SerialNumber/Get")
    suspend fun getDeviceSerialNumber(
        @Body request: SerialNumberRequest
    ): Response<SerialNumberResponse>
    
    // BC Type Serial Numbers Endpoint - fetch latest serial numbers for all BC types
    // Real API: POST SerialNo/Latest/
    @POST("SerialNo/Latest/")
    suspend fun getBCTypeSerialNumbers(
        @Body request: BCTypeSerialNumberRequest
    ): Response<BCTypeSerialNumbersResponse>
}
