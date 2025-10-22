package com.socam.bcms.presentation.sync

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentSyncBinding
import kotlinx.coroutines.launch
import java.util.*

/**
 * SyncFragment - Data synchronization screen
 * STABILITY PRIORITY: Simple, reliable sync operations
 */
class SyncFragment : Fragment() {
    
    private var _binding: FragmentSyncBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SyncViewModel by viewModels {
        SyncViewModelFactory(requireContext())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSyncBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupClickListeners()
        setupObservers()
        loadDataCounts()
    }
    
    private fun setupToolbar(): Unit {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
    
    private fun setupClickListeners(): Unit {
        // MIC Dataset Sync
        binding.syncMicCard.setOnClickListener {
            if (viewModel.isSyncing.value != true) {
                showSyncConfirmation("MIC Components") {
                    viewModel.syncComponentData("MIC")
                }
            }
        }
        
        // ALW Dataset Sync
        binding.syncAlwCard.setOnClickListener {
            if (viewModel.isSyncing.value != true) {
                showSyncConfirmation("ALW Components") {
                    viewModel.syncComponentData("ALW")
                }
            }
        }
        
        // TID Dataset Sync
        binding.syncTidCard.setOnClickListener {
            if (viewModel.isSyncing.value != true) {
                showSyncConfirmation("TID Components") {
                    viewModel.syncComponentData("TID")
                }
            }
        }
        
        // Master Data Sync
        binding.syncMasterCard.setOnClickListener {
            if (viewModel.isSyncing.value != true) {
                showSyncConfirmation("Master Data (5 datasets)") {
                    viewModel.syncMasterData()
                }
            }
        }
    }
    
    private fun setupObservers(): Unit {
        // Sync state observer
        viewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SyncState.Idle -> {
                    hideSyncProgress()
                    binding.syncStatusText.text = getString(R.string.ready_to_sync)
                }
                is SyncState.Loading -> {
                    showSyncProgress(state.message)
                    enableCards(false)
                }
                is SyncState.Success -> {
                    hideSyncProgress()
                    binding.syncStatusText.text = state.message
                    showSyncSuccess(state.message)
                    enableCards(true)
                    loadDataCounts() // Refresh counts
                }
                is SyncState.Error -> {
                    hideSyncProgress()
                    binding.syncStatusText.text = getString(R.string.sync_failed_format, state.error)
                    showSyncError(state.error)
                    enableCards(true)
                }
            }
        }
        
        // Data counts observer
        viewModel.dataCounts.observe(viewLifecycleOwner) { counts ->
            binding.micCountText.text = getString(R.string.records_count_format, counts.micCount)
            binding.alwCountText.text = getString(R.string.records_count_format, counts.alwCount)
            binding.tidCountText.text = getString(R.string.records_count_format, counts.tidCount)
            binding.masterCountText.text = getString(R.string.datasets_records_format, counts.totalMasterRecords)
        }
    }
    
    private fun loadDataCounts(): Unit {
        lifecycleScope.launch {
            viewModel.loadDataCounts()
        }
    }
    
    private fun showSyncConfirmation(datasetName: String, onConfirm: () -> Unit): Unit {
        // Simple confirmation using Snackbar with action
        Snackbar.make(binding.root, "Sync $datasetName?", Snackbar.LENGTH_LONG)
            .setAction("SYNC") { onConfirm() }
            .show()
    }
    
    private fun showSyncProgress(message: String): Unit {
        binding.syncProgress.visibility = View.VISIBLE
        binding.syncProgressText.visibility = View.VISIBLE
        binding.syncProgressText.text = message
    }
    
    private fun hideSyncProgress(): Unit {
        binding.syncProgress.visibility = View.GONE
        binding.syncProgressText.visibility = View.GONE
    }
    
    private fun enableCards(enabled: Boolean): Unit {
        binding.syncMicCard.isClickable = enabled
        binding.syncAlwCard.isClickable = enabled
        binding.syncTidCard.isClickable = enabled
        binding.syncMasterCard.isClickable = enabled
        
        val alpha = if (enabled) 1.0f else 0.6f
        binding.syncMicCard.alpha = alpha
        binding.syncAlwCard.alpha = alpha
        binding.syncTidCard.alpha = alpha
        binding.syncMasterCard.alpha = alpha
    }
    
    private fun showSyncSuccess(message: String): Unit {
        Snackbar.make(binding.root, "✅ $message", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun showSyncError(error: String): Unit {
        Snackbar.make(binding.root, "❌ Error: $error", Snackbar.LENGTH_LONG).show()
    }

    /**
     * Handle configuration changes (including language changes)
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        println("SyncFragment: Configuration changed - refreshing UI text elements")
        refreshUITextElementsForLanguageChange()
    }

    /**
     * Refresh all UI text elements for language changes
     * Uses tri-directional system similar to SettingsFragment
     */
    private fun refreshUITextElementsForLanguageChange() {
        try {
            println("SyncFragment: Starting UI text refresh for language change")
            
            // Force refresh all text elements that use string resources
            updateAllTextViewsWithLocalizedContext(binding.root, requireContext())
            
            // Re-trigger data count observer to refresh the count displays
            viewModel.dataCounts.value?.let { counts ->
                binding.micCountText.text = getString(R.string.records_count_format, counts.micCount)
                binding.alwCountText.text = getString(R.string.records_count_format, counts.alwCount)
                binding.tidCountText.text = getString(R.string.records_count_format, counts.tidCount)
                binding.masterCountText.text = getString(R.string.datasets_records_format, counts.totalMasterRecords)
            }
            
            // Update sync status text if needed
            binding.syncStatusText.text = getString(R.string.ready_to_sync)
            
            // Trigger layout refresh
            binding.root.post {
                binding.root.requestLayout()
                binding.root.invalidate()
            }
            
            println("SyncFragment: ✅ UI text refresh completed for language change")
            
        } catch (e: Exception) {
            println("SyncFragment: ❌ Error in UI text refresh: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Recursively update all TextViews with localized context
     * Similar to SettingsFragment approach
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
                    // Force TextView to re-resolve any string resources it might be using
                    view.invalidate()
                    view.requestLayout()
                }
            }
        } catch (e: Exception) {
            println("SyncFragment: Error updating TextViews: ${e.message}")
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
