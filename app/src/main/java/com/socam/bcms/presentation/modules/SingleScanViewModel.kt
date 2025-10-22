package com.socam.bcms.presentation.modules

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.R
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.database.RfidModule
import com.socam.bcms.database.MasterWorkflowSteps
import com.socam.bcms.database.MasterContracts
import com.socam.bcms.database.MasterCompanies
import com.socam.bcms.uhf.UHFManagerWrapper
import com.socam.bcms.model.TagStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import android.util.Log

/**
 * ViewModel for Single Scan Module
 * Handles RFID scanning and tag details display with role-based workflow steps
 */
class SingleScanViewModel(
    private val databaseManager: DatabaseManager,
    private val authManager: AuthManager,
    private val context: Context
) : ViewModel() {

    // UHF Manager - Use singleton from Application (following vendor demo pattern)
    // CRITICAL: Vendor demo uses singleton UHFManager stored in Application class
    private val uhfManager: UHFManagerWrapper
        get() = com.socam.bcms.BCMSApp.instance.uhfManager

    private val _uiState = MutableStateFlow(SingleScanUiState())
    val uiState: StateFlow<SingleScanUiState> = _uiState.asStateFlow()
    
    // Store current tag data for auto-filling step forms - switched to RfidModule
    private var currentTagData: com.socam.bcms.database.RfidModule? = null
    
    // Hold-to-scan pattern variables (following Tag Activation pattern)
    private val scannedTags = mutableMapOf<String, TagScanData>()
    private var scanningJob: kotlinx.coroutines.Job? = null
    
    init {
        // UHF is now initialized centrally in BCMSApp (following vendor demo pattern)
        // No need to initialize or power on here - just set ready status
        _uiState.value = _uiState.value.copy(
            scanningStatus = ScanningStatus.READY,
            statusMessage = context.getString(R.string.single_scan_ready_message),
            currentRfidModule = null
        )
        
        println("SingleScanViewModel: ViewModel initialized - using singleton UHF manager from Application")
    }
    
    // Hold-to-scan state management (following Tag Activation pattern)
    private var isScanning = false
    companion object {
        private const val TAG = "SingleScanViewModel" 
        private const val SCAN_INTERVAL_MS = 1L // High performance scanning
    }

    /**
     * Start RFID scanning (hold-to-scan pattern like Tag Activation)
     * CRITICAL: Following vendor demo pattern with high-performance scanning
     * Can be called at any time to rescan - clears all previous results and tag selections
     */
    fun startScanning(): Unit {
        
        if (_uiState.value.scanningStatus == ScanningStatus.SCANNING) {
            Log.d(TAG, "Already scanning - ignoring duplicate start request")
            return
        }
        
        viewModelScope.launch {
            try {
                val hasExistingResults = _uiState.value.tagDetails != null || _uiState.value.candidateTags.isNotEmpty()
                Log.d(TAG, "Starting hold-to-scan with vendor demo pattern${if (hasExistingResults) " (RESCANNING - clearing ${_uiState.value.candidateTags.size} previous candidates)" else ""}")
                
                // CRITICAL: Ensure clean UHF state before starting
                uhfManager.stopInventory()
                Thread.sleep(100)
                
                // Clear previous scan results (supports rescanning at any time)
                scannedTags.clear()
                
                _uiState.value = _uiState.value.copy(
                    scanningStatus = ScanningStatus.SCANNING,
                    statusMessage = context.getString(R.string.scanning_activated_tags),
                    tagDetails = null,      // Clear previous results
                    workflowSteps = emptyList(),
                    tagInformation = null,
                    currentRfidModule = null,
                    candidateTags = emptyList(),  // Clear candidate tags list
                    showTagSelection = false       // Hide tag selection UI
                )

                // Start inventory using vendor demo pattern
                val started = uhfManager.startInventory()
                if (started) {
                    isScanning = true
                    startRealTimeScanningLoop()
                } else {
                    _uiState.value = _uiState.value.copy(
                        scanningStatus = ScanningStatus.ERROR,
                        statusMessage = context.getString(R.string.failed_start_rfid_scanning)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting scan: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    scanningStatus = ScanningStatus.ERROR,
                    statusMessage = context.getString(R.string.scan_start_error_format, e.message ?: "Unknown error")
                )
            }
        }
    }

    /**
     * Stop RFID scanning and process tags (Auto mode only)
     */
    fun stopScanning(): Unit {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Stopping scan - Manual selection mode")
                
                // Stop inventory
                uhfManager.stopInventory()
                isScanning = false
                scanningJob?.cancel()
                scanningJob = null
                
                // Filter for ACTIVATED tags only (tags starting with "34")
                val activatedTags = scannedTags.values.filter { 
                    getEnhancedTagStatusFromEpc(it.epc) == EnhancedTagStatus.ACTIVATE
                }
                
                if (activatedTags.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        scanningStatus = ScanningStatus.READY,
                        statusMessage = "No ACTIVATED tags found. Only activated tags can be processed in Single Scan."
                    )
                    return@launch
                }
                
                // Always show candidate selection (manual-only mode)
                showCandidateTagsForSelection()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    scanningStatus = ScanningStatus.ERROR,
                    statusMessage = "Scan stop error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Show candidate tags for manual selection (ACTIVATED tags only)
     */
    private fun showCandidateTagsForSelection(): Unit {
        viewModelScope.launch {
        try {
            Log.d(TAG, "Manual mode - Building candidate list from ${scannedTags.size} scanned tags")
            
                // Filter for ACTIVATED tags and sort by RSSI
                val activatedTags = scannedTags.values.filter { 
                    getEnhancedTagStatusFromEpc(it.epc) == EnhancedTagStatus.ACTIVATE
                }
                
                val sortedTags = activatedTags
                .sortedByDescending { it.rssiDbm }
                    .take(10) // Show up to 10 tags for selection
                
                Log.d(TAG, "Manual mode - Showing top ${sortedTags.size} ACTIVATED tags for selection")
                
                // Query database for dispose status and tag details
                val candidateTags = withContext(Dispatchers.IO) {
                    sortedTags.map { tagScanData ->
                        // Query database for tag details and dispose status
                        val rfidRecords = databaseManager.database.rfidModuleQueries
                            .selectModulesByRFIDTagNo(tagScanData.epc)
                            .executeAsList()
                        
                        val rfidRecord = rfidRecords.firstOrNull()
                        val isDisposed = rfidRecord?.Dispose == 1L
                        
                        // Determine badge type and clickability
                        val badgeType = if (isDisposed) {
                            TagBadgeType.DISPOSED
                        } else {
                            TagBadgeType.ACTIVATE
                        }
                        
                        Log.d(TAG, "Tag ${tagScanData.epc}: Dispose=${rfidRecord?.Dispose}, Badge=$badgeType")
                        
                        CandidateTag(
                            epc = tagScanData.epc,
                            rssiDbm = tagScanData.rssiDbm,
                            rssiRaw = tagScanData.rssiRaw,
                            bcType = rfidRecord?.BCType ?: "",
                            tagNo = rfidRecord?.RFIDTagNo ?: "",
                            badgeType = badgeType,
                            isClickable = true  // Keep clickable but will show toast warning if disposed
                        )
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    scanningStatus = ScanningStatus.READY,  // CRITICAL: Set to READY to allow rescanning
                    candidateTags = candidateTags,
                    showTagSelection = true,
                    statusMessage = if (candidateTags.isNotEmpty()) {
                        "Found ${candidateTags.size} activated tags. Select one to process."
                    } else {
                        "No activated tags found during scan"
                    }
                )
                
                Log.d(TAG, "Manual mode - Candidate selection UI ready with ${candidateTags.size} tags")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error building candidate tags for manual selection: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    scanningStatus = ScanningStatus.ERROR,
                    statusMessage = "Error showing candidate tags: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Select a candidate tag from the manual selection list
     */
    fun selectCandidateTag(candidateTag: CandidateTag): Unit {
        Log.d(TAG, "User selected candidate tag: ${candidateTag.epc}")
        
        // Check if selected tag is disposed before processing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Query database for dispose status
                val rfidRecords = databaseManager.database.rfidModuleQueries
                    .selectModulesByRFIDTagNo(candidateTag.epc)
                    .executeAsList()
                
                if (rfidRecords.isNotEmpty()) {
                    val rfidRecord = rfidRecords.first()
                    if (rfidRecord.Dispose == 1L) {
                        // Tag is disposed - show toast and return
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                candidateTags = emptyList(),
                                showTagSelection = false,
                                statusMessage = "Tag ${candidateTag.epc} is DISPOSED and cannot be edited."
                            )
                            
                            // Show toast notification
                            android.widget.Toast.makeText(
                                context,
                                "This tag is already DISPOSED and cannot be selected for editing.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }
                }
                
                // Tag is not disposed - proceed with normal processing
                withContext(Dispatchers.Main) {
                    // Clear candidate selection and process the selected tag
                    _uiState.value = _uiState.value.copy(
                        candidateTags = emptyList(),
                        showTagSelection = false,
                        statusMessage = "Processing selected tag: ${candidateTag.epc}..."
                    )
                    
                    // Process the selected tag from database
                    processTagFromDatabase(candidateTag.epc)
            }
            
        } catch (e: Exception) {
                Log.e(TAG, "Error checking dispose status for tag: ${candidateTag.epc}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        candidateTags = emptyList(),
                        showTagSelection = false,
                        statusMessage = "Error checking tag status: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Handle auto mode - select strongest ACTIVATED tag automatically
     */
    private suspend fun handleAutoModeSelection(): Unit {
        // Find strongest signal tag that is ACTIVATED
        val activatedTags = scannedTags.values.filter { 
            getEnhancedTagStatusFromEpc(it.epc) == EnhancedTagStatus.ACTIVATE
        }
        
        val strongestTag = activatedTags.maxByOrNull { it.rssiDbm }
        if (strongestTag != null) {
            Log.d(TAG, "Auto mode - Processing strongest ACTIVATED tag: ${strongestTag.epc}")
            processTagFromDatabase(strongestTag.epc)
            } else {
            _uiState.value = _uiState.value.copy(
                scanningStatus = ScanningStatus.READY,
                statusMessage = "No ACTIVATED tags found during scan."
            )
        }
    }

    /**
     * Real-time scanning loop with vendor demo performance (1ms intervals)
     * CRITICAL: Just collect tags during scanning, don't do individual reads
     */
    private fun startRealTimeScanningLoop(): Unit {
        scanningJob = viewModelScope.launch {
                try {
                while (isActive && _uiState.value.scanningStatus == ScanningStatus.SCANNING) {
                    
                    // CRITICAL: Read tag from buffer at 1ms intervals (vendor demo pattern)
                    val tagData = uhfManager.readTagFromBuffer()
                    tagData?.let { tag ->
                        // Process TagData object directly - no EPC reading during scan
                        processTagData(tag)
                    }
                    
                    delay(SCAN_INTERVAL_MS) // 1ms delay
                }
            } catch (e: Exception) {
                // Handle coroutine cancellation gracefully
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Scanning loop cancelled (normal when stopping scan)")
                } else {
                    Log.e(TAG, "Scanning loop error: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Process tag data from buffer
     * CRITICAL: Don't read EPC data during scanning - causes conflicts
     */
    private fun processTagData(tagData: com.socam.bcms.model.TagData): Unit {
        try {
            val tid = tagData.tid ?: ""
            val epc = tagData.epc
            val rssiDbm = tagData.rssi
            val rssiRaw = rssiDbm.toString() // Simple string representation for raw value
            
            // Auto mode: Only process ACTIVATED tags (original behavior)
            val enhancedStatus = getEnhancedTagStatusFromEpc(epc)
                if (enhancedStatus != EnhancedTagStatus.ACTIVATE) {
                    Log.d(TAG, "Auto mode - Tag filtered out: EPC=$epc, Status=$enhancedStatus")
                    return
            }
            
            val scanData = TagScanData(
                tid = tid,
                epc = epc,
                rssiRaw = rssiRaw,
                rssiDbm = rssiDbm,
                epcData = epc
            )
            
            // Store strongest RSSI for each unique EPC
            val existing = scannedTags[epc]
            if (existing == null || rssiDbm > existing.rssiDbm) {
                scannedTags[epc] = scanData
                Log.d(TAG, "Tag collected: EPC=$epc, RSSI=$rssiDbm dBm, Status=$enhancedStatus")
                
                // Skip real-time updates - using auto-selection only
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing tag data: ${e.message}", e)
        }
    }
    
    /**
     * Process tag by looking up database record using EPC
     */
    private fun processTagFromDatabase(epc: String): Unit {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    scanningStatus = ScanningStatus.PROCESSING,
                    statusMessage = "Processing tag: $epc..."
                )
                
                withContext(Dispatchers.IO) {
                    // Try to find RfidModule record using RFIDTagNo field (should contain scanned EPC)
                    var rfidRecord = databaseManager.database.rfidModuleQueries
                        .selectModulesByRFIDTagNo(epc)
                        .executeAsList()
                        .firstOrNull()
                    
                    // Fallback: Try TagId field (may contain original hardware EPC)
                    if (rfidRecord == null) {
                        rfidRecord = databaseManager.database.rfidModuleQueries
                            .selectModulesByTagId(epc)
                                        .executeAsList()
                            .firstOrNull()
                    }
                    
                    if (rfidRecord != null) {
                        Log.d(TAG, "Found RfidModule record for EPC: $epc")
                        processRfidModule(rfidRecord)
                    } else {
                        Log.w(TAG, "No RfidModule record found for EPC: $epc")
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                scanningStatus = ScanningStatus.ERROR,
                                statusMessage = "No database record found for tag: $epc"
                            )
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing tag from database: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    scanningStatus = ScanningStatus.ERROR,
                    statusMessage = "Database error: ${e.message}"
                )
            }
        }
    }

    /**
     * Process RfidModule record and update UI with all sections
     */
    private suspend fun processRfidModule(rfidModule: RfidModule): Unit {
        try {
            Log.d(TAG, "Processing RfidModule: BCType=${rfidModule.BCType}, RFIDTagNo=${rfidModule.RFIDTagNo}")
            
            // Store for step form auto-fill
            currentTagData = rfidModule
            
            withContext(Dispatchers.IO) {
                // 1. Build tag details
                val tagDetails = TagDetails(
                    bcType = rfidModule.BCType ?: "Unknown",
                    tagChipId = rfidModule.TagId ?: "N/A",
                    tagNo = rfidModule.RFIDTagNo ?: "N/A"
                )
                
                // 2. Get workflow steps for the BC Type and current user role
                val currentUser = authManager.getCurrentUser()
                val workflowSteps = if (rfidModule.BCType != null && currentUser != null) {
                    val roleSteps = databaseManager.database.masterRolesQueries
                        .selectStepsByRoleAndBcType(currentUser.role, rfidModule.BCType)
                        .executeAsList()
                    
                    // Get full workflow step details with icon and description (aligned with Batch Process)
                    val steps = mutableListOf<WorkflowStepDisplay>()
                    for (roleStep in roleSteps) {
                        val stepDetails = databaseManager.database.masterWorkflowStepsQueries
                            .selectWorkflowStepByKey(roleStep.step_code, rfidModule.BCType)
                            .executeAsOneOrNull()
                        
                        if (stepDetails != null) {
                            // Use localized description (TC first, then EN fallback)
                            val description = stepDetails.step_desc_tc ?: stepDetails.step_desc_en ?: stepDetails.step
                            steps.add(
                                WorkflowStepDisplay(
                                    stepCode = stepDetails.step,
                                    stepDescription = "🔧 ${stepDetails.step}: $description",
                                    isRequired = false
                                )
                            )
                        }
                    }
                    steps.sortedBy { it.stepCode }
                } else {
                    emptyList()
                }
                
                // 3. Get contract and company information
                val contractInfo = if (rfidModule.ContractNo != null) {
                    databaseManager.database.masterContractsQueries
                        .selectContractByNo(rfidModule.ContractNo)
                        .executeAsOneOrNull()
                } else null
                
                val companyInfo = if (contractInfo != null) {
                    databaseManager.database.masterCompaniesQueries
                        .selectCompanyById(contractInfo.id.toString())
                        .executeAsOneOrNull()
                } else null
                
                val tagInfo = TagInformation(
                    contractNo = rfidModule.ContractNo ?: "N/A",
                    contractDescription = contractInfo?.contract_desc_en ?: "N/A",
                    contractor = companyInfo?.name_en ?: "N/A",
                    manufacturerId = rfidModule.ManufacturerId ?: "N/A",
                    manufacturerAddress = "N/A", // Not in current schema
                    tagId = rfidModule.TagId ?: "N/A",
                    bcType = rfidModule.BCType ?: "N/A",
                    tagNo = rfidModule.RFIDTagNo ?: "N/A",
                    asn = rfidModule.ASN ?: "N/A"
                )
                
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        scanningStatus = ScanningStatus.READY,
                        statusMessage = "Tag processed successfully: ${rfidModule.RFIDTagNo}",
                        tagDetails = tagDetails,
                        workflowSteps = workflowSteps,
                        tagInformation = tagInfo,
                        currentRfidModule = rfidModule
                    )
                    Log.d(TAG, "UI updated - Tag Details: ${tagDetails}, Workflow Steps: ${workflowSteps.size}, Tag Info: ${tagInfo}")
                }
            }
            
            } catch (e: Exception) {
            Log.e(TAG, "Error processing RFID module: ${e.message}")
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    scanningStatus = ScanningStatus.ERROR,
                    statusMessage = "Error processing tag data: ${e.message}"
                )
            }
        }
    }

    /**
     * Get enhanced tag status from EPC (first 2 characters)
     */
    private fun getEnhancedTagStatusFromEpc(epc: String): EnhancedTagStatus {
        return when {
            epc.startsWith("34", ignoreCase = true) -> EnhancedTagStatus.ACTIVATE
            else -> EnhancedTagStatus.INACTIVE
        }
    }

    override fun onCleared() {
        super.onCleared()
        isScanning = false
        scanningJob?.cancel()
        uhfManager.stopInventory()
        // Don't power off - keep UHF ready for other modules
    }
}

/**
 * UI State for Single Scan screen
 */
data class SingleScanUiState(
    val scanningStatus: ScanningStatus = ScanningStatus.READY,
    val statusMessage: String = "", // Will be set with localized string in ViewModel init
    val tagDetails: TagDetails? = null,
    val workflowSteps: List<WorkflowStepDisplay> = emptyList(),
    val tagInformation: TagInformation? = null,
    val currentRfidModule: com.socam.bcms.database.RfidModule? = null,
    // Tag Selection for Manual Mode 
    val candidateTags: List<CandidateTag> = emptyList(),
    val showTagSelection: Boolean = false
)


/**
 * Enhanced tag status enumeration
 */
enum class EnhancedTagStatus {
    ACTIVATE,      // EPC starts with "34"
    INACTIVE       // EPC doesn't start with "34"
}

data class TagScanData(
    val tid: String,
    val epc: String,
    val rssiRaw: String,
    val rssiDbm: Int,
    val epcData: String
)

/**
 * Tag details for display
 */
data class TagDetails(
    val bcType: String,
    val tagChipId: String,
    val tagNo: String
)

/**
 * Workflow step for display in grid
 * Format: stepDescription = "🔧 {stepCode}: {localized description}"
 * Aligned with Batch Process Module format
 */
data class WorkflowStepDisplay(
    val stepCode: String,
    val stepDescription: String,
    val isRequired: Boolean
)

/**
 * Tag information for detailed view
 */
data class TagInformation(
    val contractNo: String,
    val contractDescription: String,
    val contractor: String,
    val manufacturerId: String,
    val manufacturerAddress: String,
    val tagId: String,
    val bcType: String,
    val tagNo: String,
    val asn: String
)

/**
 * Enhanced candidate tag for manual selection with all status types and badges
 */
data class CandidateTag(
    val epc: String,
    val rssiDbm: Int,
    val rssiRaw: String,
    val bcType: String,
    val tagNo: String,
    val badgeType: TagBadgeType,
    val isClickable: Boolean
)

/**
 * Tag badge type for visual indication
 */
enum class TagBadgeType(val displayName: String, val colorHex: String) {
    ACTIVATE("ACTIVE", "#4CAF50"),
    INACTIVATE("INACTIVE", "#FF9800"),
    REMOVAL("REMOVE", "#F44336"),
    ACTIVATE_NO_RECORDS("NO RECORD", "#9C27B0"),
    DISPOSED("DISPOSED", "#607D8B")  // Gray color for disposed tags
}

/**
 * Tag scan data for hold-to-scan collection
 */