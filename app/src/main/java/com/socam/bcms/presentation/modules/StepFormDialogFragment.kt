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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.textfield.TextInputLayout
import com.socam.bcms.R
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.databinding.DialogStepFormBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog fragment for displaying and editing workflow step fields
 */
class StepFormDialogFragment : DialogFragment() {

    private var _binding: DialogStepFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StepFormViewModel by viewModels {
        StepFormViewModelFactory(DatabaseManager.getInstance(requireContext()))
    }

    private var stepCode: String = ""
    private var tagBcType: String = ""
    private var tagData: com.socam.bcms.database.RfidModule? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val datetimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    companion object {
        fun newInstance(stepCode: String, tagBcType: String, tagData: com.socam.bcms.database.RfidModule? = null): StepFormDialogFragment {
            val fragment = StepFormDialogFragment()
            val args = Bundle()
            args.putString("step_code", stepCode)
            args.putString("tag_bc_type", tagBcType)
            tagData?.let {
                // Since RfidModule is complex, we'll pass the tag ID and re-query
                args.putString("tag_id", it.TagId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stepCode = arguments?.getString("step_code") ?: ""
        tagBcType = arguments?.getString("tag_bc_type") ?: ""
        
        // Get tag data if tag_id is provided
        val tagId = arguments?.getString("tag_id")
        if (tagId != null) {
            // We'll load the tag data in onViewCreated using the ViewModel
        }
        
        setStyle(STYLE_NORMAL, R.style.Theme_BCMS_Dialog_FullScreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStepFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        
        // Load step fields with tag data
        val tagId = arguments?.getString("tag_id")
        if (tagId != null) {
            // Load tag data first, then load step fields
            loadTagDataAndStepFields(tagId)
        } else {
            // Load step fields without tag data
            viewModel.loadStepFields(stepCode, tagBcType, null)
        }
    }

    private fun setupUI(): Unit {
        binding.dialogTitle.text = "$stepCode Form"
        
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        
        binding.saveButton.setOnClickListener {
            // Capture all text field values before saving
            captureAllTextFieldValues()
            viewModel.saveStepForm()
        }
    }

    private fun setupObservers(): Unit {
        viewModel.uiState.onEach { state ->
            updateUI(state)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateUI(state: StepFormUiState): Unit {
        // Show/hide loading
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
        binding.saveButton.isEnabled = !state.isSaving && !state.isLoading
        binding.saveButton.text = if (state.isSaving) "Saving..." else "Save"
        
        // Create form fields
        if (state.stepFields.isNotEmpty()) {
            if (binding.formFieldsContainer.childCount == 0) {
                createFormFields(state.stepFields)
            } else {
                // Update existing fields (for cascading dropdowns)
                updateFormFields(state.stepFields)
            }
        }
        
        // Handle form saved
        if (state.isFormSaved) {
            Toast.makeText(requireContext(), "Step form saved successfully", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun createFormFields(fields: List<StepFieldData>): Unit {
        binding.formFieldsContainer.removeAllViews()
        
        fields.forEach { field ->
            val fieldView = createFieldView(field)
            binding.formFieldsContainer.addView(fieldView)
        }
    }

    private fun createFieldView(field: StepFieldData): View {
        return when (field.fieldType) {
            "text", "integer" -> createTextFieldView(field)
            "dropdown" -> createDropdownFieldView(field)
            "date", "datetime" -> createDateFieldView(field)
            "checkbox" -> createCheckboxFieldView(field)
            else -> createTextFieldView(field) // Default to text field
        }
    }

    private fun createTextFieldView(field: StepFieldData): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_field_text, binding.formFieldsContainer, false)
        
        val textInputLayout = view as TextInputLayout
        val textInput = textInputLayout.findViewById<TextInputEditText>(R.id.field_input)
        
        textInputLayout.hint = field.fieldLabel
        textInput.setText(field.currentValue)
        
        if (field.fieldType == "integer") {
            textInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        
        // Make License Plate No. read-only (not editable)
        if (field.fieldName == "License Plate No.") {
            textInput.isEnabled = false
            textInputLayout.isEnabled = false
            println("StepFormDialogFragment: License Plate No. field set to read-only")
        } else {
            textInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    viewModel.updateFieldValue(field.fieldName, textInput.text.toString())
                }
            }
        }
        
        return view
    }

    private fun createDropdownFieldView(field: StepFieldData): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_field_dropdown, binding.formFieldsContainer, false)
        
        val textInputLayout = view as TextInputLayout
        val dropdown = textInputLayout.findViewById<AutoCompleteTextView>(R.id.field_dropdown)
        
        textInputLayout.hint = field.fieldLabel
        
        // Map value to label for display (e.g., UUID '98d12d75...' -> "Room A")
        // This ensures dropdowns show human-readable labels instead of IDs
        val currentLabel = field.dropdownOptions.find { it.value == field.currentValue }?.label ?: field.currentValue
        dropdown.setText(currentLabel)
        
        // Set dropdown options from field data
        val options = field.dropdownOptions.map { it.label }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        dropdown.setAdapter(adapter)
        
        dropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedOption = field.dropdownOptions[position]
            viewModel.updateFieldValue(field.fieldName, selectedOption.value)
        }
        
        return view
    }

    private fun createDateFieldView(field: StepFieldData): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_field_date, binding.formFieldsContainer, false)
        
        val textInputLayout = view as TextInputLayout
        val dateInput = textInputLayout.findViewById<TextInputEditText>(R.id.field_date)
        
        textInputLayout.hint = field.fieldLabel
        dateInput.setText(field.currentValue)
        
        dateInput.setOnClickListener {
            showDatePicker(field, dateInput)
        }
        
        textInputLayout.setEndIconOnClickListener {
            showDatePicker(field, dateInput)
        }
        
        return view
    }

    private fun createCheckboxFieldView(field: StepFieldData): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_field_checkbox, binding.formFieldsContainer, false)
        
