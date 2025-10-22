package com.socam.bcms.presentation.modules

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentTagModificationBinding
import com.socam.bcms.model.ScanMode
import com.socam.bcms.model.TagModificationData
import com.socam.bcms.model.TagModificationUiState
import com.socam.bcms.model.TagStatus
import com.socam.bcms.model.TagStatusOption
import com.socam.bcms.model.TagStatusOptions
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Tag Modification Fragment
 * 
 * Features:
 * - UHF power control (5-33 dBm) with real-time slider
 * - Press-and-hold trigger scanning (start on press, stop on release)
 * - Display RSSI, TID, EPC, and EPC status data
 * - Strongest signal filtering (closest tag only)
 */
class TagModificationFragment : Fragment() {
    
    private var _binding: FragmentTagModificationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TagModificationViewModel by viewModels()
    private lateinit var multipleScanAdapter: MultipleScanTagAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTagModificationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupUI()
        setupTabLayout()
        setupRecyclerView()
        setupObservers()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Set focus for key event handling when fragment becomes visible
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
     * Handle key down events for physical scanner trigger
     * Press-and-hold behavior: Start scanning on press
     */
    private fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Check for RFID scanner trigger keys
        when (keyCode) {
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                println("TagModificationFragment: Physical trigger pressed - keyCode: $keyCode")
                if (!event.isLongPress && event.repeatCount == 0) {
                    // First press - start scanning
                    viewModel.startScanning()
                }
                return true
            }
        }
        return false
    }
    
    /**
     * Handle key up events for physical scanner trigger
     * Press-and-hold behavior: Stop scanning on release
     */
    private fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Check for RFID scanner trigger keys
        when (keyCode) {
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                println("TagModificationFragment: Physical trigger released - keyCode: $keyCode")
                // Release - stop scanning
                viewModel.stopScanning()
                return true
            }
        }
        return false
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupUI() {
        // Power slider setup
        binding.powerSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Optional: Handle start of slider interaction
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Update power when user stops dragging
                viewModel.setPowerLevel(slider.value.toInt())
            }
        })
        
        binding.powerSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                // Update power display in real-time
                binding.powerValueText.text = "${value.toInt()} dBm"
            }
        }
        
        // Status dropdown setup
        setupStatusDropdown()
        
        // Write button setup
        binding.writeButton.setOnClickListener {
            viewModel.writeTagStatus()
        }
        
        // Filter checkbox setup
        setupFilterCheckboxes()
    }
    
    private fun setupStatusDropdown() {
        // Create adapter with status options
        val statusOptions = TagStatusOptions.OPTIONS
        val displayNames = statusOptions.map { "${it.displayName} (${it.hexValue})" }
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            displayNames
        )
        
        binding.statusDropdown.setAdapter(adapter)
        
        // Handle selection
        binding.statusDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedOption = statusOptions[position]
            viewModel.setSelectedStatusOption(selectedOption)
            println("TagModificationFragment: Selected status: ${selectedOption.displayName}")
        }
    }
    
    private fun setupFilterCheckboxes() {
        // Active filter checkbox
        binding.activeFilterCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setActiveFilter(isChecked)
            println("TagModificationFragment: Active filter set to: $isChecked")
        }
        
        // Inactive filter checkbox
        binding.inactiveFilterCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setInactiveFilter(isChecked)
            println("TagModificationFragment: Inactive filter set to: $isChecked")
        }
    }

    /**
     * Setup TabLayout for scan mode selection
     */
    private fun setupTabLayout() {
        binding.scanModeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        viewModel.setScanMode(ScanMode.SINGLE)
                        showSingleScanContent()
                        println("TagModificationFragment: Switched to Single Scan mode")
                    }
                    1 -> {
                        viewModel.setScanMode(ScanMode.MULTIPLE)
                        showMultipleScanContent()
                        println("TagModificationFragment: Switched to Multiple Scan mode")
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        // Default to Single Scan tab
        binding.scanModeTabs.selectTab(binding.scanModeTabs.getTabAt(0))
    }

    /**
     * Setup RecyclerView for Multiple Scan mode
     */
    private fun setupRecyclerView() {
        multipleScanAdapter = MultipleScanTagAdapter()
        
        binding.multipleScanRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = multipleScanAdapter
        }
    }

    /**
     * Show Single Scan mode content and hide Multiple Scan content
     */
    private fun showSingleScanContent() {
        binding.singleScanContent.visibility = View.VISIBLE
        binding.multipleScanContent.visibility = View.GONE
    }

    /**
     * Show Multiple Scan mode content and hide Single Scan content
     */
    private fun showMultipleScanContent() {
        binding.singleScanContent.visibility = View.GONE
        binding.multipleScanContent.visibility = View.VISIBLE
    }
    
    private fun setupObservers() {
        viewModel.uiState.onEach { state ->
            updateUI(state)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
    
    private fun updateUI(state: TagModificationUiState) {
        // Update power level display
        binding.powerValueText.text = "${state.powerLevel} dBm"
        binding.powerSlider.value = state.powerLevel.toFloat()
        
        // Update scan mode content visibility and status messages
        when (state.currentScanMode) {
            ScanMode.SINGLE -> {
                showSingleScanContent()
                binding.statusMessage.text = state.statusMessage
                
                // Update Single Scan tab selection without triggering listener
                if (binding.scanModeTabs.selectedTabPosition != 0) {
                    binding.scanModeTabs.selectTab(binding.scanModeTabs.getTabAt(0))
                }
            }
            ScanMode.MULTIPLE -> {
                showMultipleScanContent()
                binding.multipleScanStatusMessage.text = state.statusMessage
                
                // Update Multiple Scan tab selection without triggering listener
                if (binding.scanModeTabs.selectedTabPosition != 1) {
                    binding.scanModeTabs.selectTab(binding.scanModeTabs.getTabAt(1))
                }
                
                // Update Multiple Scan RecyclerView and empty state
                updateMultipleScanResults(state)
            }
        }
        
        // Update write button state
        binding.writeButton.isEnabled = state.canWrite
        
        // Update filter checkboxes (prevent listener triggers during update)
        binding.activeFilterCheckbox.setOnCheckedChangeListener(null)
        binding.inactiveFilterCheckbox.setOnCheckedChangeListener(null)
        binding.activeFilterCheckbox.isChecked = state.showActiveFilter
        binding.inactiveFilterCheckbox.isChecked = state.showInactiveFilter
        setupFilterCheckboxes() // Re-attach listeners
        
        // Show loading indicator during write operation
        if (state.isWriting) {
            // You can add a progress indicator in the write button if needed
            binding.writeButton.text = ""
        }
        
        // Show/hide scan results (only for Single Scan mode)
        if (state.currentScanMode == ScanMode.SINGLE) {
            if (state.lastScanResult != null) {
                binding.scanResultsCard.visibility = View.VISIBLE
                updateScanResults(state.lastScanResult)
                updateStatusBadge(state.lastScanResult)
            } else {
                binding.scanResultsCard.visibility = View.GONE
                binding.statusBadge.visibility = View.GONE
            }
        } else {
            // Hide Single Scan results when in Multiple Scan mode
            binding.scanResultsCard.visibility = View.GONE
            binding.statusBadge.visibility = View.GONE
        }
        
        // Handle error messages (including success messages via error channel)
        state.errorMessage?.let { message ->
            when {
                message.startsWith("WRITE_SUCCESS:") -> {
                    val successMsg = message.substringAfter("WRITE_SUCCESS:")
                    Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show()
                }
                message.startsWith("WRITE_ERROR:") -> {
                    val errorMsg = message.substringAfter("WRITE_ERROR:")
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
                else -> {
                    println("TagModificationFragment: Error - $message")
                }
            }
            // Clear error after displaying
            viewModel.clearError()
        }
        
        // CRITICAL: Restore focus after write operations to fix trigger unresponsiveness
        if (state.needsFocusRestore) {
            println("TagModificationFragment: Restoring focus for trigger detection after write operation")
            view?.requestFocus()
            viewModel.clearFocusRestoreFlag()
        }
    }
    
    private fun updateScanResults(scanResult: TagModificationData) {
        // Update RSSI (both raw and dBm)
        binding.rssiRawText.text = scanResult.rssiRaw
        binding.rssiDbmText.text = "${scanResult.rssiDbm}"
        
        // Update TID
        binding.tidText.text = scanResult.tid
        
        // Update EPC
        binding.epcText.text = scanResult.epc
        
        // Update EPC status data
        binding.userText.text = scanResult.getFormattedEpcData()
    }
    
    private fun updateStatusBadge(scanResult: TagModificationData) {
        val statusInfo = scanResult.getStatusDisplayInfo()
        
        binding.statusBadge.apply {
            text = statusInfo.displayName
            visibility = View.VISIBLE
            
            // Set background color based on status
            val color = when (scanResult.getTagStatus()) {
                TagStatus.ACTIVE -> requireContext().getColor(android.R.color.holo_green_dark)
                TagStatus.INACTIVE -> requireContext().getColor(android.R.color.holo_red_dark)
            }
            backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        }
        
        println("TagModificationFragment: Status badge updated to: ${statusInfo.displayName}")
    }

    /**
     * Update Multiple Scan RecyclerView and empty state
     */
    private fun updateMultipleScanResults(state: TagModificationUiState) {
        val tagList = state.multipleScanResults
        
        if (tagList.isEmpty()) {
            // Show empty state
            binding.multipleScanRecyclerView.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
            binding.tagCountText.text = "0 tags"
        } else {
            // Show RecyclerView with data
            binding.multipleScanRecyclerView.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            binding.tagCountText.text = "${tagList.size} ${if (tagList.size == 1) "tag" else "tags"}"
            
            // Update adapter with real-time highlighting/sorting
            multipleScanAdapter.updateTags(tagList)
            
            println("TagModificationFragment: Updated Multiple Scan list - ${tagList.size} tags")
        }
    }
}
