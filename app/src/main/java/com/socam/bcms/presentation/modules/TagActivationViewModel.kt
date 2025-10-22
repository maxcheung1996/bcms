package com.socam.bcms.presentation.modules

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.socam.bcms.R
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.uhf.UHFManagerWrapper
import com.socam.bcms.model.TagStatus
import com.socam.bcms.model.TagStatusOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.*

/**
 * ViewModel for Tag Activation screen
 * Follows battle-tested UHF patterns from UHF_IMPLEMENTATION_GUIDE.md
 * 
 * Flow: Scan → Filter Inactive Only → Select Strongest → Activate → Write EPC → Database → Show Tag Number
 */
class TagActivationViewModel(
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "TagActivationViewModel"
        // CRITICAL: High-performance scan interval matching vendor demo
        private const val SCAN_INTERVAL_MS = 1L
    }

    private val databaseManager = DatabaseManager.getInstance(context)
    private val authManager = AuthManager.getInstance(context)
    
    // CRITICAL: Use singleton UHF manager from BCMSApp (following vendor demo pattern)
    private val uhfManager: UHFManagerWrapper
        get() = com.socam.bcms.BCMSApp.instance.uhfManager

    // UI State
    private val _uiState = MutableStateFlow(TagActivationUiState())
    val uiState: StateFlow<TagActivationUiState> = _uiState.asStateFlow()

    // BC Type options
    private val _bcTypeOptions = MutableStateFlow<List<String>>(emptyList())
    val bcTypeOptions: StateFlow<List<String>> = _bcTypeOptions.asStateFlow()
    
    // Scanning state
    private var scanningJob: Job? = null
    private val scannedTags = mutableMapOf<String, TagActivationData>()

    init {
        loadBcTypeOptions()
        initializeTagCounter()
        loadLastSelectedBcType()
        
        // CRITICAL: No UHF initialization here - use pre-initialized singleton from BCMSApp
        _uiState.value = _uiState.value.copy(
            statusMessage = context.getString(R.string.tag_activation_ready_message)
        )
        
        Log.d(TAG, "ViewModel initialized - using singleton UHF manager")
    }

    /**
     * Load BC Type options from MasterCategories
     */
    private fun loadBcTypeOptions(): Unit {
        viewModelScope.launch {
            try {
                val categories = withContext(Dispatchers.IO) {
                    databaseManager.database.masterCategoriesQueries.selectAllCategories().executeAsList()
                }
                
                val distinctBcTypes = categories.map { it.bc_type }.distinct().sorted()
                _bcTypeOptions.value = distinctBcTypes
                
                // Set default/preferred BC Type after options are loaded
                setDefaultOrPreferredBcType(distinctBcTypes)
                
                println("TagActivationViewModel: Loaded BC types: $distinctBcTypes")
            } catch (e: Exception) {
                println("TagActivationViewModel: Error loading BC types: ${e.message}")
                // Fallback to default types
                val fallbackTypes = listOf("MIC", "ALW", "TID")
                _bcTypeOptions.value = fallbackTypes
                setDefaultOrPreferredBcType(fallbackTypes)
            }
        }
    }

    /**
     * Load last selected BC Type from AppSettings
     */
    private fun loadLastSelectedBcType(): Unit {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val savedBcType = databaseManager.database.appSettingsQueries
                        .selectSettingByKey("last_selected_bc_type").executeAsOneOrNull()
                    
                    savedBcType?.setting_value?.let { bcType ->
                        println("TagActivationViewModel: Loaded preferred BC Type: $bcType")
                    }
                }
            } catch (e: Exception) {
                println("TagActivationViewModel: Error loading preferred BC Type: ${e.message}")
            }
        }
    }
    
    /**
     * Set default or preferred BC Type after options are loaded
     */
    private suspend fun setDefaultOrPreferredBcType(availableTypes: List<String>): Unit {
        if (availableTypes.isEmpty()) return
        
        try {
            val savedBcType = withContext(Dispatchers.IO) {
                databaseManager.database.appSettingsQueries
                    .selectSettingByKey("last_selected_bc_type").executeAsOneOrNull()?.setting_value
            }
            
            val bcTypeToSelect = when {
                // Use saved preference if it exists and is still available
                savedBcType != null && availableTypes.contains(savedBcType) -> {
                    println("TagActivationViewModel: Using preferred BC Type: $savedBcType")
                    savedBcType
                }
                // Default to first option if no preference or preference not available
                else -> {
                    println("TagActivationViewModel: Using default BC Type: ${availableTypes.first()}")
                    availableTypes.first()
                }
            }
            
            // Update UI state with selected BC Type
            _uiState.value = _uiState.value.copy(bcType = bcTypeToSelect)
            updateActivateButtonState()
            
        } catch (e: Exception) {
            println("TagActivationViewModel: Error setting default BC Type: ${e.message}")
            // Fallback to first option
            _uiState.value = _uiState.value.copy(bcType = availableTypes.first())
            updateActivateButtonState()
        }
    }
    
    /**
     * Save selected BC Type to AppSettings for future use
     */
    private fun saveSelectedBcType(bcType: String): Unit {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    databaseManager.database.appSettingsQueries
                        .insertOrReplaceSettingByKey("last_selected_bc_type", bcType)
                    println("TagActivationViewModel: Saved BC Type preference: $bcType")
                }
            } catch (e: Exception) {
                println("TagActivationViewModel: Error saving BC Type preference: ${e.message}")
            }
        }
    }

    /**
     * Initialize tag counter if not exists
     */
    private fun initializeTagCounter(): Unit {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val existing = databaseManager.database.appSettingsQueries
                        .getTagCounter().executeAsOneOrNull()
                    
                    if (existing == null) {
                        // Initialize counter to 1
                        databaseManager.database.appSettingsQueries
                            .insertOrReplaceTagCounter("1")
                        println("TagActivationViewModel: Initialized tag counter to 1")
                    }
                }
            } catch (e: Exception) {
                println("TagActivationViewModel: Error initializing tag counter: ${e.message}")
            }
        }
    }

    /**
     * Start RFID scanning (hold-to-scan pattern)
     * CRITICAL: Following vendor demo pattern with high-performance scanning
     */
    fun startScanning(): Unit {
        // CRITICAL: Allow scanning even after activation (multi-tag workflow)
        if (_uiState.value.isScanning) return
        
        viewModelScope.launch {
            try {
                val wasActivated = _uiState.value.isActivated
                Log.d(TAG, "Starting scan with vendor demo pattern${if (wasActivated) " (resetting for new tag)" else ""}")
                
                // CRITICAL: Ensure clean UHF state before starting
                uhfManager.stopInventory()
                Thread.sleep(100)
                
                // Clear previous scan results and reset activation state for new scan
                scannedTags.clear()
                
                _uiState.value = _uiState.value.copy(
                    isScanning = true,
                    isTriggerPressed = true,
                    scanningStatus = ScanningStatus.SCANNING,
                    scannedTag = null,           // Clear previous tag
                    isActivated = false,         // Reset activation state
                    fieldsEnabled = true,        // Re-enable fields
                    activatedTagNumber = null,   // Clear previous tag number
                    statusMessage = context.getString(R.string.scanning_inactive_tags),
                    errorMessage = null
                )

                // Start inventory using vendor demo pattern
                val started = uhfManager.startInventory()
                if (started) {
                    startRealTimeScanningLoop()
                } else {
                    handleScanError("Failed to start RFID scanning")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting scan: ${e.message}", e)
                handleScanError("Scan start error: ${e.message}")
            }
        }
    }

    /**
     * Stop RFID scanning and process tags based on selection mode (Auto vs Manual)
     * CRITICAL: Find strongest signal tag in Auto mode, show candidate list in Manual mode
     */
    fun stopScanning(): Unit {
        if (!_uiState.value.isScanning) return
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Stopping scan - Manual selection mode")
                
                // Cancel scanning loop
                scanningJob?.cancel()
                scanningJob = null
                
                // Stop inventory
                uhfManager.stopInventory()
                
                // Filter for INACTIVE tags only (tags that don't start with "34")
                val filteredTags = scannedTags.values.filter { tagData ->
                    !tagData.epc.startsWith("34", ignoreCase = true)
                }.toList()
                
                if (filteredTags.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        isTriggerPressed = false,
                        scanningStatus = ScanningStatus.READY,
                        statusMessage = "No INACTIVE tags found. Only inactive tags can be activated.",
                        needsFocusRestore = true
                    )
                    return@launch
                }
                
                // Always show candidate selection (manual-only mode)
                showCandidateTagsForSelection(filteredTags)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan: ${e.message}", e)
                handleScanError("Scan stop error: ${e.message}")
            }
        }
    }

    /**
     * Real-time scanning loop with vendor demo performance (1ms intervals)
     * CRITICAL: Just collect tags during scanning, don't do individual reads
     */
    private fun startRealTimeScanningLoop(): Unit {
        scanningJob = viewModelScope.launch {
            try {
                while (isActive && _uiState.value.isScanning) {
                    
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
     * Just collect basic tag info, filter later when scanning stops
     */
    private suspend fun processTagData(tagData: com.socam.bcms.model.TagData): Unit {
        try {
            val tid = tagData.tid
            val epc = tagData.epc
            val rssiDbm = tagData.rssi
            val rssiRaw = String.format("%04X", (rssiDbm * 10 + 256 * 256 - 256)) // Convert back to hex format
            
            // CRITICAL: Don't do additional UHF operations during scan
            // EPC from buffer already contains status information
            val activationData = TagActivationData(
                tid = tid,
                epc = epc,
                rssiRaw = rssiRaw,
                rssiDbm = rssiDbm,
                epcData = epc // Use EPC directly (like Tag Modification)
            )
            
            // Update or add tag (keep strongest signal for each EPC)
            val existing = scannedTags[epc]
            if (existing == null || activationData.rssiDbm > existing.rssiDbm) {
                scannedTags[epc] = activationData
                Log.d(TAG, "Tag collected: EPC=$epc, RSSI=${rssiDbm} dBm")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing tag data: ${e.message}", e)
        }
    }
    

    /**
     * Update BC Type selection
     * CRITICAL: Restore focus after dropdown interaction and save preference
     */
    fun updateBcType(bcType: String): Unit {
        _uiState.value = _uiState.value.copy(
            bcType = bcType,
            needsFocusRestore = true, // CRITICAL: Restore focus after dropdown
            statusMessage = if (_uiState.value.scannedTag != null) {
                "BC Type selected: $bcType. Ready to activate tag."
            } else {
                "BC Type selected: $bcType. Scan a tag to activate."
            }
        )
        
        // Save BC Type preference for next time
        saveSelectedBcType(bcType)
        
        updateActivateButtonState()
        Log.d(TAG, "✅ BC Type updated to: $bcType (focus will be restored)")
    }

    /**
     * Clear focus restore flag after Fragment restores focus
     */
    fun clearFocusRestoreFlag(): Unit {
        _uiState.value = _uiState.value.copy(needsFocusRestore = false)
    }
    
    
    /**
     * Show candidate tags for manual selection (INACTIVE tags only)
     */
    private fun showCandidateTagsForSelection(inactiveTags: List<TagActivationData>): Unit {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Manual mode - Building candidate list from ${scannedTags.size} scanned tags")
                
                // Sort by RSSI strength (strongest first) and take top tags
                val sortedTags = inactiveTags
                    .sortedByDescending { it.rssiDbm }
                    .take(10) // Show up to 10 tags for selection
                
                Log.d(TAG, "Manual mode - Showing top ${sortedTags.size} tags for selection")
                
                val candidateTags = sortedTags.mapIndexed { index, tagData ->
                    TagActivationCandidateTag(
                        epc = tagData.epc,
                        rssiDbm = tagData.rssiDbm,
                        rssiRaw = tagData.rssiRaw,
                        tagStatus = TagStatus.INACTIVE // All pre-filtered as INACTIVE
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    candidateTags = candidateTags,
                    showTagSelection = true,
                    statusMessage = if (candidateTags.isNotEmpty()) {
                        "Found ${candidateTags.size} inactive tags. Select one to activate."
                    } else {
                        "No inactive tags found during scan"
                    }
                )
                
                Log.d(TAG, "Manual mode - Candidate selection UI ready with ${candidateTags.size} tags")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error building candidate tags for manual selection: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Error showing candidate tags: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Select a candidate tag from the manual selection list
     */
    fun selectCandidateTag(candidateTag: TagActivationCandidateTag): Unit {
        Log.d(TAG, "User selected candidate tag: ${candidateTag.epc}")
        
        // Convert candidate tag to TagActivationData format for processing
        val selectedTagData = TagActivationData(
            tid = "", // Not available from candidate, but not needed for activation
            epc = candidateTag.epc,
            rssiDbm = candidateTag.rssiDbm,
            rssiRaw = candidateTag.rssiRaw,
            epcData = candidateTag.epc
        )
        
        // Set the selected tag as the scanned tag
        _uiState.value = _uiState.value.copy(
            scannedTag = selectedTagData,
            candidateTags = emptyList(),
            showTagSelection = false,
            statusMessage = "Selected tag: ${candidateTag.epc}. Choose BC Type to activate.",
            needsFocusRestore = true
        )
        
        updateActivateButtonState()
        Log.d(TAG, "✅ Candidate tag selected for activation: ${candidateTag.epc}")
    }

    /**
     * Handle auto mode selection (original behavior)
     */
    private suspend fun handleAutoModeSelection(): Unit {
        // Find strongest signal tag (highest RSSI)
        val strongestTag = scannedTags.values.maxByOrNull { it.rssiDbm }
        
        if (strongestTag != null) {
            Log.d(TAG, "Auto mode - Selected strongest INACTIVE tag: EPC=${strongestTag.epc}, RSSI=${strongestTag.rssiDbm} dBm")
            
            // CRITICAL: Use EPC string directly like Tag Modification (no additional reads)
            val tagStatus = getTagStatusFromEpc(strongestTag.epc)
            val tagWithEpcData = strongestTag.copy(epcData = strongestTag.epc)
            
            if (tagStatus == TagStatus.INACTIVE) {
                val currentBcType = _uiState.value.bcType
                val message = if (currentBcType.isNotBlank()) {
                    "Inactive tag found! BC Type: $currentBcType. Ready to activate."
                } else {
                    "Inactive tag found! EPC: ${strongestTag.epc}. Select BC Type to activate."
                }
                
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    isTriggerPressed = false,
                    scanningStatus = ScanningStatus.READY,
                    scannedTag = tagWithEpcData,
                    statusMessage = message,
                    needsFocusRestore = true
                )
                Log.d(TAG, "✅ Tag is INACTIVE - ready for activation")
            } else {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    isTriggerPressed = false,
                    scanningStatus = ScanningStatus.READY,
                    statusMessage = context.getString(R.string.tag_already_active),
                    needsFocusRestore = true
                )
                Log.d(TAG, "⚠️ Tag is ACTIVE/REMOVED - cannot activate")
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                isTriggerPressed = false,
                scanningStatus = ScanningStatus.READY,
                statusMessage = context.getString(R.string.no_tags_found_scan),
                needsFocusRestore = true
            )
        }
        
        updateActivateButtonState()
    }
    
    
    /**
     * Handle scan errors
     */
    private fun handleScanError(message: String): Unit {
        scanningJob?.cancel()
        scanningJob = null
        
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            isTriggerPressed = false,
            scanningStatus = ScanningStatus.ERROR,
            statusMessage = message,
            errorMessage = message,
            needsFocusRestore = true
        )
    }

    /**
     * Activate tag with EPC writing
     * CRITICAL: Write "ACT" status to tag EPC before database insert
     */
    fun activateTag(): Unit {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val scannedTag = currentState.scannedTag
                
                if (!currentState.canActivate || scannedTag == null) {
                    Log.w(TAG, "Cannot activate - conditions not met")
                    return@launch
                }

                _uiState.value = currentState.copy(
                    isProcessing = true,
                    statusMessage = context.getString(R.string.activating_tag_writing)
                )

                // Step 1: Generate tag number first
                val tagNumber = generateTagNumber(currentState.bcType)
                if (tagNumber == null) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = "Error generating tag number",
                        needsFocusRestore = true
                    )
                    return@launch
                }
                
                // Step 2: Write generated tag number to EPC storage area
                val originalEpc = scannedTag.epc
                
                Log.d(TAG, "Writing generated tag number to EPC: $originalEpc -> $tagNumber")
                
                val writeSuccess = writeEpcStatus(originalEpc, tagNumber)
                
                if (!writeSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = "Failed to write activation status to tag",
                        errorMessage = "EPC write failed",
                        needsFocusRestore = true
                    )
                    return@launch
                }

                // Step 3: Get current user
                val currentUser = authManager.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = "Error: No authenticated user. Please login again.",
                        needsFocusRestore = true
                    )
                    return@launch
                }

                // Step 4: Generate SerialNo
                val serialNo = generateSerialNo(currentUser.contract_no, currentState.bcType)
                
                // Step 5: Create RfidModule record with modified EPC
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Creating database record..."
                )
                
                val success = withContext(Dispatchers.IO) {
                    try {
                        val uuid = UUID.randomUUID().toString()
                        val currentTime = System.currentTimeMillis() / 1000
                        val stepCode = "${currentState.bcType}10"

                        databaseManager.database.rfidModuleQueries.insertModule(
                            Id = uuid,
                            ProjId = currentUser.project_id, // FIXED: Use current user's project_id
                            ContractNo = currentUser.contract_no,
                            ManufacturerId = null,
                            TagId = originalEpc, // CRITICAL: Store original EPC (unchanged, starts with "E")
                            IsActivated = 1,
                            ActivatedDate = currentTime,
                            BCType = currentState.bcType,
                            RFIDTagNo = tagNumber, // Generated tag number
                            StepCode = stepCode,
                            Category = null,
                            Subcategory = null,
                            SupplierId = null,
                            ConcreteGrade = null,
                            ASN = null,
                            SerialNo = serialNo, // Auto-generated SerialNo
                            WorkingNo = null,
                            ManufacturingDate = null,
                            RSCompanyId = null,
                            RSInspectionDate = null,
                            CastingDate = null,
                            FirstCastingDate = null,
                            SecondCastingDate = null,
                            WaterproofingInstallationDate = null,
                            InternalFinishDate = null,
                            DeliveryDate = null,
                            BatchNo = null,
                            LicensePlateNo = null,
                            GpsDeviceId = null,
                            SiteArrivalDate = null,
                            SiteInstallationDate = null,
                            RoomInput = null,
                            RoomId = null,
                            Floor = null,
                            Region = null,
                            ChipFailureSA = 0,
                            ChipFailureSI = 0,
                            IsCompleted10 = 0,
                            Remark10 = null,
                            IsCompleted20 = 0,
                            Remark20 = null,
                            IsCompleted30 = 0,
                            Remark30 = null,
                            IsCompleted40 = 0,
                            Remark40 = null,
                            IsCompleted50 = 0,
                            Remark50 = null,
                            IsCompleted55 = 0,
                            Remark55 = null,
                            IsCompleted60 = 0,
                            Remark60 = null,
                            IsCompleted70 = 0,
                            Remark70 = null,
                            IsCompleted80 = 0,
                            Remark80 = null,
                            Dispose = 0,
                            CreatedBy = currentUser.username,
                            UpdatedBy = currentUser.username,
                            ProductNo = null,
                            CreatedDate = currentTime,
                            UpdatedDate = currentTime,
                            sync_status = "PENDING"
                        )
                        
                        Log.d(TAG, "Tag activated successfully - ID: $uuid, TagNo: $tagNumber, OriginalEPC: $originalEpc, SerialNo: $serialNo")
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating RfidModule record: ${e.message}", e)
                        false
                    }
                }

                if (success) {
                    // Update UI to show success with tag number instead of EPC
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        isActivated = true,
                        fieldsEnabled = false,
                        activatedTagNumber = tagNumber, // Show tag number instead of EPC
                        statusMessage = context.getString(R.string.tag_activated_successfully, tagNumber),
                        needsFocusRestore = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = context.getString(R.string.error_creating_tag_record),
                        errorMessage = context.getString(R.string.database_insert_failed),
                        needsFocusRestore = true
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in activateTag: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    statusMessage = context.getString(R.string.error_format, e.message ?: "Unknown error"),
                    errorMessage = e.message,
                    needsFocusRestore = true
                )
            }
        }
    }

    /**
     * Generate SerialNo with format: ${contract_no}/C/1/4-5/${bcType}/${10-digit-random}
     * Example: 20210573/C/1/4-5/MIC/8475029384
     */
    private fun generateSerialNo(contractNo: String, bcType: String): String {
        // Generate 10-digit random number (0000000000 to 9999999999)
        val randomNumber = kotlin.random.Random.nextLong(0L, 10000000000L) // 0 to 9,999,999,999
        val paddedRandomNumber = String.format("%010d", randomNumber) // Pad to 10 digits with leading zeros
        
        // Fixed literal parts
        val fixedPart = "/C/1/4-5/"
        
        // Build SerialNo: contract_no + fixed_part + bcType + / + random_number
        val serialNo = "$contractNo$fixedPart$bcType/$paddedRandomNumber"
        
        Log.d(TAG, "Generated SerialNo: $serialNo (contract: $contractNo, bcType: $bcType, random: $paddedRandomNumber)")
        return serialNo
    }

    /**
     * Generate tag number based on configurable format: Prefix + Contract + Version + Reserved + BCTypeCode + ContractNo + AutoIncrement
     */
    private suspend fun generateTagNumber(bcType: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                // Get configurable prefix (default: 34180)
                val prefixSetting = databaseManager.database.appSettingsQueries
                    .selectSettingByKey("tag_prefix").executeAsOneOrNull()
                val prefix = prefixSetting?.setting_value ?: "34180"
                
                // Fixed main contract number (03)
                val mainContract = "03"
                
                // Fixed version number (3)
                val version = "3"
                
                // Get configurable reserved number (default: 0)
                val reservedSetting = databaseManager.database.appSettingsQueries
                    .selectSettingByKey("tag_reserved").executeAsOneOrNull()
                val reserved = reservedSetting?.setting_value ?: "0"

                // Get BC Type numeric code
                val bcTypeCode = databaseManager.database.bCTypeMappingQueries
                    .selectNumericCodeByBcType(bcType).executeAsOneOrNull() ?: "404"

                // Get current user's contract number
                val currentUser = authManager.getCurrentUser()
                val contractNo = currentUser?.tag_contract_no ?: "210573"

                // Get and increment counter
                val currentCounterStr = databaseManager.database.appSettingsQueries
                    .getTagCounter().executeAsOneOrNull()
                val currentCounter = currentCounterStr?.toIntOrNull() ?: 1

                // Format auto-increment to 6 digits
                val autoIncrement = String.format("%06d", currentCounter)

                // Increment counter for next use
                databaseManager.database.appSettingsQueries.incrementTagCounter()

                // Build tag number: Prefix + MainContract + Version + Reserved + BCTypeCode + ContractNo + AutoIncrement
                val tagNumber = "$prefix$mainContract$version$reserved$bcTypeCode$contractNo$autoIncrement"
                
                println("TagActivationViewModel: Generated tag number: $tagNumber (Prefix: $prefix, MainContract: $mainContract, Version: $version, Reserved: $reserved, BCType: $bcType -> $bcTypeCode, Contract: $contractNo, Counter: $autoIncrement)")
                tagNumber
            }
        } catch (e: Exception) {
            println("TagActivationViewModel: Error generating tag number: ${e.message}")
            null
        }
    }

    /**
     * Write generated tag number to EPC storage area (following Tag Modification pattern)
     * CRITICAL: Uses EPC filtering for safe write operation
     */
    private suspend fun writeEpcStatus(targetEpc: String, tagNumber: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Writing generated tag number to EPC using vendor demo pattern...")
                
                // CRITICAL: Stop inventory before write (vendor demo pattern)
                uhfManager.stopInventory()
                Thread.sleep(500)
                
                // Use Tag Modification EPC writing pattern - write full tag number
                val result = uhfManager.writeDataToEpc(
                    targetEpc = targetEpc,
                    password = "00000000",
                    startAddress = 2,
                    length = 6,
                    data = tagNumber
                )
                
                if (result) {
                    Log.d(TAG, "✅ EPC write successful: $targetEpc -> $tagNumber")
                } else {
                    Log.w(TAG, "❌ EPC write failed")
                }
                
                result
                
            } catch (e: Exception) {
                Log.e(TAG, "EPC write exception: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Construct new EPC with activation status
     * Replace first 2 characters with "34" (ACTIVE) or "00" (REMOVED)
     */
    private fun constructEpcWithStatus(originalEpc: String, statusHex: String): String {
        return if (originalEpc.length >= 2) {
            statusHex + originalEpc.drop(2)
        } else {
            statusHex + originalEpc
        }
    }
    
    /**
     * Get tag status directly from EPC string (following new prefix-based pattern)
     * CRITICAL: No additional UHF operations - just string checking
     */
    private fun getTagStatusFromEpc(epc: String): TagStatus {
        val epcToCheck = epc.uppercase().replace(" ", "")
        Log.d(TAG, "Checking EPC status: $epcToCheck")
        
        return when {
            epcToCheck.startsWith("34") -> {
                Log.d(TAG, "EPC starts with 34 - tag is ACTIVE")
                TagStatus.ACTIVE
            }
            epcToCheck.startsWith("00") -> {
                Log.d(TAG, "EPC starts with 00 - tag is REMOVED/INACTIVE")
                TagStatus.INACTIVE
            }
            else -> {
                Log.d(TAG, "EPC starts with neither 34 nor 00 - tag is INACTIVE")
                TagStatus.INACTIVE
            }
        }
    }
    
    /**
     * Read EPC data for write operations only (when we need to write to the tag)
     * CRITICAL: Only used during activation write process
     */
    private suspend fun readEpcDataForWrite(epc: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Reading EPC data for write operation: $epc")
                
                val result = uhfManager.readTag(
                    password = "00000000",
                    filterData = epc,
                    memoryBank = 1, // EPC bank
                    startAddress = 2,
                    length = 6
                )
                
                if (result != null) {
                    Log.d(TAG, "✅ EPC read for write successful: $result")
                } else {
                    Log.w(TAG, "⚠️ EPC read for write failed")
                }
                
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error reading EPC for write: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Parse RSSI hex to dBm value
     */
    private fun parseRssi(rssiHex: String): Int {
        return try {
            if (rssiHex.length >= 4) {
                val hb = rssiHex.substring(0, 2).toInt(16)
                val lb = rssiHex.substring(2, 4).toInt(16)
                ((hb - 256 + 1) * 256 + (lb - 256)) / 10
            } else {
                -50 // Default value
            }
        } catch (e: Exception) {
            -50 // Default value on parse error
        }
    }

    /**
     * Update activate button state based on current inputs
     */
    private fun updateActivateButtonState(): Unit {
        val currentState = _uiState.value
        val canActivate = currentState.scannedTag != null && 
                         currentState.bcType.isNotBlank() && 
                         !currentState.isActivated &&
                         !currentState.isProcessing

        _uiState.value = currentState.copy(canActivate = canActivate)
    }

    /**
     * Reset activation state for new tag
     */
    fun resetActivation(): Unit {
        scannedTags.clear()
        _uiState.value = TagActivationUiState()
        updateActivateButtonState()
    }
    
    /**
     * Clean resource management (following UHF guide pattern)
     */
    override fun onCleared() {
        super.onCleared()
        try {
            // Stop operations
            scanningJob?.cancel()
            uhfManager.stopInventory()
            
            // Clear data to prevent leaks
            scannedTags.clear()
            
            // CRITICAL: Don't powerOff - keep ready for other screens
            Log.d(TAG, "Cleared - UHF ready for next use")
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }
}

/**
 * Tag data class for activation
 */
data class TagActivationData(
    val tid: String,
    val epc: String,
    val rssiRaw: String,
    val rssiDbm: Int,
    val epcData: String?
) {
    /**
     * Format RSSI for display
     */
    fun getFormattedRssi(): String {
        return "Raw: $rssiRaw | dBm: ${rssiDbm}"
    }
}


/**
 * Candidate tag for manual selection in Tag Activation
 */
data class TagActivationCandidateTag(
    val epc: String,
    val rssiDbm: Int,
    val rssiRaw: String,
    val tagStatus: TagStatus // INACTIVE for activation candidates
)


/**
 * UI State data class for Tag Activation
 */
data class TagActivationUiState(
    val bcType: String = "",
    val scanningStatus: ScanningStatus = ScanningStatus.READY,
    val statusMessage: String = "", // Will be set with localized string in ViewModel init
    
    // Scanning state
    val isScanning: Boolean = false,
    val isTriggerPressed: Boolean = false,
    val scannedTag: TagActivationData? = null,
    
    // Tag Selection for Manual Mode
    val candidateTags: List<TagActivationCandidateTag> = emptyList(),
    val showTagSelection: Boolean = false,
    val isProcessing: Boolean = false,
    val isActivated: Boolean = false,
    val activatedTagNumber: String? = null, // Show tag number after activation
    val fieldsEnabled: Boolean = true,
    val canActivate: Boolean = false,
    
    // Error handling and focus
    val errorMessage: String? = null,
    val needsFocusRestore: Boolean = false
)


/**
 * ViewModel Factory
 */
class TagActivationViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TagActivationViewModel::class.java)) {
            return TagActivationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
