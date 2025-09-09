package com.socam.bcms.presentation.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentLoginBinding
import com.socam.bcms.presentation.AuthActivity
import kotlinx.coroutines.launch

/**
 * Login fragment with authentication UI
 * Provides username/password login with local validation
 */
class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(requireContext())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        loadVersionInfo()
    }
    
    private fun setupUI(): Unit {
        binding.loginButton.setOnClickListener {
            performLogin()
        }
        
        // Set default demo credentials for testing
        binding.usernameEditText.setText("demo")
        binding.passwordEditText.setText("password")
    }
    
    private fun setupObservers(): Unit {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Loading -> {
                    setLoadingState(true)
                }
                is LoginState.Success -> {
                    setLoadingState(false)
                    navigateToMain()
                }
                is LoginState.Error -> {
                    setLoadingState(false)
                    showError(state.message)
                }
                is LoginState.Idle -> {
                    setLoadingState(false)
                }
            }
        }
        
        viewModel.environmentInfo.observe(viewLifecycleOwner) { envInfo ->
            updateEnvironmentIndicator(envInfo)
        }
    }
    
    private fun performLogin(): Unit {
        val username = binding.usernameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        
        // Clear any previous errors
        binding.usernameLayout.error = null
        binding.passwordLayout.error = null
        
        // Validate input
        var hasError = false
        
        if (username.isEmpty()) {
            binding.usernameLayout.error = getString(R.string.error_username_required)
            hasError = true
        }
        
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.error_password_required)
            hasError = true
        }
        
        if (!hasError) {
            lifecycleScope.launch {
                viewModel.login(username, password)
            }
        }
    }
    
    private fun setLoadingState(isLoading: Boolean): Unit {
        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !isLoading
        binding.usernameEditText.isEnabled = !isLoading
        binding.passwordEditText.isEnabled = !isLoading
    }
    
    private fun showError(message: String): Unit {
        Snackbar.make(binding.snackbarAnchor, message, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.loginCard)
            .show()
    }
    
    private fun navigateToMain(): Unit {
        (requireActivity() as? AuthActivity)?.onLoginSuccess()
    }
    
    private fun loadVersionInfo(): Unit {
        lifecycleScope.launch {
            viewModel.loadAppInfo()
        }
    }
    
    private fun updateEnvironmentIndicator(envInfo: EnvironmentInfo): Unit {
        binding.versionText.text = getString(R.string.version_format, envInfo.version)
        binding.environmentText.text = envInfo.environmentName.uppercase()
        
        // Update environment dot color based on environment
        val dotColor = if (envInfo.environmentName == "dev") {
            R.color.environment_dev
        } else {
            R.color.environment_prod
        }
        binding.environmentDot.setBackgroundResource(dotColor)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
