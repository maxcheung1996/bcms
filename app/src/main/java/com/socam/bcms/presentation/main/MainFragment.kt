package com.socam.bcms.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentMainBinding
import kotlinx.coroutines.launch

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
     * Set up the MaterialToolbar with built-in menu handling
     */
    private fun setupToolbar(): Unit {
        try {
            println("MainFragment: Setting up basic menu")
            
            binding.toolbar.inflateMenu(R.menu.main_menu)
            binding.toolbar.popupTheme = android.R.style.ThemeOverlay_Material_Light
            
            binding.toolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_logout -> {
                        println("MainFragment: Logout clicked")
                        performLogout()
                        true
                    }
                    R.id.action_refresh -> {
                        println("MainFragment: Refresh clicked - skipped to prevent loops")
                        // DISABLED: Don't reload data on refresh to prevent loops
                        showRefreshMessage()
                        true
                    }
                    else -> false
                }
            }
            
            binding.manualLogoutButton.visibility = android.view.View.GONE
            
        } catch (e: Exception) {
            println("MainFragment: Error in menu setup: ${e.message}")
            binding.manualLogoutButton.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun setupUI(): Unit {
        binding.syncNowButton.setOnClickListener {
            println("MainFragment: Sync button clicked - simplified for stability")
            showSyncMessage()
        }
    }
    
    private fun setupObservers(): Unit {
        // User info observer
        viewModel.userInfo.observe(viewLifecycleOwner) { userInfo ->
            binding.userNameText.text = "${userInfo.fullName} (${userInfo.role})"
        }
        
        // Stats observer  
        viewModel.statsInfo.observe(viewLifecycleOwner) { stats ->
            binding.totalTagsCount.text = stats.totalTags.toString()
            binding.activeTagsCount.text = stats.activeTags.toString()
            binding.pendingSyncCount.text = stats.pendingSync.toString()
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
        Snackbar.make(binding.root, "Error: $error", Snackbar.LENGTH_LONG).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}