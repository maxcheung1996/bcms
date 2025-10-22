package com.socam.bcms.presentation.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.model.MemoryBank
import com.socam.bcms.model.ScanMode
import com.socam.bcms.model.TagModificationData
import com.socam.bcms.model.TagModificationUiState
import com.socam.bcms.model.TagStatus
import com.socam.bcms.model.TagStatusOption
import com.socam.bcms.uhf.UHFManagerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Tag Modification screen
 * Handles UHF scanning with press-and-hold trigger behavior
 * Modifies EPC data for tag status (more universally supported than USER bank)
 */
class TagModificationViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "TagModificationVM"
        // CRITICAL: Vendor demo uses ~1ms interval for high performance scanning
        // Changed from 50ms to match vendor demo's GetRFIDThread pattern
        private const val SCAN_INTERVAL_MS = 1L
    }
    
    // UHF Manager - Use singleton from Application (following vendor demo pattern)
    // CRITICAL: Vendor demo uses singleton UHFManager stored in Application class
    private val uhfManager: UHFManagerWrapper
        get() = com.socam.bcms.BCMSApp.instance.uhfManager
    
    private val _uiState = MutableStateFlow(TagModificationUiState())
    val uiState: StateFlow<TagModificationUiState> = _uiState.asStateFlow()
    
    private var scanningJob: Job? = null
    private val scannedTags = mutableListOf<TagModificationData>()
    
    init {
        // UHF is now initialized centrally in BCMSApp (following vendor demo pattern)
        // No need to initialize or power on here - just set ready status
        _uiState.value = _uiState.value.copy(
            statusMessage = "Ready to scan. Press and hold trigger to scan."
        )
        
        println("$TAG: ViewModel initialized - using singleton UHF manager from Application")
    }
    
    /**
     * Set UHF transmission power (5-33 dBm)
     */
    fun setPowerLevel(power: Int) {
        viewModelScope.launch {
            try {
                val success = uhfManager.setPower(power)
                if (success) {
                    _uiState.value = _uiState.value.copy(powerLevel = power)
                    println("TagModificationViewModel: Power set to $power dBm")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to set power level to $power dBm"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Power setting error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Start scanning (called on trigger press)
     */
    fun startScanning() {
        if (_uiState.value.isScanning) {
            return // Already scanning
        }
        
        viewModelScope.launch {
            try {
                if (!uhfManager.isReady()) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "UHF not ready. Please restart the module.",
                        errorMessage = "UHF module not ready"
                    )
                    return@launch
                }
                
                // Ensure clean start - especially important after write operations
                println("$TAG: Starting scan - ensuring clean UHF state...")
                uhfManager.stopInventory() // Clean any previous state
                Thread.sleep(100) // Brief pause
                
                // Start UHF inventory
                val started = uhfManager.startInventory()
                println("$TAG: UHF inventory start result: $started")
                
                if (started) {
                    _uiState.value = _uiState.value.copy(
                        isScanning = true,
                        isTriggerPressed = true,
                        statusMessage = "Scanning... Release trigger to stop.",
                        errorMessage = null,
                        canWrite = false // Disable write while scanning
                    )
                    
                    // Clear previous scan results
                    scannedTags.clear()
                    
                    // Start continuous scanning loop with real-time updates
                    startRealTimeScanningLoop()
                } else {
                    // Enhanced debugging for startInventory failure
                    println("$TAG: ❌ UHF startInventory() failed - this may be due to hardware state after write")
                    
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Failed to start RFID scanning. Try again or restart app.",
                        errorMessage = "UHF startInventory() returned false - hardware may need reset"
                    )
                    
                    // Additional diagnostic info
                    val isReady = uhfManager.isReady()
                    println("$TAG: UHF ready status during scan failure: $isReady")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Error starting scan: ${e.message}",
                    errorMessage = "Scanning start exception: ${e.message}",
                    needsFocusRestore = true // CRITICAL: Restore focus even after scan start exceptions
                )
            }
        }
    }
    
    /**
     * Stop scanning (called on trigger release)
     */
    fun stopScanning() {
        if (!_uiState.value.isScanning) {
            return // Not scanning
        }
        
        viewModelScope.launch {
            try {
                // Cancel scanning job
                scanningJob?.cancel()
                scanningJob = null
                
                // Stop UHF inventory (following vendor demo pattern for operation mode)
                val stopped = uhfManager.stopInventory()
                println("$TAG: Inventory stopped for operation mode: $stopped")
                
                // Handle scan completion based on current mode
                when (_uiState.value.currentScanMode) {
                    ScanMode.SINGLE -> handleSingleScanCompletion()
                    ScanMode.MULTIPLE -> handleMultipleScanCompletion()
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    isTriggerPressed = false,
                    statusMessage = "Error stopping scan: ${e.message}",
                    errorMessage = "Scanning stop exception: ${e.message}",
                    needsFocusRestore = true // CRITICAL: Restore focus even after scan stop exceptions
                )
            }
        }
    }
    
    /**
     * Real-time scanning loop with live UI updates
     */
    private fun startRealTimeScanningLoop() {
        scanningJob = viewModelScope.launch {
            while (isActive && _uiState.value.isScanning) {
                try {
                    // Read tag data from buffer
                    val tagData = uhfManager.readTagFromBuffer()
                    tagData?.let { tag ->
                        // Read full EPC data
                        val epcData = readEpcData(tag.epc)
                        
                        // Create TagModificationData
                        val modificationData = TagModificationData(
                            tid = tag.tid,
                            epc = tag.epc,
                            rssiRaw = tag.rssi.toString(), // Convert int to string for raw display
                            rssiDbm = tag.rssi,
                            epcData = epcData
                        )
                        
                        // REAL-TIME FILTERING: Only add/update tags that pass current filters
                        if (passesCurrentFilters(modificationData)) {
                            // Handle based on current scan mode
                            when (_uiState.value.currentScanMode) {
                                ScanMode.SINGLE -> handleSingleScanUpdate(modificationData)
                                ScanMode.MULTIPLE -> handleMultipleScanUpdate(modificationData)
                            }
                        } else {
                            // Tag filtered out - log for debugging
                            val statusInfo = modificationData.getStatusDisplayInfo()
                            println("$TAG: Tag filtered out - ${statusInfo.displayName} EPC: ${modificationData.epc}")
                        }
                        
                        println("TagModificationViewModel: Real-time scan - EPC: ${tag.epc}, RSSI: ${tag.rssi} dBm")
                    }
                    
                    delay(SCAN_INTERVAL_MS)
                    
                } catch (e: Exception) {
                    println("TagModificationViewModel: Scanning loop error: ${e.message}")
                    delay(100) // Brief delay on error
                }
            }
        }
    }

    /**
     * Handle real-time updates for Single Scan mode
     */
    private fun handleSingleScanUpdate(modificationData: TagModificationData) {
        // Add/update scanned tags (avoid duplicates based on EPC)
        val existingIndex = scannedTags.indexOfFirst { it.epc == modificationData.epc }
        if (existingIndex >= 0) {
            // Update existing tag if this one has stronger signal
            if (modificationData.isStrongerThan(scannedTags[existingIndex])) {
                scannedTags[existingIndex] = modificationData
            }
        } else {
            scannedTags.add(modificationData)
        }
        
        // Real-time update: Show strongest signal tag immediately
        val currentStrongest = findStrongestSignalTag()
        currentStrongest?.let { strongest ->
            val statusInfo = strongest.getStatusDisplayInfo()
            _uiState.value = _uiState.value.copy(
                lastScanResult = strongest,
                statusMessage = "Scanning... ${statusInfo.displayName} RSSI: ${strongest.rssiDbm} dBm (${scannedTags.size} tags)"
            )
        }
        
        println("$TAG: Single scan update - EPC: ${modificationData.epc}, RSSI: ${modificationData.rssiDbm} dBm")
    }

    /**
     * Handle real-time updates for Multiple Scan mode
     */
    private fun handleMultipleScanUpdate(modificationData: TagModificationData) {
        val currentState = _uiState.value
        val currentList = currentState.multipleScanResults.toMutableList()
        
        // Add/update tag in multiple scan list
        val existingIndex = currentList.indexOfFirst { it.epc == modificationData.epc }
        if (existingIndex >= 0) {
            // Update existing tag if this one has stronger signal
            if (modificationData.isStrongerThan(currentList[existingIndex])) {
                currentList[existingIndex] = modificationData
                println("$TAG: Updated existing tag - EPC: ${modificationData.epc}, RSSI: ${modificationData.rssiDbm} dBm")
            }
        } else {
            // Add new tag
            currentList.add(modificationData)
            println("$TAG: Added new tag - EPC: ${modificationData.epc}, RSSI: ${modificationData.rssiDbm} dBm")
        }
        
        // Sort by RSSI (strongest signal first) for real-time highlighting/sorting
        currentList.sortByDescending { it.rssiDbm }
        
        // Update UI state with new list
        _uiState.value = currentState.copy(
            multipleScanResults = currentList,
            statusMessage = "Scanning... ${currentList.size} tags found (real-time)"
        )
    }

    /**
     * Handle scan completion for Single Scan mode
     */
    private fun handleSingleScanCompletion() {
        // Find the tag with strongest signal (closest)
        val strongestTag = findStrongestSignalTag()
        
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            isTriggerPressed = false,
            statusMessage = if (strongestTag != null) {
                "Ready for write operation. Found tag with RSSI: ${strongestTag.rssiDbm} dBm"
            } else {
                "Scan complete. No tags found."
            },
            lastScanResult = strongestTag,
            canWrite = strongestTag != null && _uiState.value.selectedStatusOption != null,
            needsFocusRestore = true // CRITICAL: Restore focus after scan operations too
        )
        
        println("$TAG: Single scan completed. Found ${scannedTags.size} tags, strongest: ${strongestTag?.rssiDbm} dBm")
    }

    /**
     * Handle scan completion for Multiple Scan mode
     */
    private fun handleMultipleScanCompletion() {
        val finalList = _uiState.value.multipleScanResults
        
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            isTriggerPressed = false,
            statusMessage = if (finalList.isNotEmpty()) {
                "Scan complete. Found ${finalList.size} tags (sorted by signal strength)"
            } else {
                "Scan complete. No tags found."
            },
            needsFocusRestore = true // CRITICAL: Restore focus after scan operations too
        )
        
        println("$TAG: Multiple scan completed. Found ${finalList.size} tags")
        
        // Log all found tags for debugging
        finalList.forEachIndexed { index, tag ->
            val statusInfo = tag.getStatusDisplayInfo()
            println("$TAG: #${index + 1} - ${statusInfo.displayName} EPC: ${tag.epc} RSSI: ${tag.rssiDbm} dBm")
        }
    }

    /**
     * Read EPC memory bank data for a specific tag
     */
    private suspend fun readEpcData(epc: String): String? {
        return try {
            uhfManager.readTag(
                password = "00000000",
                filterBank = MemoryBank.EPC.value,
                filterAddress = 2,
                filterLength = epc.length / 4, // Convert hex string length to word count
                filterData = epc,
                memoryBank = MemoryBank.EPC.value, // Read from EPC bank
                startAddress = 2,
                length = 6 // Read 6 words (12 bytes) from EPC bank
            )
        } catch (e: Exception) {
            println("TagModificationViewModel: Failed to read EPC data for EPC $epc: ${e.message}")
            null
        }
    }
    
    /**
     * Find the tag with the strongest signal (highest RSSI = closest distance)
     */
    private fun findStrongestSignalTag(): TagModificationData? {
        return scannedTags.maxByOrNull { it.rssiDbm }
    }
    
    /**
     * Set the selected status option for writing
     */
    fun setSelectedStatusOption(option: TagStatusOption?) {
        _uiState.value = _uiState.value.copy(
            selectedStatusOption = option,
            canWrite = option != null && 
                      _uiState.value.lastScanResult != null && 
                      !_uiState.value.isTriggerPressed &&
                      !_uiState.value.isWriting
        )
    }
    
    /**
     * Write selected status data to the currently scanned tag
     */
    fun writeTagStatus() {
        val currentState = _uiState.value
        val targetTag = currentState.lastScanResult
        val statusOption = currentState.selectedStatusOption
        
        if (targetTag == null || statusOption == null) {
            _uiState.value = currentState.copy(
                errorMessage = "No tag scanned or status selected"
            )
            return
        }
        
        if (currentState.isTriggerPressed || currentState.isScanning) {
            _uiState.value = currentState.copy(
                errorMessage = "Cannot write while scanning. Please release trigger first."
            )
            return
        }
        
        viewModelScope.launch {
            try {
                // Set writing state
                _uiState.value = _uiState.value.copy(
                    isWriting = true,
                    canWrite = false,
                    statusMessage = "Writing ${statusOption.displayName} to tag EPC (following vendor pattern)...",
                    errorMessage = null
                )
                
                // Perform EPC write operation on IO thread
                val success = withContext(Dispatchers.IO) {
                    // Create new EPC with status suffix
                    val originalEpc = targetTag.epcData ?: targetTag.epc
                    val newEpcWithStatus = if (originalEpc.length >= 2) {
                        // Replace first 2 characters with status prefix
                        statusOption.hexValue + originalEpc.drop(2)
                    } else {
                        // If EPC too short, use status as prefix
                        statusOption.hexValue + originalEpc
                    }
                    
                    // CRITICAL: Use EPC filtering to target specific tag (vendor demo pattern)
                    // This solves the NO_TAG_ERR by telling the UHF module exactly which tag to write to
                    uhfManager.writeDataToEpc(
                        targetEpc = targetTag.epc,      // Use original EPC for filtering/targeting
                        password = "00000000",
                        startAddress = 2,
                        length = 6,
                        data = newEpcWithStatus
                    )
                }
                
                if (success) {
                    // Update the current tag's EPC data immediately
                    val updatedTag = targetTag.copy(
                        epcData = targetTag.epcData?.let { originalEpc ->
                            if (originalEpc.length >= 2) {
                                statusOption.hexValue + originalEpc.drop(2)
                            } else {
                                statusOption.hexValue + originalEpc
                            }
                        } ?: (statusOption.hexValue + targetTag.epc.drop(2))
                    )
                    
                    // CRITICAL: Prepare UHF for next scan after successful write
                    withContext(Dispatchers.IO) {
                        try {
                            // Brief wait for hardware to settle after write
                            println("$TAG: Write successful - preparing UHF for next scan...")
                            Thread.sleep(500)
                            
                            // Ensure UHF is clean and ready for next scan operation
                            uhfManager.stopInventory() // Clean up write operation
                            Thread.sleep(200)
                            
                            // Test if UHF is responsive for next scan (without calling powerOn)
                            val isReady = uhfManager.isReady()
                            println("$TAG: UHF scan readiness after write: $isReady")
                            
                            if (isReady) {
                                println("$TAG: ✅ UHF ready for next trigger press")
                            } else {
                                println("$TAG: ⚠️ UHF may need reinitialization for next scan")
                            }
                        } catch (e: Exception) {
                            println("$TAG: Error preparing UHF for next scan: ${e.message}")
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isWriting = false,
                        canWrite = true,
                        statusMessage = "✅ Successfully wrote ${statusOption.displayName} to tag. Ready for next scan.",
                        lastScanResult = updatedTag,
                        needsFocusRestore = true // Signal Fragment to restore focus
                    )
                    
                    // Trigger success toast message
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "WRITE_SUCCESS:Successfully updated tag status to ${statusOption.displayName}"
                    )
                    
                } else {
                    // CRITICAL: Ensure clean state after failed write without corrupting hardware
                    withContext(Dispatchers.IO) {
                        try {
                            println("$TAG: Write failed - ensuring clean UHF state...")
                            Thread.sleep(500)
                            
                            // Just stop inventory - don't call powerOn() which can corrupt hardware
                            uhfManager.stopInventory()
                            println("$TAG: UHF cleaned after failed write")
                        } catch (e: Exception) {
                            println("$TAG: Error cleaning UHF state after failure: ${e.message}")
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isWriting = false,
                        canWrite = true,
                        statusMessage = "❌ Failed to write to tag",
                        errorMessage = "WRITE_ERROR:Write operation failed. Please try again.",
                        needsFocusRestore = true // CRITICAL: Restore focus even after failed writes
                    )
                }
                
            } catch (e: Exception) {
                // CRITICAL: Clean UHF state after exception without corrupting hardware
                withContext(Dispatchers.IO) {
                    try {
                        println("$TAG: Write exception - ensuring clean UHF state...")
                        Thread.sleep(500)
                        
                        // Just stop inventory - avoid powerOn() which corrupts hardware
                        uhfManager.stopInventory()
                        println("$TAG: UHF cleaned after write exception")
                    } catch (restoreEx: Exception) {
                        println("$TAG: Error cleaning UHF state after exception: ${restoreEx.message}")
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isWriting = false,
                    canWrite = true,
                    statusMessage = "❌ Write error: ${e.message}",
                    errorMessage = "WRITE_ERROR:Write exception: ${e.message}",
                    needsFocusRestore = true // CRITICAL: Restore focus even after write exceptions
                )
            }
        }
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Clear focus restore flag (called by Fragment after restoring focus)
     */
    fun clearFocusRestoreFlag() {
        _uiState.value = _uiState.value.copy(needsFocusRestore = false)
    }

    /**
     * Switch between Single and Multiple scan modes
     */
    fun setScanMode(mode: ScanMode) {
        // Stop any ongoing scanning when switching modes
        if (_uiState.value.isScanning) {
            stopScanning()
        }
        
        _uiState.value = _uiState.value.copy(
            currentScanMode = mode,
            statusMessage = when (mode) {
                ScanMode.SINGLE -> "Ready to scan. Press and hold trigger to scan."
                ScanMode.MULTIPLE -> "Ready to scan multiple tags. Press and hold trigger to scan."
            }
        )
        
        println("$TAG: Scan mode switched to: ${mode.name}")
    }
    
    /**
     * Toggle Active filter checkbox
     */
    fun setActiveFilter(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showActiveFilter = enabled)
        println("$TAG: Active filter set to: $enabled")
    }
    
    /**
     * Toggle Inactive filter checkbox
     */
    fun setInactiveFilter(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showInactiveFilter = enabled)
        println("$TAG: Inactive filter set to: $enabled")
    }
    
    /**
     * Check if a tag passes the current filtering criteria
     * Returns true if tag should be displayed based on current filters
     */
    private fun passesCurrentFilters(tag: TagModificationData): Boolean {
        val currentState = _uiState.value
        val tagStatus = tag.getTagStatus()
        
        return when {
            // If both filters are enabled, show all tags
            currentState.showActiveFilter && currentState.showInactiveFilter -> true
            // If only Active filter is enabled, show only Active tags
            currentState.showActiveFilter && !currentState.showInactiveFilter -> tagStatus == TagStatus.ACTIVE
            // If only Inactive filter is enabled, show only Inactive tags
            !currentState.showActiveFilter && currentState.showInactiveFilter -> tagStatus == TagStatus.INACTIVE
            // If no filters are enabled, show no tags
            else -> false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            // Clean shutdown sequence
            println("$TAG: ViewModel clearing - performing clean UHF shutdown...")
            
            // Stop any ongoing operations
            scanningJob?.cancel()
            scanningJob = null
            
            // Ensure inventory is stopped
            uhfManager.stopInventory()
            
            // Clear scan results to prevent memory leaks
            scannedTags.clear()
            
            // Reset UI state
            _uiState.value = TagModificationUiState()
            
            // Don't power off - keep UHF ready like SingleScanViewModel pattern
            println("$TAG: Cleared - UHF stopped but ready for next use")
        } catch (e: Exception) {
            println("$TAG: Error during ViewModel cleanup: ${e.message}")
        }
    }
}
