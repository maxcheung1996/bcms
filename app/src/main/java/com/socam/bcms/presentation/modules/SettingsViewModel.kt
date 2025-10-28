package com.socam.bcms.presentation.modules

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socam.bcms.BCMSApp
import com.socam.bcms.data.auth.TokenManager
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.utils.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SettingsViewModel - Handles settings screen business logic
 * Manages user data, project details, app configuration, and UHF power settings
 */
class SettingsViewModel(private val context: Context) : ViewModel() {

    private val databaseManager = DatabaseManager.getInstance(context)
    private val authManager = AuthManager.getInstance(context)
    private val tokenManager = TokenManager.getInstance(context)

    // UI State
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // User Details
    private val _userDetails = MutableLiveData<UserDetails>()
    val userDetails: LiveData<UserDetails> = _userDetails

    // Project Details
    private val _projectDetails = MutableLiveData<ProjectDetails>()
    val projectDetails: LiveData<ProjectDetails> = _projectDetails

    // App Configuration
    private val _appConfiguration = MutableLiveData<AppConfiguration>()
    val appConfiguration: LiveData<AppConfiguration> = _appConfiguration

    // Language Setting
    private val _currentLanguage = MutableLiveData<String>("en")
    val currentLanguage: LiveData<String> = _currentLanguage

    // UHF Power Setting
    private val _uhfPowerLevel = MutableLiveData<Int>(30)
    val uhfPowerLevel: LiveData<Int> = _uhfPowerLevel
    
    // Language change callback
    private val _languageChangeRequested = MutableLiveData<String?>()
    val languageChangeRequested: LiveData<String?> = _languageChangeRequested
    
    // Tag Number Configuration Settings
    private val _tagPrefix = MutableLiveData<String>("34180")
    val tagPrefix: LiveData<String> = _tagPrefix
    
    private val _tagContractNo = MutableLiveData<String>("210573")
    val tagContractNo: LiveData<String> = _tagContractNo
    
    private val _tagReserved = MutableLiveData<String>("0")
    val tagReserved: LiveData<String> = _tagReserved
    
    private val _deviceId = MutableLiveData<String>("XX")
    val deviceId: LiveData<String> = _deviceId
    
    private val _serialNumbers = MutableLiveData<String>("Loading...")
    val serialNumbers: LiveData<String> = _serialNumbers
    
    // App Update State
    private val _updateDownloadProgress = MutableLiveData<UpdateDownloadState>()
    val updateDownloadProgress: LiveData<UpdateDownloadState> = _updateDownloadProgress

    // Initialize flag to prevent multiple loads
    private var isDataLoaded = false
    
    // Cache current user to prevent multiple database queries
    private var cachedCurrentUser: com.socam.bcms.database.User? = null

    /**
     * Initialize data loading - called from Fragment after UI setup
     */
    fun initializeData(): Unit {
        println("SettingsViewModel: initializeData() START - ${System.currentTimeMillis()}")
        if (isDataLoaded) {
            println("SettingsViewModel: Data already loaded, skipping - ${System.currentTimeMillis()}")
            return
        }
        isDataLoaded = true
        
        println("SettingsViewModel: Starting loadAllSettingsOptimized() - ${System.currentTimeMillis()}")
        loadAllSettingsOptimized()
    }

