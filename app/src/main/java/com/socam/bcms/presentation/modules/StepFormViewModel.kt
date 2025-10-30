package com.socam.bcms.presentation.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.BuildConfig
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.database.WorkflowStepFields
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing workflow step form data
 */
class StepFormViewModel(
    private val databaseManager: DatabaseManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepFormUiState())
    val uiState: StateFlow<StepFormUiState> = _uiState.asStateFlow()

    private var currentTagBcType: String = ""
    private var currentRfidModule: com.socam.bcms.database.RfidModule? = null
    private var currentStepPortion: Int? = null

    /**
     * Load step fields for the given step code and auto-fill with tag data
     */
    fun loadStepFields(stepCode: String, tagBcType: String, tagData: com.socam.bcms.database.RfidModule? = null): Unit {
        currentTagBcType = tagBcType
        currentRfidModule = tagData
        viewModelScope.launch {
            try {
                println("StepFormViewModel: Loading fields for step: $stepCode, BC type: $tagBcType")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                // Get portion from MasterWorkflowSteps
                val portion = getStepPortion(stepCode, tagBcType)
                if (portion == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Step $stepCode not found in workflow for BC type $tagBcType"
                    )
                    return@launch
                }
                currentStepPortion = portion
                println("StepFormViewModel: Step portion from MasterWorkflowSteps: $portion")

                val stepFields = withContext(Dispatchers.IO) {
                    databaseManager.database.workflowStepFieldsQueries
                        .selectFieldsByStepAndProject(stepCode, BuildConfig.PROJECT_ID)
                        .executeAsList()
                }

                println("StepFormViewModel: Found ${stepFields.size} fields for step $stepCode")
                stepFields.forEach { field ->
                    println("StepFormViewModel: Field - ${field.field_name}: ${field.field_type}")
                }

                val fieldData = stepFields.map { field ->
                    val autoFilledValue = if (tagData != null && currentStepPortion != null) {
                        getAutoFilledValue(field.field_name, currentStepPortion!!, tagData)
                    } else {
                        field.default_value ?: ""
                    }
                    
                    StepFieldData(
                        fieldName = field.field_name,
                        fieldLabel = field.field_label ?: field.field_name,
                        fieldType = field.field_type,
                        fieldOrder = field.field_order.toInt(),
                        isRequired = field.is_required == 1L,
                        defaultValue = field.default_value,
                        currentValue = autoFilledValue,
                        dropdownOptions = if (field.field_type == "dropdown") {
                            getDropdownOptions(field.field_name, currentTagBcType)
                        } else emptyList()
                    )
                }.sortedBy { it.fieldOrder }.toMutableList()

                // Debug: Print all field names to identify the correct Unit field name
                println("StepFormViewModel: All fields loaded:")
                fieldData.forEach { field ->
                    println("  - Field: '${field.fieldName}' (${field.fieldType}), currentValue='${field.currentValue}', dropdownOptions=${field.dropdownOptions.size}")
                }

                // Populate cascading dropdowns BEFORE emitting UI state to ensure options are available
                if (tagData != null) {
                    val blockValue = tagData.Region
                    val floorValue = tagData.Floor
                    val roomId = tagData.RoomId
                    println("StepFormViewModel: Auto-fill cascading - Block: '$blockValue', Floor: '$floorValue', RoomId: '$roomId'")
                    
                    if (!blockValue.isNullOrEmpty()) {
                        // Update Floor dropdown options
                        val floorIndex = fieldData.indexOfFirst { it.fieldName == "Floor" }
                        if (floorIndex != -1) {
                            val floorOptions = getFloorOptions(blockValue)
                            fieldData[floorIndex] = fieldData[floorIndex].copy(
                                dropdownOptions = floorOptions
                            )
                            println("StepFormViewModel: Auto-fill - Populated Floor dropdown with ${floorOptions.size} options")
                        } else {
                            println("StepFormViewModel: WARNING - Floor field not found in fieldData")
                        }
                        
                        // Update Unit dropdown options if Floor is available
                        // Try multiple possible field name variations
                        if (!floorValue.isNullOrEmpty()) {
                            val unitIndex = fieldData.indexOfFirst { 
                                it.fieldName.equals("Unit", ignoreCase = true) || 
                                it.fieldName.equals("RoomId", ignoreCase = true) ||
                                it.fieldName.equals("Room", ignoreCase = true)
                            }
                            
                            if (unitIndex != -1) {
                                val unitOptions = getUnitOptions(blockValue, floorValue)
                                val currentField = fieldData[unitIndex]
                                
                                // Check if current RoomId exists in the options
                                val currentRoomIdExists = unitOptions.any { it.value == roomId }
                                
                                if (!currentRoomIdExists && !roomId.isNullOrEmpty()) {
                                    println("StepFormViewModel: WARNING - RoomId '$roomId' not found in options for Block='$blockValue', Floor='$floorValue'")
                                    println("StepFormViewModel: This suggests data inconsistency - looking up correct floor for this RoomId")
                                    
                                    // Try to find the correct location by RoomId
                                    val correctLocation = try {
                                        withContext(Dispatchers.IO) {
                                            databaseManager.database.masterLocationsQueries
                                                .selectLocationById(roomId)
                                                .executeAsOneOrNull()
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                    
                                    if (correctLocation != null) {
                                        println("StepFormViewModel: Found location - Room='${correctLocation.room}', Floor='${correctLocation.floor}', Region='${correctLocation.region}'")
                                        println("StepFormViewModel: Data inconsistency detected! Tag Floor='$floorValue' but RoomId belongs to Floor='${correctLocation.floor}'")
                                        
                                        // Load options for the CORRECT floor
                                        val correctFloorValue = correctLocation.floor ?: floorValue
                                        val correctUnitOptions = getUnitOptions(blockValue, correctFloorValue)
                                        
                                        fieldData[unitIndex] = currentField.copy(
                                            dropdownOptions = correctUnitOptions
                                        )
                                        
                                        println("StepFormViewModel: Auto-fill - Loaded Unit options for CORRECT floor '$correctFloorValue' with ${correctUnitOptions.size} options")
                                    } else {
                                        // Fallback: use original options even though RoomId doesn't match
                                        fieldData[unitIndex] = currentField.copy(
                                            dropdownOptions = unitOptions
                                        )
                                        println("StepFormViewModel: Auto-fill - Using original floor options (data inconsistency may cause display issues)")
                                    }
                                } else {
                                    fieldData[unitIndex] = currentField.copy(
                                        dropdownOptions = unitOptions
                                    )
                                    println("StepFormViewModel: Auto-fill - Found Unit field '${currentField.fieldName}' at index $unitIndex")
                                    println("StepFormViewModel: Auto-fill - Populated Unit dropdown with ${unitOptions.size} options")
                                }
                                
                                println("StepFormViewModel: Auto-fill - Unit options: ${unitOptions.take(5).map { "${it.label}=${it.value}" }}... (showing first 5)")
                            } else {
                                println("StepFormViewModel: WARNING - Unit field not found in fieldData")
                                println("StepFormViewModel: Available fields: ${fieldData.map { it.fieldName }}")
                            }
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    stepCode = stepCode,
                    stepFields = fieldData,
                    isLoading = false,
                    error = null
                )

                println("StepFormViewModel: Step form loaded successfully with ${fieldData.size} fields")

            } catch (e: Exception) {
                println("StepFormViewModel: Error loading step fields: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load step fields: ${e.message}"
                )
            }
        }
    }

    /**
     * Update field value
     */
    fun updateFieldValue(fieldName: String, value: String): Unit {
        val currentFields = _uiState.value.stepFields.toMutableList()
        val fieldIndex = currentFields.indexOfFirst { it.fieldName == fieldName }
        
        println("StepFormViewModel: updateFieldValue() called - fieldName='$fieldName', value='$value'")
        println("StepFormViewModel: Current fields count: ${currentFields.size}")
        println("StepFormViewModel: Field index found: $fieldIndex")
        
        if (fieldIndex != -1) {
            val oldValue = currentFields[fieldIndex].currentValue
            currentFields[fieldIndex] = currentFields[fieldIndex].copy(currentValue = value)
            
            _uiState.value = _uiState.value.copy(stepFields = currentFields)
            println("StepFormViewModel: ✓ Updated field '$fieldName' from '$oldValue' to '$value'")
            
            // Handle cascading dropdowns for Block/Floor/Unit asynchronously
            when (fieldName) {
                "Block" -> {
                    updateCascadingDropdowns(value, null)
                }
                "Floor" -> {
                    val blockField = currentFields.find { it.fieldName == "Block" }
                    if (blockField != null) {
                        updateCascadingDropdowns(blockField.currentValue, value)
                    }
                }
            }
        } else {
            println("StepFormViewModel: ⚠ Field '$fieldName' not found in current fields!")
            println("StepFormViewModel: Available fields: ${currentFields.map { it.fieldName }}")
        }
    }

    /**
     * Save step form data to RfidModule database
     */
    fun saveStepForm(): Unit {
        viewModelScope.launch {
            try {
                val stepCode = _uiState.value.stepCode
                val stepFields = _uiState.value.stepFields
                val rfidModule = currentRfidModule
                
                println("StepFormViewModel: Saving step form for: $stepCode")
                println("StepFormViewModel: Current field values:")
                stepFields.forEach { field ->
                    println("  - ${field.fieldName}: '${field.currentValue}'")
                }
                
                if (rfidModule == null) {
                    throw Exception("No RfidModule data available to save")
                }
                
                _uiState.value = _uiState.value.copy(isSaving = true)
                
                // Map form field values back to RfidModule fields
                val updatedModule = updateRfidModuleFromFields(rfidModule, stepCode, stepFields)
                
                println("StepFormViewModel: Updated module Remark10='${updatedModule.Remark10}', Remark20='${updatedModule.Remark20}'")

                
                // Save to database
                withContext(Dispatchers.IO) {
                    databaseManager.database.rfidModuleQueries.updateModule(
                        ProjId = updatedModule.ProjId,
                        ContractNo = updatedModule.ContractNo,
                        ManufacturerId = updatedModule.ManufacturerId,
                        TagId = updatedModule.TagId,
                        IsActivated = updatedModule.IsActivated,
                        ActivatedDate = updatedModule.ActivatedDate,
                        BCType = updatedModule.BCType,
                        RFIDTagNo = updatedModule.RFIDTagNo,
                        StepCode = updatedModule.StepCode,
                        Category = updatedModule.Category,
                        Subcategory = updatedModule.Subcategory,
                        SupplierId = updatedModule.SupplierId,
                        ConcreteGrade = updatedModule.ConcreteGrade,
                        ASN = updatedModule.ASN,
                        SerialNo = updatedModule.SerialNo,
                        WorkingNo = updatedModule.WorkingNo,
                        ManufacturingDate = updatedModule.ManufacturingDate,
                        RSCompanyId = updatedModule.RSCompanyId,
                        RSInspectionDate = updatedModule.RSInspectionDate,
                        CastingDate = updatedModule.CastingDate,
                        FirstCastingDate = updatedModule.FirstCastingDate,
                        SecondCastingDate = updatedModule.SecondCastingDate,
                        WaterproofingInstallationDate = updatedModule.WaterproofingInstallationDate,
                        InternalFinishDate = updatedModule.InternalFinishDate,
                        DeliveryDate = updatedModule.DeliveryDate,
                        BatchNo = updatedModule.BatchNo,
                        LicensePlateNo = updatedModule.LicensePlateNo,
                        GpsDeviceId = updatedModule.GpsDeviceId,
                        SiteArrivalDate = updatedModule.SiteArrivalDate,
                        SiteInstallationDate = updatedModule.SiteInstallationDate,
                        RoomInput = updatedModule.RoomInput,
                        RoomId = updatedModule.RoomId,
                        Floor = updatedModule.Floor,
                        Region = updatedModule.Region,
                        ChipFailureSA = updatedModule.ChipFailureSA,
                        ChipFailureSI = updatedModule.ChipFailureSI,
                        IsCompleted10 = updatedModule.IsCompleted10,
                        Remark10 = updatedModule.Remark10,
                        IsCompleted20 = updatedModule.IsCompleted20,
                        Remark20 = updatedModule.Remark20,
                        IsCompleted30 = updatedModule.IsCompleted30,
                        Remark30 = updatedModule.Remark30,
                        IsCompleted40 = updatedModule.IsCompleted40,
                        Remark40 = updatedModule.Remark40,
                        IsCompleted50 = updatedModule.IsCompleted50,
                        Remark50 = updatedModule.Remark50,
                        IsCompleted55 = updatedModule.IsCompleted55,
                        Remark55 = updatedModule.Remark55,
                        IsCompleted60 = updatedModule.IsCompleted60,
                        Remark60 = updatedModule.Remark60,
                        IsCompleted70 = updatedModule.IsCompleted70,
                        Remark70 = updatedModule.Remark70,
                        IsCompleted80 = updatedModule.IsCompleted80,
                        Remark80 = updatedModule.Remark80,
                        Dispose = updatedModule.Dispose,
                        UpdatedBy = updatedModule.UpdatedBy,
                        ProductNo = updatedModule.ProductNo,
                        Id = updatedModule.Id // WHERE clause parameter comes last
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isFormSaved = true
                )
                
                println("StepFormViewModel: Step form saved successfully")
                stepFields.forEach { field ->
                    println("StepFormViewModel: Saved ${field.fieldName} = ${field.currentValue}")
                }
                
            } catch (e: Exception) {
                println("StepFormViewModel: Error saving step form: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save step form: ${e.message}"
                )
            }
        }
    }

    /**
     * Map form field values back to RfidModule fields
     */
    private fun updateRfidModuleFromFields(
        originalModule: com.socam.bcms.database.RfidModule,
        stepCode: String,
        stepFields: List<StepFieldData>
    ): com.socam.bcms.database.RfidModule {
        
        // Create a mutable copy of the original module
        var updatedModule = originalModule.copy()
        val stepPortion = currentStepPortion ?: throw Exception("Step portion not loaded for step: $stepCode")
        
        println("StepFormViewModel: Updating module fields using portion: $stepPortion")
        
        // Update fields based on form input
        stepFields.forEach { field ->
            when (field.fieldName) {
                // General RfidModule fields
                "Category" -> updatedModule = updatedModule.copy(Category = field.currentValue)
                "Subcategory" -> updatedModule = updatedModule.copy(Subcategory = field.currentValue)
                "Serial No." -> updatedModule = updatedModule.copy(SerialNo = field.currentValue)
                "Edit Serial No." -> updatedModule = updatedModule.copy(SerialNo = field.currentValue)
                "Hinge Supplier" -> updatedModule = updatedModule.copy(SupplierId = field.currentValue)
                "Manufacturing Date" -> {
                    val timestamp = parseDateToTimestamp(field.currentValue)
                    println("StepFormViewModel: DEBUG - Saving Manufacturing Date: '${field.currentValue}' -> timestamp: $timestamp")
                    updatedModule = updatedModule.copy(ManufacturingDate = timestamp)
                }
                "Concrete Grade" -> updatedModule = updatedModule.copy(ConcreteGrade = field.currentValue)
                "Product No." -> updatedModule = updatedModule.copy(ProductNo = field.currentValue)
                "RS Company" -> updatedModule = updatedModule.copy(RSCompanyId = field.currentValue)
                "RS Inspection Date" -> updatedModule = updatedModule.copy(RSInspectionDate = parseDateToTimestamp(field.currentValue))
                "Casting Date" -> updatedModule = updatedModule.copy(CastingDate = parseDateToTimestamp(field.currentValue))
                "Casting Date 2" -> updatedModule = updatedModule.copy(SecondCastingDate = parseDateToTimestamp(field.currentValue))
                "Internal Finishes Date" -> updatedModule = updatedModule.copy(InternalFinishDate = parseDateToTimestamp(field.currentValue))
                "Delivery Date" -> updatedModule = updatedModule.copy(DeliveryDate = parseDateToTimestamp(field.currentValue))
                "Batch No." -> updatedModule = updatedModule.copy(BatchNo = field.currentValue)
                "T Plate No." -> updatedModule = updatedModule.copy(BatchNo = field.currentValue) // T Plate No. updates BatchNo
                "License Plate No." -> {
                    // Read-only field - do not update
                    println("StepFormViewModel: License Plate No. is read-only, skipping update")
                }
                "Site Arrival Date" -> updatedModule = updatedModule.copy(SiteArrivalDate = parseDateToTimestamp(field.currentValue))
                "Installation Date" -> updatedModule = updatedModule.copy(SiteInstallationDate = parseDateToTimestamp(field.currentValue))
                "Block" -> updatedModule = updatedModule.copy(Region = field.currentValue)
                "Floor" -> updatedModule = updatedModule.copy(Floor = field.currentValue)
                "Unit" -> updatedModule = updatedModule.copy(RoomId = field.currentValue)
                "Chip Failure (SA)" -> updatedModule = updatedModule.copy(ChipFailureSA = if (field.currentValue.toBoolean()) 1 else 0)
                "Chip Failure (SI)" -> updatedModule = updatedModule.copy(ChipFailureSI = if (field.currentValue.toBoolean()) 1 else 0)
                
                // Step-specific fields
                "Remark" -> {
                    println("StepFormViewModel: Updating Remark field - stepPortion='$stepPortion', value='${field.currentValue}'")
                    updatedModule = updateRemarkField(updatedModule, stepPortion, field.currentValue)
                }
                "Is Completed" -> {
                    val isCompleted = if (field.currentValue.toBoolean()) 1L else 0L
                    println("StepFormViewModel: Updating IsCompleted field - stepPortion='$stepPortion', value=$isCompleted")
                    updatedModule = updateIsCompletedField(updatedModule, stepPortion, isCompleted)
                }
                
                else -> {
                    println("StepFormViewModel: Unknown field: ${field.fieldName}")
                }
            }
        }
        
        return updatedModule
    }
    
    /**
     * Update remark field based on step portion from MasterWorkflowSteps
     */
    private fun updateRemarkField(module: com.socam.bcms.database.RfidModule, stepPortion: Int, value: String): com.socam.bcms.database.RfidModule {
        return when (stepPortion) {
            10 -> module.copy(Remark10 = value)
            20 -> module.copy(Remark20 = value)
            30 -> module.copy(Remark30 = value)
            40 -> module.copy(Remark40 = value)
            50 -> module.copy(Remark50 = value)
            55 -> module.copy(Remark55 = value)
            60 -> module.copy(Remark60 = value)
            70 -> module.copy(Remark70 = value)
            80 -> module.copy(Remark80 = value)
            else -> throw Exception("Invalid step portion: $stepPortion. No matching Remark field found.")
        }
    }
    
    /**
     * Update IsCompleted field based on step portion from MasterWorkflowSteps
     */
    private fun updateIsCompletedField(module: com.socam.bcms.database.RfidModule, stepPortion: Int, value: Long): com.socam.bcms.database.RfidModule {
        return when (stepPortion) {
            10 -> module.copy(IsCompleted10 = value)
            20 -> module.copy(IsCompleted20 = value)
            30 -> module.copy(IsCompleted30 = value)
            40 -> module.copy(IsCompleted40 = value)
            50 -> module.copy(IsCompleted50 = value)
            55 -> module.copy(IsCompleted55 = value)
            60 -> module.copy(IsCompleted60 = value)
            70 -> module.copy(IsCompleted70 = value)
            80 -> module.copy(IsCompleted80 = value)
            else -> throw Exception("Invalid step portion: $stepPortion. No matching IsCompleted field found.")
        }
    }
    
    /**
     * Parse date string to timestamp
     */
    private fun parseDateToTimestamp(dateString: String): Long? {
        if (dateString.isEmpty()) return null
        return try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = dateFormat.parse(dateString)
            date?.time?.div(1000) // Convert to seconds
        } catch (e: Exception) {
            println("StepFormViewModel: Error parsing date $dateString: ${e.message}")
            null
        }
    }

    /**
     * Reset form state
     */
    fun resetForm(): Unit {
        _uiState.value = StepFormUiState()
    }

    /**
     * Get dropdown options based on field name and tag BC type
     */
    private suspend fun getDropdownOptions(fieldName: String, bcType: String): List<DropdownOption> {
        return withContext(Dispatchers.IO) {
            try {
                when (fieldName) {
                    "Category" -> {
                        databaseManager.database.masterCategoriesQueries
                            .selectCategoriesByTypeAndFlag(bcType, 0)
                            .executeAsList()
                            .map { DropdownOption(it.category, it.category) }
                    }
                    "Subcategory" -> {
                        databaseManager.database.masterCategoriesQueries
                            .selectCategoriesByTypeAndFlag(bcType, 1)
                            .executeAsList()
                            .map { DropdownOption(it.category, it.category) }
                    }
                    "Hinge Supplier" -> {
                        databaseManager.database.masterCompaniesQueries
                            .selectCompaniesByTypeAndBcTypeWithWildcard("HingeSupplier", bcType)
                            .executeAsList()
                            .map { DropdownOption(it.name_en ?: it.id, it.id) }
                    }
                    "RS Company" -> {
                        databaseManager.database.masterCompaniesQueries
                            .selectCompaniesByTypeAndBcTypeWithWildcard("RSCompany", bcType)
                            .executeAsList()
                            .map { DropdownOption(it.name_en ?: it.id, it.id) }
                    }
                    "Concrete Grade" -> {
                        databaseManager.database.masterConcreteGradesQueries
                            .selectAllConcreteGrades()
                            .executeAsList()
                            .map { DropdownOption(it.grade, it.grade) }
                    }
                    "Block" -> {
                        // Show distinct regions from MasterLocations
                        try {
                            val locations = databaseManager.database.masterLocationsQueries
                                .selectAllLocations()
                                .executeAsList()
                            
                            println("StepFormViewModel: Block dropdown - Found ${locations.size} total locations")
                            val blockOptions = locations.map { location ->
                                DropdownOption(label = location.region ?: "", value = location.region ?: "")
                            }.distinctBy { it.value }.filter { it.value.isNotEmpty() }
                            println("StepFormViewModel: Block dropdown - Created ${blockOptions.size} block options: ${blockOptions.map { it.label }}")
                            blockOptions
                        } catch (e: Exception) {
                            println("StepFormViewModel: Error loading blocks: ${e.message}")
                            emptyList()
                        }
                    }
                    "Floor" -> {
                        // Initially empty, will be populated when Block is selected or auto-filled
                        println("StepFormViewModel: Floor dropdown - Initially empty (will be filtered by Block selection)")
                        emptyList()
                    }
                    "Unit", "RoomId", "Room", "room_id", "unit" -> {
                        // Initially empty, will be populated when Block and Floor are selected or auto-filled
                        println("StepFormViewModel: Unit dropdown (field='$fieldName') - Initially empty (will be filtered by Block + Floor selection)")
                        emptyList()
                    }
                    else -> {
                        println("StepFormViewModel: No dropdown options handler for field: '$fieldName'")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                println("StepFormViewModel: Error getting dropdown options for $fieldName: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Get distinct block options from MasterLocations (not used anymore, kept for reference)
     */
    private suspend fun getBlockOptions(): List<DropdownOption> {
        return withContext(Dispatchers.IO) {
            try {
                // Now using MasterLocations instead of MasterRegions
                val locations = databaseManager.database.masterLocationsQueries
                    .selectAllLocations()
                    .executeAsList()
                
                locations.map { location ->
                    DropdownOption(label = location.region ?: "", value = location.region ?: "")
                }.distinctBy { it.value }.filter { it.value.isNotEmpty() }
            } catch (e: Exception) {
                println("StepFormViewModel: Error getting block options: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Get floor options filtered by selected block from MasterLocations
     */
    private suspend fun getFloorOptions(selectedBlock: String): List<DropdownOption> {
        return withContext(Dispatchers.IO) {
            try {
                val locations = databaseManager.database.masterLocationsQueries
                    .selectLocationsByRegion(selectedBlock)
                    .executeAsList()
                
                println("StepFormViewModel: getFloorOptions - Found ${locations.size} locations for block '$selectedBlock'")
                val floorOptions = locations.mapNotNull { it.floor }.distinct().map { floor ->
                    DropdownOption(label = floor, value = floor)
                }
                println("StepFormViewModel: getFloorOptions - Created ${floorOptions.size} floor options: ${floorOptions.map { it.label }}")
                floorOptions
            } catch (e: Exception) {
                println("StepFormViewModel: Error getting floor options: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Get unit options filtered by selected block and floor from MasterLocations
     */
    private suspend fun getUnitOptions(selectedBlock: String, selectedFloor: String): List<DropdownOption> {
        return withContext(Dispatchers.IO) {
            try {
                val locations = databaseManager.database.masterLocationsQueries
                    .selectLocationsByRegionAndFloor(selectedBlock, selectedFloor)
                    .executeAsList()
                
                println("StepFormViewModel: getUnitOptions - Found ${locations.size} locations for block '$selectedBlock', floor '$selectedFloor'")
                val unitOptions = locations.map { location ->
                    DropdownOption(label = location.room ?: location.room_id, value = location.room_id)
                }
                println("StepFormViewModel: getUnitOptions - Created ${unitOptions.size} unit options: ${unitOptions.map { "${it.label} (${it.value})" }}")
                unitOptions
            } catch (e: Exception) {
                println("StepFormViewModel: Error getting unit options: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Update cascading dropdowns for Block/Floor/Unit
     */
    private fun updateCascadingDropdowns(selectedBlock: String, selectedFloor: String?) {
        viewModelScope.launch {
            try {
                val currentFields = _uiState.value.stepFields.toMutableList()
                
                if (selectedFloor == null) {
                    // Block changed - update Floor and clear Unit
                    val floorIndex = currentFields.indexOfFirst { it.fieldName == "Floor" }
                    if (floorIndex != -1) {
                        val floorOptions = getFloorOptions(selectedBlock)
                        currentFields[floorIndex] = currentFields[floorIndex].copy(
                            currentValue = "",
                            dropdownOptions = floorOptions
                        )
                    }
                    
                    // Clear Unit options
                    val unitIndex = currentFields.indexOfFirst { it.fieldName == "Unit" }
                    if (unitIndex != -1) {
                        currentFields[unitIndex] = currentFields[unitIndex].copy(
                            currentValue = "",
                            dropdownOptions = emptyList()
                        )
                    }
                } else {
                    // Floor changed - update Unit
                    val unitIndex = currentFields.indexOfFirst { it.fieldName == "Unit" }
                    if (unitIndex != -1) {
                        val unitOptions = getUnitOptions(selectedBlock, selectedFloor)
                        currentFields[unitIndex] = currentFields[unitIndex].copy(
                            currentValue = "",
                            dropdownOptions = unitOptions
                        )
                    }
                }
                
                _uiState.value = _uiState.value.copy(stepFields = currentFields)
                println("StepFormViewModel: Updated cascading dropdowns")
                
            } catch (e: Exception) {
                println("StepFormViewModel: Error updating cascading dropdowns: ${e.message}")
            }
        }
    }

    /**
     * Update cascading dropdowns with auto-fill data (preserves existing values)
     */
    private suspend fun updateCascadingDropdownsWithAutoFill(blockValue: String, floorValue: String?) {
        val currentFields = _uiState.value.stepFields.toMutableList()
        
        try {
            // Update Floor dropdown with filtered options (preserve auto-filled value)
            val floorIndex = currentFields.indexOfFirst { it.fieldName == "Floor" }
            if (floorIndex != -1) {
                val floorOptions = getFloorOptions(blockValue)
                currentFields[floorIndex] = currentFields[floorIndex].copy(
                    dropdownOptions = floorOptions
                    // Keep existing currentValue from auto-fill
                )
                println("StepFormViewModel: Auto-fill cascading - Updated Floor dropdown with ${floorOptions.size} options")
            }
            
            // Update Unit dropdown if Floor value is also available
            if (!floorValue.isNullOrEmpty()) {
                val unitIndex = currentFields.indexOfFirst { it.fieldName == "Unit" }
                if (unitIndex != -1) {
                    val unitOptions = getUnitOptions(blockValue, floorValue)
                    currentFields[unitIndex] = currentFields[unitIndex].copy(
                        dropdownOptions = unitOptions
                        // Keep existing currentValue from auto-fill
                    )
                    println("StepFormViewModel: Auto-fill cascading - Updated Unit dropdown with ${unitOptions.size} options")
                }
            }
            
            _uiState.value = _uiState.value.copy(stepFields = currentFields)
        } catch (e: Exception) {
            println("StepFormViewModel: Error updating cascading dropdowns with auto-fill: ${e.message}")
        }
    }

    /**
     * Get auto-filled value for a field based on RfidModule data
     */
    private fun getAutoFilledValue(fieldName: String, stepPortion: Int, tagData: com.socam.bcms.database.RfidModule): String {
        return try {
            when (fieldName) {
                // Step-specific fields (based on step portion from MasterWorkflowSteps)
                "Remark" -> {
                    getRemarkField(tagData, stepPortion) ?: ""
                }
                "Is Completed" -> {
                    val isCompleted = getIsCompletedField(tagData, stepPortion) ?: 0
                    (isCompleted == 1L).toString()
                }
                
                // Common fields by name matching
                "Category" -> tagData.Category ?: ""
                "Subcategory" -> tagData.Subcategory ?: ""
                "Serial No." -> tagData.SerialNo ?: ""
                "Edit Serial No." -> tagData.SerialNo ?: "" // Use same as Serial No.
                "Hinge Supplier" -> tagData.SupplierId ?: ""
                "Manufacturing Date" -> formatDateFromTimestamp(tagData.ManufacturingDate)
                "Concrete Grade" -> tagData.ConcreteGrade ?: ""
                "Product No." -> tagData.ProductNo ?: ""
                "RS Company" -> tagData.RSCompanyId ?: ""
                "RS Inspection Date" -> formatDateFromTimestamp(tagData.RSInspectionDate)
                "Casting Date" -> formatDateFromTimestamp(tagData.CastingDate)
                "Casting Date 2" -> formatDateFromTimestamp(tagData.SecondCastingDate)
                "Internal Finishes Date" -> formatDateFromTimestamp(tagData.InternalFinishDate)
                "Delivery Date" -> formatDateFromTimestamp(tagData.DeliveryDate)
                "Batch No." -> tagData.BatchNo ?: ""
                "License Plate No." -> tagData.LicensePlateNo ?: "" // Read-only field (auto-populated, not editable)
                "T Plate No." -> tagData.BatchNo ?: "" // T Plate No. maps to BatchNo
                "Site Arrival Date" -> formatDateFromTimestamp(tagData.SiteArrivalDate)
                "Installation Date" -> formatDateFromTimestamp(tagData.SiteInstallationDate)
                "Block" -> tagData.Region ?: ""
                "Floor" -> tagData.Floor ?: ""
                "Unit", "RoomId", "Room" -> {
                    val value = tagData.RoomId ?: ""
                    println("StepFormViewModel: Auto-filling Unit field '$fieldName' with RoomId: '$value'")
                    value
                }
                "Chip Failure (SA)" -> (tagData.ChipFailureSA == 1L).toString()
                "Chip Failure (SI)" -> (tagData.ChipFailureSI == 1L).toString()
                
                else -> {
                    println("StepFormViewModel: No auto-fill value for field: '$fieldName'")
                    ""
                }
            }
        } catch (e: Exception) {
            println("StepFormViewModel: Error getting auto-filled value for $fieldName: ${e.message}")
            ""
        }
    }

    /**
     * Get step portion from MasterWorkflowSteps table
     */
    private suspend fun getStepPortion(stepCode: String, bcType: String): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val workflowStep = databaseManager.database.masterWorkflowStepsQueries
                    .selectWorkflowStepByKey(stepCode, bcType)
                    .executeAsOneOrNull()
                
                workflowStep?.portion?.toInt()
            } catch (e: Exception) {
                println("StepFormViewModel: Error getting step portion: ${e.message}")
                null
            }
        }
    }

    /**
     * Get remark field based on step portion from MasterWorkflowSteps
     */
    private fun getRemarkField(tagData: com.socam.bcms.database.RfidModule, stepPortion: Int): String? {
        return when (stepPortion) {
            10 -> tagData.Remark10
            20 -> tagData.Remark20
            30 -> tagData.Remark30
            40 -> tagData.Remark40
            50 -> tagData.Remark50
            55 -> tagData.Remark55
            60 -> tagData.Remark60
            70 -> tagData.Remark70
            80 -> tagData.Remark80
            else -> null
        }
    }

    /**
     * Get IsCompleted field based on step portion from MasterWorkflowSteps
     */
    private fun getIsCompletedField(tagData: com.socam.bcms.database.RfidModule, stepPortion: Int): Long? {
        return when (stepPortion) {
            10 -> tagData.IsCompleted10
            20 -> tagData.IsCompleted20
            30 -> tagData.IsCompleted30
            40 -> tagData.IsCompleted40
            50 -> tagData.IsCompleted50
            55 -> tagData.IsCompleted55
            60 -> tagData.IsCompleted60
            70 -> tagData.IsCompleted70
            80 -> tagData.IsCompleted80
            else -> null
        }
    }

    /**
     * Format timestamp to date string
     */
    private fun formatDateFromTimestamp(timestamp: Long?): String {
        return if (timestamp != null && timestamp > 0) {
            val date = java.util.Date(timestamp * 1000) // Assuming timestamp is in seconds
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            dateFormat.format(date)
        } else {
            ""
        }
    }
}

/**
 * UI state for step form
 */
data class StepFormUiState(
    val stepCode: String = "",
    val stepFields: List<StepFieldData> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormSaved: Boolean = false,
    val error: String? = null
)

/**
 * Data class for step field information
 */
data class StepFieldData(
    val fieldName: String,
    val fieldLabel: String,
    val fieldType: String, // text, dropdown, date, datetime, checkbox, integer
    val fieldOrder: Int,
    val isRequired: Boolean,
    val defaultValue: String?,
    val currentValue: String,
    val dropdownOptions: List<DropdownOption> = emptyList()
)

/**
 * Data class for dropdown options
 */
data class DropdownOption(
    val label: String,  // Display text
    val value: String   // Stored value
)
