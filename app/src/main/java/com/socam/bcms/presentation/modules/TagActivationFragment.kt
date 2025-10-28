package com.socam.bcms.presentation.modules

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentTagActivationBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*

/**
 * Tag Activation module fragment
 * Follows battle-tested UHF patterns from UHF_IMPLEMENTATION_GUIDE.md
 *  
 * Flow: Hold trigger → Scan inactive tags → Select BC Type → Activate → Write EPC → Show tag number
 */
class TagActivationFragment : Fragment() {
    
    companion object {
        private const val TAG = "TagActivationFragment"
    }
    
    private var _binding: FragmentTagActivationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TagActivationViewModel by viewModels {
        TagActivationViewModelFactory(requireContext())
    }
    
    private lateinit var bcTypeAdapter: ArrayAdapter<String>
    private lateinit var candidateTagAdapter: TagActivationCandidateTagAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTagActivationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }
    
    /**
     * Handle physical device key events (RFID scanner trigger)
     * CRITICAL: Hold-to-scan, release-to-stop pattern
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
    }
    
    /**
     * Handle key down events - start scanning on trigger press
     * CRITICAL: Following vendor demo trigger pattern
     */
    private fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                if (event?.repeatCount == 0) { // CRITICAL: Ignore auto-repeat
                    Log.d(TAG, "Physical trigger pressed - starting scan")
                    viewModel.startScanning()
                }
                return true
            }
        }
        return false
    }
    
    /**
     * Handle key up events - stop scanning on trigger release
     * CRITICAL: Release-to-stop pattern
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
    
    private fun setupToolbar(): Unit {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupUI(): Unit {
        // Setup BC Type dropdown
        bcTypeAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        binding.bcTypeDropdown.setAdapter(bcTypeAdapter)
        
        // BC Type selection listener
        binding.bcTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedBcType = bcTypeAdapter.getItem(position)
            if (selectedBcType != null) {
                Log.d(TAG, "BC Type selected: $selectedBcType (focus will be restored)")
                viewModel.updateBcType(selectedBcType)
            }
        }
        
        // Setup RecyclerView adapters
        // Note: Manual mode removed - only auto selection now
    }
    
    private fun setupRecyclerView(): Unit {
        // Manual Selection RecyclerView
        candidateTagAdapter = TagActivationCandidateTagAdapter { candidateTag ->
            viewModel.selectCandidateTag(candidateTag)
        }
        
        binding.tagSelectionRecycler.apply {
            adapter = candidateTagAdapter
            layoutManager = LinearLayoutManager(requireContext())
            
            // CRITICAL: Don't steal focus from Fragment - allow trigger events to pass through
            isFocusable = false
            isFocusableInTouchMode = false
            
            Log.d(TAG, "RecyclerView configured to allow trigger events")
        }
    }
    
    private fun setupObservers(): Unit {
        // Observe BC Type options
        viewModel.bcTypeOptions.onEach { options ->
            bcTypeAdapter.clear()
            for (option in options) {
                bcTypeAdapter.add(option)
            }
            bcTypeAdapter.notifyDataSetChanged()
        }.launchIn(viewLifecycleOwner.lifecycleScope)
        
        // Observe UI state
        viewModel.uiState.onEach { state ->
            updateUI(state)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
    
    private fun setupClickListeners(): Unit {
        // Activate button
        binding.activateButton.setOnClickListener {
            viewModel.activateTag()
        }
    }
    
    private fun updateUI(state: TagActivationUiState): Unit {
        // CRITICAL: Focus restoration pattern for trigger responsiveness
        if (state.needsFocusRestore) {
            Log.d(TAG, "🔧 Restoring Fragment focus for trigger responsiveness")
            view?.requestFocus()
            viewModel.clearFocusRestoreFlag()
        }
        
        // Update manual mode UI
        updateManualModeUI(state)
        
        // Update BC Type dropdown
        if (binding.bcTypeDropdown.text.toString() != state.bcType) {
            binding.bcTypeDropdown.setText(state.bcType, false)
        }
        
        // Update field enabled state
        binding.bcTypeDropdown.isEnabled = state.fieldsEnabled && !state.isActivated
        
        // Show/hide tag selection card for manual mode
        if (state.showTagSelection && state.candidateTags.isNotEmpty()) {
            binding.tagSelectionCard.visibility = View.VISIBLE
            updateTagSelectionUI(state.candidateTags)
            
            // CRITICAL: Restore focus to Fragment after showing tag list
            // This ensures trigger button still works for rescanning
            view?.post {
                view?.requestFocus()
                Log.d(TAG, "🔧 Focus restored after showing tag list - trigger ready for rescan")
            }
        } else {
            binding.tagSelectionCard.visibility = View.GONE
        }
        
        // Update scanned tag display
        updateScannedTagDisplay(state)
        
        // Update scanning status
        updateScanningStatus(state)
        
        // Update error display with prominent red styling
        updateErrorDisplay(state)
        
        // Update activate button
        binding.activateButton.isEnabled = state.canActivate && !state.isProcessing
        
        if (state.isActivated) {
            // Show success state with tag number
            binding.activateButton.visibility = View.GONE
            binding.successMessageCard.visibility = View.VISIBLE
            
            // Display generated tag number instead of EPC
            state.activatedTagNumber?.let { tagNumber ->
                binding.activatedTagNumberText.text = "Tag Number: $tagNumber"
                binding.activatedTagNumberText.visibility = View.VISIBLE
            }
        } else {
            binding.activateButton.visibility = View.VISIBLE
            binding.successMessageCard.visibility = View.GONE
            binding.activatedTagNumberText.visibility = View.GONE
        }
        
        // Update processing state
        if (state.isProcessing) {
            binding.activateButton.text = "Activating..."
        } else {
            binding.activateButton.text = "Activate Tag"
        }
    }
    
    /**
     * Update error display with prominent red styling
     * Shows error message card with red background, red dot, and red text
     */
    private fun updateErrorDisplay(state: TagActivationUiState): Unit {
        if (state.errorMessage != null && state.errorMessage.isNotBlank()) {
            // Show error card with prominent styling
            binding.errorMessageCard.visibility = View.VISIBLE
            binding.errorMessageText.text = state.errorMessage
            
            Log.d(TAG, "⚠️ Error displayed: ${state.errorMessage}")
        } else {
            // Hide error card when no error
            binding.errorMessageCard.visibility = View.GONE
        }
    }
    
    /**
     * Update manual mode configuration UI (similar to SingleScan)
     */
    private fun updateManualModeUI(state: TagActivationUiState): Unit {
        // Manual mode removed - only auto-selection now
    }
    
    /**
     * Update tag selection UI for manual mode
     */
    private fun updateTagSelectionUI(candidateTags: List<TagActivationCandidateTag>): Unit {
        // Update count badge
        binding.tagSelectionCount.text = "${candidateTags.size} tag${if (candidateTags.size != 1) "s" else ""}"
        
        // Update adapter
        candidateTagAdapter.updateCandidateTags(candidateTags)
    }
    
    /**
     * Update scanned tag display
     */
    private fun updateScannedTagDisplay(state: TagActivationUiState): Unit {
        val scannedTag = state.scannedTag
        
        if (scannedTag != null && !state.isActivated) {
            // Show EPC data for scanning phase
            binding.scannedEpcText.text = "EPC: ${scannedTag.epc}"
            binding.scannedRssiText.text = scannedTag.getFormattedRssi()
            
            binding.scannedTagCard.visibility = View.VISIBLE
        } else {
            binding.scannedTagCard.visibility = View.GONE
        }
    }
    
    private fun updateScanningStatus(state: TagActivationUiState): Unit {
        // Don't show error messages in status text - they're shown in the error card
        val displayMessage = if (state.errorMessage != null && state.errorMessage.isNotBlank()) {
            // If there's an error, just show a generic scanning status
            "Ready to scan"
        } else {
            state.statusMessage
        }
        
        binding.scanningStatusText.text = displayMessage
        
        // Update status icon based on scanning state
        when (state.scanningStatus) {
            ScanningStatus.READY -> {
                binding.scanningStatusIcon.setImageResource(R.drawable.ic_circle_primary)
                binding.scanningStatusIcon.setColorFilter(
                    requireContext().getColor(R.color.text_secondary)
                )
            }
            ScanningStatus.SCANNING -> {
                binding.scanningStatusIcon.setImageResource(R.drawable.ic_qr_code_scanner)
                binding.scanningStatusIcon.setColorFilter(
                    requireContext().getColor(R.color.primary)
                )
            }
            ScanningStatus.ERROR -> {
                // Don't change to error icon - keep showing normal status since error card handles it
                binding.scanningStatusIcon.setImageResource(R.drawable.ic_circle_primary)
                binding.scanningStatusIcon.setColorFilter(
                    requireContext().getColor(R.color.text_secondary)
                )
            }
        }
        
        // Show snackbar only for success messages, not errors (error card handles errors)
        if (state.statusMessage.contains("successfully")) {
            Snackbar.make(binding.root, state.statusMessage, Snackbar.LENGTH_SHORT).show()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop scanning when fragment is paused
        viewModel.stopScanning()
    }

    /**
     * Handle configuration changes (including language changes)
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        println("TagActivationFragment: Configuration changed - refreshing UI text elements")
        refreshUITextElementsForLanguageChange()
    }

    /**
     * Refresh all UI text elements for language changes
     * The ViewModel already uses localized strings, so we mainly need to refresh UI elements
     */
    private fun refreshUITextElementsForLanguageChange() {
        try {
            println("TagActivationFragment: Starting UI text refresh for language change")
            
            // Force refresh all text elements that use string resources
            updateAllTextViewsWithLocalizedContext(binding.root, requireContext())
            
            // Refresh BC Type dropdown if needed
            // Note: BC Type spinner refresh will be handled by the ViewModel's StateFlow
            // which automatically updates the UI when bcTypeOptions changes
            
            // Refresh activate button text
            binding.activateButton.text = getString(R.string.activate_tag_button)
            
            // Trigger layout refresh
            binding.root.post {
                binding.root.requestLayout()
                binding.root.invalidate()
            }
            
            println("TagActivationFragment: ✅ UI text refresh completed for language change")
            
        } catch (e: Exception) {
            println("TagActivationFragment: ❌ Error in UI text refresh: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Recursively update all TextViews with localized context
     */
    private fun updateAllTextViewsWithLocalizedContext(view: View, localizedContext: Context) {
        try {
            when (view) {
                is ViewGroup -> {
                    for (i in 0 until view.childCount) {
                        updateAllTextViewsWithLocalizedContext(view.getChildAt(i), localizedContext)
                    }
                }
                is TextView -> {
                    // Force TextView to re-resolve any string resources
                    view.invalidate()
                    view.requestLayout()
                }
            }
        } catch (e: Exception) {
            println("TagActivationFragment: Error updating TextViews: ${e.message}")
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