    /**
     * Load all settings data with optimized approach - PREVENTS CONCURRENT AuthManager CALLS
     */
    private fun loadAllSettingsOptimized(): Unit {
        println("SettingsViewModel: loadAllSettingsOptimized() START - ${System.currentTimeMillis()}")
        viewModelScope.launch {
            println("SettingsViewModel: ViewModelScope launched - ${System.currentTimeMillis()}")
            _isLoading.value = true
            try {
                // CRITICAL: Load user data FIRST to prevent multiple AuthManager calls
                println("SettingsViewModel: Loading current user - ${System.currentTimeMillis()}")
                loadCurrentUserOnce()
                println("SettingsViewModel: Current user loaded - ${System.currentTimeMillis()}")
                
                // Load ALL data using simple parallel approach
                println("SettingsViewModel: Loading user details from cache - ${System.currentTimeMillis()}")
                val userDetailsJob = launch { loadUserDetailsFromCache() }
                
                println("SettingsViewModel: Loading project details from cache - ${System.currentTimeMillis()}")
                val projectJob = launch { loadProjectDetailsFromCache() }
                
                println("SettingsViewModel: Loading app configuration - ${System.currentTimeMillis()}")
                val appConfigJob = launch { loadAppConfiguration() }
                
                println("SettingsViewModel: Loading language setting - ${System.currentTimeMillis()}")
                val languageJob = launch { loadLanguageSetting() }
                
                println("SettingsViewModel: Loading UHF power setting - ${System.currentTimeMillis()}")
                val powerJob = launch { loadUHFPowerSetting() }
                
                println("SettingsViewModel: Loading tag configuration settings - ${System.currentTimeMillis()}")
                val tagConfigJob = launch { loadTagConfigurationSettings() }
                
                println("SettingsViewModel: Loading device ID and serial numbers - ${System.currentTimeMillis()}")
                val deviceSerialJob = launch { loadDeviceIdAndSerialNumbers() }
                
                println("SettingsViewModel: Waiting for all jobs to complete - ${System.currentTimeMillis()}")
                userDetailsJob.join()
                projectJob.join()
                appConfigJob.join()
                languageJob.join()
                powerJob.join()
                tagConfigJob.join()
                deviceSerialJob.join()
                println("SettingsViewModel: All data loading jobs complete - ${System.currentTimeMillis()}")
                
            } catch (e: Exception) {
                println("SettingsViewModel: ERROR in loadAllSettingsOptimized: ${e.message}")
                _errorMessage.value = "Failed to load settings: ${e.message}"
            } finally {
                println("SettingsViewModel: Setting isLoading to false - ${System.currentTimeMillis()}")
                _isLoading.value = false
                println("SettingsViewModel: loadAllSettingsOptimized() END - ${System.currentTimeMillis()}")
            }
        }
    }

    /**
     * Load current user ONCE to prevent multiple AuthManager calls - CRITICAL OPTIMIZATION
     */
    private suspend fun loadCurrentUserOnce(): Unit = withContext(Dispatchers.IO) {
        println("SettingsViewModel: loadCurrentUserOnce() START - ${System.currentTimeMillis()}")
        try {
            if (cachedCurrentUser == null) {
                println("SettingsViewModel: Calling authManager.getCurrentUser() - ${System.currentTimeMillis()}")
                cachedCurrentUser = authManager.getCurrentUser()
                println("SettingsViewModel: authManager.getCurrentUser() returned: ${cachedCurrentUser?.username} - ${System.currentTimeMillis()}")
            } else {
                println("SettingsViewModel: User already cached: ${cachedCurrentUser?.username} - ${System.currentTimeMillis()}")
            }
        } catch (e: Exception) {
            println("SettingsViewModel: ERROR in loadCurrentUserOnce: ${e.message} - ${System.currentTimeMillis()}")
            cachedCurrentUser = null
        }
        println("SettingsViewModel: loadCurrentUserOnce() END - ${System.currentTimeMillis()}")
    }

    /**
     * Load user details from cached user - NO AuthManager calls
     */
    private suspend fun loadUserDetailsFromCache(): Unit = withContext(Dispatchers.IO) {
        println("SettingsViewModel: loadUserDetailsFromCache() START - ${System.currentTimeMillis()}")
        try {
            val currentUser = cachedCurrentUser
            println("SettingsViewModel: Using cached user: ${currentUser?.username} - ${System.currentTimeMillis()}")
            
            if (currentUser != null) {
                println("SettingsViewModel: Creating UserDetails object - ${System.currentTimeMillis()}")
                val userDetails = UserDetails(
                    username = currentUser.username,
                    fullName = currentUser.full_name,
                    email = currentUser.email ?: "N/A",
                    department = currentUser.department ?: "N/A",
                    role = currentUser.role
                )
                println("SettingsViewModel: Switching to main context to update LiveData - ${System.currentTimeMillis()}")
                withContext(Dispatchers.Main) {
                    println("SettingsViewModel: Setting _userDetails.value - ${System.currentTimeMillis()}")
                    _userDetails.value = userDetails
                    println("SettingsViewModel: _userDetails.value set complete - ${System.currentTimeMillis()}")
                }
            } else {
                println("SettingsViewModel: No cached user, using fallback - ${System.currentTimeMillis()}")
                // Fallback user details if cache is empty
                val fallbackDetails = UserDetails(
                    username = "demo_user",
                    fullName = "Demo User",
                    email = "demo@example.com",
                    department = "Development",
                    role = "Client"
                )
                withContext(Dispatchers.Main) {
                    _userDetails.value = fallbackDetails
                }
            }
        } catch (e: Exception) {
            println("SettingsViewModel: ERROR in loadUserDetailsFromCache: ${e.message} - ${System.currentTimeMillis()}")
            withContext(Dispatchers.Main) {
                println("SettingsViewModel: Error loading user details: ${e.message}")
                // Don't show error for user details, just use fallback
            }
        }
        println("SettingsViewModel: loadUserDetailsFromCache() END - ${System.currentTimeMillis()}")
    }

