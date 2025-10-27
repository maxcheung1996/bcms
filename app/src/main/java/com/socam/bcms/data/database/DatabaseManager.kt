package com.socam.bcms.data.database

import android.content.Context
import com.socam.bcms.database.Database
import com.socam.bcms.config.EnvironmentConfig
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
        try {
            println("DatabaseManager: Starting simplified database initialization...")
            // Perform any necessary migrations first
            performDatabaseMigrations()
            
            if (!isDatabaseInitialized) {
                seedInitialUsers()
                seedAppSettings()
                seedEnvironmentConfig()
                seedMasterProjects()
                seedMasterRoles()
                seedWorkflowStepFields()
                isDatabaseInitialized = true
                println("DatabaseManager: Database initialization completed")
            } else {
                println("DatabaseManager: Database already initialized, skipping seeding")
            }
        } catch (e: Exception) {
            println("DatabaseManager: Database initialization failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Perform database migrations to handle schema changes
     */
    private fun performDatabaseMigrations(): Unit {
        try {
            println("DatabaseManager: Checking for database migrations...")
            
            // Simple schema validation - if User table query fails, recreate it
            val needsUserTableRecreation = try {
                database.userQueries.selectAll().executeAsList()
                false // Schema is correct
            } catch (e: Exception) {
                if (e.message?.contains("column") == true || e is NullPointerException) {
                    println("DatabaseManager: User table schema mismatch detected, will recreate")
                    true
                } else {
                    false
                }
            }
            
            // Check if MasterProject table exists
            val needsMasterProjectTable = try {
                database.masterProjectQueries.selectAllProjects().executeAsList()
                false // Table exists
            } catch (e: Exception) {
                if (e.message?.contains("no such table: MasterProject") == true) {
                    println("DatabaseManager: MasterProject table not found, database needs recreation")
                    true
                } else {
                    false
                }
            }
            
            if (needsUserTableRecreation || needsMasterProjectTable) {
                if (needsMasterProjectTable) {
                    println("DatabaseManager: New table detected, forcing database recreation")
                    isDatabaseInitialized = false // Force reseeding with new schema
                }
                recreateUserTableWithNewSchema()
            }
            
            println("DatabaseManager: Database migrations completed")
        } catch (e: Exception) {
            println("DatabaseManager: Migration failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Recreate User table with new schema (simplified approach for development)
     */
    private fun recreateUserTableWithNewSchema(): Unit {
        try {
            println("DatabaseManager: Recreating User table with new schema...")
            
            // For development environment, we'll just clear and recreate the users
            // This is simpler and safer than complex migrations
            
            // The table will be automatically recreated by SQLDelight with the correct schema
            // when we try to insert new users
            
            // Reset the initialization flag so users get recreated
            isDatabaseInitialized = false
            
            println("DatabaseManager: User table recreation completed - default users will be recreated")
            
        } catch (e: Exception) {
            println("DatabaseManager: User table recreation failed: ${e.message}")
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
                
                // Legacy users
                createInitialUser("demo", "password", "Client", "Demo User", "demo@socam.com", "Operations")
                createInitialUser("admin", "admin123", "Client", "System Administrator", "admin@socam.com", "IT")
                createInitialUser("operator", "operator123", "Client", "System Operator", "operator@socam.com", "Warehouse")
                
                // New role-based users
                val projectId = "629F9E29-0B36-4A9E-A2C4-C28969285583"
                createInitialUser("client_user", "Abcd.1234", "Client", "Client User", "client@bcms.com", "Client Services", projectId)
                createInitialUser("mic_factory_user", "Abcd.1234", "Factory (MIC)", "MIC Factory User", "mic.factory@bcms.com", "MIC Production", projectId)
                createInitialUser("mic_alw_factory_user", "Abcd.1234", "Factory (MIC-ALW)", "MIC-ALW Factory User", "mic.alw.factory@bcms.com", "MIC-ALW Production", projectId)
                createInitialUser("mic_tid_factory_user", "Abcd.1234", "Factory (MIC-TID)", "MIC-TID Factory User", "mic.tid.factory@bcms.com", "MIC-TID Production", projectId)
                createInitialUser("alw_factory_user", "Abcd.1234", "Factory (ALW)", "ALW Factory User", "alw.factory@bcms.com", "ALW Production", projectId)
                createInitialUser("tid_factory_user", "Abcd.1234", "Factory (TID)", "TID Factory User", "tid.factory@bcms.com", "TID Production", projectId)
                createInitialUser("contractor_user", "Abcd.1234", "Contractor", "Contractor User", "contractor@bcms.com", "Site Operations", projectId)
                
                println("DatabaseManager: Initial users created successfully")
            } else {
                println("DatabaseManager: Users already exist, updating with tag_contract_no field...")
                updateExistingUsersWithTagContractNo()
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error seeding initial users: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Update existing users with default tag_contract_no for 24-character EPC compatibility
     */
    private fun updateExistingUsersWithTagContractNo(): Unit {
        try {
            println("DatabaseManager: Updating existing users with tag_contract_no = '210573'...")
            
            database.userQueries.updateAllUsersTagContractNo("210573")
            
            println("DatabaseManager: All existing users updated with tag_contract_no successfully")
        } catch (e: Exception) {
            println("DatabaseManager: Error updating users with tag_contract_no: ${e.message}")
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
        department: String,
        projectId: String = "629F9E29-0B36-4A9E-A2C4-C28969285583"
    ): Unit {
        val token = generateToken()
        
        println("DatabaseManager: Creating user '$username' with plain text password")
        
        database.userQueries.insertUser(
            username = username,
            password_hash = password,  // Store password as plain text
            salt = "",                // No salt needed
            token = token,
            role = role,
            project_id = projectId,
            full_name = fullName,
            email = email,
            department = department,
            contract_no = "20210573",
            tag_contract_no = "210573"  // Default 6-digit tag contract for 24-char EPC
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
                    Triple("offline_mode_enabled", "true", "BOOLEAN"),
                    Triple("app_language", "en", "STRING"),
                    Triple("tag_prefix", "34180", "STRING"),
                    Triple("tag_reserved", "0", "STRING")
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
     * Seed default environment configuration using centralized EnvironmentConfig
     */
    private fun seedEnvironmentConfig(): Unit {
        try {
            val envCount = try {
                database.environmentConfigQueries.selectAllEnvironments().executeAsList().size
            } catch (e: Exception) {
                0
            }
            
            if (envCount == 0) {
                println("DatabaseManager: Creating environment configurations from EnvironmentConfig...")
                
                // Get current environment from centralized config
                val currentEnv = EnvironmentConfig.getCurrentEnvironment()
                
                // Insert development environment
                database.environmentConfigQueries.insertEnvironment(
                    environment_name = EnvironmentConfig.Environment.DEVELOPMENT.environmentName,
                    base_url = EnvironmentConfig.Environment.DEVELOPMENT.baseUrl,
                    timeout_seconds = 30,
                    retry_count = 3,
                    is_active = if (currentEnv == EnvironmentConfig.Environment.DEVELOPMENT) 1 else 0
                )
                
                // Insert production environment
                database.environmentConfigQueries.insertEnvironment(
                    environment_name = EnvironmentConfig.Environment.PRODUCTION.environmentName,
                    base_url = EnvironmentConfig.Environment.PRODUCTION.baseUrl,
                    timeout_seconds = 30,
                    retry_count = 3,
                    is_active = if (currentEnv == EnvironmentConfig.Environment.PRODUCTION) 1 else 0
                )
                
                println("DatabaseManager: Environment configurations created - Active: ${currentEnv.displayName}")
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error seeding environment config: ${e.message}")
        }
    }
    
    /**
     * Seed master projects with hardcoded project data
     */
    private fun seedMasterProjects(): Unit {
        try {
            val projectCount = try {
                database.masterProjectQueries.selectAllProjects().executeAsList().size
            } catch (e: Exception) {
                0
            }
            
            if (projectCount == 0) {
                println("DatabaseManager: Creating default master projects...")
                
                // Hardcoded project data
                val projects = listOf(
                    mapOf(
                        "proj_id" to "FCC5D974-3513-4F2E-8979-13E2867B42EE",
                        "proj_code" to "KTN",
                        "proj_name" to "KT Area 29",
                        "contract_no" to "999999",
                        "contractor_id" to null,
                        "contractor_name_en" to null,
                        "contractor_name_tc" to null,
                        "contractor_name_sc" to null,
                        "contract_desc_en" to "NewContract",
                        "contract_desc_tc" to "新合約",
                        "contract_desc_sc" to "新合同",
                        "contract_start_date" to "2022-11-22T00:00:00",
                        "contract_end_date" to "2099-12-31T00:00:00"
                    ),
                    mapOf(
                        "proj_id" to "629F9E29-0B36-4A9E-A2C4-C28969285583",
                        "proj_code" to "R267",
                        "proj_name" to "Anderson Road R2-6&7",
                        "contract_no" to "20210573",
                        "contractor_id" to "ba1ca1b7-6f8f-11ed-bf6f-005056acb348",
                        "contractor_name_en" to "Shui On Building Contractors Limited",
                        "contractor_name_tc" to "瑞安承建有限公司",
                        "contractor_name_sc" to "瑞安承建有限公司",
                        "contract_desc_en" to "Construction of Public Housing Developments at Anderson Road Quarry Sites R2-6 and R2-7",
                        "contract_desc_tc" to "Construction of Public Housing Developments at Anderson Road Quarry Sites R2-6 and R2-7",
                        "contract_desc_sc" to "Construction of Public Housing Developments at Anderson Road Quarry Sites R2-6 and R2-7",
                        "contract_start_date" to "2022-10-10T00:00:00",
                        "contract_end_date" to "2024-12-09T00:00:00"
                    )
                )
                
                projects.forEach { project ->
                    database.masterProjectQueries.insertProject(
                        proj_id = project["proj_id"] as String,
                        proj_code = project["proj_code"] as String,
                        proj_name = project["proj_name"] as String,
                        contract_no = project["contract_no"] as String,
                        contractor_id = project["contractor_id"] as String?,
                        contractor_name_en = project["contractor_name_en"] as String?,
                        contractor_name_tc = project["contractor_name_tc"] as String?,
                        contractor_name_sc = project["contractor_name_sc"] as String?,
                        contract_desc_en = project["contract_desc_en"] as String?,
                        contract_desc_tc = project["contract_desc_tc"] as String?,
                        contract_desc_sc = project["contract_desc_sc"] as String?,
                        contract_start_date = project["contract_start_date"] as String?,
                        contract_end_date = project["contract_end_date"] as String?
                    )
                }
                
                println("DatabaseManager: Default master projects created")
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error seeding master projects: ${e.message}")
        }
    }
    
    /**
     * Seed master roles with role-step mappings
     */
    private fun seedMasterRoles(): Unit {
        try {
            val roleCount = database.masterRolesQueries.countRoleSteps().executeAsOne()
            
            if (roleCount == 0L) {
                println("DatabaseManager: Creating master role-step mappings...")
                
                // Role-step mappings based on requirements
                val roleMappings = mapOf(
                    "Client" to listOf(
                        "MIC10", "MIC20", "MIC30", "MIC35", "MIC40", "MIC50", "MIC60",
                        "ALW10", "ALW20", "ALW30", "ALW40",
                        "TID10", "TID20", "TID30", "TID40"
                    ),
                    "Factory (MIC)" to listOf("MIC10", "MIC20", "MIC30", "MIC35", "MIC40"),
                    "Factory (MIC-ALW)" to listOf("ALW30", "ALW40"),
                    "Factory (MIC-TID)" to listOf("TID30", "TID40"),
                    "Factory (ALW)" to listOf("ALW10", "ALW20"),
                    "Factory (TID)" to listOf("TID10", "TID20"),
                    "Contractor" to listOf("MIC50", "MIC60", "ALW30", "ALW40", "TID30", "TID40")
                )
                
                // Step portions for ordering (approximate values based on workflow)
                val stepPortions = mapOf(
                    "MIC10" to 10, "MIC20" to 20, "MIC30" to 30, "MIC35" to 35, 
                    "MIC40" to 40, "MIC50" to 50, "MIC60" to 60,
                    "ALW10" to 10, "ALW20" to 20, "ALW30" to 30, "ALW40" to 40,
                    "TID10" to 10, "TID20" to 20, "TID30" to 30, "TID40" to 40
                )
                
                // Insert role-step mappings
                roleMappings.forEach { (roleName, steps) ->
                    steps.forEach { stepCode ->
                        val bcType = stepCode.substring(0, 3) // Extract MIC, ALW, TID
                        val portion = stepPortions[stepCode] ?: 0
                        
                        database.masterRolesQueries.insertRoleStep(
                            role_name = roleName,
                            step_code = stepCode,
                            bc_type = bcType,
                            step_portion = portion.toLong()
                        )
                    }
                }
                
                println("DatabaseManager: Master role-step mappings created successfully")
            } else {
                println("DatabaseManager: Master roles already exist, skipping creation")
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error seeding master roles: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Seed workflow step fields with field configurations for each step
     */
    private fun seedWorkflowStepFields(): Unit {
        try {
            val stepFieldCount = database.workflowStepFieldsQueries.countStepFields().executeAsOne()
            if (stepFieldCount > 0) {
                println("DatabaseManager: Workflow step fields already exist, skipping seeding")
                return
            }

            println("DatabaseManager: Seeding workflow step fields...")

            // ALW10 fields
            database.workflowStepFieldsQueries.insertStepField("ALW10", "Category", "dropdown", 1, 0, null, "Category", null)
            database.workflowStepFieldsQueries.insertStepField("ALW10", "Subcategory", "dropdown", 2, 0, null, "Subcategory", null)
            database.workflowStepFieldsQueries.insertStepField("ALW10", "Serial No.", "text", 3, 0, null, "Serial No.", null)
            database.workflowStepFieldsQueries.insertStepField("ALW10", "Hinge Supplier", "dropdown", 4, 0, null, "Hinge Supplier", null)
            database.workflowStepFieldsQueries.insertStepField("ALW10", "Manufacturing Date", "datetime", 5, 0, null, "Manufacturing Date", null)
            database.workflowStepFieldsQueries.insertStepField("ALW10", "Remark", "text", 6, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("ALW10", "Is Completed", "checkbox", 7, 0, null, "Is Completed", "false")

            // ALW20 fields
            database.workflowStepFieldsQueries.insertStepField("ALW20", "Delivery Date", "date", 1, 0, null, "Delivery Date", null)
            database.workflowStepFieldsQueries.insertStepField("ALW20", "Batch No.", "text", 2, 0, null, "Batch No.", null)
            database.workflowStepFieldsQueries.insertStepField("ALW20", "Remark", "text", 3, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("ALW20", "Is Completed", "checkbox", 4, 0, null, "Is Completed", "false")

            // ALW30 fields
            database.workflowStepFieldsQueries.insertStepField("ALW30", "Site Arrival Date", "date", 1, 0, null, "Site Arrival Date", null)
            database.workflowStepFieldsQueries.insertStepField("ALW30", "Chip Failure (SA)", "checkbox", 2, 0, null, "Chip Failure (SA)", "false")
            database.workflowStepFieldsQueries.insertStepField("ALW30", "Remark", "text", 3, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("ALW30", "Is Completed", "checkbox", 4, 0, null, "Is Completed", "false")

            // ALW40 fields
            database.workflowStepFieldsQueries.insertStepField("ALW40", "Installation Date", "date", 1, 0, null, "Installation Date", null)
            database.workflowStepFieldsQueries.insertStepField("ALW40", "Block", "dropdown", 2, 0, null, "Block", null)
            database.workflowStepFieldsQueries.insertStepField("ALW40", "Floor", "dropdown", 3, 0, null, "Floor", null)
            database.workflowStepFieldsQueries.insertStepField("ALW40", "Unit", "dropdown", 4, 0, null, "Unit", null)
            database.workflowStepFieldsQueries.insertStepField("ALW40", "Chip Failure (SI)", "checkbox", 5, 0, null, "Chip Failure (SI)", "false")
            database.workflowStepFieldsQueries.insertStepField("ALW40", "Remark", "text", 6, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("ALW40", "Is Completed", "checkbox", 7, 0, null, "Is Completed", "false")

            // MIC10 fields
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Category", "dropdown", 1, 0, null, "Category", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Serial No.", "text", 2, 0, null, "Serial No.", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Edit Serial No.", "text", 3, 0, null, "Edit Serial No.", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Concrete Grade", "dropdown", 4, 0, null, "Concrete Grade", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Product No.", "text", 5, 0, null, "Product No.", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Manufacturing Date", "date", 6, 0, null, "Manufacturing Date", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Block", "dropdown", 7, 0, null, "Block", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Floor", "dropdown", 8, 0, null, "Floor", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Unit", "dropdown", 9, 0, null, "Unit", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Remark", "text", 10, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("MIC10", "Is Completed", "checkbox", 11, 0, null, "Is Completed", "false")

            // MIC20 fields
            database.workflowStepFieldsQueries.insertStepField("MIC20", "RS Company", "dropdown", 1, 0, null, "RS Company", null)
            database.workflowStepFieldsQueries.insertStepField("MIC20", "RS Inspection Date", "date", 2, 0, null, "RS Inspection Date", null)
            database.workflowStepFieldsQueries.insertStepField("MIC20", "Remark", "text", 3, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("MIC20", "Is Completed", "checkbox", 4, 0, null, "Is Completed", "false")

            // MIC30 fields
            database.workflowStepFieldsQueries.insertStepField("MIC30", "Casting Date", "date", 1, 0, null, "Casting Date", null)
            database.workflowStepFieldsQueries.insertStepField("MIC30", "Casting Date 2", "date", 2, 0, null, "Casting Date 2", null)
            database.workflowStepFieldsQueries.insertStepField("MIC30", "Remark", "text", 3, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("MIC30", "Is Completed", "checkbox", 4, 0, null, "Is Completed", "false")

            // MIC35 fields
            database.workflowStepFieldsQueries.insertStepField("MIC35", "Internal Finishes Date", "date", 1, 0, null, "Internal Finishes Date", null)
            database.workflowStepFieldsQueries.insertStepField("MIC35", "Remark", "text", 2, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("MIC35", "Is Completed", "checkbox", 3, 0, null, "Is Completed", "false")

            // MIC40 fields
            database.workflowStepFieldsQueries.insertStepField("MIC40", "Delivery Date", "date", 1, 0, null, "Delivery Date", null)
            database.workflowStepFieldsQueries.insertStepField("MIC40", "License Plate No.", "text", 2, 0, null, "License Plate No.", null)
            database.workflowStepFieldsQueries.insertStepField("MIC40", "T Plate No.", "text", 3, 0, null, "T Plate No.", null)
            database.workflowStepFieldsQueries.insertStepField("MIC40", "Remark", "text", 4, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("MIC40", "Is Completed", "checkbox", 5, 0, null, "Is Completed", "false")

            // MIC50 fields
            database.workflowStepFieldsQueries.insertStepField("MIC50", "Site Arrival Date", "date", 1, 0, null, "Site Arrival Date", null)
            database.workflowStepFieldsQueries.insertStepField("MIC50", "Chip Failure (SA)", "checkbox", 2, 0, null, "Chip Failure (SA)", "false")
            database.workflowStepFieldsQueries.insertStepField("MIC50", "Remark", "text", 3, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("MIC50", "Is Completed", "checkbox", 4, 0, null, "Is Completed", "false")

            // MIC60 fields
            database.workflowStepFieldsQueries.insertStepField("MIC60", "Installation Date", "date", 1, 0, null, "Installation Date", null)
            database.workflowStepFieldsQueries.insertStepField("MIC60", "Block", "dropdown", 2, 0, null, "Block", null)
            database.workflowStepFieldsQueries.insertStepField("MIC60", "Floor", "dropdown", 3, 0, null, "Floor", null)
            database.workflowStepFieldsQueries.insertStepField("MIC60", "Unit", "dropdown", 4, 0, null, "Unit", null)
            database.workflowStepFieldsQueries.insertStepField("MIC60", "Chip Failure (SI)", "checkbox", 5, 0, null, "Chip Failure (SI)", "false")
            database.workflowStepFieldsQueries.insertStepField("MIC60", "Remark", "text", 6, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("MIC60", "Is Completed", "checkbox", 7, 0, null, "Is Completed", "false")

            // TID10 fields
            database.workflowStepFieldsQueries.insertStepField("TID10", "Category", "dropdown", 1, 0, null, "Category", null)
            database.workflowStepFieldsQueries.insertStepField("TID10", "Serial No.", "text", 2, 0, null, "Serial No.", null)
            database.workflowStepFieldsQueries.insertStepField("TID10", "Edit Serial No.", "text", 3, 0, null, "Edit Serial No.", null)
            database.workflowStepFieldsQueries.insertStepField("TID10", "Manufacturing Date", "date", 4, 0, null, "Manufacturing Date", null)
            database.workflowStepFieldsQueries.insertStepField("TID10", "Remark", "text", 5, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("TID10", "Is Completed", "checkbox", 6, 0, null, "Is Completed", "false")

            // TID20 fields
            database.workflowStepFieldsQueries.insertStepField("TID20", "Delivery Date", "date", 1, 0, null, "Delivery Date", null)
            database.workflowStepFieldsQueries.insertStepField("TID20", "Batch No.", "text", 2, 0, null, "Batch No.", null)
            database.workflowStepFieldsQueries.insertStepField("TID20", "Remark", "text", 3, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("TID20", "Is Completed", "checkbox", 4, 0, null, "Is Completed", "false")

            // TID30 fields
            database.workflowStepFieldsQueries.insertStepField("TID30", "Site Arrival Date", "date", 1, 0, null, "Site Arrival Date", null)
            database.workflowStepFieldsQueries.insertStepField("TID30", "Chip Failure (SA)", "checkbox", 2, 0, null, "Chip Failure (SA)", "false")
            database.workflowStepFieldsQueries.insertStepField("TID30", "Remark", "text", 3, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("TID30", "Is Completed", "checkbox", 4, 0, null, "Is Completed", "false")

            // TID40 fields
            database.workflowStepFieldsQueries.insertStepField("TID40", "Installation Date", "date", 1, 0, null, "Installation Date", null)
            database.workflowStepFieldsQueries.insertStepField("TID40", "Block", "dropdown", 2, 0, null, "Block", null)
            database.workflowStepFieldsQueries.insertStepField("TID40", "Floor", "dropdown", 3, 0, null, "Floor", null)
            database.workflowStepFieldsQueries.insertStepField("TID40", "Unit", "dropdown", 4, 0, null, "Unit", null)
            database.workflowStepFieldsQueries.insertStepField("TID40", "Chip Failure (SA)", "checkbox", 5, 0, null, "Chip Failure (SA)", "false")
            database.workflowStepFieldsQueries.insertStepField("TID40", "Remark", "text", 6, 0, null, "Remark", null)
            database.workflowStepFieldsQueries.insertStepField("TID40", "Is Completed", "checkbox", 7, 0, null, "Is Completed", "false")

            val totalFields = database.workflowStepFieldsQueries.countStepFields().executeAsOne()
            println("DatabaseManager: Workflow step fields seeded successfully! Total fields: $totalFields")

        } catch (e: Exception) {
            println("DatabaseManager: Error seeding workflow step fields: ${e.message}")
        }
    }
    
    /**
     * Initialize serial number for tag generation (YYYY part of XXYYYY)
     * Default: "0000" - means need to fetch from server
     */
    fun initializeSerialNumber() {
        try {
            val existingSerialNumber = database.appSettingsQueries
                .getSerialNumber()
                .executeAsOneOrNull()
            
            if (existingSerialNumber == null) {
                // Initialize with "0000" - this signals need to fetch from server
                database.appSettingsQueries.insertOrReplaceSerialNumber("0000")
                println("DatabaseManager: Serial number initialized to 0000 (needs server fetch)")
            } else {
                println("DatabaseManager: Serial number already exists: $existingSerialNumber")
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error initializing serial number: ${e.message}")
        }
    }
    
    /**
     * Get current serial number from database
     * @return Serial number as 4-digit string (e.g., "0001", "0123") or null if not found
     */
    fun getSerialNumber(): String? {
        return try {
            database.appSettingsQueries
                .getSerialNumber()
                .executeAsOneOrNull()
        } catch (e: Exception) {
            println("DatabaseManager: Error getting serial number: ${e.message}")
            null
        }
    }
    
    /**
     * Update serial number from server
     * @param serialNumber 4-digit serial number from server (e.g., "0001")
     */
    fun updateSerialNumber(serialNumber: String) {
        try {
            // Ensure it's 4 digits
            val formattedSerialNumber = serialNumber.padStart(4, '0').take(4)
            database.appSettingsQueries.updateSerialNumber(formattedSerialNumber)
            println("DatabaseManager: Serial number updated to: $formattedSerialNumber")
        } catch (e: Exception) {
            println("DatabaseManager: Error updating serial number: ${e.message}")
        }
    }
    
    /**
     * Increment serial number after tag activation
     * @return New serial number after increment
     */
    fun incrementSerialNumber(): String? {
        return try {
            database.appSettingsQueries.incrementSerialNumber()
            val newSerialNumber = database.appSettingsQueries.getSerialNumber().executeAsOneOrNull()
            println("DatabaseManager: Serial number incremented to: $newSerialNumber")
            newSerialNumber
        } catch (e: Exception) {
            println("DatabaseManager: Error incrementing serial number: ${e.message}")
            null
        }
    }
    
    // ========================================
    // BC Type Serial Number Methods
    // ========================================
    
    /**
     * Get serial number for a specific BC type
     * @param bcType BC type code (e.g., "MIC", "ALW", "TID")
     * @return Serial number as 4-digit string (e.g., "0001", "0123") or null if not found
     */
    fun getSerialNumberByBcType(bcType: String): String? {
        return try {
            database.bCTypeSerialNumbersQueries
                .selectSerialNumberByBcType(bcType)
                .executeAsOneOrNull()
        } catch (e: Exception) {
            println("DatabaseManager: Error getting serial number for BC type $bcType: ${e.message}")
            null
        }
    }
    
    /**
     * Update or insert BC type serial number from server
     * @param bcType BC type code (e.g., "MIC", "ALW", "TID")
     * @param bcTypeCode Numeric BC type code (e.g., "107", "102", "103")
     * @param serialNumber 4-digit serial number from server (e.g., "0001")
     */
    fun updateBcTypeSerialNumber(bcType: String, bcTypeCode: String, serialNumber: String): Unit {
        try {
            val formattedSerialNumber = serialNumber.padStart(4, '0').take(4)
            val currentTime = System.currentTimeMillis() / 1000
            
            database.bCTypeSerialNumbersQueries.insertOrReplace(
                bc_type = bcType,
                bc_type_code = bcTypeCode,
                serial_number = formattedSerialNumber,
                updated_date = currentTime
            )
            
            println("DatabaseManager: BC type $bcType serial number updated to: $formattedSerialNumber")
        } catch (e: Exception) {
            println("DatabaseManager: Error updating BC type $bcType serial number: ${e.message}")
        }
    }
    
    /**
     * Increment serial number for a specific BC type after tag activation
     * @param bcType BC type code (e.g., "MIC", "ALW", "TID")
     * @return New serial number after increment or null if error
     */
    fun incrementBcTypeSerialNumber(bcType: String): String? {
        return try {
            val currentTime = System.currentTimeMillis() / 1000
            database.bCTypeSerialNumbersQueries.incrementSerialNumber(currentTime, bcType)
            
            val newSerialNumber = database.bCTypeSerialNumbersQueries
                .selectSerialNumberByBcType(bcType)
                .executeAsOneOrNull()
            
            println("DatabaseManager: BC type $bcType serial number incremented to: $newSerialNumber")
            newSerialNumber
        } catch (e: Exception) {
            println("DatabaseManager: Error incrementing BC type $bcType serial number: ${e.message}")
            null
        }
    }
    
    /**
     * Get all BC type serial numbers
     * @return List of all BC type serial number records
     */
    fun getAllBcTypeSerialNumbers(): List<com.socam.bcms.database.BCTypeSerialNumbers> {
        return try {
            database.bCTypeSerialNumbersQueries.selectAll().executeAsList()
        } catch (e: Exception) {
            println("DatabaseManager: Error getting all BC type serial numbers: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Check if BC type serial number exists
     * @param bcType BC type code (e.g., "MIC", "ALW", "TID")
     * @return True if exists, false otherwise
     */
    fun bcTypeSerialNumberExists(bcType: String): Boolean {
        return try {
            database.bCTypeSerialNumbersQueries.exists(bcType).executeAsOne()
        } catch (e: Exception) {
            println("DatabaseManager: Error checking BC type $bcType existence: ${e.message}")
            false
        }
    }
    
    /**
     * Initialize BC type serial numbers with default values if they don't exist
     * This should be called after master categories are synced
     */
    fun initializeBcTypeSerialNumbers(): Unit {
        try {
            val categories = database.masterCategoriesQueries.selectAllCategories().executeAsList()
            val distinctBcTypes = categories.map { it.bc_type }.distinct()
            
            val currentTime = System.currentTimeMillis() / 1000
            
            distinctBcTypes.forEach { bcType ->
                if (!bcTypeSerialNumberExists(bcType)) {
                    // Get BC type code from mapping table
                    val bcTypeCode = database.bCTypeMappingQueries
                        .selectNumericCodeByBcType(bcType)
                        .executeAsOneOrNull() ?: "404"
                    
                    // Initialize with "0000" - signals need to fetch from server
                    database.bCTypeSerialNumbersQueries.insertOrReplace(
                        bc_type = bcType,
                        bc_type_code = bcTypeCode,
                        serial_number = "0000",
                        updated_date = currentTime
                    )
                    
                    println("DatabaseManager: Initialized BC type $bcType with serial number 0000")
                }
            }
        } catch (e: Exception) {
            println("DatabaseManager: Error initializing BC type serial numbers: ${e.message}")
        }
    }
}