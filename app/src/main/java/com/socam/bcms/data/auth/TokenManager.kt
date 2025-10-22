package com.socam.bcms.data.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * TokenManager - Global token storage and management
 * Handles authentication tokens for the entire app
 */
class TokenManager private constructor(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "bcms_auth", Context.MODE_PRIVATE
    )
    
    companion object {
        @Volatile
        private var INSTANCE: TokenManager? = null
        
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "user_role"
        private const val KEY_LOGIN_TIME = "login_time"
        
        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Save authentication data after successful login
     */
    fun saveAuthData(token: String, userName: String, role: String): Unit {
        sharedPreferences.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USERNAME, userName)
            putString(KEY_ROLE, role)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
        println("TokenManager: Auth data saved for user: $userName")
    }
    
    /**
     * Get current authentication token
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }
    
    /**
     * Get current username
     */
    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }
    
    /**
     * Get current user role
     */
    fun getUserRole(): String? {
        return sharedPreferences.getString(KEY_ROLE, null)
    }
    
    /**
     * Check if user is authenticated (has valid token)
     */
    fun isAuthenticated(): Boolean {
        val token = getToken()
        return !token.isNullOrEmpty()
    }
    
    /**
     * Get login time
     */
    fun getLoginTime(): Long {
        return sharedPreferences.getLong(KEY_LOGIN_TIME, 0)
    }
    
    /**
     * Clear all authentication data (logout)
     */
    fun clearAuthData(): Unit {
        sharedPreferences.edit().clear().apply()
        println("TokenManager: Auth data cleared")
    }
    
    /**
     * Get Bearer token for API calls
     */
    fun getBearerToken(): String? {
        val token = getToken()
        return if (token != null) "Bearer $token" else null
    }
}