        val checkbox = view.findViewById<CheckBox>(R.id.field_checkbox)
        val label = view.findViewById<TextView>(R.id.field_label)
        
        label.text = field.fieldLabel
        checkbox.isChecked = field.currentValue.toBoolean()
        
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateFieldValue(field.fieldName, isChecked.toString())
        }
        
        return view
    }

    private fun showDatePicker(field: StepFieldData, dateInput: TextInputEditText): Unit {
        val calendar = Calendar.getInstance()
        
        // Try to parse existing date
        if (field.currentValue.isNotEmpty()) {
            try {
                val existingDate = if (field.fieldType == "datetime") {
                    datetimeFormat.parse(field.currentValue)
                } else {
                    dateFormat.parse(field.currentValue)
                }
                existingDate?.let { calendar.time = it }
            } catch (e: Exception) {
                // Use current date if parsing fails
            }
        }
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val formattedDate = if (field.fieldType == "datetime") {
                    // For datetime, use current time
                    datetimeFormat.format(calendar.time)
                } else {
                    dateFormat.format(calendar.time)
                }
                dateInput.setText(formattedDate)
                viewModel.updateFieldValue(field.fieldName, formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }

    private fun updateFormFields(fields: List<StepFieldData>): Unit {
        fields.forEachIndexed { index, field ->
            if (index < binding.formFieldsContainer.childCount) {
                val fieldView = binding.formFieldsContainer.getChildAt(index)
                
                // Update dropdown options if this is a dropdown field
                if (field.fieldType == "dropdown") {
                    val textInputLayout = fieldView as? TextInputLayout
                    val dropdown = textInputLayout?.findViewById<AutoCompleteTextView>(R.id.field_dropdown)
                    
                    if (dropdown != null) {
                        val options = field.dropdownOptions.map { it.label }.toTypedArray()
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
                        dropdown.setAdapter(adapter)
                        
                        // Update current value - map value to label (e.g., UUID to room name)
                        val currentLabel = field.dropdownOptions.find { it.value == field.currentValue }?.label ?: field.currentValue
                        if (dropdown.text.toString() != currentLabel) {
                            dropdown.setText(currentLabel)
                        }
                        
                        dropdown.setOnItemClickListener { _, _, position, _ ->
                            val selectedOption = field.dropdownOptions[position]
                            viewModel.updateFieldValue(field.fieldName, selectedOption.value)
                        }
                    }
                }
            }
        }
    }

    private fun loadTagDataAndStepFields(tagId: String) {
        // Use a simple coroutine to load tag data and then step fields
        lifecycleScope.launch {
            try {
                val databaseManager = com.socam.bcms.data.database.DatabaseManager.getInstance(requireContext())
                val tagData = withContext(Dispatchers.IO) {
                    databaseManager.database.rfidModuleQueries.selectModulesByTagId(tagId).executeAsList().firstOrNull()
                }
                
                // Load step fields with tag data
                viewModel.loadStepFields(stepCode, tagBcType, tagData)
                
            } catch (e: Exception) {
                println("StepFormDialogFragment: Error loading tag data: ${e.message}")
                // Load step fields without tag data as fallback
                viewModel.loadStepFields(stepCode, tagBcType, null)
            }
        }
    }

    /**
     * Capture all text field values from the form before saving
     * This ensures that fields with focus have their values captured
     */
    private fun captureAllTextFieldValues() {
        val stepFields = viewModel.uiState.value.stepFields
        
        println("StepFormDialogFragment: captureAllTextFieldValues() called")
        println("StepFormDialogFragment: Form has ${binding.formFieldsContainer.childCount} child views")
        println("StepFormDialogFragment: StepFields has ${stepFields.size} fields")
        
        // Iterate through all child views and capture text values
        for (i in 0 until binding.formFieldsContainer.childCount) {
            val fieldView = binding.formFieldsContainer.getChildAt(i)
            
            // Try to find the corresponding field by checking the view type
            val textInputLayout = fieldView as? TextInputLayout
            if (textInputLayout != null) {
                val textInput = textInputLayout.findViewById<TextInputEditText>(R.id.field_input)
                if (textInput != null) {
                    val currentText = textInput.text?.toString() ?: ""
                    val hint = textInputLayout.hint?.toString() ?: ""
                    
                    println("StepFormDialogFragment: View $i - hint='$hint', text='$currentText'")
                    
                    // Find the field by matching the hint/label
                    val matchingField = stepFields.find { field ->
                        field.fieldLabel == hint || field.fieldName == hint
                    }
                    
                    if (matchingField != null) {
                        println("StepFormDialogFragment: Matched field '${matchingField.fieldName}' (type=${matchingField.fieldType})")
                        
                        // Skip read-only fields like License Plate No.
                        if (matchingField.fieldName == "License Plate No.") {
                            println("StepFormDialogFragment: Skipping read-only field 'License Plate No.'")
                        } else if (matchingField.fieldType == "text" || matchingField.fieldType == "integer") {
                            // Update the ViewModel with the current text value
                            viewModel.updateFieldValue(matchingField.fieldName, currentText)
                            println("StepFormDialogFragment: ✓ Captured text field '${matchingField.fieldName}' = '$currentText'")
                        }
                    } else {
                        println("StepFormDialogFragment: ⚠ No matching field found for hint='$hint'")
                    }
                }
            }
        }
        
        println("StepFormDialogFragment: captureAllTextFieldValues() completed")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
