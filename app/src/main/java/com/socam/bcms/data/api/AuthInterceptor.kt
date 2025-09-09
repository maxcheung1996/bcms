package com.socam.bcms.data.api

import android.content.Context
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.domain.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * OkHttp interceptor for adding authentication headers to API requests
 * Automatically adds Bearer token from current authenticated user
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    
    private val databaseManager = DatabaseManager.getInstance(context)
    private val authManager = AuthManager(context)
    
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
        
        // Get current user token
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            throw IOException("No authenticated user found")
        }
        
        // Add authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${currentUser.token}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}
