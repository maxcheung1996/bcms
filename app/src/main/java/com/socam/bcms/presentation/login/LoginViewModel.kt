package com.socam.bcms.presentation.login

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.socam.bcms.BuildConfig
import com.socam.bcms.data.api.ApiClient
import com.socam.bcms.data.auth.TokenManager
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.data.dto.SerialNumberRequest
import com.socam.bcms.domain.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build
import android.provider.Settings

/**
 * ViewModel for login screen
 * Handles authentication logic and state management
 */
class LoginViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        // ================================
        // HARDCODED TOKEN CONFIGURATION
        // ================================
        // TODO: Replace these values with your actual token and user information
        private const val HARDCODED_API_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyaWQiOiIwZTIzMTk0NC0wZTlmLTRmY2UtOWJiYy03MWQzNDI0MmQ4MjkiLCJuYW1lIjoiaXRhZG1pbiIsImVtYWlsIjoiaXRhZG1pbkBpdGRlbW8uY29tIiwibmFtZWlkIjoiMGUyMzE5NDQtMGU5Zi00ZmNlLTliYmMtNzFkMzQyNDJkODI5IiwiaHR0cDovL3NjaGVtYXMubWljcm9zb2Z0LmNvbS93cy8yMDA4LzA2L2lkZW50aXR5L2NsYWltcy9leHBpcmF0aW9uIjoiMS8yNC8yMDM1IDU6NTI6MjggUE0iLCJqdGkiOiJhNjE3MDZiMy04Yzg2LTRiMTQtYWFhZC0xZGFlZjVjMjlmMzciLCJyb2xlIjoiQWRtaW4iLCJuYmYiOjE2OTMyNzM5NDgsImV4cCI6MjA1MzI3Mzk0OCwiaWF0IjoxNjkzMjczOTQ4fQ.gmF7dwsrzvTkYpmB9mqnjjOLm3BUomIrTmU5sN2Jr9k"
        private const val HARDCODED_USERNAME = "sfc5732"
        private const val HARDCODED_USER_ROLE = "admin"
    }
    
    private val authManager = AuthManager.getInstance(context)
    private val databaseManager = DatabaseManager.getInstance(context)
    private val tokenManager = TokenManager.getInstance(context)
    private val apiClient = ApiClient.getInstance(context)
    
    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState
    
    private val _environmentInfo = MutableLiveData<EnvironmentInfo>()
    val environmentInfo: LiveData<EnvironmentInfo> = _environmentInfo
    
    /**
     * Perform user authentication
     */
    fun login(username: String, password: String): Unit {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                // First, authenticate with local database
                val result = authManager.authenticateUser(username, password)
                
                when (result) {
                    is AuthManager.AuthResult.Success -> {
                        // Local authentication successful, save hardcoded API token
                        saveHardcodedToken(result.user)
                        // Initialize and fetch serial number if needed
                        initializeSerialNumber(result.user)
                    }
                    is AuthManager.AuthResult.Failure -> {
                        _loginState.value = LoginState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Login failed: ${e.message}")
            }
        }
    }
    
    /**
     * Save hardcoded API token after successful local login
     * Uses the token configured in companion object constants
     */
    private suspend fun saveHardcodedToken(user: com.socam.bcms.database.User) = withContext(Dispatchers.IO) {
        try {
            println("LoginViewModel: Using hardcoded API token...")
            
            // Save the hardcoded API token configured in companion object
            tokenManager.saveAuthData(
                token = HARDCODED_API_TOKEN,
                userName = HARDCODED_USERNAME,
                role = HARDCODED_USER_ROLE
            )
            
            println("LoginViewModel: Hardcoded API token saved for user: $HARDCODED_USERNAME")
            
            withContext(Dispatchers.Main) {
                println("LoginViewModel: Setting LoginState to Success - User: ${user.username}")
                _loginState.value = LoginState.Success(user)
                println("LoginViewModel: LoginState set to Success, current value: ${_loginState.value}")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _loginState.value = LoginState.Error("Token setup failed: ${e.message}")
            }
        }
    }
    
    /**
     * Load application and environment information
     * OPTIMIZED: Handle database errors gracefully and use defaults
     */
    suspend fun loadAppInfo(): Unit {
        viewModelScope.launch {
            try {
                // Ensure database is ready before querying
                val isDbReady = try {
                    databaseManager.database.userQueries.selectByUsername("_test_").executeAsOneOrNull()
                    true
                } catch (e: Exception) {
                    false
                }
                
                if (!isDbReady) {
                    // Database not ready, use defaults
                    _environmentInfo.value = EnvironmentInfo("1.0.0", "dev", "")
                    return@launch
                }
                
                // Get app version (with safe fallback)
                val versionSetting = try {
                    databaseManager.database.appSettingsQueries
                        .selectSettingByKey("app_version")
                        .executeAsOneOrNull()
                } catch (e: Exception) {
                    null
                }
                
                // Get current environment (with safe fallback)
                val currentEnv = try {
                    databaseManager.database.environmentConfigQueries
                        .selectActiveEnvironment()
                        .executeAsOneOrNull()
                } catch (e: Exception) {
                    null
                }
                
                val envInfo = EnvironmentInfo(
                    version = versionSetting?.setting_value ?: "1.0.0",
                    environmentName = currentEnv?.environment_name ?: "dev",
                    baseUrl = currentEnv?.base_url ?: ""
                )
                
                _environmentInfo.value = envInfo
            } catch (e: Exception) {
                // Use default values if anything fails
                println("LoginViewModel: Error loading app info: ${e.message}")
                _environmentInfo.value = EnvironmentInfo("1.0.0", "dev", "")
            }
        }
    }
    
    /**
     * Initialize serial number for tag generation
     * Step 1: Check if BC type serial numbers exist in local database
     * Step 2: If any BC type serial number is "0000" or missing, fetch from API
     */
    private suspend fun initializeSerialNumber(user: com.socam.bcms.database.User) = withContext(Dispatchers.IO) {
        try {
            println("LoginViewModel: Initializing BC type serial numbers...")
            
            // Initialize BC type serial numbers with "0000" if they don't exist
            databaseManager.initializeBcTypeSerialNumbers()
            
            // Get all BC type serial numbers
            val bcTypeSerialNumbers = databaseManager.getAllBcTypeSerialNumbers()
            println("LoginViewModel: Found ${bcTypeSerialNumbers.size} BC type serial numbers")
            
            // Check if any BC type needs fetching from server (serial number is "0000")
            val needsServerFetch = bcTypeSerialNumbers.isEmpty() || 
                bcTypeSerialNumbers.any { it.serial_number == "0000" }
            
            if (needsServerFetch) {
                println("LoginViewModel: BC type serial numbers need initialization, fetching from server...")
                fetchBcTypeSerialNumbersFromServer(user)
            } else {
                println("LoginViewModel: All BC type serial numbers already initialized")
                bcTypeSerialNumbers.forEach { 
                    println("LoginViewModel:   - ${it.bc_type}: ${it.serial_number}")
                }
            }
        } catch (e: Exception) {
            println("LoginViewModel: Error initializing BC type serial numbers: ${e.message}")
            e.printStackTrace()
            // Don't fail login if serial number initialization fails
            // It can be retried later
        }
    }
    
    /**
     * Fetch serial number from backend API
     */
    private suspend fun fetchSerialNumberFromServer(user: com.socam.bcms.database.User) = withContext(Dispatchers.IO) {
        try {
            println("LoginViewModel: Fetching serial number from API...")
            
            // Get device serial number (unique identifier)
            val deviceSerialNumber = getDeviceSerialNumber()
            println("LoginViewModel: Device serial number: $deviceSerialNumber")
            
            // Prepare API request
            val request = SerialNumberRequest(
                device_mac_address = deviceSerialNumber,
                user_id = user.id.toString(), // Convert Long to String
                project_id = BuildConfig.PROJECT_ID
            )
            
            // Call API
            val apiService = apiClient.getSyncApiService()
            val response = apiService.getDeviceSerialNumber(request)
            
            if (response.isSuccessful && response.body() != null) {
                val serialNumberResponse = response.body()!!
                
                if (serialNumberResponse.success) {
                    // Update local database with fetched serial number
                    databaseManager.updateSerialNumber(serialNumberResponse.serial_number)
                    println("LoginViewModel: Serial number fetched and saved: ${serialNumberResponse.serial_number}")
                } else {
                    println("LoginViewModel: API returned failure: ${serialNumberResponse.message}")
                }
            } else {
                println("LoginViewModel: Failed to fetch serial number from API: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            println("LoginViewModel: Error fetching serial number from API: ${e.message}")
            // Don't fail login - serial number can be fetched later
            // For now, we'll use local increment starting from "0001" if API fails
            e.printStackTrace()
        }
    }
    
    /**
     * Fetch BC type serial numbers from backend API
     * Fetches latest serial numbers for all BC types in the current project
     * 
     * API: POST SerialNo/Latest/
     * Request: { ProjId, DeviceId, Username }
     * Response: [ { BCType, ProjId, GunNum, SerialNo } ]
     */
    private suspend fun fetchBcTypeSerialNumbersFromServer(user: com.socam.bcms.database.User) = withContext(Dispatchers.IO) {
        try {
            println("LoginViewModel: Fetching BC type serial numbers from API...")
            
            // Prepare API request
            val request = com.socam.bcms.data.dto.BCTypeSerialNumberRequest(
                projId = BuildConfig.PROJECT_ID,
                deviceId = BuildConfig.DEVICE_ID,
                username = user.username
            )
            
            println("LoginViewModel: Request - ProjId: ${request.projId}, DeviceId: ${request.deviceId}, Username: ${request.username}")
            
            // Call API to get BC type serial numbers
            val apiService = apiClient.getSyncApiService()
            val response = apiService.getBCTypeSerialNumbers(request)
            
            if (response.isSuccessful && response.body() != null) {
                val bcTypeSerialNumbers = response.body()!!
                
                println("LoginViewModel: Received ${bcTypeSerialNumbers.size} BC type serial numbers from server")
                
                // Update local database with fetched serial numbers
                bcTypeSerialNumbers.forEach { bcTypeDto ->
                    // Get numeric code from BCTypeMapping table
                    val bcTypeCode = databaseManager.getNumericCodeByBcType(bcTypeDto.bcType)
                    
                    if (bcTypeCode != null) {
                        databaseManager.updateBcTypeSerialNumber(
                            bcType = bcTypeDto.bcType,
                            bcTypeCode = bcTypeCode,
                            serialNumber = bcTypeDto.serialNo
                        )
                        println("LoginViewModel: Updated ${bcTypeDto.bcType} (${bcTypeCode}) serial number to ${bcTypeDto.serialNo}")
                    } else {
                        println("LoginViewModel: WARNING - BC type ${bcTypeDto.bcType} not found in BCTypeMapping table, skipping")
                    }
                }
                
                println("LoginViewModel: BC type serial numbers successfully updated")
                
            } else {
                println("LoginViewModel: Failed to fetch BC type serial numbers from API: ${response.code()} - ${response.message()}")
                // Don't fail login - can use local counters starting from "0001"
            }
        } catch (e: Exception) {
            println("LoginViewModel: Error fetching BC type serial numbers from API: ${e.message}")
            e.printStackTrace()
            // Don't fail login - BC type serial numbers can be fetched later
            // For now, we'll use local increment starting from "0001" if API fails
        }
    }
    
    /**
     * Get device unique identifier (serial number)
     * Note: Android 10+ restricts access to device identifiers for privacy
     */
    private fun getDeviceSerialNumber(): String {
        return try {
            // Try to get Android ID (most reliable on modern Android)
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            
            if (androidId != null && androidId.isNotEmpty() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                // Fallback to Build.SERIAL (deprecated but works on older devices)
                Build.SERIAL
            }
        } catch (e: Exception) {
            println("LoginViewModel: Error getting device serial number: ${e.message}")
            // Ultimate fallback - use a generated ID
            "UNKNOWN_DEVICE"
        }
    }
    
    /**
     * Reset login state
     */
    fun resetState(): Unit {
        _loginState.value = LoginState.Idle
    }
}

/**
 * Login UI states
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: com.socam.bcms.database.User) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * Environment and app information
 */
data class EnvironmentInfo(
    val version: String,
    val environmentName: String,
    val baseUrl: String
)

/**
 * ViewModel factory for dependency injection
 */
class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
