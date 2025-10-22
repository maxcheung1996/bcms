package com.socam.bcms.presentation.modules

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.socam.bcms.databinding.FragmentSingleScanBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Single Scan module fragment
 * Handles individual RFID tag scanning operations with physical device integration
 * 
 * Features:
 * - Hold-to-scan pattern: Press and hold trigger to scan, release to stop
 * - Rescan support: Can press and hold trigger again at ANY time to rescan:
 *   * Before selecting a tag from the list
 *   * After selecting a tag and viewing details
 *   * Clears all previous results and reloads with fresh scan
 * - Tag selection: After scanning, select from list of activated tags to view details
 * - Focus management: Maintains focus on parent view to ensure trigger always works
 */
class SingleScanFragment : Fragment() {
    
    private var _binding: FragmentSingleScanBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SingleScanViewModel by viewModels {
        SingleScanViewModelFactory(requireContext())
    }
    
    private lateinit var workflowStepsAdapter: WorkflowStepsAdapter
    private lateinit var candidateTagAdapter: CandidateTagAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSingleScanBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupUI()
        setupRecyclerView()
        setupObservers()
    }
    
    /**
     * Handle physical device key events (RFID scanner trigger)
     */
    override fun onResume() {
        super.onResume()
        // Set focus to allow key event handling (hold-to-scan pattern)
        view?.isFocusableInTouchMode = true
        view?.requestFocus()
        view?.setOnKeyListener { _, keyCode, event ->
            when (event.action) {
                KeyEvent.ACTION_DOWN -> onKeyDown(keyCode, event)
                KeyEvent.ACTION_UP -> onKeyUp(keyCode, event)
                else -> false
            }
        }
    }
    
    /**
     * Handle key down events for physical scanner trigger (hold-to-scan pattern)
     */
    private fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Check for RFID scanner trigger keys
        when (keyCode) {
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                if (event.repeatCount == 0) { // CRITICAL: Ignore auto-repeat
                    println("SingleScanFragment: Physical trigger pressed - starting scan")
                    viewModel.startScanning()
                }
                return true
            }
        }
        return false
    }
    
    /**
     * Handle key up events for physical scanner trigger (hold-to-scan pattern)
     */
    private fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                println("SingleScanFragment: Physical trigger released - stopping scan")
                viewModel.stopScanning()
                return true
            }
        }
        return false
    }
    
    private fun setupToolbar(): Unit {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupUI(): Unit {
        // Hardware trigger button is used for scanning - no UI button needed
        
        // Note: Manual mode removed - using auto-selection only
    }
    
    private fun setupRecyclerView(): Unit {
        // Workflow steps adapter
        workflowStepsAdapter = WorkflowStepsAdapter { step ->
            onWorkflowStepClicked(step)
        }
        binding.workflowStepsRecycler.apply {
            adapter = workflowStepsAdapter
            layoutManager = GridLayoutManager(requireContext(), 3) // 3 items per row
        }
        
        // Candidate tag selection adapter for manual mode
        candidateTagAdapter = CandidateTagAdapter { candidateTag ->
            viewModel.selectCandidateTag(candidateTag)
        }
        binding.tagSelectionRecycler.apply {
            adapter = candidateTagAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun onWorkflowStepClicked(step: WorkflowStepDisplay): Unit {
        println("SingleScanFragment: Workflow step clicked: ${step.stepCode}")
        showStepFormDialog(step.stepCode)
    }
    
    private fun showStepFormDialog(stepCode: String): Unit {
        println("SingleScanFragment: Showing step form for: $stepCode")
        // Get current tag's BC type and RfidModule data from UI state
        val tagBcType = viewModel.uiState.value.tagDetails?.bcType ?: ""
        val currentRfidModule = viewModel.uiState.value.currentRfidModule
        
        println("SingleScanFragment: Current RfidModule data available: ${currentRfidModule != null}")
        
        // Pass the actual RfidModule record for form auto-fill
        val dialog = StepFormDialogFragment.newInstance(stepCode, tagBcType, currentRfidModule)
        dialog.show(parentFragmentManager, "StepFormDialog")
    }
    
    private fun setupObservers(): Unit {
        viewModel.uiState.onEach { state ->
            updateUI(state)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
    
    private fun updateUI(state: SingleScanUiState): Unit {
        // Update status message
        binding.statusMessage.text = state.statusMessage
        
        // Update manual mode configuration UI
        updateManualModeUI(state)
        
        // Hardware trigger controls scanning - no UI button state needed
        
        // Show/hide tag selection card for manual mode
        if (state.showTagSelection && state.candidateTags.isNotEmpty()) {
            binding.tagSelectionCard.visibility = View.VISIBLE
            updateTagSelectionUI(state.candidateTags)
            
            // CRITICAL: Re-request focus to allow rescanning even when tag list is displayed
            view?.requestFocus()
        } else {
            binding.tagSelectionCard.visibility = View.GONE
        }
        
        // Show/hide sections based on tag data availability
        if (state.tagDetails != null) {
            binding.tagDetailsCard.visibility = View.VISIBLE
            updateTagDetails(state.tagDetails)
        } else {
            binding.tagDetailsCard.visibility = View.GONE
        }
        
        if (state.workflowSteps.isNotEmpty()) {
            println("SingleScanFragment: Showing workflow steps card with ${state.workflowSteps.size} steps")
            binding.workflowStepsCard.visibility = View.VISIBLE
            workflowStepsAdapter.updateSteps(state.workflowSteps)
            println("SingleScanFragment: Workflow steps adapter updated")
        } else {
            println("SingleScanFragment: Hiding workflow steps card - no steps available")
            binding.workflowStepsCard.visibility = View.GONE
        }
        
        if (state.tagInformation != null) {
            binding.tagInformationCard.visibility = View.VISIBLE
            updateTagInformation(state.tagInformation)
        } else {
            binding.tagInformationCard.visibility = View.GONE
        }
    }
    
    /**
     * Update manual mode configuration UI
     */
    private fun updateManualModeUI(state: SingleScanUiState): Unit {
        // Note: Manual selection mode removed - auto-selection only
    }
    
    /**
     * Update tag selection UI for manual mode
     */
    private fun updateTagSelectionUI(candidateTags: List<CandidateTag>): Unit {
        // Update count badge
        binding.tagSelectionCount.text = "${candidateTags.size} tag${if (candidateTags.size != 1) "s" else ""}"
        
        // Update adapter
        candidateTagAdapter.updateCandidateTags(candidateTags)
    }
    
    private fun updateTagDetails(tagDetails: TagDetails): Unit {
        binding.bcTypeText.text = "BC Type: ${tagDetails.bcType}"
        binding.tagChipIdText.text = "Tag Chip ID: ${tagDetails.tagChipId}"
        binding.tagNoText.text = "Tag No.: ${tagDetails.tagNo}"
    }
    
    private fun updateTagInformation(tagInfo: TagInformation): Unit {
        binding.contractNoText.text = "Contract No: ${tagInfo.contractNo}"
        binding.contractDescriptionText.text = "Contract Description: ${tagInfo.contractDescription}"
        binding.contractorText.text = "Contractor: ${tagInfo.contractor}"
        binding.manufacturerIdText.text = "Manufacturer ID: ${tagInfo.manufacturerId}"
        binding.manufacturerAddressText.text = "Manufacturer Address: ${tagInfo.manufacturerAddress}"
        binding.tagIdText.text = "Tag ID: ${tagInfo.tagId}"
        binding.bcTypeInfoText.text = "BC Type: ${tagInfo.bcType}"
        binding.tagNoInfoText.text = "Tag No.: ${tagInfo.tagNo}"
        binding.asnText.text = "A.S.N.: ${tagInfo.asn}"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
