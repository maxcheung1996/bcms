package com.socam.bcms.data.api

import android.content.Context
import com.socam.bcms.config.EnvironmentConfig
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.data.api.SyncApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

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
     * Get Sync API service instance with current environment configuration
     */
    fun getSyncApiService(): SyncApiService {
        if (retrofit == null) {
            retrofit = createRetrofit()
        }
        return retrofit!!.create(SyncApiService::class.java)
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
        
        // DEVELOPMENT ONLY: Bypass SSL certificate verification based on centralized config
        if (com.socam.bcms.config.EnvironmentConfig.shouldBypassSSL()) {
            try {
                // Create a trust manager that accepts all certificates
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                
                // Create SSL context that uses our trust manager
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                
                // Create hostname verifier that accepts all hostnames
                val allHostnameVerifier = HostnameVerifier { _, _ -> true }
                
                okHttpClient
                    .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier(allHostnameVerifier)
            } catch (e: Exception) {
                e.printStackTrace()
                // If SSL bypass fails, continue with default client
            }
        }
        
        val client = okHttpClient.build()
        
        return Retrofit.Builder()
            .baseUrl(ensureBaseUrlEndsWithSlash(envConfig.baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private fun getActiveEnvironmentConfig(): EnvironmentConfig {
        // First try to get from database
        val config = try {
            databaseManager.database.environmentConfigQueries
                .selectActiveEnvironment()
                .executeAsOneOrNull()
        } catch (e: Exception) {
            println("ApiClient: Failed to get environment from database: ${e.message}")
            null
        }
        
        return if (config != null) {
            // Use database configuration
            EnvironmentConfig(
                environmentName = config.environment_name,
                baseUrl = config.base_url,
                apiKey = config.api_key,
                timeoutSeconds = config.timeout_seconds.toInt(),
                retryCount = config.retry_count.toInt()
            )
        } else {
            // Fallback to centralized environment configuration
            val centralizedEnv = com.socam.bcms.config.EnvironmentConfig.getCurrentEnvironment()
            println("ApiClient: Using centralized environment configuration: ${centralizedEnv.displayName}")
            EnvironmentConfig(
                environmentName = centralizedEnv.environmentName,
                baseUrl = centralizedEnv.baseUrl,
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