    /**
     * Load project details from cached user - NO AuthManager calls
     */
    private suspend fun loadProjectDetailsFromCache(): Unit = withContext(Dispatchers.IO) {
        try {
            val currentUser = cachedCurrentUser
            if (currentUser != null) {
                val project = try {
                    databaseManager.database.masterProjectQueries
                        .selectProjectById(currentUser.project_id)
                        .executeAsOneOrNull()
                } catch (e: Exception) {
                    // Handle case where MasterProject table doesn't exist yet
                    if (e.message?.contains("no such table: MasterProject") == true) {
                        println("SettingsViewModel: MasterProject table not found, using fallback data")
                        null
                    } else {
                        throw e
                    }
                }

                if (project != null) {
                    // Determine which description to use based on current language
                    val currentLang = getCurrentLanguageFromSettings()
                    val description = when (currentLang) {
                        "tc" -> project.contract_desc_tc ?: project.contract_desc_en ?: "N/A"
                        "cn" -> project.contract_desc_sc ?: project.contract_desc_en ?: "N/A"
                        else -> project.contract_desc_en ?: "N/A"
                    }

                    val projectDetails = ProjectDetails(
                        projectName = project.proj_name,
                        contractNumber = project.contract_no,
                        projectDescription = description
                    )
                    withContext(Dispatchers.Main) {
                        _projectDetails.value = projectDetails
                    }
                } else {
                    // Fallback project details when table doesn't exist or project not found
                    val fallbackProjectDetails = ProjectDetails(
                        projectName = "Anderson Road R2-6&7", // Default project
                        contractNumber = currentUser.contract_no,
                        projectDescription = "Construction of Public Housing Developments at Anderson Road Quarry Sites R2-6 and R2-7"
                    )
                    withContext(Dispatchers.Main) {
                        _projectDetails.value = fallbackProjectDetails
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                println("SettingsViewModel: Error loading project details: ${e.message}")
                // Provide fallback data using cached user
                _projectDetails.value = ProjectDetails(
                    projectName = "Project Name Unavailable",
                    contractNumber = cachedCurrentUser?.contract_no ?: "N/A",
                    projectDescription = "Project details temporarily unavailable"
                )
            }
        }
    }

    /**
     * Load project details ONLY - does not trigger language updates (prevents infinite loops)
     */
    private suspend fun loadProjectDetailsFromCacheOnly(): Unit = withContext(Dispatchers.IO) {
        println("SettingsViewModel: loadProjectDetailsFromCacheOnly() START - ${System.currentTimeMillis()}")
        try {
            val currentUser = cachedCurrentUser
            if (currentUser != null) {
                val project = try {
                    databaseManager.database.masterProjectQueries
                        .selectProjectById(currentUser.project_id)
                        .executeAsOneOrNull()
                } catch (e: Exception) {
                    // Handle case where MasterProject table doesn't exist yet
                    if (e.message?.contains("no such table: MasterProject") == true) {
                        println("SettingsViewModel: MasterProject table not found, using fallback data")
                        null
                    } else {
                        throw e
                    }
                }

                if (project != null) {
                    // Determine which description to use based on current language
                    val currentLang = _currentLanguage.value ?: "en"  // Use in-memory value, don't query again
                    val description = when (currentLang) {
                        "tc" -> project.contract_desc_tc ?: project.contract_desc_en ?: "N/A"
                        "cn" -> project.contract_desc_sc ?: project.contract_desc_en ?: "N/A"
                        else -> project.contract_desc_en ?: "N/A"
                    }

                    val projectDetails = ProjectDetails(
                        projectName = project.proj_name,
                        contractNumber = project.contract_no,
                        projectDescription = description
                    )
                    withContext(Dispatchers.Main) {
                        _projectDetails.value = projectDetails
                    }
                } else {
                    // Fallback project details when table doesn't exist or project not found
                    val fallbackProjectDetails = ProjectDetails(
                        projectName = "Anderson Road R2-6&7", // Default project
                        contractNumber = currentUser.contract_no,
                        projectDescription = "Construction of Public Housing Developments at Anderson Road Quarry Sites R2-6 and R2-7"
                    )
                    withContext(Dispatchers.Main) {
                        _projectDetails.value = fallbackProjectDetails
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                println("SettingsViewModel: Error loading project details: ${e.message}")
                // Provide fallback data using cached user
                _projectDetails.value = ProjectDetails(
                    projectName = "Project Name Unavailable",
                    contractNumber = cachedCurrentUser?.contract_no ?: "N/A",
                    projectDescription = "Project details temporarily unavailable"
                )
            }
        }
        println("SettingsViewModel: loadProjectDetailsFromCacheOnly() END - ${System.currentTimeMillis()}")
    }

    /**
     * Load app configuration (version, API endpoint) - with fallback
     */
    private suspend fun loadAppConfiguration(): Unit = withContext(Dispatchers.IO) {
        try {
            // Get app version with fallback
            val versionSetting = try {
                databaseManager.database.appSettingsQueries
                    .selectSettingByKey("app_version")
                    .executeAsOneOrNull()
            } catch (e: Exception) {
                null
            }

            // Get current environment with fallback
            val currentEnv = try {
                databaseManager.database.environmentConfigQueries
                    .selectActiveEnvironment()
                    .executeAsOneOrNull()
            } catch (e: Exception) {
                null
            }

            val appConfig = AppConfiguration(
                appVersion = versionSetting?.setting_value ?: "1.0.0",
                apiEndpoint = currentEnv?.base_url ?: "https://micservice.shuion.com.hk/api",
                environmentName = currentEnv?.environment_name ?: "prod"
            )

            withContext(Dispatchers.Main) {
                _appConfiguration.value = appConfig
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                println("SettingsViewModel: Error loading app config: ${e.message}")
                // Use fallback config
                _appConfiguration.value = AppConfiguration(
                    appVersion = "1.0.0",
                    apiEndpoint = "https://micservice.shuion.com.hk/api",
                    environmentName = "prod"
                )
            }
        }
    }

    /**
     * Load current language setting - fast with fallback
     */
    private suspend fun loadLanguageSetting(): Unit = withContext(Dispatchers.IO) {
        try {
            val languageSetting = try {
                databaseManager.database.appSettingsQueries
                    .selectSettingByKey("app_language")
                    .executeAsOneOrNull()
            } catch (e: Exception) {
                null
            }

            val language = languageSetting?.setting_value ?: "en"
            withContext(Dispatchers.Main) {
                _currentLanguage.value = language
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _currentLanguage.value = "en"
            }
        }
    }

    /**
     * Load UHF power setting - fast with fallback
     */
    private suspend fun loadUHFPowerSetting(): Unit = withContext(Dispatchers.IO) {
        try {
            val powerSetting = try {
                databaseManager.database.appSettingsQueries
                    .selectSettingByKey("uhf_power_level")
                    .executeAsOneOrNull()
            } catch (e: Exception) {
                null
            }

            val powerLevel = powerSetting?.setting_value?.toIntOrNull() ?: 30
            withContext(Dispatchers.Main) {
                _uhfPowerLevel.value = powerLevel
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _uhfPowerLevel.value = 30
            }
        }
    }

    /**
     * Update language setting - ENHANCED: Actually change app language using LocaleHelper
     */
    fun updateLanguage(newLanguage: String): Unit {
        println("SettingsViewModel: updateLanguage($newLanguage) START - ${System.currentTimeMillis()}")
        
        // Prevent duplicate updates
        if (_currentLanguage.value == newLanguage) {
            println("SettingsViewModel: Language already set to $newLanguage, skipping update")
            return
        }
        
        // Validate language is supported
        if (!LocaleHelper.isSupportedLanguage(newLanguage)) {
            _errorMessage.value = "Unsupported language: $newLanguage"
            return
        }
        
        viewModelScope.launch {
            try {
                // Save to both database and SharedPreferences for consistency
                withContext(Dispatchers.IO) {
                    // Save to database
                    databaseManager.database.appSettingsQueries.updateSetting(
                        setting_value = newLanguage,
                        setting_key = "app_language"
                    )
                    println("SettingsViewModel: Language saved to database: $newLanguage")
                    
                    // Also save to SharedPreferences for LocaleHelper consistency
                    try {
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("current_language", newLanguage)
                            .apply()
                        println("SettingsViewModel: Language saved to SharedPreferences: $newLanguage")
                    } catch (e: Exception) {
                        println("SettingsViewModel: Failed to save to SharedPreferences: ${e.message}")
                    }
                }
                
                // Update current language state AFTER database save completes
                _currentLanguage.value = newLanguage
                println("SettingsViewModel: Set _currentLanguage.value to $newLanguage")
                
                // Wait a moment to ensure the state update is processed
                withContext(Dispatchers.Main) {
                    // Request language change in Fragment
                    _languageChangeRequested.value = newLanguage
                    println("SettingsViewModel: Language change requested for: $newLanguage (${LocaleHelper.getLanguageDisplayName(newLanguage)})")
                }
                
                // Reload project details for new language (in background)
                launch(Dispatchers.IO) {
                    loadProjectDetailsFromCacheOnly()
                }
                
            } catch (e: Exception) {
                println("SettingsViewModel: ERROR in updateLanguage: ${e.message}")
                _errorMessage.value = "Failed to update language: ${e.message}"
            }
        }
        println("SettingsViewModel: updateLanguage($newLanguage) END - ${System.currentTimeMillis()}")
    }
    
    /**
     * Clear language change request (called by Fragment after handling)
     */
    fun clearLanguageChangeRequest(): Unit {
        _languageChangeRequested.value = null
    }

    /**
     * Update UHF power setting with immediate persistence and hardware update
     * FIXED: Proper global UHF power setting following Tag Modification pattern
     */
    fun updateUHFPower(newPowerLevel: Int): Unit {
        viewModelScope.launch {
            try {
                println("SettingsViewModel: Updating UHF power to $newPowerLevel dBm")
                
                // Validate power level
                if (newPowerLevel < 5 || newPowerLevel > 33) {
                    _errorMessage.value = "Power level must be between 5 and 33 dBm"
                    return@launch
                }

                // Update database first
                withContext(Dispatchers.IO) {
                    databaseManager.database.appSettingsQueries.updateSetting(
                        setting_value = newPowerLevel.toString(),
                        setting_key = "uhf_power_level"
                    )
                    println("SettingsViewModel: Power level saved to database")
                }

                // Update global app setting for immediate use across all modules
                BCMSApp.powerSize = newPowerLevel
                println("SettingsViewModel: Global power level set to ${BCMSApp.powerSize}")

                // Update UHF hardware immediately (following Tag Modification pattern)
                try {
                    withContext(Dispatchers.IO) {
                        val uhfManager = BCMSApp.instance.uhfManager
                        if (uhfManager.isReady()) {
                            val success = uhfManager.setPower(newPowerLevel)
                            if (success) {
                                println("SettingsViewModel: ✅ UHF hardware power updated successfully to $newPowerLevel dBm")
                            } else {
                                println("SettingsViewModel: ⚠️ UHF hardware power update failed")
                                // Don't show error to user - database update succeeded
                            }
                        } else {
                            println("SettingsViewModel: UHF manager not ready - power will be applied when available")
                        }
                    }
                } catch (e: Exception) {
                    // UHF hardware update failed, but database update succeeded
                    println("SettingsViewModel: UHF hardware update exception: ${e.message}")
                    // Don't show error to user - the setting is saved and will be applied
                }

                // Update UI immediately
                _uhfPowerLevel.value = newPowerLevel
                println("SettingsViewModel: UHF power setting completed successfully")
                
            } catch (e: Exception) {
                println("SettingsViewModel: ERROR in updateUHFPower: ${e.message}")
                _errorMessage.value = "Failed to update UHF power: ${e.message}"
            }
        }
    }

    /**
     * Load tag configuration settings - prefix, tag contract, and reserved number
     */
    private suspend fun loadTagConfigurationSettings(): Unit = withContext(Dispatchers.IO) {
        try {
            // Load tag prefix from AppSettings
            val prefixSetting = try {
                databaseManager.database.appSettingsQueries
                    .selectSettingByKey("tag_prefix")
                    .executeAsOneOrNull()
            } catch (e: Exception) {
                null
            }

            // Load tag contract from current user
            val currentUser = cachedCurrentUser ?: authManager.getCurrentUser()
            val tagContractNo = currentUser?.tag_contract_no ?: "210573"

            // Load tag reserved number from AppSettings
            val reservedSetting = try {
                databaseManager.database.appSettingsQueries
                    .selectSettingByKey("tag_reserved")
                    .executeAsOneOrNull()
            } catch (e: Exception) {
                null
            }

            val prefix = prefixSetting?.setting_value ?: "34180"
            val reserved = reservedSetting?.setting_value ?: "0"

            withContext(Dispatchers.Main) {
                _tagPrefix.value = prefix
                _tagContractNo.value = tagContractNo
                _tagReserved.value = reserved
                println("SettingsViewModel: Tag configuration loaded successfully!")
                println("SettingsViewModel: Loaded values - prefix: $prefix, tagContract: $tagContractNo, reserved: $reserved")
                println("SettingsViewModel: Database records found - prefix: ${prefixSetting != null}, reserved: ${reservedSetting != null}")
                if (prefixSetting != null) println("SettingsViewModel: Prefix record - key: ${prefixSetting.setting_key}, value: ${prefixSetting.setting_value}")
                if (reservedSetting != null) println("SettingsViewModel: Reserved record - key: ${reservedSetting.setting_key}, value: ${reservedSetting.setting_value}")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _tagPrefix.value = "34180"
                _tagContractNo.value = "210573"
                _tagReserved.value = "0"
                println("SettingsViewModel: Failed to load tag configuration, using defaults: ${e.message}")
            }
        }
    }

    /**
     * Update tag configuration settings with validation (including tag contract)
     */
    fun updateTagConfiguration(prefix: String, tagContract: String, reserved: String): Boolean {
        return try {
            // Validate inputs
            val prefixError = validateTagPrefix(prefix)
            val tagContractError = validateTagContract(tagContract)
            val reservedError = validateTagReserved(reserved)
            
            if (prefixError != null || tagContractError != null || reservedError != null) {
                _errorMessage.value = prefixError ?: tagContractError ?: reservedError
                return false
            }

            // Save to database and update UI
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        // Save tag prefix with proper error handling
                        try {
                            val updateCount = databaseManager.database.appSettingsQueries.updateSetting(
                                setting_value = prefix,
                                setting_key = "tag_prefix"
                            )
                            println("SettingsViewModel: Tag prefix update completed")
                        } catch (e: Exception) {
                            println("SettingsViewModel: Tag prefix update failed, inserting new record: ${e.message}")
                            // Insert if update fails (first time)
                            databaseManager.database.appSettingsQueries.insertSetting(
                                setting_key = "tag_prefix",
                                setting_value = prefix,
                                setting_type = "STRING",
                                description = "Configurable prefix for tag number generation",
                                is_user_configurable = 1
                            )
                            println("SettingsViewModel: Tag prefix inserted successfully")
                        }

                        // Save tag contract to current user
                        val currentUser = cachedCurrentUser ?: authManager.getCurrentUser()
                        if (currentUser != null) {
                            try {
                                databaseManager.database.userQueries.updateUser(
                                    full_name = currentUser.full_name,
                                    email = currentUser.email,
                                    department = currentUser.department,
                                    contract_no = currentUser.contract_no,
                                    tag_contract_no = tagContract,
                                    role = currentUser.role,
                                    project_id = currentUser.project_id,
                                    id = currentUser.id
                                )
                                println("SettingsViewModel: Tag contract updated in user table")
                            } catch (e: Exception) {
                                println("SettingsViewModel: Failed to update tag contract: ${e.message}")
                                throw e
                            }
                        }

                        // Save tag reserved number with proper error handling
                        try {
                            val updateCount = databaseManager.database.appSettingsQueries.updateSetting(
                                setting_value = reserved,
                                setting_key = "tag_reserved"
                            )
                            println("SettingsViewModel: Tag reserved update completed")
                        } catch (e: Exception) {
                            println("SettingsViewModel: Tag reserved update failed, inserting new record: ${e.message}")
                            // Insert if update fails (first time)
                            databaseManager.database.appSettingsQueries.insertSetting(
                                setting_key = "tag_reserved",
                                setting_value = reserved,
                                setting_type = "STRING",
                                description = "Configurable reserved number for tag generation",
                                is_user_configurable = 1
                            )
                            println("SettingsViewModel: Tag reserved inserted successfully")
                        }

                        // Verify the save by reading back from database
                        val verifyPrefix = databaseManager.database.appSettingsQueries
                            .selectSettingByKey("tag_prefix").executeAsOneOrNull()?.setting_value
                        val verifyReserved = databaseManager.database.appSettingsQueries
                            .selectSettingByKey("tag_reserved").executeAsOneOrNull()?.setting_value
                        
                        println("SettingsViewModel: Tag configuration saved successfully!")
                        println("SettingsViewModel: Saved values - prefix: $prefix, tagContract: $tagContract, reserved: $reserved")
                        println("SettingsViewModel: Verified values - prefix: $verifyPrefix, reserved: $verifyReserved")
                    }

                    // Update UI
                    _tagPrefix.value = prefix
                    _tagContractNo.value = tagContract
                    _tagReserved.value = reserved
                    _errorMessage.value = null
                    
                } catch (e: Exception) {
                    println("SettingsViewModel: ERROR saving tag configuration: ${e.message}")
                    _errorMessage.value = "Failed to save tag configuration: ${e.message}"
                }
            }
            true
        } catch (e: Exception) {
            _errorMessage.value = "Validation error: ${e.message}"
            false
        }
    }

    /**
     * Validate tag prefix input
     */
    private fun validateTagPrefix(prefix: String): String? {
        return when {
            prefix.isBlank() -> "Prefix cannot be empty"
            prefix.length != 5 -> "Prefix must be exactly 5 digits"
            !prefix.all { it.isDigit() } -> "Prefix must contain only numbers"
            else -> null
        }
    }

    /**
     * Validate tag contract number input (6 digits)
     */
    private fun validateTagContract(tagContract: String): String? {
        return when {
            tagContract.isBlank() -> "Tag contract number cannot be empty"
            tagContract.length != 6 -> "Tag contract number must be exactly 6 digits"
            !tagContract.all { it.isDigit() } -> "Tag contract number must contain only numbers"
            else -> null
        }
    }

    /**
     * Validate tag reserved number input
     */
    private fun validateTagReserved(reserved: String): String? {
        return when {
            reserved.isBlank() -> "Reserved number cannot be empty"
            reserved.length != 1 -> "Reserved number must be exactly 1 digit"
            !reserved.all { it.isDigit() } -> "Reserved number must contain only numbers"
            else -> null
        }
    }

    /**
     * Generate tag number preview for UI (24-character format)
     * Format: Prefix + MainContract + Version + Reserved + BCTypeCode + ContractNo + DeviceID + SerialNumber
     * Example: 34180 + 03 + 3 + 9 + XXX + 210573 + 01 + 0001 = 341800339XXX210573010001
     */
    fun generateTagPreview(prefix: String, tagContract: String, reserved: String): String {
        val mainContract = "03"  // Fixed: 2 digits
        val version = "3"         // Fixed: 1 digit
        val bcTypeCode = "XXX"    // Placeholder: 3 digits
        val deviceIdVal = _deviceId.value ?: "XX"  // Device ID: 2 digits
        val serialNo = "0001"     // Placeholder: 4 digits
        
        // Total: 5 + 2 + 1 + 1 + 3 + 6 + 2 + 4 = 24 characters
        return "$prefix$mainContract$version$reserved$bcTypeCode$tagContract$deviceIdVal$serialNo"
    }

    /**
     * Load device ID from BuildConfig and serial numbers from BC Type database
     */
    private suspend fun loadDeviceIdAndSerialNumbers(): Unit = withContext(Dispatchers.IO) {
        try {
            // Load Device ID from BuildConfig
            val deviceIdValue = try {
                com.socam.bcms.BuildConfig.DEVICE_ID
            } catch (e: Exception) {
                "DEVICE_ID_NOT_FOUND"
            }
            
            // Format device ID for display and preview
            val displayDeviceId = if (deviceIdValue == "DEVICE_ID_NOT_FOUND" || deviceIdValue.isBlank()) {
                "DEVICE_ID_NOT_FOUND"
            } else {
                deviceIdValue
            }
            
            val previewDeviceId = if (deviceIdValue == "DEVICE_ID_NOT_FOUND" || deviceIdValue.isBlank()) {
                "XX"
            } else {
                deviceIdValue.padStart(2, '0').take(2)
            }
            
            // CRITICAL: Ensure BC type serial numbers are initialized
            // Initialize from MasterCategories if available, or use common defaults
            initializeBcTypeSerialNumbersIfNeeded()
            
            // Load all BC type serial numbers
            val bcTypeSerialNumbers = databaseManager.getAllBcTypeSerialNumbers()
            
            // Format serial numbers with line breaks: "MIC: 0001\nALW: 0001\nTID: 0001"
            val serialNumbersText = if (bcTypeSerialNumbers.isEmpty()) {
                "No BC types configured"
            } else {
                bcTypeSerialNumbers
                    .sortedBy { it.bc_type } // Sort alphabetically for consistent display
                    .joinToString("\n") { bcType ->
                        "${bcType.bc_type}: ${bcType.serial_number}"
                    }
            }
            
            withContext(Dispatchers.Main) {
                _deviceId.value = displayDeviceId
                _serialNumbers.value = serialNumbersText
                println("SettingsViewModel: Device ID loaded: $displayDeviceId (preview: $previewDeviceId)")
                println("SettingsViewModel: Serial numbers loaded: $serialNumbersText")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _deviceId.value = "DEVICE_ID_NOT_FOUND"
                _serialNumbers.value = "Failed to load serial numbers"
                println("SettingsViewModel: Failed to load device ID/serial numbers: ${e.message}")
            }
        }
    }
    
    /**
     * Initialize BC type serial numbers if they don't exist
     * First tries to use MasterCategories, then falls back to common BC types
     */
    private suspend fun initializeBcTypeSerialNumbersIfNeeded(): Unit = withContext(Dispatchers.IO) {
        try {
            println("SettingsViewModel: Checking if BC type serial numbers need initialization...")
            
            // Try to initialize from MasterCategories first
            databaseManager.initializeBcTypeSerialNumbers()
            
            // Check if we have any records now
            val existingRecords = databaseManager.getAllBcTypeSerialNumbers()
            
            // If still empty, initialize common BC types manually
            if (existingRecords.isEmpty()) {
                println("SettingsViewModel: No BC types in MasterCategories, initializing common types...")
                val commonBcTypes = listOf("MIC", "ALW", "TID")
                val currentTime = System.currentTimeMillis() / 1000
                
                commonBcTypes.forEach { bcType ->
                    // Get BC type code from mapping table
                    val bcTypeCode = try {
                        databaseManager.database.bCTypeMappingQueries
                            .selectNumericCodeByBcType(bcType)
                            .executeAsOneOrNull() ?: "404"
                    } catch (e: Exception) {
                        "404"
                    }
                    
                    // Initialize with "0001" for new installations
                    databaseManager.database.bCTypeSerialNumbersQueries.insertOrReplace(
                        bc_type = bcType,
                        bc_type_code = bcTypeCode,
                        serial_number = "0001",
                        updated_date = currentTime
                    )
                    
                    println("SettingsViewModel: Initialized $bcType with serial number 0001")
                }
            } else {
                println("SettingsViewModel: Found ${existingRecords.size} existing BC type records")
                existingRecords.forEach { 
                    println("SettingsViewModel:   - ${it.bc_type}: ${it.serial_number}")
                }
            }
        } catch (e: Exception) {
            println("SettingsViewModel: Error initializing BC type serial numbers: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Logout user
     */
    fun logout(): Unit {
        viewModelScope.launch {
            try {
                authManager.logout()
                // Additional cleanup if needed
            } catch (e: Exception) {
                _errorMessage.value = "Logout failed: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError(): Unit {
        _errorMessage.value = null
    }

    /**
     * Get current language from settings (synchronous helper)
     */
    private suspend fun getCurrentLanguageFromSettings(): String = withContext(Dispatchers.IO) {
        try {
            val languageSetting = databaseManager.database.appSettingsQueries
                .selectSettingByKey("app_language")
                .executeAsOneOrNull()
            languageSetting?.setting_value ?: "en"
        } catch (e: Exception) {
            "en"
        }
    }

    /**
     * Start app update download
     */
    fun startAppUpdate(downloadUrl: String = "https://example.com/bcms_update.apk"): Unit {
        println("SettingsViewModel: Starting app update download from: $downloadUrl")
        _updateDownloadProgress.value = UpdateDownloadState.Downloading
        
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val apkDownloader = com.socam.bcms.utils.ApkDownloader(context)
                apkDownloader.downloadApk(downloadUrl, "bcms_update.apk") { success, errorMessage ->
                    if (success) {
                        println("SettingsViewModel: App update download completed successfully")
                        _updateDownloadProgress.value = UpdateDownloadState.Completed
                    } else {
                        println("SettingsViewModel: App update download failed: $errorMessage")
                        _updateDownloadProgress.value = UpdateDownloadState.Failed(errorMessage ?: "Unknown error")
                    }
                }
            } catch (e: Exception) {
                println("SettingsViewModel: Error starting app update: ${e.message}")
                _updateDownloadProgress.value = UpdateDownloadState.Failed(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset update download state
     */
    fun resetUpdateState(): Unit {
        _updateDownloadProgress.value = UpdateDownloadState.Idle
    }

    /**
     * Data classes for UI state
     */
    data class UserDetails(
        val username: String,
        val fullName: String,
        val email: String,
        val department: String,
        val role: String
    )

    data class ProjectDetails(
        val projectName: String,
        val contractNumber: String,
        val projectDescription: String
    )

    data class AppConfiguration(
        val appVersion: String,
        val apiEndpoint: String,
        val environmentName: String
    )
    
    /**
     * App update download states
     */
    sealed class UpdateDownloadState {
        object Idle : UpdateDownloadState()
        object Downloading : UpdateDownloadState()
        object Completed : UpdateDownloadState()
        data class Failed(val error: String) : UpdateDownloadState()
    }
}
