package com.socam.bcms.presentation.modules

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.socam.bcms.R
import com.socam.bcms.databinding.DialogBatchStepFormBinding
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog fragment for batch editing workflow step fields across multiple tags
 */
class BatchStepFormDialogFragment : DialogFragment() {

    companion object {
        private const val TAG = "BatchStepFormDialog"
        
        fun newInstance(
            stepCode: String,
            bcType: String,
            tagEpcs: List<String>
        ): BatchStepFormDialogFragment {
            return BatchStepFormDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("step_code", stepCode)
                    putString("bc_type", bcType)
                    putStringArrayList("tag_epcs", ArrayList(tagEpcs))
                }
            }
        }
    }

    private var _binding: DialogBatchStepFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BatchStepFormViewModel by viewModels {
        BatchStepFormViewModelFactory(requireContext())
    }

    private lateinit var stepCode: String
    private lateinit var bcType: String
    private lateinit var tagEpcs: List<String>

    // Track enabled fields for batch save
    private val enabledFields = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        stepCode = arguments?.getString("step_code") ?: ""
        bcType = arguments?.getString("bc_type") ?: ""
        tagEpcs = arguments?.getStringArrayList("tag_epcs") ?: emptyList()
        
        Log.d(TAG, "BatchStepFormDialog created for step: $stepCode, BC type: $bcType, tags: ${tagEpcs.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBatchStepFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        
        // Load step fields for batch editing
        viewModel.loadStepFields(stepCode, bcType, tagEpcs)
    }

    private fun setupUI() {
        binding.dialogTitle.text = "Batch Edit - $stepCode Form"
        binding.batchInfoText.text = "Editing workflow step for ${tagEpcs.size} $bcType tags. Only enabled fields will be updated."
        
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        
        binding.saveButton.setOnClickListener {
            Log.d(TAG, "Save button clicked - enabled fields: $enabledFields")
            // Capture all enabled text field values before saving
            captureAllEnabledTextFieldValues()
            viewModel.saveBatchStepForm(enabledFields.toList())
        }
    }

    private fun setupObservers() {
        viewModel.uiState.onEach { state ->
            updateUI(state)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateUI(state: BatchStepFormUiState) {
        binding.loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.formScrollView.visibility = if (state.isLoading) View.GONE else View.VISIBLE
        
        // Show/hide error
        if (state.error != null) {
            binding.errorMessage.text = state.error
            binding.errorMessage.visibility = View.VISIBLE
        } else {
            binding.errorMessage.visibility = View.GONE
        }
        
        // Update save button state
        binding.saveButton.isEnabled = !state.isSaving && !state.isLoading && enabledFields.isNotEmpty()
        binding.saveButton.text = if (state.isSaving) "Saving to ${tagEpcs.size} tags..." else "Save to All Tags"
        
        // Create form fields
        if (state.stepFields.isNotEmpty() && binding.formFieldsContainer.childCount == 0) {
            createBatchFormFields(state.stepFields)
        }
        
        // Handle form saved
        if (state.isFormSaved) {
            Toast.makeText(requireContext(), "Batch step form saved to ${tagEpcs.size} tags", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun createBatchFormFields(fields: List<BatchStepFieldData>) {
        binding.formFieldsContainer.removeAllViews()
        
        fields.forEach { field ->
            val fieldView = createBatchFieldView(field)
            binding.formFieldsContainer.addView(fieldView)
        }
        
        Log.d(TAG, "Created ${fields.size} batch form fields")
    }

    private fun createBatchFieldView(field: BatchStepFieldData): View {
        return when (field.fieldType) {
            "text", "integer" -> createBatchTextFieldView(field)
            "dropdown" -> createBatchDropdownFieldView(field)
            "date", "datetime" -> createBatchDateFieldView(field)
            "checkbox" -> createBatchCheckboxFieldView(field)
            else -> createBatchTextFieldView(field) // Default to text field
        }
    }

    private fun createBatchTextFieldView(field: BatchStepFieldData): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_batch_field_text, binding.formFieldsContainer, false)
        
        val textInputLayout = view.findViewById<TextInputLayout>(R.id.textInputLayout)
        val textInput = view.findViewById<TextInputEditText>(R.id.field_input)
        val editButton = view.findViewById<Button>(R.id.editButton)
        
        textInputLayout.hint = field.fieldLabel
        textInput.setText(field.defaultValue)
        
        if (field.fieldType == "integer") {
            textInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        
        // Note: Unique fields (Serial No., Edit Serial No., License Plate No.) are filtered out 
        // at the ViewModel level and won't appear in the batch form
        
        // Pen button toggles field editing
        editButton.setOnClickListener {
            val isEnabled = !textInput.isEnabled
            textInput.isEnabled = isEnabled
            textInputLayout.isEnabled = isEnabled
            
            if (isEnabled) {
                enabledFields.add(field.fieldName)
                editButton.setTextColor(android.graphics.Color.GREEN)
                textInput.requestFocus()
                Log.d(TAG, "Enabled field: ${field.fieldName}")
            } else {
                enabledFields.remove(field.fieldName)
                editButton.setTextColor(android.graphics.Color.GRAY)
                Log.d(TAG, "Disabled field: ${field.fieldName}")
            }
            
            // Update save button state
            binding.saveButton.isEnabled = enabledFields.isNotEmpty() && !viewModel.uiState.value.isSaving
        }
        
        // Update field value when changed
        textInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && enabledFields.contains(field.fieldName)) {
                viewModel.updateFieldValue(field.fieldName, textInput.text.toString())
            }
        }
        
        return view
    }

    private fun createBatchDropdownFieldView(field: BatchStepFieldData): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_batch_field_dropdown, binding.formFieldsContainer, false)
        
        val dropdownInputLayout = view.findViewById<TextInputLayout>(R.id.dropdownInputLayout)
        val dropdown = view.findViewById<AutoCompleteTextView>(R.id.field_dropdown)
        val editButton = view.findViewById<Button>(R.id.editButton)
        
        dropdownInputLayout.hint = field.fieldLabel
        
        // Set up dropdown options
        val options = field.dropdownOptions.map { it.label }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        dropdown.setAdapter(adapter)
        dropdown.setText(field.defaultValue, false)
        
        // Pen button toggles field editing
        editButton.setOnClickListener {
            val isEnabled = !dropdown.isEnabled
            dropdown.isEnabled = isEnabled
            dropdownInputLayout.isEnabled = isEnabled
            
            if (isEnabled) {
                enabledFields.add(field.fieldName)
                editButton.setTextColor(android.graphics.Color.GREEN)
                Log.d(TAG, "Enabled dropdown field: ${field.fieldName}")
            } else {
                enabledFields.remove(field.fieldName)
                editButton.setTextColor(android.graphics.Color.GRAY)
                Log.d(TAG, "Disabled dropdown field: ${field.fieldName}")
            }
            
            // Update save button state
            binding.saveButton.isEnabled = enabledFields.isNotEmpty() && !viewModel.uiState.value.isSaving
        }
        
        // Update field value when selected
        dropdown.setOnItemClickListener { _, _, position, _ ->
            if (enabledFields.contains(field.fieldName)) {
                val selectedOption = field.dropdownOptions[position]
                viewModel.updateFieldValue(field.fieldName, selectedOption.value)
                Log.d(TAG, "Dropdown selection: ${field.fieldName} = ${selectedOption.value}")
            }
        }
        
        return view
    }

    private fun createBatchDateFieldView(field: BatchStepFieldData): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_batch_field_date, binding.formFieldsContainer, false)
        
        val dateInputLayout = view.findViewById<TextInputLayout>(R.id.dateInputLayout)
        val dateInput = view.findViewById<TextInputEditText>(R.id.field_date)
        val editButton = view.findViewById<Button>(R.id.editButton)
        
        dateInputLayout.hint = field.fieldLabel
        dateInput.setText(field.defaultValue)
        
        // Pen button toggles field editing
        editButton.setOnClickListener {
            val isEnabled = !dateInput.isEnabled
            dateInput.isEnabled = isEnabled
            dateInputLayout.isEnabled = isEnabled
            
            if (isEnabled) {
                enabledFields.add(field.fieldName)
                editButton.setTextColor(android.graphics.Color.GREEN)
                Log.d(TAG, "Enabled date field: ${field.fieldName}")
            } else {
                enabledFields.remove(field.fieldName)
                editButton.setTextColor(android.graphics.Color.GRAY)
                Log.d(TAG, "Disabled date field: ${field.fieldName}")
            }
            
            // Update save button state
            binding.saveButton.isEnabled = enabledFields.isNotEmpty() && !viewModel.uiState.value.isSaving
        }
        
        // Date picker when field is clicked and enabled
        dateInput.setOnClickListener {
            if (dateInput.isEnabled && enabledFields.contains(field.fieldName)) {
                showDatePicker(field.fieldName, dateInput)
            }
        }
        
        // Calendar icon click
        dateInputLayout.setEndIconOnClickListener {
            if (dateInput.isEnabled && enabledFields.contains(field.fieldName)) {
                showDatePicker(field.fieldName, dateInput)
            }
        }
        
        return view
    }

    private fun createBatchCheckboxFieldView(field: BatchStepFieldData): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_batch_field_checkbox, binding.formFieldsContainer, false)
        
        val fieldLabel = view.findViewById<TextView>(R.id.fieldLabel)
        val checkbox = view.findViewById<MaterialCheckBox>(R.id.field_checkbox)
        val editButton = view.findViewById<Button>(R.id.editButton)
        
        fieldLabel.text = field.fieldLabel
        checkbox.isChecked = field.defaultValue == "1"
        
        // Pen button toggles field editing
        editButton.setOnClickListener {
            val isEnabled = !checkbox.isEnabled
            checkbox.isEnabled = isEnabled
            
            if (isEnabled) {
                enabledFields.add(field.fieldName)
                editButton.setTextColor(android.graphics.Color.GREEN)
                Log.d(TAG, "Enabled checkbox field: ${field.fieldName}")
            } else {
                enabledFields.remove(field.fieldName)
                editButton.setTextColor(android.graphics.Color.GRAY)
                Log.d(TAG, "Disabled checkbox field: ${field.fieldName}")
            }
            
            // Update save button state
            binding.saveButton.isEnabled = enabledFields.isNotEmpty() && !viewModel.uiState.value.isSaving
        }
        
        // Update field value when changed
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (enabledFields.contains(field.fieldName)) {
                val value = if (isChecked) "1" else "0"
                viewModel.updateFieldValue(field.fieldName, value)
                Log.d(TAG, "Checkbox changed: ${field.fieldName} = $value")
            }
        }
        
        return view
    }

    private fun showDatePicker(fieldName: String, dateInput: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val currentDate = dateInput.text.toString()
        
        // Parse current date if available
        if (currentDate.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(currentDate)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse date: $currentDate")
            }
        }
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                dateInput.setText(selectedDate)
                viewModel.updateFieldValue(fieldName, selectedDate)
                Log.d(TAG, "Date selected: $fieldName = $selectedDate")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }

    /**
     * Capture all enabled field values from the batch form before saving
     * This ensures that fields with focus have their values captured
     * Handles: text, integer, date, dropdown, and checkbox fields
     */
    private fun captureAllEnabledTextFieldValues() {
        val stepFields = viewModel.uiState.value.stepFields
        
        Log.d(TAG, "captureAllEnabledFieldValues() called")
        Log.d(TAG, "Form has ${binding.formFieldsContainer.childCount} child views")
        Log.d(TAG, "Enabled fields: $enabledFields")
        
        // Iterate through all child views and capture enabled field values
        for (i in 0 until binding.formFieldsContainer.childCount) {
            val fieldView = binding.formFieldsContainer.getChildAt(i)
            
            // 1. Try to find TextInputLayout (for text, integer, and date fields)
            val textInputLayout = fieldView.findViewById<TextInputLayout>(R.id.textInputLayout)
                ?: fieldView.findViewById<TextInputLayout>(R.id.dateInputLayout)
            
            if (textInputLayout != null) {
                val textInput = textInputLayout.findViewById<TextInputEditText>(R.id.field_input)
                    ?: textInputLayout.findViewById<TextInputEditText>(R.id.field_date)
                
                if (textInput != null && textInput.isEnabled) {
                    val currentText = textInput.text?.toString() ?: ""
                    val hint = textInputLayout.hint?.toString() ?: ""
                    
                    Log.d(TAG, "View $i - TEXT/DATE field: hint='$hint', text='$currentText', enabled=${textInput.isEnabled}")
                    
                    // Find the field by matching the hint/label
                    val matchingField = stepFields.find { field ->
                        field.fieldLabel == hint || field.fieldName == hint
                    }
                    
                    if (matchingField != null && enabledFields.contains(matchingField.fieldName)) {
                        Log.d(TAG, "Matched enabled field '${matchingField.fieldName}' (type=${matchingField.fieldType})")
                        
                        // Update the ViewModel with the current text value
                        viewModel.updateFieldValue(matchingField.fieldName, currentText)
                        Log.d(TAG, "✓ Captured enabled text field '${matchingField.fieldName}' = '$currentText'")
                    }
                }
                continue
            }
            
            // 2. Try to find dropdown (AutoCompleteTextView)
            val dropdownInputLayout = fieldView.findViewById<TextInputLayout>(R.id.dropdownInputLayout)
            if (dropdownInputLayout != null) {
                val dropdown = fieldView.findViewById<AutoCompleteTextView>(R.id.field_dropdown)
                
                if (dropdown != null && dropdown.isEnabled) {
                    val currentValue = dropdown.text?.toString() ?: ""
                    val hint = dropdownInputLayout.hint?.toString() ?: ""
                    
                    Log.d(TAG, "View $i - DROPDOWN field: hint='$hint', value='$currentValue', enabled=${dropdown.isEnabled}")
                    
                    // Find the field by matching the hint/label
                    val matchingField = stepFields.find { field ->
                        field.fieldLabel == hint || field.fieldName == hint
                    }
                    
                    if (matchingField != null && enabledFields.contains(matchingField.fieldName)) {
                        // For dropdown, we need to find the actual value from the label
                        val selectedOption = matchingField.dropdownOptions.find { it.label == currentValue }
                        val valueToSave = selectedOption?.value ?: currentValue
                        
                        viewModel.updateFieldValue(matchingField.fieldName, valueToSave)
                        Log.d(TAG, "✓ Captured enabled dropdown field '${matchingField.fieldName}' = '$valueToSave' (label='$currentValue')")
                    }
                }
                continue
            }
            
            // 3. Try to find checkbox (MaterialCheckBox)
            val checkbox = fieldView.findViewById<MaterialCheckBox>(R.id.field_checkbox)
            if (checkbox != null) {
                val fieldLabel = fieldView.findViewById<TextView>(R.id.fieldLabel)
                
                if (checkbox.isEnabled && fieldLabel != null) {
                    val isChecked = checkbox.isChecked
                    val label = fieldLabel.text?.toString() ?: ""
                    val value = if (isChecked) "1" else "0"
                    
                    Log.d(TAG, "View $i - CHECKBOX field: label='$label', checked=$isChecked, enabled=${checkbox.isEnabled}")
                    
                    // Find the field by matching the label
                    val matchingField = stepFields.find { field ->
                        field.fieldLabel == label || field.fieldName == label
                    }
                    
                    if (matchingField != null && enabledFields.contains(matchingField.fieldName)) {
                        viewModel.updateFieldValue(matchingField.fieldName, value)
                        Log.d(TAG, "✓ Captured enabled checkbox field '${matchingField.fieldName}' = '$value'")
                    }
                }
            }
        }
        
        Log.d(TAG, "captureAllEnabledFieldValues() completed")
    }

    override fun onStart() {
        super.onStart()
        // Make dialog larger
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "BatchStepFormDialog view destroyed")
    }
}
