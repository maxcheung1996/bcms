package com.socam.bcms.data.api

import android.content.Context
import com.socam.bcms.data.auth.TokenManager
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * OkHttp interceptor for adding authentication headers to API requests
 * Automatically adds Bearer token from TokenManager for authenticated users
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    
    private val databaseManager = DatabaseManager.getInstance(context)
    private val authManager = AuthManager.getInstance(context)
    private val tokenManager = TokenManager.getInstance(context)
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip auth for certain endpoints
        val skipAuthPaths = listOf("/System/Health", "/Auth/Login")
        val shouldSkipAuth = skipAuthPaths.any { path ->
            originalRequest.url.encodedPath.contains(path)
        }
        
        if (shouldSkipAuth) {
            return chain.proceed(originalRequest)
        }
        
        // Check if user is authenticated and get API token
        if (!authManager.isAuthenticated()) {
            throw IOException("User not authenticated")
        }
        
        val apiToken = tokenManager.getBearerToken()
        if (apiToken == null) {
            throw IOException("No API token available")
        }
        
        // Add authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader("Authorization", apiToken)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}
