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
import com.socam.bcms.utils.LocaleHelper
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
        
        // Show DEVELOPMENT badge immediately - SIMPLE approach
        showDevelopmentBadge()
        
        // Load version info in background to avoid blocking startup
        loadVersionInfoAsync()
    }
    
    /**
     * Handle configuration changes (like language changes) to refresh UI smoothly
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        println("LoginFragment: onConfigurationChanged() - Language change detected")
        
        try {
            // Refresh login form labels with new language
            refreshLoginFormLabels()
        } catch (e: Exception) {
            println("LoginFragment: Error in onConfigurationChanged: ${e.message}")
        }
    }
    
    private fun setupUI(): Unit {
        binding.loginButton.setOnClickListener {
            performLogin()
        }
        
        // Language switching buttons
        binding.languageEnButton.setOnClickListener {
            changeLanguage("en")
        }
        
        binding.languageTcButton.setOnClickListener {
            changeLanguage("tc")
        }
        
        binding.languageCnButton.setOnClickListener {
            changeLanguage("cn")
        }
        
        // Set default client_user credentials for testing
        binding.usernameEditText.setText("client_user")
        binding.passwordEditText.setText("Abcd.1234")
        
        // Update current language button state
        updateLanguageButtonState(getCurrentLanguage())
    }
    
    private fun setupObservers(): Unit {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            println("LoginFragment: Received state change: $state")
            when (state) {
                is LoginState.Loading -> {
                    println("LoginFragment: Setting loading state")
                    setLoadingState(true)
                }
                is LoginState.Success -> {
                    println("LoginFragment: Login successful! Navigating to main for user: ${state.user.username}")
                    setLoadingState(false)
                    navigateToMain()
                }
                is LoginState.Error -> {
                    println("LoginFragment: Login error: ${state.message}")
                    setLoadingState(false)
                    showError(state.message)
                }
                is LoginState.Idle -> {
                    println("LoginFragment: Login state is idle")
                    setLoadingState(false)
                }
            }
        }
        
        viewModel.environmentInfo.observe(viewLifecycleOwner) { envInfo ->
            updateEnvironmentIndicator(envInfo)
            // Update app version display
            binding.appVersion.text = "Version ${envInfo.version}"
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
            viewModel.login(username, password)
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
        println("LoginFragment: navigateToMain() called")
        val authActivity = requireActivity() as? AuthActivity
        println("LoginFragment: AuthActivity instance: $authActivity")
        
        if (authActivity != null) {
            // We're in AuthActivity - use the proper flow
            authActivity.onLoginSuccess()
            println("LoginFragment: onLoginSuccess() called on AuthActivity")
        } else {
            // Fallback: We're in MainActivity (shouldn't happen with the fix, but just in case)
            println("LoginFragment: Fallback - LoginFragment is in MainActivity, redirecting to MainActivity directly")
            val intent = android.content.Intent(requireContext(), com.socam.bcms.MainActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
    
    private fun loadVersionInfoAsync(): Unit {
        // Don't block startup - load version info asynchronously after a delay
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1000) // Wait 1 second to avoid blocking startup
            try {
                viewModel.loadAppInfo()
            } catch (e: Exception) {
                // Silently fail - version info is not critical for login
                println("LoginFragment: Failed to load version info: ${e.message}")
            }
        }
    }
    
    /**
     * Change application language with smooth transition (no flicker)
     */
    private fun changeLanguage(languageCode: String): Unit {
        try {
            // Update button state immediately for visual feedback
            updateLanguageButtonState(languageCode)
            
            // Show changing message
            showLanguageChangeMessage(languageCode)
            
            // Apply language using smooth LocaleHelper method
            activity?.let { activity ->
                LocaleHelper.applyLanguageToActivity(activity, languageCode)
                
                // Refresh login form labels manually for immediate feedback
                view?.post {
                    refreshLoginFormLabels()
                }
            }
        } catch (e: Exception) {
            println("LoginFragment: Error changing language: ${e.message}")
        }
    }
    
    /**
     * Manually refresh login form labels after language change
     */
    private fun refreshLoginFormLabels(): Unit {
        try {
            // Update username and password field labels
            binding.usernameLayout.hint = getString(R.string.username_hint)
            binding.passwordLayout.hint = getString(R.string.password_hint)
            
            // Update login button text
            binding.loginButton.text = getString(R.string.login_button_text)
            
            // Update any error messages that might be shown
            binding.usernameLayout.error = null
            binding.passwordLayout.error = null
            
            println("LoginFragment: Form labels refreshed successfully")
        } catch (e: Exception) {
            println("LoginFragment: Error refreshing form labels: ${e.message}")
        }
    }
    
    /**
     * Update language button visual state
     */
    private fun updateLanguageButtonState(currentLanguage: String): Unit {
        // Reset all buttons to default state
        binding.languageEnButton.isSelected = false
        binding.languageTcButton.isSelected = false
        binding.languageCnButton.isSelected = false
        
        // Highlight current language button
        when (currentLanguage) {
            "en" -> binding.languageEnButton.isSelected = true
            "tc" -> binding.languageTcButton.isSelected = true
            "cn" -> binding.languageCnButton.isSelected = true
        }
    }
    
    /**
     * Get current language from LocaleHelper
     */
    private fun getCurrentLanguage(): String {
        return try {
            LocaleHelper.getLanguageFromLocale(requireContext())
        } catch (e: Exception) {
            "en" // Default to English
        }
    }
    
    /**
     * Show language change confirmation
     */
    private fun showLanguageChangeMessage(languageCode: String): Unit {
        val languageName = when (languageCode) {
            "en" -> "English"
            "tc" -> "繁體中文"
            "cn" -> "简体中文"
            else -> "English"
        }
        
        Snackbar.make(binding.root, "Language: $languageName", Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Show DEVELOPMENT badge immediately - SIMPLE and SAFE approach
     */
    private fun showDevelopmentBadge(): Unit {
        try {
            // Always show DEVELOPMENT for now - simple and safe
            binding.environmentText.text = "DEVELOPMENT"
            binding.environmentDot.setBackgroundResource(R.color.environment_dev)
            binding.appVersion.text = "Version 1.0.0"
            
            println("LoginFragment: ✅ DEVELOPMENT badge set!")
            println("LoginFragment: Badge text: '${binding.environmentText.text}'")
            println("LoginFragment: Environment badge is now in the header section!")
            
            // Make badge visible
            binding.environmentLayout.visibility = android.view.View.VISIBLE
            
        } catch (e: Exception) {
            println("LoginFragment: ❌ Error showing DEVELOPMENT badge: ${e.message}")
        }
    }

    private fun updateEnvironmentIndicator(envInfo: EnvironmentInfo): Unit {
        try {
            binding.environmentText.text = envInfo.environmentName.uppercase()
            
            // Update environment dot color based on environment
            val dotColor = if (envInfo.environmentName == "dev" || envInfo.environmentName == "development") {
                R.color.environment_dev
            } else {
                R.color.environment_prod
            }
            binding.environmentDot.setBackgroundResource(dotColor)
        } catch (e: Exception) {
            // Ignore errors in environment update - not critical
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
