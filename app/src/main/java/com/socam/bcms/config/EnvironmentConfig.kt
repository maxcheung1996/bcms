package com.socam.bcms.config

/**
 * Centralized Environment Configuration
 * 
 * CRITICAL: This is the SINGLE place to control dev/prod environment
 * Change BUILD_ENVIRONMENT to switch between environments
 */
object EnvironmentConfig {
    
    /**
     * MASTER ENVIRONMENT SWITCH
     * 
     * Set to:
     * - Environment.DEVELOPMENT for dev environment
     * - Environment.PRODUCTION for prod environment
     */
    //val BUILD_ENVIRONMENT = Environment.PRODUCTION // ← COMMENT HERE TO SWITCH
    val BUILD_ENVIRONMENT = Environment.DEVELOPMENT // ← COMMENT HERE TO SWITCH

    enum class Environment(
        val environmentName: String,
        val baseUrl: String,
        val displayName: String
    ) {
        DEVELOPMENT(
            environmentName = "development",
            baseUrl = "https://dev.socam.com/iot/api",
            displayName = "Development"
        ),
        PRODUCTION(
            environmentName = "production", 
            baseUrl = "https://micservice.shuion.com.hk/api",
            displayName = "Production"
        )
    }
    
    /**
     * Get current environment configuration
     */
    fun getCurrentEnvironment(): Environment = BUILD_ENVIRONMENT
    
    /**
     * Get current base URL
     */
    fun getBaseUrl(): String = BUILD_ENVIRONMENT.baseUrl
    
    /**
     * Get current environment name
     */
    fun getEnvironmentName(): String = BUILD_ENVIRONMENT.environmentName
    
    /**
     * Check if running in development mode
     */
    fun isDevelopment(): Boolean = BUILD_ENVIRONMENT == Environment.DEVELOPMENT
    
    /**
     * Check if running in production mode  
     */
    fun isProduction(): Boolean = BUILD_ENVIRONMENT == Environment.PRODUCTION
    
    /**
     * Get SSL bypass setting (only for dev or specific prod domains)
     */
    fun shouldBypassSSL(): Boolean {
        return isDevelopment() || BUILD_ENVIRONMENT.baseUrl.contains("micservice.shuion.com.hk")
    }
}
