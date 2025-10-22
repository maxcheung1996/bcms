package com.socam.bcms.presentation.modules

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentBatchProcessBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import android.util.Log

/**
 * BatchProcessFragment - Batch processing module for multiple RFID tags
 * 
 * Features:
 * - BC Type filtering with dropdown
 * - Workflow steps horizontal list (like Single Scan)
 * - Real-time tag scanning with list display
 * - Batch editing with pen-to-edit functionality
 * - Individual workflow step saving
 */
class BatchProcessFragment : Fragment() {

    companion object {
        private const val TAG = "BatchProcessFragment"
    }

    private var _binding: FragmentBatchProcessBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BatchProcessViewModel by viewModels {
        BatchProcessViewModelFactory(requireContext())
    }

    private lateinit var workflowStepsAdapter: WorkflowStepsAdapter
    private lateinit var batchTagsAdapter: BatchTagsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchProcessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupUI()
        setupRecyclerViews()
        setupObservers()
        
        Log.d(TAG, "BatchProcessFragment initialized")
    }

    /**
     * Handle physical device key events for UHF scanning
     */
    override fun onResume() {
        super.onResume()
        // Set focus to allow key event handling
        view?.isFocusableInTouchMode = true
        view?.requestFocus()
        view?.setOnKeyListener { _, keyCode, event ->
            when (event.action) {
                KeyEvent.ACTION_DOWN -> onKeyDown(keyCode, event)
                KeyEvent.ACTION_UP -> onKeyUp(keyCode, event)
                else -> false
            }
        }
        Log.d(TAG, "Fragment focused for trigger key events")
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupUI() {
        // Clear List button
        binding.clearListButton.setOnClickListener {
            viewModel.clearTagList()
            Log.d(TAG, "Clear list button clicked")
        }
    }

    private fun setupRecyclerViews() {
        // Workflow Steps RecyclerView (3 columns like Single Scan)
        workflowStepsAdapter = WorkflowStepsAdapter { step ->
            onWorkflowStepClicked(step)
        }
        binding.workflowStepsRecycler.apply {
            adapter = workflowStepsAdapter
            layoutManager = GridLayoutManager(requireContext(), 3) // 3 items per row like Single Scan
        }

        // Batch Tags RecyclerView
        batchTagsAdapter = BatchTagsAdapter { epc ->
            viewModel.removeTag(epc)
            Log.d(TAG, "Remove tag clicked: $epc")
        }
        binding.scannedTagsRecycler.apply {
            adapter = batchTagsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        Log.d(TAG, "RecyclerViews configured")
    }

    private fun setupObservers() {
        viewModel.uiState.onEach { state ->
            updateUI(state)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateUI(state: BatchProcessUiState) {
        // Update BC Type dropdown
        updateBcTypeDropdown(state.availableBcTypes, state.selectedBcType)
        
        // Update workflow steps
        updateWorkflowSteps(state.workflowSteps)
        
        // Update scanned tags list
        updateScannedTagsList(state.scannedTags)
        
        // Update status message
        binding.statusMessage.text = state.statusMessage
        
        Log.d(TAG, "UI updated - BC Type: ${state.selectedBcType}, " +
                "Steps: ${state.workflowSteps.size}, Tags: ${state.scannedTags.size}")
    }

    private fun updateBcTypeDropdown(availableTypes: List<String>, selectedType: String) {
        if (availableTypes.isNotEmpty()) {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                availableTypes
            )
            binding.bcTypeDropdown.setAdapter(adapter)
            
            // Set selected value if not already set
            if (binding.bcTypeDropdown.text.toString() != selectedType) {
                binding.bcTypeDropdown.setText(selectedType, false)
            }
            
            // Set up selection listener
            binding.bcTypeDropdown.setOnItemClickListener { _, _, position, _ ->
                val newBcType = availableTypes[position]
                if (newBcType != selectedType) {
                    Log.d(TAG, "BC Type changed from $selectedType to $newBcType")
                    viewModel.selectBcType(newBcType)
                    
                    // Restore focus for trigger events
                    view?.requestFocus()
                }
            }
        }
    }

    private fun updateWorkflowSteps(workflowSteps: List<WorkflowStepDisplay>) {
        workflowStepsAdapter.updateSteps(workflowSteps)
        
        // Show/hide empty message
        if (workflowSteps.isEmpty()) {
            binding.workflowStepsEmptyMessage.visibility = View.VISIBLE
            binding.workflowStepsRecycler.visibility = View.GONE
        } else {
            binding.workflowStepsEmptyMessage.visibility = View.GONE
            binding.workflowStepsRecycler.visibility = View.VISIBLE
        }
    }

    private fun updateScannedTagsList(scannedTags: List<BatchTagData>) {
        batchTagsAdapter.submitList(scannedTags)
        
        // Show/hide empty state
        if (scannedTags.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.scannedTagsRecycler.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.scannedTagsRecycler.visibility = View.VISIBLE
        }
    }

    private fun onWorkflowStepClicked(step: WorkflowStepDisplay) {
        Log.d(TAG, "Workflow step clicked: ${step.stepCode}")
        
        val scannedTags = viewModel.uiState.value.scannedTags
        val selectedBcType = viewModel.uiState.value.selectedBcType
        
        if (scannedTags.isEmpty()) {
            // Show message if no tags scanned
            android.widget.Toast.makeText(
                requireContext(),
                "Please scan some tags first before editing workflow steps",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Show batch step form dialog
        showBatchStepFormDialog(step.stepCode, selectedBcType, scannedTags)
    }

    private fun showBatchStepFormDialog(stepCode: String, bcType: String, scannedTags: List<BatchTagData>) {
        Log.d(TAG, "Showing batch step form for: $stepCode with ${scannedTags.size} tags")
        
        // Create batch step form dialog
        val tagEpcs = scannedTags.map { it.epc }
        val dialog = BatchStepFormDialogFragment.newInstance(stepCode, bcType, tagEpcs)
        dialog.show(parentFragmentManager, "BatchStepFormDialog")
        
        // Restore focus for trigger events
        view?.requestFocus()
    }

    /**
     * Handle key down events for physical scanner trigger
     */
    private fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                if (event.repeatCount == 0) { // Ignore auto-repeat
                    Log.d(TAG, "Physical trigger pressed - starting scan")
                    viewModel.startScanning()
                }
                return true
            }
        }
        return false
    }

    /**
     * Handle key up events for physical scanner trigger
     */
    private fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                Log.d(TAG, "Physical trigger released - stopping scan")
                viewModel.stopScanning()
                return true
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "Fragment view destroyed")
    }
}