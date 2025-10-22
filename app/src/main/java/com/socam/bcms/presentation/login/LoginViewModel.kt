package com.socam.bcms.presentation.login

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.socam.bcms.data.auth.TokenManager
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
