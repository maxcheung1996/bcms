package com.socam.bcms

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.socam.bcms.databinding.ActivityMainBinding
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.presentation.AuthActivity

/**
 * Main activity that hosts the primary app navigation
 * Requires user to be authenticated before access
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authManager = AuthManager(this)
        
        // Check if user is authenticated
        if (!authManager.isAuthenticated()) {
            // Redirect to login
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
    }
    
    private fun setupNavigation(): Unit {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup action bar with navigation
        setupActionBarWithNavController(navController)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check authentication status
        if (!authManager.isAuthenticated()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
}