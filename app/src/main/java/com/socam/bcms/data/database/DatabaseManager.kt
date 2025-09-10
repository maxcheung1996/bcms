package com.socam.bcms.data.database

import android.content.Context
import com.socam.bcms.database.Database
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import java.security.SecureRandom

/**
 * Database manager for SQLDelight database operations - SIMPLIFIED for stability
 * 
 * PRIORITY: Stability over security
 * - Plain text passwords (no PBKDF2 hashing)
 * - Minimal initialization
 * - No heavy cryptographic operations
 */
class DatabaseManager private constructor(context: Context) {
    
    private val driver: SqlDriver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = context,
        name = "bcms_database.db"
    )
    
    val database: Database = Database(driver)
    
    companion object {
        @Volatile
        private var INSTANCE: DatabaseManager? = null
        
        fun getInstance(context: Context): DatabaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Close database connection
     */
    fun close(): Unit {
        driver.close()
    }
    
    @Volatile
    private var isDatabaseInitialized = false
    
    /**
     * Initialize database with default data if needed - SIMPLIFIED
     */
    fun initializeDatabase(): Unit {
        if (isDatabaseInitialized) {
            println("DatabaseManager: Database already initialized, skipping")
            return
        }
        
        try {
            println("DatabaseManager: Starting simplified database initialization...")
            seedInitialUsers()
            seedAppSettings()
            seedEnvironmentConfig()
            isDatabaseInitialized = true
            println("DatabaseManager: Database initialization completed")
        } catch (e: Exception) {
            println("DatabaseManager: Database initialization failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Create initial users if they don't exist - SIMPLIFIED with plain text passwords
     */
    private fun seedInitialUsers(): Unit {
        try {
            val demoUser = try {
                database.userQueries.selectByUsername("demo").executeAsOneOrNull()
            } catch (e: Exception) {
                null
            }
            
            if (demoUser == null) {
                println("DatabaseManager: Creating initial users with plain text passwords...")
                createInitialUser("demo", "password", "USER", "Demo User", "demo@socam.com", "Operations")
                createInitialUser("admin", "admin123", "ADMIN", "System Administrator", "admin@socam.com", "IT")
                createInitialUser("operator", "operator123", "OPERATOR", "System Operator", "operator@socam.com", "Warehouse")
                println("DatabaseManager: Initial users created successfully")
            } else {
                println("DatabaseManager: Users already exist, skipping creation")
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error seeding initial users: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Create initial user with PLAIN TEXT password for stability
     */
    private fun createInitialUser(
        username: String,
        password: String,
        role: String,
        fullName: String,
        email: String,
        department: String
    ): Unit {
        val token = generateToken()
        
        println("DatabaseManager: Creating user '$username' with plain text password")
        
        database.userQueries.insertUser(
            username = username,
            password_hash = password,  // Store password as plain text
            salt = "",                // No salt needed
            token = token,
            role = role,
            full_name = fullName,
            email = email,
            department = department
        )
        
        println("DatabaseManager: User '$username' created successfully")
    }
    
    /**
     * Force recreate all users with plain text passwords
     */
    fun recreateUsers(): Unit {
        try {
            println("DatabaseManager: Force recreating all users with plain text passwords...")
            database.userQueries.deleteAllUsers()
            
            createInitialUser("demo", "password", "USER", "Demo User", "demo@socam.com", "Operations")
            createInitialUser("admin", "admin123", "ADMIN", "System Administrator", "admin@socam.com", "IT")
            createInitialUser("operator", "operator123", "OPERATOR", "System Operator", "operator@socam.com", "Warehouse")
            
            println("DatabaseManager: All users recreated successfully")
        } catch (e: Exception) {
            println("DatabaseManager: Error recreating users: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Verify password - SIMPLIFIED for plain text comparison
     */
    fun verifyPassword(inputPassword: String, storedPassword: String, salt: String): Boolean {
        return try {
            // Simple plain text comparison for stability
            inputPassword == storedPassword
        } catch (e: Exception) {
            println("DatabaseManager: Error verifying password: ${e.message}")
            false
        }
    }
    
    /**
     * Generate simple token for user sessions
     */
    private fun generateToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..32)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * Seed default app settings if they don't exist
     */
    private fun seedAppSettings(): Unit {
        try {
            val settingsCount = try {
                database.appSettingsQueries.selectAllSettings().executeAsList().size
            } catch (e: Exception) {
                0
            }
            
            if (settingsCount == 0) {
                println("DatabaseManager: Creating default app settings...")
                
                val defaultSettings = listOf(
                    Triple("app_version", "1.0.0", "STRING"),
                    Triple("uhf_power_level", "30", "INTEGER"),
                    Triple("scan_timeout_seconds", "5", "INTEGER"),
                    Triple("offline_mode_enabled", "true", "BOOLEAN")
                )
                
                defaultSettings.forEach { (key, value, type) ->
                    database.appSettingsQueries.insertSetting(
                        setting_key = key,
                        setting_value = value,
                        setting_type = type,
                        description = "Default $key setting",
                        is_user_configurable = if (key == "app_version") 0 else 1
                    )
                }
                
                println("DatabaseManager: Default app settings created")
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error seeding app settings: ${e.message}")
        }
    }
    
    /**
     * Seed default environment configuration if it doesn't exist
     */
    private fun seedEnvironmentConfig(): Unit {
        try {
            val envCount = try {
                database.environmentConfigQueries.selectAllEnvironments().executeAsList().size
            } catch (e: Exception) {
                0
            }
            
            if (envCount == 0) {
                println("DatabaseManager: Creating default environment configurations...")
                
                database.environmentConfigQueries.insertEnvironment(
                    environment_name = "development",
                    base_url = "https://dev.socam.com/iot/api",
                    timeout_seconds = 30,
                    retry_count = 3,
                    is_active = 1
                )
                
                database.environmentConfigQueries.insertEnvironment(
                    environment_name = "production",
                    base_url = "https://micservice.shuion.com.hk/api",
                    timeout_seconds = 30,
                    retry_count = 3,
                    is_active = 0
                )
                
                println("DatabaseManager: Default environment configurations created")
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error seeding environment config: ${e.message}")
        }
    }
}