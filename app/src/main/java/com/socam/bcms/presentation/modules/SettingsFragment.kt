package com.socam.bcms.presentation.modules

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.socam.bcms.R
import com.socam.bcms.databinding.FragmentSettingsBinding
import com.socam.bcms.utils.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SettingsFragment - PERFORMANCE OPTIMIZED settings screen
 * Eliminates main thread blocking through lazy loading and cached view references
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Lazy ViewModel creation - truly lazy, only accessed when needed
    private val viewModel: SettingsViewModel by lazy {
        SettingsViewModelFactory(requireContext().applicationContext).create(SettingsViewModel::class.java)
    }
    
    // Flag to prevent language change loops
    private var isLanguageChangeInProgress = false
    
    // Simple approach - no complex caching needed

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        println("SettingsFragment: onCreateView() START - ${System.currentTimeMillis()}")
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        println("SettingsFragment: onCreateView() END - binding inflated - ${System.currentTimeMillis()}")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("SettingsFragment: onViewCreated() START - ${System.currentTimeMillis()}")
        super.onViewCreated(view, savedInstanceState)

        // MINIMAL VERSION: Start with just toolbar and basic user display
        println("SettingsFragment: Setting up toolbar - ${System.currentTimeMillis()}")
        setupToolbar()
        println("SettingsFragment: Toolbar setup complete - ${System.currentTimeMillis()}")
        
        // STEP 1: Complete Settings UI using simple, direct approach
        setupCompleteSettingsUI()
        
        println("SettingsFragment: onViewCreated() END - ${System.currentTimeMillis()}")
    }
    
    /**
     * Handle configuration changes (like language changes) to refresh UI smoothly
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        println("SettingsFragment: onConfigurationChanged() - Language change detected")
        
        try {
            // Refresh all UI text elements with new language
            refreshUITextElements()
            
            // Force refresh of any dynamic content that might be cached
            view?.post {
                // Re-trigger observers to ensure all text is updated
                viewModel.currentLanguage.value?.let { currentLang ->
                    println("SettingsFragment: Refreshing UI for language: $currentLang")
                }
            }
        } catch (e: Exception) {
            println("SettingsFragment: Error in onConfigurationChanged: ${e.message}")
        }
    }

    /**
     * Setup toolbar with back navigation - immediate, non-blocking
     */
    private fun setupToolbar(): Unit {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * COMPLETE SETTINGS UI: All features using simple, direct approach (no complex caching)
     */
    private fun setupCompleteSettingsUI(): Unit {
        println("SettingsFragment: setupCompleteSettingsUI() START - ${System.currentTimeMillis()}")
        
        try {
            // Direct findViewById calls for all UI elements (simple approach)
            println("SettingsFragment: Finding all UI elements - ${System.currentTimeMillis()}")
            val usernameField = binding.root.findViewById<TextView>(R.id.username_value)
            val fullNameField = binding.root.findViewById<TextView>(R.id.full_name_value)
            val emailField = binding.root.findViewById<TextView>(R.id.email_value)
            val departmentField = binding.root.findViewById<TextView>(R.id.department_value)
            val roleField = binding.root.findViewById<TextView>(R.id.role_value)
            
            val projectNameField = binding.root.findViewById<TextView>(R.id.project_name_value)
            val contractNumberField = binding.root.findViewById<TextView>(R.id.contract_number_value)
            val projectDescField = binding.root.findViewById<TextView>(R.id.project_description_value)
            
            val languageToggleGroup = binding.root.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.language_toggle_group)
            val powerSlider = binding.root.findViewById<com.google.android.material.slider.Slider>(R.id.power_slider)
            val powerValueText = binding.root.findViewById<TextView>(R.id.power_value_text)
            
            // Tag Number Configuration elements
            val prefixInput = binding.root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.prefix_input)
            val prefixInputLayout = binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.prefix_input_layout)
            val tagContractInput = binding.root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tag_contract_input)
            val tagContractInputLayout = binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tag_contract_input_layout)
            val reservedInput = binding.root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.reserved_input)
            val reservedInputLayout = binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.reserved_input_layout)
            val tagPreviewText = binding.root.findViewById<TextView>(R.id.tag_preview_text)
            val saveTagConfigButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.save_tag_config_button)
            
            val appVersionField = binding.root.findViewById<TextView>(R.id.app_version_value)
            val apiEndpointField = binding.root.findViewById<TextView>(R.id.api_endpoint_value)
            val logoutButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.logout_button)
            
            println("SettingsFragment: UI elements found - ${System.currentTimeMillis()}")
            
            // Set initial loading state
            usernameField?.text = "Loading..."
            fullNameField?.text = "Loading..."
            projectNameField?.text = "Loading..."
            
            // Get ViewModel (simple access)
            println("SettingsFragment: Accessing ViewModel - ${System.currentTimeMillis()}")
            val vm = viewModel
            println("SettingsFragment: ViewModel created - ${System.currentTimeMillis()}")
            
            // Setup observers directly (simple approach)
            println("SettingsFragment: Setting up all observers - ${System.currentTimeMillis()}")
            
            // User details observer
            vm.userDetails.observe(viewLifecycleOwner) { userDetails ->
                println("SettingsFragment: User details received - ${System.currentTimeMillis()}")
                userDetails?.let {
                    usernameField?.text = it.username
                    fullNameField?.text = it.fullName
                    emailField?.text = it.email
                    departmentField?.text = it.department
                    roleField?.text = it.role
                }
            }
            
            // Project details observer  
            vm.projectDetails.observe(viewLifecycleOwner) { projectDetails ->
                println("SettingsFragment: Project details received - ${System.currentTimeMillis()}")
                projectDetails?.let {
                    projectNameField?.text = it.projectName
                    contractNumberField?.text = it.contractNumber
                    projectDescField?.text = it.projectDescription
                }
            }
            
            // App configuration observer
            vm.appConfiguration.observe(viewLifecycleOwner) { appConfig ->
                println("SettingsFragment: App config received - ${System.currentTimeMillis()}")
                appConfig?.let {
                    appVersionField?.text = it.appVersion
                    apiEndpointField?.text = formatApiEndpoint(it.apiEndpoint)
                }
            }
            
            // Language observer - FIXED: Prevent infinite loop by temporarily removing listener
            vm.currentLanguage.observe(viewLifecycleOwner) { language ->
                println("SettingsFragment: Language received: $language - ${System.currentTimeMillis()}")
                language?.let {
                    val buttonId = when (it) {
                        "en" -> R.id.language_en_button
                        "tc" -> R.id.language_tc_button  
                        "cn" -> R.id.language_cn_button
                        else -> R.id.language_en_button
                    }
                    
                    // CRITICAL FIX: Remove listener temporarily to prevent infinite loop
                    languageToggleGroup?.clearOnButtonCheckedListeners()
                    languageToggleGroup?.clearChecked()
                    languageToggleGroup?.check(buttonId)
                    
                    // Re-add listener after setting value
                    setupLanguageListener(languageToggleGroup, vm)
                }
            }
            
            // UHF Power observer
            vm.uhfPowerLevel.observe(viewLifecycleOwner) { powerLevel ->
                println("SettingsFragment: Power level received: $powerLevel - ${System.currentTimeMillis()}")
                powerLevel?.let {
                    powerSlider?.value = it.toFloat()
                    powerValueText?.text = getString(R.string.power_value_format, it)
                }
            }
            
            // Tag Configuration observers
            vm.tagPrefix.observe(viewLifecycleOwner) { prefix ->
                println("SettingsFragment: Tag prefix received: $prefix - ${System.currentTimeMillis()}")
                prefix?.let { 
                    prefixInput?.setText(it)
                    updateTagPreview(vm, prefixInput, tagContractInput, reservedInput, tagPreviewText)
                }
            }
            
            vm.tagContractNo.observe(viewLifecycleOwner) { tagContract ->
                println("SettingsFragment: Tag contract received: $tagContract - ${System.currentTimeMillis()}")
                tagContract?.let { 
                    tagContractInput?.setText(it)
                    updateTagPreview(vm, prefixInput, tagContractInput, reservedInput, tagPreviewText)
                }
            }
            
            vm.tagReserved.observe(viewLifecycleOwner) { reserved ->
                println("SettingsFragment: Tag reserved received: $reserved - ${System.currentTimeMillis()}")
                reserved?.let { 
                    reservedInput?.setText(it)
                    updateTagPreview(vm, prefixInput, tagContractInput, reservedInput, tagPreviewText)
                }
            }
            
            // Language change request observer - FIXED: Prevent infinite loops
            vm.languageChangeRequested.observe(viewLifecycleOwner) { requestedLanguage ->
                requestedLanguage?.let { language ->
                    // Prevent infinite loops
                    if (isLanguageChangeInProgress) {
                        println("SettingsFragment: Language change already in progress, ignoring: $language")
                        vm.clearLanguageChangeRequest()
                        return@let
                    }
                    
                    println("SettingsFragment: Language change requested: $language")
                    isLanguageChangeInProgress = true
                    
                    // Apply language change using smooth manual update (no flicker)
                    activity?.let { activity ->
                        try {
                            println("SettingsFragment: ✅ Language change applied successfully: $language")
                            vm.clearLanguageChangeRequest()
                            
                            // Use smooth LocaleHelper method (enhanced manual update)
                            LocaleHelper.applyLanguageToActivity(activity, language)
                            
                            // CRITICAL: Manually refresh all UI elements immediately with target language
                            view?.post {
                                println("SettingsFragment: Manually refreshing UI after language change to: $language")
                                refreshAllUITextElementsForLanguage(language)
                            }
                            
                            // Reset flag after a brief delay to allow processing
                            view?.postDelayed({
                                isLanguageChangeInProgress = false
                            }, 300) // Shorter delay since no recreation
                            
                        } catch (e: Exception) {
                            isLanguageChangeInProgress = false
                            println("SettingsFragment: ❌ Language change failed: ${e.message}")
                            // Show error to user
                            Snackbar.make(binding.root, "Failed to change language: ${e.message}", Snackbar.LENGTH_LONG).show()
                            vm.clearLanguageChangeRequest()
                        }
                    } ?: run {
                        isLanguageChangeInProgress = false
                        vm.clearLanguageChangeRequest()
                    }
                }
            }
            
            // Setup UI listeners (simple approach)
            println("SettingsFragment: Setting up UI listeners - ${System.currentTimeMillis()}")
            
            // Language toggle listener - Set up initially
            setupLanguageListener(languageToggleGroup, vm)
            
            // Power slider listener
            powerSlider?.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
                override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                    val newPowerLevel = slider.value.toInt()
                    vm.updateUHFPower(newPowerLevel)
                }
            })
            
            // Tag Configuration listeners
            prefixInput?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    updateTagPreview(vm, prefixInput, tagContractInput, reservedInput, tagPreviewText)
                    clearTagConfigErrors(prefixInputLayout)
                }
            })
            
            tagContractInput?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    updateTagPreview(vm, prefixInput, tagContractInput, reservedInput, tagPreviewText)
                    clearTagConfigErrors(tagContractInputLayout)
                }
            })
            
            reservedInput?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    updateTagPreview(vm, prefixInput, tagContractInput, reservedInput, tagPreviewText)
                    clearTagConfigErrors(reservedInputLayout)
                }
            })
            
            saveTagConfigButton?.setOnClickListener {
                saveTagConfiguration(vm, prefixInput, prefixInputLayout, tagContractInput, tagContractInputLayout, reservedInput, reservedInputLayout)
            }
            
            // Logout button listener
            logoutButton?.setOnClickListener {
                showLogoutConfirmation()
            }
            
            println("SettingsFragment: UI listeners setup complete - ${System.currentTimeMillis()}")
            
            // Initialize data (background)
            println("SettingsFragment: Initializing all data - ${System.currentTimeMillis()}")
            lifecycleScope.launch(Dispatchers.IO) {
                println("SettingsFragment: Background thread started for full data - ${System.currentTimeMillis()}")
                vm.initializeData()
                println("SettingsFragment: Full data initialization complete - ${System.currentTimeMillis()}")
            }
            
        } catch (e: Exception) {
            println("SettingsFragment: ERROR in setupCompleteSettingsUI: ${e.message}")
            e.printStackTrace()
        }
        
        println("SettingsFragment: setupCompleteSettingsUI() END - ${System.currentTimeMillis()}")
    }

    /**
     * Setup language listener - ISOLATED to prevent infinite loops
     */
    private fun setupLanguageListener(
        languageToggleGroup: com.google.android.material.button.MaterialButtonToggleGroup?,
        vm: SettingsViewModel
    ): Unit {
        languageToggleGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newLanguage = when (checkedId) {
                    R.id.language_en_button -> "en"
                    R.id.language_tc_button -> "tc"
                    R.id.language_cn_button -> "cn"
                    else -> "en"
                }
                println("SettingsFragment: User selected language: $newLanguage - ${System.currentTimeMillis()}")
                vm.updateLanguage(newLanguage)
            }
        }
    }

    /**
     * Format API endpoint for display (show last part of URL)
     */
    private fun formatApiEndpoint(fullUrl: String): String {
        return try {
            val parts = fullUrl.split("//")
            if (parts.size > 1) {
                parts[1] // Remove protocol part
            } else {
                fullUrl
            }
        } catch (e: Exception) {
            fullUrl
        }
    }

    /**
     * Show logout confirmation dialog
     */
    private fun showLogoutConfirmation(): Unit {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.logout_confirmation_title))
        builder.setMessage(getString(R.string.logout_confirmation_message))
        builder.setPositiveButton(getString(R.string.logout)) { _, _ ->
            performLogout()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    /**
     * Perform logout and navigate to login
     */
    private fun performLogout(): Unit {
        viewModel.logout()
        
        // Redirect to AuthActivity for proper login flow
        val intent = android.content.Intent(requireContext(), com.socam.bcms.presentation.AuthActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    /**
     * Refresh UI text elements for a specific target language
     */
    private fun refreshUITextElementsForLanguage(targetLanguage: String) {
        try {
            println("SettingsFragment: Refreshing UI text elements for language: $targetLanguage")
            
            // Create a configuration with the target locale to get correct strings
            val targetLocale = when (targetLanguage) {
                "tc" -> java.util.Locale("zh", "TW")
                "cn" -> java.util.Locale("zh", "CN") 
                else -> java.util.Locale("en")
            }
            
            // Create a context with the target locale
            val config = android.content.res.Configuration(requireContext().resources.configuration)
            config.setLocale(targetLocale)
            val localizedContext = requireContext().createConfigurationContext(config)
            
            // Update toolbar title
            binding.toolbar.title = localizedContext.getString(R.string.settings_title)
            
            // Update power value text with current setting (preserve the current value but update format)
            val powerSlider = binding.root.findViewById<com.google.android.material.slider.Slider>(R.id.power_slider)
            val powerValueText = binding.root.findViewById<TextView>(R.id.power_value_text)
            if (powerSlider != null && powerValueText != null) {
                powerValueText.text = localizedContext.getString(R.string.power_value_format, powerSlider.value.toInt())
            }
            
            // Update Tag Configuration Input Labels (hints and helper text)
            val prefixInputLayout = binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.prefix_input_layout)
            prefixInputLayout?.hint = localizedContext.getString(R.string.tag_prefix_hint)
            prefixInputLayout?.helperText = localizedContext.getString(R.string.tag_prefix_helper)
            
            val reservedInputLayout = binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.reserved_input_layout)
            reservedInputLayout?.hint = localizedContext.getString(R.string.tag_reserved_hint)
            reservedInputLayout?.helperText = localizedContext.getString(R.string.tag_reserved_helper)
            
            // === Language Toggle Buttons ===
            // Update button text - these are the most important for immediate feedback
            binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.language_en_button)?.text = localizedContext.getString(R.string.language_english)
            binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.language_tc_button)?.text = localizedContext.getString(R.string.language_traditional_chinese)  
            binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.language_cn_button)?.text = localizedContext.getString(R.string.language_simplified_chinese)
            
            // === Update Button Text ===
            val saveTagConfigButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.save_tag_config_button)
            saveTagConfigButton?.text = localizedContext.getString(R.string.save_tag_configuration)
            
            val logoutButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.logout_button)
            logoutButton?.text = localizedContext.getString(R.string.logout)
            
            println("SettingsFragment: All UI text elements refreshed successfully for language: $targetLanguage")
            
        } catch (e: Exception) {
            println("SettingsFragment: Error refreshing UI text elements for language: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Legacy method - delegates to the specific language method
     */
    private fun refreshUITextElements() {
        val targetLanguage = viewModel.currentLanguage.value ?: "en"
        refreshUITextElementsForLanguage(targetLanguage)
    }
    
    /**
     * COMPREHENSIVE UI refresh - updates ALL text elements for a specific target language
     */
    private fun refreshAllUITextElementsForLanguage(targetLanguage: String) {
        try {
            println("SettingsFragment: Starting COMPREHENSIVE UI text refresh for language: $targetLanguage")
            
            // 1. Start with the basic refresh (buttons, dynamic elements) 
            refreshUITextElementsForLanguage(targetLanguage)
            
            // 2. Force refresh of ALL static labels by updating them explicitly
            refreshAllStaticLabelsForLanguage(targetLanguage)
            
            // 3. Force a complete layout refresh
            binding.root.post {
                binding.root.requestLayout()
                binding.root.invalidate()
            }
            
            println("SettingsFragment: ✅ COMPREHENSIVE UI refresh completed for language: $targetLanguage")
            
        } catch (e: Exception) {
            println("SettingsFragment: ❌ Error in comprehensive UI refresh: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * COMPREHENSIVE UI refresh - updates ALL text elements including static labels
     * This method handles the case where static @string resources don't update automatically
     */
    private fun refreshAllUITextElements() {
        // Get current language and delegate to the specific method
        val currentLanguage = viewModel.currentLanguage.value ?: "en"
        refreshAllUITextElementsForLanguage(currentLanguage)
    }
    
    /**
     * NUCLEAR OPTION: Force refresh all TextView elements by clearing and re-inflating
     * This should force all string resources to be re-resolved
     */
    private fun forceRefreshAllTextViewsWithStringResources(view: android.view.View) {
        try {
            when (view) {
                is android.view.ViewGroup -> {
                    // Recursively process all child views
                    for (i in 0 until view.childCount) {
                        forceRefreshAllTextViewsWithStringResources(view.getChildAt(i))
                    }
                }
                is TextView -> {
                    // For TextViews, try to force re-inflation by clearing and resetting text
                    try {
                        val originalText = view.text?.toString() ?: ""
                        // Only process if it looks like it might be from string resources
                        if (originalText.isNotEmpty() && !originalText.contains("@") && 
                            !originalText.all { it.isDigit() || it == '.' || it == ' ' || it == ':' }) {
                            
                            view.post {
                                // Force TextView to reload its text from resources
                                // This is a hack but should work
                                val layoutParams = view.layoutParams
                                val parent = view.parent as? android.view.ViewGroup
                                
                                // Temporarily clear the text and set it back to force refresh
                                val currentText = view.text
                                view.text = ""
                                view.text = currentText
                                
                                // Force invalidation
                                view.invalidate()
                                view.requestLayout()
                                
                                println("SettingsFragment: Force-refreshed TextView with text: ${originalText.take(20)}...")
                            }
                        }
                    } catch (e: Exception) {
                        // Continue with other TextViews if one fails
                        println("SettingsFragment: Could not force-refresh TextView: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("SettingsFragment: Error in forceRefreshAllTextViewsWithStringResources: ${e.message}")
        }
    }
    
    /**
     * DIRECT APPROACH: Manually update ALL TextViews with correct language strings
     * This bypasses the resource system issues and directly sets the correct text
     */
    private fun refreshAllStaticLabelsForLanguage(targetLanguage: String) {
        try {
            println("SettingsFragment: DIRECT approach - updating all TextViews manually for language: $targetLanguage")
            
            // Create a configuration with the target locale to get correct strings
            val targetLocale = when (targetLanguage) {
                "tc" -> java.util.Locale("zh", "TW")
                "cn" -> java.util.Locale("zh", "CN") 
                else -> java.util.Locale("en")
            }
            
            // Create a context with the target locale
            val config = android.content.res.Configuration(requireContext().resources.configuration)
            config.setLocale(targetLocale)
            val localizedContext = requireContext().createConfigurationContext(config)
            
            // Now update all labels using the correctly localized context
            updateAllLabelsWithLocalizedContext(localizedContext)
            
        } catch (e: Exception) {
            println("SettingsFragment: Error in refreshAllStaticLabelsForLanguage: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Legacy method - delegates to the specific language method
     */
    private fun refreshAllStaticLabels() {
        val targetLanguage = viewModel.currentLanguage.value ?: "en"
        refreshAllStaticLabelsForLanguage(targetLanguage)
    }
    
    /**
     * Update all labels using a properly localized context
     * Focus only on elements that actually exist in the layouts
     */
    private fun updateAllLabelsWithLocalizedContext(localizedContext: android.content.Context) {
        try {
            println("SettingsFragment: Updating labels with localized context")
            
            // === Buttons (using localized context) ===
            binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.language_en_button)?.text = 
                localizedContext.getString(R.string.language_english)
            binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.language_tc_button)?.text = 
                localizedContext.getString(R.string.language_traditional_chinese)  
            binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.language_cn_button)?.text = 
                localizedContext.getString(R.string.language_simplified_chinese)
                
            val saveTagConfigButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.save_tag_config_button)
            saveTagConfigButton?.text = localizedContext.getString(R.string.save_tag_configuration)
            
            val logoutButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.logout_button)
            logoutButton?.text = localizedContext.getString(R.string.logout)
            
            // === Input Field Hints ===
            val prefixInputLayout = binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.prefix_input_layout)
            prefixInputLayout?.hint = localizedContext.getString(R.string.tag_prefix_hint)
            prefixInputLayout?.helperText = localizedContext.getString(R.string.tag_prefix_helper)
            
            val reservedInputLayout = binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.reserved_input_layout)
            reservedInputLayout?.hint = localizedContext.getString(R.string.tag_reserved_hint)
            reservedInputLayout?.helperText = localizedContext.getString(R.string.tag_reserved_helper)
            
            // === Dynamic Elements (preserve current values but update format) ===
            val powerSlider = binding.root.findViewById<com.google.android.material.slider.Slider>(R.id.power_slider)
            val powerValueText = binding.root.findViewById<TextView>(R.id.power_value_text)
            if (powerSlider != null && powerValueText != null) {
                powerValueText.text = localizedContext.getString(R.string.power_value_format, powerSlider.value.toInt())
            }
            
            // Update toolbar title
            binding.toolbar.title = localizedContext.getString(R.string.settings_title)
            
            // === Force refresh ALL TextViews by recursively updating them ===
            updateAllTextViewsWithLocalizedContext(binding.root, localizedContext)
            
            // === TARGETED APPROACH: Directly fix the specific TextViews that are stuck in Chinese ===
            directlyUpdateChineseLabels(localizedContext)
            
            println("SettingsFragment: ✅ All labels updated with localized context successfully")
            
        } catch (e: Exception) {
            println("SettingsFragment: ❌ Error updating labels with localized context: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Recursively update all TextViews in the view hierarchy with the correct localized context
     * This will update any TextView that uses @string resources
     */
    private fun updateAllTextViewsWithLocalizedContext(view: android.view.View, localizedContext: android.content.Context) {
        try {
            when (view) {
                is android.view.ViewGroup -> {
                    // Recursively process all child views
                    for (i in 0 until view.childCount) {
                        updateAllTextViewsWithLocalizedContext(view.getChildAt(i), localizedContext)
                    }
                }
                is TextView -> {
                    // For TextViews that contain certain patterns, try to refresh them
                    val currentText = view.text?.toString() ?: ""
                    
                    // Try to determine if this TextView should be updated based on its content
                    if (shouldUpdateTextView(currentText)) {
                        // Force the TextView to refresh by using the localized context
                        view.post {
                            view.invalidate()
                            view.requestLayout()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("SettingsFragment: Error updating TextView: ${e.message}")
        }
    }
    
    /**
     * Determine if a TextView should be updated based on its content
     */
    private fun shouldUpdateTextView(text: String): Boolean {
        // Skip if empty or contains only digits/symbols
        if (text.isEmpty() || text.all { it.isDigit() || it == '.' || it == ' ' || it == ':' || it == '/' }) {
            return false
        }
        
        // Skip URLs and email addresses
        if (text.contains("@") || text.contains("://") || text.contains(".com")) {
            return false
        }
        
        // Skip version numbers and specific values
        if (text.matches(Regex("\\d+\\.\\d+.*")) || text.contains("dBm")) {
            return false
        }
        
        // Update everything else (likely labels and UI text)
        return true
    }
    
    /**
     * BIDIRECTIONAL APPROACH: Update labels based on target language
     * Works in both directions: Chinese ↔ English ↔ Chinese
     */
    private fun directlyUpdateChineseLabels(localizedContext: android.content.Context) {
        try {
            println("SettingsFragment: TRI-DIRECTIONAL - Updating labels for any current state (TC ↔ CN ↔ EN)")
            
            // Create comprehensive mapping for all three languages
            val labelMappings = createBidirectionalLabelMappings(localizedContext)
            
            // Debug: Show the size of mappings
            println("SettingsFragment: 📊 Created ${labelMappings.size} tri-directional mappings (TC/CN/EN)")
            
            // Recursively find and update TextViews with any matching text
            findAndUpdateAllLabelTextViews(binding.root, labelMappings)
            
            println("SettingsFragment: ✅ Tri-directional label updates completed (TC/CN/EN)")
            
        } catch (e: Exception) {
            println("SettingsFragment: ❌ Error in directlyUpdateChineseLabels: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Create a comprehensive mapping of all possible label text variations
     * This includes Chinese, English, and any other variations we might encounter
     */
    private fun createBidirectionalLabelMappings(localizedContext: android.content.Context): Map<String, String> {
        // Debug: Check what language the localized context is actually providing
        val testUserDetails = localizedContext.getString(R.string.user_details)
        val testUsername = localizedContext.getString(R.string.username)
        val detectedLanguage = when {
            testUserDetails.contains("用戶") -> "Traditional Chinese (TC)"
            testUserDetails.contains("用户") -> "Simplified Chinese (CN)"
            else -> "English (EN)"
        }
        println("SettingsFragment: 🔍 LocalizedContext detected language: $detectedLanguage")
        println("SettingsFragment: 🔍 Sample strings - user_details: '$testUserDetails', username: '$testUsername'")
        
        return mapOf(
            // User Details Section - ALL LANGUAGE VARIATIONS (TC + CN + EN)
            // Traditional Chinese (TC) variations
            "用戶詳細資訊" to localizedContext.getString(R.string.user_details),
            "用戶名稱" to localizedContext.getString(R.string.username),
            "全名" to localizedContext.getString(R.string.full_name),
            "電子郵件" to localizedContext.getString(R.string.email),
            "部門" to localizedContext.getString(R.string.department),
            "角色" to localizedContext.getString(R.string.role),
            
            // Simplified Chinese (CN) variations - ADD THESE!
            "用户详细信息" to localizedContext.getString(R.string.user_details),
            "用户名称" to localizedContext.getString(R.string.username),
            "全名" to localizedContext.getString(R.string.full_name), // Same in CN
            "电子邮件" to localizedContext.getString(R.string.email),
            "部门" to localizedContext.getString(R.string.department), // Same in CN
            "角色" to localizedContext.getString(R.string.role), // Same in CN
            
            // English variations
            "User Details" to localizedContext.getString(R.string.user_details),
            "Username" to localizedContext.getString(R.string.username),
            "Full Name" to localizedContext.getString(R.string.full_name),
            "Email" to localizedContext.getString(R.string.email),
            "Department" to localizedContext.getString(R.string.department),
            "Role" to localizedContext.getString(R.string.role),
            
            // Project Details Section - ALL LANGUAGE VARIATIONS
            // Traditional Chinese (TC)
            "專案詳細資訊" to localizedContext.getString(R.string.project_details),
            "專案名稱" to localizedContext.getString(R.string.project_name),
            "合約編號" to localizedContext.getString(R.string.contract_number),
            "專案描述" to localizedContext.getString(R.string.project_description),
            
            // Simplified Chinese (CN) - ADD THESE!
            "项目详细信息" to localizedContext.getString(R.string.project_details),
            "项目名称" to localizedContext.getString(R.string.project_name),
            "合同编号" to localizedContext.getString(R.string.contract_number),
            "项目描述" to localizedContext.getString(R.string.project_description),
            
            // English
            "Project Details" to localizedContext.getString(R.string.project_details),
            "Project Name" to localizedContext.getString(R.string.project_name),
            "Contract Number" to localizedContext.getString(R.string.contract_number),
            "Project Description" to localizedContext.getString(R.string.project_description),
            
            // App Configuration Section - ALL LANGUAGE VARIATIONS
            // Traditional Chinese (TC)
            "應用程式配置" to localizedContext.getString(R.string.app_configuration),
            "語言設定" to localizedContext.getString(R.string.language_setting),
            "UHF 傳輸功率" to localizedContext.getString(R.string.uhf_power_setting),
            "API 端點" to localizedContext.getString(R.string.api_endpoint),
            "應用程式版本" to localizedContext.getString(R.string.app_version),
            
            // Simplified Chinese (CN) - ADD THESE!
            "应用程序配置" to localizedContext.getString(R.string.app_configuration),
            "语言设定" to localizedContext.getString(R.string.language_setting),
            "UHF 传输功率" to localizedContext.getString(R.string.uhf_power_setting),
            "API 端点" to localizedContext.getString(R.string.api_endpoint),
            "应用程序版本" to localizedContext.getString(R.string.app_version),
            
            // English
            "App Configuration" to localizedContext.getString(R.string.app_configuration),
            "Language Setting" to localizedContext.getString(R.string.language_setting),
            "UHF Transmission Power" to localizedContext.getString(R.string.uhf_power_setting),
            "API Endpoint" to localizedContext.getString(R.string.api_endpoint),
            "App Version" to localizedContext.getString(R.string.app_version),
            
            // Tag Configuration Section - ALL LANGUAGE VARIATIONS
            // Traditional Chinese (TC)
            "標籤編號配置" to localizedContext.getString(R.string.tag_number_configuration),
            "標籤編號預覽：" to localizedContext.getString(R.string.tag_preview_label),
            "前綴" to localizedContext.getString(R.string.prefix_label),
            "保留" to localizedContext.getString(R.string.reserved_label),
            
            // Simplified Chinese (CN) - ADD THESE!
            "标签编号配置" to localizedContext.getString(R.string.tag_number_configuration),
            "标签编号预览：" to localizedContext.getString(R.string.tag_preview_label),
            "前缀" to localizedContext.getString(R.string.prefix_label),
            "保留" to localizedContext.getString(R.string.reserved_label), // Same in CN
            
            // English
            "Tag Number Configuration" to localizedContext.getString(R.string.tag_number_configuration),
            "Tag Number Preview:" to localizedContext.getString(R.string.tag_preview_label),
            "Prefix" to localizedContext.getString(R.string.prefix_label),
            "Reserved" to localizedContext.getString(R.string.reserved_label),
            
            // Fixed text - ALL LANGUAGE VARIATIONS
            // Traditional Chinese (TC)
            "合約" to detectFixedText(localizedContext, "Contract", "合約", "合同"),
            "版本" to detectFixedText(localizedContext, "Version", "版本", "版本"),
            "BC類型 + 合約 + 計數器" to detectFixedText(localizedContext, "BCType + Contract + Counter", "BC類型 + 合約 + 計數器", "BC类型 + 合同 + 计数器"),
            
            // Simplified Chinese (CN) - ADD THESE!
            "合同" to detectFixedText(localizedContext, "Contract", "合約", "合同"),
            "版本" to detectFixedText(localizedContext, "Version", "版本", "版本"), // Same
            "BC类型 + 合同 + 计数器" to detectFixedText(localizedContext, "BCType + Contract + Counter", "BC類型 + 合約 + 計數器", "BC类型 + 合同 + 计数器"),
            
            // English
            "Contract" to detectFixedText(localizedContext, "Contract", "合約", "合同"),
            "Version" to detectFixedText(localizedContext, "Version", "版本", "版本"),
            "BCType + Contract + Counter" to detectFixedText(localizedContext, "BCType + Contract + Counter", "BC類型 + 合約 + 計數器", "BC类型 + 合同 + 计数器")
        )
    }
    
    /**
     * Detect which fixed text to use based on the target language context
     */
    private fun detectFixedText(localizedContext: android.content.Context, englishText: String, tcText: String, cnText: String): String {
        // Test what language the localized context is providing by checking a known string
        val testString = localizedContext.getString(R.string.user_details)
        return when {
            testString.contains("用戶") -> tcText // Traditional Chinese detected
            testString.contains("用户") -> cnText // Simplified Chinese detected  
            else -> englishText // Default to English
        }
    }
    
    /**
     * Recursively find and update TextViews that contain any mappable text
     * Works bidirectionally: Chinese ↔ English ↔ Chinese
     */
    private fun findAndUpdateAllLabelTextViews(view: android.view.View, mapping: Map<String, String>) {
        try {
            when (view) {
                is android.view.ViewGroup -> {
                    // Recursively process all child views
                    for (i in 0 until view.childCount) {
                        findAndUpdateAllLabelTextViews(view.getChildAt(i), mapping)
                    }
                }
                is TextView -> {
                    val currentText = view.text?.toString()?.trim() ?: ""
                    
                    // Check if this TextView contains any mappable text
                    mapping.forEach { (sourceText, targetText) ->
                        if (currentText == sourceText) {
                            view.text = targetText
                            println("SettingsFragment: 🎯 TRI-DIRECTIONAL UPDATE: '$sourceText' → '$targetText'")
                            return@forEach // Found a match, no need to continue
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("SettingsFragment: Error in findAndUpdateAllLabelTextViews: ${e.message}")
        }
    }
    
    /**
     * DEBUG: Log all TextView contents for debugging purposes
     */
    private fun debugLogAllTextViews(view: android.view.View, phase: String, level: Int = 0) {
        try {
            when (view) {
                is android.view.ViewGroup -> {
                    // Recursively process all child views
                    for (i in 0 until view.childCount) {
                        debugLogAllTextViews(view.getChildAt(i), phase, level + 1)
                    }
                }
                is TextView -> {
                    val currentText = view.text?.toString()?.trim() ?: ""
                    if (currentText.isNotEmpty() && shouldUpdateTextView(currentText)) {
                        val indent = "  ".repeat(level)
                        println("SettingsFragment: 📝 $phase - TextView$indent: '$currentText'")
                    }
                }
            }
        } catch (e: Exception) {
            println("SettingsFragment: Error in debugLogAllTextViews: ${e.message}")
        }
    }
    
    /**
     * Legacy method for backward compatibility  
     */
    private fun findAndUpdateChineseTextViews(view: android.view.View, mapping: Map<String, String>) {
        findAndUpdateAllLabelTextViews(view, mapping)
    }
    
    /**
     * Recursively update all TextView elements in the view hierarchy
     * This forces a refresh of all text that should come from string resources
     */
    private fun updateAllTextViewsRecursively(view: android.view.View) {
        try {
            when (view) {
                is android.view.ViewGroup -> {
                    // Recursively process all child views
                    for (i in 0 until view.childCount) {
                        updateAllTextViewsRecursively(view.getChildAt(i))
                    }
                }
                is TextView -> {
                    // For TextViews, try to refresh their content by getting their current text
                    // and setting it again (this forces a refresh from string resources)
                    val currentText = view.text
                    if (currentText != null && currentText.isNotEmpty()) {
                        view.post {
                            // Force the TextView to re-resolve its text from resources
                            view.invalidate()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("SettingsFragment: Error updating TextView: ${e.message}")
        }
    }

    /**
     * Update tag number preview when inputs change
     */
    private fun updateTagPreview(
        vm: SettingsViewModel,
        prefixInput: com.google.android.material.textfield.TextInputEditText?,
        tagContractInput: com.google.android.material.textfield.TextInputEditText?,
        reservedInput: com.google.android.material.textfield.TextInputEditText?,
        tagPreviewText: TextView?
    ) {
        val prefix = prefixInput?.text?.toString() ?: "34180"
        val tagContract = tagContractInput?.text?.toString() ?: "210573"
        val reserved = reservedInput?.text?.toString() ?: "0"
        val preview = vm.generateTagPreview(prefix, tagContract, reserved)
        tagPreviewText?.text = preview
    }
    
    /**
     * Save tag configuration with validation
     */
    private fun saveTagConfiguration(
        vm: SettingsViewModel,
        prefixInput: com.google.android.material.textfield.TextInputEditText?,
        prefixInputLayout: com.google.android.material.textfield.TextInputLayout?,
        tagContractInput: com.google.android.material.textfield.TextInputEditText?,
        tagContractInputLayout: com.google.android.material.textfield.TextInputLayout?,
        reservedInput: com.google.android.material.textfield.TextInputEditText?,
        reservedInputLayout: com.google.android.material.textfield.TextInputLayout?
    ) {
        val prefix = prefixInput?.text?.toString()?.trim() ?: ""
        val tagContract = tagContractInput?.text?.toString()?.trim() ?: ""
        val reserved = reservedInput?.text?.toString()?.trim() ?: ""
        
        // Clear previous errors
        prefixInputLayout?.error = null
        tagContractInputLayout?.error = null
        reservedInputLayout?.error = null
        
        // Validate inputs
        val prefixError = validateTagPrefix(prefix)
        val tagContractError = validateTagContract(tagContract)
        val reservedError = validateTagReserved(reserved)
        
        var hasErrors = false
        
        if (prefixError != null) {
            prefixInputLayout?.error = prefixError
            hasErrors = true
        }
        
        if (tagContractError != null) {
            tagContractInputLayout?.error = tagContractError
            hasErrors = true
        }
        
        if (reservedError != null) {
            reservedInputLayout?.error = reservedError
            hasErrors = true
        }
        
        if (hasErrors) {
            return
        }
        
        // Save configuration
        val success = vm.updateTagConfiguration(prefix, tagContract, reserved)
        if (success) {
            Snackbar.make(binding.root, getString(R.string.tag_config_saved), Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, getString(R.string.tag_config_error), Snackbar.LENGTH_LONG).show()
        }
    }
    
    /**
     * Validate tag prefix input
     */
    private fun validateTagPrefix(prefix: String): String? {
        return when {
            prefix.isBlank() -> getString(R.string.tag_prefix_required)
            prefix.length != 5 -> getString(R.string.tag_prefix_required)
            !prefix.all { it.isDigit() } -> getString(R.string.tag_prefix_invalid)
            else -> null
        }
    }
    
    /**
     * Validate tag contract number input (6 digits)
     */
    private fun validateTagContract(tagContract: String): String? {
        return when {
            tagContract.isBlank() -> "Tag contract number is required"
            tagContract.length != 6 -> "Tag contract number must be exactly 6 digits"
            !tagContract.all { it.isDigit() } -> "Tag contract number can only contain digits"
            else -> null
        }
    }
    
    /**
     * Validate tag reserved number input
     */
    private fun validateTagReserved(reserved: String): String? {
        return when {
            reserved.isBlank() -> getString(R.string.tag_reserved_required)
            reserved.length != 1 -> getString(R.string.tag_reserved_required)
            !reserved.all { it.isDigit() } -> getString(R.string.tag_reserved_invalid)
            else -> null
        }
    }
    
    /**
     * Clear tag configuration input errors
     */
    private fun clearTagConfigErrors(inputLayout: com.google.android.material.textfield.TextInputLayout?) {
        inputLayout?.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isLanguageChangeInProgress = false // Reset flag on view destruction
        _binding = null
    }
}