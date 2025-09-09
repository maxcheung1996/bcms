package com.socam.bcms.data.api

import android.content.Context
import com.socam.bcms.data.database.DatabaseManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API client factory for creating and managing Retrofit instances
 * Supports environment switching (dev/prod) and authentication
 */
class ApiClient private constructor(private val context: Context) {
    
    private val databaseManager = DatabaseManager.getInstance(context)
    private var retrofit: Retrofit? = null
    
    companion object {
        @Volatile
        private var INSTANCE: ApiClient? = null
        
        fun getInstance(context: Context): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Get API service instance with current environment configuration
     */
    fun getApiService(): ApiService {
        if (retrofit == null) {
            retrofit = createRetrofit()
        }
        return retrofit!!.create(ApiService::class.java)
    }
    
    /**
     * Recreate API client with updated environment settings
     */
    fun recreateClient(): Unit {
        retrofit = null
    }
    
    private fun createRetrofit(): Retrofit {
        val envConfig = getActiveEnvironmentConfig()
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (envConfig.environmentName == "dev") {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(context))
            .connectTimeout(envConfig.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(envConfig.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(envConfig.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(ensureBaseUrlEndsWithSlash(envConfig.baseUrl))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private fun getActiveEnvironmentConfig(): EnvironmentConfig {
        val config = databaseManager.database.environmentConfigQueries
            .selectActiveEnvironment()
            .executeAsOneOrNull()
        
        return if (config != null) {
            EnvironmentConfig(
                environmentName = config.environment_name,
                baseUrl = config.base_url,
                apiKey = config.api_key,
                timeoutSeconds = config.timeout_seconds.toInt(),
                retryCount = config.retry_count.toInt()
            )
        } else {
            // Fallback to dev environment
            EnvironmentConfig(
                environmentName = "dev",
                baseUrl = "https://dev.socam.com/iot/api",
                apiKey = null,
                timeoutSeconds = 30,
                retryCount = 3
            )
        }
    }
    
    private fun ensureBaseUrlEndsWithSlash(baseUrl: String): String {
        return if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }
    
    data class EnvironmentConfig(
        val environmentName: String,
        val baseUrl: String,
        val apiKey: String?,
        val timeoutSeconds: Int,
        val retryCount: Int
    )
}
