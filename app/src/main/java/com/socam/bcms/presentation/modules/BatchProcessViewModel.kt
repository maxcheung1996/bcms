package com.socam.bcms.presentation.modules

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.BuildConfig
import com.socam.bcms.R
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.model.TagData
import com.socam.bcms.model.TagStatus
import com.socam.bcms.uhf.UHFManagerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * BatchProcessViewModel - Handles batch processing of multiple RFID tags
 * Key Features:
 * - BC Type filtering with workflow step management
 * - Hold-to-scan pattern with real-time tag list
 * - Batch editing with pen-to-edit functionality 
 * - Individual workflow step saving
 * - Role-based permissions
 */
class BatchProcessViewModel(
    private val context: Context,
    private val databaseManager: DatabaseManager,
    private val authManager: AuthManager
) : ViewModel() {

    companion object {
        private const val TAG = "BatchProcessViewModel"
        private const val SCAN_INTERVAL_MS = 1L // High performance scanning like other modules
    }

    // Use singleton UHF manager from Application
    private val uhfManager: UHFManagerWrapper
        get() = com.socam.bcms.BCMSApp.instance.uhfManager

    // UI State
    private val _uiState = MutableStateFlow(BatchProcessUiState())
    val uiState: StateFlow<BatchProcessUiState> = _uiState.asStateFlow()

    // Scanning state management
    private var isScanning = false
    private var scanningJob: Job? = null
    private val scannedTags = mutableMapOf<String, BatchTagData>()

    // Available BC Types
    private val availableBcTypes = listOf("MIC", "ALW", "TID")

    init {
        Log.d(TAG, "BatchProcessViewModel initialized - using singleton UHF manager")
        loadInitialData()
    }

    /**
     * Load initial data - BC types and default workflow steps
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Set default BC type (first one)
                val defaultBcType = availableBcTypes.first()
                
                _uiState.value = _uiState.value.copy(
                    availableBcTypes = availableBcTypes,
                    selectedBcType = defaultBcType,
                    statusMessage = context.getString(R.string.batch_ready_scan_format, defaultBcType)
                )

                // Load workflow steps for default BC type
                loadWorkflowStepsForBcType(defaultBcType)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = context.getString(R.string.error_loading_initial_data_format, e.message ?: "Unknown error")
                )
            }
        }
    }

    /**
     * Handle BC Type selection change
     */
    fun selectBcType(bcType: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "BC Type changed to: $bcType")
                
                // Stop scanning if active
                if (isScanning) {
                    stopScanning()
                }
                
                // Clear current tag list
                clearTagList()
                
                // Update BC type and load corresponding workflow steps
                _uiState.value = _uiState.value.copy(
                    selectedBcType = bcType,
                    statusMessage = "Ready to scan $bcType tags. Hold trigger to scan."
                )
                
                // Load workflow steps for new BC type
                loadWorkflowStepsForBcType(bcType)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error changing BC type: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Error changing BC type: ${e.message}"
                )
            }
        }
    }

    /**
     * Load workflow steps for given BC type and current user role
     */
    private suspend fun loadWorkflowStepsForBcType(bcType: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading workflow steps for BC Type: $bcType")
                
                // Get current user's role
                val currentUser = authManager.getCurrentUser()
                val userRole = currentUser?.role ?: "Client"
                Log.d(TAG, "Current user role: $userRole")

                // Get allowed steps for this role and BC type
                val allowedSteps = databaseManager.database.masterRolesQueries
                    .selectStepsByRoleAndBcTypeAndProject(userRole, bcType, BuildConfig.PROJECT_ID)
                    .executeAsList()
                
                Log.d(TAG, "Found ${allowedSteps.size} allowed steps for role $userRole and BC type $bcType")

                // Get workflow step details for allowed steps
                val workflowSteps = mutableListOf<WorkflowStepDisplay>()
                
                for (roleStep in allowedSteps) {
                    val stepDetails = databaseManager.database.masterWorkflowStepsQueries
                        .selectWorkflowStepByKey(roleStep.step_code, bcType)
                        .executeAsOneOrNull()
                    
                    if (stepDetails != null) {
                        val description = stepDetails.step_desc_tc ?: stepDetails.step_desc_en ?: stepDetails.step
                        workflowSteps.add(
                            WorkflowStepDisplay(
                                stepCode = stepDetails.step,
                                stepDescription = "🔧 ${stepDetails.step}: $description",
                                isRequired = false
                            )
                        )
                        Log.d(TAG, "Added workflow step: ${stepDetails.step}")
                    }
                }

                // Sort steps by step code
                val sortedSteps = workflowSteps.sortedBy { it.stepCode }
                
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        workflowSteps = sortedSteps
                    )
                    Log.d(TAG, "Updated UI with ${sortedSteps.size} workflow steps")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading workflow steps: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        workflowSteps = emptyList(),
                        statusMessage = "Error loading workflow steps: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Start UHF scanning (hold-to-scan pattern)
     */
    fun startScanning() {
        if (isScanning) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting batch scanning for BC Type: ${_uiState.value.selectedBcType}")
                
                // Ensure clean UHF state
                uhfManager.stopInventory()
                Thread.sleep(100)
                
                _uiState.value = _uiState.value.copy(
                    isScanning = true,
                    statusMessage = "Scanning ${_uiState.value.selectedBcType} tags... (hold trigger)"
                )

                // Start UHF inventory
                val started = uhfManager.startInventory()
                if (started) {
                    isScanning = true
                    startRealTimeScanningLoop()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        statusMessage = "Failed to start RFID scanning"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting scan: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    statusMessage = "Scan start error: ${e.message}"
                )
            }
        }
    }

    /**
     * Stop UHF scanning (release-to-stop pattern)
     */
    fun stopScanning() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Stopping batch scanning")
                
                uhfManager.stopInventory()
                isScanning = false
                scanningJob?.cancel()
                scanningJob = null
                
                val tagCount = scannedTags.size
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    statusMessage = "Scan completed. Found $tagCount ${_uiState.value.selectedBcType} tags."
                )
                
                Log.d(TAG, "Scanning stopped. Total tags: $tagCount")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    statusMessage = "Scan stop error: ${e.message}"
                )
            }
        }
    }

    /**
     * Real-time scanning loop (vendor demo pattern)
     */
    private fun startRealTimeScanningLoop() {
        scanningJob = viewModelScope.launch {
            while (isScanning && _uiState.value.isScanning) {
                try {
                    val tagData = uhfManager.readTagFromBuffer()
                    tagData?.let { tag ->
                        processScannedTag(tag)
                    }
                    
                    delay(SCAN_INTERVAL_MS)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Scanning loop error: ${e.message}")
                    delay(100)
                }
            }
        }
    }

    /**
     * Process scanned tag with BC type and activation filtering
     */
    private suspend fun processScannedTag(tagData: TagData) {
        withContext(Dispatchers.IO) {
            try {
                val epc = tagData.epc
                
                // Check if tag is activated (following Single Scan pattern)
                val tagStatus = getTagStatusFromEpc(epc)
                if (tagStatus != TagStatus.ACTIVE) {
                    Log.d(TAG, "Tag $epc is not activated - skipping")
                    return@withContext
                }

                // Get tag details from database by RFIDTagNo (now contains scanned EPC)
                val rfidRecords = databaseManager.database.rfidModuleQueries
                    .selectModulesByRFIDTagNo(epc)
                    .executeAsList()

                if (rfidRecords.isEmpty()) {
                    Log.d(TAG, "Tag $epc not found in database - skipping")
                    return@withContext
                }

                val rfidRecord = rfidRecords.first()
                
                // Check if tag is disposed - filter out disposed tags
                if (rfidRecord.Dispose == 1L) {
                    Log.d(TAG, "Tag $epc is DISPOSED - filtering out from batch list")
                    return@withContext
                }
                
                val dbBcType = rfidRecord.BCType

                // Check if BC type matches current filter
                val selectedBcType = _uiState.value.selectedBcType
                if (dbBcType != selectedBcType) {
                    Log.d(TAG, "Tag $epc has BC type $dbBcType, but filtering for $selectedBcType - skipping")
                    return@withContext
                }

                // Create or update batch tag data
                val batchTagData = BatchTagData(
                    epc = epc,
                    tid = tagData.tid,
                    bcType = dbBcType ?: selectedBcType,
                    tagNumber = rfidRecord.RFIDTagNo ?: "N/A",
                    rssiDbm = tagData.rssi,
                    rfidRecordId = rfidRecord.Id,
                    timestamp = System.currentTimeMillis()
                )

                // Add/update in scanned tags map
                val existing = scannedTags[epc]
                if (existing == null || batchTagData.isStrongerThan(existing)) {
                    scannedTags[epc] = batchTagData
                    Log.d(TAG, "Added non-disposed tag to batch: $epc")
                    
                    // Update UI with sorted tag list (strongest first)
                    val sortedTags = scannedTags.values.sortedByDescending { it.rssiDbm }
                    
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            scannedTags = sortedTags,
                            statusMessage = "Scanning... ${scannedTags.size} active $selectedBcType tags found"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing scanned tag: ${e.message}", e)
            }
        }
    }

    /**
     * Get tag status from EPC (following Single Scan pattern)
     */
    private fun getTagStatusFromEpc(epc: String): TagStatus {
        val epcToCheck = epc.uppercase().replace(" ", "")
        return when {
            epcToCheck.startsWith("34") -> TagStatus.ACTIVE   // Starts with "34"
            epcToCheck.startsWith("00") -> TagStatus.INACTIVE // Starts with "00"
            else -> TagStatus.INACTIVE                          // Unknown = Inactive
        }
    }

    /**
     * Remove individual tag from list
     */
    fun removeTag(epc: String) {
        scannedTags.remove(epc)
        val sortedTags = scannedTags.values.sortedByDescending { it.rssiDbm }
        _uiState.value = _uiState.value.copy(
            scannedTags = sortedTags,
            statusMessage = "${scannedTags.size} ${_uiState.value.selectedBcType} tags in list"
        )
        Log.d(TAG, "Removed tag: $epc. Remaining tags: ${scannedTags.size}")
    }

    /**
     * Clear all tags from list
     */
    fun clearTagList() {
        scannedTags.clear()
        _uiState.value = _uiState.value.copy(
            scannedTags = emptyList(),
            statusMessage = "Tag list cleared. Ready to scan ${_uiState.value.selectedBcType} tags."
        )
        Log.d(TAG, "Tag list cleared")
    }

    override fun onCleared() {
        super.onCleared()
        try {
            isScanning = false
            scanningJob?.cancel()
            uhfManager.stopInventory()
            scannedTags.clear()
            Log.d(TAG, "ViewModel cleared - UHF ready for next use")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}

/**
 * UI State for Batch Process screen
 */
data class BatchProcessUiState(
    val availableBcTypes: List<String> = emptyList(),
    val selectedBcType: String = "",
    val workflowSteps: List<WorkflowStepDisplay> = emptyList(),
    val scannedTags: List<BatchTagData> = emptyList(),
    val isScanning: Boolean = false,
    val statusMessage: String = "Loading..."
)

/**
 * Simplified batch tag data for dialog passing
 */
data class BatchTagData(
    val epc: String,
    val tid: String,
    val bcType: String,
    val tagNumber: String,
    val rssiDbm: Int,
    val rfidRecordId: String, // Just pass the ID, re-query in dialog
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isStrongerThan(other: BatchTagData): Boolean = this.rssiDbm > other.rssiDbm
    
    fun getFormattedRssi(): String = "${rssiDbm} dBm"
    
    fun getDisplayName(): String = "$bcType: $tagNumber"
}

