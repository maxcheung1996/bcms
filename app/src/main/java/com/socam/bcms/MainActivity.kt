package com.socam.bcms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.socam.bcms.databinding.ActivityMainBinding
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.presentation.AuthActivity
import com.socam.bcms.utils.LocaleHelper

/**
 * Main activity that hosts the primary app navigation
 * Requires user to be authenticated before access
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var authManager: AuthManager
    
    /**
     * Apply saved language locale to Activity context
     * This ensures all strings are loaded in the correct language
     */
    override fun attachBaseContext(newBase: Context?) {
        val context = newBase ?: return
        val localizedContext = LocaleHelper.applyLanguageToActivityContext(context)
        super.attachBaseContext(localizedContext)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup UI first to avoid blocking
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check authentication in background to avoid main thread database access
        checkAuthenticationAsync()
    }
    
    private fun checkAuthenticationAsync(): Unit {
        // Initialize AuthManager and check authentication off main thread
        Thread {
            try {
                // 檢查新的記憶體限制 / Check new memory limit
                val runtime = Runtime.getRuntime()
                val maxMemory = runtime.maxMemory()
                val totalMemory = runtime.totalMemory()
                val freeMemory = runtime.freeMemory()
                val usedMemory = totalMemory - freeMemory
                
                println("=== MEMORY INFO / 記憶體資訊 ===")
                println("Max Heap (最大記憶體): ${maxMemory / 1024 / 1024}MB")
                println("Total Allocated (已分配): ${totalMemory / 1024 / 1024}MB") 
                println("Used Memory (已使用): ${usedMemory / 1024 / 1024}MB")
                println("Free Memory (可用): ${freeMemory / 1024 / 1024}MB")
                println("Available (可用空間): ${(maxMemory - usedMemory) / 1024 / 1024}MB")
                println("===============================")
                
                authManager = AuthManager.getInstance(this)
                val isAuthenticated = authManager.isAuthenticated()
                
                runOnUiThread {
                    if (!isAuthenticated) {
                        // Redirect to login
                        startActivity(Intent(this, AuthActivity::class.java))
                        finish()
                    } else {
                        // User is authenticated, setup navigation
                        setupNavigation()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    // On error, redirect to login for safety
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }
            }
        }.start()
    }
    
    private fun setupNavigation(): Unit {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navController = navHostFragment.navController
        
        // Hide the default ActionBar so only MaterialToolbar is visible
        supportActionBar?.hide()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check authentication status (only if authManager is initialized)
        if (::authManager.isInitialized && !authManager.isAuthenticated()) {
            println("MainActivity: Authentication lost, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
}