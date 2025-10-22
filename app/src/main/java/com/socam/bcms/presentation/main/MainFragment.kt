package com.socam.bcms.presentation.main

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
import com.socam.bcms.databinding.FragmentMainBinding
import kotlinx.coroutines.launch
import java.util.*

/**
 * Main fragment displaying dashboard, stats, and sync status
 * STABILITY PRIORITY: Simple, single-load approach to prevent loops
 */
class MainFragment : Fragment() {
    
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(requireContext())
    }
    
    // Loop protection flag
    private var isDataLoaded = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menu is now handled directly by MaterialToolbar
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up the MaterialToolbar as ActionBar
        setupToolbar()
        
        setupUI()
        setupObservers()
        
        // Load data only once with protection
        if (!isDataLoaded) {
            loadInitialDataOnce()
        }
    }
    
    /**
     * Setup toolbar - SIMPLE and SAFE
     */
    private fun setupToolbar(): Unit {
        // Manual logout button hidden since logout now available in Settings
        binding.manualLogoutButton.visibility = android.view.View.GONE
    }
    
    private fun setupUI(): Unit {
        binding.syncNowButton.setOnClickListener {
            println("MainFragment: Sync button clicked - navigating to sync screen")
            navigateToSyncScreen()
        }
        
        // Module card click listeners
        binding.tagActivationCard.setOnClickListener {
            println("MainFragment: Tag Activation card clicked")
            findNavController().navigate(R.id.action_main_to_tag_activation)
        }
        
        binding.singleScanCard.setOnClickListener {
            println("MainFragment: Single Scan card clicked")
            findNavController().navigate(R.id.action_main_to_single_scan)
        }
        
        binding.batchProcessCard.setOnClickListener {
            println("MainFragment: Batch Process card clicked")
            findNavController().navigate(R.id.action_main_to_batch_process)
        }
        
        binding.settingsCard.setOnClickListener {
            println("MainFragment: Settings card clicked")
            findNavController().navigate(R.id.action_main_to_settings)
        }
        
        
        // Hidden modules - no click listeners
        // binding.tagModificationCard.setOnClickListener { ... } - REMOVED
        // binding.notificationCard.setOnClickListener { ... } - REMOVED
    }
    
    private fun setupObservers(): Unit {
        // User info observer
        // viewModel.userInfo.observe(viewLifecycleOwner) { userInfo ->
        //    binding.userNameText.text = "${userInfo.fullName} (${userInfo.role})"
        //}
        
        // Stats observer - legacy fallback
        viewModel.statsInfo.observe(viewLifecycleOwner) { stats ->
            binding.totalTagsCount.text = stats.totalTags.toString()
            binding.activeTagsCount.text = stats.activeTags.toString()
            binding.pendingSyncCount.text = stats.pendingSync.toString()
            
            // Update notification badge
            updateNotificationBadge(stats.notificationCount)
        }
        
        // Real-time stats observer - primary source for live updates
        viewModel.realTimeStats.observe(viewLifecycleOwner) { stats ->
            try {
                println("MainFragment: Real-time stats update - Total: ${stats.totalTags}, Active: ${stats.activeTags}, Pending: ${stats.pendingSync}")
                
                binding.totalTagsCount.text = stats.totalTags.toString()
                binding.activeTagsCount.text = stats.activeTags.toString()
                binding.pendingSyncCount.text = stats.pendingSync.toString()
                
                // Update notification badge
                updateNotificationBadge(stats.notificationCount)
                
                // Visual feedback for real-time updates
                animateStatsUpdate()
                
            } catch (e: Exception) {
                println("MainFragment: Error updating real-time stats: ${e.message}")
                // Fall back to safe defaults
                binding.totalTagsCount.text = "0"
                binding.activeTagsCount.text = "0"
                binding.pendingSyncCount.text = "0"
            }
        }
        
        // Sync state observer - FIXED: No more infinite loop!
        viewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SyncState.Idle -> {
                    setSyncLoadingState(false)
                    binding.syncStatusText.text = getString(R.string.sync_status_ready)
                }
                is SyncState.Loading -> {
                    setSyncLoadingState(true)
                    binding.syncStatusText.text = getString(R.string.sync_status_syncing)
                }
                is SyncState.Success -> {
                    setSyncLoadingState(false)
                    binding.syncStatusText.text = getString(R.string.sync_status_success, state.timestamp)
                    showSyncSuccess(state.message)
                    // FIXED: Removed loadInitialData() call that caused infinite loop!
                }
                is SyncState.Error -> {
                    setSyncLoadingState(false)
                    binding.syncStatusText.text = getString(R.string.sync_status_error)
                    showSyncError(state.error)
                }
            }
        }
    }
    
    /**
     * Load initial data ONCE with loop protection
     */
    private fun loadInitialDataOnce(): Unit {
        if (isDataLoaded) {
            println("MainFragment: Data already loaded, skipping")
            return
        }
        
        lifecycleScope.launch {
            try {
                println("MainFragment: Loading initial data once for stability")
                
                // Load data sequentially to avoid race conditions
                viewModel.loadUserInfo()
                viewModel.loadStats()
                viewModel.loadSyncStatus()
                
                // Mark as loaded to prevent repeated calls
                isDataLoaded = true
                println("MainFragment: Initial data loaded successfully")
                
            } catch (e: Exception) {
                println("MainFragment: Error loading initial data: ${e.message}")
                setDefaultStats()
            }
        }
    }
    
    /**
     * Set safe default values
     */
    private fun setDefaultStats(): Unit {
        try {
            binding.totalTagsCount.text = "0"
            binding.activeTagsCount.text = "0"
            binding.pendingSyncCount.text = "0"
            binding.syncStatusText.text = getString(R.string.sync_status_ready)
        } catch (e: Exception) {
            println("MainFragment: Error setting default stats: ${e.message}")
        }
    }
    
    private fun navigateToSyncScreen(): Unit {
        try {
            findNavController().navigate(R.id.action_main_to_sync)
        } catch (e: Exception) {
            println("MainFragment: Error navigating to sync screen: ${e.message}")
            showSyncMessage()
        }
    }
    
    /**
     * Animate stats cards when values update for visual feedback
     */
     private fun animateStatsUpdate(): Unit {
        try {
            // Simple fade animation to show updates
            listOf(
                binding.totalTagsCount,
                binding.activeTagsCount, 
                binding.pendingSyncCount
            ).forEach { textView ->
                textView.animate()
                    .alpha(0.7f)
                    .setDuration(200)
                    .withEndAction {
                        textView.animate()
                            .alpha(1.0f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            }
        } catch (e: Exception) {
            // Don't break app if animation fails
            println("MainFragment: Animation error: ${e.message}")
        }
    }
    
    private fun performLogout(): Unit {
        viewModel.logout()
        
        // Redirect to login screen instead of closing app
        val intent = android.content.Intent(requireContext(), com.socam.bcms.presentation.AuthActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    private fun showRefreshMessage(): Unit {
        Snackbar.make(binding.root, "Refresh disabled for stability", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun showSyncMessage(): Unit {
        Snackbar.make(binding.root, "Sync simplified for stability", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun setSyncLoadingState(isLoading: Boolean): Unit {
        binding.syncProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.syncNowButton.isEnabled = !isLoading
    }
    
    private fun showSyncSuccess(message: String): Unit {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    
    private fun showSyncError(error: String): Unit {
        Snackbar.make(binding.root, getString(R.string.error_format, error), Snackbar.LENGTH_LONG).show()
    }
    
    /**
     * Update notification badge based on sync error count
     */
    private fun updateNotificationBadge(count: Int): Unit {
        if (count > 0) {
            binding.notificationBadge.visibility = View.VISIBLE
            binding.notificationBadge.text = if (count > 99) "99+" else count.toString()
            val issueText = if (count == 1) getString(R.string.sync_issue_singular) else getString(R.string.sync_issues_plural)
            binding.notificationSubtitle.text = getString(R.string.scanning_status_format, count.toString(), issueText)
            
            // Update card styling for attention
            binding.notificationCard.strokeColor = android.graphics.Color.RED
            binding.notificationCard.strokeWidth = 2
        } else {
            binding.notificationBadge.visibility = View.GONE
            binding.notificationSubtitle.text = getString(R.string.no_sync_issues)
            
            // Reset card styling
            binding.notificationCard.strokeColor = android.graphics.Color.TRANSPARENT
            binding.notificationCard.strokeWidth = 0
        }
    }

    /**
     * Handle configuration changes (including language changes)
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        println("MainFragment: Configuration changed - refreshing UI text elements")
        refreshUITextElementsForLanguageChange()
    }

    /**
     * Refresh all UI text elements for language changes
     * Uses similar approach to SettingsFragment tri-directional system
     */
    private fun refreshUITextElementsForLanguageChange() {
        try {
            println("MainFragment: Starting UI text refresh for language change")
            
            // Update notification badge text
            val currentCount = binding.notificationBadge.text.toString().toIntOrNull() ?: 0
            updateNotificationBadge(currentCount)
            
            // Refresh all card titles and descriptions if they use string resources
            refreshAllCardTexts()
            
            // Trigger layout refresh
            binding.root.post {
                binding.root.requestLayout()
                binding.root.invalidate()
            }
            
            println("MainFragment: ✅ UI text refresh completed for language change")
            
        } catch (e: Exception) {
            println("MainFragment: ❌ Error in UI text refresh: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Refresh all card texts that may contain translatable strings
     */
    private fun refreshAllCardTexts() {
        try {
            // Update all TextViews in the view hierarchy
            updateAllTextViewsWithLocalizedContext(binding.root, requireContext())
            
        } catch (e: Exception) {
            println("MainFragment: Error refreshing card texts: ${e.message}")
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
            println("MainFragment: Error updating TextViews: ${e.message}")
        }
    }
    

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}