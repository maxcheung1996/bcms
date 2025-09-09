package com.socam.bcms.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentMainBinding
import kotlinx.coroutines.launch

/**
 * Main screen fragment with module navigation cards
 * Provides access to all BCMS modules and data synchronization
 */
class MainFragment : Fragment() {
    
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(requireContext())
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        
        setupUI()
        setupObservers()
        loadInitialData()
    }
    
    private fun setupUI(): Unit {
        // Module card click listeners
        binding.tagActivationCard.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_tag_activation)
        }
        
        binding.singleScanCard.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_single_scan)
        }
        
        binding.batchProcessCard.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_batch_process)
        }
        
        binding.settingsCard.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_settings)
        }
        
        // Sync button click listener
        binding.syncNowButton.setOnClickListener {
            performDataSync()
        }
    }
    
    private fun setupObservers(): Unit {
        viewModel.userInfo.observe(viewLifecycleOwner) { userInfo ->
            binding.userNameText.text = getString(R.string.welcome_user_format, userInfo.fullName)
        }
        
        viewModel.statsInfo.observe(viewLifecycleOwner) { stats ->
            binding.totalTagsCount.text = stats.totalTags.toString()
            binding.activeTagsCount.text = stats.activeTags.toString()
            binding.pendingSyncCount.text = stats.pendingSync.toString()
        }
        
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
                    // Refresh stats after successful sync
                    loadInitialData()
                }
                is SyncState.Error -> {
                    setSyncLoadingState(false)
                    binding.syncStatusText.text = getString(R.string.sync_status_error)
                    showSyncError(state.message)
                }
            }
        }
    }
    
    private fun loadInitialData(): Unit {
        lifecycleScope.launch {
            viewModel.loadUserInfo()
            viewModel.loadStats()
            viewModel.loadSyncStatus()
        }
    }
    
    private fun performDataSync(): Unit {
        lifecycleScope.launch {
            viewModel.performSync()
        }
    }
    
    private fun setSyncLoadingState(isLoading: Boolean): Unit {
        binding.syncProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.syncNowButton.isEnabled = !isLoading
    }
    
    private fun showSyncSuccess(message: String): Unit {
        Snackbar.make(binding.snackbarAnchor, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.syncSectionCard)
            .show()
    }
    
    private fun showSyncError(message: String): Unit {
        Snackbar.make(binding.snackbarAnchor, "Sync failed: $message", Snackbar.LENGTH_LONG)
            .setAnchorView(binding.syncSectionCard)
            .setAction("Retry") {
                performDataSync()
            }
            .show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            R.id.action_refresh -> {
                loadInitialData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun performLogout(): Unit {
        lifecycleScope.launch {
            viewModel.logout()
            findNavController().navigate(R.id.action_main_to_login)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to main screen
        loadInitialData()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
