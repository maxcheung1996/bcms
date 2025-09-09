package com.socam.bcms.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.socam.bcms.R
import com.socam.bcms.databinding.ActivityAuthBinding
import com.socam.bcms.domain.AuthManager

/**
 * Authentication activity that handles login flow
 * Contains navigation between login and main app screens
 */
class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    private lateinit var navController: NavController
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager(this)
        
        setupNavigation()
        checkExistingAuthentication()
    }
    
    private fun setupNavigation(): Unit {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }
    
    private fun checkExistingAuthentication(): Unit {
        if (authManager.isAuthenticated()) {
            // User is already logged in, navigate to main app
            startActivity(Intent(this, com.socam.bcms.MainActivity::class.java))
            finish()
        }
    }
    
    /**
     * Called when user successfully logs in
     */
    fun onLoginSuccess(): Unit {
        startActivity(Intent(this, com.socam.bcms.MainActivity::class.java))
        finish()
    }
}
