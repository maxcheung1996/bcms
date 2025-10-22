package com.socam.bcms.presentation.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentNotificationsBinding
import com.socam.bcms.presentation.sync.SyncViewModel
import com.socam.bcms.presentation.sync.SyncViewModelFactory

/**
 * NotificationFragment - Displays sync error notifications
 * Allows users to view and clear sync errors from data synchronization operations
 */
class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NotificationViewModel
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Load sync errors
        viewModel.loadSyncErrors()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupViewModel() {
        // Create SyncViewModel first (needed as dependency)
        val syncViewModelFactory = SyncViewModelFactory(requireContext())
        val syncViewModel = ViewModelProvider(this, syncViewModelFactory)[SyncViewModel::class.java]

        // Create NotificationViewModel
        val factory = NotificationViewModelFactory(requireContext(), syncViewModel)
        viewModel = ViewModelProvider(this, factory)[NotificationViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { recordId ->
            viewModel.clearError(recordId)
        }

        binding.recyclerViewErrors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationFragment.adapter
        }
    }

    private fun setupObservers() {
        // Sync errors
        viewModel.syncErrors.observe(viewLifecycleOwner) { errors ->
            updateErrorsList(errors)
        }

        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }

        // Error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonClearAll.setOnClickListener {
            showClearAllConfirmation()
        }
    }

    private fun updateErrorsList(errors: List<SyncErrorDisplayItem>) {
        adapter.submitList(errors)
        
        val errorCount = errors.size
        binding.textErrorCount.text = if (errorCount == 0) {
            "No sync errors found"
        } else {
            "$errorCount sync ${if (errorCount == 1) "error" else "errors"} found"
        }

        // Show/hide appropriate views
        when {
            errorCount == 0 -> {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerViewErrors.visibility = View.GONE
                binding.buttonClearAll.visibility = View.GONE
            }
            else -> {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerViewErrors.visibility = View.VISIBLE
                binding.buttonClearAll.visibility = View.VISIBLE
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewErrors.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
    }

    private fun showClearAllConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear All Notifications")
            .setMessage("Are you sure you want to clear all sync error notifications? This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                viewModel.clearAllErrors()
                Toast.makeText(requireContext(), "All notifications cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
