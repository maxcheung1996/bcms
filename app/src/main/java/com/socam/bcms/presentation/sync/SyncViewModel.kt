package com.socam.bcms.presentation.sync

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.R
import com.socam.bcms.data.api.ApiClient
import com.socam.bcms.data.api.SyncApiService
import com.socam.bcms.data.auth.TokenManager
import com.socam.bcms.data.dto.RfidModuleDto
import com.socam.bcms.data.dto.RfidModuleRequest
import com.socam.bcms.data.dto.RfidModificationDto
import com.socam.bcms.data.dto.BatchModificationResponse
import com.socam.bcms.data.dto.SyncError
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * SyncViewModel - Handles data synchronization logic
 * STABILITY PRIORITY: Simple, reliable sync operations
 */
class SyncViewModel(
    private val databaseManager: DatabaseManager,
    private val authManager: AuthManager,
    private val context: Context
) : ViewModel() {

    private val tokenManager = TokenManager.getInstance(context)
    private val apiClient = ApiClient.getInstance(context)

    // ✅ FIXED: Use centralized ApiClient instead of hardcoded URL
    private val apiService: SyncApiService by lazy {
        println("SyncViewModel: Creating SyncApiService using centralized ApiClient")
        apiClient.getSyncApiService()
    }

    private val _syncState = MutableLiveData<SyncState>(SyncState.Idle)
    val syncState: LiveData<SyncState> = _syncState

    private val _isSyncing = MutableLiveData<Boolean>(false)
    val isSyncing: LiveData<Boolean> = _isSyncing

    private val _dataCounts = MutableLiveData<DataCounts>()
    val dataCounts: LiveData<DataCounts> = _dataCounts
    
    // Error tracking for failed sync attempts (for future notification module)
    private val syncErrors = mutableListOf<SyncError>()
    
    /**
     * Get current user's project ID for API calls
     */
    private suspend fun getCurrentProjectId(): String = withContext(Dispatchers.IO) {
        try {
            val currentUser = authManager.getCurrentUser()
            val projectId = currentUser?.project_id
            
            if (!projectId.isNullOrEmpty()) {
                println("SyncViewModel: Using project ID from user: $projectId")
                return@withContext projectId
            } else {
                // Fallback to default project ID
                val defaultProjectId = SyncApiService.PROJECT_ID
                println("SyncViewModel: No project ID found for user, using default: $defaultProjectId")
                return@withContext defaultProjectId
            }
        } catch (e: Exception) {
            // Fallback to default project ID
            val defaultProjectId = SyncApiService.PROJECT_ID
            println("SyncViewModel: Error getting project ID, using default: $defaultProjectId - ${e.message}")
            return@withContext defaultProjectId
        }
    }

    /**
     * Load current data counts from database including pending sync counts
     */
    suspend fun loadDataCounts(): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val micCount = databaseManager.database.rfidModuleQueries.countByBCType("MIC").executeAsOneOrNull() ?: 0
                val alwCount = databaseManager.database.rfidModuleQueries.countByBCType("ALW").executeAsOneOrNull() ?: 0
                val tidCount = databaseManager.database.rfidModuleQueries.countByBCType("TID").executeAsOneOrNull() ?: 0
                
                // Get pending sync counts for each BC type
                val micPendingCount = databaseManager.database.rfidModuleQueries.countPendingByBCType("MIC").executeAsOneOrNull() ?: 0
                val alwPendingCount = databaseManager.database.rfidModuleQueries.countPendingByBCType("ALW").executeAsOneOrNull() ?: 0
                val tidPendingCount = databaseManager.database.rfidModuleQueries.countPendingByBCType("TID").executeAsOneOrNull() ?: 0
                
                val regionsCount = databaseManager.database.masterRegionsQueries.countRegions().executeAsOneOrNull() ?: 0
                val gradesCount = databaseManager.database.masterConcreteGradesQueries.countConcreteGrades().executeAsOneOrNull() ?: 0
                val locationsCount = databaseManager.database.masterLocationsQueries.countLocations().executeAsOneOrNull() ?: 0
                val categoriesCount = databaseManager.database.masterCategoriesQueries.countCategories().executeAsOneOrNull() ?: 0
                val companiesCount = databaseManager.database.masterCompaniesQueries.countCompanies().executeAsOneOrNull() ?: 0
                
                val totalMasterRecords = regionsCount + gradesCount + locationsCount + categoriesCount + companiesCount
                
                val counts = DataCounts(
                    micCount = micCount.toInt(),
                    alwCount = alwCount.toInt(),
                    tidCount = tidCount.toInt(),
                    totalMasterRecords = totalMasterRecords.toInt(),
                    micPendingCount = micPendingCount.toInt(),
                    alwPendingCount = alwPendingCount.toInt(),
                    tidPendingCount = tidPendingCount.toInt()
                )
                
                withContext(Dispatchers.Main) {
                    _dataCounts.value = counts
                }
            } catch (e: Exception) {
                println("SyncViewModel: Error loading data counts: ${e.message}")
                withContext(Dispatchers.Main) {
                    _dataCounts.value = DataCounts(0, 0, 0, 0, 0, 0, 0)
                }
            }
        }
    }

    /**
     * Test token validation before performing sync operations
     */
    fun validateToken(): Unit {
        viewModelScope.launch {
            try {
                _syncState.value = SyncState.Loading("Validating authentication...")
                
                val currentToken = tokenManager.getBearerToken()
                println("SyncViewModel: Testing with token: ${currentToken?.take(30)}...")
                
                if (currentToken == null) {
                    _syncState.value = SyncState.Error("No token available")
                    return@launch
                }
                
                // Test with a simple endpoint first
                val projectId = getCurrentProjectId()
                val response = apiService.getMasterRegions(projectId)
                
                if (response.isSuccessful) {
                    val regions = response.body()
                    _syncState.value = SyncState.Success("Token validation successful! Found ${regions?.size ?: 0} regions")
                    println("SyncViewModel: Token validation successful")
                } else {
                    _syncState.value = SyncState.Error("Token validation failed: ${response.code()} ${response.message()}")
                    println("SyncViewModel: Token validation failed: ${response.code()}")
                }
                
            } catch (e: Exception) {
                val errorMsg = "Token validation failed: ${e.javaClass.simpleName}: ${e.message}"
                _syncState.value = SyncState.Error(errorMsg)
                println("SyncViewModel: $errorMsg")
                e.printStackTrace()
            }
        }
    }

    /**
     * Two-phase sync for component data (MIC/ALW/TID)
     * Phase 1: Upload pending changes to server
     * Phase 2: Download latest data from server
     */
    fun syncComponentData(bcType: String): Unit {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                println("SyncViewModel: Starting two-phase sync for $bcType components")
                
                // Phase 1: Upload pending changes
                val uploadSuccess = uploadPendingChanges(bcType)
                
                // Phase 2: Download server data (continue even if upload had some failures)
                if (uploadSuccess) {
                    downloadServerData(bcType)
                } else {
                    // Still try to download even if upload failed partially
                    _syncState.value = SyncState.Loading("Upload completed with some errors. Downloading $bcType data from server...")
                    downloadServerData(bcType)
                }
                
            } catch (e: Exception) {
                val errorMsg = "Sync failed: ${e.javaClass.simpleName}: ${e.message}"
                _syncState.value = SyncState.Error(errorMsg)
                println("SyncViewModel: $errorMsg")
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Phase 1: Upload pending changes to server with retry logic
     */
    private suspend fun uploadPendingChanges(bcType: String): Boolean {
        try {
            // Get pending records from database
            val pendingRecords = withContext(Dispatchers.IO) {
                databaseManager.database.rfidModuleQueries.selectPendingByBCType(bcType).executeAsList()
            }
            
            val pendingCount = pendingRecords.size
            
            if (pendingCount == 0) {
                _syncState.value = SyncState.Loading("No pending $bcType changes to upload. Downloading server data...")
                return true // No upload needed, but download should proceed
            }
            
            _syncState.value = SyncState.Loading("Uploading $pendingCount pending $bcType changes...")
            println("SyncViewModel: Found $pendingCount pending $bcType records to upload")
            
            // Convert to DTOs
            val modificationDtos = pendingRecords.map { record ->
                convertToModificationDto(record)
            }
            
            // LOG POST BODY for debugging
            val gson = com.google.gson.Gson()
            val jsonBody = gson.toJson(modificationDtos)
            println("SyncViewModel: === POST BODY FOR DEBUGGING ===")
            println("SyncViewModel: URL: POST https://dev.socam.com/iot/api/Rfids/ModificationAppv2/Multi")
            println("SyncViewModel: Body: $jsonBody")
            println("SyncViewModel: === END POST BODY ===")
            
            // Upload with retry logic
            var uploadSuccess = false
            var retryCount = 0
            val maxRetries = 2
            
            while (!uploadSuccess && retryCount <= maxRetries) {
                try {
                    _syncState.value = SyncState.Loading("Uploading $pendingCount $bcType changes... (attempt ${retryCount + 1})")
                    
                    val response = apiService.batchModifyRfidModules(modificationDtos)
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null && responseBody.status == 200) {
                            // Success - mark all records as synced
                            withContext(Dispatchers.IO) {
                                pendingRecords.forEach { record ->
                                    databaseManager.database.rfidModuleQueries.updateSyncStatusById("SYNCED", record.Id)
                                }
                            }
                            
                            _syncState.value = SyncState.Loading("✅ Uploaded $pendingCount $bcType changes successfully. Downloading server data...")
                            println("SyncViewModel: Successfully uploaded $pendingCount $bcType changes")
                            uploadSuccess = true
                            
                        } else {
                            // API returned error response
                            val errorMsg = responseBody?.message ?: "Unknown API error"
                            println("SyncViewModel: API returned error: $errorMsg")
                            
                            if (retryCount < maxRetries) {
                                retryCount++
                                _syncState.value = SyncState.Loading("Upload failed: $errorMsg. Retrying ($retryCount/$maxRetries)...")
                            } else {
                                // Max retries reached - log errors for notification module
                                logSyncErrors(pendingRecords, bcType, errorMsg)
                                _syncState.value = SyncState.Loading("⚠️ Upload failed after $maxRetries retries. Continuing with download...")
                                break // CRITICAL: Break out of while loop after max retries
                            }
                        }
                    } else {
                        // HTTP error
                        val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                        println("SyncViewModel: HTTP error during upload: $errorMsg")
                        
                        if (retryCount < maxRetries) {
                            retryCount++
                            _syncState.value = SyncState.Loading("Upload failed: $errorMsg. Retrying ($retryCount/$maxRetries)...")
                        } else {
                            // Max retries reached
                            logSyncErrors(pendingRecords, bcType, errorMsg)
                            _syncState.value = SyncState.Loading("⚠️ Upload failed after $maxRetries retries. Continuing with download...")
                            break // CRITICAL: Break out of while loop after max retries
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = "${e.javaClass.simpleName}: ${e.message}"
                    println("SyncViewModel: Exception during upload: $errorMsg")
                    
                    if (retryCount < maxRetries) {
                        retryCount++
                        _syncState.value = SyncState.Loading("Upload failed: $errorMsg. Retrying ($retryCount/$maxRetries)...")
                    } else {
                        // Max retries reached
                        logSyncErrors(pendingRecords, bcType, errorMsg)
                        _syncState.value = SyncState.Loading("⚠️ Upload failed after $maxRetries retries. Continuing with download...")
                        break // CRITICAL: Break out of while loop after max retries
                    }
                }
            }
            
            return uploadSuccess
            
        } catch (e: Exception) {
            println("SyncViewModel: Error during upload phase: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Phase 2: Download server data (existing logic)
     */
    private suspend fun downloadServerData(bcType: String) {
        try {
            _syncState.value = SyncState.Loading("Downloading latest $bcType data from server...")
            println("SyncViewModel: Starting download phase for $bcType components")
                
                // Call API with POST request
                val projectId = getCurrentProjectId()
            val request = RfidModuleRequest(listOf(bcType))
            val response = apiService.getRfidModules(projectId, request)
                
                if (response.isSuccessful) {
                    val components = response.body()
                    if (components != null) {
                    println("SyncViewModel: Received ${components.size} $bcType components from server")
                        
                        // Clear existing data for this BC type
                        withContext(Dispatchers.IO) {
                        databaseManager.database.rfidModuleQueries.deleteAllByBCType(bcType)
                        }
                        
                        // Insert new data
                    _syncState.value = SyncState.Loading("Saving ${components.size} $bcType components to local database...")
                        
                        withContext(Dispatchers.IO) {
                            components.forEach { dto ->
                                try {
                                databaseManager.database.rfidModuleQueries.insertModule(
                                    Id = dto.id,
                                    ProjId = dto.projId,
                                    ContractNo = dto.contractNo,
                                    ManufacturerId = dto.manufacturerId,
                                    TagId = dto.tagId,
                                    IsActivated = dto.isActivated.toLong(),
                                    ActivatedDate = parseDateTime(dto.activatedDate),
                                    BCType = dto.bcType,
                                    RFIDTagNo = dto.rfidTagNo,
                                    StepCode = dto.stepCode,
                                    Category = dto.category,
                                    Subcategory = dto.subcategory,
                                    SupplierId = dto.supplierId,
                                    ConcreteGrade = dto.concreteGrade,
                                    ASN = dto.asn,
                                    SerialNo = dto.serialNo,
                                    WorkingNo = dto.workingNo?.toLong(),
                                    ManufacturingDate = parseDateTime(dto.manufacturingDate),
                                    RSCompanyId = dto.rsCompanyId,
                                    RSInspectionDate = parseDateTime(dto.rsInspectionDate),
                                    CastingDate = parseDateTime(dto.castingDate),
                                    FirstCastingDate = parseDateTime(dto.firstCastingDate),
                                    SecondCastingDate = parseDateTime(dto.secondCastingDate),
                                    WaterproofingInstallationDate = parseDateTime(dto.waterproofingInstallationDate),
                                    InternalFinishDate = parseDateTime(dto.internalFinishDate),
                                    DeliveryDate = parseDateTime(dto.deliveryDate),
                                    BatchNo = dto.batchNo,
                                    LicensePlateNo = dto.licensePlateNo,
                                    GpsDeviceId = dto.gpsDeviceId,
                                    SiteArrivalDate = parseDateTime(dto.siteArrivalDate),
                                    SiteInstallationDate = parseDateTime(dto.siteInstallationDate),
                                    RoomInput = dto.roomInput,
                                    RoomId = dto.roomId,
                                    Floor = dto.floor,
                                    Region = dto.region,
                                    ChipFailureSA = dto.chipFailureSa.toLong(),
                                    ChipFailureSI = dto.chipFailureSi.toLong(),
                                    IsCompleted10 = dto.isCompleted10.toLong(),
                                    Remark10 = dto.remark10,
                                    IsCompleted20 = dto.isCompleted20.toLong(),
                                    Remark20 = dto.remark20,
                                    IsCompleted30 = dto.isCompleted30.toLong(),
                                    Remark30 = dto.remark30,
                                    IsCompleted40 = dto.isCompleted40.toLong(),
                                    Remark40 = dto.remark40,
                                    IsCompleted50 = dto.isCompleted50.toLong(),
                                    Remark50 = dto.remark50,
                                    IsCompleted55 = dto.isCompleted55.toLong(),
                                    Remark55 = dto.remark55,
                                    IsCompleted60 = dto.isCompleted60.toLong(),
                                    Remark60 = dto.remark60,
                                    IsCompleted70 = dto.isCompleted70.toLong(),
                                    Remark70 = dto.remark70,
                                    IsCompleted80 = dto.isCompleted80.toLong(),
                                    Remark80 = dto.remark80,
                                    Dispose = dto.dispose.toLong(),
                                    CreatedBy = dto.createdBy,
                                    UpdatedBy = dto.updatedBy,
                                    ProductNo = dto.productNo,
                                    CreatedDate = parseDateTime(dto.createdDate) ?: System.currentTimeMillis() / 1000,
                                    UpdatedDate = parseDateTime(dto.updatedDate) ?: System.currentTimeMillis() / 1000,
                                    sync_status = "SYNCED"
                                    )
                                } catch (e: Exception) {
                                println("SyncViewModel: Error inserting RfidModule ${dto.id}: ${e.message}")
                            }
                            }
                        }
                        
                        val message = "$bcType sync completed! ${components.size} components synchronized."
                        _syncState.value = SyncState.Success(message)
                        println("SyncViewModel: $message")
                    
                    } else {
                        _syncState.value = SyncState.Error("No data received from server")
                    }
                } else {
                val errorMsg = "Download failed: HTTP ${response.code()} ${response.message()}"
                    _syncState.value = SyncState.Error(errorMsg)
                    println("SyncViewModel: $errorMsg")
                }
                
            } catch (e: Exception) {
            val errorMsg = "Download failed: ${e.javaClass.simpleName}: ${e.message}"
                _syncState.value = SyncState.Error(errorMsg)
                println("SyncViewModel: $errorMsg")
                e.printStackTrace()
        }
    }
    
    /**
     * Log sync errors for future notification module
     */
    private fun logSyncErrors(failedRecords: List<com.socam.bcms.database.RfidModule>, bcType: String, errorMessage: String) {
        failedRecords.forEach { record ->
            val syncError = SyncError(
                recordId = record.Id,
                bcType = bcType,
                errorMessage = errorMessage,
                timestamp = System.currentTimeMillis(),
                retryCount = 2 // Max retries attempted
            )
            syncErrors.add(syncError)
        }
        println("SyncViewModel: Logged ${failedRecords.size} sync errors for future notification")
    }
    
    /**
     * Convert RfidModule database record to RfidModificationDto for API
     */
    private fun convertToModificationDto(record: com.socam.bcms.database.RfidModule): RfidModificationDto {
        // Debug: Log ManufacturingDate value from database
        println("SyncViewModel: DEBUG - Record ID: ${record.Id}")
        println("SyncViewModel: DEBUG - ManufacturingDate from DB: ${record.ManufacturingDate}")
        println("SyncViewModel: DEBUG - Formatted ManufacturingDate: ${formatDateOnlyForApi(record.ManufacturingDate)}")
        
        return RfidModificationDto(
            id = record.Id,
            projId = record.ProjId,
            contractNo = record.ContractNo,
            manufacturerId = record.ManufacturerId,
            tagId = record.TagId,
            isActivated = record.IsActivated.toInt(),
            activatedDate = formatDateTimeForApi(record.ActivatedDate),
            bcType = record.BCType,
            rfidTagNo = record.RFIDTagNo,
            stepCode = record.StepCode,
            category = record.Category,
            subcategory = record.Subcategory,
            supplierId = record.SupplierId,
            concreteGrade = record.ConcreteGrade,
            asn = record.ASN,
            serialNo = record.SerialNo,
            workingNo = record.WorkingNo?.toInt(),
            manufacturingDate = formatDateOnlyForApi(record.ManufacturingDate),
            productNo = record.ProductNo,
            rsCompanyId = record.RSCompanyId,
            rsInspectionDate = formatDateTimeForApi(record.RSInspectionDate),
            castingDate = formatDateTimeForApi(record.CastingDate),
            firstCastingDate = formatDateTimeForApi(record.FirstCastingDate),
            secondCastingDate = formatDateTimeForApi(record.SecondCastingDate),
            waterproofingInstallationDate = formatDateTimeForApi(record.WaterproofingInstallationDate),
            internalFinishDate = formatDateOnlyForApi(record.InternalFinishDate),
            deliveryDate = formatDateOnlyForApi(record.DeliveryDate),
            batchNo = record.BatchNo,
            licensePlateNo = record.LicensePlateNo,
            gpsDeviceId = record.GpsDeviceId,
            siteArrivalDate = formatDateOnlyForApi(record.SiteArrivalDate),
            siteInstallationDate = formatDateOnlyForApi(record.SiteInstallationDate),
            roomCode = record.RoomId, // RoomCode maps to RoomId in our schema
            roomId = record.RoomId,
            roomNo = "NA", // Default value as shown in API sample
            roomInput = record.RoomInput,
            locationType = null,
            areaGroup = null,
            floor = record.Floor,
            region = record.Region,
            chipFailureSa = record.ChipFailureSA.toInt(),
            chipFailureSi = record.ChipFailureSI.toInt(),
            isCompleted10 = record.IsCompleted10.toInt(),
            remark10 = record.Remark10,
            isCompleted20 = record.IsCompleted20.toInt(),
            remark20 = record.Remark20,
            isCompleted30 = record.IsCompleted30.toInt(),
            remark30 = record.Remark30,
            isCompleted40 = record.IsCompleted40.toInt(),
            remark40 = record.Remark40,
            isCompleted50 = record.IsCompleted50.toInt(),
            remark50 = record.Remark50,
            isCompleted55 = record.IsCompleted55.toInt(),
            remark55 = record.Remark55,
            isCompleted60 = record.IsCompleted60.toInt(),
            remark60 = record.Remark60,
            isCompleted70 = record.IsCompleted70.toInt(),
            remark70 = record.Remark70,
            isCompleted80 = record.IsCompleted80.toInt(),
            remark80 = record.Remark80,
            dispose = record.Dispose.toInt(),
            createdDate = formatDateTimeForApi(record.CreatedDate),
            createdBy = record.CreatedBy,
            updatedDate = formatDateTimeForApi(record.UpdatedDate),
            updatedBy = record.UpdatedBy
        )
    }
    
    /**
     * Format datetime for API (with time) - handles null safely
     */
    private fun formatDateTimeForApi(timestamp: Long?): String? {
        if (timestamp == null) return null
        return try {
            val date = java.util.Date(timestamp * 1000) // Convert from seconds to milliseconds
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            println("SyncViewModel: Error formatting datetime $timestamp: ${e.message}")
            null
        }
    }
    
    /**
     * Format date only for API (no time) - handles null safely
     */
    private fun formatDateOnlyForApi(timestamp: Long?): String? {
        if (timestamp == null) return null
        return try {
            val date = java.util.Date(timestamp * 1000) // Convert from seconds to milliseconds
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            println("SyncViewModel: Error formatting date $timestamp: ${e.message}")
            null
        }
    }

    /**
     * Sync all master data (7 endpoints)
     */
    fun syncMasterData(): Unit {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                var successCount = 0
                val totalEndpoints = 7
                
                // 1. Sync Regions
                _syncState.value = SyncState.Loading(context.getString(R.string.syncing_regions_progress))
                if (syncRegions()) successCount++
                
                // 2. Sync Concrete Grades
                _syncState.value = SyncState.Loading(context.getString(R.string.syncing_concrete_grades_progress))
                if (syncConcreteGrades()) successCount++
                
                // 3. Sync Locations (includes floor data)
                _syncState.value = SyncState.Loading(context.getString(R.string.syncing_locations_progress))
                if (syncLocations()) successCount++
                
                // 4. Sync Categories
                _syncState.value = SyncState.Loading(context.getString(R.string.syncing_categories_progress))
                if (syncCategories()) successCount++
                
                // 5. Sync Companies
                _syncState.value = SyncState.Loading(context.getString(R.string.syncing_companies_progress))
                if (syncCompanies()) successCount++
                
                // 6. Sync Workflow Steps
                _syncState.value = SyncState.Loading(context.getString(R.string.syncing_workflow_steps_progress))
                if (syncWorkflowSteps()) successCount++
                
                // 7. Sync Contracts
                _syncState.value = SyncState.Loading(context.getString(R.string.syncing_contracts_progress))
                if (syncContracts()) successCount++
                
                if (successCount == totalEndpoints) {
                    _syncState.value = SyncState.Success(context.getString(R.string.master_data_sync_completed))
                } else {
                    _syncState.value = SyncState.Error(context.getString(R.string.master_data_sync_partial_failed, successCount, totalEndpoints))
                }
                
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(context.getString(R.string.error_format, e.message ?: "Unknown error"))
                println("SyncViewModel: Master data sync error: ${e.message}")
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Helper functions for master data sync
    private suspend fun syncRegions(): Boolean = withContext(Dispatchers.IO) {
        try {
            println("SyncViewModel: 🔵 SYNC VERSION: NO-PROGUARD-v1.0.1-BUILD-20240919-1700 🔵")
            println("SyncViewModel: 📱 App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
            println("SyncViewModel: 🏗️ Version Code: ${context.packageManager.getPackageInfo(context.packageName, 0).versionCode}")
            val projectId = getCurrentProjectId()
            val response = apiService.getMasterRegions(projectId)
            if (response.isSuccessful) {
                val regions = response.body()
                println("SyncViewModel: API response successful, regions count: ${regions?.size}")
                
                if (regions != null && regions.isNotEmpty()) {
                    println("SyncViewModel: First region sample: key='${regions[0].key}', value='${regions[0].value}'")
                    
                    databaseManager.database.masterRegionsQueries.deleteAllRegions()
                    regions.forEachIndexed { index, dto ->
                        // Debug each region before inserting
                        if (dto.key == null) {
                            println("SyncViewModel: ❌ Region at index $index has NULL key! dto=$dto")
                            return@withContext false
                        }
                        if (dto.value == null) {
                            println("SyncViewModel: ❌ Region at index $index has NULL value! dto=$dto")
                            return@withContext false
                        }
                        databaseManager.database.masterRegionsQueries.insertRegion(dto.key, dto.value)
                    }
                    println("SyncViewModel: ✅ Synced ${regions.size} regions successfully")
                    return@withContext true
                } else {
                    println("SyncViewModel: ⚠️ Regions list is null or empty")
                }
            } else {
                println("SyncViewModel: ❌ API response failed: ${response.code()} ${response.message()}")
            }
            false
        } catch (e: Exception) {
            println("SyncViewModel: ❌ Exception syncing regions: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            false
        }
    }


    private suspend fun syncConcreteGrades(): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = getCurrentProjectId()
            val response = apiService.getMasterConcreteGrades(projectId)
            if (response.isSuccessful) {
                val grades = response.body()
                if (grades != null) {
                    databaseManager.database.masterConcreteGradesQueries.deleteAllConcreteGrades()
                    grades.forEach { dto ->
                        databaseManager.database.masterConcreteGradesQueries.insertConcreteGrade(
                            dto.id.toLong(), dto.grade, dto.isDefault.toLong()
                        )
                    }
                    println("SyncViewModel: Synced ${grades.size} concrete grades")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            println("SyncViewModel: Error syncing concrete grades: ${e.message}")
            false
        }
    }

    private suspend fun syncLocations(): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = getCurrentProjectId()
            val response = apiService.getMasterLocations(projectId)
            if (response.isSuccessful) {
                val locations = response.body()
                if (locations != null) {
                    databaseManager.database.masterLocationsQueries.deleteAllLocations()
                    locations.forEach { dto ->
                        databaseManager.database.masterLocationsQueries.insertLocation(
                            room_id = dto.roomId,
                            proj_id = dto.projId,
                            region_floor_code = dto.regionFloorCode,
                            region = dto.region,
                            floor = dto.floor,
                            region_floor_sort = dto.regionFloorSort?.toLong(),
                            area_location_code = dto.areaLocationCode,
                            area_group = dto.areaGroup,
                            location_type = dto.locationType,
                            area_location_sort = dto.areaLocationSort?.toLong(),
                            room = dto.room,
                            remarks = dto.remarks,
                            room_sort = dto.roomSort?.toLong(),
                            room_rfid = dto.roomRfid.toLong(),
                            floor_plan_file_guid = dto.floorPlanFileGuid
                        )
                    }
                    println("SyncViewModel: Synced ${locations.size} locations")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            println("SyncViewModel: Error syncing locations: ${e.message}")
            false
        }
    }

    private suspend fun syncCategories(): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = getCurrentProjectId()
            val response = apiService.getMasterCategories(projectId)
            if (response.isSuccessful) {
                val categories = response.body()
                if (categories != null) {
                    databaseManager.database.masterCategoriesQueries.deleteAllCategories()
                    categories.forEach { dto ->
                        databaseManager.database.masterCategoriesQueries.insertCategory(
                            bc_type = dto.bctype,
                            is_subcategory = dto.isSubcategory.toLong(),
                            category = dto.category,
                            desc_en = dto.descEN,
                            desc_tc = dto.descTC,
                            desc_sc = dto.descSC,
                            is_default = dto.isDefault.toLong()
                        )
                    }
                    println("SyncViewModel: Synced ${categories.size} categories")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            println("SyncViewModel: Error syncing categories: ${e.message}")
            false
        }
    }

    private suspend fun syncCompanies(): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = getCurrentProjectId()
            val response = apiService.getMasterCompanies(projectId)
            if (response.isSuccessful) {
                val companies = response.body()
                if (companies != null) {
                    databaseManager.database.masterCompaniesQueries.deleteAllCompanies()
                    companies.forEach { dto ->
                        databaseManager.database.masterCompaniesQueries.insertCompany(
                            id = dto.id,
                            type = dto.type,
                            bc_type = dto.bcType,
                            ref_code = dto.refCode,
                            name_en = dto.nameEN,
                            name_tc = dto.nameTC,
                            name_sc = dto.nameSC,
                            address_en = dto.addressEN,
                            address_tc = dto.addressTC,
                            address_sc = dto.addressSC,
                            gps_lat = dto.gpsLat,
                            gps_long = dto.gpsLong,
                            is_default = dto.isDefault.toLong()
                        )
                    }
                    println("SyncViewModel: Synced ${companies.size} companies")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            println("SyncViewModel: Error syncing companies: ${e.message}")
            false
        }
    }

    private suspend fun syncWorkflowSteps(): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = getCurrentProjectId()
            val response = apiService.getMasterWorkflowSteps(projectId)
            if (response.isSuccessful) {
                val workflowSteps = response.body()
                if (workflowSteps != null) {
                    databaseManager.database.masterWorkflowStepsQueries.deleteAllWorkflowSteps()
                    workflowSteps.forEach { dto ->
                        // Convert AllowField list to JSON string
                        val allowFieldJson = dto.allowField.joinToString(",") { "\"$it\"" }
                        val allowFieldArray = "[$allowFieldJson]"
                        
                        databaseManager.database.masterWorkflowStepsQueries.insertWorkflowStep(
                            step = dto.step,
                            portion = dto.portion.toLong(),
                            bc_type = dto.bctype,
                            can_update = dto.canUpdate.toLong(),
                            type_en = dto.typeEN,
                            type_tc = dto.typeTC,
                            type_sc = dto.typeSC,
                            step_desc_en = dto.stepDescEN,
                            step_desc_tc = dto.stepDescTC,
                            step_desc_sc = dto.stepDescSC,
                            allow_field = allowFieldArray
                        )
                    }
                    println("SyncViewModel: Synced ${workflowSteps.size} workflow steps")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            println("SyncViewModel: Error syncing workflow steps: ${e.message}")
            false
        }
    }

    private suspend fun syncContracts(): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = getCurrentProjectId()
            val response = apiService.getMasterContracts(projectId)
            if (response.isSuccessful) {
                val contracts = response.body()
                if (contracts != null) {
                    databaseManager.database.masterContractsQueries.deleteAllContracts()
                    contracts.forEach { dto ->
                        databaseManager.database.masterContractsQueries.insertContract(
                            proj_id = dto.projId,
                            contract_no = dto.contractNo,
                            contractor_name_en = dto.contractorNameEN,
                            contractor_name_tc = dto.contractorNameTC,
                            contractor_name_sc = dto.contractorNameSC,
                            contract_desc_en = dto.contractDescEN,
                            contract_desc_tc = dto.contractDescTC,
                            contract_desc_sc = dto.contractDescSC,
                            contract_start_date = parseDateTime(dto.contractStartDate),
                            contract_end_date = parseDateTime(dto.contractEndDate)
                        )
                    }
                    println("SyncViewModel: Synced ${contracts.size} contracts")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            println("SyncViewModel: Error syncing contracts: ${e.message}")
            false
        }
    }

    /**
     * Get sync errors for future notification module
     */
    fun getSyncErrors(): List<SyncError> {
        return syncErrors.toList() // Return a copy to prevent external modification
    }
    
    /**
     * Clear sync errors (for when user acknowledges them in notification module)
     */
    fun clearSyncErrors(): Unit {
        syncErrors.clear()
        println("SyncViewModel: Sync errors cleared")
    }

    /**
     * Parse datetime string to Unix timestamp
     */
    private fun parseDateTime(dateTime: String?): Long? {
        if (dateTime.isNullOrBlank()) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            format.parse(dateTime)?.time?.div(1000)
        } catch (e: Exception) {
            println("SyncViewModel: Error parsing datetime '$dateTime': ${e.message}")
            null
        }
    }
}

/**
 * Data classes for sync state management
 */
sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

data class DataCounts(
    val micCount: Int,
    val alwCount: Int,
    val tidCount: Int,
    val totalMasterRecords: Int,
    val micPendingCount: Int = 0,
    val alwPendingCount: Int = 0,
    val tidPendingCount: Int = 0
)
