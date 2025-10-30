package com.socam.bcms.presentation.modules

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.BuildConfig
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * ViewModel for batch step form editing across multiple tags
 */
class BatchStepFormViewModel(
    private val context: Context,
    private val databaseManager: DatabaseManager,
    private val authManager: AuthManager
) : ViewModel() {

    companion object {
        private const val TAG = "BatchStepFormViewModel"
    }

    private val _uiState = MutableStateFlow(BatchStepFormUiState())
    val uiState: StateFlow<BatchStepFormUiState> = _uiState.asStateFlow()

    private lateinit var currentStepCode: String
    private lateinit var currentBcType: String
    private lateinit var currentTagEpcs: List<String>
    private var currentStepPortion: Int? = null
    private val fieldValues = mutableMapOf<String, String>()

    /**
     * Load step fields for batch editing
     */
    fun loadStepFields(stepCode: String, bcType: String, tagEpcs: List<String>) {
        currentStepCode = stepCode
        currentBcType = bcType
        currentTagEpcs = tagEpcs
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                Log.d(TAG, "Loading step fields for: $stepCode, BC type: $bcType, tags: ${tagEpcs.size}")
                
                // Get portion from MasterWorkflowSteps
                val portion = getStepPortion(stepCode, bcType)
                if (portion == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Step $stepCode not found in workflow for BC type $bcType"
                    )
                    return@launch
                }
                currentStepPortion = portion
                Log.d(TAG, "Step portion from MasterWorkflowSteps: $portion")
                
                val stepFields = loadStepFieldsFromDatabase(stepCode, bcType)
                
                if (stepFields.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No fields found for step $stepCode"
                    )
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stepFields = stepFields
                )
                
                Log.d(TAG, "Loaded ${stepFields.size} step fields successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading step fields: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load step fields: ${e.message}"
                )
            }
        }
    }

    /**
     * Load step fields from database
     */
    private suspend fun loadStepFieldsFromDatabase(stepCode: String, bcType: String): List<BatchStepFieldData> = withContext(Dispatchers.IO) {
        try {
            // Get workflow step fields filtered by project
            val stepFields = databaseManager.database.workflowStepFieldsQueries
                .selectFieldsByStepAndProject(stepCode, BuildConfig.PROJECT_ID)
                .executeAsList()
            
            Log.d(TAG, "Found ${stepFields.size} fields in database for step: $stepCode")
            
            val result = mutableListOf<BatchStepFieldData>()
            
            for (field in stepFields) {
                // Skip unique fields that cannot be batch edited (Serial No., Edit Serial No., License Plate No.)
                val isUniqueField = field.field_name == "Serial No." || 
                                    field.field_name == "Edit Serial No." || 
                                    field.field_name == "License Plate No."
                
                if (isUniqueField) {
                    Log.d(TAG, "Skipping unique field '${field.field_name}' in batch mode (cannot be batch edited)")
                    continue
                }
                
                val batchField = BatchStepFieldData(
                    fieldName = field.field_name,
                    fieldType = field.field_type ?: "text",
                    fieldLabel = field.field_label ?: field.field_name,
                    fieldOrder = field.field_order.toInt(),
                    isRequired = field.is_required == 1L,
                    defaultValue = field.default_value ?: "",
                    dropdownOptions = loadDropdownOptions(field.field_name, bcType)
                )
                
                result.add(batchField)
                Log.d(TAG, "Added batch field: ${batchField.fieldName} (${batchField.fieldType})")
            }
            
            // Sort by field order
            result.sortedBy { it.fieldOrder }
            
        } catch (e: Exception) {
            Log.e(TAG, "Database error loading step fields: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Load dropdown options for a field
     */
    private suspend fun loadDropdownOptions(fieldName: String, bcType: String): List<DropdownOption> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading dropdown options for field: '$fieldName' (bcType: $bcType)")
            when (fieldName.lowercase()) {
                "category", "subcategory" -> {
                    try {
                        val categories = databaseManager.database.masterCategoriesQueries
                            .selectCategoriesByBcType(bcType)
                            .executeAsList()
                        
                        categories.map { category ->
                            DropdownOption(
                                value = category.category,
                                label = category.desc_en ?: category.category
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading categories: ${e.message}")
                        emptyList()
                    }
                }
                "supplierid", "manufacturerid" -> {
                    try {
                        val companies = databaseManager.database.masterCompaniesQueries
                            .selectCompaniesByTypeAndBcTypeWithWildcard("HingeSupplier", bcType)
                            .executeAsList()
                        
                        companies.map { company ->
                            DropdownOption(
                                value = company.id,
                                label = company.name_en ?: company.id
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading companies: ${e.message}")
                        emptyList()
                    }
                }
                "region", "block" -> {
                    try {
                        val locations = databaseManager.database.masterLocationsQueries
                            .selectAllLocations()
                            .executeAsList()
                        
                        Log.d(TAG, "Block dropdown: Found ${locations.size} total locations")
                        val blockOptions = locations.map { location ->
                            DropdownOption(value = location.region ?: "", label = location.region ?: "")
                        }.distinctBy { it.value }.filter { it.value.isNotEmpty() }
                        Log.d(TAG, "Block dropdown: Created ${blockOptions.size} block options: ${blockOptions.map { it.label }}")
                        blockOptions
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading regions: ${e.message}")
                        emptyList()
                    }
                }
                "floor" -> {
                    try {
                        val locations = databaseManager.database.masterLocationsQueries
                            .selectAllLocations()
                            .executeAsList()
                        
                        Log.d(TAG, "Floor dropdown: Found ${locations.size} total locations")
                        val floorOptions = locations.mapNotNull { it.floor }.distinct().map { floor ->
                            DropdownOption(value = floor, label = floor)
                        }
                        Log.d(TAG, "Floor dropdown: Created ${floorOptions.size} floor options: ${floorOptions.map { it.label }}")
                        floorOptions
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading floors: ${e.message}")
                        emptyList()
                    }
                }
                "room_id", "unit" -> {
                    try {
                        val locations = databaseManager.database.masterLocationsQueries
                            .selectAllLocations()
                            .executeAsList()
                        
                        Log.d(TAG, "Unit dropdown: Found ${locations.size} total locations")
                        val unitOptions = locations.map { location ->
                            DropdownOption(
                                label = location.room ?: location.room_id,
                                value = location.room_id
                            )
                        }.distinctBy { it.value }
                        Log.d(TAG, "Unit dropdown: Created ${unitOptions.size} unit options: ${unitOptions.map { "${it.label} (${it.value})" }}")
                        unitOptions
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading rooms: ${e.message}")
                        emptyList()
                    }
                }
                "concrete grade" -> {
                    try {
                        val grades = databaseManager.database.masterConcreteGradesQueries
                            .selectAllConcreteGrades()
                            .executeAsList()
                        
                        Log.d(TAG, "Concrete Grade dropdown: Found ${grades.size} grades")
                        val gradeOptions = grades.map { grade ->
                            DropdownOption(
                                label = grade.grade,
                                value = grade.grade
                            )
                        }
                        Log.d(TAG, "Concrete Grade dropdown: Created ${gradeOptions.size} grade options: ${gradeOptions.map { it.label }}")
                        gradeOptions
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading concrete grades: ${e.message}")
                        emptyList()
                    }
                }
                "rs company", "rscompanyid", "rs_company_id" -> {
                    try {
                        val companies = databaseManager.database.masterCompaniesQueries
                            .selectCompaniesByTypeAndBcTypeWithWildcard("RSCompany", bcType)
                            .executeAsList()
                        
                        Log.d(TAG, "RS Company dropdown: Found ${companies.size} companies")
                        val companyOptions = companies.map { company ->
                            DropdownOption(
                                value = company.id,
                                label = company.name_en ?: company.id
                            )
                        }
                        Log.d(TAG, "RS Company dropdown: Created ${companyOptions.size} company options: ${companyOptions.map { it.label }}")
                        companyOptions
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading RS companies: ${e.message}")
                        emptyList()
                    }
                }
                else -> {
                    Log.w(TAG, "No dropdown options handler for field: '$fieldName' (lowercase: '${fieldName.lowercase()}')")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dropdown options for $fieldName: ${e.message}")
            emptyList()
        }
    }

    /**
     * Update field value
     */
    fun updateFieldValue(fieldName: String, value: String) {
        fieldValues[fieldName] = value
        Log.d(TAG, "Field value updated: $fieldName = $value")
    }

    /**
     * Save batch step form - update all tags with enabled field values
     */
    fun saveBatchStepForm(enabledFieldNames: List<String>) {
        Log.d(TAG, "Starting batch save process for step: $currentStepCode")
        
        if (enabledFieldNames.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "No fields selected for editing. Please enable fields with the pen button before saving."
            )
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                
                val updatedCount = updateBatchTagsInDatabase(enabledFieldNames)
                
                if (updatedCount > 0) {
                    Log.d(TAG, "Successfully updated $updatedCount tags")
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isFormSaved = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "No tags were updated. Please check field values and try again."
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving batch step form: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save step form: ${e.message}"
                )
            }
        }
    }

    /**
     * Update all batch tags in database with enabled field values
     */
    private suspend fun updateBatchTagsInDatabase(enabledFieldNames: List<String>): Int = withContext(Dispatchers.IO) {
        try {
            var updatedCount = 0
            
            // Get step portion for generic field mapping
            val stepPortion = currentStepPortion ?: throw Exception("Step portion not loaded for step: $currentStepCode")
            
            Log.d(TAG, "Updating batch tags using portion: $stepPortion")
            
            for (epc in currentTagEpcs) {
                // Query RfidModule record by RFIDTagNo (now contains scanned EPC)
                val rfidRecords = databaseManager.database.rfidModuleQueries
                    .selectModulesByRFIDTagNo(epc)
                    .executeAsList()
                
                val rfidRecord = rfidRecords.firstOrNull()
                if (rfidRecord == null) {
                    Log.w(TAG, "No RfidModule record found for EPC: $epc")
                    continue
                }
                
                val rfidModuleId = rfidRecord.Id
                Log.d(TAG, "Updating tag $epc (ID: $rfidModuleId) with enabled fields: $enabledFieldNames")
                
                // Build update query based on enabled fields
                var hasUpdates = false
                
                for (fieldName in enabledFieldNames) {
                    val fieldValue = fieldValues[fieldName]
                    if (fieldValue != null) {
                        // Map generic field names to step-specific fields
                        val mappedFieldName = mapGenericFieldName(fieldName, stepPortion)
                        updateRfidModuleField(rfidModuleId, mappedFieldName, fieldValue)
                        hasUpdates = true
                        Log.d(TAG, "Updated field $fieldName (mapped to $mappedFieldName) = $fieldValue for tag $rfidModuleId")
                    }
                }
                
                if (hasUpdates) {
                    // Mark as pending sync
                    databaseManager.database.rfidModuleQueries.updateSyncStatusById("PENDING", rfidModuleId)
                    updatedCount++
                }
            }
            
            updatedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Database error updating batch tags: ${e.message}", e)
            throw e
        }
    }

    /**
     * Map generic field names to step-specific field names based on step portion
     * E.g., "Remark" -> "Remark60" for step with portion 60
     */
    private fun mapGenericFieldName(fieldName: String, stepPortion: Int): String {
        return when (fieldName) {
            "Remark" -> "Remark$stepPortion"
            "Is Completed" -> "IsCompleted$stepPortion"
            else -> fieldName
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
                Log.e(TAG, "Error getting step portion: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Update a specific field in RfidModule table
     */
    private suspend fun updateRfidModuleField(rfidModuleId: String, fieldName: String, fieldValue: String) = withContext(Dispatchers.IO) {
        try {
            when (fieldName.lowercase()) {
                "category" -> {
                    databaseManager.database.rfidModuleQueries.updateCategoryById(fieldValue, rfidModuleId)
                }
                "subcategory" -> {
                    databaseManager.database.rfidModuleQueries.updateSubcategoryById(fieldValue, rfidModuleId)
                }
                "serialno", "serial_no", "serial no.", "serial no", "edit serial no.", "edit serial no" -> {
                    // Serial No. is a unique field - skip update in batch mode to prevent duplicates
                    Log.d(TAG, "Serial No. is a unique field, skipping batch update to prevent duplicates")
                }
                "manufacturerid", "manufacturer_id" -> {
                    databaseManager.database.rfidModuleQueries.updateManufacturerIdById(fieldValue, rfidModuleId)
                }
                "supplierid", "supplier_id", "hinge supplier" -> {
                    databaseManager.database.rfidModuleQueries.updateSupplierIdById(fieldValue, rfidModuleId)
                }
                "region", "block" -> {
                    databaseManager.database.rfidModuleQueries.updateRegionById(fieldValue, rfidModuleId)
                }
                "floor" -> {
                    databaseManager.database.rfidModuleQueries.updateFloorById(fieldValue, rfidModuleId)
                }
                "room_id", "unit" -> {
                    databaseManager.database.rfidModuleQueries.updateRoomIdById(fieldValue, rfidModuleId)
                }
                "asn" -> {
                    databaseManager.database.rfidModuleQueries.updateASNById(fieldValue, rfidModuleId)
                }
                "batchno", "batch_no", "batch no.", "batch no" -> {
                    databaseManager.database.rfidModuleQueries.updateBatchNoById(fieldValue, rfidModuleId)
                }
                "t plate no.", "t_plate_no", "tplateno" -> {
                    // T Plate No. maps to BatchNo field
                    databaseManager.database.rfidModuleQueries.updateBatchNoById(fieldValue, rfidModuleId)
                    Log.d(TAG, "T Plate No. updated (mapped to BatchNo)")
                }
                "license plate no.", "license_plate_no", "licenseplateno" -> {
                    // License Plate No. is read-only - skip update
                    Log.d(TAG, "License Plate No. is read-only, skipping update")
                }
                "rscompanyid", "rs_company_id", "rs company" -> {
                    databaseManager.database.rfidModuleQueries.updateRSCompanyIdById(fieldValue, rfidModuleId)
                    Log.d(TAG, "RS Company updated")
                }
                "productno", "product_no", "product no.", "product no" -> {
                    databaseManager.database.rfidModuleQueries.updateProductNoById(fieldValue, rfidModuleId)
                    Log.d(TAG, "Product No. updated")
                }
                "concrete grade", "concretegrade" -> {
                    databaseManager.database.rfidModuleQueries.updateConcreteGradeById(fieldValue, rfidModuleId)
                }
                "rsinspectiondate", "rs_inspection_date", "rs inspection date" -> {
                    val timestampValue = convertDateStringToTimestamp(fieldValue)
                    databaseManager.database.rfidModuleQueries.updateRSInspectionDateById(timestampValue, rfidModuleId)
                    Log.d(TAG, "RS Inspection Date updated")
                }
                "internalfinishdate", "internal_finish_date", "internal finishes date" -> {
                    val timestampValue = convertDateStringToTimestamp(fieldValue)
                    databaseManager.database.rfidModuleQueries.updateInternalFinishDateById(timestampValue, rfidModuleId)
                    Log.d(TAG, "Internal Finishes Date updated")
                }
                "sitearrivaldate", "site_arrival_date", "site arrival date" -> {
                    val timestampValue = convertDateStringToTimestamp(fieldValue)
                    databaseManager.database.rfidModuleQueries.updateSiteArrivalDateById(timestampValue, rfidModuleId)
                    Log.d(TAG, "Site Arrival Date updated")
                }
                "castingdate", "casting_date", "casting date" -> {
                    // Date fields are INTEGER (timestamps) in schema
                    val timestampValue = convertDateStringToTimestamp(fieldValue)
                    databaseManager.database.rfidModuleQueries.updateCastingDateById(timestampValue, rfidModuleId)
                    Log.d(TAG, "Casting Date updated")
                }
                "casting date 2", "castingdate2", "casting_date_2" -> {
                    val timestampValue = convertDateStringToTimestamp(fieldValue)
                    databaseManager.database.rfidModuleQueries.updateSecondCastingDateById(timestampValue, rfidModuleId)
                    Log.d(TAG, "Casting Date 2 (SecondCastingDate) updated")
                }
                "deliverydate", "delivery_date", "delivery date" -> {
                    val timestampValue = convertDateStringToTimestamp(fieldValue)
                    databaseManager.database.rfidModuleQueries.updateDeliveryDateById(timestampValue, rfidModuleId)
                }
                "manufacturingdate", "manufacturing_date", "manufacturing date" -> {
                    val timestampValue = convertDateStringToTimestamp(fieldValue)
                    Log.d(TAG, "DEBUG - Saving Manufacturing Date: '$fieldValue' -> timestamp: $timestampValue")
                    databaseManager.database.rfidModuleQueries.updateManufacturingDateById(timestampValue, rfidModuleId)
                }
                "siteinstallationdate", "site_installation_date", "installation date", "site installation date" -> {
                    val timestampValue = convertDateStringToTimestamp(fieldValue)
                    databaseManager.database.rfidModuleQueries.updateSiteInstallationDateById(timestampValue, rfidModuleId)
                }
                // Step completion fields
                "iscompleted10", "is_completed_10" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted10ById(intValue.toLong(), rfidModuleId)
                }
                "remark10", "remark_10" -> {
                    databaseManager.database.rfidModuleQueries.updateRemark10ById(fieldValue, rfidModuleId)
                }
                "iscompleted20", "is_completed_20" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted20ById(intValue.toLong(), rfidModuleId)
                }
                "remark20", "remark_20" -> {
                    databaseManager.database.rfidModuleQueries.updateRemark20ById(fieldValue, rfidModuleId)
                }
                "iscompleted30", "is_completed_30" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted30ById(intValue.toLong(), rfidModuleId)
                }
                "remark30", "remark_30" -> {
                    databaseManager.database.rfidModuleQueries.updateRemark30ById(fieldValue, rfidModuleId)
                }
                "iscompleted35", "is_completed_35" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted55ById(intValue.toLong(), rfidModuleId)
                }
                "remark35", "remark_35" -> {
                    // MIC35 maps to Remark55
                    databaseManager.database.rfidModuleQueries.updateRemark55ById(fieldValue, rfidModuleId)
                }
                "iscompleted40", "is_completed_40" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted40ById(intValue.toLong(), rfidModuleId)
                }
                "remark40", "remark_40" -> {
                    databaseManager.database.rfidModuleQueries.updateRemark40ById(fieldValue, rfidModuleId)
                }
                "iscompleted50", "is_completed_50" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted50ById(intValue.toLong(), rfidModuleId)
                }
                "remark50", "remark_50" -> {
                    databaseManager.database.rfidModuleQueries.updateRemark50ById(fieldValue, rfidModuleId)
                }
                "iscompleted60", "is_completed_60" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted60ById(intValue.toLong(), rfidModuleId)
                }
                "remark60", "remark_60" -> {
                    databaseManager.database.rfidModuleQueries.updateRemark60ById(fieldValue, rfidModuleId)
                }
                "iscompleted70", "is_completed_70" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted70ById(intValue.toLong(), rfidModuleId)
                }
                "remark70", "remark_70" -> {
                    databaseManager.database.rfidModuleQueries.updateRemark70ById(fieldValue, rfidModuleId)
                }
                "iscompleted80", "is_completed_80" -> {
                    val intValue = if (fieldValue == "1" || fieldValue.lowercase() == "true") 1 else 0
                    databaseManager.database.rfidModuleQueries.updateIsCompleted80ById(intValue.toLong(), rfidModuleId)
                }
                "remark80", "remark_80" -> {
                    databaseManager.database.rfidModuleQueries.updateRemark80ById(fieldValue, rfidModuleId)
                }
                // Generic remark field (maps based on step code - handled by caller)
                "remark" -> {
                    // This should be handled by the caller to map to correct remarkXX field
                    Log.w(TAG, "Generic 'remark' field should be mapped to specific remarkXX field by caller")
                }
                else -> {
                    Log.w(TAG, "Unknown field name for update: $fieldName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating field $fieldName: ${e.message}", e)
            throw e
        }
    }

    /**
     * Convert date string to timestamp for database storage
     */
    private fun convertDateStringToTimestamp(dateString: String): Long? {
        return try {
            if (dateString.isBlank()) {
                null
            } else {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val date = format.parse(dateString)
                date?.time?.div(1000) // Convert to seconds
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse date string: $dateString, using null")
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "BatchStepFormViewModel cleared")
    }
}

/**
 * UI State for Batch Step Form
 */
data class BatchStepFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormSaved: Boolean = false,
    val error: String? = null,
    val stepFields: List<BatchStepFieldData> = emptyList()
)

/**
 * Batch step field data with default values
 */
data class BatchStepFieldData(
    val fieldName: String,
    val fieldType: String,
    val fieldLabel: String,
    val fieldOrder: Int,
    val isRequired: Boolean,
    val defaultValue: String,
    val dropdownOptions: List<DropdownOption> = emptyList()
)
