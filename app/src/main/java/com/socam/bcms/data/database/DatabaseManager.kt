package com.socam.bcms.data.database

import android.content.Context
import com.socam.bcms.database.Database
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

/**
 * Database manager for SQLDelight database operations
 * Handles database initialization and provides database instance
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
    
    /**
     * Initialize database with default data if needed
     */
    fun initializeDatabase(): Unit {
        // Database schema is automatically created
        // Default data is inserted via SQL INSERT statements in .sq files
    }
}
